/**
 * F1 SignalR Core Client
 * Interfaces with official F1 Live Timing stream (livetiming.formula1.com/signalrcore)
 */

const { EventEmitter } = require('events');
const { SIGNALR_NEGOTIATE_URL, SIGNALR_WS_BASE_URL, DEFAULT_HEADERS, TOPICS, RECORD_SEPARATOR } = require('./config');
const TelemetryDecoder = require('./decoder');

class F1SignalRClient extends EventEmitter {
  constructor(options = {}) {
    super();
    this.negotiateUrl = options.negotiateUrl || SIGNALR_NEGOTIATE_URL;
    this.wsBaseUrl = options.wsBaseUrl || SIGNALR_WS_BASE_URL;
    this.headers = { ...DEFAULT_HEADERS, ...(options.headers || {}) };
    this.topics = options.topics || TOPICS;
    this.ws = null;
    this.connectionToken = null;
    this.cookie = null;
    this.isConnected = false;
    this.handshakeCompleted = false;
    this.messageBuffer = '';
  }

  /**
   * Performs the HTTP negotiate request with F1 SignalR Core endpoint
   */
  async negotiate() {
    const startTime = Date.now();
    try {
      const response = await fetch(this.negotiateUrl, {
        method: 'POST',
        headers: this.headers
      });

      const latencyMs = Date.now() - startTime;

      if (!response.ok) {
        throw new Error(`Negotiate failed with status ${response.status} (${response.statusText})`);
      }

      this.cookie = response.headers.get('set-cookie');
      const data = await response.json();

      if (!data.connectionToken) {
        throw new Error('Negotiation response did not contain a valid connectionToken');
      }

      this.connectionToken = data.connectionToken;
      return {
        success: true,
        latencyMs,
        connectionId: data.connectionId,
        connectionToken: data.connectionToken,
        availableTransports: data.availableTransports,
        cookie: this.cookie
      };
    } catch (err) {
      return {
        success: false,
        latencyMs: Date.now() - startTime,
        error: err.message
      };
    }
  }

  /**
   * Establishes WebSocket connection and completes SignalR protocol handshake
   */
  async connect() {
    if (!this.connectionToken) {
      const negResult = await this.negotiate();
      if (!negResult.success) {
        throw new Error(`Cannot connect: negotiation failed (${negResult.error})`);
      }
    }

    const wsUrl = `${this.wsBaseUrl}?id=${encodeURIComponent(this.connectionToken)}`;

    return new Promise((resolve, reject) => {
      const wsHeaders = {
        'User-Agent': this.headers['User-Agent'] || 'BestHTTP'
      };
      if (this.cookie) {
        wsHeaders['Cookie'] = this.cookie;
      }

      this.ws = new WebSocket(wsUrl, { headers: wsHeaders });

      const connectionTimeout = setTimeout(() => {
        if (!this.isConnected) {
          if (this.ws) this.ws.close();
          reject(new Error('Connection timed out after 10000ms'));
        }
      }, 10000);

      this.ws.onopen = () => {
        this.isConnected = true;
        // Send SignalR protocol handshake
        const handshakeMessage = JSON.stringify({ protocol: 'json', version: 1 }) + RECORD_SEPARATOR;
        this.ws.send(handshakeMessage);
      };

      this.ws.onmessage = (event) => {
        const raw = event.data.toString();
        this._handleIncomingData(raw, resolve);
      };

      this.ws.onerror = (err) => {
        clearTimeout(connectionTimeout);
        this.emit('error', err);
        if (!this.isConnected) {
          reject(err);
        }
      };

      this.ws.onclose = (ev) => {
        clearTimeout(connectionTimeout);
        this.isConnected = false;
        this.handshakeCompleted = false;
        this.emit('close', { code: ev.code, reason: ev.reason });
      };
    });
  }

  /**
   * Subscribes to the desired streaming topics
   */
  subscribe(topicsToSubscribe = null) {
    if (!this.isConnected || !this.ws) {
      throw new Error('Cannot subscribe: client is not connected');
    }

    const topics = topicsToSubscribe || this.topics;
    const invocation = {
      type: 1, // Invocation
      target: 'Subscribe',
      arguments: [topics]
    };

    this.ws.send(JSON.stringify(invocation) + RECORD_SEPARATOR);
    this.emit('subscribed', topics);
  }

  /**
   * Internal parser for SignalR Core framing
   */
  _handleIncomingData(rawChunk, handshakeResolver) {
    this.messageBuffer += rawChunk;

    const messages = this.messageBuffer.split(RECORD_SEPARATOR);
    // The last item is either empty or an incomplete message
    this.messageBuffer = messages.pop() || '';

    for (const msgStr of messages) {
      if (!msgStr || msgStr.trim() === '') continue;

      if (!this.handshakeCompleted) {
        // Initial handshake response from server is typically "{}"
        try {
          const handshake = JSON.parse(msgStr);
          if (handshake.error) {
            this.emit('error', new Error(handshake.error));
            return;
          }
          this.handshakeCompleted = true;
          this.emit('connected');
          this.subscribe();
          if (handshakeResolver) handshakeResolver(this);
          continue;
        } catch {
          // Continue to parse as regular message
        }
      }

      try {
        const parsed = JSON.parse(msgStr);

        // Ping message
        if (parsed.type === 6) {
          this.emit('ping');
          continue;
        }

        // Hub invocation message from server (type 1)
        if (parsed.type === 1 && parsed.target === 'feed') {
          const [topic, payload, timestamp] = parsed.arguments || [];

          let decoded = payload;
          if (topic === 'CarData.z' && typeof payload === 'string') {
            try {
              decoded = TelemetryDecoder.decodeCarData(payload);
            } catch (decErr) {
              this.emit('decodeError', { topic, error: decErr.message });
            }
          }

          this.emit('feed', { topic, data: decoded, rawPayload: payload, timestamp });
          this.emit(`feed:${topic}`, { data: decoded, rawPayload: payload, timestamp });
        } else {
          this.emit('message', parsed);
        }
      } catch (err) {
        this.emit('parseError', { raw: msgStr, error: err.message });
      }
    }
  }

  /**
   * Disconnects the WebSocket
   */
  disconnect() {
    if (this.ws) {
      try {
        this.ws.close();
      } catch {
        // Ignore close errors
      }
      this.ws = null;
    }
    this.isConnected = false;
    this.handshakeCompleted = false;
  }
}

module.exports = F1SignalRClient;

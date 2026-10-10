/**
 * FastestLap LiveTiming - F1 SignalR Core Client
 *
 * Direct WebSocket client for the official Formula 1 Live Timing SignalR service.
 * Handles HTTP negotiation, cookies, WS handshake, subscription and delta decoding.
 */

const { TelemetryDecoder } = require('./telemetry_decoder');

const F1_ORIGIN = 'https://www.formula1.com';
const F1_HTTP_BASE = 'https://livetiming.formula1.com/signalrcore';
const F1_WS_BASE = 'wss://livetiming.formula1.com/signalrcore';

const RECORD_SEPARATOR = '\x1e';
const HANDSHAKE_PAYLOAD = `{"protocol":"json","version":1}${RECORD_SEPARATOR}`;

const TOPICS = [
  'Heartbeat',
  'CarData.z',
  'Position.z',
  'TimingData',
  'TimingDataF1',
  'TimingAppData',
  'DriverList',
  'SessionInfo',
  'WeatherData',
  'RaceControlMessages',
  'LapCount',
  'TeamRadio'
];

class SignalRStreamClient {
  constructor(stateStore, options = {}) {
    this.stateStore = stateStore;
    this.options = options;
    this.ws = null;
    this.isConnected = false;
    this.isHandshakeComplete = false;
    this.watchdogTimer = null;
    this.reconnectTimer = null;
    this.listeners = new Set();
    this.lastMessageTime = null;
  }

  onUpdate(callback) {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);
  }

  notifyUpdate(type, data) {
    for (const cb of this.listeners) {
      try {
        cb(type, data);
      } catch (e) {
        console.error('[SignalRClient] Error in update listener:', e);
      }
    }
  }

  async connect() {
    this.stateStore.setConnectionStatus('CONNECTING');
    try {
      console.log('[SignalRClient] Initiating F1 negotiation sequence...');
      
      // Step 1: Pre-negotiate OPTIONS for AWSALBCORS session cookie
      let awsAlbCorsCookie = '';
      try {
        const preResp = await fetch(`${F1_HTTP_BASE}/negotiate`, {
          method: 'OPTIONS',
          headers: {
            'User-Agent': 'BestHTTP',
            'Origin': F1_ORIGIN
          }
        });
        const setCookie = preResp.headers.get('set-cookie') || '';
        const match = setCookie.match(/AWSALBCORS=([^;]+)/);
        if (match) {
          awsAlbCorsCookie = match[1];
        }
      } catch (optErr) {
        console.warn('[SignalRClient] Pre-flight OPTIONS notice:', optErr.message);
      }

      // Step 2: HTTP POST Negotiation
      const postHeaders = {
        'User-Agent': 'BestHTTP',
        'Origin': F1_ORIGIN,
        'Content-Type': 'text/plain'
      };
      if (awsAlbCorsCookie) {
        postHeaders['Cookie'] = `AWSALBCORS=${awsAlbCorsCookie}`;
      }

      const negResp = await fetch(`${F1_HTTP_BASE}/negotiate?negotiateVersion=1`, {
        method: 'POST',
        headers: postHeaders
      });

      if (!negResp.ok) {
        throw new Error(`Negotiation failed with HTTP ${negResp.status}`);
      }

      const negJson = await negResp.json();
      if (!negJson.connectionToken) {
        throw new Error('No connectionToken received in negotiation response');
      }

      const connectionToken = encodeURIComponent(negJson.connectionToken);
      const wsUrl = `${F1_WS_BASE}?id=${connectionToken}`;

      console.log('[SignalRClient] Negotiation successful. Connecting WebSocket to:', wsUrl.substring(0, 45) + '...');
      
      // Step 3: Establish WebSocket
      const wsHeaders = {
        'User-Agent': 'BestHTTP',
        'Origin': F1_ORIGIN
      };
      if (awsAlbCorsCookie) {
        wsHeaders['Cookie'] = `AWSALBCORS=${awsAlbCorsCookie}`;
      }

      // In Node 22+, global WebSocket is available
      const WsClass = globalThis.WebSocket;
      if (!WsClass) {
        throw new Error('Global WebSocket not available in this Node runtime');
      }

      this.ws = new WsClass(wsUrl, { headers: wsHeaders });
      this._bindSocketEvents();

    } catch (err) {
      console.error('[SignalRClient] Connection error:', err.message);
      this.stateStore.setConnectionStatus('ERROR');
      this._scheduleReconnect(5000);
    }
  }

  _bindSocketEvents() {
    this.isHandshakeComplete = false;

    this.ws.onopen = () => {
      console.log('[SignalRClient] WebSocket open. Sending protocol handshake...');
      this.ws.send(HANDSHAKE_PAYLOAD);
    };

    this.ws.onmessage = (event) => {
      this.lastMessageTime = new Date();
      this._resetWatchdog();

      const raw = typeof event.data === 'string' ? event.data : event.data.toString('utf-8');
      const messages = raw.split(RECORD_SEPARATOR);

      for (const msg of messages) {
        if (!msg || msg.trim().length === 0) continue;
        try {
          const frame = JSON.parse(msg);

          if (!this.isHandshakeComplete) {
            if (frame.error) {
              console.error('[SignalRClient] Handshake error:', frame.error);
              this.ws.close();
              return;
            }
            this.isHandshakeComplete = true;
            this.isConnected = true;
            this.stateStore.setConnectionStatus('CONNECTED');
            console.log('[SignalRClient] Protocol handshake accepted! Subscribing to F1 topics...');
            this._sendSubscriptions();
            continue;
          }

          // Handle Snapshot from subscription response
          if (frame.type === 3 && frame.result) {
            this._handleInitialSnapshot(frame.result);
            continue;
          }

          // Handle live stream delta invocation
          if (frame.type === 1 && frame.target === 'feed') {
            const args = frame.arguments;
            if (args && args.length >= 2) {
              const topic = args[0];
              const payload = args[1];
              this._routeTopicData(topic, payload);
            }
          }

        } catch (parseErr) {
          // Ignore incomplete chunk splits
        }
      }
    };

    this.ws.onclose = (event) => {
      console.warn(`[SignalRClient] WebSocket disconnected (Code: ${event.code})`);
      this.isConnected = false;
      this.isHandshakeComplete = false;
      this.stateStore.setConnectionStatus('DISCONNECTED');
      this._scheduleReconnect(5000);
    };

    this.ws.onerror = (err) => {
      console.error('[SignalRClient] WebSocket error event:', err.message || err);
    };
  }

  _sendSubscriptions() {
    if (!this.ws || this.ws.readyState !== 1) return;
    const invocation = {
      type: 1,
      invocationId: '0',
      target: 'Subscribe',
      arguments: [TOPICS]
    };
    this.ws.send(`${JSON.stringify(invocation)}${RECORD_SEPARATOR}`);
    console.log(`[SignalRClient] Subscribed to ${TOPICS.length} live channels.`);
  }

  _handleInitialSnapshot(snapshot) {
    if (!snapshot) return;
    console.log('[SignalRClient] Processing initial session snapshot...');
    for (const [key, val] of Object.entries(snapshot)) {
      this._routeTopicData(key, val);
    }
    
    const sess = this.stateStore.sessionInfo;
    const meetingName = sess && sess.Meeting ? sess.Meeting.Name : 'F1 Session';
    const sessType = sess ? (sess.Name || sess.Type || '') : '';
    const sessStatus = sess ? (sess.SessionStatus || 'Live') : 'Live';
    const driversCount = Object.keys(this.stateStore.driverList).length;

    console.log(`[SignalRClient] ✅ Snapshot processed successfully!`);
    console.log(`[SignalRClient] 🏁 Session: ${meetingName} - ${sessType} (Stato: ${sessStatus})`);
    console.log(`[SignalRClient] 🏎️  Piloti registrati: ${driversCount}`);
    console.log(`[SignalRClient] 🌐 Dashboard pronta e consultabile su http://localhost:3000`);
    if (sessStatus.toLowerCase() === 'finalised' || sessStatus.toLowerCase() === 'inactive') {
      console.log(`[SignalRClient] ℹ️  Nota: Nessuna vettura in pista al momento (sessione terminata). In attesa di nuovi eventi o avvio del prossimo GP.`);
    } else {
      console.log(`[SignalRClient] 📡 In ascolto dei flussi telemetrici live ad alta frequenza...`);
    }

    this.notifyUpdate('SNAPSHOT', this.stateStore.getFullSnapshot());
  }

  _routeTopicData(topic, payload) {
    if (!payload) return;

    try {
      if (topic === 'CarData.z') {
        const decoded = TelemetryDecoder.decodeCarData(payload);
        if (decoded.length > 0) {
          const latest = decoded[decoded.length - 1];
          for (const [driverNum, car] of Object.entries(latest.cars)) {
            this.stateStore.updateCarTelemetry(driverNum, car);
          }
          this.notifyUpdate('TELEMETRY', latest.cars);
        }
        return;
      }

      if (topic === 'Position.z') {
        const decoded = TelemetryDecoder.decodePosition(payload);
        if (decoded.length > 0) {
          const latest = decoded[decoded.length - 1];
          for (const [driverNum, pos] of Object.entries(latest.entries)) {
            this.stateStore.updateDriverPosition(driverNum, pos);
          }
          this.notifyUpdate('POSITIONS', latest.entries);
        }
        return;
      }

      // Delta JSON channels
      const data = typeof payload === 'string' ? JSON.parse(payload) : payload;

      switch (topic) {
        case 'DriverList':
          this.stateStore.updateDrivers(data);
          this.notifyUpdate('DRIVERS', data);
          break;
        case 'TimingData':
        case 'TimingDataF1':
          this.stateStore.updateTimingData(data);
          this.notifyUpdate('TIMING', this.stateStore.getLeaderboard());
          break;
        case 'TimingAppData':
          this.stateStore.updateTimingAppData(data);
          this.notifyUpdate('TYRES', data);
          break;
        case 'SessionInfo':
          this.stateStore.updateSessionInfo(data);
          this.notifyUpdate('SESSION', data);
          break;
        case 'WeatherData':
          this.stateStore.updateWeather(data);
          this.notifyUpdate('WEATHER', data);
          break;
        case 'LapCount':
          this.stateStore.updateLapCount(data);
          this.notifyUpdate('LAPS', data);
          break;
        case 'RaceControlMessages':
          if (data.Messages) {
            const msgs = Array.isArray(data.Messages) ? data.Messages : Object.values(data.Messages);
            this.stateStore.addRaceControlMessage(msgs);
            this.notifyUpdate('RACE_CONTROL', msgs);
          }
          break;
        default:
          break;
      }
    } catch (err) {
      console.error(`[SignalRClient] Error processing topic ${topic}:`, err.message);
    }
  }

  _resetWatchdog() {
    if (this.watchdogTimer) clearTimeout(this.watchdogTimer);
    // If no message for 25 seconds, reconnect
    this.watchdogTimer = setTimeout(() => {
      if (this.isConnected) {
        console.warn('[SignalRClient] Watchdog: No data received for 25s. Forcing reconnect...');
        try {
          if (this.ws) this.ws.close();
        } catch (e) {}
      }
    }, 25000);
  }

  _scheduleReconnect(delayMs) {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => {
      console.log('[SignalRClient] Attempting reconnection...');
      this.connect();
    }, delayMs);
  }

  disconnect() {
    if (this.watchdogTimer) clearTimeout(this.watchdogTimer);
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.ws) {
      try {
        this.ws.close();
      } catch (e) {}
      this.ws = null;
    }
    this.isConnected = false;
    this.stateStore.setConnectionStatus('STOPPED');
  }
}

module.exports = {
  SignalRStreamClient
};

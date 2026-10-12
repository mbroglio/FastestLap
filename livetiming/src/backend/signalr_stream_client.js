/**
 * FastestLap LiveTiming - F1 SignalR Core Client (Corretto)
 */

const WebSocket = require('ws');
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
  'TrackStatus'
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
    this.pingTimer = null;
    this.messageBuffer = ''; // Buffer per riassemblare chunk frammentati
    this.listeners = new Set();
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
        console.error('[SignalRClient] Errore listener update:', e);
      }
    }
  }

  async connect() {
    this.stateStore.setConnectionStatus('CONNECTING');
    this._cleanupTimers();

    try {
      console.log('[SignalRClient] Avvio negoziazione SignalR...');

      // 1. Negoziazione HTTP POST per ottenere il connectionToken e i cookie di Session Affinity
      const negResp = await fetch(`${F1_HTTP_BASE}/negotiate?negotiateVersion=1`, {
        method: 'POST',
        headers: {
          'User-Agent': 'BestHTTP',
          'Origin': F1_ORIGIN,
          'Content-Type': 'text/plain'
        }
      });

      if (!negResp.ok) {
        throw new Error(`Negoziazione fallita con codice HTTP ${negResp.status}`);
      }

      // Estrai cookie AWSALB / AWSALBCORS per mantenere la sessione sullo stesso server
      let cookieHeader = '';
      const rawCookies = negResp.headers.getSetCookie 
        ? negResp.headers.getSetCookie() 
        : [negResp.headers.get('set-cookie')];

      if (rawCookies && rawCookies.length > 0) {
        cookieHeader = rawCookies
          .filter(Boolean)
          .map(c => c.split(';')[0])
          .join('; ');
      }

      const negJson = await negResp.json();
      if (!negJson.connectionToken) {
        throw new Error('connectionToken mancante nella risposta di negoziazione');
      }

      const connectionToken = encodeURIComponent(negJson.connectionToken);
      const wsUrl = `${F1_WS_BASE}?id=${connectionToken}`;

      console.log('[SignalRClient] Negoziazione riuscita. Connessione a WebSocket...');

      // 2. Connessione WebSocket tramite libreria ws con headers corretti
      const wsOptions = {
        headers: {
          'User-Agent': 'BestHTTP',
          'Origin': F1_ORIGIN
        }
      };
      if (cookieHeader) {
        wsOptions.headers['Cookie'] = cookieHeader;
      }

      this.messageBuffer = '';
      this.ws = new WebSocket(wsUrl, wsOptions);
      this._bindSocketEvents();

    } catch (err) {
      console.error('[SignalRClient] Errore durante la connessione:', err.message);
      this.stateStore.setConnectionStatus('ERROR');
      this._scheduleReconnect(5000);
    }
  }

  _bindSocketEvents() {
    this.isHandshakeComplete = false;

    this.ws.on('open', () => {
      console.log('[SignalRClient] WebSocket aperto. Invio handshake SignalR...');
      this.ws.send(HANDSHAKE_PAYLOAD);
      this._startPingInterval();
    });

    this.ws.on('message', (data) => {
      this._resetWatchdog();

      const chunk = typeof data === 'string' ? data : data.toString('utf-8');
      this.messageBuffer += chunk;

      // De-frammentazione: dividi per il record separator 0x1e
      const messages = this.messageBuffer.split(RECORD_SEPARATOR);
      
      // L'ultimo elemento è incompleto o vuoto se il messaggio terminava per 0x1e
      this.messageBuffer = messages.pop();

      for (const msg of messages) {
        if (!msg || msg.trim().length === 0) continue;

        try {
          const frame = JSON.parse(msg);

          // Ping di ritorno dal server
          if (frame.type === 6) continue;

          // Handshake ACK
          if (!this.isHandshakeComplete) {
            if (frame.error) {
              console.error('[SignalRClient] Errore handshake:', frame.error);
              this.ws.close();
              return;
            }
            this.isHandshakeComplete = true;
            this.isConnected = true;
            this.stateStore.setConnectionStatus('CONNECTED');
            console.log('[SignalRClient] Handshake accettato. Invio sottoscrizioni ai canali...');
            this._sendSubscriptions();
            continue;
          }

          // Snapshot iniziale
          if (frame.type === 3 && frame.result) {
            this._handleInitialSnapshot(frame.result);
            continue;
          }

          // Eventi live streaming feed
          if (frame.type === 1 && frame.target === 'feed') {
            const args = frame.arguments;
            if (args && args.length >= 2) {
              this._routeTopicData(args[0], args[1]);
            }
          }

        } catch (parseErr) {
          console.warn('[SignalRClient] Errore parsing frame JSON:', parseErr.message);
        }
      }
    });

    this.ws.on('close', (code, reason) => {
      console.warn(`[SignalRClient] WebSocket disconnesso (Codice: ${code}, Motivo: ${reason || 'N/A'})`);
      this._cleanupConnection();
      this.stateStore.setConnectionStatus('DISCONNECTED');
      this._scheduleReconnect(5000);
    });

    this.ws.on('error', (err) => {
      console.error('[SignalRClient] Errore WebSocket:', err.message);
    });
  }

  _startPingInterval() {
    if (this.pingTimer) clearInterval(this.pingTimer);
    // Ping SignalR ogni 15 secondi per mantenere aperto il canale
    this.pingTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(`{"type":6}${RECORD_SEPARATOR}`);
      }
    }, 15000);
  }

  _sendSubscriptions() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    const invocation = {
      type: 1,
      invocationId: '0',
      target: 'Subscribe',
      arguments: [TOPICS]
    };
    this.ws.send(`${JSON.stringify(invocation)}${RECORD_SEPARATOR}`);
    console.log(`[SignalRClient] Sottoscrizione inviata per ${TOPICS.length} canali.`);
  }

  _handleInitialSnapshot(snapshot) {
    if (!snapshot) return;
    console.log('[SignalRClient] Elaborazione snapshot iniziale...');
    for (const [key, val] of Object.entries(snapshot)) {
      this._routeTopicData(key, val);
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
      console.error(`[SignalRClient] Errore elaborazione topic ${topic}:`, err.message);
    }
  }

  _resetWatchdog() {
    if (this.watchdogTimer) clearTimeout(this.watchdogTimer);
    // Timeout a 40s per evitare disconnessioni accidentali
    this.watchdogTimer = setTimeout(() => {
      if (this.isConnected) {
        console.warn('[SignalRClient] Watchdog: Nessun dato da 40s. Riconnessione...');
        if (this.ws) {
          try { this.ws.terminate(); } catch (e) {}
        }
      }
    }, 40000);
  }

  _scheduleReconnect(delayMs) {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = setTimeout(() => {
      console.log('[SignalRClient] Tentativo di riconnessione...');
      this.connect();
    }, delayMs);
  }

  _cleanupTimers() {
    if (this.watchdogTimer) clearTimeout(this.watchdogTimer);
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    if (this.pingTimer) clearInterval(this.pingTimer);
  }

  _cleanupConnection() {
    this._cleanupTimers();
    this.isConnected = false;
    this.isHandshakeComplete = false;
    this.messageBuffer = '';
    if (this.ws) {
      try { this.ws.removeAllListeners(); } catch (e) {}
      this.ws = null;
    }
  }

  disconnect() {
    this._cleanupConnection();
    this.stateStore.setConnectionStatus('STOPPED');
  }
}

module.exports = { SignalRStreamClient };
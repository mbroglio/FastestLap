/**
 * FastestLap LiveTiming - Direct Web SignalR Client
 * Connects directly from any browser to F1 official SignalR Core (wss://livetiming.formula1.com/signalrcore)
 * Enables pure standalone web usage with ZERO Node.js / npm commands required.
 */
class SignalRWebClient {
  constructor(stateStore, callbacks = {}) {
    this.stateStore = stateStore;
    this.callbacks = callbacks;
    this.ws = null;
    this.isConnected = false;
    this.isHandshakeComplete = false;
    this.pingTimer = null;
    this.reconnectTimer = null;
    this.messageBuffer = '';
    this.RECORD_SEPARATOR = '\x1e';
    this.TOPICS = [
      'Heartbeat', 'CarData.z', 'Position.z', 'TimingData', 'TimingDataF1',
      'TimingAppData', 'DriverList', 'SessionInfo', 'SessionStatus', 'SessionData',
      'WeatherData', 'RaceControlMessages', 'LapCount', 'TrackStatus'
    ];
  }

  connect() {
    this.disconnect();
    this._notifyStatus('connecting', 'Connessione a F1 SignalR Web...');

    const wsUrl = 'wss://livetiming.formula1.com/signalrcore';
    console.log('[F1WebSignalR] Connessione diretta browser a:', wsUrl);

    try {
      this.ws = new WebSocket(wsUrl);
    } catch (err) {
      console.warn('[F1WebSignalR] Creazione WebSocket fallita:', err);
      this._scheduleReconnect(5000);
      return;
    }

    this.ws.onopen = () => {
      console.log('[F1WebSignalR] WebSocket aperto. Invio handshake protocollo...');
      this.ws.send(JSON.stringify({ protocol: 'json', version: 1 }) + this.RECORD_SEPARATOR);
    };

    this.ws.onmessage = async (event) => {
      const raw = typeof event.data === 'string' ? event.data : event.data.toString();
      this.messageBuffer += raw;

      let sepIndex;
      while ((sepIndex = this.messageBuffer.indexOf(this.RECORD_SEPARATOR)) !== -1) {
        const message = this.messageBuffer.substring(0, sepIndex).trim();
        this.messageBuffer = this.messageBuffer.substring(sepIndex + 1);

        if (!message) continue;

        try {
          const frame = JSON.parse(message);

          // Risposta iniziale handshake: {}
          if (!this.isHandshakeComplete && Object.keys(frame).length === 0) {
            this.isHandshakeComplete = true;
            this.isConnected = true;
            console.log('[F1WebSignalR] Handshake accettato. Invio sottoscrizioni canali...');
            this._notifyStatus('live', '● F1 SIGNALR LIVE');
            this._sendSubscriptions();
            this._startPingInterval();
            continue;
          }

          // Frame di dati SignalR
          if (frame.type === 3 && frame.result) {
            // Snapshot iniziale
            await this._handleInitialSnapshot(frame.result);
          } else if (frame.type === 1 && frame.target === 'feed' && Array.isArray(frame.arguments)) {
            // Stream incrementale delta
            await this._handleFeedUpdate(frame.arguments[0], frame.arguments[1]);
          } else if (frame.type === 6) {
            // SignalR Keepalive Ping
          }
        } catch (parseErr) {
          console.warn('[F1WebSignalR] Errore parsing frame:', parseErr.message);
        }
      }
    };

    this.ws.onclose = (event) => {
      console.warn(`[F1WebSignalR] WebSocket disconnesso (Codice: ${event.code})`);
      this._cleanup();
      this._notifyStatus('disconnected', 'Disconnesso — Riconnessione in corso...');
      this._scheduleReconnect(5000);
    };

    this.ws.onerror = (err) => {
      console.error('[F1WebSignalR] Errore WebSocket:', err);
    };
  }

  _sendSubscriptions() {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    const sub = {
      type: 1,
      invocationId: '0',
      target: 'Subscribe',
      arguments: [this.TOPICS]
    };
    this.ws.send(JSON.stringify(sub) + this.RECORD_SEPARATOR);
  }

  _startPingInterval() {
    if (this.pingTimer) clearInterval(this.pingTimer);
    this.pingTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send(JSON.stringify({ type: 6 }) + this.RECORD_SEPARATOR);
      }
    }, 10000);
  }

  async _handleInitialSnapshot(snapshot) {
    if (!snapshot || typeof snapshot !== 'object') return;

    if (snapshot.DriverList) this.stateStore.updateDrivers(snapshot.DriverList);
    if (snapshot.SessionInfo) this.stateStore.updateSessionInfo(snapshot.SessionInfo);
    if (snapshot.SessionStatus) this.stateStore.updateSessionStatus(snapshot.SessionStatus);
    if (snapshot.SessionData) this.stateStore.updateSessionData(snapshot.SessionData);
    if (snapshot.TrackStatus) this.stateStore.updateTrackStatus(snapshot.TrackStatus);
    if (snapshot.WeatherData) this.stateStore.updateWeather(snapshot.WeatherData);
    if (snapshot.LapCount) this.stateStore.updateLapCount(snapshot.LapCount);
    if (snapshot.TimingData) this.stateStore.updateTimingData(snapshot.TimingData);
    if (snapshot.TimingAppData) this.stateStore.updateTimingAppData(snapshot.TimingAppData);

    if (snapshot['Position.z'] && window.TelemetryDecoder) {
      const decodedPos = await window.TelemetryDecoder.decodePositions(snapshot['Position.z']);
      if (decodedPos && decodedPos.length > 0) {
        const lastEntry = decodedPos[decodedPos.length - 1];
        for (const [drv, pos] of Object.entries(lastEntry.positions)) {
          this.stateStore.updateDriverPosition(drv, pos);
        }
      }
    }

    if (snapshot['CarData.z'] && window.TelemetryDecoder) {
      const decodedCar = await window.TelemetryDecoder.decodeCarData(snapshot['CarData.z']);
      if (decodedCar && decodedCar.length > 0) {
        const lastEntry = decodedCar[decodedCar.length - 1];
        for (const [drv, car] of Object.entries(lastEntry.cars)) {
          this.stateStore.updateDriverTelemetry(drv, car);
        }
      }
    }

    this._notifyUpdate('SNAPSHOT', this.stateStore.getFullSnapshot());
  }

  async _handleFeedUpdate(topic, data) {
    if (!topic || data === undefined) return;

    switch (topic) {
      case 'DriverList':
        this.stateStore.updateDrivers(data);
        break;
      case 'TimingData':
      case 'TimingDataF1':
        this.stateStore.updateTimingData(data);
        break;
      case 'TimingAppData':
        this.stateStore.updateTimingAppData(data);
        break;
      case 'SessionInfo':
        this.stateStore.updateSessionInfo(data);
        break;
      case 'SessionStatus':
        this.stateStore.updateSessionStatus(data);
        break;
      case 'SessionData':
        this.stateStore.updateSessionData(data);
        break;
      case 'TrackStatus':
        this.stateStore.updateTrackStatus(data);
        break;
      case 'WeatherData':
        this.stateStore.updateWeather(data);
        break;
      case 'LapCount':
        this.stateStore.updateLapCount(data);
        break;
      case 'RaceControlMessages':
        this.stateStore.addRaceControlMessage(data.Messages || data);
        break;
      case 'Position.z':
        if (typeof data === 'string' && window.TelemetryDecoder) {
          const decoded = await window.TelemetryDecoder.decodePositions(data);
          for (const item of decoded) {
            for (const [num, pos] of Object.entries(item.positions)) {
              this.stateStore.updateDriverPosition(num, pos);
            }
          }
        }
        break;
      case 'CarData.z':
        if (typeof data === 'string' && window.TelemetryDecoder) {
          const decoded = await window.TelemetryDecoder.decodeCarData(data);
          for (const item of decoded) {
            for (const [num, car] of Object.entries(item.cars)) {
              this.stateStore.updateDriverTelemetry(num, car);
            }
          }
        }
        break;
    }

    this._notifyUpdate('UPDATE', this.stateStore.getFullSnapshot());
  }

  _notifyStatus(status, label) {
    if (typeof this.callbacks.onStatusChange === 'function') {
      this.callbacks.onStatusChange(status, label);
    }
  }

  _notifyUpdate(type, snapshot) {
    if (typeof this.callbacks.onDataUpdate === 'function') {
      this.callbacks.onDataUpdate(type, snapshot);
    }
  }

  _scheduleReconnect(delayMs) {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, delayMs);
  }

  _cleanup() {
    this.isConnected = false;
    this.isHandshakeComplete = false;
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
      this.pingTimer = null;
    }
  }

  disconnect() {
    this._cleanup();
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      try { this.ws.close(); } catch (e) {}
      this.ws = null;
    }
  }
}

if (typeof window !== 'undefined') {
  window.SignalRWebClient = SignalRWebClient;
}

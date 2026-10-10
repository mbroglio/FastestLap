/**
 * FastestLap — signalr_client.js
 * Client Streaming Real-time SignalR Core per F1 Live Timing
 * Supporta:
 *  1. Negoziazione POST e WebSocket SignalR Core a wss://livetiming.formula1.com/signalrcore
 *  2. Decompressione nativa C++/Java raw DEFLATE tramite FastestLapBridge.inflateRaw()
 *  3. Ingestion automatica di Snapshot e feed live (TimingData, CarData.z, Position.z, TrackStatus)
 *  4. Fallback automatico su Server Locale Node.js SSE (localhost:3000 / 10.0.2.2:3000)
 */

class SignalRStreamClient {
  constructor() {
    this.ws = null;
    this.sse = null;
    this.mode = 'live'; // 'live' | 'simulation'
    this.isConnected = false;
    this.isHandshakeComplete = false;
    this.reconnectTimer = null;
    this.statusListeners = [];
    this.messageListeners = [];
    this.subscribedFeeds = [
      'Heartbeat',
      'CarData.z',
      'Position.z',
      'TimingData',
      'TimingAppData',
      'SessionInfo',
      'SessionStatus',
      'TrackStatus',
      'RaceControlMessages',
      'DriverList'
    ];

    window.onSignalRFeed = (feed, data) => this.processFeedData(feed, data);
    window.onSignalRStatus = (status, detail) => {
      this.mode = (status === 'live' ? 'live' : this.mode);
      this.isConnected = (status === 'live');
      this.notifyStatus(status, detail);
    };
  }

  onStatusChange(fn) {
    if (typeof fn === 'function') this.statusListeners.push(fn);
  }

  onMessage(fn) {
    if (typeof fn === 'function') this.messageListeners.push(fn);
  }

  notifyStatus(status, detail) {
    this.statusListeners.forEach(fn => {
      try { fn(status, detail); } catch (e) { console.error(e); }
    });
  }

  notifyMessage(feed, data) {
    this.messageListeners.forEach(fn => {
      try { fn(feed, data); } catch (e) { console.error(e); }
    });
  }

  setMode(mode) {
    this.mode = mode;
    if (mode === 'live') {
      this.connect();
    } else {
      this.disconnect();
      this.notifyStatus('simulation', 'Modalità Collaudo/Simulazione Attiva');
    }
  }

  async connect() {
    this.disconnect();
    this.notifyStatus('connecting', 'Connessione a SignalR F1 Live...');

    // 1. Android Bridge nativo ad alte prestazioni (OkHttp WebSocket + Sticky Cookie AWS ALB)
    if (window.FastestLapBridge && typeof window.FastestLapBridge.startSignalRStream === 'function') {
      console.log('[SignalR] Connessione delegata a FastestLapBridge nativo OkHttp...');
      window.FastestLapBridge.startSignalRStream();
      if (typeof window.FastestLapBridge.isSignalRConnected === 'function' && window.FastestLapBridge.isSignalRConnected()) {
        this.notifyStatus('live', '● SIGNALR LIVE');
      }
      if (typeof window.FastestLapBridge.requestInitialFeeds === 'function') {
        window.FastestLapBridge.requestInitialFeeds();
      }
      return;
    }

    // 2. Tenta connessione proxy locale se attivo
    const localConnected = await this.tryConnectLocalServer();
    if (localConnected) return;

    // 3. Connessione diretta SignalR Core a F1 (per browser desktop)
    try {
      await this.negotiateAndConnectSignalR();
    } catch (err) {
      console.warn('[SignalR] Connessione live SignalR non riuscita:', err.message);
      this.notifyStatus('simulation', 'In attesa flusso live — Collaudo attivo');
      this.mode = 'simulation';
    }
  }

  async tryConnectLocalServer() {
    const endpoints = [
      'http://10.0.2.2:3000/api/stream', // Emulatore Android
      'http://localhost:3000/api/stream' // Browser desktop
    ];

    for (const url of endpoints) {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 600);
        const check = await fetch(url.replace('/stream', '/snapshot'), { signal: controller.signal });
        clearTimeout(timeoutId);

        if (check.ok) {
          console.log('[SignalR] Connesso a server locale:', url);
          this.connectSSE(url);
          return true;
        }
      } catch (ignored) {}
    }
    return false;
  }

  connectSSE(url) {
    if (this.sse) this.sse.close();
    try {
      this.sse = new EventSource(url);
      this.sse.onopen = () => {
        this.isConnected = true;
        this.notifyStatus('live', '● FLUSSO LOCALE CONNESSO');
      };
      this.sse.addEventListener('INIT', e => this.handleLocalSnapshot(JSON.parse(e.data)));
      this.sse.addEventListener('TICK', e => this.handleLocalTick(JSON.parse(e.data)));
      this.sse.addEventListener('POSITIONS', e => this.handlePositionsPayload(JSON.parse(e.data)));
      this.sse.addEventListener('TELEMETRY', e => this.handleTelemetryPayload(JSON.parse(e.data)));
      this.sse.addEventListener('TIMING', e => this.handleTimingData(JSON.parse(e.data)));
      this.sse.addEventListener('RACE_CONTROL', e => this.notifyMessage('RaceControlMessages', JSON.parse(e.data)));
      this.sse.onerror = () => {
        this.disconnect();
        this.notifyStatus('simulation', 'Server locale disconnesso — Fallback');
      };
    } catch (e) {
      console.error(e);
    }
  }

  async negotiateAndConnectSignalR() {
    const RECORD_SEP = String.fromCharCode(0x1e);
    const negotiateUrl = 'https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1';

    // F1 SignalR Core richiede HTTP POST per la negoziazione
    const resp = await fetch(negotiateUrl, {
      method: 'POST',
      headers: {
        'User-Agent': 'BestHTTP',
        'Accept': 'application/json'
      }
    });

    if (!resp.ok) {
      throw new Error('Negoziazione SignalR fallita: HTTP ' + resp.status);
    }

    const data = await resp.json();
    if (!data.connectionToken) {
      throw new Error('Nessun connectionToken ricevuto nella negoziazione');
    }

    const token = encodeURIComponent(data.connectionToken);
    const wsUrl = `wss://livetiming.formula1.com/signalrcore?id=${token}`;

    console.log('[SignalR] Negoziazione riuscita, connessione WebSocket...');
    this.ws = new WebSocket(wsUrl);
    this.isHandshakeComplete = false;

    this.ws.onopen = () => {
      console.log('[SignalR] WebSocket aperto. Invio handshake protocollo...');
      this.ws.send(JSON.stringify({ protocol: 'json', version: 1 }) + RECORD_SEP);
    };

    this.ws.onmessage = (event) => {
      const raw = typeof event.data === 'string' ? event.data : event.data.toString();
      const messages = raw.split(RECORD_SEP);

      for (const msg of messages) {
        if (!msg || msg.trim().length === 0) continue;
        try {
          const frame = JSON.parse(msg);

          if (!this.isHandshakeComplete) {
            if (frame.error) {
              console.error('[SignalR] Errore handshake:', frame.error);
              this.ws.close();
              return;
            }
            this.isHandshakeComplete = true;
            this.isConnected = true;
            this.notifyStatus('live', '● SIGNALR LIVE');

            // Sottoscrizione ai canali live F1
            const subMsg = JSON.stringify({
              type: 1,
              invocationId: '0',
              target: 'Subscribe',
              arguments: [this.subscribedFeeds]
            }) + RECORD_SEP;
            this.ws.send(subMsg);
            console.log('[SignalR] Sottoscrizione inviata per canali live');
            continue;
          }

          // Snapshot iniziale restituito dalla sottoscrizione (type 3)
          if (frame.type === 3 && frame.result) {
            console.log('[SignalR] Snapshot iniziale ricevuto!');
            this.handleInitialSnapshot(frame.result);
            continue;
          }

          // Invocazione delta live stream (type 1)
          if (frame.type === 1 && frame.target === 'feed') {
            const args = frame.arguments;
            if (args && args.length >= 2) {
              this.processFeedData(args[0], args[1]);
            }
          }

          // Ping da server (type 6) -> Rispondi con ping
          if (frame.type === 6) {
            this.ws.send(JSON.stringify({ type: 6 }) + RECORD_SEP);
          }
        } catch (e) {}
      }
    };

    this.ws.onerror = (err) => {
      console.warn('[SignalR WS Error]', err);
    };

    this.ws.onclose = () => {
      this.isConnected = false;
      this.isHandshakeComplete = false;
      if (this.mode === 'live') {
        this.notifyStatus('connecting', 'Riconnessione SignalR...');
        this.reconnectTimer = setTimeout(() => this.connect(), 4000);
      }
    };
  }

  handleInitialSnapshot(snapshot) {
    if (!snapshot || typeof snapshot !== 'object') return;
    for (const [topic, payload] of Object.entries(snapshot)) {
      this.processFeedData(topic, payload);
    }
  }

  processFeedData(feedName, payload) {
    let data = payload;
    if (typeof payload === 'string' && (payload.startsWith('{') || payload.startsWith('['))) {
      try {
        data = JSON.parse(payload);
      } catch (e) {
        data = payload;
      }
    }

    // 1. Decompressione z-feed tramite Bridge Java nativo Inflater o payload nativo già decompresso
    if (feedName === 'Position.z' || feedName === 'CarData.z') {
      let parsed = (typeof data === 'object' && data !== null) ? data : null;
      if (!parsed && typeof data === 'string') {
        let decompressed = data;
        if (!data.startsWith('{') && !data.startsWith('[')) {
          if (window.FastestLapBridge && typeof window.FastestLapBridge.inflateRaw === 'function') {
            decompressed = window.FastestLapBridge.inflateRaw(data);
          }
        }
        if (decompressed && decompressed !== '{}') {
          try {
            parsed = JSON.parse(decompressed);
          } catch (e) {
            console.error('[SignalR] Errore parsing decompresso:', e);
          }
        }
      }
      if (parsed) {
        if (feedName === 'Position.z') {
          this.handleDecompressedPositions(parsed);
        } else {
          this.handleDecompressedCarData(parsed);
        }
      }
      return;
    }

    // 2. Classifica Live & Dati di Sessione
    if (feedName === 'TimingData' || feedName === 'TimingAppData') {
      this.handleTimingData(data);
    } else if (feedName === 'SessionInfo') {
      this.handleSessionInfo(data);
      this.notifyMessage(feedName, data);
    } else if (feedName === 'TrackStatus') {
      this.handleTrackStatus(data);
    } else if (feedName === 'DriverList') {
      this.handleDriverList(data);
      this.notifyMessage(feedName, data);
    } else if (feedName === 'RaceControlMessages') {
      this.handleRaceControlMessages(data);
      this.notifyMessage(feedName, data);
    } else if (feedName === 'SessionStatus') {
      this.notifyMessage(feedName, data);
    }
  }

  handleSessionInfo(info) {
    if (!info || typeof info !== 'object') return;
    const sType = info.Type || info.Name || '';
    const sName = info.Name || '';
    const gpName = (info.Meeting && (info.Meeting.Name || info.Meeting.OfficialName)) ? info.Meeting.Name : '';

    if (gpName) {
      const hdrEvent = document.getElementById('hdrEventTitle');
      if (hdrEvent) hdrEvent.textContent = gpName.toUpperCase();
    }

    if (sType) {
      sessionType = (typeof normalizeSessionType === 'function')
        ? normalizeSessionType(sType, sName)
        : sType.toLowerCase();
      if (sName.includes('3') || sName.includes('Q3')) sessionPart = 'Q3';
      else if (sName.includes('2') || sName.includes('Q2')) sessionPart = 'Q2';
      else if (sName.includes('1') || sName.includes('Q1')) sessionPart = 'Q1';

      if (typeof renderTableHeaders === 'function') {
        renderTableHeaders(sessionType);
      }
    }

    const sessVal = document.getElementById('statusBarSessionVal');
    if (sessVal) {
      let displayName = sName.toUpperCase();
      if (displayName.includes('PRACTICE 1') || displayName === 'FP1') displayName = 'PROVE LIBERE 1';
      else if (displayName.includes('PRACTICE 2') || displayName === 'FP2') displayName = 'PROVE LIBERE 2';
      else if (displayName.includes('PRACTICE 3') || displayName === 'FP3') displayName = 'PROVE LIBERE 3';
      else if (displayName.includes('QUALIFYING') || displayName === 'QUALIFY') displayName = 'QUALIFICHE';
      else if (displayName.includes('SPRINT QUALIFYING')) displayName = 'QUALIFICA SPRINT';
      else if (displayName.includes('SPRINT')) displayName = 'GARA SPRINT';
      else if (displayName.includes('RACE')) displayName = 'GARA';
      sessVal.textContent = displayName;
    }

    if (typeof updateSessionStatusBar === 'function') updateSessionStatusBar();
    if (typeof updateTimingTables === 'function') updateTimingTables();
  }

  handleTrackStatus(data) {
    if (!data) return;
    const status = String(data.Status || '');
    window.CURRENT_TRACK_STATUS = status;
    const flagEl = document.getElementById('statusBarFlag');
    const flagIcon = document.getElementById('statusBarFlagIcon');
    const flagText = document.getElementById('statusBarFlagText');
    if (!flagEl || !flagText) return;

    flagEl.className = 'status-bar-flag-badge';
    if (status === '1') {
      flagEl.classList.add('flag-green');
      if (flagIcon) flagIcon.textContent = '🟢';
      flagText.textContent = 'BANDIERA VERDE';
    } else if (status === '2') {
      flagEl.classList.add('flag-sc');
      if (flagIcon) flagIcon.textContent = '🟡';
      flagText.textContent = 'BANDIERA GIALLA';
    } else if (status === '4') {
      flagEl.classList.add('flag-sc');
      if (flagIcon) flagIcon.textContent = '🟡';
      flagText.textContent = 'SAFETY CAR';
    } else if (status === '5') {
      flagEl.classList.add('flag-vsc');
      if (flagIcon) flagIcon.textContent = '🔴';
      flagText.textContent = 'BANDIERA ROSSA (SESSIONE SOSPESA / RIMANDATA)';
    } else if (status === '6') {
      flagEl.classList.add('flag-vsc');
      if (flagIcon) flagIcon.textContent = '🟡';
      flagText.textContent = 'VIRTUAL SAFETY CAR';
    }
  }

  handleDriverList(payload) {
    if (!payload) return;
    let driversObj = payload;
    if (typeof payload === 'string') {
      try {
        driversObj = JSON.parse(payload);
      } catch (e) {
        return;
      }
    }
    const entries = driversObj.Lines || driversObj;
    if (!entries || typeof entries !== 'object') return;

    let updated = false;
    for (const [key, info] of Object.entries(entries)) {
      if (!info || typeof info !== 'object') continue;
      const num = String(info.RacingNumber || key);
      const code = (info.Tla || info.code || '').toUpperCase();
      const first = info.FirstName || info.firstName || '';
      const last = info.LastName || info.lastName || info.BroadcastName || '';
      const team = info.TeamName || info.team || '';
      let color = info.TeamColour || info.color || '#e10600';
      if (color && !color.startsWith('#')) color = '#' + color;

      if (num && num !== 'undefined') {
        const existing = DRIVERS[num] || {};
        DRIVERS[num] = {
          code: code || existing.code || ('#' + num),
          number: parseInt(num, 10) || num,
          firstName: first || existing.firstName || '',
          lastName: last || existing.lastName || ('Pilota ' + num),
          team: team || existing.team || 'Team',
          color: color
        };
        updated = true;
      }
    }

    if (updated) {
      GRID_ORDER = Object.keys(DRIVERS).sort((a, b) => {
        const posA = DRIVER_TELEMETRY[a]?.position || (parseInt(a, 10) || 99);
        const posB = DRIVER_TELEMETRY[b]?.position || (parseInt(b, 10) || 99);
        return posA - posB;
      });
      if (typeof populateDriverDropdowns === 'function') populateDriverDropdowns();
      if (typeof updateTimingTables === 'function') updateTimingTables();
      if (typeof drawTrack === 'function') drawTrack();
    }
  }

  handleRaceControlMessages(payload) {
    if (!payload) return;
    if (typeof window.updateRaceControlMessages === 'function') {
      window.updateRaceControlMessages(payload);
    }
  }

  handleTimingData(data) {
    if (!data || typeof data !== 'object') return;
    const lines = data.Lines || data;
    if (!lines || typeof lines !== 'object') return;

    for (const [drvNum, line] of Object.entries(lines)) {
      if (!line) continue;
      if (!DRIVER_TELEMETRY[drvNum]) DRIVER_TELEMETRY[drvNum] = {};

      if (line.Position) {
        const p = parseInt(line.Position, 10);
        if (!isNaN(p)) DRIVER_TELEMETRY[drvNum].position = p;
      }
      if (line.GapToLeader) DRIVER_TELEMETRY[drvNum].gapLeader = line.GapToLeader;
      if (line.IntervalToPositionAhead && line.IntervalToPositionAhead.Value) {
        DRIVER_TELEMETRY[drvNum].interval = line.IntervalToPositionAhead.Value;
      }
      if (line.InPit !== undefined) DRIVER_TELEMETRY[drvNum].inPit = Boolean(line.InPit);
      if (line.PitOut !== undefined && line.PitOut) DRIVER_TELEMETRY[drvNum].inPit = false;
      if (line.Retired !== undefined) DRIVER_TELEMETRY[drvNum].isRetired = Boolean(line.Retired);
      if (line.NumberOfLaps !== undefined) DRIVER_TELEMETRY[drvNum].pitCount = line.NumberOfLaps;
      if (line.KnockedOut !== undefined) DRIVER_TELEMETRY[drvNum].isKnockedOut = Boolean(line.KnockedOut);

      // BestLapTimes per Qualifying (Q1, Q2, Q3)
      if (Array.isArray(line.BestLapTimes)) {
        if (line.BestLapTimes[0] && line.BestLapTimes[0].Value) {
          DRIVER_TELEMETRY[drvNum].q1Best = this.parseLapTimeToSec(line.BestLapTimes[0].Value);
        }
        if (line.BestLapTimes[1] && line.BestLapTimes[1].Value) {
          DRIVER_TELEMETRY[drvNum].q2Best = this.parseLapTimeToSec(line.BestLapTimes[1].Value);
        }
        if (line.BestLapTimes[2] && line.BestLapTimes[2].Value) {
          DRIVER_TELEMETRY[drvNum].q3Best = this.parseLapTimeToSec(line.BestLapTimes[2].Value);
        }
      }

      // BestLapTime globale
      if (line.BestLapTime && line.BestLapTime.Value) {
        const timeStr = line.BestLapTime.Value;
        const durSec = this.parseLapTimeToSec(timeStr);
        if (durSec > 0) {
          DRIVER_TELEMETRY[drvNum].overallBest = durSec;
          if (!DRIVER_TELEMETRY[drvNum].q1Best) DRIVER_TELEMETRY[drvNum].q1Best = durSec;
          if (!DRIVER_LAPS[drvNum]) DRIVER_LAPS[drvNum] = [];
          if (DRIVER_LAPS[drvNum].length === 0) {
            DRIVER_LAPS[drvNum].push({
              lap: 1,
              startSec: 0,
              dur: durSec,
              s1: durSec * 0.32,
              s2: durSec * 0.39,
              s3: durSec * 0.29
            });
          } else {
            DRIVER_LAPS[drvNum][0].dur = durSec;
          }
        }
      }

      if (line.LastLapTime && line.LastLapTime.Value) {
        DRIVER_TELEMETRY[drvNum].lastLapTimeStr = line.LastLapTime.Value;
      }
    }

    if (typeof updateTimingTables === 'function') {
      updateTimingTables();
    }
  }

  parseLapTimeToSec(str) {
    if (!str || typeof str !== 'string' || !str.includes('.')) return 0;
    const parts = str.split(':');
    if (parts.length === 2) {
      return (parseFloat(parts[0]) * 60) + parseFloat(parts[1]);
    }
    return parseFloat(str) || 0;
  }

  handleDecompressedPositions(data) {
    const positionsList = Array.isArray(data) ? data : (data.Position || [data]);
    positionsList.forEach(entry => {
      const entries = entry.Entries || entry;
      if (entries && typeof entries === 'object') {
        for (const [drvNum, pos] of Object.entries(entries)) {
          if (pos && pos.X !== undefined && pos.Y !== undefined) {
            if (!window.LIVE_DRIVER_POSITIONS) window.LIVE_DRIVER_POSITIONS = {};
            const cur = window.LIVE_DRIVER_POSITIONS[drvNum] || { curX: pos.X, curY: pos.Y };
            window.LIVE_DRIVER_POSITIONS[drvNum] = {
              x: pos.X,
              y: pos.Y,
              z: pos.Z || 0,
              status: pos.Status || 'OnTrack',
              curX: cur.curX,
              curY: cur.curY
            };
          }
        }
      }
    });
  }

  handleDecompressedCarData(data) {
    const entriesList = Array.isArray(data) ? data : (data.Entries || [data]);
    entriesList.forEach(item => {
      const cars = item.Cars || item;
      if (cars && typeof cars === 'object') {
        for (const [drvNum, carInfo] of Object.entries(cars)) {
          const ch = carInfo.Channels || carInfo;
          if (ch) {
            const speed = ch['2'] !== undefined ? ch['2'] : ch.Speed;
            const gear = ch['3'] !== undefined ? ch['3'] : ch.Gear;
            const throttle = ch['4'] !== undefined ? ch['4'] : ch.Throttle;
            const brake = ch['5'] !== undefined ? ch['5'] : ch.Brake;
            const rpm = ch['0'] !== undefined ? ch['0'] : ch.Rpm;
            const drs = ch['45'] !== undefined ? ch['45'] : ch.Drs;

            if (typeof window.updateDriverTelemetry === 'function') {
              window.updateDriverTelemetry({
                driverNumber: drvNum,
                speed: speed,
                gear: gear,
                throttle: throttle,
                brake: brake,
                rpm: rpm,
                drs: (drs === 10 || drs === 12 || drs === 14 || drs === 1) ? 1 : 0
              });
            }
          }
        }
      }
    });
  }

  handlePositionsPayload(positions) {
    if (!positions || typeof positions !== 'object') return;
    if (!window.LIVE_DRIVER_POSITIONS) window.LIVE_DRIVER_POSITIONS = {};
    for (const [num, pos] of Object.entries(positions)) {
      if (pos && pos.x !== undefined && pos.y !== undefined) {
        const cur = window.LIVE_DRIVER_POSITIONS[num] || { curX: pos.x, curY: pos.y };
        window.LIVE_DRIVER_POSITIONS[num] = {
          x: pos.x,
          y: pos.y,
          status: pos.status || 'OnTrack',
          curX: cur.curX,
          curY: cur.curY
        };
      }
    }
  }

  handleTelemetryPayload(telemetry) {
    if (typeof window.updateDriverTelemetry === 'function') {
      window.updateDriverTelemetry(telemetry);
    }
  }

  handleLocalSnapshot(snapshot) {
    if (!snapshot) return;
    if (snapshot.positions) this.handlePositionsPayload(snapshot.positions);
    if (snapshot.telemetry) this.handleTelemetryPayload(snapshot.telemetry);
    if (snapshot.leaderboard) this.handleTimingData(snapshot.leaderboard);
    if (snapshot.raceControl) this.notifyMessage('RaceControlMessages', snapshot.raceControl);
  }

  disconnect() {
    if (window.FastestLapBridge && typeof window.FastestLapBridge.stopSignalRStream === 'function') {
      try { window.FastestLapBridge.stopSignalRStream(); } catch (e) {}
    }
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      try { this.ws.close(); } catch (e) {}
      this.ws = null;
    }
    if (this.sse) {
      try { this.sse.close(); } catch (e) {}
      this.sse = null;
    }
    this.isConnected = false;
    this.isHandshakeComplete = false;
  }
}

window.SignalRStreamClient = SignalRStreamClient;

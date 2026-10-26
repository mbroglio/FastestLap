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
      'DriverList', 'TimingDataF1', 'SessionData', 'WeatherData', 'LapCount'
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
    this.mode = 'live';
    this.connect();
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
      this.notifyStatus('connecting', 'In attesa del flusso SignalR live...');
      this.mode = 'live';
      if (!this.reconnectTimer) {
        this.reconnectTimer = setTimeout(() => {
          this.reconnectTimer = null;
          this.connect();
        }, 5000);
      }
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
      this.sse.addEventListener('TYRES', e => this.handleTimingAppData(JSON.parse(e.data)));
      this.sse.addEventListener('RACE_CONTROL', e => this.notifyMessage('RaceControlMessages', JSON.parse(e.data)));
      this.sse.onerror = () => {
        this.disconnect();
        this.notifyStatus('connecting', 'Server locale disconnesso — Riconnessione in corso...');
        this.mode = 'live';
        if (!this.reconnectTimer) {
          this.reconnectTimer = setTimeout(() => {
            this.reconnectTimer = null;
            this.connect();
          }, 4000);
        }
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
    if (feedName === 'TimingData' || feedName === 'TimingDataF1' || feedName === 'TimingAppData' || feedName === 'SessionData') {
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
    } else if (feedName === 'LapCount') {
      if (data) {
        if (data.CurrentLap !== undefined) window.CURRENT_SESSION_LAP = Number(data.CurrentLap);
        if (Number(data.TotalLaps) > 0) TOTAL_LAPS = Number(data.TotalLaps);
      }
      this.notifyMessage(feedName, data);
    } else if (feedName === 'SessionStatus') {
      this.handleSessionStatus(data);
      this.notifyMessage(feedName, data);
    }
  }

  resetQualifyingLiveTimes() {
    for (const k of Object.keys(DRIVER_TELEMETRY)) {
      if (DRIVER_TELEMETRY[k]) {
        DRIVER_TELEMETRY[k].overallBest = null;
        DRIVER_TELEMETRY[k].gapLeader = '-';
        DRIVER_TELEMETRY[k].interval = '-';
      }
      if (typeof DRIVER_LAPS !== 'undefined' && DRIVER_LAPS[k]) {
        DRIVER_LAPS[k] = [];
      }
    }
  }

  handleSessionStatus(data) {
    if (!data || typeof data !== 'object') return;
    const st = String(data.Status || '').toUpperCase();
    const started = String(data.Started || '').toUpperCase();
    window.CURRENT_SESSION_STATUS = st;
    if (st === 'STARTED') {
      window.QUALI_PART_ENDED = false;
    } else if (st === 'INACTIVE' || st === 'FINALISED' || st === 'ENDS' || st === 'ABORTED' || started === 'FINISHED') {
      window.QUALI_PART_ENDED = true;
      if ((typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType) === 'qualifying') { window.QUALI_PART_ENDED = true; this.resetQualifyingLiveTimes(); }
      if (typeof updateTimingTables === 'function') updateTimingTables();
      if (typeof updateSessionStatusBar === 'function') updateSessionStatusBar();
      if (typeof checkSessionTermination === 'function') checkSessionTermination();
    }
  }

  handleSessionInfo(info) {
    if (!info || typeof info !== 'object') return;
    if (info.ArchiveStatus && (info.ArchiveStatus.Status === 'Generating' || info.ArchiveStatus.Status === 'Complete')) {
      if (typeof checkSessionTermination === 'function') checkSessionTermination();
    }
    if (info.SessionStatus && ['FINALISED', 'FINISHED', 'ENDS', 'INACTIVE', 'ABORTED'].includes(String(info.SessionStatus).toUpperCase())) {
      window.CURRENT_SESSION_STATUS = String(info.SessionStatus).toUpperCase();
      if (typeof checkSessionTermination === 'function') checkSessionTermination();
    }
    const sType = info.Type || info.Name || '';
    const sName = info.Name || '';
    const gpName = (info.Meeting && (info.Meeting.Name || info.Meeting.OfficialName)) ? info.Meeting.Name : '';

    if (gpName) {
      const hdrEvent = document.getElementById('hdrEventTitle');
      if (hdrEvent) hdrEvent.textContent = gpName.toUpperCase();
    }

    if (info.Meeting && info.Meeting.Circuit && info.Meeting.Circuit.Key && typeof loadOfficialCircuitNodes === 'function') {
      loadOfficialCircuitNodes(info.Meeting.Circuit.Key);
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

  
  handleTimingAppData(data) {
    if (!data || typeof data !== 'object') return;
    const lines = data.Lines || data;
    if (!lines || typeof lines !== 'object') return;

    for (const [key, appLine] of Object.entries(lines)) {
      if (!appLine) continue;
      const drvNum = String(appLine.racingNumber || appLine.RacingNumber || key);
      const stintsRaw = appLine.Stints;
      if (!stintsRaw) continue;

      let currentStint = null;
      if (Array.isArray(stintsRaw)) {
        for (let i = stintsRaw.length - 1; i >= 0; i--) {
          const s = stintsRaw[i];
          if (s && (s.Compound || s.compound)) {
            currentStint = s;
            break;
          }
        }
        if (!currentStint && stintsRaw.length > 0) currentStint = stintsRaw[stintsRaw.length - 1];
      } else if (typeof stintsRaw === 'object') {
        const keys = Object.keys(stintsRaw).sort((a, b) => Number(a) - Number(b));
        for (let i = keys.length - 1; i >= 0; i--) {
          const s = stintsRaw[keys[i]];
          if (s && (s.Compound || s.compound)) {
            currentStint = s;
            break;
          }
        }
        if (!currentStint && keys.length > 0) currentStint = stintsRaw[keys[keys.length - 1]];
      }

      if (currentStint) {
        const rawComp = (currentStint.Compound || currentStint.compound || '').toUpperCase();
        let shortCode = 'MED';
        if (rawComp.includes('SOFT')) shortCode = 'SFT';
        else if (rawComp.includes('HARD')) shortCode = 'HRD';
        else if (rawComp.includes('INTER')) shortCode = 'INT';
        else if (rawComp.includes('WET')) shortCode = 'WET';

        const totalLaps = Number(currentStint.TotalLaps) || 0;
        const startLaps = Number(currentStint.StartLaps) || 0;
        const stintLapNum = Number(currentStint.LapNumber) || 0;

        if (!DRIVER_TELEMETRY[drvNum]) DRIVER_TELEMETRY[drvNum] = {};
        DRIVER_TELEMETRY[drvNum].tyreTotalLaps = totalLaps;
        DRIVER_TELEMETRY[drvNum].tyreStartLaps = startLaps;
        DRIVER_TELEMETRY[drvNum].tyreStintLapNumber = stintLapNum;
        DRIVER_TELEMETRY[drvNum].tyreCompound = rawComp || 'SOFT';

        DRIVER_STINTS[drvNum] = [{
          compound: rawComp || 'SOFT',
          code: shortCode,
          age: totalLaps,
          totalLaps: totalLaps,
          startLaps: startLaps,
          lapStart: stintLapNum || 1,
          new: currentStint.New === true || currentStint.New === 'true'
        }];
      }
    }

    if (typeof updateTimingTables === 'function') {
      updateTimingTables();
    }
  }

  handleTimingData(data) {
    if (!data || typeof data !== 'object') return;
    if (data.SessionPart !== undefined) {
      const nextPart = Number(data.SessionPart);
      if (this.currentSessionPart !== undefined && !isNaN(nextPart) && nextPart !== this.currentSessionPart) {
        if ((typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType) === 'qualifying') { window.QUALI_PART_ENDED = true; this.resetQualifyingLiveTimes(); }
      }
      if (!isNaN(nextPart) && nextPart > 0) {
        this.currentSessionPart = nextPart;
        const isSq = typeof sessionPart === 'string' && sessionPart.toUpperCase().startsWith('SQ');
        sessionPart = (isSq ? 'SQ' : 'Q') + nextPart;
      }
    }
    const partIdx = (this.currentSessionPart && this.currentSessionPart > 0) ? (this.currentSessionPart - 1) : -1;
    const lines = data.Lines || data;
    if (!lines || typeof lines !== 'object') return;

    for (const [drvKey, line] of Object.entries(lines)) {
      if (!line || typeof line !== 'object') continue;
      const drvNum = String(line.racingNumber || line.RacingNumber || drvKey);
      if (!DRIVER_TELEMETRY[drvNum]) DRIVER_TELEMETRY[drvNum] = {};

      const posRaw = line.Position !== undefined ? line.Position : line.position;
      if (posRaw !== undefined) {
        const p = parseInt(posRaw, 10);
        if (!isNaN(p)) DRIVER_TELEMETRY[drvNum].position = p;
      }

      // Tyre compound from local server leaderboard if present
      if (line.tyreCompound && line.tyreCompound !== 'UNKNOWN') {
        const cmp = String(line.tyreCompound).toUpperCase();
        let shortCode = 'MED';
        if (cmp.includes('SOFT')) shortCode = 'SFT';
        else if (cmp.includes('HARD')) shortCode = 'HRD';
        else if (cmp.includes('INTER')) shortCode = 'INT';
        else if (cmp.includes('WET')) shortCode = 'WET';
        const ageVal = Number(line.tyreAge) || 0;
        DRIVER_TELEMETRY[drvNum].tyreAge = ageVal;
        DRIVER_TELEMETRY[drvNum].tyreCompound = cmp;
        DRIVER_TELEMETRY[drvNum].tyreNew = Boolean(line.tyreNew);
        DRIVER_STINTS[drvNum] = [{
          compound: cmp,
          code: shortCode,
          age: ageVal,
          totalLaps: ageVal,
          lapStart: 1,
          new: Boolean(line.tyreNew)
        }];
      }

      if (line.KnockedOut !== undefined) DRIVER_TELEMETRY[drvNum].isKnockedOut = Boolean(line.KnockedOut);
      else if (line.knockedOut !== undefined) DRIVER_TELEMETRY[drvNum].isKnockedOut = Boolean(line.knockedOut);

      // Raw Gap & Interval if provided
      let rawGap = line.GapToLeader !== undefined ? line.GapToLeader : line.gapToLeader;
      if (rawGap && typeof rawGap === 'object') rawGap = rawGap.Value;
      if (!rawGap && line.TimeDiffToFastest) {
        rawGap = typeof line.TimeDiffToFastest === 'object' ? line.TimeDiffToFastest.Value : line.TimeDiffToFastest;
      }
      if (!rawGap && line.Stats) {
        if (partIdx >= 0) {
          const stPart = Array.isArray(line.Stats) ? line.Stats[partIdx] : line.Stats[String(partIdx)];
          if (stPart && stPart.TimeDiffToFastest) rawGap = stPart.TimeDiffToFastest;
        } else {
          const statsList = Array.isArray(line.Stats) ? line.Stats : Object.values(line.Stats);
          for (let i = statsList.length - 1; i >= 0; i--) {
            if (statsList[i] && statsList[i].TimeDiffToFastest) {
              rawGap = statsList[i].TimeDiffToFastest;
              break;
            }
          }
        }
      }
      if (rawGap !== undefined) {
        DRIVER_TELEMETRY[drvNum].gapLeader = (rawGap && rawGap !== '--' && rawGap !== '-') ? String(rawGap) : '-';
      }

      let rawInt = line.IntervalToPositionAhead !== undefined ? line.IntervalToPositionAhead : line.intervalToAhead;
      if (rawInt && typeof rawInt === 'object') rawInt = rawInt.Value;
      if (!rawInt && (line.TimeDifftoPositionAhead || line.TimeDiffToPositionAhead)) {
        const tInt = line.TimeDifftoPositionAhead || line.TimeDiffToPositionAhead;
        rawInt = typeof tInt === 'object' ? tInt.Value : tInt;
      }
      if (!rawInt && line.Stats) {
        if (partIdx >= 0) {
          const stPart = Array.isArray(line.Stats) ? line.Stats[partIdx] : line.Stats[String(partIdx)];
          const tInt = stPart && (stPart.TimeDifftoPositionAhead || stPart.TimeDiffToPositionAhead);
          if (tInt) rawInt = tInt;
        } else {
          const statsList = Array.isArray(line.Stats) ? line.Stats : Object.values(line.Stats);
          for (let i = statsList.length - 1; i >= 0; i--) {
            const st = statsList[i];
            const tInt = st && (st.TimeDifftoPositionAhead || st.TimeDiffToPositionAhead);
            if (tInt) {
              rawInt = tInt;
              break;
            }
          }
        }
      }
      if (rawInt !== undefined) {
        DRIVER_TELEMETRY[drvNum].interval = (rawInt && rawInt !== '--' && rawInt !== '-') ? String(rawInt) : '-';
      }

      if (line.InPit !== undefined) DRIVER_TELEMETRY[drvNum].inPit = Boolean(line.InPit);
      else if (line.inPit !== undefined) DRIVER_TELEMETRY[drvNum].inPit = Boolean(line.inPit);

      if (line.PitOut !== undefined) {
        DRIVER_TELEMETRY[drvNum].isOutLap = Boolean(line.PitOut);
        if (line.PitOut) DRIVER_TELEMETRY[drvNum].inPit = false;
      } else if (line.pitOut !== undefined) {
        DRIVER_TELEMETRY[drvNum].isOutLap = Boolean(line.pitOut);
        if (line.pitOut) DRIVER_TELEMETRY[drvNum].inPit = false;
      }

      // Speed traps live (ST, FL, I1, I2) per calibrare il picco velocistico sul rettilineo
      const rawSpeeds = line.Speeds || line.speeds;
      if (rawSpeeds && typeof rawSpeeds === 'object') {
        const parseSpd = (s) => {
          if (!s) return 0;
          const v = parseFloat(typeof s === 'object' ? s.Value : s);
          return !isNaN(v) && v > 0 ? v : 0;
        };
        const maxTrap = Math.max(parseSpd(rawSpeeds.ST), parseSpd(rawSpeeds.FL), parseSpd(rawSpeeds.I1), parseSpd(rawSpeeds.I2));
        if (maxTrap > 200) DRIVER_TELEMETRY[drvNum].topSpeedTrap = maxTrap;
      }

      // Ancoraggio posizione in pista sui mini-settori reali (Sectors[0..2].Segments)
      const rawSectors = line.Sectors || line.sectors;
      if (rawSectors && typeof rawSectors === 'object') {
        if (!DRIVER_TELEMETRY[drvNum].sectorsState) {
          DRIVER_TELEMETRY[drvNum].sectorsState = {
            0: { Value: '', Segments: {} },
            1: { Value: '', Segments: {} },
            2: { Value: '', Segments: {} }
          };
        }
        const secState = DRIVER_TELEMETRY[drvNum].sectorsState;
        for (let sIdx = 0; sIdx < 3; sIdx++) {
          const incomingSec = rawSectors[String(sIdx)] || rawSectors[sIdx];
          if (!incomingSec || typeof incomingSec !== 'object') continue;
          if (incomingSec.Value !== undefined) secState[sIdx].Value = incomingSec.Value;
          else if (incomingSec.value !== undefined) secState[sIdx].Value = incomingSec.value;
          const incomingSegs = incomingSec.Segments || incomingSec.segments;
          if (incomingSegs && typeof incomingSegs === 'object') {
            for (const [k, v] of Object.entries(incomingSegs)) {
              if (v && typeof v === 'object') {
                const stVal = v.Status !== undefined ? v.Status : v.status;
                if (!secState[sIdx].Segments[k]) secState[sIdx].Segments[k] = {};
                if (stVal !== undefined) secState[sIdx].Segments[k].Status = Number(stVal) || 0;
              }
            }
          }
        }

        const toSortedSegArray = (segMap) => {
          return Object.keys(segMap)
            .map(k => parseInt(k, 10))
            .filter(k => !isNaN(k))
            .sort((a, b) => a - b)
            .map(k => segMap[String(k)]);
        };
        const segs0 = toSortedSegArray(secState[0].Segments);
        const segs1 = toSortedSegArray(secState[1].Segments);
        const segs2 = toSortedSegArray(secState[2].Segments);
        const n0 = segs0.length;
        const n1 = segs1.length;
        const n2 = segs2.length;
        const totalSegs = n0 + n1 + n2;
        if (totalSegs > 0) {
          const countContiguous = (arr) => {
            let c = 0;
            for (let i = 0; i < arr.length; i++) {
              const st = Number(arr[i] && arr[i].Status);
              if (st > 0 && st !== 2064) c++;
              else break;
            }
            return c;
          };
          const d0 = countContiguous(segs0);
          const d1 = countContiguous(segs1);
          const d2 = countContiguous(segs2);
          if (d0 + d1 + d2 > 0) {
            let currentMiniSector = 0;
            if (d0 < n0) {
              currentMiniSector = d0;
            } else if (d1 < n1) {
              currentMiniSector = n0 + d1;
            } else if (d2 < n2) {
              currentMiniSector = n0 + n1 + d2;
            } else {
              currentMiniSector = 0;
            }
            const dur = typeof LAP_DURATION !== 'undefined' && LAP_DURATION ? LAP_DURATION : 90.0;
            const getMiniSectorStartSec = (mIdx) => {
              if (mIdx <= 0) return 0;
              if (mIdx >= totalSegs) return dur;
              if (Array.isArray(window.OFFICIAL_MINI_SECTOR_INDEXES) &&
                  window.OFFICIAL_MINI_SECTOR_INDEXES.length >= totalSegs &&
                  Array.isArray(NODES) && NODES.length > 0) {
                const ptIdx = Math.max(0, Math.min(NODES.length - 1, window.OFFICIAL_MINI_SECTOR_INDEXES[mIdx]));
                if (NODES[ptIdx] && NODES[ptIdx][0] !== undefined) {
                  return NODES[ptIdx][0];
                }
              }
              return (mIdx / totalSegs) * dur;
            };
            const segStartSec = getMiniSectorStartSec(currentMiniSector);
            let segEndSec = getMiniSectorStartSec(currentMiniSector + 1);
            if (segEndSec <= segStartSec) segEndSec = dur;

            DRIVER_TELEMETRY[drvNum].currentMiniSector = currentMiniSector;
            DRIVER_TELEMETRY[drvNum].totalMiniSectors = totalSegs;
            DRIVER_TELEMETRY[drvNum].segStartSec = segStartSec;
            DRIVER_TELEMETRY[drvNum].segEndSec = segEndSec;
          }
        }
      }

      if (line.Retired !== undefined) DRIVER_TELEMETRY[drvNum].isRetired = Boolean(line.Retired);
      else if (line.retired !== undefined) DRIVER_TELEMETRY[drvNum].isRetired = Boolean(line.retired);

      if (line.NumberOfLaps !== undefined) DRIVER_TELEMETRY[drvNum].lapsCount = Number(line.NumberOfLaps);
      else if (line.numberOfLaps !== undefined) DRIVER_TELEMETRY[drvNum].lapsCount = Number(line.numberOfLaps);
      if (line.NumberOfPitStops !== undefined) DRIVER_TELEMETRY[drvNum].pitCount = line.NumberOfPitStops;
      else if (line.numberOfPitStops !== undefined) DRIVER_TELEMETRY[drvNum].pitCount = line.numberOfPitStops;

      // BestLapTimes per Qualifying (Q1, Q2, Q3) - supports Array or indexed Object
      if (line.BestLapTimes && typeof line.BestLapTimes === 'object') {
        const b0 = line.BestLapTimes[0] || line.BestLapTimes['0'];
        const b1 = line.BestLapTimes[1] || line.BestLapTimes['1'];
        const b2 = line.BestLapTimes[2] || line.BestLapTimes['2'];
        if (b0 && b0.Value !== undefined) DRIVER_TELEMETRY[drvNum].q1Best = b0.Value ? this.parseLapTimeToSec(b0.Value) : null;
        if (b1 && b1.Value !== undefined) DRIVER_TELEMETRY[drvNum].q2Best = b1.Value ? this.parseLapTimeToSec(b1.Value) : null;
        if (b2 && b2.Value !== undefined) DRIVER_TELEMETRY[drvNum].q3Best = b2.Value ? this.parseLapTimeToSec(b2.Value) : null;
      }

      // BestLapTime della sessione corrente
      let bestLapRaw = null;
      if (partIdx >= 0 && line.BestLapTimes && typeof line.BestLapTimes === 'object') {
        const bCurr = line.BestLapTimes[partIdx] || line.BestLapTimes[String(partIdx)];
        bestLapRaw = (bCurr && bCurr.Value) ? bCurr.Value : '';
      } else if (line.BestLapTime !== undefined) {
        bestLapRaw = (line.BestLapTime && line.BestLapTime.Value !== undefined) ? line.BestLapTime.Value : line.BestLapTime;
      } else if (line.bestLapTime !== undefined) {
        bestLapRaw = line.bestLapTime;
      }

      if (DRIVER_TELEMETRY[drvNum].isKnockedOut || window.QUALI_PART_ENDED) {
        DRIVER_TELEMETRY[drvNum].overallBest = null;
        DRIVER_TELEMETRY[drvNum].gapLeader = '-';
        DRIVER_TELEMETRY[drvNum].interval = '-';
      } else if (bestLapRaw !== null) {
        if (bestLapRaw && bestLapRaw !== '--' && bestLapRaw !== '-') {
          const durSec = this.parseLapTimeToSec(String(bestLapRaw));
          if (durSec > 0) {
            DRIVER_TELEMETRY[drvNum].overallBest = durSec;
            if (!DRIVER_TELEMETRY[drvNum].q1Best && partIdx <= 0) DRIVER_TELEMETRY[drvNum].q1Best = durSec;
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
        } else {
          DRIVER_TELEMETRY[drvNum].overallBest = null;
          DRIVER_TELEMETRY[drvNum].gapLeader = '-';
          DRIVER_TELEMETRY[drvNum].interval = '-';
        }
      }

      const lastLapRaw = (line.LastLapTime && line.LastLapTime.Value) ? line.LastLapTime.Value : (typeof line.lastLapTime === 'string' ? line.lastLapTime : null);
      if (lastLapRaw && lastLapRaw !== '--' && lastLapRaw !== '-') {
        DRIVER_TELEMETRY[drvNum].lastLapTimeStr = lastLapRaw;
        const lastSec = this.parseLapTimeToSec(lastLapRaw);
        if (lastSec > 0) DRIVER_TELEMETRY[drvNum].lastLapSec = lastSec;
      }
    }

    // Dynamic calculation of GAP (to P1) and INT (to driver ahead) across sorted drivers
    const allDrvKeys = Object.keys(DRIVERS).length > 0 ? Object.keys(DRIVERS) : Object.keys(DRIVER_TELEMETRY);
    allDrvKeys.sort((a, b) => {
      const ta = DRIVER_TELEMETRY[a] || {};
      const tb = DRIVER_TELEMETRY[b] || {};
      if (ta.isRetired && !tb.isRetired) return 1;
      if (!ta.isRetired && tb.isRetired) return -1;
      const pa = ta.position || 99;
      const pb = tb.position || 99;
      if (pa !== pb) return pa - pb;
      return (ta.overallBest || Infinity) - (tb.overallBest || Infinity);
    });
    GRID_ORDER = allDrvKeys;

    const parseDeltaSec = (s) => {
      if (!s || typeof s !== 'string') return null;
      const c = s.trim().toUpperCase();
      if (!c || c === '-' || c === '--') return null;
      if (c === 'LEADER' || c === '0' || c === '0.000' || c === '+0.000') return 0;
      if (c.includes('LAP') || c.includes('L')) return null;
      const v = parseFloat(c.replace('+', '').replace('S', ''));
      return !isNaN(v) && v >= 0 ? v : null;
    };

    if (allDrvKeys.length > 0) {
      if (window.QUALI_PART_ENDED) {
        for (const k of allDrvKeys) {
          if (DRIVER_TELEMETRY[k]) {
            DRIVER_TELEMETRY[k].overallBest = null;
            DRIVER_TELEMETRY[k].gapLeader = '-';
            DRIVER_TELEMETRY[k].interval = '-';
          }
        }
      } else {
        const leaderKey = allDrvKeys[0];
        if (!DRIVER_TELEMETRY[leaderKey]) DRIVER_TELEMETRY[leaderKey] = {};
        const leaderBest = DRIVER_TELEMETRY[leaderKey].overallBest || 0;
        const isQualiMode = partIdx >= 0 || (typeof sessionType === 'string' && sessionType.toLowerCase().includes('qualifying'));
        DRIVER_TELEMETRY[leaderKey].gapLeader = (isQualiMode && !leaderBest) ? '-' : 'LEADER';
        DRIVER_TELEMETRY[leaderKey].interval = '-';

        for (let i = 1; i < allDrvKeys.length; i++) {
          const currKey = allDrvKeys[i];
          const aheadKey = allDrvKeys[i - 1];
          if (!DRIVER_TELEMETRY[currKey]) DRIVER_TELEMETRY[currKey] = {};
          const curr = DRIVER_TELEMETRY[currKey];
          const ahead = DRIVER_TELEMETRY[aheadKey] || {};

          const currBest = curr.overallBest || 0;
          const aheadBest = ahead.overallBest || 0;

          const gapMissing = !curr.gapLeader || curr.gapLeader === '-' || curr.gapLeader === '--' || curr.gapLeader === 'LEADER';
          const intMissing = !curr.interval || curr.interval === '-' || curr.interval === '--' || curr.interval === 'LEADER';

          if (!currBest && gapMissing && intMissing) {
            curr.gapLeader = '-';
            curr.interval = '-';
            continue;
          }

          // 1. Dynamic GAP (distacco dal primo)
          if (gapMissing) {
            if (currBest > 0 && leaderBest > 0 && currBest >= leaderBest) {
              curr.gapLeader = '+' + (currBest - leaderBest).toFixed(3) + 's';
            } else {
              const aheadGap = i === 1 ? 0 : parseDeltaSec(ahead.gapLeader);
              const currInt = parseDeltaSec(curr.interval);
              if (aheadGap !== null && currInt !== null) {
                curr.gapLeader = '+' + (aheadGap + currInt).toFixed(3) + 's';
              }
            }
          }

            // 2. Dynamic INT (distacco dal pilota davanti)
          if (intMissing) {
            const currGap = parseDeltaSec(curr.gapLeader);
            const aheadGap = i === 1 ? 0 : parseDeltaSec(ahead.gapLeader);
            if (currGap !== null && aheadGap !== null && currGap >= aheadGap) {
              curr.interval = '+' + (currGap - aheadGap).toFixed(3) + 's';
            } else if (currBest > 0 && aheadBest > 0 && currBest >= aheadBest) {
              curr.interval = '+' + (currBest - aheadBest).toFixed(3) + 's';
            }
          }
        }

        // 3. Spaziatura intra-mini-settore ordinata per classifica e INT reale
        const nowSimSec = typeof currentSecond !== 'undefined' ? currentSecond : 0;
        let idx = 0;
        while (idx < allDrvKeys.length) {
          const drvK = allDrvKeys[idx];
          const tel = DRIVER_TELEMETRY[drvK];
          if (!tel || tel.currentMiniSector === undefined || tel.inPit || tel.isRetired) {
            idx++;
            continue;
          }
          const mSec = tel.currentMiniSector;
          const group = [drvK];
          let j = idx + 1;
          while (j < allDrvKeys.length) {
            const nextK = allDrvKeys[j];
            const nextTel = DRIVER_TELEMETRY[nextK];
            if (!nextTel || nextTel.inPit || nextTel.isRetired) {
              j++;
              continue;
            }
            if (nextTel.currentMiniSector === mSec) {
              group.push(nextK);
              j++;
            } else {
              break;
            }
          }

          const segStart = tel.segStartSec || 0;
          const segEnd = tel.segEndSec || (segStart + 4.0);
          const segSpan = Math.max(0.2, segEnd - segStart);
          const usableStart = segStart + segSpan * 0.06;
          const usableEnd = segEnd - segSpan * 0.04;
          const usableSpan = Math.max(0.1, usableEnd - usableStart);

          if (group.length === 1) {
            const segKey = mSec + '/' + (tel.totalMiniSectors || 22) + ':solo';
            tel.segMaxSec = usableEnd;
            if (tel.lastSegKey !== segKey) {
              tel.lastSegKey = segKey;
              const curSec = (tel.anchorTrackSec !== undefined && tel.anchorSimSec !== undefined && typeof getDriverTrackSec === 'function')
                ? getDriverTrackSec(drvK, nowSimSec)
                : usableStart;
              tel.anchorTrackSec = curSec;
              tel.anchorSimSec = nowSimSec;
              tel.targetTrackSec = undefined;
            }
          } else {
            const cumInt = [0];
            for (let k = 1; k < group.length; k++) {
              const tMember = DRIVER_TELEMETRY[group[k]] || {};
              const dInt = parseDeltaSec(tMember.interval);
              const step = dInt !== null && dInt > 0 ? Math.min(3.5, Math.max(0.18, dInt)) : 0.45;
              cumInt.push(cumInt[k - 1] + step);
            }
            const totalGroupInt = Math.max(0.3, cumInt[cumInt.length - 1]);
            const scale = totalGroupInt > usableSpan * 0.85 ? (usableSpan * 0.85) / totalGroupInt : 1.0;

            for (let k = 0; k < group.length; k++) {
              const memberKey = group[k];
              const mTel = DRIVER_TELEMETRY[memberKey];
              const offsetFromFront = cumInt[k] * scale;
              const memberMaxSec = Math.max(usableStart, usableEnd - offsetFromFront);
              const memberStartSec = Math.max(segStart + 0.02, memberMaxSec - Math.min(0.6, segSpan * 0.2));
              mTel.segMaxSec = memberMaxSec;
              const segKey = mSec + '/' + (mTel.totalMiniSectors || 22) + ':' + k + '/' + group.length;
              if (mTel.lastSegKey !== segKey) {
                mTel.lastSegKey = segKey;
                const curSec = (mTel.anchorTrackSec !== undefined && mTel.anchorSimSec !== undefined && typeof getDriverTrackSec === 'function')
                  ? getDriverTrackSec(memberKey, nowSimSec)
                  : memberStartSec;
                mTel.anchorTrackSec = curSec;
                mTel.anchorSimSec = nowSimSec;
                mTel.targetTrackSec = memberMaxSec;
              }
            }
          }

          idx = j;
        }
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
    if (snapshot.lapCount) {
      if (snapshot.lapCount.CurrentLap) window.CURRENT_SESSION_LAP = Number(snapshot.lapCount.CurrentLap);
      if (snapshot.lapCount.TotalLaps && Number(snapshot.lapCount.TotalLaps) > 0) TOTAL_LAPS = Number(snapshot.lapCount.TotalLaps);
    }
    if (snapshot.drivers) this.handleDriverList(snapshot.drivers);
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

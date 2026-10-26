/**
 * FastestLap — live_timing_app.js
 * Modulo Principale / Orchestratore Live Timing & Telemetria
 * Integra Android Bridge, parametri URL, ciclo di animazione e navigazione schermate.
 */

const CIRCUIT_TOTAL_LAPS = {
  '9': 56, '63': 57, '149': 50, '1': 58, '10': 53, '7': 56, '151': 57,
  '6': 63, '22': 78, '23': 70, '4': 66, '19': 71, '2': 52, '14': 70,
  '20': 44, '55': 72, '39': 53, '144': 51, '61': 62, '65': 71, '15': 71,
  '152': 50, '150': 57, '70': 58,
  'austin': 56, 'cota': 56, 'united_states': 56, 'usa': 56,
  'bahrain': 57, 'sakhir': 57,
  'jeddah': 50, 'saudi_arabia': 50,
  'albert_park': 58, 'melbourne': 58, 'australia': 58,
  'suzuka': 53, 'japan': 53,
  'shanghai': 56, 'china': 56,
  'miami': 57,
  'imola': 63, 'emilia_romagna': 63,
  'monaco': 78, 'monte_carlo': 78,
  'montreal': 70, 'canada': 70, 'villeneuve': 70,
  'catalunya': 66, 'barcelona': 66, 'spain': 66,
  'red_bull_ring': 71, 'spielberg': 71, 'austria': 71,
  'silverstone': 52, 'great_britain': 52, 'britain': 52,
  'hungaroring': 70, 'hungary': 70, 'budapest': 70,
  'spa': 44, 'belgium': 44, 'francorchamps': 44,
  'zandvoort': 72, 'netherlands': 72, 'dutch': 72,
  'monza': 53, 'italy': 53,
  'baku': 51, 'azerbaijan': 51,
  'marina_bay': 62, 'singapore': 62,
  'rodriguez': 71, 'mexico': 71,
  'interlagos': 71, 'brazil': 71, 'sao_paulo': 71,
  'las_vegas': 50, 'vegas': 50,
  'lusail': 57, 'losail': 57, 'qatar': 57,
  'yas_marina': 58, 'abu_dhabi': 58
};

function resolveCircuitTotalLaps(query) {
  if (!query) return 56;
  const str = String(query).toLowerCase().trim().replace(/ /g, '_');
  if (CIRCUIT_TOTAL_LAPS[str]) return CIRCUIT_TOTAL_LAPS[str];
  for (const [k, v] of Object.entries(CIRCUIT_TOTAL_LAPS)) {
    if (str.includes(k)) return v;
  }
  return 56;
}

let preferredCircuit = '';

function updateSessionStatusBar() {
  const curSt = String(window.CURRENT_SESSION_STATUS || '').toUpperCase();
  const isFinished = curSt === 'FINALISED' || curSt === 'FINISHED' || curSt === 'ENDS' || curSt === 'INACTIVE' || curSt === 'ABORTED' ||
                     (typeof SESSION_DURATION !== 'undefined' && currentSecond >= SESSION_DURATION) ||
                     (typeof RACE_FINISH_SEC !== 'undefined' && currentSecond >= RACE_FINISH_SEC);
  if (isFinished && typeof checkSessionTermination === 'function') {
    checkSessionTermination();
  }

  const currentTrackStatus = window.CURRENT_TRACK_STATUS || '';
  const scActive = !isFinished && (currentTrackStatus === '4' || isSafetyCarActive(currentSecond));
  const vscActive = !isFinished && (currentTrackStatus === '6' || isVirtualSafetyCarActive(currentSecond));

  const flagBadge = document.getElementById('statusBarFlag');
  const flagIcon = document.getElementById('statusBarFlagIcon');
  const flagText = document.getElementById('statusBarFlagText');

  if (flagBadge && flagText && flagIcon) {
    flagBadge.className = 'status-bar-flag-badge';
    if (isFinished) {
      flagBadge.classList.add('flag-chequered');
      flagIcon.textContent = '🏁';
      flagText.textContent = 'BANDIERA A SCACCHI';
    } else if (currentTrackStatus === '5') {
      flagBadge.classList.add('flag-vsc');
      flagIcon.textContent = '🔴';
      flagText.textContent = 'BANDIERA ROSSA (SESSIONE SOSPESA / RIMANDATA)';
    } else if (scActive) {
      flagBadge.classList.add('flag-sc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'SAFETY CAR';
    } else if (vscActive) {
      flagBadge.classList.add('flag-vsc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'VSC';
    } else if (currentTrackStatus === '2') {
      flagBadge.classList.add('flag-sc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'BANDIERA GIALLA';
    } else {
      flagBadge.classList.add('flag-green');
      flagIcon.textContent = '🟢';
      flagText.textContent = 'BANDIERA VERDE';
    }
  }

  const liveBadge = document.getElementById('liveBadgeIndicator');
  if (liveBadge) {
    if (isFinished) {
      liveBadge.innerHTML = 'TERMINATA';
    } else {
      liveBadge.innerHTML = '<div class="live-dot"></div>LIVE';
    }
  }

  const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType;
  const isSq = typeof isSprintQualifying === 'function' ? isSprintQualifying(sessionType, sessionPart) : (sessionPart && sessionPart.toUpperCase().startsWith('SQ'));

  // Nome e Tipo Sessione
  const sessVal = document.getElementById('statusBarSessionVal');
  if (sessVal) {
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionName === 'function') {
      const bSess = window.FastestLapBridge.getSessionName();
      if (bSess && bSess.trim().length > 0 && bSess !== 'Sessione Live') {
        sessVal.textContent = bSess.toUpperCase().trim();
      } else if (sType === 'practice') {
        sessVal.textContent = sessionPart || 'PROVE LIBERE';
      } else if (sType === 'qualifying') {
        sessVal.textContent = isSq ? (sessionPart || 'QUALIFICA SPRINT') : (sessionPart || 'QUALIFICHE');
      } else {
        sessVal.textContent = (sType === 'sprint' ? 'GARA SPRINT' : 'GARA');
      }
    } else if (sType === 'practice') {
      sessVal.textContent = sessionPart || 'PROVE LIBERE';
    } else if (sType === 'qualifying') {
      sessVal.textContent = isSq ? (sessionPart || 'QUALIFICA SPRINT') : (sessionPart || 'QUALIFICHE');
    } else {
      sessVal.textContent = (sType === 'sprint' ? 'GARA SPRINT' : 'GARA');
    }
  }

  // Progresso Sessione / Conteggio Giri
  const lapVal = document.getElementById('statusBarLapVal');
  if (lapVal) {
    if (isFinished) {
      lapVal.textContent = 'SESSIONE TERMINATA';
    } else if (currentTrackStatus === '5') {
      lapVal.textContent = 'SESSIONE SOSPESA / RIMANDATA';
    } else if (currentSecond <= 0) {
      lapVal.textContent = 'ATTESA PARTENZA';
    } else if (sType === 'practice') {
      lapVal.textContent = 'SESSIONE IN CORSO';
    } else if (sType === 'qualifying') {
      const qPrefix = isSq ? 'SQ' : 'Q';
      let qPhase = qPrefix + '1';
      if (sessionPart === 'Q3' || sessionPart === 'SQ3') qPhase = qPrefix + '3';
      else if (sessionPart === 'Q2' || sessionPart === 'SQ2') qPhase = qPrefix + '2';
      else if (sessionPart === 'Q1' || sessionPart === 'SQ1') qPhase = qPrefix + '1';
      lapVal.textContent = qPhase + ' IN CORSO';
    } else {
      const curLap = getDriverLap(focusedDriver, currentSecond);
      if (!TOTAL_LAPS || TOTAL_LAPS <= 0) {
        TOTAL_LAPS = resolveCircuitTotalLaps(currentCircuit || preferredCircuit || sessionName);
        window.CURRENT_SESSION_TOTAL_LAPS = TOTAL_LAPS;
      }
      if (TOTAL_LAPS && TOTAL_LAPS > 0) {
        lapVal.textContent = 'GIRO ' + curLap + ' / ' + TOTAL_LAPS;
      } else {
        lapVal.textContent = 'GIRO ' + curLap;
      }
    }
  }

  // Orologio UTC
  const clockVal = document.getElementById('statusBarClockVal');
  if (clockVal) {
    clockVal.textContent = formatSimClock(currentSecond);
  }
}

function switchMainView(viewId) {
  currentMainView = viewId;
  document.querySelectorAll('.main-view').forEach(v => v.classList.remove('active'));
  const target = document.getElementById(viewId);
  if (target) target.classList.add('active');

  const navMap = {
    'view-standings': 'btnNavStandings',
    'view-track': 'btnNavTrack',
    'view-race-control': 'btnNavRaceControl'
  };
  document.querySelectorAll('.bottom-nav-btn').forEach(b => b.classList.remove('active'));
  const btn = document.getElementById(navMap[viewId]);
  if (btn) btn.classList.add('active');

  // Notifica l'Activity Android per adattare l'orientamento dello schermo (Landscape per Mappa, Libero per Classifica/RC)
  if (window.FastestLapBridge && typeof window.FastestLapBridge.onTabChanged === 'function') {
    try { window.FastestLapBridge.onTabChanged(viewId); } catch (e) { console.error(e); }
  }

  if (viewId === 'view-track') {
    setTimeout(() => {
      resizeTrackCanvas();
      drawTrack();
    }, 10);
    setTimeout(() => {
      resizeTrackCanvas();
      drawTrack();
    }, 70);
    setTimeout(() => {
      resizeTrackCanvas();
      drawTrack();
    }, 200);
  } else if (viewId === 'view-race-control') {
    updateRaceControl();
  } else {
    updateTimingTables();
  }
}

function switchSideTab(tabId) {
  currentSideTab = tabId;
  document.querySelectorAll('.side-tab-content').forEach(c => c.classList.remove('active'));
  const t = document.getElementById(tabId);
  if (t) t.classList.add('active');

  document.querySelectorAll('.side-tab-btn').forEach(b => b.classList.remove('active'));
  if (tabId === 'side-standings') {
    const b = document.getElementById('btnSideStandings');
    if (b) b.classList.add('active');
  } else if (tabId === 'side-telemetry') {
    const b = document.getElementById('btnSideTelemetry');
    if (b) b.classList.add('active');
    updateCockpit(1, selectedDriver1);
    updateCockpit(2, selectedDriver2);
  } else if (tabId === 'side-rc') {
    const b = document.getElementById('btnSideRaceControl');
    if (b) b.classList.add('active');
    updateRaceControl();
  }

  const cockpitBtn = document.getElementById('statusBarCockpitBtn');
  if (cockpitBtn) cockpitBtn.classList.toggle('active', tabId === 'side-telemetry');

  updateTimingTables();
  drawTrack();
}

function updateStatusBarCockpitTrigger(drvKey) {
  const btn = document.getElementById('statusBarCockpitBtn');
  const badge = document.getElementById('statusBarDrvBadge');
  if (!btn || !drvKey) return;
  const d = DRIVERS[drvKey] || {};
  const code = d.code || ('#' + drvKey);
  if (badge) badge.textContent = code;
  btn.style.display = 'inline-flex';
  btn.classList.toggle('active', currentSideTab === 'side-telemetry');
}

function toggleTelemetrySidePanel() {
  if (currentMainView !== 'view-track') {
    switchMainView('view-track');
  }
  if (currentSideTab === 'side-telemetry') {
    switchSideTab('side-standings');
  } else {
    switchSideTab('side-telemetry');
  }
}

function closeTelemetrySidePanel() {
  switchSideTab('side-standings');
}

function closeSession() {
  if (window.FastestLapBridge && typeof window.FastestLapBridge.closeLive === 'function') {
    window.FastestLapBridge.closeLive();
  } else {
    window.history.back();
  }
}

let sessionEndedTimestamp = null;
let sessionTerminationTimer = null;
let isSessionTerminatedState = false;

function checkSessionTermination() {
  if (isSessionTerminatedState) return;

  const curSt = String(window.CURRENT_SESSION_STATUS || '').toUpperCase();
  const isEnded = curSt === 'FINALISED' || curSt === 'FINISHED' || curSt === 'ENDS' || curSt === 'INACTIVE' || curSt === 'ABORTED' ||
                  (typeof SESSION_DURATION !== 'undefined' && currentSecond >= SESSION_DURATION) ||
                  (typeof RACE_FINISH_SEC !== 'undefined' && currentSecond >= RACE_FINISH_SEC);

  if (!isEnded) return;

  if (!sessionEndedTimestamp) {
    sessionEndedTimestamp = Date.now();
  }

  const GRACE_PERIOD_MS = 180 * 1000; // 3 minuti di tolleranza
  const elapsed = Date.now() - sessionEndedTimestamp;

  if (elapsed >= GRACE_PERIOD_MS) {
    terminateLiveTimingSession();
  } else if (!sessionTerminationTimer) {
    const banner = document.getElementById('sessionEndingBanner');
    if (banner) banner.style.display = 'flex';

    function tickEnding() {
      const remSec = Math.ceil((sessionEndedTimestamp + GRACE_PERIOD_MS - Date.now()) / 1000);
      if (remSec <= 0) {
        if (sessionTerminationTimer) {
          clearInterval(sessionTerminationTimer);
          sessionTerminationTimer = null;
        }
        terminateLiveTimingSession();
      } else {
        const m = Math.floor(remSec / 60);
        const s = remSec % 60;
        const timerEl = document.getElementById('sessionEndingTimer');
        if (timerEl) {
          timerEl.textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
        }
      }
    }

    tickEnding();
    sessionTerminationTimer = setInterval(tickEnding, 1000);
  }
}

function terminateLiveTimingSession() {
  isSessionTerminatedState = true;
  if (sessionTerminationTimer) {
    clearInterval(sessionTerminationTimer);
    sessionTerminationTimer = null;
  }

  const banner = document.getElementById('sessionEndingBanner');
  if (banner) banner.style.display = 'none';

  // Interrompe stream SignalR
  try {
    if (window.signalRClient && typeof window.signalRClient.disconnect === 'function') {
      window.signalRClient.disconnect();
    }
    if (window.FastestLapBridge && typeof window.FastestLapBridge.stopSignalRStream === 'function') {
      window.FastestLapBridge.stopSignalRStream();
    }
  } catch (e) {}

  // Mostra overlay di chiusura sessione terminata
  const overlay = document.getElementById('sessionTerminatedOverlay');
  if (overlay) {
    const sub = document.getElementById('terminatedEventSubtitle');
    const hdr = document.getElementById('hdrEventTitle');
    if (sub && hdr) sub.textContent = hdr.textContent || 'FORMULA 1 GRAND PRIX';
    overlay.style.display = 'flex';
  }

  // Se eseguito dentro l'app Android, chiude automaticamente la schermata live dopo 4 secondi
  if (window.FastestLapBridge && typeof window.FastestLapBridge.closeLive === 'function') {
    setTimeout(() => {
      closeSession();
    }, 4000);
  }
}

window.checkSessionTermination = checkSessionTermination;
window.terminateLiveTimingSession = terminateLiveTimingSession;

function applyGenericRealSession(circuitId, sType) {
  isGenericRealMode = true;
  currentCircuit = circuitId || 'generic';
  sessionType = typeof normalizeSessionType === 'function'
    ? normalizeSessionType(sType || sessionType || 'race', sessionPart)
    : (sType || sessionType || 'race');

  DRIVERS = { ...GRID_2026_DRIVERS };
  GRID_ORDER = Object.keys(DRIVERS);

  const cid = (circuitId || '').toLowerCase().trim();
  if (cid.includes('sepang') && typeof SIM_SEPANG !== 'undefined') {
    NODES = SIM_SEPANG.nodes;
    SVG_WIDTH = SIM_SEPANG.svgWidth || 1280;
    SVG_HEIGHT = SIM_SEPANG.svgHeight || 1057;
    LAP_DURATION = SIM_SEPANG.lapDuration || 95.0;
  } else if (cid.includes('baku') && typeof SIM_BAKU !== 'undefined') {
    NODES = SIM_BAKU.nodes;
    SVG_WIDTH = SIM_BAKU.svgWidth || 500;
    SVG_HEIGHT = SIM_BAKU.svgHeight || 371;
    LAP_DURATION = SIM_BAKU.lapDuration || 108.6;
  } else {
    SVG_WIDTH = (trackBgImage && trackBgImage.naturalWidth) ? trackBgImage.naturalWidth : 1000;
    SVG_HEIGHT = (trackBgImage && trackBgImage.naturalHeight) ? trackBgImage.naturalHeight : 800;
    LAP_DURATION = 90.0;
    if (typeof buildDefaultTrackNodes === 'function') {
      NODES = buildDefaultTrackNodes(SVG_WIDTH, SVG_HEIGHT);
    }
  }

  PIT_LANE_NODES = [];
  PIT_LANE_DISTS = [0];
  PIT_LANE_TOTAL_DIST = 1;

  DRIVER_KEYFRAMES = {};
  PIT_STOPS = [];
  RETIREMENTS = [];
  INCIDENTS = [];
  RACE_EVENTS = [];

  // Lettura messaggi da Android Bridge se disponibili
  let bridgedRc = null;
  if (window.FastestLapBridge && typeof window.FastestLapBridge.getRaceControlMessagesJson === 'function') {
    try {
      const rcStr = window.FastestLapBridge.getRaceControlMessagesJson();
      if (rcStr && rcStr.trim().length > 2) {
        bridgedRc = JSON.parse(rcStr);
      }
    } catch (e) {}
  }

  if (Array.isArray(bridgedRc) && bridgedRc.length > 0) {
    RACE_CONTROL_MESSAGES = bridgedRc.map(m => ({
      category: m.category || (m.flag ? 'Flag' : 'DIREZIONE GARA'),
      message: m.message || '',
      flag: m.flag || null,
      lap: m.lapNumber !== undefined ? m.lapNumber : m.lap,
      date: m.date || null,
      timeSec: m.timeSec !== undefined ? m.timeSec : 0
    }));
  } else {
    RACE_CONTROL_MESSAGES = getInitialRaceControlMessages(sessionType, sessionPart);
  }

  TOTAL_LAPS = null;
  if (window.FastestLapBridge && typeof window.FastestLapBridge.getTotalLaps === 'function') {
    try {
      const tl = parseInt(window.FastestLapBridge.getTotalLaps(), 10);
      if (!isNaN(tl) && tl > 0) TOTAL_LAPS = tl;
    } catch (e) {}
  }
  if (!TOTAL_LAPS) {
    try {
      const urlParams = new URLSearchParams(window.location.search);
      const pLaps = urlParams.get('total_laps');
      if (pLaps) {
        const tl = parseInt(pLaps, 10);
        if (!isNaN(tl) && tl > 0) TOTAL_LAPS = tl;
      }
    } catch (e) {}
  }
  if (!TOTAL_LAPS) {
    TOTAL_LAPS = resolveCircuitTotalLaps(preferredCircuit || currentCircuit || sessionName);
  }
  window.CURRENT_SESSION_TOTAL_LAPS = TOTAL_LAPS;
  LAP_DURATION = 90.0;
  const isRace = (sessionType === 'race');
  const isSprint = (sessionType === 'sprint');
  const baseDuration = isSprint ? 4500 : (isRace ? 7200 : 3600);

  let sessionElapsedSec = 0;
  if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionElapsedSeconds === 'function') {
    try {
      const es = window.FastestLapBridge.getSessionElapsedSeconds();
      if (!isNaN(es) && es >= 0) sessionElapsedSec = es;
    } catch (e) {}
  }
  if (!sessionElapsedSec) {
    try {
      const urlParams = new URLSearchParams(window.location.search);
      const pElapsed = urlParams.get('session_elapsed_sec');
      if (pElapsed) {
        const es = parseInt(pElapsed, 10);
        if (!isNaN(es) && es >= 0) sessionElapsedSec = es;
      }
    } catch (e) {}
  }

  SESSION_DURATION = baseDuration;
  RACE_FINISH_SEC = SESSION_DURATION;
  currentSecond = sessionElapsedSec;

  buildGenericSessionLaps(sessionType);

  populateDriverDropdowns();
  focusedDriver = '16';
  selectedDriver1 = '16';
  selectedDriver2 = '1';

  const hdrTitle = document.getElementById('hdrEventTitle');
  if (hdrTitle) {
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
      const bTitle = window.FastestLapBridge.getEventTitle();
      if (bTitle && bTitle.trim().length > 0 && bTitle.toLowerCase() !== 'live timing') {
        hdrTitle.textContent = bTitle;
      }
    }
  }

  renderTableHeaders(sessionType);
  resizeTrackCanvas();
  updateTimingTables();
  updateRaceControl();
}

function animate(timestamp) {
  if (!lastAnimFrameTimestamp) lastAnimFrameTimestamp = timestamp;
  const deltaSec = Math.min((timestamp - lastAnimFrameTimestamp) / 1000, 0.1);
  lastAnimFrameTimestamp = timestamp;
  if (typeof currentDeltaTime !== 'undefined') {
    currentDeltaTime = deltaSec;
  }

  if (isPlaying) {
    currentSecond += deltaSec * simSpeedMultiplier;
    // In live streaming, non bloccare la progressione del tempo
    if (window.IS_SYNTHETIC_SIMULATION && currentSecond >= SESSION_DURATION) {
      currentSecond = SESSION_DURATION;
      isPlaying = false;
    }
  }

  try {
    drawTrack();
  } catch (err) {
    console.error('[TrackCanvas] drawTrack error:', err);
  }

  if (timestamp - lastUiUpdateTime > 100) {
    lastUiUpdateTime = timestamp;
    try {
      if (window.FastestLapBridge && typeof window.FastestLapBridge.getDriverTelemetryJson === 'function') {
        const telStr = window.FastestLapBridge.getDriverTelemetryJson();
        if (telStr && telStr.trim().length > 2) {
          window.updateDriverTelemetry(telStr);
        }
      }
    } catch (e) {}

    try { updateSessionStatusBar(); } catch (e) { console.error(e); }
    try { updateTimingTables(); } catch (e) { console.error(e); }
    try { updateCockpit(1, selectedDriver1); } catch (e) { console.error(e); }
    try { updateCockpit(2, selectedDriver2); } catch (e) { console.error(e); }
    try { updateRaceControl(); } catch (e) { console.error(e); }
  }

  requestAnimationFrame(animate);
}

let appInitialized = false;
function initApp() {
  if (appInitialized) return;
  appInitialized = true;

  canvas = document.getElementById('trackCanvas');
  if (canvas) ctx = canvas.getContext('2d');

  buildLedBars();

  // Controllo Parametri Query String & Android Bridge
  let preferredCircuit = '';
  let isLive = true;

  try {
    const urlParams = new URLSearchParams(window.location.search);
    const pCircuit = urlParams.get('circuit_id');
    const pImg = urlParams.get('circuit_image');
    const pType = urlParams.get('session_type');
    const pPart = urlParams.get('session_part');
    const pIsLive = urlParams.get('is_live');
    const pTz = urlParams.get('circuit_timezone');

    if (pIsLive !== null) {
      isLive = (pIsLive === 'true' || pIsLive === '1');
    }
    if (pTz && pTz.trim().length > 0) {
      circuitTimeZone = pTz.trim();
    }
    if (pCircuit && pCircuit.trim().length > 0) preferredCircuit = pCircuit.toLowerCase().trim();
    if (pPart && pPart.trim().length > 0) sessionPart = pPart.toUpperCase().trim();
    if (pType && pType.trim().length > 0) {
      if (typeof isSprintQualifying === 'function' && isSprintQualifying(pType, sessionPart)) {
        sessionType = 'qualifying';
        if (!sessionPart) sessionPart = 'SQ';
      } else {
        sessionType = typeof normalizeSessionType === 'function' ? normalizeSessionType(pType, sessionPart) : pType.toLowerCase().trim();
      }
    }
    if (pImg && pImg.trim().length > 0) {
      window.setCircuitImage(pImg.trim());
    }
  } catch (ignored) {}

  if (window.FastestLapBridge) {
    if (typeof window.FastestLapBridge.isSessionLive === 'function') {
      try {
        isLive = window.FastestLapBridge.isSessionLive();
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getCircuitTimeZone === 'function') {
      try {
        const tz = window.FastestLapBridge.getCircuitTimeZone();
        if (tz && tz.trim().length > 0) circuitTimeZone = tz.trim();
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getCircuitId === 'function') {
      try {
        const cid = window.FastestLapBridge.getCircuitId();
        if (cid && cid.trim().length > 0) preferredCircuit = cid.toLowerCase().trim();
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getSessionPart === 'function') {
      try {
        const sp = window.FastestLapBridge.getSessionPart();
        if (sp && sp.trim().length > 0) sessionPart = sp.toUpperCase().trim();
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getSessionType === 'function') {
      try {
        const st = window.FastestLapBridge.getSessionType();
        if (st && st.trim().length > 0) {
          if (typeof isSprintQualifying === 'function' && isSprintQualifying(st, sessionPart)) {
            sessionType = 'qualifying';
            if (!sessionPart) sessionPart = 'SQ';
          } else {
            sessionType = typeof normalizeSessionType === 'function' ? normalizeSessionType(st, sessionPart) : st.toLowerCase().trim();
          }
        }
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getCircuitImageUrl === 'function') {
      try {
        const imgUrl = window.FastestLapBridge.getCircuitImageUrl();
        if (imgUrl && imgUrl.trim().length > 0) {
          window.setCircuitImage(imgUrl.trim());
        }
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getRaceControlMessagesJson === 'function') {
      try {
        const rcStr = window.FastestLapBridge.getRaceControlMessagesJson();
        if (rcStr && rcStr.trim().length > 2) {
          window.updateRaceControlMessages(rcStr);
        }
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getDriverTelemetryJson === 'function') {
      try {
        const telStr = window.FastestLapBridge.getDriverTelemetryJson();
        if (telStr && telStr.trim().length > 2) {
          window.updateDriverTelemetry(telStr);
        }
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getPitStopsJson === 'function') {
      try {
        const pitStr = window.FastestLapBridge.getPitStopsJson();
        if (pitStr && pitStr.trim().length > 2) {
          window.updatePitStops(pitStr);
        }
      } catch (e) {}
    }
  }



  const offlineView = document.getElementById('offlineView');
  const liveView = document.getElementById('liveView');

  window.startSimulationFromOffline = function() {
    isLive = true;
    window.IS_SYNTHETIC_SIMULATION = true;
    if (offlineView) offlineView.style.display = 'none';
    if (liveView) liveView.style.display = 'flex';
    applyGenericRealSession(preferredCircuit, sessionType);
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
      const hdrEvent = document.getElementById('hdrEventTitle');
      if (hdrEvent) {
        const bTitle = window.FastestLapBridge.getEventTitle();
        if (bTitle && bTitle.trim().length > 0 && bTitle.toLowerCase() !== 'live timing') {
          hdrEvent.textContent = bTitle;
        }
      }
    }
    renderTableHeaders(sessionType);
    updateSessionStatusBar();
    resizeTrackCanvas();
    drawTrack();
    if (!lastAnimFrameTimestamp) requestAnimationFrame(animate);
    if (streamClient) streamClient.setMode('simulation');
  };

  if (!isLive) {
    if (offlineView) offlineView.style.display = 'flex';
    if (liveView) liveView.style.display = 'none';

    let displayEventTitle = 'FORMULA 1 GRAND PRIX';
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
      try {
        const bTitle = window.FastestLapBridge.getEventTitle();
        if (bTitle && bTitle.trim().length > 0 && bTitle.toLowerCase() !== 'live timing') {
          displayEventTitle = bTitle.trim();
        }
      } catch (e) {}
    }
    let displaySessionName = 'NESSUNA SESSIONE IN CORSO';
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionName === 'function') {
      try {
        const sName = window.FastestLapBridge.getSessionName();
        if (sName && sName.trim().length > 0) {
          displaySessionName = sName.trim().toUpperCase();
        }
      } catch (e) {}
    }

    const offTitle = document.getElementById('offlineTitle');
    const offEvent = document.getElementById('offlineEventTitle');
    const offSess = document.getElementById('offlineSessionName');
    const offDesc = document.getElementById('offlineDesc');
    let sessionElapsedSec = 0;
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionElapsedSeconds === 'function') {
      try {
        const es = window.FastestLapBridge.getSessionElapsedSeconds();
        if (!isNaN(es) && es >= 0) sessionElapsedSec = es;
      } catch (e) {}
    }
    const isPast = sessionElapsedSec > 0;
    if (offTitle) offTitle.textContent = isPast ? 'SESSIONE TERMINATA' : 'SESSIONE NON ANCORA INIZIATA';
    if (offEvent) offEvent.textContent = displayEventTitle;
    if (offSess) offSess.textContent = displaySessionName;
    if (offDesc && isPast) {
      offDesc.innerHTML = 'La sessione di pista è terminata.<br>I risultati ufficiali e i distacchi sono disponibili nella schermata dell\'evento.';
    }

    return; // Attesa utente: Torna all'evento o Avvia Collaudo
  }

  window.IS_SYNTHETIC_SIMULATION = false;
  if (offlineView) offlineView.style.display = 'none';
  if (liveView) liveView.style.display = 'flex';

  // Inizializzazione Streaming Client SignalR / Fallback
  if (typeof SignalRStreamClient !== 'undefined') {
    streamClient = new SignalRStreamClient();
    streamClient.onStatusChange((status, detail) => {
      const liveBadge = document.getElementById('liveBadgeIndicator');
      if (liveBadge) {
        if (status === 'live') {
          liveBadge.className = 'live-badge live-active';
          liveBadge.innerHTML = '<div class="live-dot"></div>LIVE';
        } else if (status === 'connecting') {
          liveBadge.className = 'live-badge';
          liveBadge.innerHTML = '<div class="live-dot" style="background:#e3b341;"></div>CONNECT';
        } else if (window.IS_SYNTHETIC_SIMULATION) {
          liveBadge.className = 'live-badge';
          liveBadge.innerHTML = '<div class="live-dot" style="background:#238636;"></div>COLLAUDO';
        } else {
          liveBadge.className = 'live-badge';
          liveBadge.innerHTML = '<div class="live-dot" style="background:#e3b341;"></div>ATTESA';
        }
      }
    });

    if (isLive) {
      streamClient.connect();
    }
  }

  // Configurazione dinamica generica della sessione
  applyGenericRealSession(preferredCircuit, sessionType);

  if (window.FastestLapBridge) {
    if (typeof window.FastestLapBridge.getEventTitle === 'function') {
      const hdrEvent = document.getElementById('hdrEventTitle');
      if (hdrEvent) {
        const bTitle = window.FastestLapBridge.getEventTitle();
        if (bTitle && bTitle.trim().length > 0 && bTitle.toLowerCase() !== 'live timing') {
          hdrEvent.textContent = bTitle;
        }
      }
    }
    if (typeof window.FastestLapBridge.getCircuitImageUrl === 'function') {
      const bImg = window.FastestLapBridge.getCircuitImageUrl();
      if (bImg && bImg.trim().length > 0) {
        window.setCircuitImage(bImg.trim());
      }
    }
    if (typeof window.FastestLapBridge.requestInitialFeeds === 'function') {
      window.FastestLapBridge.requestInitialFeeds();
    }
  }

  renderTableHeaders(sessionType);
  updateSessionStatusBar();
  checkSessionTermination();

  window.addEventListener('resize', () => {
    resizeTrackCanvas();
    drawTrack();
  });

  setupTrackMapInteractions();
  resizeTrackCanvas();

  requestAnimationFrame(animate);
}

window.onload = initApp;
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}

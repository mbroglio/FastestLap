/**
 * FastestLap — live_timing_app.js
 * Modulo Principale / Orchestratore Live Timing & Telemetria
 * Integra Android Bridge, parametri URL, ciclo di animazione e navigazione schermate.
 */

function updateSessionStatusBar() {
  const scActive = isSafetyCarActive(currentSecond);
  const vscActive = isVirtualSafetyCarActive(currentSecond);

  const flagBadge = document.getElementById('statusBarFlag');
  const flagIcon = document.getElementById('statusBarFlagIcon');
  const flagText = document.getElementById('statusBarFlagText');

  if (flagBadge && flagText && flagIcon) {
    flagBadge.className = 'status-bar-flag-badge';
    if (scActive) {
      flagBadge.classList.add('flag-sc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'SAFETY CAR';
    } else if (vscActive) {
      flagBadge.classList.add('flag-vsc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'VSC';
    } else {
      flagBadge.classList.add('flag-green');
      flagIcon.textContent = '🟢';
      flagText.textContent = 'BANDIERA VERDE';
    }
  }

  // Nome e Tipo Sessione
  const sessVal = document.getElementById('statusBarSessionVal');
  if (sessVal) {
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionName === 'function') {
      const bSess = window.FastestLapBridge.getSessionName();
      if (bSess && bSess.trim().length > 0 && bSess !== 'Sessione Live') {
        sessVal.textContent = bSess.toUpperCase().trim();
      } else if (sessionType === 'practice') {
        sessVal.textContent = sessionPart || 'PROVE LIBERE';
      } else if (sessionType === 'qualifying') {
        sessVal.textContent = sessionPart || 'QUALIFICHE';
      } else {
        sessVal.textContent = (sessionType === 'sprint' ? 'GARA SPRINT' : 'GARA');
      }
    } else if (sessionType === 'practice') {
      sessVal.textContent = sessionPart || 'PROVE LIBERE';
    } else if (sessionType === 'qualifying') {
      sessVal.textContent = sessionPart || 'QUALIFICHE';
    } else {
      sessVal.textContent = (sessionType === 'sprint' ? 'GARA SPRINT' : 'GARA');
    }
  }

  // Progresso Sessione / Conteggio Giri
  const lapVal = document.getElementById('statusBarLapVal');
  if (lapVal) {
    if (sessionType === 'practice') {
      lapVal.textContent = 'SESSIONE ATTIVA';
    } else if (sessionType === 'qualifying') {
      const isSq = (sessionPart && sessionPart.toUpperCase().startsWith('SQ'));
      const qPrefix = isSq ? 'SQ' : 'Q';
      const q1Cut = SESSION_DURATION * 0.35;
      const q2Cut = SESSION_DURATION * 0.70;
      let qPhase = qPrefix + '1';
      if (sessionPart === 'Q3' || sessionPart === 'SQ3') qPhase = qPrefix + '3';
      else if (sessionPart === 'Q2' || sessionPart === 'SQ2') qPhase = qPrefix + '2';
      else if (sessionPart === 'Q1' || sessionPart === 'SQ1') qPhase = qPrefix + '1';
      else if (currentSecond >= q2Cut) qPhase = qPrefix + '3';
      else if (currentSecond >= q1Cut) qPhase = qPrefix + '2';
      lapVal.textContent = qPhase + ' ATTIVA';
    } else {
      const curLap = getDriverLap(focusedDriver, currentSecond);
      lapVal.textContent = 'GIRO ' + curLap + '/' + TOTAL_LAPS;
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
    }, 50);
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

  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
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
  updateTimingTables();
  drawTrack();
}

function closeSession() {
  if (window.FastestLapBridge && typeof window.FastestLapBridge.closeLive === 'function') {
    window.FastestLapBridge.closeLive();
  } else {
    window.history.back();
  }
}

function applyGenericRealSession(circuitId, sType) {
  isGenericRealMode = true;
  currentCircuit = circuitId || 'generic';
  sessionType = sType || 'race';

  DRIVERS = { ...GRID_2026_DRIVERS };
  GRID_ORDER = Object.keys(DRIVERS);

  NODES = [];
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

  TOTAL_LAPS = 55;
  if (window.FastestLapBridge && typeof window.FastestLapBridge.getTotalLaps === 'function') {
    const tl = parseInt(window.FastestLapBridge.getTotalLaps(), 10);
    if (!isNaN(tl) && tl > 0) TOTAL_LAPS = tl;
  }
  LAP_DURATION = 90.0;
  SESSION_DURATION = sessionType === 'practice' ? 3600 : (sessionType === 'qualifying' ? 3600 : 7200);
  RACE_FINISH_SEC = SESSION_DURATION;

  buildGenericSessionLaps(sessionType);

  const now = new Date();
  const sessionElapsedSec = Math.max(120, Math.min(3540, ((now.getMinutes() >= 30 ? (now.getMinutes() - 30) : now.getMinutes()) * 60) + now.getSeconds()));
  currentSecond = sessionElapsedSec;

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
  const deltaSec = (timestamp - lastAnimFrameTimestamp) / 1000;
  lastAnimFrameTimestamp = timestamp;

  if (isPlaying) {
    currentSecond += deltaSec * simSpeedMultiplier;
    if (currentSecond >= SESSION_DURATION) {
      currentSecond = 0.0;
    }
  }

  drawTrack();

  if (timestamp - lastUiUpdateTime > 100) {
    lastUiUpdateTime = timestamp;
    updateSessionStatusBar();
    updateTimingTables();
    updateCockpit(1, selectedDriver1);
    updateCockpit(2, selectedDriver2);
    updateRaceControl();
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

  try {
    const urlParams = new URLSearchParams(window.location.search);
    const pCircuit = urlParams.get('circuit_id');
    const pImg = urlParams.get('circuit_image');
    const pType = urlParams.get('session_type');
    const pPart = urlParams.get('session_part');
    if (pCircuit && pCircuit.trim().length > 0) preferredCircuit = pCircuit.toLowerCase().trim();
    if (pType && pType.trim().length > 0) sessionType = pType.toLowerCase().trim();
    if (pPart && pPart.trim().length > 0) sessionPart = pPart.toUpperCase().trim();
    if (pImg && pImg.trim().length > 0) {
      window.setCircuitImage(pImg.trim());
    }
  } catch (ignored) {}

  if (window.FastestLapBridge) {
    if (typeof window.FastestLapBridge.getCircuitId === 'function') {
      try {
        const cid = window.FastestLapBridge.getCircuitId();
        if (cid && cid.trim().length > 0) preferredCircuit = cid.toLowerCase().trim();
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getSessionType === 'function') {
      try {
        const st = window.FastestLapBridge.getSessionType();
        if (st && st.trim().length > 0) sessionType = st.toLowerCase().trim();
      } catch (e) {}
    }
    if (typeof window.FastestLapBridge.getSessionPart === 'function') {
      try {
        const sp = window.FastestLapBridge.getSessionPart();
        if (sp && sp.trim().length > 0) sessionPart = sp.toUpperCase().trim();
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
  }

  const offlineView = document.getElementById('offlineView');
  const liveView = document.getElementById('liveView');
  if (offlineView) offlineView.style.display = 'none';
  if (liveView) liveView.style.display = 'flex';

  // Configurazione dinamica generica della sessione
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

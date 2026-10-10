/**
 * FastestLap — live_timing_app.js
 * Modulo Principale / Orchestratore Live Timing & Telemetria
 * Integra Android Bridge, parametri URL, ciclo di animazione e navigazione schermate.
 */

function updateSessionStatusBar() {
  const isFinished = (typeof SESSION_DURATION !== 'undefined' && currentSecond >= SESSION_DURATION) ||
                     (typeof RACE_FINISH_SEC !== 'undefined' && currentSecond >= RACE_FINISH_SEC);

  const scActive = !isFinished && isSafetyCarActive(currentSecond);
  const vscActive = !isFinished && isVirtualSafetyCarActive(currentSecond);

  const flagBadge = document.getElementById('statusBarFlag');
  const flagIcon = document.getElementById('statusBarFlagIcon');
  const flagText = document.getElementById('statusBarFlagText');

  if (flagBadge && flagText && flagIcon) {
    flagBadge.className = 'status-bar-flag-badge';
    if (isFinished) {
      flagBadge.classList.add('flag-chequered');
      flagIcon.textContent = '🏁';
      flagText.textContent = 'BANDIERA A SCACCHI';
    } else if (scActive) {
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
      if (TOTAL_LAPS && TOTAL_LAPS > 0) {
        lapVal.textContent = 'GIRO ' + curLap + '/' + TOTAL_LAPS;
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
  sessionType = typeof normalizeSessionType === 'function'
    ? normalizeSessionType(sType || sessionType || 'race', sessionPart)
    : (sType || sessionType || 'race');

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
  const deltaSec = (timestamp - lastAnimFrameTimestamp) / 1000;
  lastAnimFrameTimestamp = timestamp;

  if (isPlaying) {
    currentSecond += deltaSec * simSpeedMultiplier;
    if (currentSecond >= SESSION_DURATION) {
      currentSecond = SESSION_DURATION;
      isPlaying = false;
    }
  }

  drawTrack();

  if (timestamp - lastUiUpdateTime > 100) {
    lastUiUpdateTime = timestamp;
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getDriverTelemetryJson === 'function') {
      try {
        const telStr = window.FastestLapBridge.getDriverTelemetryJson();
        if (telStr && telStr.trim().length > 2) {
          window.updateDriverTelemetry(telStr);
        }
      } catch (e) {}
    }
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
    const isPast = sessionElapsedSec > 0;
    if (offTitle) offTitle.textContent = isPast ? 'SESSIONE TERMINATA' : 'SESSIONE NON ANCORA INIZIATA';
    if (offEvent) offEvent.textContent = displayEventTitle;
    if (offSess) offSess.textContent = displaySessionName;
    if (offDesc && isPast) {
      offDesc.innerHTML = 'La sessione di pista è terminata.<br>I risultati ufficiali e i distacchi sono disponibili nella schermata dell\'evento.';
    }

    return; // Sessione non live: non avviare simulazioni, non creare classifiche fasulle
  }

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

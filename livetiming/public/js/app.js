/**
 * FastestLap LiveTiming - Web Application Main Controller (Corretto)
 */

document.addEventListener('DOMContentLoaded', () => {
  const sessionTitleEl = document.getElementById('sessionTitle');
  const lapCountEl = document.getElementById('lapCount');
  const weatherAirEl = document.getElementById('weatherAir');
  const weatherTrackEl = document.getElementById('weatherTrack');
  const weatherRainEl = document.getElementById('weatherRain');
  const statusBadgeEl = document.getElementById('statusBadge');
  const rcFlagEl = document.getElementById('rcFlag');
  const rcTextEl = document.getElementById('rcText');

  const trackCanvas = new TrackCanvas(document.getElementById('trackCanvas'));

  // Analisi parametri URL passati dall'app o dal browser
  const urlParams = new URLSearchParams(window.location.search);
  const paramCircuitImage = urlParams.get('circuit_image');
  const paramEventTitle = urlParams.get('event_title');
  const paramSessionName = urlParams.get('session_name');
  const paramTotalLaps = urlParams.get('total_laps');

  if (paramCircuitImage) {
    trackCanvas.setCircuitImage(paramCircuitImage);
  }
  if (paramEventTitle) {
    sessionTitleEl.textContent = paramSessionName
      ? `${paramEventTitle.toUpperCase()} — ${paramSessionName.toUpperCase()}`
      : paramEventTitle.toUpperCase();
  }
  if (paramTotalLaps) {
    lapCountEl.textContent = `LAP 0 / ${paramTotalLaps}`;
  }

  let selectedDriver = null;
  let driversMap = {};
  let currentTelemetry = {};
  let currentLeaderboard = [];
  let hasDirectCarData = false;
  let currentSessionType = (urlParams.get('session_type') || paramSessionName || '').toLowerCase();
  let currentSessionName = paramSessionName || '';
  let currentSessionPart = urlParams.get('session_part') || null;
  let currentCircuitKey = null;
  const towerLapBadge = document.getElementById('towerLapBadge');

  const CIRCUIT_TOTAL_LAPS = {
    '9': 56, '63': 57, '149': 50, '1': 58, '10': 53, '7': 56, '151': 57,
    '6': 63, '22': 78, '23': 70, '4': 66, '19': 71, '2': 52, '14': 70,
    '20': 44, '55': 72, '39': 53, '144': 51, '61': 62, '65': 71, '15': 71,
    '152': 50, '150': 57, '70': 58,
    'austin': 56, 'cota': 56, 'united states': 56, 'usa': 56,
    'bahrain': 57, 'sakhir': 57,
    'jeddah': 50, 'saudi': 50,
    'melbourne': 58, 'albert park': 58, 'australia': 58,
    'suzuka': 53, 'japan': 53,
    'shanghai': 56, 'china': 56,
    'miami': 57,
    'imola': 63, 'emilia': 63,
    'monaco': 78, 'monte carlo': 78,
    'montreal': 70, 'canada': 70, 'villeneuve': 70,
    'barcelona': 66, 'catalunya': 66, 'spain': 66,
    'spielberg': 71, 'austria': 71, 'red bull ring': 71,
    'silverstone': 52, 'great britain': 52, 'britain': 52,
    'hungaroring': 70, 'hungary': 70, 'budapest': 70,
    'spa': 44, 'belgium': 44, 'francorchamps': 44,
    'zandvoort': 72, 'netherlands': 72, 'dutch': 72,
    'monza': 53, 'italy': 53,
    'baku': 51, 'azerbaijan': 51,
    'singapore': 62, 'marina bay': 62,
    'mexico': 71, 'rodriguez': 71,
    'interlagos': 71, 'brazil': 71, 'sao paulo': 71,
    'las vegas': 50, 'vegas': 50,
    'lusail': 57, 'losail': 57, 'qatar': 57,
    'yas marina': 58, 'abu dhabi': 58
  };

  function resolveCircuitTotalLaps(query) {
    if (!query) return 56;
    const str = String(query).toLowerCase().trim();
    if (CIRCUIT_TOTAL_LAPS[str]) return CIRCUIT_TOTAL_LAPS[str];
    for (const [k, v] of Object.entries(CIRCUIT_TOTAL_LAPS)) {
      if (str.includes(k)) return v;
    }
    return 56;
  }

  let currentLapCount = {
    CurrentLap: 0,
    TotalLaps: paramTotalLaps ? Number(paramTotalLaps) : resolveCircuitTotalLaps(paramSessionName || paramEventTitle)
  };

  function updateSessionPhaseBadge() {
    const sType = String(currentSessionType || currentSessionName || '').toLowerCase();
    const isQuali = sType.includes('qualifying') || sType.includes('qualifica') || sType.includes('shootout');
    const isPractice = sType.includes('practice') || sType.includes('prove') || sType.includes('fp');

    let badgeText = '';
    if (isQuali) {
      const isSq = sType.includes('sprint') || sType.includes('shootout') || String(currentSessionPart || '').toUpperCase().startsWith('SQ');
      const prefix = isSq ? 'SQ' : 'Q';
      const pStr = String(currentSessionPart || '').toUpperCase();
      if (pStr.startsWith('Q') || pStr.startsWith('SQ')) {
        badgeText = `PHASE ${pStr}`;
      } else {
        const partNum = Number(currentSessionPart) || 1;
        badgeText = `PHASE ${prefix}${partNum}`;
      }
    } else if (isPractice) {
      badgeText = (currentSessionName || 'PRACTICE').toUpperCase();
    } else {
      const curLap = (currentLapCount && currentLapCount.CurrentLap != null && currentLapCount.CurrentLap > 0)
        ? currentLapCount.CurrentLap
        : ((currentLeaderboard && currentLeaderboard[0] && currentLeaderboard[0].lapsCompleted)
          ? currentLeaderboard[0].lapsCompleted + 1
          : 1);
      let totLaps = (currentLapCount && Number(currentLapCount.TotalLaps) > 0)
        ? currentLapCount.TotalLaps
        : (paramTotalLaps ? Number(paramTotalLaps) : null);

      if (!totLaps || totLaps <= 0) {
        totLaps = resolveCircuitTotalLaps(currentCircuitKey || currentSessionName || paramEventTitle);
      }
      badgeText = `LAP ${curLap} / ${totLaps || '--'}`;
    }

    if (lapCountEl) lapCountEl.textContent = badgeText;
    if (towerLapBadge) towerLapBadge.textContent = badgeText;
  }

  const btnToggleCockpit = document.getElementById('btnToggleCockpit');
  const btnCloseCockpit = document.getElementById('btnCloseCockpit');
  const rcCockpitDriverBadge = document.getElementById('rcCockpitDriverBadge');
  const dashboardGrid = document.getElementById('dashboardGrid');

  function updateCockpitTriggerButton() {
    if (!btnToggleCockpit || !selectedDriver) return;
    const d = (driversMap && driversMap[selectedDriver]) || {};
    const tla = d.Tla || d.tla || selectedDriver;
    if (rcCockpitDriverBadge) {
      rcCockpitDriverBadge.textContent = `${tla} #${selectedDriver}`;
    }
    btnToggleCockpit.style.display = 'inline-flex';
    const isOpen = dashboardGrid && !dashboardGrid.classList.contains('cockpit-closed');
    btnToggleCockpit.classList.toggle('active', isOpen);
  }

  function setCockpitOpen(open) {
    if (!dashboardGrid) return;
    if (open) {
      dashboardGrid.classList.remove('cockpit-closed');
      if (btnToggleCockpit) btnToggleCockpit.classList.add('active');
    } else {
      dashboardGrid.classList.add('cockpit-closed');
      if (btnToggleCockpit) btnToggleCockpit.classList.remove('active');
    }
    setTimeout(() => {
      if (trackCanvas && typeof trackCanvas.resizeCanvas === 'function') {
        trackCanvas.resizeCanvas();
      }
    }, 40);
  }

  if (btnToggleCockpit) {
    btnToggleCockpit.addEventListener('click', () => {
      const isOpen = dashboardGrid && !dashboardGrid.classList.contains('cockpit-closed');
      setCockpitOpen(!isOpen);
    });
  }

  if (btnCloseCockpit) {
    btnCloseCockpit.addEventListener('click', () => {
      setCockpitOpen(false);
    });
  }

  // Session Termination Elements & Logic
  const sessionEndedBanner = document.getElementById('sessionEndedBanner');
  const sessionCloseCountdown = document.getElementById('sessionCloseCountdown');
  const btnExitLiveNow = document.getElementById('btnExitLiveNow');
  const sessionTerminatedModal = document.getElementById('sessionTerminatedModal');
  const stSessionSubtitle = document.getElementById('stSessionSubtitle');
  const stWinnerBox = document.getElementById('stWinnerBox');
  const stWinnerName = document.getElementById('stWinnerName');
  const stWinnerTeam = document.getElementById('stWinnerTeam');
  const stBtnCloseWindow = document.getElementById('stBtnCloseWindow');
  const stBtnViewFrozen = document.getElementById('stBtnViewFrozen');

  let sessionTerminatedTimer = null;
  let isSessionTerminated = false;
  let sessionTerminationTimestamp = null;
  const GRACE_PERIOD_MS = 180 * 1000; // 3 minuti di tolleranza post-termine

  function checkSessionTermination(snapOrStatus) {
    if (isSessionTerminated || !snapOrStatus) return;

    const stStr = String(
      (snapOrStatus.sessionStatus && snapOrStatus.sessionStatus.Status) ||
      snapOrStatus.sessionStatus ||
      (snapOrStatus.status && snapOrStatus.status.sessionStatus) ||
      ''
    ).toUpperCase();

    const isEnded = Boolean(
      snapOrStatus.isSessionEnded ||
      (snapOrStatus.status && snapOrStatus.status.isSessionEnded) ||
      ['FINALISED', 'FINISHED', 'ENDS', 'INACTIVE', 'ABORTED'].includes(stStr) ||
      (snapOrStatus.session && snapOrStatus.session.ArchiveStatus &&
        ['GENERATING', 'COMPLETE'].includes(String(snapOrStatus.session.ArchiveStatus.Status || '').toUpperCase()))
    );

    if (!isEnded) return;

    const endedIso = snapOrStatus.sessionEndedAt ||
      (snapOrStatus.status && snapOrStatus.status.sessionEndedAt) ||
      snapOrStatus.lastUpdate ||
      (snapOrStatus.status && snapOrStatus.status.lastUpdate);

    let endTs = endedIso ? new Date(endedIso).getTime() : Date.now();
    if (isNaN(endTs) || endTs <= 0) endTs = Date.now();

    if (!sessionTerminationTimestamp) {
      sessionTerminationTimestamp = endTs;
    }

    const elapsed = Date.now() - sessionTerminationTimestamp;
    if (elapsed >= GRACE_PERIOD_MS) {
      terminateLiveSession();
    } else if (!sessionTerminatedTimer) {
      startTerminationCountdown(sessionTerminationTimestamp, GRACE_PERIOD_MS);
    }
  }

  function startTerminationCountdown(endTs, totalGraceMs) {
    if (sessionEndedBanner) sessionEndedBanner.style.display = 'flex';
    statusBadgeEl.className = 'status-badge chequered';
    statusBadgeEl.textContent = '🏁 SESSIONE CONCLUSA';

    function tick() {
      const remainingMs = (endTs + totalGraceMs) - Date.now();
      if (remainingMs <= 0) {
        if (sessionTerminatedTimer) {
          clearInterval(sessionTerminatedTimer);
          sessionTerminatedTimer = null;
        }
        terminateLiveSession();
      } else {
        const sec = Math.ceil(remainingMs / 1000);
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        const fmt = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
        if (sessionCloseCountdown) {
          sessionCloseCountdown.textContent = `Chiusura live timing tra: ${fmt}`;
        }
      }
    }

    tick();
    sessionTerminatedTimer = setInterval(tick, 1000);
  }

  function terminateLiveSession() {
    isSessionTerminated = true;
    if (sessionTerminatedTimer) {
      clearInterval(sessionTerminatedTimer);
      sessionTerminatedTimer = null;
    }

    if (sessionEndedBanner) sessionEndedBanner.style.display = 'none';

    // Interrompe stream SSE e rendering
    if (eventSource) {
      eventSource.close();
      eventSource = null;
    }

    if (trackCanvas && typeof trackCanvas.stop === 'function') {
      trackCanvas.stop();
    }

    statusBadgeEl.className = 'status-badge chequered';
    statusBadgeEl.textContent = '🏁 TERMINATA';

    // Compila dati vincitore
    if (stSessionSubtitle) {
      stSessionSubtitle.textContent = sessionTitleEl.textContent || 'SESSIONE CONCLUSA';
    }
    if (currentLeaderboard && currentLeaderboard.length > 0) {
      const winner = currentLeaderboard[0];
      if (stWinnerBox) stWinnerBox.style.display = 'block';
      if (stWinnerName) stWinnerName.textContent = `#${winner.racingNumber} ${winner.fullName || winner.broadcastName || winner.tla}`;
      if (stWinnerTeam) stWinnerTeam.textContent = winner.teamName || 'Formula 1';
    }

    if (sessionTerminatedModal) {
      sessionTerminatedModal.style.display = 'flex';
    }
  }

  if (btnExitLiveNow) {
    btnExitLiveNow.addEventListener('click', () => {
      terminateLiveSession();
    });
  }

  if (stBtnCloseWindow) {
    stBtnCloseWindow.addEventListener('click', () => {
      if (window.opener) {
        window.close();
      } else if (window.history.length > 1) {
        window.history.back();
      } else {
        window.location.href = '/';
      }
    });
  }

  if (stBtnViewFrozen) {
    stBtnViewFrozen.addEventListener('click', () => {
      if (sessionTerminatedModal) sessionTerminatedModal.style.display = 'none';
    });
  }

  const cockpit = new CockpitDashboard(document.getElementById('cockpitContainer'));

  function handleSelectDriver(driverNum) {
    selectedDriver = String(driverNum);
    trackCanvas.setSelectedDriver(selectedDriver);
    if (typeof timingTower.setSelected === 'function') {
      timingTower.setSelected(selectedDriver);
    }
    cockpit.setDriver(selectedDriver, driversMap[selectedDriver]);
    if (currentTelemetry[selectedDriver] && currentTelemetry[selectedDriver].speed !== undefined) {
      cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
    } else {
      updateSynthesizedCockpitForSelected();
    }
    updateCockpitTriggerButton();
  }

  const timingTower = new TimingTower(
    document.getElementById('timingTableBody'),
    (driverNum) => {
      handleSelectDriver(driverNum);
    }
  );

  trackCanvas.onSelectDriver = (driverNum) => {
    handleSelectDriver(driverNum);
  };

  let currentMode = 'live';

  function updateModeUI() {
    if (statusBadgeEl) {
      statusBadgeEl.className = 'status-badge live';
      statusBadgeEl.textContent = '● SIGNALR LIVE';
    }
  }

  async function syncTyres() {
    try {
      const resp = await fetch('/api/tyres');
      if (resp.ok) {
        const tyres = await resp.json();
        timingTower.updateTyres(tyres);
      }
    } catch (e) {}
  }

  function extractSpeedVal(speedEntry) {
    if (!speedEntry) return 0;
    const raw = typeof speedEntry === 'object' ? speedEntry.Value : speedEntry;
    const num = parseFloat(raw);
    return !isNaN(num) && num > 0 ? num : 0;
  }

  /**
   * Sintetizza la telemetria del cruscotto con corrispondenza 1:1 rispetto alla posizione
   * fisica istantanea della vettura sul circuito (modello QSS: staccate, apice curva, trazione, rettilinei)
   * calibrata con le speed trap reali (Speeds I1/I2/FL/ST) e lo stato InPit/PitOut di TimingData.
   */
  function synthesizeTelemetryFromTiming(row) {
    if (!row) return null;

    if (row.inPit || row.status === 'PIT' || row.retired || row.status === 'RET') {
      return {
        speed: 0,
        rpm: 0,
        gear: 0,
        throttle: 0,
        brake: 0,
        brakeRaw: 0,
        drsState: 'OFF',
        drsActive: false,
        ersMode: 'NONE',
        battery: 100
      };
    }

    const metrics = trackCanvas.getDriverTrackMetrics(row.racingNumber);

    if ((row.pitOut || row.status === 'OUT') && (metrics.distMeters || 0) < 320) {
      return {
        speed: 80,
        rpm: 9650,
        gear: 2,
        throttle: 42,
        brake: 0,
        brakeRaw: 0,
        drsState: 'OFF',
        drsActive: false,
        ersMode: 'RECHARGE',
        battery: 96
      };
    }

    const speeds = row.speeds || {};
    const stSpeed = extractSpeedVal(speeds.ST) || 322;
    const i1Speed = extractSpeedVal(speeds.I1) || 280;
    const i2Speed = extractSpeedVal(speeds.I2) || 270;
    const flSpeed = extractSpeedVal(speeds.FL) || 290;

    const prog = metrics.progress || 0;
    const longG = metrics.longG || 0;
    const kappa = metrics.kappa || 0;
    const basePhysSpeed = metrics.physicsSpeedKmh || 240;

    // Calibra il picco velocistico sul riferimento reale della speed trap del pilota
    const driverTopTrap = Math.max(295, Math.min(352, Math.max(stSpeed, flSpeed, i1Speed, i2Speed)));
    const trapScale = driverTopTrap / 326;
    const speed = Math.round(
      Math.max(82, Math.min(352, basePhysSpeed > 230 ? basePhysSpeed * trapScale : basePhysSpeed))
    );

    // Marcia F1 (8 rapporti) accoppiata alla velocità reale nel punto del circuito
    let gear = 8;
    if (speed < 106) gear = 2;
    else if (speed < 140) gear = 3;
    else if (speed < 176) gear = 4;
    else if (speed < 212) gear = 5;
    else if (speed < 248) gear = 6;
    else if (speed < 286) gear = 7;
    else gear = 8;

    const gearMinSpeeds = [0, 0, 78, 106, 140, 176, 212, 248, 286];
    const gearMaxSpeeds = [0, 82, 114, 148, 184, 220, 256, 294, 348];
    const gMin = gearMinSpeeds[gear] || 80;
    const gMax = gearMaxSpeeds[gear] || 340;
    const gearRatio = Math.max(0, Math.min(1, (speed - gMin) / Math.max(1, gMax - gMin)));

    // Freno e Acceleratore ricavati direttamente dalla dinamica longitudinale (longG) e curvatura (kappa) nel punto mappa
    let brakeRaw = 0;
    let throttle = 100;

    if (longG < -0.28) {
      // Zona di staccata prima della curva: freno proporzionale alla decelerazione in G
      brakeRaw = Math.min(100, Math.max(15, Math.round((-longG / 2.8) * 100)));
      throttle = longG < -0.85 ? 0 : Math.max(0, Math.round(25 * (1 + longG)));
    } else if (kappa > 0.0085) {
      // Apice curva stretta / media: rilascio freno e parzializzazione acceleratore in percorrenza
      brakeRaw = 0;
      const apexLoad = Math.min(1, (kappa - 0.0085) / 0.025);
      throttle = Math.round(85 - apexLoad * 48);
    } else if (kappa > 0.003) {
      // Curva veloce in appoggio
      brakeRaw = 0;
      throttle = Math.round(92 - Math.min(1, (kappa - 0.003) / 0.006) * 15);
    } else {
      // Rettilineo / piena trazione fuori dalla curva
      brakeRaw = 0;
      throttle = 100;
    }

    // RPM motore sincronizzato con rapporto marcia e fase di staccata/trazione
    const rpmBase = brakeRaw > 30 ? 9650 : 10050;
    const rpmSpan = brakeRaw > 30 ? 1850 : 2350;
    const rpm = Math.round(rpmBase + gearRatio * rpmSpan);

    // DRS attivo esclusivamente sui rettilinei ad alta velocità fuori dalle zone di frenata
    const drsOpen = metrics.isOnStraight && brakeRaw === 0 && throttle >= 95 && speed > 276;
    const drsAvailable = !drsOpen && metrics.isOnStraight && brakeRaw === 0 && speed > 235;
    const drsState = drsOpen ? 'OPEN' : (drsAvailable ? 'AVAILABLE' : 'OFF');

    // ERS: ricarica MGU-K in staccata, erogazione su rettilineo e uscita curva
    const ersMode = brakeRaw > 25 ? 'RECHARGE' : (drsOpen ? 'OVERTAKE' : (throttle >= 95 ? 'HOTLAP' : 'BALANCED'));
    const battery = Math.max(18, Math.min(100, Math.round(62 + 32 * Math.cos(prog * 2 * Math.PI) + (brakeRaw > 25 ? 4 : 0))));

    return {
      speed,
      rpm,
      gear,
      throttle,
      brake: brakeRaw > 0 ? 1 : 0,
      brakeRaw,
      drsState,
      drsActive: drsOpen,
      ersMode,
      battery
    };
  }

  function updateSynthesizedCockpitForSelected() {
    if (hasDirectCarData || !selectedDriver || currentLeaderboard.length === 0) return;
    const row = currentLeaderboard.find(r => String(r.racingNumber) === String(selectedDriver));
    if (!row) return;
    const synth = synthesizeTelemetryFromTiming(row);
    if (synth) {
      cockpit.updateTelemetry(synth);
    }
  }

  // Loop ad alta frequenza (12.5Hz / 80ms) per mantenere il cruscotto telemetrico
  // perfettamente sincronizzato con la posizione istantanea a 60fps della vettura sul circuito
  setInterval(() => {
    if (currentLeaderboard.length > 0) {
      trackCanvas.updatePositionsFromLeaderboard(currentLeaderboard);
      updateSynthesizedCockpitForSelected();
    }
  }, 80);

  let eventSource = null;

  function connectSSE() {
    if (eventSource) eventSource.close();

    eventSource = new EventSource('/api/stream');

    eventSource.addEventListener('INIT', (e) => {
      try {
        applySnapshot(JSON.parse(e.data));
        syncTyres();
      } catch (err) {}
    });

    eventSource.addEventListener('SNAPSHOT', (e) => {
      try {
        applySnapshot(JSON.parse(e.data));
        syncTyres();
      } catch (err) {}
    });

    eventSource.addEventListener('TICK', (e) => {
      try {
        const tick = JSON.parse(e.data);
        if (tick.positions && Object.keys(tick.positions).length > 0) {
          trackCanvas.updatePositions(tick.positions);
        }
        if (tick.telemetry && Object.keys(tick.telemetry).length > 0) {
          hasDirectCarData = true;
          Object.assign(currentTelemetry, tick.telemetry);
          if (selectedDriver && currentTelemetry[selectedDriver]) {
            cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
          }
        }
        if (tick.leaderboard) {
          currentLeaderboard = tick.leaderboard;
          timingTower.updateLeaderboard(tick.leaderboard);
          trackCanvas.updatePositionsFromLeaderboard(currentLeaderboard);
        }
      } catch (err) {}
    });

    eventSource.addEventListener('TIMING', (e) => {
      try {
        const lb = JSON.parse(e.data);
        currentLeaderboard = lb;
        timingTower.updateLeaderboard(lb);
        trackCanvas.updatePositionsFromLeaderboard(currentLeaderboard);
        updateSynthesizedCockpitForSelected();
      } catch (err) {}
    });

    eventSource.addEventListener('TYRES', (e) => {
      try { timingTower.updateTyres(JSON.parse(e.data)); } catch (err) {}
    });

    eventSource.addEventListener('TELEMETRY', (e) => {
      try {
        const tel = JSON.parse(e.data);
        if (tel && Object.keys(tel).length > 0) {
          hasDirectCarData = true;
          Object.assign(currentTelemetry, tel);
          if (selectedDriver && currentTelemetry[selectedDriver]) {
            cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
          }
        }
      } catch (err) {}
    });

    eventSource.addEventListener('POSITIONS', (e) => {
      try {
        const pos = JSON.parse(e.data);
        if (pos && Object.keys(pos).length > 0) {
          trackCanvas.updatePositions(pos);
        }
      } catch (err) {}
    });

    eventSource.addEventListener('DRIVERS', (e) => {
      try {
        const data = JSON.parse(e.data);
        driversMap = data;
        trackCanvas.updateDrivers(driversMap);
      } catch (err) {}
    });

    eventSource.addEventListener('WEATHER', (e) => {
      try {
        const w = JSON.parse(e.data);
        if (w.AirTemp) weatherAirEl.textContent = `${w.AirTemp}°C`;
        if (w.TrackTemp) weatherTrackEl.textContent = `${w.TrackTemp}°C`;
        if (w.Rainfall !== undefined) weatherRainEl.textContent = `${w.Rainfall}%`;
      } catch (err) {}
    });

    eventSource.addEventListener('LAPS', (e) => {
      try {
        const laps = JSON.parse(e.data);
        if (laps) {
          if (!currentLapCount) currentLapCount = {};
          if (laps.CurrentLap !== undefined) currentLapCount.CurrentLap = Number(laps.CurrentLap);
          if (Number(laps.TotalLaps) > 0) currentLapCount.TotalLaps = Number(laps.TotalLaps);
          updateSessionPhaseBadge();
        }
      } catch (err) {}
    });

    eventSource.addEventListener('SESSION', (e) => {
      try {
        const s = JSON.parse(e.data);
        currentSessionType = String(s.Type || s.Name || paramSessionName || '').toLowerCase();
        currentSessionName = s.Name || s.Type || currentSessionName;
        const isQuali = currentSessionType.includes('qualifying') || currentSessionType.includes('qualifica') || currentSessionType.includes('shootout');
        timingTower.setSessionContext({ isQualifying: isQuali });
        updateSessionPhaseBadge();
        checkSessionTermination({ session: s });
        if (s.Meeting) {
          if (!paramEventTitle) {
            const sName = s.Name || s.Type || 'Session';
            sessionTitleEl.textContent = `${s.Meeting.Name || 'Formula 1'} — ${sName.toUpperCase()}`;
          }
          if (s.Meeting.Circuit && s.Meeting.Circuit.Key) {
            currentCircuitKey = String(s.Meeting.Circuit.Key);
            trackCanvas.loadCircuitByKey(s.Meeting.Circuit.Key);
          }
        }
      } catch (err) {}
    });

    eventSource.addEventListener('SESSION_STATUS', (e) => {
      try {
        const st = JSON.parse(e.data);
        if (st && st.Status) {
          timingTower.setSessionContext({ sessionStatus: st.Status });
          checkSessionTermination({ sessionStatus: st });
        }
      } catch (err) {}
    });

    eventSource.addEventListener('TRACK_STATUS', (e) => {
      try {
        const ts = JSON.parse(e.data);
        if (ts) {
          updateTrackStatusUI(ts);
        }
      } catch (err) {}
    });

    eventSource.addEventListener('RACE_CONTROL', (e) => {
      try {
        const msgs = JSON.parse(e.data);
        if (Array.isArray(msgs) && msgs.length > 0) {
          const hasChequered = msgs.slice(-5).some(m => m && String(m.Flag || '').toUpperCase() === 'CHEQUERED');
          timingTower.setSessionContext({ isChequered: hasChequered });
          updateRaceControlUI(msgs[msgs.length - 1]);
        }
      } catch (err) {}
    });

    eventSource.onerror = () => {
      // Se il server locale non è raggiungibile, passa automaticamente a F1 SignalR diretto nel browser
      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }
      console.warn('[FastestLap] Server locale SSE non disponibile. Fallback a F1 SignalR diretto nel browser...');
      connectDirectWebSignalR();
    };
  }

  let clientSideStateStore = null;
  let directWebSignalRClient = null;

  async function checkLocalServer() {
    if (window.location.protocol === 'file:') return false;
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 500);
      const res = await fetch('/api/status', { signal: controller.signal });
      clearTimeout(timeoutId);
      return res.ok;
    } catch (e) {
      return false;
    }
  }

  function connectDirectWebSignalR() {
    if (!window.StateStore || !window.SignalRWebClient) {
      console.warn('[FastestLap] Modulo StateStore o SignalRWebClient non caricato.');
      return;
    }

    if (!clientSideStateStore) {
      clientSideStateStore = new window.StateStore();
    }

    if (directWebSignalRClient) {
      directWebSignalRClient.disconnect();
    }

    directWebSignalRClient = new window.SignalRWebClient(clientSideStateStore, {
      onStatusChange: (status, label) => {
        statusBadgeEl.className = `status-badge ${status}`;
        statusBadgeEl.textContent = label;
      },
      onDataUpdate: (type, snapshot) => {
        applySnapshot(snapshot);
      }
    });

    directWebSignalRClient.connect();
  }

  async function initConnection() {
    const hasLocalServer = await checkLocalServer();
    if (hasLocalServer) {
      console.log('[FastestLap] Server locale di test rilevato. Connessione SSE /api/stream...');
      connectSSE();
    } else {
      console.log('[FastestLap] Server locale non in esecuzione. Connessione diretta Web -> F1 SignalR...');
      connectDirectWebSignalR();
    }
  }

  function applySnapshot(snap) {
    if (!snap) return;

    if (snap.status) {
      currentMode = 'live';
      if (snap.status.sessionPart) {
        currentSessionPart = snap.status.sessionPart;
      }
      updateModeUI();
    }

    if (snap.session) {
      currentSessionType = String(snap.session.Type || snap.session.Name || paramSessionName || '').toLowerCase();
      currentSessionName = snap.session.Name || snap.session.Type || currentSessionName;
      const isQuali = currentSessionType.includes('qualifying') || currentSessionType.includes('qualifica') || currentSessionType.includes('shootout');
      const hasChequered = Array.isArray(snap.raceControl) && snap.raceControl.slice(-5).some(m => m && String(m.Flag || '').toUpperCase() === 'CHEQUERED');
      const stStr = (snap.sessionStatus && snap.sessionStatus.Status) || (snap.status && snap.status.sessionStatus) || '';
      timingTower.setSessionContext({
        isQualifying: isQuali,
        isChequered: hasChequered,
        sessionStatus: stStr
      });
      if (snap.session.Meeting) {
        if (!paramEventTitle) {
          const sessionName = snap.session.Name || snap.session.Type || 'Session';
          sessionTitleEl.textContent = `${snap.session.Meeting.Name || 'Formula 1'} — ${sessionName.toUpperCase()}`;
        }
        if (snap.session.Meeting.Circuit && snap.session.Meeting.Circuit.Key) {
          currentCircuitKey = String(snap.session.Meeting.Circuit.Key);
          trackCanvas.loadCircuitByKey(snap.session.Meeting.Circuit.Key);
        }
      }
    }

    if (snap.lapCount) {
      if (!currentLapCount) currentLapCount = {};
      if (snap.lapCount.CurrentLap !== undefined) currentLapCount.CurrentLap = Number(snap.lapCount.CurrentLap);
      if (Number(snap.lapCount.TotalLaps) > 0) currentLapCount.TotalLaps = Number(snap.lapCount.TotalLaps);
    }
    if (snap.status && Number(snap.status.totalLaps) > 0 && (!currentLapCount.TotalLaps || currentLapCount.TotalLaps <= 0)) {
      currentLapCount.TotalLaps = Number(snap.status.totalLaps);
    }
    updateSessionPhaseBadge();

    if (snap.weather) {
      if (snap.weather.AirTemp) weatherAirEl.textContent = `${snap.weather.AirTemp}°C`;
      if (snap.weather.TrackTemp) weatherTrackEl.textContent = `${snap.weather.TrackTemp}°C`;
      if (snap.weather.Rainfall !== undefined) weatherRainEl.textContent = `${snap.weather.Rainfall}%`;
    }

    if (snap.drivers && Object.keys(snap.drivers).length > 0) {
      driversMap = snap.drivers;
      trackCanvas.updateDrivers(driversMap);

      if (!selectedDriver || !driversMap[selectedDriver]) {
        selectedDriver = Object.keys(driversMap)[0];
      }
      cockpit.setDriver(selectedDriver, driversMap[selectedDriver]);
      trackCanvas.setSelectedDriver(selectedDriver);
      updateCockpitTriggerButton();
    }

    if (snap.leaderboard) {
      currentLeaderboard = snap.leaderboard;
      timingTower.updateLeaderboard(snap.leaderboard);
      if (selectedDriver) timingTower.setSelected(selectedDriver);
      trackCanvas.updatePositionsFromLeaderboard(currentLeaderboard);
    }

    if (snap.positions && Object.keys(snap.positions).length > 0) {
      trackCanvas.updatePositions(snap.positions);
    }

    if (snap.telemetry && Object.keys(snap.telemetry).length > 0) {
      hasDirectCarData = true;
      currentTelemetry = snap.telemetry;
      if (selectedDriver && currentTelemetry[selectedDriver]) {
        cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
      }
    } else {
      updateSynthesizedCockpitForSelected();
    }

    if (snap.trackStatus) {
      updateTrackStatusUI(snap.trackStatus);
    }

    if (snap.raceControl && snap.raceControl.length > 0) {
      updateRaceControlUI(snap.raceControl[snap.raceControl.length - 1]);
    }

    checkSessionTermination(snap);
  }

  function updateTrackStatusUI(ts) {
    if (!ts) return;
    const code = String(ts.Status || '1');
    if (code === '5') {
      rcFlagEl.textContent = 'RED';
      rcFlagEl.className = 'rc-flag red';
      rcTextEl.textContent = 'RED FLAG — SESSION SUSPENDED';
    } else if (code === '4') {
      rcFlagEl.textContent = 'SC';
      rcFlagEl.className = 'rc-flag sc';
      rcTextEl.textContent = 'SAFETY CAR DEPLOYED';
    } else if (code === '6' || code === '7') {
      rcFlagEl.textContent = 'VSC';
      rcFlagEl.className = 'rc-flag sc';
      rcTextEl.textContent = code === '7' ? 'VIRTUAL SAFETY CAR ENDING' : 'VIRTUAL SAFETY CAR DEPLOYED';
    } else if (code === '2') {
      rcFlagEl.textContent = 'YELLOW';
      rcFlagEl.className = 'rc-flag yellow';
    } else if (code === '1') {
      rcFlagEl.textContent = 'GREEN';
      rcFlagEl.className = 'rc-flag green';
    }
  }

  function updateRaceControlUI(msg) {
    if (!msg) return;
    const text = msg.Message || 'SESSION IN PROGRESS';
    rcTextEl.textContent = text;
    const upperText = text.toUpperCase();
    const rawFlag = String(msg.Flag || '').toUpperCase();
    const rawCat = String(msg.Category || '').toUpperCase();

    let badge = 'GREEN';
    let cls = 'green';
    if (rawFlag.includes('RED') || upperText.includes('RED FLAG')) {
      badge = 'RED';
      cls = 'red';
    } else if (rawCat === 'SAFETYCAR' || upperText.includes('VIRTUAL SAFETY CAR') || upperText.includes('VSC')) {
      badge = upperText.includes('VIRTUAL') || upperText.includes('VSC') ? 'VSC' : 'SC';
      cls = 'sc';
    } else if (upperText.includes('SAFETY CAR')) {
      badge = 'SC';
      cls = 'sc';
    } else if (rawFlag.includes('YELLOW') || upperText.includes('YELLOW')) {
      badge = 'YELLOW';
      cls = 'yellow';
    } else if (rawFlag === 'CHEQUERED' || upperText.includes('CHEQUERED')) {
      badge = 'FINISH';
      cls = 'green';
    }

    rcFlagEl.textContent = badge;
    rcFlagEl.className = `rc-flag ${cls}`;
  }

  initConnection();
});

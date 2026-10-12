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
  const btnToggleMode = document.getElementById('btnToggleMode');
  const rcFlagEl = document.getElementById('rcFlag');
  const rcTextEl = document.getElementById('rcText');

  const trackCanvas = new TrackCanvas(document.getElementById('trackCanvas'));
  
  let selectedDriver = null;
  let driversMap = {};
  let currentTelemetry = {};

  const cockpit = new CockpitDashboard(document.getElementById('cockpitContainer'));

  const timingTower = new TimingTower(
    document.getElementById('timingTableBody'),
    (driverNum) => {
      selectedDriver = String(driverNum);
      trackCanvas.setSelectedDriver(selectedDriver);
      cockpit.setDriver(selectedDriver, driversMap[selectedDriver]);
      if (currentTelemetry[selectedDriver]) {
        cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
      }
    }
  );

  let currentMode = 'mock';

  btnToggleMode.addEventListener('click', async () => {
    const nextMode = currentMode === 'live' ? 'mock' : 'live';
    try {
      btnToggleMode.disabled = true;
      const resp = await fetch('/api/mode', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ mode: nextMode })
      });
      const data = await resp.json();
      if (data.success) {
        currentMode = data.mode;
        updateModeUI();
      }
    } catch (e) {
      console.error('Errore cambio modalità:', e);
    } finally {
      btnToggleMode.disabled = false;
    }
  });

  function updateModeUI() {
    if (currentMode === 'live') {
      statusBadgeEl.className = 'status-badge live';
      statusBadgeEl.textContent = '● SIGNALR LIVE';
      btnToggleMode.textContent = 'Passa a Simulazione';
    } else {
      statusBadgeEl.className = 'status-badge simulation';
      statusBadgeEl.textContent = '⚡ SIMULATION';
      btnToggleMode.textContent = 'Passa a Live Stream';
    }
  }

  let eventSource = null;

  function connectSSE() {
    if (eventSource) eventSource.close();

    eventSource = new EventSource('/api/stream');

    eventSource.addEventListener('INIT', (e) => {
      try { applySnapshot(JSON.parse(e.data)); } catch (err) {}
    });

    eventSource.addEventListener('SNAPSHOT', (e) => {
      try { applySnapshot(JSON.parse(e.data)); } catch (err) {}
    });

    eventSource.addEventListener('TICK', (e) => {
      try {
        const tick = JSON.parse(e.data);
        if (tick.positions) trackCanvas.updatePositions(tick.positions);
        if (tick.telemetry) {
          Object.assign(currentTelemetry, tick.telemetry);
          if (selectedDriver && currentTelemetry[selectedDriver]) {
            cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
          }
        }
        if (tick.leaderboard) timingTower.updateLeaderboard(tick.leaderboard);
      } catch (err) {}
    });

    eventSource.addEventListener('TIMING', (e) => {
      try { timingTower.updateLeaderboard(JSON.parse(e.data)); } catch (err) {}
    });

    eventSource.addEventListener('TELEMETRY', (e) => {
      try {
        const tel = JSON.parse(e.data);
        Object.assign(currentTelemetry, tel);
        if (selectedDriver && currentTelemetry[selectedDriver]) {
          cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
        }
      } catch (err) {}
    });

    eventSource.addEventListener('POSITIONS', (e) => {
      try { trackCanvas.updatePositions(JSON.parse(e.data)); } catch (err) {}
    });

    eventSource.addEventListener('RACE_CONTROL', (e) => {
      try {
        const msgs = JSON.parse(e.data);
        if (Array.isArray(msgs) && msgs.length > 0) {
          updateRaceControlUI(msgs[msgs.length - 1]);
        }
      } catch (err) {}
    });

    eventSource.addEventListener('MODE_CHANGE', (e) => {
      try {
        const data = JSON.parse(e.data);
        currentMode = data.mode;
        updateModeUI();
      } catch (err) {}
    });

    eventSource.onerror = () => {
      statusBadgeEl.className = 'status-badge disconnected';
      statusBadgeEl.textContent = 'RICONNESSIONE...';
    };
  }

  function applySnapshot(snap) {
    if (!snap) return;

    if (snap.status) {
      currentMode = snap.status.mode || currentMode;
      updateModeUI();
    }

    if (snap.session && snap.session.Meeting) {
      const sessionName = snap.session.Name || snap.session.Type || 'Session';
      sessionTitleEl.textContent = `${snap.session.Meeting.Name || 'Formula 1'} — ${sessionName.toUpperCase()}`;
    }

    if (snap.lapCount) {
      lapCountEl.textContent = `LAP ${snap.lapCount.CurrentLap} / ${snap.lapCount.TotalLaps}`;
    }

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
    }

    if (snap.leaderboard) {
      timingTower.updateLeaderboard(snap.leaderboard);
      if (selectedDriver) timingTower.setSelected(selectedDriver);
    }

    if (snap.positions) {
      trackCanvas.updatePositions(snap.positions);
    }

    if (snap.telemetry) {
      currentTelemetry = snap.telemetry;
      if (selectedDriver && currentTelemetry[selectedDriver]) {
        cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
      }
    }

    if (snap.raceControl && snap.raceControl.length > 0) {
      updateRaceControlUI(snap.raceControl[snap.raceControl.length - 1]);
    }
  }

  function updateRaceControlUI(msg) {
    if (!msg) return;
    rcTextEl.textContent = msg.Message || 'SESSION IN PROGRESS';
    const flag = (msg.Flag || (msg.Category === 'Flag' ? msg.Flag : 'GREEN')).toUpperCase();
    rcFlagEl.textContent = flag;
    rcFlagEl.className = 'rc-flag ' + (
      flag.includes('YELLOW') ? 'yellow' : (flag.includes('RED') ? 'red' : (flag.includes('SC') ? 'sc' : 'green'))
    );
  }

  connectSSE();
});
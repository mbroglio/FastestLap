/**
 * FastestLap LiveTiming - Web Application Main Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  // DOM Elements
  const sessionTitleEl = document.getElementById('sessionTitle');
  const lapCountEl = document.getElementById('lapCount');
  const weatherAirEl = document.getElementById('weatherAir');
  const weatherTrackEl = document.getElementById('weatherTrack');
  const weatherRainEl = document.getElementById('weatherRain');
  const statusBadgeEl = document.getElementById('statusBadge');
  const btnToggleMode = document.getElementById('btnToggleMode');
  const rcFlagEl = document.getElementById('rcFlag');
  const rcTextEl = document.getElementById('rcText');

  // Sub-controllers
  const trackCanvas = new TrackCanvas(document.getElementById('trackCanvas'));
  
  let selectedDriver = '1';
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

  // Toggle Mode Button
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
      console.error('Failed to switch mode:', e);
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

  // ─────────────────────────────────────────────────────────────
  // Server-Sent Events (SSE) Stream Receiver
  // ─────────────────────────────────────────────────────────────
  let eventSource = null;

  function connectSSE() {
    if (eventSource) eventSource.close();

    eventSource = new EventSource('/api/stream');

    eventSource.addEventListener('INIT', (e) => {
      const snap = JSON.parse(e.data);
      applySnapshot(snap);
    });

    eventSource.addEventListener('TICK', (e) => {
      const tick = JSON.parse(e.data);
      if (tick.positions) trackCanvas.updatePositions(tick.positions);
      if (tick.telemetry) {
        Object.assign(currentTelemetry, tick.telemetry);
        if (currentTelemetry[selectedDriver]) {
          cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
        }
      }
      if (tick.leaderboard) timingTower.updateLeaderboard(tick.leaderboard);
    });

    eventSource.addEventListener('TIMING', (e) => {
      const lb = JSON.parse(e.data);
      timingTower.updateLeaderboard(lb);
    });

    eventSource.addEventListener('TELEMETRY', (e) => {
      const tel = JSON.parse(e.data);
      Object.assign(currentTelemetry, tel);
      if (currentTelemetry[selectedDriver]) {
        cockpit.updateTelemetry(currentTelemetry[selectedDriver]);
      }
    });

    eventSource.addEventListener('POSITIONS', (e) => {
      const pos = JSON.parse(e.data);
      trackCanvas.updatePositions(pos);
    });

    eventSource.addEventListener('RACE_CONTROL', (e) => {
      const msgs = JSON.parse(e.data);
      if (Array.isArray(msgs) && msgs.length > 0) {
        const last = msgs[msgs.length - 1];
        updateRaceControlUI(last);
      }
    });

    eventSource.addEventListener('MODE_CHANGE', (e) => {
      const data = JSON.parse(e.data);
      currentMode = data.mode;
      updateModeUI();
    });

    eventSource.onerror = () => {
      statusBadgeEl.className = 'status-badge disconnected';
      statusBadgeEl.textContent = 'RICONNESSIONE...';
      // Attempt fallback polling
      pollFallback();
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

    if (snap.drivers) {
      driversMap = snap.drivers;
      trackCanvas.updateDrivers(driversMap);
      if (!selectedDriver && Object.keys(driversMap).length > 0) {
        selectedDriver = Object.keys(driversMap)[0];
      }
      cockpit.setDriver(selectedDriver, driversMap[selectedDriver]);
      trackCanvas.setSelectedDriver(selectedDriver);
    }

    if (snap.leaderboard) {
      timingTower.updateLeaderboard(snap.leaderboard);
      timingTower.setSelected(selectedDriver);
    }

    if (snap.positions) {
      trackCanvas.updatePositions(snap.positions);
    }

    if (snap.telemetry) {
      currentTelemetry = snap.telemetry;
      if (currentTelemetry[selectedDriver]) {
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

  async function pollFallback() {
    try {
      const res = await fetch('/api/snapshot');
      if (res.ok) {
        const snap = await res.json();
        applySnapshot(snap);
      }
    } catch (e) {}
  }

  // Initial connect
  connectSSE();
});

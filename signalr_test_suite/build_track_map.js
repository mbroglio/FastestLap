const fs = require('fs');
const path = require('path');
const config = require('./src/config');
const raceModel = require('./src/race_model');
const { RACE_CONTROL_MESSAGES } = require('./src/race_control_events');

const nodes = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'data/sepang_exact_track_full.json'), 'utf-8')
);

// Compact nodes: [t, px, py, x, y, sector, speed, gear, rpm, throttle, brake, drs, location]
const compactNodes = nodes.map(n => [
  n.t,
  Math.round(n.px * 100) / 100,
  Math.round(n.py * 100) / 100,
  Math.round(n.x),
  Math.round(n.y),
  n.sector,
  n.speed,
  n.gear,
  n.rpm,
  n.throttle,
  n.brake,
  n.drs,
  n.location
]);

// Dedicated Sepang data files
const drivers = JSON.parse(fs.readFileSync(path.join(__dirname, 'data/sepang_drivers.json'), 'utf-8'));
const driversJson = JSON.stringify(drivers, null, 2);
const driverKeyframesJson = fs.readFileSync(path.join(__dirname, 'data/sepang_keyframes.json'), 'utf-8');
const pitStopsJson = fs.readFileSync(path.join(__dirname, 'data/sepang_pit_stops.json'), 'utf-8');
const raceControlJson = fs.readFileSync(path.join(__dirname, 'data/sepang_race_control_messages.json'), 'utf-8');
const retirementsJson = fs.readFileSync(path.join(__dirname, 'data/sepang_retirements.json'), 'utf-8');
const lapStartsJson = JSON.stringify(raceModel.LAP_STARTS);
const driverStintsJson = fs.readFileSync(path.join(__dirname, 'data/sepang_driver_stints.json'), 'utf-8');
const driverLapsJson = fs.readFileSync(path.join(__dirname, 'data/sepang_driver_laps.json'), 'utf-8');
const raceEventsJson = fs.readFileSync(path.join(__dirname, 'data/sepang_race_events.json'), 'utf-8');
const incidentsJson = fs.readFileSync(path.join(__dirname, 'data/sepang_incidents.json'), 'utf-8');

const driverOptionsHtml = Object.entries(drivers).map(([num, d]) => {
  return `<option value="${num}">#${num} ${d.code} — ${d.firstName} ${d.lastName} (${d.team})</option>`;
}).join('\n            ');

const templateHtml = `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>FastestLap — Sepang GPS Live Track Map & Telemetry (22 Piloti 2026)</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #0d1117;
      color: #e6edf3;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      display: flex;
      flex-direction: column;
      height: 100vh;
      overflow: hidden;
    }
    header {
      background: #161b22;
      padding: 10px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #30363d;
      flex-shrink: 0;
    }
    .title-box { display: flex; align-items: center; gap: 12px; }
    .badge {
      background: #ff1801;
      color: white;
      font-size: 11px;
      font-weight: 800;
      padding: 3px 8px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }
    h1 { font-size: 17px; font-weight: 700; }
    .session-info { font-size: 12px; color: #8b949e; margin-left: 8px; }
    .circuit-switch-bar {
      display: flex;
      gap: 6px;
      align-items: center;
      margin-left: 10px;
    }
    .switch-btn {
      background: #21262d;
      color: #c9d1d9;
      border: 1px solid #30363d;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 600;
      text-decoration: none;
      transition: all 0.2s;
    }
    .switch-btn:hover { background: #30363d; color: #fff; }
    .switch-btn.active {
      background: #238636;
      color: #fff;
      border-color: #2ea043;
    }
    .clock-display {
      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
      font-size: 14px;
      font-weight: 700;
      color: #58a6ff;
      background: #0d1117;
      padding: 4px 12px;
      border-radius: 6px;
      border: 1px solid #30363d;
    }

    .main-container {
      display: flex;
      flex: 1;
      overflow: hidden;
    }

    /* Left Pane: Canvas Track Map */
    .map-pane {
      flex: 1;
      position: relative;
      background: #090d13;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }
    canvas {
      display: block;
      cursor: crosshair;
    }

    /* Right Pane: Cockpit Telemetry, Standings, Race Control */
    .hud-pane {
      width: 530px;
      background: #161b22;
      border-left: 1px solid #30363d;
      display: flex;
      flex-direction: column;
      padding: 12px;
      gap: 10px;
      overflow-y: auto;
      flex-shrink: 0;
    }
    .hud-pane::-webkit-scrollbar { width: 6px; }
    .hud-pane::-webkit-scrollbar-thumb { background: #30363d; border-radius: 3px; }

    .card {
      background: #0d1117;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 10px 12px;
    }
    .card-title {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: #8b949e;
      font-weight: 700;
      margin-bottom: 6px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    /* Live Overtake & Events Ticker */
    .event-card {
      background: #1c1408;
      border: 1px solid #d29922;
    }
    .event-ticker {
      font-size: 11.5px;
      font-weight: 700;
      color: #f0f6fc;
      line-height: 1.35;
      min-height: 32px;
      display: flex;
      align-items: center;
      transition: color 0.3s;
    }

    /* FIA Race Control Panel */
    .rc-card {
      border-left: 3px solid #ff8000;
    }
    .rc-status-row {
      display: flex;
      gap: 6px;
      margin-bottom: 6px;
      align-items: center;
    }
    .rc-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 800;
      letter-spacing: 0.5px;
    }
    .rc-flag-green { background: #238636; color: #fff; }
    .rc-flag-yellow { background: #d29922; color: #000; animation: blinkYellow 1.2s infinite; }
    .rc-flag-vsc { background: #e3b341; color: #000; animation: pulseVsc 1s infinite; }
    .rc-flag-chequered { background: #fff; color: #000; border: 1px solid #8b949e; }
    .rc-drs-on { background: #238636; color: #fff; }
    .rc-drs-off { background: #21262d; color: #8b949e; }
    .rc-feed-box {
      max-height: 110px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-right: 2px;
    }
    .rc-feed-box::-webkit-scrollbar { width: 4px; }
    .rc-feed-box::-webkit-scrollbar-thumb { background: #30363d; border-radius: 2px; }
    .rc-item {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 3px 6px;
      background: #0d1117;
      border: 1px solid #21262d;
      border-radius: 4px;
      font-size: 9.5px;
      line-height: 1.3;
      cursor: pointer;
      transition: background 0.15s;
    }
    .rc-item:hover { background: #161b22; }
    .rc-item-time { color: #8b949e; font-family: monospace; font-size: 9px; white-space: nowrap; }
    .rc-item-lap { color: #58a6ff; font-weight: 700; white-space: nowrap; }
    .rc-badge-tag {
      font-size: 8px;
      padding: 1px 4px;
      border-radius: 3px;
      font-weight: 800;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .rc-tag-flag { background: #d29922; color: #000; }
    .rc-tag-drs { background: #238636; color: #fff; }
    .rc-tag-vsc { background: #f0883e; color: #000; }
    .rc-tag-limits { background: #da3633; color: #fff; }
    .rc-tag-other { background: #388bfd; color: #fff; }
    .rc-item-text { color: #c9d1d9; flex: 1; }

    @keyframes pulseVsc {
      0% { box-shadow: 0 0 4px #e3b341; }
      50% { box-shadow: 0 0 12px #e3b341; }
      100% { box-shadow: 0 0 4px #e3b341; }
    }
    @keyframes blinkYellow {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.6; }
    }

    /* Driver Selector */
    select {
      width: 100%;
      background: #161b22;
      color: #e6edf3;
      border: 1px solid #30363d;
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 12.5px;
      outline: none;
      cursor: pointer;
    }
    select:focus { border-color: #58a6ff; }

    /* Active Driver Meta Banner */
    .driver-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px;
      background: #161b22;
      border-radius: 6px;
      margin-top: 6px;
      border: 1px solid #21262d;
    }
    .driver-color-bar {
      width: 5px;
      height: 34px;
      border-radius: 2px;
      background: #ff8000;
    }
    .driver-num-badge {
      font-size: 18px;
      font-weight: 900;
      color: #f0f6fc;
      width: 32px;
      text-align: center;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    }
    .driver-meta { flex: 1; }
    .driver-fullname { font-size: 13px; font-weight: 700; color: #f0f6fc; }
    .driver-teamname { font-size: 11px; color: #8b949e; margin-top: 1px; }
    .tire-badge {
      font-size: 10px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 4px;
      text-align: center;
    }
    .tire-S { background: #da3633; color: white; }
    .tire-M { background: #e3b341; color: black; }
    .tire-H { background: #f0f6fc; color: black; }
    .tire-I { background: #238636; color: white; border: 1px solid #3fb950; }
    .tire-W { background: #1f6feb; color: white; border: 1px solid #58a6ff; }

    /* Cockpit Gauges */
    .cockpit-grid {
      display: grid;
      grid-template-columns: 80px 1fr;
      gap: 10px;
      align-items: center;
      margin-top: 4px;
    }
    .gear-box {
      height: 68px;
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 42px;
      font-weight: 900;
      color: #58a6ff;
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    }
    .speed-box {
      display: flex;
      flex-direction: column;
      justify-content: center;
    }
    .speed-value {
      font-size: 34px;
      font-weight: 900;
      color: #f0f6fc;
      line-height: 1;
    }
    .speed-unit { font-size: 11px; color: #8b949e; margin-top: 2px; }
    .rpm-text { font-size: 12.5px; font-weight: 600; color: #79c0ff; margin-top: 2px; }
    
    .led-bar {
      display: flex;
      gap: 3px;
      margin-top: 6px;
    }
    .led {
      flex: 1;
      height: 6px;
      border-radius: 2px;
      background: #21262d;
    }
    .led.green.on { background: #238636; box-shadow: 0 0 6px #2ea043; }
    .led.yellow.on { background: #d29922; box-shadow: 0 0 6px #e3b341; }
    .led.red.on { background: #da3633; box-shadow: 0 0 8px #f85149; }

    .pedal-bar-wrap { margin-top: 6px; }
    .pedal-label {
      display: flex;
      justify-content: space-between;
      font-size: 10.5px;
      font-weight: 600;
      color: #8b949e;
      margin-bottom: 2px;
    }
    .progress-track {
      background: #21262d;
      height: 7px;
      border-radius: 4px;
      overflow: hidden;
    }
    .progress-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.05s ease-out;
    }
    .fill-throttle { background: #2ea043; }
    
    .tags-row {
      display: flex;
      gap: 8px;
      margin-top: 6px;
    }
    .tag {
      flex: 1;
      text-align: center;
      padding: 4px;
      border-radius: 4px;
      font-size: 10.5px;
      font-weight: 800;
      background: #21262d;
      color: #484f58;
    }
    .tag.brake-on { background: #da3633; color: white; box-shadow: 0 0 10px rgba(218,54,51,0.6); }
    .tag.drs-on { background: #238636; color: white; box-shadow: 0 0 10px rgba(35,134,54,0.6); }
    .tag.pit-on { background: #d29922; color: black; box-shadow: 0 0 10px rgba(210,153,34,0.8); }

    /* Location */
    .loc-name { font-weight: 700; color: #f0f6fc; font-size: 12px; line-height: 1.4; }
    .gps-coords { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 10.5px; color: #58a6ff; margin-top: 2px; }
    .sector-badge {
      display: inline-block;
      padding: 2px 7px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 800;
      margin-top: 3px;
    }
    .sec-1 { background: #e3b341; color: #000; }
    .sec-2 { background: #da3633; color: #fff; }
    .sec-3 { background: #388bfd; color: #fff; }

    /* Cockpit Lap & Sector Card */
    .timing-hud-card {
      border-left: 3px solid #a855f7;
    }
    .hud-sectors-row {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: 6px;
      margin-top: 4px;
    }
    .hud-sec-box {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 6px 6px;
      text-align: center;
      transition: border-color 0.2s, box-shadow 0.2s;
    }
    .hud-sec-box.active-sec {
      border-color: #58a6ff;
      box-shadow: 0 0 8px rgba(88, 166, 255, 0.45);
    }
    .hud-sec-lbl {
      font-size: 9px;
      font-weight: 800;
      color: #8b949e;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .hud-sec-val {
      font-size: 14.5px;
      font-weight: 900;
      color: #f0f6fc;
      font-family: ui-monospace, SFMono-Regular, monospace;
      margin-top: 2px;
    }
    .hud-sec-tag {
      font-size: 8px;
      font-weight: 800;
      text-transform: uppercase;
      margin-top: 2px;
      padding: 1px 4px;
      border-radius: 3px;
      display: inline-block;
    }
    .hud-laps-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 6px;
      margin-top: 6px;
    }
    .hud-lap-box {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 6px 8px;
    }
    .hud-lap-lbl {
      font-size: 9px;
      font-weight: 800;
      color: #8b949e;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .hud-lap-val {
      font-size: 15px;
      font-weight: 900;
      font-family: ui-monospace, SFMono-Regular, monospace;
      color: #f0f6fc;
      margin-top: 2px;
    }
    .hud-lap-status {
      font-size: 8.5px;
      font-weight: 700;
      margin-top: 2px;
      padding: 1px 4px;
      border-radius: 3px;
      display: inline-block;
    }

    /* Live Timing Tower (22 Drivers) */
    .timing-table-wrap {
      max-height: 240px;
      overflow-y: auto;
      border: 1px solid #21262d;
      border-radius: 6px;
      margin-top: 4px;
    }
    .timing-table-wrap::-webkit-scrollbar { width: 4px; }
    .timing-table-wrap::-webkit-scrollbar-thumb { background: #30363d; border-radius: 2px; }
    .timing-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 10.5px;
      font-family: ui-monospace, SFMono-Regular, monospace;
    }
    .timing-table th {
      background: #161b22;
      color: #8b949e;
      font-weight: 700;
      padding: 4px 3px;
      text-align: center;
      position: sticky;
      top: 0;
      font-size: 8.5px;
      border-bottom: 1px solid #30363d;
      letter-spacing: 0.3px;
    }
    .timing-table td {
      padding: 2.5px 3px;
      border-bottom: 1px solid #21262d;
      white-space: nowrap;
      text-align: center;
    }
    .timing-row {
      cursor: pointer;
      transition: background 0.15s;
    }
    .timing-row:hover { background: #21262d; }
    .timing-row.active {
      background: rgba(88, 166, 255, 0.15);
      border-left: 3px solid #58a6ff;
    }
    .pos-cell { font-weight: 800; color: #f0f6fc; width: 18px; text-align: center; }
    .delta-cell { width: 18px; text-align: center; font-size: 9px; font-weight: 700; }
    .driver-pill { display: flex; align-items: center; gap: 4px; font-weight: 700; font-family: -apple-system, sans-serif; text-align: left; }
    .color-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; flex-shrink: 0; }

    /* F1 Sector & Lap Badges */
    .f1-badge {
      display: inline-block;
      padding: 1px 4px;
      border-radius: 3px;
      font-size: 9.5px;
      font-weight: 700;
      line-height: 1.25;
    }
    .badge-purple { background: rgba(176, 85, 255, 0.22); color: #d2a8ff; border: 1px solid rgba(176, 85, 255, 0.55); font-weight: 800; }
    .badge-green { background: rgba(46, 160, 67, 0.22); color: #3fb950; border: 1px solid rgba(46, 160, 67, 0.55); font-weight: 700; }
    .badge-yellow { background: rgba(210, 153, 34, 0.18); color: #e3b341; border: 1px solid rgba(210, 153, 34, 0.45); font-weight: 600; }
    .badge-dim { color: #484f58; font-weight: 500; }

    /* Controls */
    .controls-card {
      padding: 8px 12px;
    }
    .ctrl-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .btn-primary {
      background: #238636;
      color: white;
      border: none;
      padding: 6px 14px;
      border-radius: 6px;
      font-weight: 700;
      font-size: 12px;
      cursor: pointer;
      transition: background 0.2s;
    }
    .btn-primary:hover { background: #2ea043; }
    input[type=range] {
      flex: 1;
      accent-color: #58a6ff;
      cursor: pointer;
    }
    .shortcuts-bar {
      display: flex;
      gap: 4px;
      flex-wrap: wrap;
      margin-top: 6px;
    }
    .jump-btn {
      background: #21262d;
      color: #c9d1d9;
      border: 1px solid #30363d;
      padding: 3px 6px;
      border-radius: 4px;
      font-size: 9.5px;
      font-weight: 600;
      cursor: pointer;
    }
    .jump-btn:hover { background: #30363d; color: white; }
    .jump-btn.race-event { border-color: #d29922; color: #e3b341; }
    .jump-btn.race-event:hover { background: #d29922; color: black; }

    /* Mobile & Responsive Adjustments */
    @media (max-width: 900px), (max-device-width: 900px) {
      body {
        height: auto;
        min-height: 100vh;
        overflow-y: auto;
      }
      header {
        padding: 8px 12px;
        flex-wrap: wrap;
        gap: 6px;
      }
      .title-box h1 {
        font-size: 13.5px;
      }
      .session-info {
        display: none;
      }
      .main-container {
        flex-direction: column;
        height: auto;
        overflow: visible;
      }
      .map-pane {
        height: 48vh;
        min-height: 280px;
        width: 100%;
        flex: none;
      }
      .hud-pane {
        width: 100%;
        border-left: none;
        border-top: 1px solid #30363d;
        overflow-y: visible;
        padding: 8px;
        box-sizing: border-box;
      }
      .cockpit-grid {
        grid-template-columns: 60px 1fr;
      }
      .gear-box {
        height: 52px;
        font-size: 30px;
      }
      .speed-value {
        font-size: 26px;
      }
      .timing-table-wrap {
        max-height: 260px;
      }
    }
  </style>
</head>
<body>
  <header>
    <div class="title-box">
      <span class="badge">SIGNALR LIVE</span>
      <h1>Sepang Grand Prix (4 Ottobre 2026) — Gara Ufficiale 55 Giri</h1>
      <div class="circuit-switch-bar">
        <a href="track_map.html" class="switch-btn active">🇲🇾 Sepang GP (Bagnato)</a>
        <a href="track_map_baku.html" class="switch-btn">🇦🇿 Baku GP (Asciutto)</a>
      </div>
    </div>
    <div class="clock-display" id="headerClock">
      4 Ottobre 2026 | 08:33:00 UTC | Giro: 1/55
    </div>
  </header>

  <div class="main-container">
    <div class="map-pane">
      <canvas id="trackCanvas"></canvas>
    </div>

    <div class="hud-pane">
      <!-- Race Events & Overtake Live Ticker -->
      <div class="card event-card">
        <div class="card-title">
          <span>⚡ Feed Sorpassi & Duelli in Pista</span>
          <span style="font-size: 10px; color: #ff8000;" id="eventCountBadge">Live Feed</span>
        </div>
        <div class="event-ticker" id="overtakeTicker">
          🏁 08:33:00 — Partenza: Semafori spenti! Tutti i 22 piloti scattano dalla griglia del GP di Sepang!
        </div>
      </div>

      <!-- FIA Race Control Messages Card -->
      <div class="card rc-card">
        <div class="card-title">
          <span>🏁 FIA Race Control — Direzione Gara</span>
          <span style="font-size: 10px; color: #ff8000;" id="rcStatusText">LIVE DIRECT</span>
        </div>
        <div class="rc-status-row">
          <div class="rc-pill rc-flag-green" id="rcFlagBadge">🟢 TRACK CLEAR</div>
          <div class="rc-pill rc-drs-on" id="rcDrsBadge">DRS ON</div>
          <span style="font-size: 10px; color: #8b949e; margin-left: auto;" id="rcTotalCount">48 Messaggi</span>
        </div>
        <div class="rc-feed-box" id="rcFeedBox">
          <!-- Dynamically populated from RACE_CONTROL_MESSAGES up to currentSecond -->
        </div>
      </div>

      <!-- Driver Selector Card -->
      <div class="card">
        <div class="card-title">
          <span>Selezione Pilota & Vista</span>
          <span style="font-size: 10px; color: #58a6ff;" id="driverModeLabel">Vista Globale: 22 Piloti</span>
        </div>
        <select id="driverSelect">
          <option value="all">🏁 Vista Globale — Tutti i 22 Piloti in Pista</option>
          <optgroup label="Griglia Ufficiale F1 2026 (22 Piloti — 11 Team)">
            __DRIVER_OPTIONS_HTML__
          </optgroup>
        </select>

        <!-- Driver Active Header Banner -->
        <div class="driver-banner" id="activeDriverBanner">
          <div class="driver-color-bar" id="bannerColorBar" style="background: #3671C6;"></div>
          <div class="driver-num-badge" id="bannerNum">#3</div>
          <div class="driver-meta">
            <div class="driver-fullname" id="bannerName">Max Verstappen (VER)</div>
            <div class="driver-teamname" id="bannerTeam">Oracle Red Bull Racing</div>
          </div>
          <div style="display: flex; flex-direction: column; align-items: flex-end; gap: 3px;">
            <span class="badge" style="background: #238636; font-size: 9px;" id="bannerStatus">LIVE TRACKING</span>
            <span class="tire-badge tire-M" id="bannerTire">MEDIUM</span>
          </div>
        </div>
      </div>

      <!-- Cockpit Gauges (1:1 Synchronized with selected/focused driver) -->
      <div class="card">
        <div class="card-title">
          <span>Cockpit Telemetria Real-Time</span>
          <span id="syncIndicator" style="color: #2ea043; font-size: 10px;">● 1:1 Sincronizzato</span>
        </div>
        <div class="cockpit-grid">
          <div class="gear-box" id="gearVal">7</div>
          <div class="speed-box">
            <div><span class="speed-value" id="speedVal">280</span> <span class="speed-unit">KM/H</span></div>
            <div class="rpm-text" id="rpmVal">12,094 RPM</div>
          </div>
        </div>

        <div class="led-bar" id="ledBar">
          <div class="led green"></div><div class="led green"></div><div class="led green"></div><div class="led green"></div>
          <div class="led yellow"></div><div class="led yellow"></div><div class="led yellow"></div><div class="led yellow"></div>
          <div class="led red"></div><div class="led red"></div>
        </div>

        <div class="pedal-bar-wrap">
          <div class="pedal-label"><span>ACCELERATORE (THROTTLE)</span><span id="throttleVal">100%</span></div>
          <div class="progress-track"><div class="progress-fill fill-throttle" id="throttleFill" style="width: 100%;"></div></div>
        </div>

        <div class="tags-row">
          <div class="tag" id="brakeTag">FRENO (BRAKE)</div>
          <div class="tag drs-on" id="drsTag">ACTIVE AERO: Z-MODE (LOW DRAG)</div>
        </div>

        <div style="display: flex; gap: 8px; margin-top: 6px; font-size: 11px; font-weight: 700;">
          <div style="flex: 1; background: #21262d; border: 1px solid #30363d; border-radius: 4px; padding: 4px 8px; display: flex; justify-content: space-between;">
            <span style="color: #8b949e;">BATTERIA SoC:</span>
            <span id="socVal" style="color: #3fb950;">84%</span>
          </div>
          <div style="flex: 1; background: #21262d; border: 1px solid #30363d; border-radius: 4px; padding: 4px 8px; display: flex; justify-content: space-between;">
            <span style="color: #8b949e;">OVERRIDE (MOM):</span>
            <span id="momVal" style="color: #38bdf8;">DISPONIBILE</span>
          </div>
        </div>
      </div>

      <!-- GPS & Track Location -->
      <div class="card location-card">
        <div class="card-title">Posizione Pista & Coordinate GPS Decimetriche</div>
        <div class="loc-name" id="locName">📍 Main Straight (Start/Finish DRS Zone 1 — 927m)</div>
        <div id="sectorBadge" class="sector-badge sec-1">SETTORE 1</div>
        <div class="gps-coords" id="gpsCoords">GPS: X = +72.0m, Y = -8.0m (X:720, Y:-80 decimetri)</div>
        <div style="font-size: 10.5px; color: #8b949e; margin-top: 2px;" id="svgCoords">Coordinate Asfalto SVG: px = 713.97, py = 538.21</div>
      </div>

      <!-- Tempi sul Giro & Settori Pilota (Live Timing & Micro-Settori) -->
      <div class="card timing-hud-card">
        <div class="card-title">
          <span>Tempi sul Giro & Settori (Live Timing)</span>
          <span id="hudLapNumBadge" style="font-size: 10px; color: #58a6ff; font-weight: 800;">GIRO 1 / 55</span>
        </div>
        
        <!-- Live Sectors Row -->
        <div class="hud-sectors-row">
          <div class="hud-sec-box" id="hudSec1Box">
            <div class="hud-sec-lbl">SETTORE 1</div>
            <div class="hud-sec-val" id="hudSec1Val">-</div>
            <div class="hud-sec-tag badge-dim" id="hudSec1Tag">IN ATTESA</div>
          </div>
          <div class="hud-sec-box" id="hudSec2Box">
            <div class="hud-sec-lbl">SETTORE 2</div>
            <div class="hud-sec-val" id="hudSec2Val">-</div>
            <div class="hud-sec-tag badge-dim" id="hudSec2Tag">IN ATTESA</div>
          </div>
          <div class="hud-sec-box" id="hudSec3Box">
            <div class="hud-sec-lbl">SETTORE 3</div>
            <div class="hud-sec-val" id="hudSec3Val">-</div>
            <div class="hud-sec-tag badge-dim" id="hudSec3Tag">IN ATTESA</div>
          </div>
        </div>

        <!-- Last Lap & Best Lap Row -->
        <div class="hud-laps-row">
          <div class="hud-lap-box">
            <div class="hud-lap-lbl">ULTIMO GIRO</div>
            <div class="hud-lap-val" id="hudLastLapVal">-</div>
            <div class="hud-lap-status badge-dim" id="hudLastLapTag">NESSUN GIRO</div>
          </div>
          <div class="hud-lap-box">
            <div class="hud-lap-lbl">MIGLIOR GIRO</div>
            <div class="hud-lap-val" id="hudBestLapVal">-</div>
            <div class="hud-lap-status badge-dim" id="hudBestLapTag">IN ATTESA</div>
          </div>
        </div>

        <div style="font-size: 9.5px; color: #8b949e; margin-top: 6px; display: flex; justify-content: space-between; border-top: 1px solid #21262d; padding-top: 4px;">
          <span>RECORD GARA: <strong id="hudSessionFastestVal" style="color: #d2a8ff;">1:38.220</strong> (<span id="hudSessionFastestDrv">#3 VER</span>)</span>
          <span style="display: flex; gap: 6px;">
            <span style="color: #d2a8ff; font-weight: 700;">● Viola: Assoluto</span>
            <span style="color: #3fb950; font-weight: 700;">● Verde: Personale</span>
            <span style="color: #e3b341; font-weight: 700;">● Giallo: No Migl.</span>
          </span>
        </div>
      </div>

      <!-- Live Timing Tower / Standings (All 22 Drivers) -->
      <div class="card">
        <div class="card-title">
          <span>Classifica Live in Pista (22 Piloti)</span>
          <span style="font-size: 10px; color: #8b949e;">Click riga per focus pilota</span>
        </div>
        <div class="timing-table-wrap">
          <table class="timing-table">
            <thead>
              <tr>
                <th class="pos-cell">POS</th>
                <th class="delta-cell">+/-</th>
                <th style="text-align: left; padding-left: 6px;">PILOTA</th>
                <th>S1</th>
                <th>S2</th>
                <th>S3</th>
                <th>ULTIMO</th>
                <th>MIGLIORE</th>
                <th>DISTACCO</th>
                <th>GOMMA</th>
              </tr>
            </thead>
            <tbody id="timingTableBody">
              <!-- Dynamically populated rows -->
            </tbody>
          </table>
        </div>
      </div>

      <!-- Playback Controls -->
      <div class="card controls-card">
        <div class="card-title">
          <span>Controlli Simulazione & Salto Momenti Chiave</span>
          <span style="font-size: 10px; color: #3fb950; font-weight: 700;">PRODUZIONE: 1x (TEMPO REALE)</span>
        </div>
        <div class="ctrl-row">
          <button class="btn-primary" id="playPauseBtn">PAUSA</button>
          <span style="font-size: 12px; font-weight: 600;" id="speedLabel">Velocità: 2x (Test)</span>
          <div style="display: flex; gap: 4px; margin-left: auto;">
            <button class="jump-btn" onclick="setSimulationSpeed(1)" title="Velocità reale 1:1 come nel feed live">1x (Reale)</button>
            <button class="jump-btn" onclick="setSimulationSpeed(2)">2x</button>
            <button class="jump-btn" onclick="setSimulationSpeed(5)">5x</button>
            <button class="jump-btn" onclick="setSimulationSpeed(10)">10x</button>
            <button class="jump-btn" onclick="setSimulationSpeed(20)">20x</button>
          </div>
        </div>
        <div class="ctrl-row" style="margin-top: 6px;">
          <span style="font-size: 11px; color: #8b949e; width: 65px;">Velocità:</span>
          <input type="range" id="speedRange" min="1" max="20" value="2">
        </div>
        <div class="ctrl-row" style="margin-top: 4px;">
          <span style="font-size: 11px; color: #8b949e; width: 65px;">Timeline:</span>
          <input type="range" id="timeScrubber" min="0" max="6550" value="0">
        </div>

        <!-- Highlights & Overtakes Shortcuts -->
        <div class="shortcuts-bar">
          <span style="font-size: 10px; color: #e3b341; width: 100%; margin-top: 4px; font-weight: 700;">Momenti Chiave, Race Control & Sorpassi:</span>
          <button class="jump-btn race-event" onclick="jumpToRace(0)">🟡 08:33:00 Form. Lap 1: Giro Formazione 1 (Safety Car)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(108)">🟡 08:34:48 Form. Lap 2: Giro Formazione 2 (Safety Car)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(202)">🏎️ 08:36:22 Schieramento Griglia (Rientro SC)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(217)">⚡ 08:36:37 Standing Start (Partenza Ufficiale da Fermo!)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(250)">🔧 08:37:10 G1: Transito Box Pérez (Linea Nera 80 km/h)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(405)">🔧 08:39:45 G2: Transito Box Piastri/Bortoleto/Stroll</button>
          <button class="jump-btn race-event" onclick="jumpToRace(1156)">🟡 08:52:16 G9: Safety Car 1 in Pista (Bottas DNF)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(1589)">🟢 08:59:29 G12: Safety Car Rientra / Antonelli P1</button>
          <button class="jump-btn race-event" onclick="jumpToRace(1620)">⚡ 09:00:00 G13: Restart / Antonelli-Verstappen</button>
          <button class="jump-btn race-event" onclick="jumpToRace(2460)">👑 09:14:00 G21: Verstappen Leadership (P1)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(3820)">🔧 09:36:40 G33: Pit Stop Hard (Corsia Box)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(4760)">⚠️ 09:52:20 G41: Ritiro Albon (DNF Idraulica)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(4801)">🟡 09:53:01 G43: VSC Deployed</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5126)">🟡 09:58:26 G45: Safety Car 2 in Pista (Albon DNF)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5436)">🔄 10:03:36 G48: Sdoppiamento 55, 5, 11</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5840)">💥 10:10:20 G49: Ritiro Russell (DNF Motore)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5974)">🟢 10:12:34 G51: Safety Car Out / Sprint Finale</button>
          <button class="jump-btn race-event" onclick="jumpToRace(6435)">🏆 10:20:15 G55: Verstappen Vince! (Podio Ufficiale)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(6550)">🛑 10:22:10 Fine Sessione / Parc Fermé</button>
        </div>

        <!-- Track Corner Shortcuts -->
        <div class="shortcuts-bar" style="margin-top: 6px;">
          <span style="font-size: 10px; color: #8b949e; width: 100%;">Salta a Sezione Pista (Pilota Attivo):</span>
          <button class="jump-btn" onclick="jumpToCorner(0.0)">Rettifilo Arrivo (320 km/h DRS)</button>
          <button class="jump-btn" onclick="jumpToCorner(5.1)">Staccata Curva 1 (Brake ON)</button>
          <button class="jump-btn" onclick="jumpToCorner(8.2)">Ape Curva 1 (74 km/h G2)</button>
          <button class="jump-btn" onclick="jumpToCorner(23.3)">Staccata Curva 4</button>
          <button class="jump-btn" onclick="jumpToCorner(32.5)">Curve Veloci 5-6 (Esses 238 km/h)</button>
          <button class="jump-btn" onclick="jumpToCorner(46.7)">Staccata Salita Curva 9</button>
          <button class="jump-btn" onclick="jumpToCorner(78.0)">Back Straight (DRS ON, 320 km/h)</button>
          <button class="jump-btn" onclick="jumpToCorner(84.6)">Staccata Tornante 15</button>
        </div>
      </div>
    </div>
  </div>

  <script>
    // 453 Exact Track Nodes extracted directly from Sepang.svg.webp centerline
    // Structure: [t, px, py, x, y, sector, speed, gear, rpm, throttle, brake, drs, location]
    const NODES = __NODES_JSON__;
    const DRIVERS = __DRIVERS_JSON__;
    const DRIVER_KEYFRAMES = __KEYFRAMES_JSON__;
    const PIT_STOPS = __PIT_STOPS_JSON__;
    const RACE_EVENTS = __RACE_EVENTS_JSON__;
    const RACE_CONTROL_MESSAGES = __RACE_CONTROL_MESSAGES_JSON__;
    const RETIREMENTS = __RETIREMENTS_JSON__;
    const LAP_STARTS = __LAP_STARTS_JSON__;
    const DRIVER_STINTS = __DRIVER_STINTS_JSON__;
    const DRIVER_LAPS = __DRIVER_LAPS_JSON__;

    const SVG_WIDTH = 1280;
    const SVG_HEIGHT = 1057;
    const LAP_DURATION = 95.0;
    const TOTAL_LAPS = 55;
    const RACE_FINISH_SEC = 6435;
    const SESSION_DURATION = 6550; // 08:33:00 to 10:22:10 UTC
    const PIT_EXIT_TRACK_SEC = 5.06; // Sepang Curva 1 entrance node at pit lane exit (px: 141.0, py: 585.5)
    const PIT_ENTRY_TRACK_SEC = 89.34; // Sepang Curva 15 entrance node into pit lane (px: 1070.0, py: 466.0)

    function getDriverLapRecord(drvKey, t) {
      const dl = DRIVER_LAPS[drvKey];
      if (!dl || dl.length === 0) return null;
      for (let i = 0; i < dl.length; i++) {
        const l = dl[i];
        const nextStart = dl[i + 1] ? dl[i + 1].startSec : (l.startSec + (l.dur || 100.0));
        if (t >= l.startSec && t < nextStart) {
          return l;
        }
      }
      if (t < dl[0].startSec) return dl[0];
      return dl[dl.length - 1];
    }

    // Official Grid order from Jolpica qualifying
    const GRID_ORDER = ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41'];

    // Sepang Pit Lane path following the authentic black line next to Start/Finish Straight
    // Branching off before Turn 15 (px: 1103) through Pit Boxes (px: 714) to Pit Exit (px: 141)
    const PIT_LANE_NODES = [
      { px: 1103.0, py: 526.0 }, // Branching off track before Turn 15
      { px: 1096.0, py: 508.0 },
      { px: 1085.0, py: 484.0 },
      { px: 1070.0, py: 466.0 },
      { px: 1050.0, py: 451.0 },
      { px: 1030.0, py: 446.0 }, // Crest of pit entry inside Turn 15
      { px: 1005.0, py: 451.0 },
      { px: 980.0,  py: 460.0 },
      { px: 950.0,  py: 471.0 },
      { px: 920.0,  py: 482.0 },
      { px: 885.0,  py: 493.0 },
      { px: 850.0,  py: 504.0 },
      { px: 800.0,  py: 514.5 },
      { px: 750.0,  py: 520.0 },
      { px: 714.0,  py: 523.5 }, // Central Pit Box Zone opposite S/F line
      { px: 650.0,  py: 528.0 },
      { px: 550.0,  py: 536.0 },
      { px: 450.0,  py: 544.0 },
      { px: 350.0,  py: 552.5 },
      { px: 250.0,  py: 562.0 },
      { px: 200.0,  py: 570.5 },
      { px: 175.0,  py: 578.0 },
      { px: 155.0,  py: 583.5 },
      { px: 141.0,  py: 585.5 }  // Rejoining track edge before Turn 1
    ];

    const PIT_LANE_DISTS = [0];
    for (let i = 0; i < PIT_LANE_NODES.length - 1; i++) {
      const dx = PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px;
      const dy = PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py;
      PIT_LANE_DISTS.push(PIT_LANE_DISTS[i] + Math.sqrt(dx * dx + dy * dy));
    }
    const PIT_LANE_TOTAL_DIST = PIT_LANE_DISTS[PIT_LANE_DISTS.length - 1];

    function getPitLaneCoordAtDist(dist) {
      if (dist <= 0) return { px: PIT_LANE_NODES[0].px, py: PIT_LANE_NODES[0].py };
      if (dist >= PIT_LANE_TOTAL_DIST) return { px: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].px, py: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].py };
      for (let i = 0; i < PIT_LANE_DISTS.length - 1; i++) {
        if (dist >= PIT_LANE_DISTS[i] && dist <= PIT_LANE_DISTS[i + 1]) {
          const segLen = PIT_LANE_DISTS[i + 1] - PIT_LANE_DISTS[i];
          const u = segLen > 0 ? (dist - PIT_LANE_DISTS[i]) / segLen : 0;
          return {
            px: PIT_LANE_NODES[i].px + (PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px) * u,
            py: PIT_LANE_NODES[i].py + (PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py) * u
          };
        }
      }
      return { px: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].px, py: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].py };
    }

    function getDriverGap(drvKey, t) {
      if (t < 216.9) {
        const gridIdx = GRID_ORDER.indexOf(drvKey);
        return (gridIdx >= 0 ? gridIdx : 21) * 0.15;
      }
      const kfs = DRIVER_KEYFRAMES[drvKey];
      if (!kfs) return 0;
      if (t <= kfs[0][0]) return kfs[0][1];
      if (t >= kfs[kfs.length - 1][0]) return kfs[kfs.length - 1][1];

      let low = 0, high = kfs.length - 1;
      while (low <= high) {
        const mid = (low + high) >> 1;
        if (kfs[mid][0] <= t) low = mid + 1;
        else high = mid - 1;
      }
      const i1 = Math.max(0, high);
      const i2 = Math.min(kfs.length - 1, i1 + 1);
      const p1 = kfs[i1];
      const p2 = kfs[i2];
      const dt = p2[0] - p1[0];
      if (dt <= 0) return p1[1];
      const u = (t - p1[0]) / dt;
      return p1[1] + (p2[1] - p1[1]) * u;
    }

    function isDriverRetired(drvKey, t) {
      return Boolean(RETIREMENTS[drvKey] && t >= RETIREMENTS[drvKey].timeSec);
    }

    function getDriverProgress(drvKey, t) {
      if (isDriverRetired(drvKey, t)) {
        const ret = RETIREMENTS[drvKey];
        return ret.timeSec - getDriverGap(drvKey, ret.timeSec);
      }
      if (t < 216.9) {
        const gridIdx = GRID_ORDER.indexOf(drvKey);
        const pos = gridIdx >= 0 ? gridIdx : 21;
        return t - (pos * 0.15);
      }
      if (t >= RACE_FINISH_SEC) {
        const finishProg = RACE_FINISH_SEC - getDriverGap(drvKey, RACE_FINISH_SEC);
        const inLapElapsed = Math.min(115, t - RACE_FINISH_SEC);
        return finishProg + inLapElapsed * 0.35;
      }
      return t - getDriverGap(drvKey, t);
    }

    function getDriverLap(drvKey, t) {
      if (isDriverRetired(drvKey, t)) {
        return RETIREMENTS[drvKey].lap;
      }
      if (t >= RACE_FINISH_SEC) return TOTAL_LAPS;
      if (t < 108.0) return 1; // Formation Lap 1 (Behind SC)
      if (t < 216.9) return 2; // Formation Lap 2 (Behind SC & Grid Lineup)
      const rec = getDriverLapRecord(drvKey, t);
      if (rec) {
        return rec.lap;
      }
      const gap = getDriverGap(drvKey, t);
      const tEff = Math.max(0, t - gap);
      for (let l = 1; l <= TOTAL_LAPS; l++) {
        if (tEff >= LAP_STARTS[l] && tEff < LAP_STARTS[l + 1]) {
          return l;
        }
      }
      return TOTAL_LAPS;
    }

    function getSlotOffset(pos) {
      return -(0.24 + pos * 0.208);
    }

    function getDriverTrackSec(drvKey, t) {
      if (isDriverRetired(drvKey, t)) {
        return 30.0; // Parked safely off track
      }
      const gridIdx = GRID_ORDER.indexOf(drvKey);
      const pos = gridIdx >= 0 ? gridIdx : 21;
      const slotOff = getSlotOffset(pos);
      const totalTarget = slotOff + 2 * LAP_DURATION;

      if (t < 216.9) {
        const stopTime = 204.0 + pos * 0.42;
        if (t < stopTime) {
          const u = t / stopTime;
          const smoothU = u < 0.85 ? u : 0.85 + (1 - Math.cos((u - 0.85) / 0.15 * Math.PI / 2)) * 0.15;
          const unwrapDist = slotOff + (totalTarget - slotOff) * smoothU;
          return ((unwrapDist % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
        } else {
          return ((slotOff % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
        }
      }
      if (t >= RACE_FINISH_SEC) {
        const finProg = Math.min(LAP_DURATION, (t - RACE_FINISH_SEC) * 0.35);
        return finProg;
      }

      // 1. Is driver currently inside the pit lane?
      const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
      if (activeStop) {
        const frac = (t - activeStop.startSec) / Math.max(1, activeStop.endSec - activeStop.startSec);
        return frac * PIT_EXIT_TRACK_SEC;
      }

      // 2. Out-lap: has driver just exited the pit lane?
      const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
      if (outStop) {
        const outEnd = outStop.outLapEndSec || (outStop.endSec + 80);
        const u = Math.min(1.0, Math.max(0.0, (t - outStop.endSec) / Math.max(1, outEnd - outStop.endSec)));
        // Continuous forward progress from Curva 1 pit exit (5.06) all the way to Start/Finish line (95.0)
        return PIT_EXIT_TRACK_SEC + u * (LAP_DURATION - PIT_EXIT_TRACK_SEC);
      }

      // 3. Official Racing Lap 2: standing start launch from grid box into Turn 1 and around circuit
      const rec3 = DRIVER_LAPS[drvKey]?.find(l => l.lap === 3);
      const lap3StartSec = rec3 ? rec3.startSec : (LAP_STARTS[3] || 392.2);

      if (t < lap3StartSec) {
        const dt = t - 216.9;
        const launchDist = slotOff + 0.12 * Math.pow(dt, 1.62);

        const lapRacingDuration = lap3StartSec - 216.9;
        const u = Math.max(0, Math.min(0.9999, (t - 216.9) / lapRacingDuration));
        const S1_TRACK = 21.11;
        const S2_TRACK = 46.58;
        let raceDist;
        if (u < 0.26) {
          raceDist = (u / 0.26) * S1_TRACK;
        } else if (u < 0.58) {
          raceDist = S1_TRACK + ((u - 0.26) / 0.32) * (S2_TRACK - S1_TRACK);
        } else {
          raceDist = S2_TRACK + ((u - 0.58) / 0.42) * (LAP_DURATION - S2_TRACK);
        }

        let finalDist;
        if (t < 221.0) {
          finalDist = launchDist;
        } else if (t < 228.0) {
          const w = (t - 221.0) / 7.0;
          const sw = w * w * (3 - 2 * w);
          finalDist = launchDist * (1 - sw) + raceDist * sw;
        } else {
          finalDist = raceDist;
        }
        return ((finalDist % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
      }

      // 4. Lap 3 onwards: Driver authentic lap progress from FastF1 telemetry records
      const rec = getDriverLapRecord(drvKey, t);
      if (rec && rec.dur && rec.dur > 0 && rec.lap >= 3) {
        // In-lap check: is this driver entering the pits later in this lap?
        const inStop = PIT_STOPS.find(p => p.driver === drvKey && p.startSec > rec.startSec && p.startSec <= rec.startSec + rec.dur && t < p.startSec && t >= rec.startSec);
        if (inStop) {
          const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(1, inStop.startSec - rec.startSec)));
          return u * PIT_ENTRY_TRACK_SEC;
        }

        // Piecewise sector progression with exact 1:1 sector beam crossing on track
        const tS1 = rec.startSec + (rec.s1 || (rec.dur * 0.2222));
        const tS2 = tS1 + (rec.s2 || (rec.dur * 0.2681));
        const S1_TRACK = 21.11;
        const S2_TRACK = 46.58;

        if (t < tS1) {
          const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(0.1, tS1 - rec.startSec)));
          return u * S1_TRACK;
        } else if (t < tS2) {
          const u = Math.min(1.0, Math.max(0.0, (t - tS1) / Math.max(0.1, tS2 - tS1)));
          return S1_TRACK + u * (S2_TRACK - S1_TRACK);
        } else {
          const tEnd = rec.startSec + rec.dur;
          const u = Math.min(0.9999, Math.max(0.0, (t - tS2) / Math.max(0.1, tEnd - tS2)));
          return S2_TRACK + u * (LAP_DURATION - S2_TRACK);
        }
      }

      const gap = getDriverGap(drvKey, t);
      const tEff = Math.max(0, t - gap);
      const l = getDriverLap(drvKey, t);
      const dur = (LAP_STARTS[l + 1] || (RACE_FINISH_SEC + 0.4)) - LAP_STARTS[l];
      const f = Math.max(0, Math.min(0.9999, (tEff - LAP_STARTS[l]) / (dur || LAP_DURATION)));
      return f * LAP_DURATION;
    }

    function getDriverTire(drvKey, t) {
      const l = getDriverLap(drvKey, t);
      const stints = DRIVER_STINTS[drvKey];
      if (!stints || stints.length === 0) return 'M';
      for (let i = 0; i < stints.length; i++) {
        const s = stints[i];
        if (l >= s.lapStart && l <= s.lapEnd) {
          return s.code; // 'I', 'S', 'M', or 'H'
        }
      }
      return stints[stints.length - 1].code;
    }

    function isDriverInPit(drvKey, t) {
      if (isDriverRetired(drvKey, t)) return true; // Retired
      if (t >= SESSION_DURATION) return true; // All cars parked in parc fermé
      return PIT_STOPS.some(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
    }

    function isSafetyCarActive(t) {
      // Formation Laps behind SC (0s to 200s); Safety Car 1: 1156s to 1589s; Safety Car 2: 5126s to 5974s
      return (t >= 0 && t <= 200) || (t >= 1156 && t <= 1589) || (t >= 5126 && t <= 5974);
    }

    function isVSCActive(t) {
      return t >= 4801 && t < 5126;
    }

    function formatSectorTime(sec) {
      if (!sec || isNaN(sec) || sec <= 0) return '-';
      return sec.toFixed(3);
    }

    function formatLapTime(sec) {
      if (!sec || isNaN(sec) || sec <= 0) return '-';
      const m = Math.floor(sec / 60);
      const s = (sec % 60).toFixed(3).padStart(6, '0');
      return m + ':' + s;
    }

    function computeSessionTimingStats(t) {
      let sessionBestLap = Infinity;
      let sessionBestLapDriver = null;
      let sessionBestS1 = Infinity;
      let sessionBestS2 = Infinity;
      let sessionBestS3 = Infinity;

      if (t < 216.9) {
        return { sessionBestLap, sessionBestLapDriver, sessionBestS1, sessionBestS2, sessionBestS3 };
      }

      const dKeys = Object.keys(DRIVERS);
      for (let i = 0; i < dKeys.length; i++) {
        const k = dKeys[i];
        const dl = DRIVER_LAPS[k];
        if (!dl) continue;
        for (let j = 0; j < dl.length; j++) {
          const l = dl[j];
          if (l.startSec + l.dur <= 216.9) continue;
          if (l.startSec > t) break;

          // S1 crossing
          const tS1 = l.startSec + l.s1;
          if (t >= tS1 && tS1 >= 216.9 && l.s1 > 10.0 && l.s1 < sessionBestS1) {
            sessionBestS1 = l.s1;
          }
          // S2 crossing
          const tS2 = l.startSec + l.s1 + l.s2;
          if (t >= tS2 && tS2 >= 216.9 && l.s2 > 10.0 && l.s2 < sessionBestS2) {
            sessionBestS2 = l.s2;
          }
          // Lap / S3 completion
          const tEnd = l.startSec + l.dur;
          if (t >= tEnd && tEnd >= 216.9 && l.dur > 50.0) {
            if (l.s3 > 10.0 && l.s3 < sessionBestS3) {
              sessionBestS3 = l.s3;
            }
            if (l.dur < sessionBestLap) {
              sessionBestLap = l.dur;
              sessionBestLapDriver = k;
            }
          }
        }
      }
      return { sessionBestLap, sessionBestLapDriver, sessionBestS1, sessionBestS2, sessionBestS3 };
    }

    let lastStatsSec = -1;
    let cachedSessionBests = null;
    function getSessionBests(t) {
      const s = Math.floor(t);
      if (s === lastStatsSec && cachedSessionBests) {
        return cachedSessionBests;
      }
      lastStatsSec = s;
      cachedSessionBests = computeSessionTimingStats(t);
      return cachedSessionBests;
    }

    function getDriverTimingStats(drvKey, t, sessionBests) {
      if (t < 216.9 || isDriverRetired(drvKey, t)) {
        if (t < 216.9) {
          return {
            curLap: null,
            lastLap: null,
            s1Str: '-',
            s1Badge: 'dim',
            s2Str: '-',
            s2Badge: 'dim',
            s3Str: '-',
            s3Badge: 'dim',
            lastLapStr: '-',
            lastLapBadge: 'dim',
            bestLapStr: '-',
            bestLapBadge: 'dim',
            pBestLap: null
          };
        }
      }

      const dl = DRIVER_LAPS[drvKey] || [];
      const completedLaps = [];
      let curLap = null;

      for (let i = 0; i < dl.length; i++) {
        const l = dl[i];
        if (l.startSec + l.dur <= 216.9) continue;
        if (t >= l.startSec + l.dur) {
          completedLaps.push(l);
        } else if (t >= l.startSec && !curLap) {
          curLap = l;
        }
      }

      let pBestLap = Infinity;
      let pBestS1 = Infinity;
      let pBestS2 = Infinity;
      let pBestS3 = Infinity;

      for (let i = 0; i < completedLaps.length; i++) {
        const l = completedLaps[i];
        if (l.dur > 50.0 && l.dur < pBestLap) pBestLap = l.dur;
        if (l.s1 > 10.0 && l.s1 < pBestS1) pBestS1 = l.s1;
        if (l.s2 > 10.0 && l.s2 < pBestS2) pBestS2 = l.s2;
        if (l.s3 > 10.0 && l.s3 < pBestS3) pBestS3 = l.s3;
      }

      if (curLap) {
        if (t >= curLap.startSec + curLap.s1 && curLap.s1 > 10.0 && curLap.s1 < pBestS1) pBestS1 = curLap.s1;
        if (t >= curLap.startSec + curLap.s1 + curLap.s2 && curLap.s2 > 10.0 && curLap.s2 < pBestS2) pBestS2 = curLap.s2;
      }

      const lastLap = completedLaps.length > 0 ? completedLaps[completedLaps.length - 1] : null;

      // S1
      let s1Val = null, s1Badge = 'dim';
      if (curLap && t >= curLap.startSec + curLap.s1) {
        s1Val = curLap.s1;
      } else if (lastLap) {
        s1Val = lastLap.s1;
      }
      if (s1Val != null && s1Val > 10.0) {
        if (s1Val <= sessionBests.sessionBestS1 + 0.0001) s1Badge = 'purple';
        else if (s1Val <= pBestS1 + 0.0001) s1Badge = 'green';
        else s1Badge = 'yellow';
      }

      // S2
      let s2Val = null, s2Badge = 'dim';
      if (curLap && t >= curLap.startSec + curLap.s1 + curLap.s2) {
        s2Val = curLap.s2;
      } else if (lastLap) {
        s2Val = lastLap.s2;
      }
      if (s2Val != null && s2Val > 10.0) {
        if (s2Val <= sessionBests.sessionBestS2 + 0.0001) s2Badge = 'purple';
        else if (s2Val <= pBestS2 + 0.0001) s2Badge = 'green';
        else s2Badge = 'yellow';
      }

      // S3
      let s3Val = null, s3Badge = 'dim';
      if (lastLap && lastLap.s3 > 10.0) {
        s3Val = lastLap.s3;
        if (s3Val <= sessionBests.sessionBestS3 + 0.0001) s3Badge = 'purple';
        else if (s3Val <= pBestS3 + 0.0001) s3Badge = 'green';
        else s3Badge = 'yellow';
      }

      // Last Lap
      let lastLapStr = '-', lastLapBadge = 'dim';
      if (lastLap && lastLap.dur > 50.0) {
        lastLapStr = formatLapTime(lastLap.dur);
        if (lastLap.dur <= sessionBests.sessionBestLap + 0.0001) lastLapBadge = 'purple';
        else if (lastLap.dur <= pBestLap + 0.0001) lastLapBadge = 'green';
        else lastLapBadge = 'yellow';
      }

      // Best Lap
      let bestLapStr = '-', bestLapBadge = 'dim';
      if (pBestLap < Infinity) {
        bestLapStr = formatLapTime(pBestLap);
        if (pBestLap <= sessionBests.sessionBestLap + 0.0001) bestLapBadge = 'purple';
        else bestLapBadge = 'green';
      }

      return {
        curLap,
        lastLap,
        s1Str: formatSectorTime(s1Val),
        s1Badge,
        s2Str: formatSectorTime(s2Val),
        s2Badge,
        s3Str: formatSectorTime(s3Val),
        s3Badge,
        lastLapStr,
        lastLapBadge,
        bestLapStr,
        bestLapBadge,
        pBestLap: pBestLap < Infinity ? pBestLap : null
      };
    }

    function renderBadge(valStr, badgeType) {
      if (!valStr || valStr === '-') return '<span class="f1-badge badge-dim">-</span>';
      return '<span class="f1-badge badge-' + badgeType + '">' + valStr + '</span>';
    }

    // Binary search for track point on centerline
    function getTrackPointAtTime(sec) {
      const t = ((sec % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
      let low = 0, high = NODES.length - 1;
      while (low <= high) {
        const mid = (low + high) >> 1;
        if (NODES[mid][0] <= t) low = mid + 1;
        else high = mid - 1;
      }
      const i1 = Math.max(0, high);
      const i2 = (i1 + 1) % NODES.length;
      const n1 = NODES[i1];
      const n2 = NODES[i2];

      let dt = n2[0] - n1[0];
      if (dt <= 0) dt += LAP_DURATION;
      let u = 0;
      if (dt > 0) {
        let el = t - n1[0];
        if (el < 0) el += LAP_DURATION;
        u = Math.min(1, Math.max(0, el / dt));
      }

      return {
        px: n1[1] + (n2[1] - n1[1]) * u,
        py: n1[2] + (n2[2] - n1[2]) * u,
        x: n1[3] + (n2[3] - n1[3]) * u,
        y: n1[4] + (n2[4] - n1[4]) * u,
        sector: n1[5],
        speed: Math.round(n1[6] + (n2[6] - n1[6]) * u),
        gear: n1[7],
        rpm: Math.round(n1[8] + (n2[8] - n1[8]) * u),
        throttle: Math.round(n1[9] + (n2[9] - n1[9]) * u),
        brake: n1[10],
        drs: n1[11],
        location: n1[12]
      };
    }

    const SEPANG_INCIDENTS = ${incidentsJson};

    function getDriverVisualState(drvKey, t) {
      const gridIdx = GRID_ORDER.indexOf(drvKey);
      const pos = gridIdx >= 0 ? gridIdx : 21;
      const isRet = isDriverRetired(drvKey, t);
      const inPit = isDriverInPit(drvKey, t);

      // 1. Check incidents (off-track excursion or retirement)
      const inc = SEPANG_INCIDENTS.find(i => {
        if (i.driver !== drvKey) return false;
        if (t < i.startSec) return false;
        if (i.type === 'OFF_TRACK_REJOIN') {
          return t <= i.startSec + (i.durationSec || 14.0);
        }
        return true;
      });

      if (inc) {
        const dt = t - inc.startSec;
        const targetPt = { px: inc.targetPx, py: inc.targetPy };

        // Excursion with off-track movement & rejoin
        if (inc.type === 'OFF_TRACK_REJOIN') {
          const tEntry = inc.entryDuration || 3.0;
          const tMan = inc.maneuverDuration || 8.0;
          const tRejoin = inc.rejoinDuration || 3.0;

          if (dt < tEntry) {
            const u = Math.min(1.0, Math.max(0.0, dt / tEntry));
            const s = u * u * (3 - 2 * u);
            const originPt = getTrackPointAtTime(inc.trackSec);
            const px = originPt.px * (1 - s) + targetPt.px * s;
            const py = originPt.py * (1 - s) + targetPt.py * s;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: Math.round(originPt.speed * (1 - s) + 20 * s),
              gear: 1, throttle: 0, brake: 100,
              rpm: Math.round(11000 * (1 - s) + 4000 * s),
              drs: 0, inPit: false, isOffTrack: true, hazard: true, isRetired: false,
              status: 'OFF_TRACK_SLIDE',
              location: inc.locationName
            };
          } else if (dt < tEntry + tMan) {
            const u = (dt - tEntry) / tMan;
            const px = targetPt.px + Math.sin(u * Math.PI) * 2.0;
            const py = targetPt.py + Math.cos(u * Math.PI) * 1.5;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: 18,
              gear: 1, throttle: 25, brake: 10, rpm: 4500,
              drs: 0, inPit: false, isOffTrack: true, hazard: true, isRetired: false,
              status: 'ESCAPE_ROAD_MANEUVER',
              location: inc.locationName + ' — Manovra in Via di Fuga'
            };
          } else {
            const u = Math.min(1.0, Math.max(0.0, (dt - tEntry - tMan) / tRejoin));
            const s = u * u * (3 - 2 * u);
            const rejoinPt = getTrackPointAtTime(inc.trackSec + 2.5);
            const px = targetPt.px * (1 - s) + rejoinPt.px * s;
            const py = targetPt.py * (1 - s) + rejoinPt.py * s;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: Math.round(20 * (1 - s) + 140 * s),
              gear: u < 0.5 ? 2 : 3, throttle: 80, brake: 0,
              rpm: Math.round(5000 * (1 - s) + 10500 * s),
              drs: 0, inPit: false, isOffTrack: false, hazard: true, isRetired: false,
              status: 'REJOINING_TRACK',
              location: inc.locationName + ' — Rientro in Pista'
            };
          }
        }

        // Stopped Retirement Off-Track
        if (inc.type === 'STOPPED_RETIRED') {
          const tTrans = inc.transitionDuration || 3.0;
          const originPt = getTrackPointAtTime(inc.trackSec);
          if (dt < tTrans) {
            const u = Math.min(1.0, Math.max(0.0, dt / tTrans));
            const s = u * u * (3 - 2 * u);
            const px = originPt.px * (1 - s) + targetPt.px * s;
            const py = originPt.py * (1 - s) + targetPt.py * s;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: Math.round(originPt.speed * (1 - s)),
              gear: 0, throttle: 0, brake: 100,
              rpm: Math.round(9000 * (1 - s)),
              drs: 0, inPit: false, isOffTrack: true, hazard: true, isRetired: true,
              status: 'CRASH_TRANSITION',
              location: inc.locationName
            };
          } else {
            return {
              px: targetPt.px,
              py: targetPt.py,
              speed: 0,
              gear: 0, throttle: 0, brake: 0, rpm: 0,
              drs: 0, inPit: false, isOffTrack: true, hazard: false, isRetired: true,
              status: 'RETIRED_STOPPED',
              location: inc.locationName
            };
          }
        }
      }

      // Normal driving / Pit / Formation
      const drvSec = getDriverTrackSec(drvKey, t);
      const carPt = getTrackPointAtTime(drvSec);

      if (inPit) {
        const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
        let pitPx = 714, pitPy = 523.5;
        let inBox = false;
        if (activeStop) {
          const frac = Math.max(0, Math.min(1, (t - activeStop.startSec) / Math.max(1, (activeStop.endSec - activeStop.startSec))));
          const teamBoxDist = Math.max(100, Math.min(PIT_LANE_TOTAL_DIST - 100, 441.7 + (pos - 10) * 8.5));
          if (frac < 0.35) {
            const u = frac / 0.35;
            const pt = getPitLaneCoordAtDist(u * teamBoxDist);
            pitPx = pt.px; pitPy = pt.py;
          } else if (frac <= 0.65) {
            const pt = getPitLaneCoordAtDist(teamBoxDist);
            pitPx = pt.px; pitPy = pt.py;
            inBox = true;
          } else {
            const u = (frac - 0.65) / 0.35;
            const pt = getPitLaneCoordAtDist(teamBoxDist + u * (PIT_LANE_TOTAL_DIST - teamBoxDist));
            pitPx = pt.px; pitPy = pt.py;
          }
        }
        return {
          px: Number(pitPx.toFixed(2)),
          py: Number(pitPy.toFixed(2)),
          speed: inBox ? 0 : 80,
          gear: inBox ? 1 : 2,
          rpm: inBox ? 3500 : 4500,
          throttle: inBox ? 0 : 35,
          brake: inBox ? 1 : 0, drs: 0, inPit: true, isOffTrack: false, hazard: false, isRetired: false,
          status: inBox ? 'PIT_BOX_STOP' : 'PIT_LANE_DRIVE',
          location: inBox ? 'Corsia Box — Piazzola Sostituzione Gomme' : 'Corsia Box — Limitatore di Velocità Attivo (80 km/h)'
        };
      }

      // Check pit exit merge (2.0s)
      const recentExit = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= p.endSec + 2.0);
      if (recentExit) {
        const u = Math.min(1.0, Math.max(0.0, (t - recentExit.endSec) / 2.0));
        const px = (1 - u) * 141.0 + u * carPt.px;
        const py = (1 - u) * 585.5 + u * carPt.py;
        return {
          px: Number(px.toFixed(2)),
          py: Number(py.toFixed(2)),
          speed: Math.round(80 * (1 - u) + carPt.speed * u),
          gear: carPt.gear, rpm: carPt.rpm, throttle: carPt.throttle, brake: carPt.brake, drs: 0,
          inPit: false, isOffTrack: false, hazard: false, isRetired: false,
          status: 'PIT_EXIT_MERGE',
          location: 'Rientro in Pista da Pit Lane verso Curva 1'
        };
      }

      // Smooth lateral offset on straight for grid lining up (t = 195.0..224.0s)
      let latX = 0, latY = 0;
      if (t >= 195.0 && t < 224.0) {
        const stopTime = 204.0 + pos * 0.42;
        const rawOffset = (pos % 2 === 0) ? -5.5 : +5.5;
        let latOffset = 0;
        if (t < stopTime) {
          latOffset = rawOffset * ((t - 195.0) / Math.max(1.0, stopTime - 195.0));
        } else if (t <= 216.9) {
          latOffset = rawOffset;
        } else {
          latOffset = rawOffset * Math.max(0, 1.0 - (t - 216.9) / 6.0);
        }
        latX = -latOffset * 0.07;
        latY = -latOffset * 0.99;
      }

      // Dynamic telemetry computation based on exact track physics and session phase
      let speed, gear, rpm, throttle, brake, drs, status, loc;
      const isSC = isSafetyCarActive(t);
      const isVSC = isVSCActive(t);
      const isFinished = t >= RACE_FINISH_SEC;

      if (t < 195.0) {
        // Formation Laps 1 & 2 behind Safety Car (Wet Conditions & Controlled Pace)
        // Cornering speed down in turns, downshifts, braking zones, acceleration on straights
        const scMaxSpeed = t < 108.0 ? 152 : 148;
        speed = Math.round(carPt.speed > scMaxSpeed ? scMaxSpeed + (carPt.speed - scMaxSpeed) * 0.12 : carPt.speed * 0.92);
        gear = speed < 90 ? 2 : (speed < 130 ? 3 : (speed < 170 ? 4 : (speed < 220 ? 5 : 6)));
        brake = carPt.brake;
        throttle = carPt.brake === 1 ? 0 : Math.round(Math.min(75, (speed / scMaxSpeed) * 75));
        rpm = Math.round(5200 + (speed / scMaxSpeed) * 5800 + (drv.rpmDelta || 0));
        drs = 0;
        status = 'FORMATION';
        loc = (t < 108.0 ? '🟡 Giro Formazione 1 (SC) — ' : '🟡 Giro Formazione 2 (SC) — ') + (carPt.location || 'Sepang');
      } else if (t < 216.9) {
        // Grid Lineup before Standing Start
        const stopTime = 204.0 + pos * 0.42;
        if (t < stopTime) {
          const u = Math.min(1.0, Math.max(0.0, (t - 195.0) / Math.max(1.0, stopTime - 195.0)));
          speed = Math.round(Math.max(0, 50 * (1 - u)));
          gear = speed > 20 ? 2 : 1;
          rpm = Math.round(4500 + speed * 35);
          throttle = speed > 20 ? 15 : 0;
          brake = speed < 25 ? 1 : 0;
          drs = 0;
          status = 'FORMATION';
          loc = '🏁 Rientro Safety Car — Posizionamento in Casella P' + (pos + 1);
        } else {
          speed = 0;
          gear = 1;
          drs = 0;
          brake = 1;
          status = 'GRID_STANDING';
          if (t >= 212.0) {
            rpm = 11600;
            throttle = 100;
            loc = '🚦 Semafori Accesi — Launch Control Attivo (Casella P' + (pos + 1) + ')';
          } else {
            rpm = 5000;
            throttle = 0;
            loc = '🏁 Schieramento in Griglia — Fermo in Casella P' + (pos + 1);
          }
        }
      } else if (t < 224.0) {
        // Standing start launch phase
        const dt = t - 216.9;
        speed = Math.round(Math.min(280, dt * 45));
        gear = speed < 80 ? 1 : (speed < 140 ? 2 : (speed < 190 ? 3 : (speed < 235 ? 4 : (speed < 265 ? 5 : 6))));
        rpm = Math.min(12500, Math.round(10500 + dt * 250));
        throttle = 100;
        brake = 0;
        drs = 0;
        status = 'RACING';
        loc = '⚡ Partenza da Fermo (Standing Start) — Allungo verso Curva 1';
      } else if (isSC || isVSC) {
        speed = Math.min(160, Math.round(carPt.speed * 0.62));
        rpm = Math.min(9600, Math.round(carPt.rpm * 0.72));
        throttle = carPt.brake === 1 ? 0 : Math.min(50, carPt.throttle);
        brake = carPt.brake;
        gear = Math.min(6, carPt.gear);
        drs = 0;
        status = isSC ? 'SAFETY_CAR' : 'VSC';
        loc = (isSC ? '🟠 SAFETY CAR IN PISTA — ' : '🟡 VSC DELTA — ') + carPt.location;
      } else if (isFinished) {
        speed = Math.min(120, Math.round(carPt.speed * 0.45));
        rpm = 7200;
        throttle = 35;
        brake = carPt.brake;
        gear = 4;
        drs = 0;
        status = 'FINISHED';
        loc = '🏁 In-Lap / Cooldown — Rientro in Parc Fermé';
      } else {
        speed = Math.max(50, Math.round(carPt.speed + (drv.speedDelta || 0)));
        rpm = Math.max(9500, Math.round(carPt.rpm + (drv.rpmDelta || 0)));
        brake = carPt.brake;
        throttle = carPt.brake === 1 ? 0 : carPt.throttle;
        gear = carPt.gear;
        drs = carPt.drs;
        status = 'RACING';
        loc = '📍 ' + carPt.location;
      }

      return {
        px: Number((carPt.px + latX).toFixed(2)),
        py: Number((carPt.py + latY).toFixed(2)),
        speed: Math.round(speed),
        gear,
        rpm: Math.round(rpm),
        throttle: Math.round(throttle),
        brake,
        drs,
        inPit: false,
        isOffTrack: false,
        hazard: false,
        isRetired: isRet,
        status: isRet ? 'RETIRED' : status,
        location: loc
      };
    }

    const canvas = document.getElementById('trackCanvas');
    const ctx = canvas.getContext('2d');

    // Load official Sepang circuit image
    const bgImage = new Image();
    let bgLoaded = false;
    bgImage.onload = () => { bgLoaded = true; };
    bgImage.src = 'Sepang.svg.webp';

    let currentSecond = 0;
    let isPlaying = true;
    let speedMult = 2;
    let selectedDriver = 'all';  // 'all' or specific driver number (e.g. '1', '3', '16')
    let focusedDriver = '3';     // Driver whose telemetry is displayed in the cockpit HUD (#3 Verstappen - Winner)
    let lastTableUpdateSec = -1;
    let lastEventText = '';

    function resizeCanvas() {
      canvas.width = canvas.parentElement.clientWidth;
      canvas.height = canvas.parentElement.clientHeight;
    }
    window.addEventListener('resize', resizeCanvas);
    resizeCanvas();

    function getViewportBounds() {
      const pad = 24;
      const availW = Math.max(100, canvas.width - pad * 2);
      const availH = Math.max(100, canvas.height - pad * 2);
      const scale = Math.min(availW / SVG_WIDTH, availH / SVG_HEIGHT);
      const drawW = SVG_WIDTH * scale;
      const drawH = SVG_HEIGHT * scale;
      const ox = pad + (availW - drawW) / 2;
      const oy = pad + (availH - drawH) / 2;
      return { ox, oy, drawW, drawH, scale };
    }

    function pxToScreen(px, py) {
      const bounds = getViewportBounds();
      return {
        x: bounds.ox + (px / SVG_WIDTH) * bounds.drawW,
        y: bounds.oy + (py / SVG_HEIGHT) * bounds.drawH
      };
    }

    // Cache of screen positions for mouse hit testing
    const carScreenPositions = {};

    function drawTrack() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const bounds = getViewportBounds();

      // 1. Draw Official Sepang Track Map (clean, NO background box)
      if (bgLoaded) {
        ctx.save();
        ctx.drawImage(bgImage, bounds.ox, bounds.oy, bounds.drawW, bounds.drawH);
        ctx.restore();
      } else {
        // Fallback: draw crisp asphalt centerline so circuit is always immediately visible
        ctx.save();
        ctx.strokeStyle = '#21262d';
        ctx.lineWidth = 12;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.beginPath();
        for (let i = 0; i < NODES.length; i++) {
          const pt = pxToScreen(NODES[i][1], NODES[i][2]);
          if (i === 0) ctx.moveTo(pt.x, pt.y);
          else ctx.lineTo(pt.x, pt.y);
        }
        ctx.closePath();
        ctx.stroke();
        ctx.restore();
      }

      // Active driver key for telemetry HUD
      const activeKey = (selectedDriver === 'all') ? focusedDriver : selectedDriver;
      const driverKeys = Object.keys(DRIVERS);

      // Compute progress for all drivers at currentSecond
      const driverProgressMap = {};
      driverKeys.forEach(k => {
        driverProgressMap[k] = getDriverProgress(k, currentSecond);
      });

      // 2. Draw Pit Lane Path & Pit Boxes Zone
      ctx.save();
      ctx.strokeStyle = '#388bfd';
      ctx.lineWidth = 2.5;
      ctx.lineCap = 'round';
      ctx.setLineDash([5, 4]);
      ctx.beginPath();
      for (let pi = 0; pi < PIT_LANE_NODES.length; pi++) {
        const pscr = pxToScreen(PIT_LANE_NODES[pi].px, PIT_LANE_NODES[pi].py);
        if (pi === 0) ctx.moveTo(pscr.x, pscr.y);
        else ctx.lineTo(pscr.x, pscr.y);
      }
      ctx.stroke();
      ctx.setLineDash([]);

      const pbScr = pxToScreen(714, 523.5);
      ctx.fillStyle = 'rgba(56, 139, 253, 0.15)';
      ctx.fillRect(pbScr.x - 50, pbScr.y - 7, 100, 14);
      ctx.strokeStyle = '#58a6ff';
      ctx.lineWidth = 1;
      ctx.strokeRect(pbScr.x - 50, pbScr.y - 7, 100, 14);
      ctx.fillStyle = '#58a6ff';
      ctx.font = 'bold 8px ui-monospace, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('PIT BOXES 80 km/h', pbScr.x, pbScr.y - 9);
      ctx.restore();

      // 3. Starting Gantry (5 Red Lights) during standing start
      if (currentSecond >= 210 && currentSecond <= 221) {
        ctx.save();
        const gCenter = pxToScreen(714, 485);
        const elapsedGantry = currentSecond - 210;
        const lightsOn = currentSecond < 216.9 ? Math.min(5, Math.floor(elapsedGantry / 1.2) + 1) : 0;
        ctx.fillStyle = '#0d1117';
        ctx.strokeStyle = '#30363d';
        ctx.lineWidth = 1.5;
        if (ctx.roundRect) ctx.roundRect(gCenter.x - 65, gCenter.y - 14, 130, 28, 6);
        else ctx.rect(gCenter.x - 65, gCenter.y - 14, 130, 28);
        ctx.fill();
        ctx.stroke();

        for (let li = 0; li < 5; li++) {
          const lx = (gCenter.x - 65) + 17 + li * 24;
          ctx.beginPath();
          ctx.arc(lx, gCenter.y, 6.0, 0, Math.PI * 2);
          if (li < lightsOn) {
            ctx.fillStyle = '#ff1801';
            ctx.shadowColor = '#ff1801';
            ctx.shadowBlur = 10;
          } else {
            ctx.fillStyle = '#21262d';
            ctx.shadowBlur = 0;
          }
          ctx.fill();
        }
        ctx.restore();
      }

      // 4. Draw all 22 drivers or selected driver
      driverKeys.forEach(drvKey => {
        const drv = DRIVERS[drvKey];
        if (!drv) return;

        const myProg = driverProgressMap[drvKey];
        const state = getDriverVisualState(drvKey, currentSecond);
        const gridIdx = GRID_ORDER.indexOf(drvKey);
        let carScr = pxToScreen(state.px, state.py);

        // Dynamic Lateral Separation during battles & overtakes (only on track)
        if (!state.inPit && !state.isOffTrack && !state.isRetired && currentSecond > 224.0) {
          let lateralOffset = 0;
          for (const otherKey of driverKeys) {
            if (otherKey === drvKey) continue;
            const otherProg = driverProgressMap[otherKey];
            const diff = Math.abs(myProg - otherProg);
            if (diff < 1.0) {
              const side = (myProg > otherProg) ? 1 : -1;
              lateralOffset = side * 4.5 * (1.0 - diff);
              break;
            }
          }
          if (lateralOffset !== 0) {
            const drvSec = getDriverTrackSec(drvKey, currentSecond);
            const nextSec = (drvSec + 0.5) % LAP_DURATION;
            const nextPt = getTrackPointAtTime(nextSec);
            const nextScr = pxToScreen(nextPt.px, nextPt.py);
            const dx = nextScr.x - carScr.x;
            const dy = nextScr.y - carScr.y;
            const len = Math.hypot(dx, dy) || 1;
            carScr.x += (-dy / len) * lateralOffset;
            carScr.y += (dx / len) * lateralOffset;
          }
        }

        carScreenPositions[drvKey] = { x: carScr.x, y: carScr.y, drv: drv };
        const isCurrentActive = drvKey === activeKey;

        // If a specific driver is selected, dim other drivers
        if (selectedDriver !== 'all' && selectedDriver !== drvKey) {
          ctx.save();
          ctx.beginPath();
          ctx.arc(carScr.x, carScr.y, 4, 0, Math.PI * 2);
          ctx.fillStyle = 'rgba(100, 100, 100, 0.35)';
          ctx.fill();
          ctx.restore();
          return;
        }

        // Draw car dot
        ctx.save();
        const baseRadius = isCurrentActive ? 9.5 : 7.0;

        // Outer glow
        ctx.beginPath();
        ctx.arc(carScr.x, carScr.y, baseRadius + (isCurrentActive ? 5 : (state.isOffTrack ? 4 : 2)), 0, Math.PI * 2);
        ctx.fillStyle = isCurrentActive 
          ? 'rgba(88, 166, 255, 0.45)' 
          : (state.inPit ? 'rgba(210, 153, 34, 0.3)' : (state.isOffTrack ? 'rgba(255, 170, 0, 0.55)' : 'rgba(0, 0, 0, 0.5)'));
        ctx.fill();

        // Main dot
        ctx.beginPath();
        ctx.arc(carScr.x, carScr.y, baseRadius, 0, Math.PI * 2);
        ctx.fillStyle = state.inPit ? '#d29922' : (state.isOffTrack ? '#ffaa00' : drv.color);
        ctx.fill();
        ctx.lineWidth = isCurrentActive ? 3.0 : (state.isOffTrack ? 2.5 : 1.5);
        ctx.strokeStyle = isCurrentActive ? '#ffffff' : (state.isOffTrack ? '#ff3b30' : '#0d1117');
        ctx.stroke();

        // Driver number text
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold ' + (isCurrentActive ? '10px' : '8px') + ' ui-monospace, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(drv.number, carScr.x, carScr.y);

        // Driver code label badge
        if (selectedDriver === 'all' || isCurrentActive) {
          ctx.font = 'bold 9px -apple-system, sans-serif';
          ctx.fillStyle = isCurrentActive ? '#ffffff' : '#e6edf3';
          let label = drv.code;
          if (state.inPit) label += ' [PIT]';
          else if (state.isOffTrack) label += ' [OFF]';
          else if (state.status === 'REJOINING_TRACK') label += ' [REJOIN]';
          else if (currentSecond <= 0.8) label += ' P' + (gridIdx + 1);
          ctx.fillText(label, carScr.x, carScr.y - baseRadius - 5);
        }

        ctx.restore();
      });

      // 5. Draw Safety Car Vehicle (Active on Track vs Parked at Pit Exit)
      const scActive = isSafetyCarActive(currentSecond);
      let scScr;
      if (scActive) {
        // Place Safety Car leading the race pack ahead of leader
        const leaderKey = driverKeys.reduce((best, k) => (driverProgressMap[k] > (driverProgressMap[best] || -999) ? k : best), '3');
        const scSec = (getDriverTrackSec(leaderKey, currentSecond) + 2.5) % LAP_DURATION;
        const scPt = getTrackPointAtTime(scSec);
        scScr = pxToScreen(scPt.px, scPt.py);
      } else {
        // Parked at pit exit
        scScr = pxToScreen(155, 578);
      }

      ctx.save();
      const isBeaconFlash = scActive && (Math.floor(Date.now() / 200) % 2 === 0);
      const scRadius = scActive ? 10.5 : 7.0;

      // Glow aura
      ctx.beginPath();
      ctx.arc(scScr.x, scScr.y, scRadius + (scActive ? 6 : 2), 0, Math.PI * 2);
      ctx.fillStyle = scActive ? (isBeaconFlash ? 'rgba(255, 152, 0, 0.65)' : 'rgba(255, 87, 34, 0.45)') : 'rgba(100, 100, 100, 0.2)';
      ctx.fill();

      // Main car body
      ctx.beginPath();
      ctx.arc(scScr.x, scScr.y, scRadius, 0, Math.PI * 2);
      ctx.fillStyle = scActive ? '#ff9800' : '#484f58';
      ctx.fill();
      ctx.lineWidth = 2.0;
      ctx.strokeStyle = scActive ? '#ffffff' : '#8b949e';
      ctx.stroke();

      // Flashing roof beacon lights
      if (scActive) {
        ctx.fillStyle = isBeaconFlash ? '#ffffff' : '#ffeb3b';
        ctx.beginPath();
        ctx.arc(scScr.x - 4, scScr.y - 7, 2.5, 0, Math.PI * 2);
        ctx.arc(scScr.x + 4, scScr.y - 7, 2.5, 0, Math.PI * 2);
        ctx.fill();
      }

      // SC Label
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold ' + (scActive ? '10px' : '8px') + ' ui-monospace, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('SC', scScr.x, scScr.y);

      ctx.font = 'bold 9px -apple-system, sans-serif';
      ctx.fillStyle = scActive ? '#ff9800' : '#8b949e';
      ctx.fillText(scActive ? 'SAFETY CAR' : '[SC] PIT EXIT', scScr.x, scScr.y - scRadius - 5);
      ctx.restore();

      // 6. Start/Finish Line badge
      const s0 = pxToScreen(NODES[0][1], NODES[0][2]);
      ctx.save();
      ctx.fillStyle = '#238636';
      ctx.beginPath();
      ctx.arc(s0.x, s0.y, 5, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#2ea043';
      ctx.font = 'bold 9px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('FINISH', s0.x, s0.y + 14);
      ctx.restore();
    }

    function selectDriverFromTable(drvKey) {
      if (!DRIVERS[drvKey]) return;
      focusedDriver = drvKey;
      if (selectedDriver !== 'all') {
        selectedDriver = drvKey;
        document.getElementById('driverSelect').value = drvKey;
      }
      updateHUD();
      drawTrack();
    }

    function updateHUD() {
      const activeKey = (selectedDriver === 'all') ? focusedDriver : selectedDriver;
      const drv = DRIVERS[activeKey] || DRIVERS['1'];

      // Active driver total progress and in-lap status
      const prog = getDriverProgress(activeKey, currentSecond);
      const activeSec = getDriverTrackSec(activeKey, currentSecond);
      const isRet = isDriverRetired(activeKey, currentSecond);
      const inPit = isDriverInPit(activeKey, currentSecond);
      const tire = getDriverTire(activeKey, currentSecond);
      const isVSC = isVSCActive(currentSecond);
      const isSC = isSafetyCarActive(currentSecond);
      const isFinished = currentSecond >= 6435;

      const pt = getTrackPointAtTime(activeSec);

      // Banner update
      document.getElementById('bannerColorBar').style.background = drv.color;
      document.getElementById('bannerNum').textContent = '#' + drv.number;
      document.getElementById('bannerName').textContent = drv.firstName + ' ' + drv.lastName + ' (' + drv.code + ')';
      document.getElementById('bannerTeam').textContent = drv.team;
      
      const bannerStatus = document.getElementById('bannerStatus');
      const state = getDriverVisualState(activeKey, currentSecond);
      const isOff = state.isOffTrack;

      if (isRet) {
        bannerStatus.style.background = '#da3633';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = 'RITIRATO (DNF)';
      } else if (isOff) {
        bannerStatus.style.background = '#ffaa00';
        bannerStatus.style.color = '#000';
        bannerStatus.textContent = 'FUORI PISTA (OFF TRACK)';
      } else if (inPit) {
        bannerStatus.style.background = '#d29922';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = currentSecond >= 6550 ? 'PARC FERMÉ' : 'PIT LANE';
      } else if (isFinished) {
        bannerStatus.style.background = '#8957e5';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = 'TRAGUARDO';
      } else if (isSC || isVSC) {
        bannerStatus.style.background = '#e3b341';
        bannerStatus.style.color = '#000';
        bannerStatus.textContent = isSC ? 'SAFETY CAR' : 'VSC DELTA';
      } else {
        bannerStatus.style.background = '#238636';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = 'TRACK LIVE';
      }

      const bannerTire = document.getElementById('bannerTire');
      bannerTire.className = 'tire-badge tire-' + tire;
      bannerTire.textContent = tire === 'I' ? 'INTERMEDIATE' : (tire === 'S' ? 'SOFT' : (tire === 'M' ? 'MEDIUM' : (tire === 'W' ? 'WET' : 'HARD')));

      // Cockpit Telemetry HUD
      const activeStop = PIT_STOPS.find(p => p.driver === activeKey && currentSecond >= p.startSec && currentSecond <= p.endSec);
      let pitFrac = 0;
      if (activeStop) {
        pitFrac = Math.max(0, Math.min(1, (currentSecond - activeStop.startSec) / Math.max(1, (activeStop.endSec - activeStop.startSec))));
      }

      // Cockpit Telemetry HUD
      const speed = state.speed;
      const rpm = state.rpm;
      const throttle = state.throttle;
      const gear = state.gear;
      const drs = state.drs;
      const locText = state.isRetired ? ('🛑 ' + state.location) : (state.isOffTrack ? ('⚠️ ' + state.location) : (state.status === 'REJOINING_TRACK' ? ('↩️ ' + state.location) : (inPit ? ('🔧 ' + state.location) : state.location));

      document.getElementById('gearVal').textContent = gear;
      document.getElementById('speedVal').textContent = Math.round(speed);
      document.getElementById('rpmVal').textContent = rpm.toLocaleString() + ' RPM';

      // RPM Shift Lights
      const leds = document.querySelectorAll('.led');
      const rpmFrac = Math.max(0, Math.min(1, (rpm - 10000) / 2500));
      const activeLeds = Math.round(rpmFrac * leds.length);
      leds.forEach((led, idx) => {
        if (idx < activeLeds) led.classList.add('on');
        else led.classList.remove('on');
      });

      // Throttle Bar
      document.getElementById('throttleVal').textContent = throttle + '%';
      document.getElementById('throttleFill').style.width = throttle + '%';

      // Brake & DRS
      const isBraking = state.brake === 1 || (state.status === 'OFF_TRACK_SLIDE' || state.status === 'CRASH_TRANSITION');
      const brakeTag = document.getElementById('brakeTag');
      if (isRet) {
        brakeTag.className = 'tag';
        brakeTag.textContent = 'VETTURA FERMA';
      } else if (inPit) {
        brakeTag.className = 'tag pit-on';
        brakeTag.textContent = (activeStop && pitFrac >= 0.35 && pitFrac <= 0.65) ? 'CAMBIO GOMME (BOX)' : 'PIT LIMITER (80 KM/H)';
      } else if (isBraking) {
        brakeTag.className = 'tag brake-on';
        brakeTag.textContent = (currentSecond >= 212 && currentSecond < 216.9) ? 'FRENO ATTIVO (LAUNCH)' : 'FRENO ATTIVO';
      } else {
        brakeTag.className = 'tag';
        brakeTag.textContent = 'FRENO (OFF)';
      }

      const drsTag = document.getElementById('drsTag');
      if (drs === 1) {
        drsTag.className = 'tag drs-on';
        drsTag.textContent = 'ACTIVE AERO: Z-MODE (LOW DRAG)';
      } else {
        drsTag.className = 'tag';
        drsTag.textContent = 'ACTIVE AERO: X-MODE (HIGH DOWNFORCE)';
      }

      // Battery SoC & Manual Override Mode (MOM 2026)
      const soc = Math.round(68 + 24 * Math.sin(currentSecond * 0.15) - (throttle > 80 ? 6 : -4));
      const batterySoc = Math.min(100, Math.max(15, soc));
      const socEl = document.getElementById('socVal');
      if (socEl) socEl.textContent = batterySoc + '%';

      const momEl = document.getElementById('momVal');
      if (momEl) {
        const gapVal = getDriverGap(activeKey, currentSecond);
        if (drs === 1 && throttle > 90) {
          momEl.textContent = '⚡ BOOST ATTIVO';
          momEl.style.color = '#d2a8ff';
        } else if (gapVal > 0 && (gapVal % 1.2) <= 1.0) {
          momEl.textContent = '🟢 DISPONIBILE (<1.0s)';
          momEl.style.color = '#3fb950';
        } else {
          momEl.textContent = 'STANDBY';
          momEl.style.color = '#8b949e';
        }
      }

      // Location & GPS Decimeters
      document.getElementById('locName').textContent = locText;
      document.getElementById('gpsCoords').textContent = 'GPS: X = ' + (pt.x >= 0 ? '+' : '') + pt.x + '.0m, Y = ' + (pt.y >= 0 ? '+' : '') + pt.y + '.0m (X:' + (pt.x * 10) + ' dm, Y:' + (pt.y * 10) + ' dm)';
      document.getElementById('svgCoords').textContent = 'Coordinate Asfalto SVG: px = ' + pt.px.toFixed(2) + ', py = ' + pt.py.toFixed(2);

      // Sector Badge
      const secBadge = document.getElementById('sectorBadge');
      secBadge.className = 'sector-badge sec-' + (isRet ? 2 : (inPit ? 1 : pt.sector));
      secBadge.textContent = isRet ? 'OUT' : (inPit ? (currentSecond >= 6550 ? 'PARC FERMÉ' : 'PIT LANE') : 'SETTORE ' + pt.sector);

      // Update Cockpit Lap & Sector Timing Card
      const sessionBests = getSessionBests(currentSecond);
      const activeStats = getDriverTimingStats(activeKey, currentSecond, sessionBests);
      const curLapNum = getDriverLap(activeKey, currentSecond);
      document.getElementById('hudLapNumBadge').textContent = 'GIRO ' + curLapNum + ' / 55';

      const hudSec1Box = document.getElementById('hudSec1Box');
      const hudSec2Box = document.getElementById('hudSec2Box');
      const hudSec3Box = document.getElementById('hudSec3Box');
      hudSec1Box.classList.remove('active-sec');
      hudSec2Box.classList.remove('active-sec');
      hudSec3Box.classList.remove('active-sec');

      if (!isRet && !inPit && currentSecond >= 216.9) {
        if (pt.sector === 1) hudSec1Box.classList.add('active-sec');
        else if (pt.sector === 2) hudSec2Box.classList.add('active-sec');
        else if (pt.sector === 3) hudSec3Box.classList.add('active-sec');
      }

      // Sector 1
      const hudSec1Val = document.getElementById('hudSec1Val');
      const hudSec1Tag = document.getElementById('hudSec1Tag');
      hudSec1Val.textContent = activeStats.s1Str;
      hudSec1Tag.className = 'hud-sec-tag f1-badge badge-' + activeStats.s1Badge;
      hudSec1Tag.textContent = activeStats.s1Badge === 'purple' ? 'ASSOLUTO' : (activeStats.s1Badge === 'green' ? 'PERSONALE' : (activeStats.s1Badge === 'yellow' ? 'NO MIGL.' : 'IN ATTESA'));

      // Sector 2
      const hudSec2Val = document.getElementById('hudSec2Val');
      const hudSec2Tag = document.getElementById('hudSec2Tag');
      hudSec2Val.textContent = activeStats.s2Str;
      hudSec2Tag.className = 'hud-sec-tag f1-badge badge-' + activeStats.s2Badge;
      hudSec2Tag.textContent = activeStats.s2Badge === 'purple' ? 'ASSOLUTO' : (activeStats.s2Badge === 'green' ? 'PERSONALE' : (activeStats.s2Badge === 'yellow' ? 'NO MIGL.' : 'IN ATTESA'));

      // Sector 3
      const hudSec3Val = document.getElementById('hudSec3Val');
      const hudSec3Tag = document.getElementById('hudSec3Tag');
      hudSec3Val.textContent = activeStats.s3Str;
      hudSec3Tag.className = 'hud-sec-tag f1-badge badge-' + activeStats.s3Badge;
      hudSec3Tag.textContent = activeStats.s3Badge === 'purple' ? 'ASSOLUTO' : (activeStats.s3Badge === 'green' ? 'PERSONALE' : (activeStats.s3Badge === 'yellow' ? 'NO MIGL.' : 'IN ATTESA'));

      // Last Lap
      const hudLastLapVal = document.getElementById('hudLastLapVal');
      const hudLastLapTag = document.getElementById('hudLastLapTag');
      hudLastLapVal.textContent = activeStats.lastLapStr;
      hudLastLapTag.className = 'hud-lap-status f1-badge badge-' + activeStats.lastLapBadge;
      hudLastLapTag.textContent = activeStats.lastLapBadge === 'purple' ? 'ASSOLUTO' : (activeStats.lastLapBadge === 'green' ? 'MIGLIOR PERSONALE' : (activeStats.lastLapBadge === 'yellow' ? 'NO MIGLIORAMENTO' : 'NESSUN GIRO'));

      // Best Lap
      const hudBestLapVal = document.getElementById('hudBestLapVal');
      const hudBestLapTag = document.getElementById('hudBestLapTag');
      hudBestLapVal.textContent = activeStats.bestLapStr;
      hudBestLapTag.className = 'hud-lap-status f1-badge badge-' + activeStats.bestLapBadge;
      hudBestLapTag.textContent = activeStats.bestLapBadge === 'purple' ? 'MIGLIORE GARA' : (activeStats.bestLapBadge === 'green' ? 'RECORD PERSONALE' : 'IN ATTESA');

      // Fastest Lap of the session footer
      const fastestLapValEl = document.getElementById('hudSessionFastestVal');
      const fastestLapDrvEl = document.getElementById('hudSessionFastestDrv');
      if (sessionBests.sessionBestLap < Infinity) {
        fastestLapValEl.textContent = formatLapTime(sessionBests.sessionBestLap);
        const fDrv = DRIVERS[sessionBests.sessionBestLapDriver];
        fastestLapDrvEl.textContent = fDrv ? ('#' + fDrv.number + ' ' + fDrv.code) : '';
      } else {
        fastestLapValEl.textContent = '-';
        fastestLapDrvEl.textContent = 'In attesa';
      }

      // Header Clock & Timeline Scrubber (08:33:00 Official Race Director Start)
      const baseSec = 8 * 3600 + 33 * 60 + Math.floor(currentSecond);
      const hh = String(Math.floor(baseSec / 3600) % 24).padStart(2, '0');
      const mm = String(Math.floor((baseSec % 3600) / 60)).padStart(2, '0');
      const ss = String(baseSec % 60).padStart(2, '0');
      
      let lapText = '';
      if (currentSecond < 108.0) {
        lapText = 'Giro Formazione 1 (Dietro Safety Car)';
      } else if (currentSecond < 205.0) {
        lapText = 'Giro Formazione 2 (Dietro Safety Car)';
      } else if (currentSecond < 216.9) {
        lapText = 'Schieramento in Griglia (Attesa Partenza)';
      } else if (currentSecond < RACE_FINISH_SEC) {
        const lapNum = getDriverLap(activeKey, currentSecond);
        lapText = 'Giro: ' + lapNum + '/55 (Gara Ufficiale)';
      } else if (currentSecond < SESSION_DURATION) {
        lapText = 'Giro: 55/55 🏁 BANDIERA A SCACCHI';
      } else {
        lapText = 'Giro: 55/55 🏁 GARA CONCLUSA (Parc Fermé)';
      }

      document.getElementById('headerClock').textContent = '4 Ottobre 2026 | ' + hh + ':' + mm + ':' + ss + ' UTC | ' + lapText;
      document.getElementById('timeScrubber').value = Math.floor(currentSecond);

      // Update Live Timing Tower & Race Control once per second
      const secFloor = Math.floor(currentSecond);
      if (secFloor !== lastTableUpdateSec) {
        lastTableUpdateSec = secFloor;
        updateTimingTower(activeKey);
        updateEventTicker();
        updateRaceControl();
      }
    }

    function updateEventTicker() {
      let latest = RACE_EVENTS[0];
      for (const ev of RACE_EVENTS) {
        if (ev.timeSec <= currentSecond) {
          latest = ev;
        } else {
          break;
        }
      }
      if (latest && latest.text !== lastEventText) {
        lastEventText = latest.text;
        const ticker = document.getElementById('overtakeTicker');
        ticker.textContent = latest.text;
        ticker.style.color = '#ff8000';
        setTimeout(() => { ticker.style.color = '#f0f6fc'; }, 400);
      }
    }

    function updateRaceControl() {
      const currentUtcSec = Math.floor(currentSecond);
      const activeMsgs = RACE_CONTROL_MESSAGES.filter(m => m.timeSec <= currentUtcSec);

      // Badges
      const flagBadge = document.getElementById('rcFlagBadge');
      const drsBadge = document.getElementById('rcDrsBadge');

      if (currentSecond >= 6435) {
        flagBadge.className = 'rc-pill rc-flag-chequered';
        flagBadge.textContent = '🏁 BANDIERA A SCACCHI';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else if (currentSecond < 205.0) {
        flagBadge.className = 'rc-pill rc-flag-vsc';
        flagBadge.textContent = '🟠 FORMAZIONE (SAFETY CAR)';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else if (currentSecond < 216.9) {
        flagBadge.className = 'rc-pill rc-flag-vsc';
        flagBadge.textContent = '🔴 SCHIERAMENTO (SEMAFORI ROSSI)';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else if (isSafetyCarActive(currentSecond)) {
        flagBadge.className = 'rc-pill rc-flag-vsc';
        flagBadge.textContent = '🟠 SAFETY CAR DEPLOYED';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else if (isVSCActive(currentSecond)) {
        flagBadge.className = 'rc-pill rc-flag-vsc';
        flagBadge.textContent = '🟡 VSC DEPLOYED';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else {
        flagBadge.className = 'rc-pill rc-flag-green';
        flagBadge.textContent = '🟢 GARA ATTIVA (BANDIERA VERDE)';
        if (currentSecond >= 400 && !isSafetyCarActive(currentSecond) && !isVSCActive(currentSecond)) {
          drsBadge.className = 'rc-pill rc-drs-on';
          drsBadge.textContent = 'DRS ON';
        } else {
          drsBadge.className = 'rc-pill rc-drs-off';
          drsBadge.textContent = 'DRS OFF';
        }
      }

      document.getElementById('rcTotalCount').textContent = activeMsgs.length + ' / ' + RACE_CONTROL_MESSAGES.length + ' Messaggi';

      // Recent messages (reverse chronological)
      const recent = activeMsgs.slice(-7).reverse();
      const feedBox = document.getElementById('rcFeedBox');
      let html = '';
      recent.forEach(m => {
        let tagClass = 'rc-tag-other';
        let tagLabel = m.category;
        if (m.category === 'Flag') { tagClass = 'rc-tag-flag'; tagLabel = m.flag || 'FLAG'; }
        else if (m.category === 'SafetyCar') { tagClass = 'rc-tag-vsc'; tagLabel = 'SAFETY CAR'; }
        else if (m.category === 'Drs') { tagClass = 'rc-tag-drs'; tagLabel = 'DRS'; }
        else if (m.category === 'TrackLimits') { tagClass = 'rc-tag-limits'; tagLabel = 'LIMITS'; }

        const timeStr = m.date.substring(11, 19);
        let lapStr = m.lap_number ? ('L' + m.lap_number) : 'PRE';
        if (m.timeSec <= 0) {
          lapStr = 'PRE';
          const umsg = (m.message || '').toUpperCase();
          if (umsg.includes('SUSPENDED')) {
            tagLabel = 'PROCEDURA SOSPESA';
            tagClass = 'rc-tag-flag';
          } else if (umsg.includes('FORMATION')) {
            tagLabel = 'GIRO FORMAZIONE';
            tagClass = 'rc-tag-vsc';
          } else if (umsg.includes('DELAYED')) {
            tagLabel = 'PARTENZA RITARDATA';
            tagClass = 'rc-tag-flag';
          } else if (umsg.includes('ORIGINAL GRID')) {
            tagLabel = 'SCHIERAMENTO ORIGINARIO';
            tagClass = 'rc-tag-other';
          }
        }
        html += '<div class="rc-item" onclick="jumpToRace(' + Math.max(0, m.timeSec) + ')" title="Clicca per saltare a questo secondo">'
          + '<span class="rc-item-time">' + timeStr + '</span>'
          + '<span class="rc-item-lap">' + lapStr + '</span>'
          + '<span class="rc-badge-tag ' + tagClass + '">' + tagLabel + '</span>'
          + '<span class="rc-item-text">' + m.message + '</span>'
          + '</div>';
      });
      feedBox.innerHTML = html || '<div style="font-size:10px; color:#8b949e; padding:4px;">Nessuna comunicazione prima del via.</div>';
    }

    function updateTimingTower(activeKey) {
      const tbody = document.getElementById('timingTableBody');
      const driverKeys = Object.keys(DRIVERS);
      const sessionBests = getSessionBests(currentSecond);

      // Sort drivers by true dynamic race progression
      const ranked = driverKeys.map(k => {
        const d = DRIVERS[k];
        const prog = getDriverProgress(k, currentSecond);
        const lapProgress = ((prog % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
        const pt = getTrackPointAtTime(lapProgress);
        const isRet = isDriverRetired(k, currentSecond);
        const inPit = isDriverInPit(k, currentSecond);
        const tire = getDriverTire(k, currentSecond);
        const gridIndex = GRID_ORDER.indexOf(k) + 1;
        const stats = getDriverTimingStats(k, currentSecond, sessionBests);

        return {
          key: k,
          driver: d,
          totalProgress: prog,
          lapProgress: lapProgress,
          inPit: inPit,
          isRetired: isRet,
          retiredLap: RETIREMENTS[k]?.lap,
          tire: tire,
          gridPos: gridIndex,
          stats: stats
        };
      }).sort((a, b) => b.totalProgress - a.totalProgress);

      const leaderProgress = ranked[0].totalProgress;

      var html = '';
      ranked.forEach(function(r, idx) {
        var d = r.driver;
        var isCurrentActive = r.key === activeKey;
        var gap = r.isRetired 
          ? '<span style="color:#da3633; font-weight:800;">DNF (L' + r.retiredLap + ')</span>' 
          : (idx === 0 ? 'LEADER' : '+' + (leaderProgress - r.totalProgress).toFixed(1) + 's');
        var currentPos = idx + 1;
        var posDelta = r.gridPos - currentPos;

        var deltaHtml = '<span style="color: #8b949e;">-</span>';
        if (!r.isRetired && posDelta > 0) {
          deltaHtml = '<span style="color: #2ea043; font-weight: 800;">▲' + posDelta + '</span>';
        } else if (!r.isRetired && posDelta < 0) {
          deltaHtml = '<span style="color: #da3633; font-weight: 800;">▼' + Math.abs(posDelta) + '</span>';
        }

        var tireColor = r.tire === 'I' ? '#3fb950' : (r.tire === 'S' ? '#da3633' : (r.tire === 'M' ? '#e3b341' : (r.tire === 'W' ? '#58a6ff' : '#f0f6fc')));

        var s1Badge = renderBadge(r.stats.s1Str, r.stats.s1Badge);
        var s2Badge = renderBadge(r.stats.s2Str, r.stats.s2Badge);
        var s3Badge = renderBadge(r.stats.s3Str, r.stats.s3Badge);
        var lastBadge = renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge);
        var bestBadge = renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge);

        html += '<tr class="timing-row ' + (isCurrentActive ? 'active' : '') + '" data-driver="' + r.key + '">'
          + '<td class="pos-cell">' + currentPos + '</td>'
          + '<td class="delta-cell">' + deltaHtml + '</td>'
          + '<td style="text-align: left;"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span>#' + d.number + ' ' + d.code + '</span></span></td>'
          + '<td>' + s1Badge + '</td>'
          + '<td>' + s2Badge + '</td>'
          + '<td>' + s3Badge + '</td>'
          + '<td>' + lastBadge + '</td>'
          + '<td>' + bestBadge + '</td>'
          + '<td style="text-align: right; color: ' + (idx === 0 ? '#e3b341' : '#8b949e') + '; font-family: monospace;">' + gap + '</td>'
          + '<td style="text-align: center;"><span style="color:' + tireColor + '; font-weight:800; font-size:10px;">[' + r.tire + ']</span></td>'
          + '</tr>';
      });
      tbody.innerHTML = html;
    }

    document.getElementById('timingTableBody').addEventListener('click', function(e) {
      var row = e.target.closest('tr');
      if (row && row.dataset.driver) {
        selectDriverFromTable(row.dataset.driver);
      }
    });

    // Mouse hover detection over cars on track canvas
    let hoveredDriver = null;
    canvas.addEventListener('mousemove', (e) => {
      const rect = canvas.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      const my = e.clientY - rect.top;

      let found = null;
      for (const [key, item] of Object.entries(carScreenPositions)) {
        const dist = Math.hypot(mx - item.x, my - item.y);
        if (dist <= 14) {
          found = key;
          break;
        }
      }
      hoveredDriver = found;
      canvas.style.cursor = found ? 'pointer' : 'crosshair';
    });

    canvas.addEventListener('click', () => {
      if (hoveredDriver) {
        selectDriverFromTable(hoveredDriver);
      }
    });

    let lastTime = Date.now();
    function tick() {
      try {
        const now = Date.now();
        const dt = Math.min(0.2, (now - lastTime) / 1000);
        lastTime = now;

        if (isPlaying) {
          currentSecond += dt * speedMult;
          if (currentSecond >= SESSION_DURATION) {
            currentSecond = SESSION_DURATION;
            isPlaying = false;
            const playBtn = document.getElementById('playPauseBtn');
            playBtn.textContent = 'GARA CONCLUSA';
            playBtn.style.background = '#30363d';
            const ticker = document.getElementById('overtakeTicker');
            ticker.textContent = '🏁 SESSION FINISHED — 55/55 Giri completati. Vincitore: Max Verstappen (Red Bull Racing)! Podio: 1° Verstappen, 2° Antonelli (+2.3s), 3° Hamilton (+4.9s). Flusso telemetria concluso.';
            ticker.style.color = '#3fb950';
          }
        }

        drawTrack();
        updateHUD();
      } catch (err) {
        console.error('Tick error:', err);
      }
      requestAnimationFrame(tick);
    }
    requestAnimationFrame(tick);

    // Controls
    const playBtn = document.getElementById('playPauseBtn');
    playBtn.onclick = () => {
      if (currentSecond >= SESSION_DURATION) {
        currentSecond = 0;
      }
      isPlaying = !isPlaying;
      playBtn.textContent = isPlaying ? 'PAUSA' : 'RIPRENDI';
      playBtn.style.background = isPlaying ? '#238636' : '#8957e5';
    };

    function setSimulationSpeed(spd) {
      speedMult = spd;
      document.getElementById('speedRange').value = spd;
      document.getElementById('speedLabel').textContent = 'Velocità: ' + spd + 'x' + (spd === 1 ? ' (Tempo Reale)' : ' (Test)');
    }
    window.setSimulationSpeed = setSimulationSpeed;

    const speedRange = document.getElementById('speedRange');
    speedRange.oninput = (e) => {
      speedMult = parseInt(e.target.value, 10);
      document.getElementById('speedLabel').textContent = 'Velocità: ' + speedMult + 'x' + (speedMult === 1 ? ' (Tempo Reale)' : ' (Test)');
    };

    const timeScrubber = document.getElementById('timeScrubber');
    timeScrubber.oninput = (e) => {
      currentSecond = parseInt(e.target.value, 10);
      updateHUD();
      drawTrack();
    };

    const driverSelect = document.getElementById('driverSelect');
    driverSelect.onchange = (e) => {
      const val = e.target.value;
      selectedDriver = val;
      if (val !== 'all') {
        focusedDriver = val;
      }
      updateHUD();
      drawTrack();
    };

    function jumpToRace(sec) {
      currentSecond = sec;
      updateHUD();
      drawTrack();
    }
    window.jumpToRace = jumpToRace;

    function jumpToCorner(sec) {
      const activeKey = (selectedDriver === 'all') ? focusedDriver : selectedDriver;
      const prog = getDriverProgress(activeKey, currentSecond);
      const lapStart = Math.floor(prog / LAP_DURATION) * LAP_DURATION;
      const targetProg = lapStart + sec;
      currentSecond = targetProg + getDriverGap(activeKey, currentSecond);
      updateHUD();
      drawTrack();
    }
    window.jumpToCorner = jumpToCorner;
  </script>
</body>
</html>`;

const finalHtml = templateHtml
  .replace('__NODES_JSON__', JSON.stringify(compactNodes))
  .replace('__DRIVERS_JSON__', driversJson)
  .replace('__KEYFRAMES_JSON__', driverKeyframesJson)
  .replace('__PIT_STOPS_JSON__', pitStopsJson)
  .replace('__RACE_EVENTS_JSON__', raceEventsJson)
  .replace('__RACE_CONTROL_MESSAGES_JSON__', raceControlJson)
  .replace('__RETIREMENTS_JSON__', retirementsJson)
  .replace('__LAP_STARTS_JSON__', lapStartsJson)
  .replace('__DRIVER_STINTS_JSON__', driverStintsJson)
  .replace('__DRIVER_LAPS_JSON__', driverLapsJson)
  .replace('__DRIVER_OPTIONS_HTML__', driverOptionsHtml);

fs.writeFileSync(path.join(__dirname, 'track_map.html'), finalHtml, 'utf-8');
console.log('Successfully wrote track_map.html with official Race Director 08:33:00 - 10:20:15 UTC times, strictly 55 laps, and authentic OpenF1 & Jolpica gaps! Size:', finalHtml.length);

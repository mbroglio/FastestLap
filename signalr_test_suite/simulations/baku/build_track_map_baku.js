const fs = require('fs');
const path = require('path');

const nodes = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'data/baku_exact_track_full.json'), 'utf-8')
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

const drivers = JSON.parse(fs.readFileSync(path.join(__dirname, 'data/baku_drivers.json'), 'utf-8'));
const driversJson = JSON.stringify(drivers, null, 2);
const driverKeyframesJson = fs.readFileSync(path.join(__dirname, 'data/baku_keyframes.json'), 'utf-8');
const pitStopsJson = fs.readFileSync(path.join(__dirname, 'data/baku_pit_stops.json'), 'utf-8');
const pitLaneNodesJson = fs.readFileSync(path.join(__dirname, 'data/baku_pit_lane_nodes.json'), 'utf-8');
const raceControlJson = fs.readFileSync(path.join(__dirname, 'data/baku_race_control_messages.json'), 'utf-8');
const retirementsJson = fs.readFileSync(path.join(__dirname, 'data/baku_retirements.json'), 'utf-8');
const driverStintsJson = fs.readFileSync(path.join(__dirname, 'data/baku_driver_stints.json'), 'utf-8');
const driverLapsJson = fs.readFileSync(path.join(__dirname, 'data/baku_driver_laps.json'), 'utf-8');
const raceEventsJson = fs.readFileSync(path.join(__dirname, 'data/baku_race_events.json'), 'utf-8');
const incidentsJson = fs.readFileSync(path.join(__dirname, 'data/baku_incidents.json'), 'utf-8');

// Build driver options HTML
const driverOptionsHtml = Object.keys(drivers)
  .map(k => {
    const d = drivers[k];
    return `<option value="${k}">#${d.number} ${d.code} — ${d.firstName} ${d.lastName} (${d.team})</option>`;
  })
  .join('\n');

const templateHtml = `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Baku City Circuit Live Timing — GP Azerbaigian 2026 (FastestLap)</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background: #0d1117;
      color: #c9d1d9;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      height: 100vh;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    /* Top Circuit Switcher & Header */
    header {
      background: #161b22;
      border-bottom: 1px solid #30363d;
      padding: 8px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-shrink: 0;
    }
    .title-box { display: flex; align-items: center; gap: 10px; }
    .title-box h1 { font-size: 14px; font-weight: 700; color: #f0f6fc; }
    .badge {
      background: #0093cc;
      color: #fff;
      font-size: 10px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }
    .circuit-switch-bar {
      display: flex;
      gap: 6px;
      align-items: center;
    }
    .switch-btn {
      background: #21262d;
      color: #c9d1d9;
      border: 1px solid #30363d;
      padding: 4px 8px;
      border-radius: 4px;
      font-size: 10.5px;
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
      font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
      font-size: 11px;
      color: #58a6ff;
    }

    .main-container {
      flex: 1;
      display: flex;
      overflow: hidden;
    }

    /* Left: Map Pane */
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

    /* Right: HUD Pane */
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

    /* Live Ticker */
    .event-card {
      background: #101923;
      border: 1px solid #388bfd;
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

    /* Race Control */
    .rc-card { border-left: 3px solid #ff8000; }
    .rc-status-row { display: flex; gap: 6px; margin-bottom: 6px; align-items: center; }
    .rc-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 800;
    }
    .rc-flag-green { background: #238636; color: #fff; }
    .rc-flag-yellow { background: #d29922; color: #000; }
    .rc-flag-vsc { background: #e3b341; color: #000; }
    .rc-flag-chequered { background: #fff; color: #000; }
    .rc-drs-on { background: #238636; color: #fff; }
    .rc-drs-off { background: #21262d; color: #8b949e; }
    .rc-feed-box {
      max-height: 100px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 4px;
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
      cursor: pointer;
    }
    .rc-item:hover { background: #161b22; }
    .rc-item-time { color: #8b949e; font-family: monospace; font-size: 9px; white-space: nowrap; }
    .rc-item-lap { color: #58a6ff; font-weight: 700; white-space: nowrap; }
    .rc-badge-tag {
      font-size: 8px;
      padding: 1px 4px;
      border-radius: 3px;
      font-weight: 800;
      white-space: nowrap;
    }
    .rc-tag-flag { background: #d29922; color: #000; }
    .rc-tag-drs { background: #238636; color: #fff; }
    .rc-tag-vsc { background: #f0883e; color: #000; }
    .rc-tag-other { background: #388bfd; color: #fff; }
    .rc-item-text { color: #c9d1d9; flex: 1; }

    /* Driver Selector */
    select {
      width: 100%;
      background: #161b22;
      color: #e6edf3;
      border: 1px solid #30363d;
      padding: 6px 10px;
      border-radius: 6px;
      font-size: 12px;
      cursor: pointer;
    }
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
    .driver-color-bar { width: 5px; height: 34px; border-radius: 2px; background: #27F4D2; }
    .driver-num-badge { font-size: 18px; font-weight: 900; color: #f0f6fc; width: 32px; text-align: center; }
    .driver-meta { flex: 1; }
    .driver-fullname { font-size: 13px; font-weight: 700; color: #f0f6fc; }
    .driver-teamname { font-size: 11px; color: #8b949e; margin-top: 1px; }
    .tire-badge {
      font-size: 10px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 4px;
    }
    .tire-S { background: #da3633; color: white; }
    .tire-M { background: #e3b341; color: black; }
    .tire-H { background: #f0f6fc; color: black; }

    /* Cockpit Gauges */
    .cockpit-grid {
      display: grid;
      grid-template-columns: 80px 1fr;
      gap: 10px;
      align-items: center;
      margin-top: 4px;
    }
    .gear-box {
      height: 64px;
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 38px;
      font-weight: 900;
      color: #58a6ff;
      font-family: monospace;
    }
    .speed-box { display: flex; flex-direction: column; justify-content: center; }
    .speed-value { font-size: 32px; font-weight: 900; color: #f0f6fc; line-height: 1; }
    .speed-unit { font-size: 11px; color: #8b949e; margin-top: 2px; }
    .rpm-text { font-size: 12px; font-weight: 600; color: #79c0ff; margin-top: 2px; }
    .led-bar { display: flex; gap: 3px; margin-top: 6px; }
    .led { flex: 1; height: 6px; border-radius: 2px; background: #21262d; }
    .led.green.on { background: #238636; box-shadow: 0 0 6px #2ea043; }
    .led.yellow.on { background: #d29922; box-shadow: 0 0 6px #e3b341; }
    .led.red.on { background: #da3633; box-shadow: 0 0 8px #f85149; }
    .pedal-bar-wrap { margin-top: 6px; }
    .pedal-label { display: flex; justify-content: space-between; font-size: 10px; color: #8b949e; margin-bottom: 2px; }
    .progress-track { background: #21262d; height: 6px; border-radius: 3px; overflow: hidden; }
    .progress-fill { height: 100%; border-radius: 3px; background: #2ea043; transition: width 0.05s ease-out; }
    .tags-row { display: flex; gap: 8px; margin-top: 6px; }
    .tag { flex: 1; text-align: center; padding: 4px; border-radius: 4px; font-size: 10px; font-weight: 800; background: #21262d; color: #484f58; }
    .tag.brake-on { background: #da3633; color: white; box-shadow: 0 0 8px rgba(218,54,51,0.6); }
    .tag.drs-on { background: #238636; color: white; box-shadow: 0 0 8px rgba(35,134,54,0.6); }
    .tag.pit-on { background: #d29922; color: black; box-shadow: 0 0 8px rgba(210,153,34,0.8); }

    /* Location Card */
    .loc-name { font-weight: 700; color: #f0f6fc; font-size: 12px; }
    .sector-badge { display: inline-block; padding: 2px 7px; border-radius: 4px; font-size: 10px; font-weight: 800; margin-top: 3px; }
    .sec-1 { background: #e3b341; color: #000; }
    .sec-2 { background: #da3633; color: #fff; }
    .sec-3 { background: #388bfd; color: #fff; }

    /* Cockpit Lap & Sector Card */
    .timing-hud-card { border-left: 3px solid #a855f7; }
    .hud-sectors-row { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 6px; margin-top: 4px; }
    .hud-sec-box {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 6px;
      text-align: center;
      transition: all 0.2s;
    }
    .hud-sec-box.active-sec {
      border-color: #58a6ff;
      box-shadow: 0 0 8px rgba(88, 166, 255, 0.45);
    }
    .hud-sec-lbl { font-size: 9px; font-weight: 800; color: #8b949e; }
    .hud-sec-val { font-size: 14px; font-weight: 800; color: #f0f6fc; font-family: monospace; margin: 2px 0; }
    .hud-sec-tag { font-size: 8.5px; font-weight: 700; border-radius: 3px; padding: 1px 4px; display: inline-block; }
    .hud-laps-row { display: grid; grid-template-columns: 1fr 1fr; gap: 6px; margin-top: 6px; }
    .hud-lap-box { background: #161b22; border: 1px solid #30363d; border-radius: 6px; padding: 6px; text-align: center; }
    .hud-lap-lbl { font-size: 9px; font-weight: 800; color: #8b949e; }
    .hud-lap-val { font-size: 15px; font-weight: 900; color: #f0f6fc; font-family: monospace; margin: 2px 0; }
    .hud-lap-status { font-size: 8.5px; font-weight: 700; border-radius: 3px; padding: 1px 4px; display: inline-block; }

    /* Live Timing Tower */
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
      font-size: 10px;
      font-family: monospace;
    }
    .timing-table th {
      background: #161b22;
      color: #8b949e;
      font-weight: 700;
      padding: 4px 2px;
      text-align: center;
      position: sticky;
      top: 0;
      font-size: 8px;
      border-bottom: 1px solid #30363d;
    }
    .timing-table td {
      padding: 2.5px 2px;
      border-bottom: 1px solid #21262d;
      white-space: nowrap;
      text-align: center;
      font-size: 9.5px;
    }
    .timing-row { cursor: pointer; transition: background 0.15s; }
    .timing-row:hover { background: #21262d; }
    .timing-row.active { background: rgba(88, 166, 255, 0.15); border-left: 3px solid #58a6ff; }
    .pos-cell { font-weight: 800; color: #f0f6fc; width: 18px; text-align: center; }
    .delta-cell { width: 18px; text-align: center; font-size: 9px; font-weight: 700; }
    .driver-pill { display: flex; align-items: center; gap: 4px; font-weight: 700; font-family: sans-serif; text-align: left; }
    .color-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; flex-shrink: 0; }

    /* F1 Badges */
    .f1-badge { display: inline-block; padding: 1px 4px; border-radius: 3px; font-size: 9px; font-weight: 700; }
    .badge-purple { background: rgba(176, 85, 255, 0.22); color: #d2a8ff; border: 1px solid rgba(176, 85, 255, 0.55); font-weight: 800; }
    .badge-green { background: rgba(46, 160, 67, 0.22); color: #3fb950; border: 1px solid rgba(46, 160, 67, 0.55); font-weight: 700; }
    .badge-yellow { background: rgba(210, 153, 34, 0.18); color: #e3b341; border: 1px solid rgba(210, 153, 34, 0.45); font-weight: 600; }
    .badge-dim { color: #484f58; font-weight: 500; }

    /* Controls */
    .controls-card { padding: 8px 12px; }
    .ctrl-row { display: flex; align-items: center; gap: 8px; }
    .btn-primary {
      background: #238636;
      color: white;
      border: none;
      padding: 6px 14px;
      border-radius: 6px;
      font-weight: 700;
      font-size: 12px;
      cursor: pointer;
    }
    .btn-primary:hover { background: #2ea043; }
    input[type=range] { flex: 1; accent-color: #58a6ff; cursor: pointer; }
    .shortcuts-bar { display: flex; gap: 4px; flex-wrap: wrap; margin-top: 6px; }
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
      .clock-display {
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
      <h1>Baku City Circuit (26 Settembre 2026) — GP Azerbaigian 51 Giri</h1>
      <div class="circuit-switch-bar">
        <a href="track_map.html" class="switch-btn">🇲🇾 Sepang GP (Bagnato)</a>
        <a href="track_map_baku.html" class="switch-btn active">🇦🇿 Baku GP (Asciutto)</a>
      </div>
    </div>
    <div class="clock-display" id="headerClock">
      26 Settembre 2026 | 11:03:51 UTC | Giro: 1/51
    </div>
  </header>

  <div class="main-container">
    <div class="map-pane">
      <canvas id="trackCanvas"></canvas>
    </div>

    <div class="hud-pane">
      <!-- Live Overtake & Events Ticker -->
      <div class="card event-card">
        <div class="card-title">
          <span>⚡ Feed Sorpassi & Eventi Baku</span>
          <span style="font-size: 10px; color: #58a6ff;" id="eventCountBadge">Live Feed</span>
        </div>
        <div class="event-ticker" id="overtakeTicker">
          🏁 11:03:51 — Semafori spenti! Le 22 monoposto scattano dalla griglia di Baku per il GP dell'Azerbaigian!
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
          <span style="font-size: 10px; color: #8b949e; margin-left: auto;" id="rcTotalCount">141 Messaggi</span>
        </div>
        <div class="rc-feed-box" id="rcFeedBox"></div>
      </div>

      <!-- Driver Selector Card -->
      <div class="card">
        <div class="card-title">
          <span>Selezione Pilota & Focus</span>
          <span style="font-size: 10px; color: #58a6ff;" id="driverModeLabel">Vista Globale: 22 Piloti</span>
        </div>
        <select id="driverSelect">
          <option value="all">🏁 Vista Globale — Tutti i 22 Piloti in Pista</option>
          <optgroup label="Griglia Ufficiale F1 2026 (22 Piloti — Baku)">
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

      <!-- Cockpit Telemetry HUD -->
      <div class="card">
        <div class="card-title">
          <span>Cockpit Telemetria Real-Time</span>
          <span id="syncIndicator" style="color: #2ea043; font-size: 10px;">● 1:1 Sincronizzato</span>
        </div>
        <div class="cockpit-grid">
          <div class="gear-box" id="gearVal">8</div>
          <div class="speed-box">
            <div><span class="speed-value" id="speedVal">335</span> <span class="speed-unit">KM/H</span></div>
            <div class="rpm-text" id="rpmVal">12,400 RPM</div>
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
          <div class="tag" id="brakeTag">FRENO (OFF)</div>
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
      <div class="card">
        <div class="card-title">Posizione Pista & Coordinate GPS Decimetriche</div>
        <div class="loc-name" id="locName">📍 Rettifilo Arrivo (DRS Zona 1 — 335 km/h)</div>
        <div id="sectorBadge" class="sector-badge sec-1">SETTORE 1</div>
        <div class="clock-display" style="font-size: 10px; margin-top: 2px;" id="gpsCoords">GPS: X = +92.4m, Y = -65.0m</div>
      </div>

      <!-- Tempi sul Giro & Settori Pilota -->
      <div class="card timing-hud-card">
        <div class="card-title">
          <span>Tempi sul Giro & Settori (Live Timing)</span>
          <span id="hudLapNumBadge" style="font-size: 10px; color: #58a6ff; font-weight: 800;">GIRO 1 / 51</span>
        </div>
        
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
          <span>RECORD GARA: <strong id="hudSessionFastestVal" style="color: #d2a8ff;">1:44.916</strong> (<span id="hudSessionFastestDrv">#63 RUS</span>)</span>
          <span style="display: flex; gap: 6px;">
            <span style="color: #d2a8ff; font-weight: 700;">● Viola: Assoluto</span>
            <span style="color: #3fb950; font-weight: 700;">● Verde: Personale</span>
            <span style="color: #e3b341; font-weight: 700;">● Giallo: No Migl.</span>
          </span>
        </div>
      </div>

      <!-- Live Timing Tower -->
      <div class="card">
        <div class="card-title">
          <span>Classifica Live in Pista (22 Piloti)</span>
          <span style="font-size: 10px; color: #8b949e;">Click riga per focus</span>
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
            <tbody id="timingTableBody"></tbody>
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
            <button class="jump-btn" onclick="setSimulationSpeed(1)">1x (Reale)</button>
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
          <input type="range" id="timeScrubber" min="0" max="6000" value="0">
        </div>

        <!-- Shortcuts Bar -->
        <div class="shortcuts-bar">
          <span style="font-size: 10px; color: #e3b341; width: 100%; margin-top: 4px; font-weight: 700;">Momenti Chiave GP Baku:</span>
          <button class="jump-btn race-event" onclick="jumpToRace(0)">⚡ 11:03:51 Standing Start: Partenza da Fermo!</button>
          <button class="jump-btn race-event" onclick="jumpToRace(250)">🏎️ 11:08:01 G3: Verstappen supera Norris (DRS 338 km/h)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(890, '18')">⚠️ 11:18:41 G8: Ritiro Stroll (Sospensione a C4)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(1200)">🏎️ 11:23:51 G11: Piastri supera Leclerc (P2)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(1650)">🏎️ 11:31:21 G15: Verstappen supera Leclerc (P3)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(1850, '44')">⚠️ 11:34:41 G17: Hamilton lungo a Curva 1 (Via di Fuga & Rientro)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(2226)">🔧 11:40:57 G20: Pit stop Sainz (Hard)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(2520, '55')">⚠️ 11:45:51 G23: Sainz lungo a Curva 3 (Via di Fuga & Rientro)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(2700)">🏎️ 11:48:51 G25: Verstappen supera Piastri per P2</button>
          <button class="jump-btn race-event" onclick="jumpToRace(3280, '23')">🟡 11:58:31 G30: Safety Car 1 (Albon Barriera Curva 15)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(4044)">🟢 12:11:14 G35: Restart Gara 1</button>
          <button class="jump-btn race-event" onclick="jumpToRace(4120, '1')">💥 12:12:31 G36: Collisione Norris-Gasly Curva 1 (Via di Fuga)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(4130)">🟡 12:12:41 G36: Safety Car 2</button>
          <button class="jump-btn race-event" onclick="jumpToRace(4396)">🟢 12:17:06 G38: Restart Finale (13 Giri)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5350)">👑 12:33:00 G47: Verstappen all'inseguimento di Russell (-0.5s)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5600)">🟣 12:37:10 G49: Giro Veloce Russell (1:44.916)</button>
          <button class="jump-btn race-event" onclick="jumpToRace(5881)">🏆 12:41:52 G51: Verstappen Vince in Volata su Russell (+0.003s)!</button>
        </div>

        <div class="shortcuts-bar" style="margin-top: 6px;">
          <span style="font-size: 10px; color: #8b949e; width: 100%;">Salta a Sezione Pista (Pilota Attivo):</span>
          <button class="jump-btn" onclick="jumpToCorner(0.0)">Rettilineo Arrivo (335 km/h DRS)</button>
          <button class="jump-btn" onclick="jumpToCorner(6.0)">Staccata Curva 1 (105 km/h)</button>
          <button class="jump-btn" onclick="jumpToCorner(13.0)">Curva 2 (Sinistra 90°)</button>
          <button class="jump-btn" onclick="jumpToCorner(21.0)">Khagani Street (DRS 315 km/h)</button>
          <button class="jump-btn" onclick="jumpToCorner(29.0)">Staccata Curva 3</button>
          <button class="jump-btn" onclick="jumpToCorner(42.0)">Curve 5-6 (Chicane)</button>
          <button class="jump-btn" onclick="jumpToCorner(58.0)">Sezione Castello (Curve 8-12)</button>
          <button class="jump-btn" onclick="jumpToCorner(78.0)">Staccata Curva 15 Discesa</button>
          <button class="jump-btn" onclick="jumpToCorner(85.0)">Curva 16 Rampa d'Uscita</button>
          <button class="jump-btn" onclick="jumpToCorner(100.0)">Maxi-Rettilineo Neftchilar (345 km/h)</button>
        </div>
      </div>
    </div>
  </div>

  <script>
    // 450 Exact Track Nodes for Baku City Circuit
    // [t, px, py, x, y, sector, speed, gear, rpm, throttle, brake, drs, location]
    const NODES = __NODES_JSON__;
    const DRIVERS = __DRIVERS_JSON__;
    const DRIVER_KEYFRAMES = __KEYFRAMES_JSON__;
    const PIT_STOPS = __PIT_STOPS_JSON__;
    const PIT_LANE_NODES = __PIT_LANE_NODES_JSON__;
    const RACE_EVENTS = __RACE_EVENTS_JSON__;
    const RACE_CONTROL_MESSAGES = __RACE_CONTROL_MESSAGES_JSON__;
    const RETIREMENTS = __RETIREMENTS_JSON__;
    const INCIDENTS = __INCIDENTS_JSON__;
    const DRIVER_STINTS = __DRIVER_STINTS_JSON__;
    const DRIVER_LAPS = __DRIVER_LAPS_JSON__;

    const SVG_WIDTH = 500;
    const SVG_HEIGHT = 371;
    const LAP_DURATION = 108.6;
    const TOTAL_LAPS = 51;
    const RACE_FINISH_SEC = 5881.3;
    const SESSION_DURATION = 6000;
    const PIT_EXIT_TRACK_SEC = 6.76;   // Exact pit exit merge node before Turn 1 (px: 478.7, py: 71.4)
    const PIT_ENTRY_TRACK_SEC = 102.81; // Exact pit entry branch node after Turn 20 (px: 349.0, py: 141.5)

    // Official Starting Grid Order for Baku 2026
    const GRID_ORDER = ['63', '16', '81', '6', '1', '3', '44', '10', '55', '43', '87', '30', '23', '31', '41', '12', '5', '27', '14', '11', '77', '18'];

    // Pit lane total distance computation
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

    function getDriverLapRecord(drvKey, t) {
      const dl = DRIVER_LAPS[drvKey];
      if (!dl || dl.length === 0) return null;
      for (let i = 0; i < dl.length; i++) {
        const l = dl[i];
        const nextStart = dl[i + 1] ? dl[i + 1].startSec : (l.startSec + (l.dur || LAP_DURATION));
        if (t >= l.startSec && t < nextStart) {
          return l;
        }
      }
      if (t < dl[0].startSec) return dl[0];
      return dl[dl.length - 1];
    }

    function getDriverGap(drvKey, t) {
      const kfs = DRIVER_KEYFRAMES[drvKey];
      if (!kfs || kfs.length === 0) return 0;
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
      if (t < 0) {
        const gridIdx = GRID_ORDER.indexOf(drvKey);
        const pos = gridIdx >= 0 ? gridIdx : 21;
        return -(pos * 0.15);
      }
      if (t >= RACE_FINISH_SEC) {
        const finishProg = RACE_FINISH_SEC - getDriverGap(drvKey, RACE_FINISH_SEC);
        const inLapElapsed = Math.min(100, t - RACE_FINISH_SEC);
        return finishProg + inLapElapsed * 0.35;
      }
      return t - getDriverGap(drvKey, t);
    }

    function getDriverLap(drvKey, t) {
      if (isDriverRetired(drvKey, t)) return RETIREMENTS[drvKey].lap;
      if (t >= RACE_FINISH_SEC) return TOTAL_LAPS;
      if (t < 0) return 1;
      const rec = getDriverLapRecord(drvKey, t);
      if (rec) return rec.lap;
      const prog = getDriverProgress(drvKey, t);
      return Math.min(TOTAL_LAPS, Math.max(1, Math.floor(prog / LAP_DURATION) + 1));
    }

    function getDriverTrackSec(drvKey, t) {
      if (isDriverRetired(drvKey, t)) {
        const inc = INCIDENTS.find(i => i.driver === drvKey);
        return inc ? inc.trackSec : 78.0;
      }

      // Pre-race: standing on grid slots
      if (t <= 0) return 0.0;
      if (t >= RACE_FINISH_SEC) return Math.min(LAP_DURATION, (t - RACE_FINISH_SEC) * 0.35);

      // 1. Is driver in pit lane?
      const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
      if (activeStop) {
        const frac = (t - activeStop.startSec) / Math.max(1, activeStop.endSec - activeStop.startSec);
        return frac * PIT_EXIT_TRACK_SEC;
      }

      // 2. Out-lap
      const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
      if (outStop) {
        const outEnd = outStop.outLapEndSec || (outStop.endSec + 80);
        const u = Math.min(1.0, Math.max(0.0, (t - outStop.endSec) / Math.max(1, outEnd - outStop.endSec)));
        return PIT_EXIT_TRACK_SEC + u * (LAP_DURATION - PIT_EXIT_TRACK_SEC);
      }

      // 3. Piecewise sector progression with exact 1:1 sector beam crossings
      const rec = getDriverLapRecord(drvKey, t);
      if (rec && rec.dur && rec.dur > 0) {
        // In-lap check
        const inStop = PIT_STOPS.find(p => p.driver === drvKey && p.startSec > rec.startSec && p.startSec <= rec.startSec + rec.dur && t < p.startSec && t >= rec.startSec);
        if (inStop) {
          const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(1, inStop.startSec - rec.startSec)));
          return u * PIT_ENTRY_TRACK_SEC;
        }

        const tS1 = rec.startSec + (rec.s1 || (rec.dur * 0.357));
        const tS2 = tS1 + (rec.s2 || (rec.dur * 0.409));
        const S1_TRACK = 38.85;
        const S2_TRACK = 83.5;

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

      const prog = getDriverProgress(drvKey, t);
      return ((prog % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
    }

    function getDriverTire(drvKey, t) {
      const l = getDriverLap(drvKey, t);
      const stints = DRIVER_STINTS[drvKey];
      if (!stints || stints.length === 0) return 'M';
      for (let i = 0; i < stints.length; i++) {
        const s = stints[i];
        if (l >= s.lapStart && l <= s.lapEnd) return s.code;
      }
      return stints[stints.length - 1].code;
    }

    function isDriverInPit(drvKey, t) {
      if (t >= SESSION_DURATION) return true;
      const isRet = isDriverRetired(drvKey, t);
      if (isRet) {
        const inc = INCIDENTS.find(i => i.driver === drvKey);
        return inc ? (inc.type === 'PIT_RETIRED') : false;
      }
      return PIT_STOPS.some(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
    }

    function getDriverVisualState(drvKey, t) {
      const drv = DRIVERS[drvKey];
      const gridIdx = GRID_ORDER.indexOf(drvKey);
      const isRet = isDriverRetired(drvKey, t);
      const inPit = isDriverInPit(drvKey, t);

      // Check incident (excursion, crash, failure)
      const inc = INCIDENTS.find(i => {
        if (i.driver !== drvKey) return false;
        if (t < i.startSec) return false;
        if (i.type === 'OFF_TRACK_REJOIN') {
          const dur = i.durationSec || 14.0;
          return t <= i.startSec + dur;
        }
        return true;
      });

      // 1. Off-track Incident Handling
      if (inc) {
        const dt = t - inc.startSec;
        const targetPt = { px: inc.targetPx, py: inc.targetPy };

        // Case A: Excursion with Rejoin (Hamilton T1, Sainz T3)
        if (inc.type === 'OFF_TRACK_REJOIN') {
          const dur = inc.durationSec || 14.0;
          const tEntry = inc.entryDuration || 3.0;
          const tMan = inc.maneuverDuration || (dur - 6.0);
          const tRejoin = inc.rejoinDuration || 3.0;
          const originPt = getTrackPointAtTime(inc.trackSec);

          if (dt < tEntry) {
            // Sliding into runoff / escape road
            const u = Math.min(1.0, Math.max(0.0, dt / tEntry));
            const s = u * u * (3 - 2 * u);
            const px = originPt.px * (1 - s) + targetPt.px * s;
            const py = originPt.py * (1 - s) + targetPt.py * s;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: Math.round(originPt.speed * (1 - s) + 20 * s),
              gear: 1,
              throttle: 0,
              brake: 100,
              rpm: Math.round(11000 * (1 - s) + 4000 * s),
              drs: 0,
              inPit: false,
              isOffTrack: true,
              hazard: true,
              isRetired: false,
              status: 'OFF_TRACK_SLIDE',
              location: inc.locationName + ' — Bloccaggio & Uscita'
            };
          } else if (dt < tEntry + tMan) {
            // Maneuvering in escape road
            const u = (dt - tEntry) / tMan;
            const px = targetPt.px + Math.sin(u * Math.PI) * 1.5;
            const py = targetPt.py + Math.cos(u * Math.PI) * 1.0;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: 18,
              gear: 1,
              throttle: 25,
              brake: 10,
              rpm: 4500,
              drs: 0,
              inPit: false,
              isOffTrack: true,
              hazard: true,
              isRetired: false,
              status: 'ESCAPE_ROAD_MANEUVER',
              location: inc.locationName + ' — Manovra in Via di Fuga'
            };
          } else {
            // Rejoining track onto racing line
            const u = Math.min(1.0, Math.max(0.0, (dt - tEntry - tMan) / tRejoin));
            const s = u * u * (3 - 2 * u);
            const rejoinPt = getTrackPointAtTime(inc.trackSec + 2.5);
            const px = targetPt.px * (1 - s) + rejoinPt.px * s;
            const py = targetPt.py * (1 - s) + rejoinPt.py * s;
            return {
              px: Number(px.toFixed(2)),
              py: Number(py.toFixed(2)),
              speed: Math.round(20 * (1 - s) + 140 * s),
              gear: u < 0.5 ? 2 : 3,
              throttle: 80,
              brake: 0,
              rpm: Math.round(5000 * (1 - s) + 10500 * s),
              drs: 0,
              inPit: false,
              isOffTrack: false,
              hazard: true,
              isRetired: false,
              status: 'REJOINING_TRACK',
              location: inc.locationName + ' — Rientro in Pista'
            };
          }
        }

        // Case B: Crash / Stopped Retirement
        if (inc.type === 'CRASH_RETIRED' || inc.type === 'STOPPED_RETIRED') {
          const tTrans = inc.transitionDuration || 2.5;
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
              gear: 0,
              throttle: 0,
              brake: 100,
              rpm: Math.round(9000 * (1 - s)),
              drs: 0,
              inPit: false,
              isOffTrack: true,
              hazard: true,
              isRetired: true,
              status: 'CRASH_TRANSITION',
              location: inc.locationName
            };
          } else {
            return {
              px: targetPt.px,
              py: targetPt.py,
              speed: 0,
              gear: 0,
              throttle: 0,
              brake: 0,
              rpm: 0,
              drs: 0,
              inPit: false,
              isOffTrack: true,
              hazard: false,
              isRetired: true,
              status: 'RETIRED_STOPPED',
              location: inc.locationName
            };
          }
        }

        // Case C: Pit Retirement
        if (inc.type === 'PIT_RETIRED') {
          return {
            px: targetPt.px,
            py: targetPt.py,
            speed: 0,
            gear: 0,
            throttle: 0,
            brake: 0,
            rpm: 0,
            drs: 0,
            inPit: true,
            isOffTrack: false,
            hazard: false,
            isRetired: true,
            status: 'RETIRED_PIT',
            location: inc.locationName
          };
        }
      }

      // 2. Pre-race Standing Start Grid Slots
      if (t <= 0) {
        const pIdx = gridIdx >= 0 ? gridIdx : 21;
        const slotPx = 416.5 - (pIdx * 5.0) * 0.817 + ((pIdx % 2 === 0 ? 1 : -1) * 2.8) * 0.577;
        const slotPy = 115.3 - (pIdx * 5.0) * (-0.577) + ((pIdx % 2 === 0 ? 1 : -1) * 2.8) * 0.817;
        return {
          px: Number(slotPx.toFixed(2)),
          py: Number(slotPy.toFixed(2)),
          speed: 0,
          gear: 1,
          throttle: 100,
          brake: 0,
          rpm: 10500,
          drs: 0,
          inPit: false,
          isOffTrack: false,
          hazard: false,
          isRetired: false,
          status: 'GRID_STANDING',
          location: 'Griglia di Partenza — Baku Main Straight'
        };
      }

      // 3. Launch Acceleration (0 to 6.8s)
      const drvSec = getDriverTrackSec(drvKey, t);
      const carPt = getTrackPointAtTime(drvSec);

      if (t <= 6.8) {
        const pIdx = gridIdx >= 0 ? gridIdx : 21;
        const slotPx = 416.5 - (pIdx * 5.0) * 0.817 + ((pIdx % 2 === 0 ? 1 : -1) * 2.8) * 0.577;
        const slotPy = 115.3 - (pIdx * 5.0) * (-0.577) + ((pIdx % 2 === 0 ? 1 : -1) * 2.8) * 0.817;
        const u = Math.min(1.0, t / 6.8);
        const s = u * u * (3 - 2 * u);
        const px = slotPx * (1 - s) + carPt.px * s;
        const py = slotPy * (1 - s) + carPt.py * s;
        const speed = Math.round(u * 255);
        let gear, rpm;
        if (speed < 90) { gear = 1; rpm = 7000 + Math.round(speed * 45); }
        else if (speed < 145) { gear = 2; rpm = 8000 + Math.round((speed - 90) * 55); }
        else if (speed < 195) { gear = 3; rpm = 8500 + Math.round((speed - 145) * 50); }
        else if (speed < 235) { gear = 4; rpm = 9000 + Math.round((speed - 195) * 55); }
        else { gear = 5; rpm = 9500 + Math.round((speed - 235) * 50); }
        return {
          px: Number(px.toFixed(2)),
          py: Number(py.toFixed(2)),
          speed, gear, rpm,
          throttle: 100,
          brake: 0,
          drs: 0,
          inPit: false,
          isOffTrack: false,
          hazard: false,
          isRetired: false,
          status: 'LAUNCH',
          location: 'Partenza da Fermo — Scatto al Via verso Curva 1'
        };
      }

      // 4. Pit Lane Navigation
      if (inPit) {
        const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
        let pitPx = 416.5, pitPy = 109.1;
        let inBox = false;
        if (activeStop) {
          const frac = Math.max(0, Math.min(1, (t - activeStop.startSec) / Math.max(1, (activeStop.endSec - activeStop.startSec))));
          const teamBoxDist = Math.max(20, Math.min(PIT_LANE_TOTAL_DIST - 20, (gridIdx / 21.0) * (PIT_LANE_TOTAL_DIST - 40) + 20));
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
          rpm: inBox ? 3200 : 4500,
          throttle: inBox ? 0 : 35,
          brake: 0,
          drs: 0,
          inPit: true,
          isOffTrack: false,
          hazard: false,
          isRetired: false,
          status: inBox ? 'PIT_BOX_STOP' : 'PIT_LANE_DRIVE',
          location: inBox ? 'Corsia Box — Piazzola Sostituzione Gomme' : 'Corsia Box — Limitatore di Velocità Attivo (80 km/h)'
        };
      }

      // 5. Pit Exit Hermite Blend onto Track Centerline (2.5s)
      const recentExit = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= p.endSec + 2.5);
      if (recentExit) {
        const u = Math.min(1.0, Math.max(0.0, (t - recentExit.endSec) / 2.5));
        const s = u * u * (3 - 2 * u);
        const exitNode = PIT_LANE_NODES[PIT_LANE_NODES.length - 1];
        const px = exitNode.px * (1 - s) + carPt.px * s;
        const py = exitNode.py * (1 - s) + carPt.py * s;
        return {
          px: Number(px.toFixed(2)),
          py: Number(py.toFixed(2)),
          speed: Math.round(80 * (1 - s) + carPt.speed * s),
          gear: carPt.gear,
          rpm: carPt.rpm,
          throttle: carPt.throttle,
          brake: carPt.brake,
          drs: 0,
          inPit: false,
          isOffTrack: false,
          hazard: false,
          isRetired: false,
          status: 'PIT_EXIT_MERGE',
          location: 'Rientro in Pista da Corsia Box'
        };
      }

      // 6. Normal On-Track Position
      const isSC = isSafetyCarActive(t);
      const isFinished = t >= RACE_FINISH_SEC;
      let speed, rpm, throttle, gear, drs, loc;

      if (isFinished) {
        speed = 110; rpm = 6800; throttle = 30; gear = 4; drs = 0;
        loc = 'Giro d\'Onore — Bandiera a Scacchi';
      } else if (isSC) {
        speed = Math.min(160, Math.round(carPt.speed * 0.65));
        rpm = Math.min(9500, Math.round(carPt.rpm * 0.75));
        throttle = carPt.brake === 1 ? 0 : Math.min(50, carPt.throttle);
        gear = Math.min(6, carPt.gear);
        drs = 0;
        loc = carPt.location;
      } else {
        speed = Math.round(carPt.speed);
        rpm = carPt.rpm;
        throttle = carPt.brake === 1 ? 0 : carPt.throttle;
        gear = carPt.gear;
        drs = (t >= 250 && !isSC) ? carPt.drs : 0;
        loc = carPt.location;
      }

      return {
        px: carPt.px,
        py: carPt.py,
        speed: Math.round(speed),
        gear, rpm, throttle,
        brake: carPt.brake,
        drs,
        inPit: false,
        isOffTrack: false,
        hazard: false,
        isRetired: false,
        status: isSC ? 'SAFETY_CAR' : (isFinished ? 'FINISHED' : 'RACING'),
        location: loc
      };
    }

    function isSafetyCarActive(t) {
      // SC1 (Lap 31-35): 3285s to 4044s | SC2 (Lap 36-38): 4130s to 4396s
      return (t >= 3285 && t <= 4044) || (t >= 4130 && t <= 4396);
    }

    function isVSCActive(t) {
      return false; // Linear dry race, VSC not deployed (full SC used)
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

      if (t < 0) return { sessionBestLap, sessionBestLapDriver, sessionBestS1, sessionBestS2, sessionBestS3 };

      const dKeys = Object.keys(DRIVERS);
      for (let i = 0; i < dKeys.length; i++) {
        const k = dKeys[i];
        const dl = DRIVER_LAPS[k];
        if (!dl) continue;
        for (let j = 0; j < dl.length; j++) {
          const l = dl[j];
          if (l.startSec > t) break;

          if (t >= l.startSec + l.s1 && l.s1 > 10.0 && l.s1 < sessionBestS1) {
            sessionBestS1 = l.s1;
          }
          if (t >= l.startSec + l.s1 + l.s2 && l.s2 > 10.0 && l.s2 < sessionBestS2) {
            sessionBestS2 = l.s2;
          }
          if (t >= l.startSec + l.dur && l.dur > 50.0) {
            if (l.s3 > 10.0 && l.s3 < sessionBestS3) sessionBestS3 = l.s3;
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
      if (s === lastStatsSec && cachedSessionBests) return cachedSessionBests;
      lastStatsSec = s;
      cachedSessionBests = computeSessionTimingStats(t);
      return cachedSessionBests;
    }

    function getDriverTimingStats(drvKey, t, sessionBests) {
      if (t < 0 || isDriverRetired(drvKey, t)) {
        if (t < 0) {
          return {
            curLap: null, lastLap: null,
            s1Str: '-', s1Badge: 'dim', s2Str: '-', s2Badge: 'dim', s3Str: '-', s3Badge: 'dim',
            lastLapStr: '-', lastLapBadge: 'dim', bestLapStr: '-', bestLapBadge: 'dim', pBestLap: null
          };
        }
      }

      const dl = DRIVER_LAPS[drvKey] || [];
      const completedLaps = [];
      let curLap = null;

      for (let i = 0; i < dl.length; i++) {
        const l = dl[i];
        if (t >= l.startSec + l.dur) completedLaps.push(l);
        else if (t >= l.startSec && !curLap) curLap = l;
      }

      let pBestLap = Infinity, pBestS1 = Infinity, pBestS2 = Infinity, pBestS3 = Infinity;
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
      if (curLap && t >= curLap.startSec + curLap.s1) s1Val = curLap.s1;
      else if (lastLap) s1Val = lastLap.s1;
      if (s1Val != null && s1Val > 10.0) {
        if (s1Val <= sessionBests.sessionBestS1 + 0.0001) s1Badge = 'purple';
        else if (s1Val <= pBestS1 + 0.0001) s1Badge = 'green';
        else s1Badge = 'yellow';
      }

      // S2
      let s2Val = null, s2Badge = 'dim';
      if (curLap && t >= curLap.startSec + curLap.s1 + curLap.s2) s2Val = curLap.s2;
      else if (lastLap) s2Val = lastLap.s2;
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
        curLap, lastLap,
        s1Str: formatSectorTime(s1Val), s1Badge,
        s2Str: formatSectorTime(s2Val), s2Badge,
        s3Str: formatSectorTime(s3Val), s3Badge,
        lastLapStr, lastLapBadge,
        bestLapStr, bestLapBadge,
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

    const canvas = document.getElementById('trackCanvas');
    const ctx = canvas.getContext('2d');

    const bgImage = new Image();
    let bgLoaded = false;
    bgImage.onload = () => { bgLoaded = true; };
    bgImage.src = 'Baku_Formula_One_circuit_map.svg.webp';

    let currentSecond = 0;
    let isPlaying = true;
    let speedMult = 2;
    let selectedDriver = 'all';
    let focusedDriver = '3'; // Winner Verstappen #3
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

    const carScreenPositions = {};

    function drawTrack() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const bounds = getViewportBounds();

      // 1. Draw Baku Circuit Map (clean, NO background box)
      if (bgLoaded) {
        ctx.save();
        ctx.drawImage(bgImage, bounds.ox, bounds.oy, bounds.drawW, bounds.drawH);
        ctx.restore();
      } else {
        ctx.save();
        ctx.strokeStyle = '#21262d';
        ctx.lineWidth = 10;
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

      const activeKey = (selectedDriver === 'all') ? focusedDriver : selectedDriver;
      const driverKeys = Object.keys(DRIVERS);

      const driverProgressMap = {};
      driverKeys.forEach(k => { driverProgressMap[k] = getDriverProgress(k, currentSecond); });

      // 2. Draw Pit Lane Path
      ctx.save();
      ctx.strokeStyle = '#388bfd';
      ctx.lineWidth = 2.0;
      ctx.setLineDash([4, 4]);
      ctx.beginPath();
      for (let pi = 0; pi < PIT_LANE_NODES.length; pi++) {
        const pscr = pxToScreen(PIT_LANE_NODES[pi].px, PIT_LANE_NODES[pi].py);
        if (pi === 0) ctx.moveTo(pscr.x, pscr.y);
        else ctx.lineTo(pscr.x, pscr.y);
      }
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.restore();

      // 3. Draw All 22 Drivers
      driverKeys.forEach(drvKey => {
        const drv = DRIVERS[drvKey];
        if (!drv) return;

        const state = getDriverVisualState(drvKey, currentSecond);
        const carScr = pxToScreen(state.px, state.py);
        carScreenPositions[drvKey] = { x: carScr.x, y: carScr.y, drv: drv, state: state };
        const isCurrentActive = drvKey === activeKey;

        // Render retired driver with dimmed DNF badge at exact incident location
        if (state.isRetired) {
          ctx.save();
          ctx.globalAlpha = 0.55;
          ctx.beginPath();
          ctx.arc(carScr.x, carScr.y, 5.5, 0, Math.PI * 2);
          ctx.fillStyle = '#da3633';
          ctx.fill();
          ctx.lineWidth = 1.2;
          ctx.strokeStyle = '#ffffff';
          ctx.stroke();

          ctx.fillStyle = '#ffffff';
          ctx.font = 'bold 6.5px monospace';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText('OUT', carScr.x, carScr.y);

          if (selectedDriver === 'all' || isCurrentActive) {
            ctx.font = 'bold 7.5px sans-serif';
            ctx.fillStyle = '#f85149';
            ctx.fillText(drv.code + ' (DNF)', carScr.x, carScr.y - 8);
          }
          ctx.restore();
          return;
        }

        // Active Off-track Hazard Halo & Badge
        if (state.isOffTrack) {
          ctx.save();
          const pulse = 0.5 + 0.5 * Math.sin(Date.now() / 140);
          ctx.beginPath();
          ctx.arc(carScr.x, carScr.y, 11.0 + 3.5 * pulse, 0, Math.PI * 2);
          ctx.strokeStyle = 'rgba(255, 170, 0, ' + (0.4 + 0.6 * pulse) + ')';
          ctx.lineWidth = 2.5;
          ctx.stroke();

          ctx.font = 'bold 8px sans-serif';
          ctx.fillStyle = '#ffa500';
          ctx.textAlign = 'center';
          ctx.fillText('⚠️ VIA DI FUGA', carScr.x, carScr.y - 12);
          ctx.restore();
        } else if (state.status === 'REJOINING_TRACK') {
          ctx.save();
          ctx.font = 'bold 8px sans-serif';
          ctx.fillStyle = '#58a6ff';
          ctx.textAlign = 'center';
          ctx.fillText('↩️ RIENTRO', carScr.x, carScr.y - 12);
          ctx.restore();
        }

        if (selectedDriver !== 'all' && selectedDriver !== drvKey) {
          ctx.save();
          ctx.beginPath();
          ctx.arc(carScr.x, carScr.y, 3.5, 0, Math.PI * 2);
          ctx.fillStyle = state.isOffTrack ? 'rgba(255, 170, 0, 0.6)' : 'rgba(100, 100, 100, 0.35)';
          ctx.fill();
          ctx.restore();
          return;
        }

        // Draw active car dot
        ctx.save();
        const baseRadius = isCurrentActive ? 9.0 : 6.5;

        ctx.beginPath();
        ctx.arc(carScr.x, carScr.y, baseRadius + (isCurrentActive ? 4 : 2), 0, Math.PI * 2);
        ctx.fillStyle = isCurrentActive ? 'rgba(88, 166, 255, 0.45)' : (state.inPit ? 'rgba(210, 153, 34, 0.3)' : (state.isOffTrack ? 'rgba(255, 170, 0, 0.4)' : 'rgba(0, 0, 0, 0.5)'));
        ctx.fill();

        ctx.beginPath();
        ctx.arc(carScr.x, carScr.y, baseRadius, 0, Math.PI * 2);
        ctx.fillStyle = state.inPit ? '#d29922' : (state.isOffTrack ? '#ffaa00' : drv.color);
        ctx.fill();
        ctx.lineWidth = isCurrentActive ? 2.5 : 1.5;
        ctx.strokeStyle = isCurrentActive ? '#ffffff' : '#0d1117';
        ctx.stroke();

        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold ' + (isCurrentActive ? '9px' : '7.5px') + ' monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(drv.number, carScr.x, carScr.y);

        if (selectedDriver === 'all' || isCurrentActive) {
          ctx.font = 'bold 8.5px sans-serif';
          ctx.fillStyle = isCurrentActive ? '#ffffff' : '#e6edf3';
          let label = drv.code;
          if (state.inPit) label += ' [PIT]';
          else if (state.isOffTrack) label += ' [OFF]';
          else if (state.status === 'REJOINING_TRACK') label += ' [REJOIN]';
          ctx.fillText(label, carScr.x, carScr.y - baseRadius - 4);
        }
        ctx.restore();
      });

      // 4. Draw Safety Car if active
      const scActive = isSafetyCarActive(currentSecond);
      if (scActive) {
        const leaderKey = driverKeys.reduce((best, k) => (driverProgressMap[k] > (driverProgressMap[best] || -999) ? k : best), '3');
        const scSec = (getDriverTrackSec(leaderKey, currentSecond) + 3.0) % LAP_DURATION;
        const scPt = getTrackPointAtTime(scSec);
        const scScr = pxToScreen(scPt.px, scPt.py);

        ctx.save();
        ctx.beginPath();
        ctx.arc(scScr.x, scScr.y, 9.5, 0, Math.PI * 2);
        ctx.fillStyle = '#ff9800';
        ctx.fill();
        ctx.lineWidth = 2.0;
        ctx.strokeStyle = '#ffffff';
        ctx.stroke();

        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 8.5px monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('SC', scScr.x, scScr.y);
        ctx.fillText('SAFETY CAR', scScr.x, scScr.y - 13);
        ctx.restore();
      }

      // 5. Start/Finish Line badge
      const s0 = pxToScreen(NODES[0][1], NODES[0][2]);
      ctx.save();
      ctx.fillStyle = '#238636';
      ctx.beginPath();
      ctx.arc(s0.x, s0.y, 4.5, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#2ea043';
      ctx.font = 'bold 8.5px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('FINISH', s0.x, s0.y + 12);
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
      const drv = DRIVERS[activeKey] || DRIVERS['3'];

      const prog = getDriverProgress(activeKey, currentSecond);
      const activeSec = getDriverTrackSec(activeKey, currentSecond);
      const state = getDriverVisualState(activeKey, currentSecond);
      const isRet = state.isRetired;
      const inPit = state.inPit;
      const tire = getDriverTire(activeKey, currentSecond);
      const isSC = isSafetyCarActive(currentSecond);
      const isFinished = currentSecond >= RACE_FINISH_SEC;

      const pt = getTrackPointAtTime(activeSec);

      // Banner update
      document.getElementById('bannerColorBar').style.background = drv.color;
      document.getElementById('bannerNum').textContent = '#' + drv.number;
      document.getElementById('bannerName').textContent = drv.firstName + ' ' + drv.lastName + ' (' + drv.code + ')';
      document.getElementById('bannerTeam').textContent = drv.team;
      
      const bannerStatus = document.getElementById('bannerStatus');
      if (isRet) {
        bannerStatus.style.background = '#da3633';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = 'RITIRATO (DNF)';
      } else if (state.isOffTrack) {
        bannerStatus.style.background = '#e3b341';
        bannerStatus.style.color = '#000';
        bannerStatus.textContent = '⚠️ FUORI PISTA (VIA DI FUGA)';
      } else if (state.status === 'REJOINING_TRACK') {
        bannerStatus.style.background = '#1f6feb';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = '↩️ RIENTRO IN PISTA';
      } else if (inPit) {
        bannerStatus.style.background = '#d29922';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = currentSecond >= SESSION_DURATION ? 'PARC FERMÉ' : 'PIT LANE';
      } else if (isFinished) {
        bannerStatus.style.background = '#8957e5';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = 'TRAGUARDO';
      } else if (isSC) {
        bannerStatus.style.background = '#e3b341';
        bannerStatus.style.color = '#000';
        bannerStatus.textContent = 'SAFETY CAR';
      } else {
        bannerStatus.style.background = '#238636';
        bannerStatus.style.color = '#fff';
        bannerStatus.textContent = 'TRACK LIVE';
      }

      const bannerTire = document.getElementById('bannerTire');
      bannerTire.className = 'tire-badge tire-' + tire;
      bannerTire.textContent = tire === 'S' ? 'SOFT' : (tire === 'M' ? 'MEDIUM' : 'HARD');

      // Cockpit Telemetry
      const speed = state.speed;
      const rpm = state.rpm;
      const throttle = state.throttle;
      const gear = state.gear;
      const drs = state.drs;
      const locText = state.isRetired ? ('🛑 ' + state.location) : (state.isOffTrack ? ('⚠️ ' + state.location) : (state.status === 'REJOINING_TRACK' ? ('↩️ ' + state.location) : (inPit ? ('🔧 ' + state.location) : (isSC ? ('🟠 SAFETY CAR IN PISTA — ' + state.location) : ('📍 ' + state.location)))));

      document.getElementById('gearVal').textContent = gear;
      document.getElementById('speedVal').textContent = Math.round(speed);
      document.getElementById('rpmVal').textContent = rpm.toLocaleString() + ' RPM';

      const leds = document.querySelectorAll('.led');
      const rpmFrac = Math.max(0, Math.min(1, (rpm - 10000) / 2500));
      const activeLeds = Math.round(rpmFrac * leds.length);
      leds.forEach((led, idx) => {
        if (idx < activeLeds) led.classList.add('on');
        else led.classList.remove('on');
      });

      document.getElementById('throttleVal').textContent = throttle + '%';
      document.getElementById('throttleFill').style.width = throttle + '%';

      const brakeTag = document.getElementById('brakeTag');
      const isBraking = (currentSecond < 0) || (state.brake >= 50 && currentSecond > 6.8 && !isFinished && !isRet && !inPit) || (state.status === 'OFF_TRACK_SLIDE' || state.status === 'CRASH_TRANSITION');
      if (isBraking) {
        brakeTag.className = 'tag brake-on';
        brakeTag.textContent = currentSecond < 0 ? 'FRENO ATTIVO (HOLD)' : (state.isOffTrack ? 'FRENO ATTIVO (STACCATA/BLOCCAGGIO)' : 'FRENO ATTIVO');
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

      document.getElementById('locName').textContent = locText;
      document.getElementById('gpsCoords').textContent = 'GPS: X = ' + (pt.x >= 0 ? '+' : '') + pt.x + '.0m, Y = ' + (pt.y >= 0 ? '+' : '') + pt.y + '.0m';

      const secBadge = document.getElementById('sectorBadge');
      secBadge.className = 'sector-badge sec-' + (isRet ? 2 : (inPit ? 1 : pt.sector));
      secBadge.textContent = isRet ? 'OUT' : (inPit ? 'PIT LANE' : 'SETTORE ' + pt.sector);

      // Cockpit Lap & Sector Card
      const sessionBests = getSessionBests(currentSecond);
      const activeStats = getDriverTimingStats(activeKey, currentSecond, sessionBests);
      const curLapNum = getDriverLap(activeKey, currentSecond);
      document.getElementById('hudLapNumBadge').textContent = 'GIRO ' + curLapNum + ' / 51';

      const hudSec1Box = document.getElementById('hudSec1Box');
      const hudSec2Box = document.getElementById('hudSec2Box');
      const hudSec3Box = document.getElementById('hudSec3Box');
      hudSec1Box.classList.remove('active-sec');
      hudSec2Box.classList.remove('active-sec');
      hudSec3Box.classList.remove('active-sec');

      if (!isRet && !inPit && currentSecond >= 0) {
        if (pt.sector === 1) hudSec1Box.classList.add('active-sec');
        else if (pt.sector === 2) hudSec2Box.classList.add('active-sec');
        else if (pt.sector === 3) hudSec3Box.classList.add('active-sec');
      }

      const hudSec1Val = document.getElementById('hudSec1Val');
      const hudSec1Tag = document.getElementById('hudSec1Tag');
      hudSec1Val.textContent = activeStats.s1Str;
      hudSec1Tag.className = 'hud-sec-tag f1-badge badge-' + activeStats.s1Badge;
      hudSec1Tag.textContent = activeStats.s1Badge === 'purple' ? 'ASSOLUTO' : (activeStats.s1Badge === 'green' ? 'PERSONALE' : (activeStats.s1Badge === 'yellow' ? 'NO MIGL.' : 'IN ATTESA'));

      const hudSec2Val = document.getElementById('hudSec2Val');
      const hudSec2Tag = document.getElementById('hudSec2Tag');
      hudSec2Val.textContent = activeStats.s2Str;
      hudSec2Tag.className = 'hud-sec-tag f1-badge badge-' + activeStats.s2Badge;
      hudSec2Tag.textContent = activeStats.s2Badge === 'purple' ? 'ASSOLUTO' : (activeStats.s2Badge === 'green' ? 'PERSONALE' : (activeStats.s2Badge === 'yellow' ? 'NO MIGL.' : 'IN ATTESA'));

      const hudSec3Val = document.getElementById('hudSec3Val');
      const hudSec3Tag = document.getElementById('hudSec3Tag');
      hudSec3Val.textContent = activeStats.s3Str;
      hudSec3Tag.className = 'hud-sec-tag f1-badge badge-' + activeStats.s3Badge;
      hudSec3Tag.textContent = activeStats.s3Badge === 'purple' ? 'ASSOLUTO' : (activeStats.s3Badge === 'green' ? 'PERSONALE' : (activeStats.s3Badge === 'yellow' ? 'NO MIGL.' : 'IN ATTESA'));

      const hudLastLapVal = document.getElementById('hudLastLapVal');
      const hudLastLapTag = document.getElementById('hudLastLapTag');
      hudLastLapVal.textContent = activeStats.lastLapStr;
      hudLastLapTag.className = 'hud-lap-status f1-badge badge-' + activeStats.lastLapBadge;
      hudLastLapTag.textContent = activeStats.lastLapBadge === 'purple' ? 'ASSOLUTO' : (activeStats.lastLapBadge === 'green' ? 'MIGLIOR PERSONALE' : (activeStats.lastLapBadge === 'yellow' ? 'NO MIGLIORAMENTO' : 'NESSUN GIRO'));

      const hudBestLapVal = document.getElementById('hudBestLapVal');
      const hudBestLapTag = document.getElementById('hudBestLapTag');
      hudBestLapVal.textContent = activeStats.bestLapStr;
      hudBestLapTag.className = 'hud-lap-status f1-badge badge-' + activeStats.bestLapBadge;
      hudBestLapTag.textContent = activeStats.bestLapBadge === 'purple' ? 'MIGLIORE GARA' : (activeStats.bestLapBadge === 'green' ? 'RECORD PERSONALE' : 'IN ATTESA');

      const fastestLapValEl = document.getElementById('hudSessionFastestVal');
      const fastestLapDrvEl = document.getElementById('hudSessionFastestDrv');
      if (sessionBests.sessionBestLap < Infinity) {
        fastestLapValEl.textContent = formatLapTime(sessionBests.sessionBestLap);
        const fDrv = DRIVERS[sessionBests.sessionBestLapDriver];
        fastestLapDrvEl.textContent = fDrv ? ('#' + fDrv.number + ' ' + fDrv.code) : '';
      } else {
        fastestLapValEl.textContent = '1:44.916';
        fastestLapDrvEl.textContent = '#63 RUS';
      }

      // Header Clock
      const baseSec = 11 * 3600 + 3 * 60 + 51 + Math.floor(currentSecond);
      const hh = String(Math.floor(baseSec / 3600) % 24).padStart(2, '0');
      const mm = String(Math.floor((baseSec % 3600) / 60)).padStart(2, '0');
      const ss = String(baseSec % 60).padStart(2, '0');
      
      let lapText = '';
      if (currentSecond < RACE_FINISH_SEC) {
        lapText = 'Giro: ' + curLapNum + '/51 (Gara Lineare)';
      } else {
        lapText = 'Giro: 51/51 🏁 BANDIERA A SCACCHI';
      }
      document.getElementById('headerClock').textContent = '26 Settembre 2026 | ' + hh + ':' + mm + ':' + ss + ' UTC | ' + lapText;
      document.getElementById('timeScrubber').value = Math.floor(currentSecond);

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
        if (ev.timeSec <= currentSecond) latest = ev;
        else break;
      }
      if (latest && latest.text !== lastEventText) {
        lastEventText = latest.text;
        const ticker = document.getElementById('overtakeTicker');
        ticker.textContent = latest.text;
        ticker.style.color = '#388bfd';
        setTimeout(() => { ticker.style.color = '#f0f6fc'; }, 400);
      }
    }

    function updateRaceControl() {
      const currentUtcSec = Math.floor(currentSecond);
      const activeMsgs = RACE_CONTROL_MESSAGES.filter(m => m.timeSec <= currentUtcSec);

      const flagBadge = document.getElementById('rcFlagBadge');
      const drsBadge = document.getElementById('rcDrsBadge');

      if (currentSecond >= RACE_FINISH_SEC) {
        flagBadge.className = 'rc-pill rc-flag-chequered';
        flagBadge.textContent = '🏁 BANDIERA A SCACCHI';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else if (isSafetyCarActive(currentSecond)) {
        flagBadge.className = 'rc-pill rc-flag-vsc';
        flagBadge.textContent = '🟠 SAFETY CAR DEPLOYED';
        drsBadge.className = 'rc-pill rc-drs-off';
        drsBadge.textContent = 'DRS OFF';
      } else {
        flagBadge.className = 'rc-pill rc-flag-green';
        flagBadge.textContent = '🟢 GARA ATTIVA (BANDIERA VERDE)';
        if (currentSecond >= 250 && !isSafetyCarActive(currentSecond)) {
          drsBadge.className = 'rc-pill rc-drs-on';
          drsBadge.textContent = 'DRS ON';
        } else {
          drsBadge.className = 'rc-pill rc-drs-off';
          drsBadge.textContent = 'DRS OFF';
        }
      }

      document.getElementById('rcTotalCount').textContent = activeMsgs.length + ' / ' + RACE_CONTROL_MESSAGES.length + ' Messaggi';

      const recent = activeMsgs.slice(-7).reverse();
      const feedBox = document.getElementById('rcFeedBox');
      let html = '';
      recent.forEach(m => {
        let tagClass = 'rc-tag-other';
        let tagLabel = m.category;
        if (m.category === 'Flag') { tagClass = 'rc-tag-flag'; tagLabel = m.flag || 'FLAG'; }
        else if (m.category === 'SafetyCar') { tagClass = 'rc-tag-vsc'; tagLabel = 'SAFETY CAR'; }
        else if (m.category === 'Drs') { tagClass = 'rc-tag-drs'; tagLabel = 'DRS'; }

        const timeStr = m.date.substring(11, 19);
        const lapStr = m.lap_number ? ('L' + m.lap_number) : 'PRE';
        html += '<div class="rc-item" onclick="jumpToRace(' + Math.max(0, m.timeSec) + ')">'
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

        const curLap = getDriverLap(k, currentSecond);

        return {
          key: k,
          driver: d,
          totalProgress: prog,
          lapProgress: lapProgress,
          currentLap: curLap,
          inPit: inPit,
          isRetired: isRet,
          retiredLap: RETIREMENTS[k]?.lap,
          tire: tire,
          gridPos: gridIndex,
          stats: stats
        };
      }).sort((a, b) => {
        // Active cars always before retired cars
        if (a.isRetired && !b.isRetired) return 1;
        if (!a.isRetired && b.isRetired) return -1;
        if (a.isRetired && b.isRetired) {
          // If both retired, sort by completed laps descending, then retirement time
          if (b.retiredLap !== a.retiredLap) return b.retiredLap - a.retiredLap;
          return (RETIREMENTS[b.key]?.timeSec || 0) - (RETIREMENTS[a.key]?.timeSec || 0);
        }
        // Both active: sort strictly by continuous race progress
        return b.totalProgress - a.totalProgress;
      });

      const leaderProgress = ranked[0].totalProgress;
      const leaderLap = ranked[0].currentLap;

      var html = '';
      ranked.forEach(function(r, idx) {
        var d = r.driver;
        var isCurrentActive = r.key === activeKey;
        if (r.isRetired) {
          gap = '<span style="color:#da3633; font-weight:800;">DNF (L' + r.retiredLap + ')</span>';
        } else {
          const vState = getDriverVisualState(r.key, currentSecond);
          if (vState.isOffTrack) {
            gap = '<span style="background:#e3b341; color:#000; font-size:8.5px; font-weight:800; padding:1px 5px; border-radius:3px;">FUORI PISTA</span>';
          } else if (vState.status === 'REJOINING_TRACK') {
            gap = '<span style="background:#1f6feb; color:#fff; font-size:8.5px; font-weight:800; padding:1px 5px; border-radius:3px;">RIENTRO</span>';
          } else if (idx === 0) {
            gap = 'LEADER';
          } else {
            var diff = leaderProgress - r.totalProgress;
            if (diff < 0) diff = 0;
            if (leaderLap - r.currentLap >= 2) {
              gap = '+' + (leaderLap - r.currentLap) + ' GIRI';
            } else if (leaderLap - r.currentLap === 1) {
              gap = '+1 GIRO';
            } else {
              gap = '+' + diff.toFixed(1) + 's';
            }
          }
        }
        var currentPos = idx + 1;
        var posDelta = r.gridPos - currentPos;

        var deltaHtml = '<span style="color: #8b949e;">-</span>';
        if (!r.isRetired && posDelta > 0) {
          deltaHtml = '<span style="color: #2ea043; font-weight: 800;">▲' + posDelta + '</span>';
        } else if (!r.isRetired && posDelta < 0) {
          deltaHtml = '<span style="color: #da3633; font-weight: 800;">▼' + Math.abs(posDelta) + '</span>';
        }

        var tireColor = r.tire === 'S' ? '#da3633' : (r.tire === 'M' ? '#e3b341' : '#f0f6fc');

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
          + '<td style="text-align: right; color: ' + (idx === 0 ? '#e3b341' : (r.isRetired ? '#da3633' : '#8b949e')) + '; font-family: monospace;">' + gap + '</td>'
          + '<td style="text-align: center;"><span style="color:' + (r.isRetired ? '#64748b' : tireColor) + '; font-weight:800; font-size:10px;">[' + (r.isRetired ? '-' : r.tire) + ']</span></td>'
          + '</tr>';
      });
      tbody.innerHTML = html;
    }

    document.getElementById('timingTableBody').addEventListener('click', function(e) {
      var row = e.target.closest('tr');
      if (row && row.dataset.driver) selectDriverFromTable(row.dataset.driver);
    });

    let hoveredDriver = null;
    canvas.addEventListener('mousemove', (e) => {
      const rect = canvas.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      const my = e.clientY - rect.top;

      let found = null;
      for (const [key, item] of Object.entries(carScreenPositions)) {
        const dist = Math.hypot(mx - item.x, my - item.y);
        if (dist <= 14) { found = key; break; }
      }
      hoveredDriver = found;
      canvas.style.cursor = found ? 'pointer' : 'crosshair';
    });

    canvas.addEventListener('click', () => {
      if (hoveredDriver) selectDriverFromTable(hoveredDriver);
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
            ticker.textContent = '🏁 SESSION FINISHED — 51/51 Giri completati. Vincitore: Max Verstappen (Red Bull Racing)! Podio: 1° Verstappen, 2° Russell (+0.003s in volata!), 3° Hadjar (+10.5s).';
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

    const playBtn = document.getElementById('playPauseBtn');
    playBtn.onclick = () => {
      if (currentSecond >= SESSION_DURATION) currentSecond = 0;
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
      if (val !== 'all') focusedDriver = val;
      updateHUD();
      drawTrack();
    };

    function jumpToRace(sec, drvKey) {
      currentSecond = sec;
      if (drvKey) {
        selectedDriver = drvKey;
        focusedDriver = drvKey;
        const sel = document.getElementById('driverSelect');
        if (sel) sel.value = drvKey;
      }
      updateHUD();
      drawTrack();
      updateLeaderboard();
      updateRaceControl();
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
  .replace('__PIT_LANE_NODES_JSON__', pitLaneNodesJson)
  .replace('__RACE_EVENTS_JSON__', raceEventsJson)
  .replace('__RACE_CONTROL_MESSAGES_JSON__', raceControlJson)
  .replace('__RETIREMENTS_JSON__', retirementsJson)
  .replace('__INCIDENTS_JSON__', incidentsJson)
  .replace('__DRIVER_STINTS_JSON__', driverStintsJson)
  .replace('__DRIVER_LAPS_JSON__', driverLapsJson)
  .replace('__DRIVER_OPTIONS_HTML__', driverOptionsHtml);

// Extract CSS and JS to separate modular files
const styleMatch = finalHtml.match(/<style>([\s\S]*?)<\/style>/);
const scriptMatch = finalHtml.match(/<script>([\s\S]*?)<\/script>/);

if (styleMatch && scriptMatch) {
  const css = styleMatch[1].trim();
  const js = scriptMatch[1].trim();
  const cleanHtml = finalHtml
    .replace(/<style>[\s\S]*?<\/style>/, '<link rel="stylesheet" href="css/baku_timing.css">')
    .replace(/<script>[\s\S]*?<\/script>/, '<script src="js/baku_timing_app.js"></script>');

  const cssDir = path.join(__dirname, 'css');
  const jsDir = path.join(__dirname, 'js');
  const simBakuDir = path.join(__dirname, 'simulations', 'baku');
  if (!fs.existsSync(cssDir)) fs.mkdirSync(cssDir, { recursive: true });
  if (!fs.existsSync(jsDir)) fs.mkdirSync(jsDir, { recursive: true });
  if (!fs.existsSync(simBakuDir)) fs.mkdirSync(simBakuDir, { recursive: true });

  fs.writeFileSync(path.join(cssDir, 'baku_timing.css'), css, 'utf-8');
  fs.writeFileSync(path.join(jsDir, 'baku_timing_app.js'), js, 'utf-8');
  fs.writeFileSync(path.join(__dirname, 'track_map_baku.html'), cleanHtml, 'utf-8');

  // Isolated simulation files
  fs.writeFileSync(path.join(simBakuDir, 'baku_timing.css'), css, 'utf-8');
  fs.writeFileSync(path.join(simBakuDir, 'baku_timing_app.js'), js, 'utf-8');
  fs.writeFileSync(path.join(simBakuDir, 'track_map_baku.html'), cleanHtml.replace('src="js/', 'src="').replace('href="css/', 'href="'), 'utf-8');

  console.log('Successfully wrote modular Baku files:');
  console.log('  - css/baku_timing.css:', css.length, 'bytes');
  console.log('  - js/baku_timing_app.js:', js.length, 'bytes');
  console.log('  - track_map_baku.html:', cleanHtml.length, 'bytes');
  console.log('  - simulations/baku/track_map_baku.html & assets isolated');
} else {
  fs.writeFileSync(path.join(__dirname, 'track_map_baku.html'), finalHtml, 'utf-8');
  const simBakuDir = path.join(__dirname, 'simulations', 'baku');
  if (!fs.existsSync(simBakuDir)) fs.mkdirSync(simBakuDir, { recursive: true });
  fs.writeFileSync(path.join(simBakuDir, 'track_map_baku.html'), finalHtml, 'utf-8');
  console.log('Successfully wrote track_map_baku.html! Size:', finalHtml.length);
}

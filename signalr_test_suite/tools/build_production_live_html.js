const fs = require('fs');
const path = require('path');

const dataDir = path.join(__dirname, '..', 'data');
const assetsDir = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'live_timing');

// 1. Load authentic Sepang data tested and validated in test suite
const sepangNodes = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_exact_track_full.json'), 'utf-8'));
const compactSepangNodes = sepangNodes.map(n => [
  n.t,
  Math.round(n.px * 100) / 100,
  Math.round(n.py * 100) / 100,
  Math.round(n.x),
  Math.round(n.y),
  n.sector,
  Math.round(n.speed),
  n.gear,
  n.rpm,
  n.throttle,
  n.brake,
  n.drs,
  n.location
]);

const sepangDrivers = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_drivers.json'), 'utf-8'));
const sepangKeyframes = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_keyframes.json'), 'utf-8'));
const sepangPitStops = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_pit_stops.json'), 'utf-8'));
const sepangRC = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_race_control_messages.json'), 'utf-8'));
const sepangRetirements = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_retirements.json'), 'utf-8'));
const sepangStints = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_driver_stints.json'), 'utf-8'));
const sepangLaps = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_driver_laps.json'), 'utf-8'));
const sepangEvents = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_race_events.json'), 'utf-8'));
const sepangIncidents = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_incidents.json'), 'utf-8'));

const rm = require('../src/race_model.js');
const sepangLapStarts = rm.LAP_STARTS;

console.log('Loaded Sepang data: ' + compactSepangNodes.length + ' nodes, ' + Object.keys(sepangDrivers).length + ' drivers.');

// Driver dropdown options
const driverOptionsHtml = Object.entries(sepangDrivers).map(([num, d]) => {
  return `<option value="${num}">#${num} ${d.code} — ${d.firstName} ${d.lastName} (${d.team})</option>`;
}).join('\n                ');

// Build the complete production HTML template
const htmlContent = `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
  <title>FastestLap — Live Timing & Telemetria</title>
  <style>
    :root {
      --bg-dark: #000000;
      --bg-panel: #0d1117;
      --bg-card: #161b22;
      --bg-card-hover: #1c2128;
      --border-color: #30363d;
      --border-focus: #58a6ff;
      --text-main: #f0f6fc;
      --text-muted: #8b949e;
      --text-dim: #484f58;
      --red-f1: #e10600;
      --green-f1: #238636;
      --purple-f1: #a371f7;
      --yellow-f1: #d29922;
      --cyan-f1: #38bdf8;
      --header-h: 36px;
      --safe-top: max(4px, env(safe-area-inset-top));
      --safe-left: max(8px, env(safe-area-inset-left));
      --safe-right: max(8px, env(safe-area-inset-right));
    }

    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }

    html, body {
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: var(--bg-dark);
      color: var(--text-main);
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      user-select: none;
      -webkit-user-select: none;
    }

    /* ══════════════════════════════════════════════════════════════
       OFFLINE / NO SESSION IN PROGRESS VIEW
       ══════════════════════════════════════════════════════════════ */
    #offlineView {
      display: none;
      width: 100vw;
      height: 100vh;
      background-color: #000000;
      position: absolute;
      top: 0;
      left: 0;
      z-index: 9999;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px;
      text-align: center;
    }

    .offline-box {
      max-width: 500px;
      width: 90%;
      background: #090d13;
      border: 1px solid #30363d;
      border-radius: 12px;
      padding: 24px 20px;
      box-shadow: 0 16px 40px rgba(0, 0, 0, 0.85);
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .offline-badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #21262d;
      color: #8b949e;
      border: 1px solid #30363d;
      font-size: 11px;
      font-weight: 700;
      padding: 3px 10px;
      border-radius: 20px;
      margin-bottom: 14px;
      text-transform: uppercase;
      letter-spacing: 0.8px;
    }

    .offline-title {
      font-size: 18px;
      font-weight: 900;
      letter-spacing: 1.2px;
      color: #f0f6fc;
      margin-bottom: 8px;
      text-transform: uppercase;
      font-family: -apple-system, BlinkMacSystemFont, sans-serif;
    }

    .offline-event {
      font-size: 13px;
      font-weight: 700;
      color: var(--red-f1);
      margin-bottom: 4px;
      text-transform: uppercase;
    }

    .offline-session {
      font-size: 12px;
      font-weight: 600;
      color: #8b949e;
      margin-bottom: 12px;
    }

    .offline-desc {
      font-size: 11.5px;
      color: #8b949e;
      line-height: 1.5;
      margin-bottom: 18px;
    }

    .offline-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: #21262d;
      color: #f0f6fc;
      border: 1px solid #30363d;
      font-size: 12px;
      font-weight: 700;
      padding: 8px 18px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .offline-btn:hover, .offline-btn:active {
      background: var(--red-f1);
      border-color: var(--red-f1);
      color: #fff;
    }

    /* ══════════════════════════════════════════════════════════════
       PRODUCTION LIVE TIMING VIEW (LANDSCAPE FULLSCREEN)
       ══════════════════════════════════════════════════════════════ */
    #liveView {
      display: flex;
      flex-direction: column;
      width: 100vw;
      height: 100vh;
      overflow: hidden;
      background: var(--bg-dark);
    }

    /* Top Navigation Header with Safe-Area margin to prevent Android status bar overlap */
    header {
      height: calc(var(--header-h) + var(--safe-top));
      padding-top: var(--safe-top);
      padding-left: var(--safe-left);
      padding-right: var(--safe-right);
      background: #11141a;
      border-bottom: 1px solid #21262d;
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-shrink: 0;
      gap: 8px;
    }

    .hdr-left {
      display: flex;
      align-items: center;
      gap: 8px;
      min-width: 0;
    }

    .back-nav-btn {
      background: #21262d;
      border: 1px solid #30363d;
      color: #f0f6fc;
      width: 26px;
      height: 26px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      cursor: pointer;
      flex-shrink: 0;
    }
    .back-nav-btn:hover { background: #30363d; }

    .live-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: var(--red-f1);
      color: #fff;
      font-size: 9.5px;
      font-weight: 800;
      padding: 2px 6px;
      border-radius: 4px;
      letter-spacing: 0.5px;
      flex-shrink: 0;
    }
    .live-dot {
      width: 5px;
      height: 5px;
      background: #fff;
      border-radius: 50%;
      animation: pulseLive 1.2s infinite ease-in-out;
    }
    @keyframes pulseLive {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.4; transform: scale(0.8); }
    }

    .hdr-titles {
      display: flex;
      align-items: baseline;
      gap: 6px;
      overflow: hidden;
      white-space: nowrap;
    }
    .hdr-event {
      font-size: 11.5px;
      font-weight: 800;
      color: #f0f6fc;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .hdr-session {
      font-size: 10.5px;
      font-weight: 700;
      color: #8b949e;
      text-transform: uppercase;
    }

    .hdr-center {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-shrink: 0;
    }
    .lap-indicator {
      font-size: 11px;
      font-weight: 800;
      color: #f0f6fc;
      background: #1c2128;
      border: 1px solid #30363d;
      padding: 2px 7px;
      border-radius: 4px;
      font-family: ui-monospace, Menlo, Consolas, monospace;
    }
    .clock-indicator {
      font-size: 11px;
      font-weight: 700;
      color: var(--cyan-f1);
      font-family: ui-monospace, Menlo, Consolas, monospace;
    }

    .speed-multiplier-box {
      display: inline-flex;
      background: #1c2128;
      border: 1px solid #30363d;
      border-radius: 4px;
      overflow: hidden;
    }
    .spd-btn {
      background: transparent;
      border: none;
      color: #8b949e;
      font-size: 9.5px;
      font-weight: 800;
      padding: 2px 6px;
      cursor: pointer;
    }
    .spd-btn.active {
      background: #238636;
      color: #fff;
    }

    /* Tabs Bar */
    .hdr-tabs {
      display: flex;
      gap: 4px;
      align-items: center;
      flex-shrink: 0;
    }
    .tab-btn {
      background: transparent;
      border: 1px solid transparent;
      color: #8b949e;
      padding: 3px 8px;
      font-size: 10.5px;
      font-weight: 700;
      border-radius: 6px;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      transition: all 0.15s ease;
    }
    .tab-btn:hover {
      background: #21262d;
      color: #f0f6fc;
    }
    .tab-btn.active {
      background: #21262d;
      color: #f0f6fc;
      border-color: #388bfd;
      box-shadow: inset 0 -2px 0 #388bfd;
    }

    /* ══════════════════════════════════════════════════════════════
       MAIN CONTENT CONTAINER
       ══════════════════════════════════════════════════════════════ */
    .content-body {
      display: flex;
      flex: 1;
      height: calc(100vh - (var(--header-h) + var(--safe-top)));
      overflow: hidden;
      background: #000000;
    }

    /* Left Pane: Shared Interactive Canvas (NO white background box!) */
    .track-pane {
      position: relative;
      background: #070a0f;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      flex-shrink: 0;
      transition: width 0.2s ease;
    }
    .track-pane.w-35 { width: 36%; }
    .track-pane.w-32 { width: 32%; }

    #trackCanvas {
      display: block;
      cursor: crosshair;
    }

    .canvas-overlay-bar {
      position: absolute;
      top: 6px;
      left: 8px;
      display: flex;
      gap: 5px;
      align-items: center;
      pointer-events: none;
      z-index: 5;
    }
    .status-pill {
      background: rgba(13, 17, 23, 0.85);
      border: 1px solid #30363d;
      color: #f0f6fc;
      font-size: 9.5px;
      font-weight: 700;
      padding: 2px 6px;
      border-radius: 4px;
      backdrop-filter: blur(4px);
    }
    .status-pill.green { border-color: #238636; color: #3fb950; }
    .status-pill.yellow { border-color: #d29922; color: #e3b341; }
    .status-pill.sc { border-color: #f0883e; background: rgba(240, 136, 62, 0.25); color: #ffa657; }

    /* Right Pane: Swappable Tab Panes */
    .view-pane {
      flex: 1;
      background: #0d1117;
      border-left: 1px solid #21262d;
      display: none;
      flex-direction: column;
      height: 100%;
      overflow: hidden;
      min-width: 0;
    }
    .view-pane.active {
      display: flex;
    }

    /* ══════════════════════════════════════════════════════════════
       TAB 1: MAPPA & CLASSIFICA (INGRANDITA & COMPLETAMENTE VISIBILE)
       ══════════════════════════════════════════════════════════════ */
    .standings-table-container {
      flex: 1;
      width: 100%;
      height: 100%;
      overflow-y: auto;
      overflow-x: auto;
      background: #0d1117;
    }
    .standings-table-container::-webkit-scrollbar { width: 5px; height: 5px; }
    .standings-table-container::-webkit-scrollbar-thumb { background: #30363d; border-radius: 3px; }

    .timing-table {
      width: 100%;
      min-width: 530px;
      border-collapse: collapse;
      font-size: 11.5px;
      table-layout: auto;
    }
    .timing-table th {
      background: #161b22;
      color: #8b949e;
      font-weight: 800;
      padding: 6px 4px;
      text-align: center;
      position: sticky;
      top: 0;
      font-size: 9.5px;
      border-bottom: 1px solid #30363d;
      letter-spacing: 0.4px;
      text-transform: uppercase;
      z-index: 10;
      white-space: nowrap;
    }
    .timing-table td {
      padding: 3.5px 4px;
      border-bottom: 1px solid #1c2128;
      white-space: nowrap;
      text-align: center;
    }
    .timing-row {
      cursor: pointer;
      transition: background 0.15s;
    }
    .timing-row:hover { background: #1c2128; }
    .timing-row.active {
      background: rgba(88, 166, 255, 0.16);
      border-left: 3px solid #58a6ff;
    }

    .pos-cell { font-weight: 800; color: #f0f6fc; width: 22px; text-align: center; font-size: 11.5px; }
    .delta-cell { width: 18px; text-align: center; font-size: 9.5px; font-weight: 700; }
    
    .driver-cell {
      text-align: left !important;
      font-weight: 700;
      color: #f0f6fc;
      padding-left: 6px !important;
    }
    .driver-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
    }
    .color-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      display: inline-block;
      flex-shrink: 0;
    }

    /* Badges for Sectors & Lap Times */
    .f1-badge {
      display: inline-block;
      padding: 1.5px 5px;
      border-radius: 3px;
      font-family: ui-monospace, Menlo, Consolas, monospace;
      font-size: 10.5px;
      font-weight: 700;
      min-width: 36px;
      text-align: center;
    }
    .badge-purple { background: rgba(163, 113, 247, 0.22); color: #d2a8ff; border: 1px solid rgba(163, 113, 247, 0.5); }
    .badge-green { background: rgba(35, 134, 54, 0.22); color: #56d364; border: 1px solid rgba(46, 160, 67, 0.5); }
    .badge-yellow { background: rgba(210, 153, 34, 0.18); color: #e3b341; }
    .badge-dim { color: #484f58; }

    .gap-cell {
      text-align: right !important;
      font-family: ui-monospace, Menlo, Consolas, monospace;
      font-size: 11px;
      padding-right: 8px !important;
      font-weight: 600;
    }
    .tyre-badge {
      font-size: 10px;
      font-weight: 800;
      padding: 1px 4px;
      border-radius: 3px;
      text-align: center;
    }
    .tyre-S { color: #ff7b72; border: 1px solid #f85149; }
    .tyre-M { color: #f2cc60; border: 1px solid #d29922; }
    .tyre-H { color: #f0f6fc; border: 1px solid #8b949e; }
    .tyre-I { color: #56d364; border: 1px solid #238636; }
    .tyre-W { color: #79c0ff; border: 1px solid #1f6feb; }

    .aero-badge {
      font-size: 9.5px;
      font-weight: 800;
      padding: 1px 5px;
      border-radius: 3px;
      text-align: center;
      display: inline-block;
    }
    .aero-z { background: rgba(56, 189, 248, 0.2); color: #38bdf8; border: 1px solid #0284c7; }
    .aero-x { background: rgba(35, 134, 54, 0.15); color: #3fb950; border: 1px solid #238636; }

    /* ══════════════════════════════════════════════════════════════
       TAB 2: TELEMETRIA (DUAL COCKPIT COMPACT & RESPONSIVE)
       ══════════════════════════════════════════════════════════════ */
    .telemetry-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 6px;
      gap: 5px;
      overflow-y: auto;
      overflow-x: hidden;
      box-sizing: border-box;
    }
    .telemetry-container::-webkit-scrollbar { width: 4px; }
    .telemetry-container::-webkit-scrollbar-thumb { background: #30363d; border-radius: 2px; }

    .dual-selector-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 6px;
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 4px 8px;
      flex-shrink: 0;
    }
    .sel-group {
      display: flex;
      align-items: center;
      gap: 5px;
      flex: 1;
      min-width: 0;
    }
    .sel-label {
      font-size: 10px;
      font-weight: 800;
      color: #8b949e;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .drv-dropdown {
      background: #0d1117;
      color: #f0f6fc;
      border: 1px solid #30363d;
      border-radius: 4px;
      padding: 3px 6px;
      font-size: 10.5px;
      font-weight: 700;
      flex: 1;
      cursor: pointer;
      min-width: 0;
      text-overflow: ellipsis;
    }
    .drv-dropdown:focus { border-color: #388bfd; outline: none; }
    .swap-btn {
      background: #21262d;
      border: 1px solid #30363d;
      color: #f0f6fc;
      width: 24px;
      height: 24px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      font-size: 12px;
      flex-shrink: 0;
    }

    .dual-cockpit-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 6px;
      flex: 1;
      min-height: 0;
    }

    .cockpit-card {
      background: #11141a;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 7px;
      display: flex;
      flex-direction: column;
      gap: 5px;
      box-sizing: border-box;
      overflow: hidden;
    }

    .card-top-info {
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-bottom: 1px solid #21262d;
      padding-bottom: 4px;
    }
    .card-drv-name {
      font-size: 12px;
      font-weight: 800;
      color: #f0f6fc;
      display: flex;
      align-items: center;
      gap: 5px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .card-drv-team {
      font-size: 9.5px;
      color: #8b949e;
      font-weight: 600;
      white-space: nowrap;
    }

    /* Speed & Gear cluster */
    .speed-gear-cluster {
      display: flex;
      align-items: center;
      justify-content: space-around;
      background: #090d13;
      border: 1px solid #21262d;
      border-radius: 6px;
      padding: 4px 6px;
    }
    .speed-display { text-align: center; }
    .speed-num {
      font-size: 26px;
      font-weight: 900;
      color: #f0f6fc;
      font-family: ui-monospace, Menlo, monospace;
      line-height: 1;
    }
    .speed-label {
      font-size: 9px;
      font-weight: 800;
      color: #8b949e;
    }

    .gear-display { text-align: center; min-width: 36px; }
    .gear-num {
      font-size: 28px;
      font-weight: 900;
      color: var(--red-f1);
      font-family: ui-monospace, Menlo, monospace;
      line-height: 1;
    }
    .gear-label {
      font-size: 9px;
      font-weight: 800;
      color: #8b949e;
    }

    .rpm-container {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .rpm-meta {
      display: flex;
      justify-content: space-between;
      font-size: 9px;
      font-weight: 700;
      color: #8b949e;
    }
    .rpm-num {
      color: #79c0ff;
      font-family: ui-monospace, Menlo, monospace;
      font-weight: 800;
    }

    .shift-led-bar {
      display: flex;
      gap: 2px;
      height: 5px;
      background: #090d13;
      padding: 1px;
      border-radius: 3px;
      border: 1px solid #21262d;
    }
    .shift-led {
      flex: 1;
      border-radius: 1px;
      background: #1c2128;
    }
    .shift-led.green-on { background: #3fb950; box-shadow: 0 0 4px rgba(63, 185, 80, 0.8); }
    .shift-led.yellow-on { background: #d29922; box-shadow: 0 0 4px rgba(210, 153, 34, 0.8); }
    .shift-led.red-on { background: #f85149; box-shadow: 0 0 5px rgba(248, 81, 73, 0.8); }

    .pedals-box {
      display: flex;
      gap: 6px;
    }
    .pedal-col {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 1px;
    }
    .pedal-label {
      display: flex;
      justify-content: space-between;
      font-size: 8.5px;
      font-weight: 700;
      color: #8b949e;
    }
    .pedal-track {
      height: 6px;
      background: #090d13;
      border-radius: 2px;
      border: 1px solid #21262d;
      overflow: hidden;
    }
    .pedal-bar-thr {
      height: 100%;
      background: linear-gradient(90deg, #238636, #3fb950);
      width: 0%;
      transition: width 0.06s ease;
    }
    .pedal-bar-brk {
      height: 100%;
      background: linear-gradient(90deg, #da3633, #f85149);
      width: 0%;
      transition: width 0.06s ease;
    }

    /* 2026 Regs: Active Aero, Battery SoC & Boost */
    .regs-2026-box {
      display: flex;
      flex-direction: column;
      gap: 4px;
      background: #090d13;
      border: 1px solid #21262d;
      border-radius: 5px;
      padding: 5px;
    }

    .aero-status-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 9px;
      font-weight: 800;
      color: #8b949e;
    }
    .aero-mode-badge {
      padding: 1px 6px;
      border-radius: 3px;
      font-size: 8.5px;
      font-weight: 800;
      letter-spacing: 0.4px;
    }
    .aero-zmode {
      background: rgba(56, 189, 248, 0.2);
      color: #38bdf8;
      border: 1px solid #0284c7;
    }
    .aero-xmode {
      background: rgba(35, 134, 54, 0.2);
      color: #3fb950;
      border: 1px solid #238636;
    }

    .battery-soc-box {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }
    .battery-meta {
      display: flex;
      justify-content: space-between;
      font-size: 8.5px;
      font-weight: 700;
      color: #8b949e;
    }
    .battery-track {
      height: 6px;
      background: #161b22;
      border-radius: 2px;
      border: 1px solid #30363d;
      overflow: hidden;
    }
    .battery-fill {
      height: 100%;
      width: 80%;
      background: linear-gradient(90deg, #e3b341, #3fb950);
      transition: width 0.2s ease;
    }

    .boost-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 9px;
      font-weight: 800;
      color: #8b949e;
    }
    .boost-pill {
      padding: 1.5px 6px;
      border-radius: 3px;
      font-size: 8.5px;
      font-weight: 800;
    }
    .boost-active {
      background: rgba(188, 140, 255, 0.25);
      color: #d2a8ff;
      border: 1px solid #a371f7;
    }
    .boost-avail {
      background: rgba(35, 134, 54, 0.2);
      color: #3fb950;
      border: 1px solid #238636;
    }
    .boost-standby {
      background: #1c2128;
      color: #8b949e;
      border: 1px solid #30363d;
    }

    .card-loc-text {
      font-size: 9px;
      color: #8b949e;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    /* ══════════════════════════════════════════════════════════════
       TAB 3: RACE CONTROL MESSAGES FEED
       ══════════════════════════════════════════════════════════════ */
    .rc-feed-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 8px;
      overflow-y: auto;
      gap: 6px;
    }
    .rc-feed-container::-webkit-scrollbar { width: 4px; }
    .rc-feed-container::-webkit-scrollbar-thumb { background: #30363d; border-radius: 2px; }

    .rc-status-banner {
      display: flex;
      gap: 6px;
      align-items: center;
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 6px;
      padding: 6px 10px;
      flex-shrink: 0;
    }
    .rc-pill {
      font-size: 9.5px;
      font-weight: 800;
      padding: 2px 7px;
      border-radius: 4px;
    }
    .rc-pill.flag-green { background: #238636; color: #fff; }
    .rc-pill.flag-yellow { background: #d29922; color: #000; }
    .rc-pill.flag-sc { background: #f0883e; color: #000; }
    .rc-pill.flag-vsc { background: #e3b341; color: #000; }
    .rc-pill.flag-chequered { background: #ffffff; color: #000; }

    .rc-ticker-card {
      background: #1c1408;
      border: 1px solid #d29922;
      border-radius: 6px;
      padding: 6px 10px;
      font-size: 11px;
      font-weight: 700;
      color: #f0f6fc;
      line-height: 1.35;
      flex-shrink: 0;
    }

    .rc-msg-card {
      background: #161b22;
      border: 1px solid #21262d;
      border-left: 3px solid #388bfd;
      border-radius: 5px;
      padding: 6px 8px;
      display: flex;
      flex-direction: column;
      gap: 2px;
      cursor: pointer;
      transition: background 0.15s;
    }
    .rc-msg-card:hover { background: #1c2128; }
    .rc-msg-card.yellow { border-left-color: #d29922; }
    .rc-msg-card.green { border-left-color: #238636; }
    .rc-msg-card.sc { border-left-color: #f0883e; }
    .rc-msg-meta {
      display: flex;
      justify-content: space-between;
      font-size: 9.5px;
      font-weight: 700;
      color: #8b949e;
    }
    .rc-msg-text {
      font-size: 11px;
      font-weight: 600;
      color: #f0f6fc;
      line-height: 1.3;
    }
  </style>
</head>
<body>

  <!-- ══════════════════════════════════════════════════════════════
       OFFLINE VIEW (Shown when no session is in progress)
       ══════════════════════════════════════════════════════════════ -->
  <div id="offlineView">
    <div class="offline-box">
      <div class="offline-badge">FASTESTLAP LIVE TIMING</div>
      <div class="offline-title" id="offlineTitle">content not available</div>
      <div class="offline-event" id="offlineEventTitle">FORMULA 1 GRAND PRIX</div>
      <div class="offline-session" id="offlineSessionName">SESSIONE NON IN CORSO</div>
      <p class="offline-desc">
        Nessuna sessione di pista è attualmente in corso per questo gran premio.<br>
        Il modulo live timing e telemetria si attiverà non appena la sessione avrà inizio.
      </p>
      <button class="offline-btn" onclick="closeSession()">
        ← TORNA ALL'EVENTO
      </button>
    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════════════
       PRODUCTION LIVE VIEW (Multi-tab Landscape)
       ══════════════════════════════════════════════════════════════ -->
  <div id="liveView">
    <header>
      <div class="hdr-left">
        <button class="back-nav-btn" onclick="closeSession()" title="Chiudi">←</button>
        <div class="live-badge">
          <div class="live-dot"></div>
          LIVE
        </div>
        <div class="hdr-titles">
          <span class="hdr-event" id="hdrEventTitle">MALAYSIAN GRAND PRIX</span>
          <span class="hdr-session" id="hdrSessionName">GARA</span>
        </div>
      </div>

      <div class="hdr-center">
        <div class="lap-indicator" id="hdrLaps">GIRO 1/55</div>
        <div class="clock-indicator" id="hdrClock">08:33:00 UTC</div>
        <div class="speed-multiplier-box">
          <button class="spd-btn active" id="btnSpd1x" onclick="setSimSpeed(1)">1x</button>
          <button class="spd-btn" id="btnSpd2x" onclick="setSimSpeed(2)">2x</button>
          <button class="spd-btn" id="btnSpd5x" onclick="setSimSpeed(5)">5x</button>
        </div>
      </div>

      <div class="hdr-tabs">
        <button class="tab-btn active" data-tab="tab-map-standings" onclick="switchTab('tab-map-standings')">
          🏁 Mappa & Classifica
        </button>
        <button class="tab-btn" data-tab="tab-telemetry" onclick="switchTab('tab-telemetry')">
          🏎️ Telemetria
        </button>
        <button class="tab-btn" data-tab="tab-race-control" onclick="switchTab('tab-race-control')">
          🚩 Race Control
        </button>
      </div>
    </header>

    <div class="content-body">
      <!-- Shared Left Pane: Interactive Track Map Canvas (Pure dark background, NO white cards!) -->
      <div class="track-pane w-35" id="trackPane">
        <div class="canvas-overlay-bar">
          <div class="status-pill green" id="trackFlagBadge">BANDIERA VERDE</div>
          <div class="status-pill" id="trackWeatherBadge">PISTA BAGNATA (SC)</div>
        </div>
        <canvas id="trackCanvas"></canvas>
      </div>

      <!-- Tab 1: Mappa & Classifica (Ingrandita e completamente visibile) -->
      <div class="view-pane active" id="tab-map-standings">
        <div class="standings-table-container">
          <table class="timing-table">
            <thead>
              <tr>
                <th style="width: 26px;">POS</th>
                <th style="width: 20px;">+/-</th>
                <th style="text-align: left; padding-left: 6px;">PILOTA</th>
                <th style="width: 42px;">S1</th>
                <th style="width: 42px;">S2</th>
                <th style="width: 42px;">S3</th>
                <th style="width: 62px;">ULTIMO</th>
                <th style="width: 62px;">MIGLIORE</th>
                <th style="width: 58px; text-align: right; padding-right: 8px;">DISTACCO</th>
                <th style="width: 32px;">GOMMA</th>
                <th style="width: 54px;">AERO</th>
              </tr>
            </thead>
            <tbody id="timingTableBody">
              <!-- Populated dynamically -->
            </tbody>
          </table>
        </div>
      </div>

      <!-- Tab 2: Telemetria (Dual Driver Cockpit Comparison - Adattato per schermo landscape senza overflow) -->
      <div class="view-pane" id="tab-telemetry">
        <div class="telemetry-container">
          <div class="dual-selector-bar">
            <div class="sel-group">
              <span class="sel-label">Pilota 1:</span>
              <select class="drv-dropdown" id="drv1Select" onchange="onDriverSelectChange(1, this.value)">
                ${driverOptionsHtml}
              </select>
            </div>
            <button class="swap-btn" onclick="swapSelectedDrivers()" title="Inverti confronto">⇄</button>
            <div class="sel-group">
              <span class="sel-label">Pilota 2:</span>
              <select class="drv-dropdown" id="drv2Select" onchange="onDriverSelectChange(2, this.value)">
                ${driverOptionsHtml}
              </select>
            </div>
          </div>

          <div class="dual-cockpit-grid">
            <!-- Cockpit 1 -->
            <div class="cockpit-card" id="cockpitCard1">
              <div class="card-top-info">
                <div class="card-drv-name" id="c1Name">
                  <span class="color-dot" id="c1ColorDot" style="background:#3671c6;"></span>
                  <span id="c1Title">#3 VER — Max Verstappen</span>
                </div>
                <div class="card-drv-team" id="c1Team">Red Bull Racing</div>
              </div>
              <div class="speed-gear-cluster">
                <div class="speed-display">
                  <div class="speed-num" id="c1Speed">0</div>
                  <div class="speed-label">KM/H</div>
                </div>
                <div class="gear-display">
                  <div class="gear-num" id="c1Gear">N</div>
                  <div class="gear-label">MARCIA</div>
                </div>
              </div>
              <div class="rpm-container">
                <div class="rpm-meta">
                  <span>REGIME MOTORE</span>
                  <span class="rpm-num" id="c1Rpm">0 RPM</span>
                </div>
                <div class="shift-led-bar" id="c1LedBar"></div>
              </div>
              <div class="pedals-box">
                <div class="pedal-col">
                  <div class="pedal-label"><span>ACC</span><span id="c1ThrVal">0%</span></div>
                  <div class="pedal-track"><div class="pedal-bar-thr" id="c1ThrBar"></div></div>
                </div>
                <div class="pedal-col">
                  <div class="pedal-label"><span>FRENO</span><span id="c1BrkVal">0%</span></div>
                  <div class="pedal-track"><div class="pedal-bar-brk" id="c1BrkBar"></div></div>
                </div>
              </div>
              <div class="regs-2026-box">
                <div class="aero-status-box">
                  <span>ACTIVE AERO (2026)</span>
                  <span class="aero-mode-badge aero-xmode" id="c1AeroBadge">X-MODE</span>
                </div>
                <div class="battery-soc-box">
                  <div class="battery-meta">
                    <span>BATTERIA ERS</span>
                    <span id="c1SocVal">85%</span>
                  </div>
                  <div class="battery-track">
                    <div class="battery-fill" id="c1SocBar" style="width: 85%;"></div>
                  </div>
                </div>
                <div class="boost-box">
                  <span>MODALITÀ SORPASSO (BOOST)</span>
                  <span class="boost-pill boost-standby" id="c1BoostBadge">STANDBY</span>
                </div>
              </div>
              <div class="card-loc-text" id="c1LocText">📍 Rettilineo Principale</div>
            </div>

            <!-- Cockpit 2 -->
            <div class="cockpit-card" id="cockpitCard2">
              <div class="card-top-info">
                <div class="card-drv-name" id="c2Name">
                  <span class="color-dot" id="c2ColorDot" style="background:#e8002d;"></span>
                  <span id="c2Title">#16 LEC — Charles Leclerc</span>
                </div>
                <div class="card-drv-team" id="c2Team">Scuderia Ferrari</div>
              </div>
              <div class="speed-gear-cluster">
                <div class="speed-display">
                  <div class="speed-num" id="c2Speed">0</div>
                  <div class="speed-label">KM/H</div>
                </div>
                <div class="gear-display">
                  <div class="gear-num" id="c2Gear">N</div>
                  <div class="gear-label">MARCIA</div>
                </div>
              </div>
              <div class="rpm-container">
                <div class="rpm-meta">
                  <span>REGIME MOTORE</span>
                  <span class="rpm-num" id="c2Rpm">0 RPM</span>
                </div>
                <div class="shift-led-bar" id="c2LedBar"></div>
              </div>
              <div class="pedals-box">
                <div class="pedal-col">
                  <div class="pedal-label"><span>ACC</span><span id="c2ThrVal">0%</span></div>
                  <div class="pedal-track"><div class="pedal-bar-thr" id="c2ThrBar"></div></div>
                </div>
                <div class="pedal-col">
                  <div class="pedal-label"><span>FRENO</span><span id="c2BrkVal">0%</span></div>
                  <div class="pedal-track"><div class="pedal-bar-brk" id="c2BrkBar"></div></div>
                </div>
              </div>
              <div class="regs-2026-box">
                <div class="aero-status-box">
                  <span>ACTIVE AERO (2026)</span>
                  <span class="aero-mode-badge aero-xmode" id="c2AeroBadge">X-MODE</span>
                </div>
                <div class="battery-soc-box">
                  <div class="battery-meta">
                    <span>BATTERIA ERS</span>
                    <span id="c2SocVal">80%</span>
                  </div>
                  <div class="battery-track">
                    <div class="battery-fill" id="c2SocBar" style="width: 80%;"></div>
                  </div>
                </div>
                <div class="boost-box">
                  <span>MODALITÀ SORPASSO (BOOST)</span>
                  <span class="boost-pill boost-standby" id="c2BoostBadge">STANDBY</span>
                </div>
              </div>
              <div class="card-loc-text" id="c2LocText">📍 Curva 1</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab 3: Race Control -->
      <div class="view-pane" id="tab-race-control">
        <div class="rc-feed-container">
          <div class="rc-status-banner">
            <span class="rc-pill flag-green" id="rcFlagPill">🟢 BANDIERA VERDE</span>
            <span style="font-size: 10px; color: #8b949e; margin-left: auto;" id="rcTotalCount">0 Comunicazioni</span>
          </div>
          <div class="rc-ticker-card" id="rcTickerCard">
            🟡 08:33:00 — Inizio Sessione: Giri di formazione sul bagnato dietro la Safety Car
          </div>
          <div id="rcFeedList" style="display: flex; flex-direction: column; gap: 4px;">
            <!-- Populated dynamically -->
          </div>
        </div>
      </div>
    </div>
  </div>

  <script>
    // ══════════════════════════════════════════════════════════════
    // AUTHENTIC SEPANG DATASETS (Exactly identical to test suite)
    // ══════════════════════════════════════════════════════════════
    const NODES = ${JSON.stringify(compactSepangNodes)};
    const DRIVERS = ${JSON.stringify(sepangDrivers)};
    const DRIVER_KEYFRAMES = ${JSON.stringify(sepangKeyframes)};
    const PIT_STOPS = ${JSON.stringify(sepangPitStops)};
    const RACE_CONTROL_MESSAGES = ${JSON.stringify(sepangRC)};
    const RETIREMENTS = ${JSON.stringify(sepangRetirements)};
    const DRIVER_STINTS = ${JSON.stringify(sepangStints)};
    const DRIVER_LAPS = ${JSON.stringify(sepangLaps)};
    const RACE_EVENTS = ${JSON.stringify(sepangEvents)};
    const SEPANG_INCIDENTS = ${JSON.stringify(sepangIncidents)};
    const LAP_STARTS = ${JSON.stringify(sepangLapStarts)};

    const GRID_ORDER = ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41'];

    const PIT_LANE_NODES = [
      { px: 1103.0, py: 526.0 },
      { px: 1096.0, py: 508.0 },
      { px: 1085.0, py: 484.0 },
      { px: 1070.0, py: 466.0 },
      { px: 1050.0, py: 451.0 },
      { px: 1030.0, py: 446.0 },
      { px: 1005.0, py: 451.0 },
      { px: 980.0,  py: 460.0 },
      { px: 950.0,  py: 471.0 },
      { px: 920.0,  py: 482.0 },
      { px: 885.0,  py: 493.0 },
      { px: 850.0,  py: 504.0 },
      { px: 800.0,  py: 514.5 },
      { px: 750.0,  py: 520.0 },
      { px: 714.0,  py: 523.5 },
      { px: 650.0,  py: 528.0 },
      { px: 550.0,  py: 536.0 },
      { px: 450.0,  py: 544.0 },
      { px: 350.0,  py: 552.5 },
      { px: 250.0,  py: 562.0 },
      { px: 200.0,  py: 570.5 },
      { px: 175.0,  py: 578.0 },
      { px: 155.0,  py: 583.5 },
      { px: 141.0,  py: 585.5 }
    ];

    const PIT_LANE_DISTS = [0];
    for (let i = 0; i < PIT_LANE_NODES.length - 1; i++) {
      const dx = PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px;
      const dy = PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py;
      PIT_LANE_DISTS.push(PIT_LANE_DISTS[i] + Math.sqrt(dx * dx + dy * dy));
    }
    const PIT_LANE_TOTAL_DIST = PIT_LANE_DISTS[PIT_LANE_DISTS.length - 1];

    const SVG_WIDTH = 1280;
    const SVG_HEIGHT = 1057;
    const LAP_DURATION = 95.0;
    const TOTAL_LAPS = 55;
    const RACE_FINISH_SEC = 6435;
    const SESSION_DURATION = 6550;
    const PIT_EXIT_TRACK_SEC = 5.06;
    const PIT_ENTRY_TRACK_SEC = 89.34;

    // ══════════════════════════════════════════════════════════════
    // AUTHENTIC SIMULATION FUNCTIONS (Exact 1:1 match to test suite)
    // ══════════════════════════════════════════════════════════════
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
      if (t < 108.0) return 0; // Formation Lap 1 (Reconnaissance behind SC)
      if (t < 216.9) return 0; // Formation Lap 2 (Reconnaissance behind SC & Grid Lineup)
      const rec = getDriverLapRecord(drvKey, t);
      if (rec) {
        return rec.lap;
      }
      const gap = getDriverGap(drvKey, t);
      const tEff = Math.max(0, t - gap);
      for (let l = 1; l <= TOTAL_LAPS; l++) {
        if (tEff >= LAP_STARTS[l] && tEff < (LAP_STARTS[l + 1] || Infinity)) {
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

      // 1. Is driver inside pit lane?
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

      // 3. Official Race Start (Launch into Turn 1)
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

      // 4. Lap 3 onwards: Driver authentic lap progress
      const rec = getDriverLapRecord(drvKey, t);
      if (rec && rec.dur && rec.dur > 0 && rec.lap >= 3) {
        const inStop = PIT_STOPS.find(p => p.driver === drvKey && p.startSec > rec.startSec && p.startSec <= rec.startSec + rec.dur && t < p.startSec && t >= rec.startSec);
        if (inStop) {
          const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(1, inStop.startSec - rec.startSec)));
          return u * PIT_ENTRY_TRACK_SEC;
        }

        const tS1 = rec.startSec + (rec.s1 || (rec.dur * 0.2222));
        const tS2 = tS1 + (rec.s2 || (rec.dur * 0.2681));
        const S1_TRACK = 21.11;
        const S2_TRACK = 46.58;

        if (t < tS1) {
          const u = Math.max(0, (t - rec.startSec) / Math.max(0.1, tS1 - rec.startSec));
          return u * S1_TRACK;
        } else if (t < tS2) {
          const u = Math.max(0, (t - tS1) / Math.max(0.1, tS2 - tS1));
          return S1_TRACK + u * (S2_TRACK - S1_TRACK);
        } else {
          const u = Math.max(0, (t - tS2) / Math.max(0.1, (rec.startSec + rec.dur) - tS2));
          return S2_TRACK + u * (LAP_DURATION - S2_TRACK);
        }
      }

      const prog = getDriverProgress(drvKey, t);
      return ((prog % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
    }

    function getDriverTire(drvKey, t) {
      const stints = DRIVER_STINTS[drvKey];
      if (!stints || stints.length === 0) return 'I';
      const l = getDriverLap(drvKey, t);
      for (let i = 0; i < stints.length; i++) {
        const s = stints[i];
        if (l >= s.lapStart && l <= s.lapEnd) {
          return s.code;
        }
      }
      return stints[stints.length - 1].code;
    }

    function isDriverInPit(drvKey, t) {
      if (isDriverRetired(drvKey, t)) return true;
      if (t >= SESSION_DURATION) return true;
      return PIT_STOPS.some(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
    }

    function isSafetyCarActive(t) {
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

          const tS1 = l.startSec + l.s1;
          if (t >= tS1 && tS1 >= 216.9 && l.s1 > 10.0 && l.s1 < sessionBestS1) sessionBestS1 = l.s1;
          const tS2 = l.startSec + l.s1 + l.s2;
          if (t >= tS2 && tS2 >= 216.9 && l.s2 > 10.0 && l.s2 < sessionBestS2) sessionBestS2 = l.s2;
          const tEnd = l.startSec + l.dur;
          if (t >= tEnd && tEnd >= 216.9 && l.dur > 50.0) {
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
      if (t < 216.9 || isDriverRetired(drvKey, t)) {
        return {
          curLap: null, lastLap: null,
          s1Str: '-', s1Badge: 'dim',
          s2Str: '-', s2Badge: 'dim',
          s3Str: '-', s3Badge: 'dim',
          lastLapStr: '-', lastLapBadge: 'dim',
          bestLapStr: '-', bestLapBadge: 'dim',
          pBestLap: null
        };
      }

      const dl = DRIVER_LAPS[drvKey] || [];
      const completedLaps = [];
      let curLap = null;

      for (let i = 0; i < dl.length; i++) {
        const l = dl[i];
        if (l.startSec + l.dur <= 216.9) continue;
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

      let s1Val = null, s1Badge = 'dim';
      if (curLap && t >= curLap.startSec + curLap.s1) s1Val = curLap.s1;
      else if (lastLap) s1Val = lastLap.s1;
      if (s1Val != null && s1Val > 10.0) {
        if (s1Val <= sessionBests.sessionBestS1 + 0.0001) s1Badge = 'purple';
        else if (s1Val <= pBestS1 + 0.0001) s1Badge = 'green';
        else s1Badge = 'yellow';
      }

      let s2Val = null, s2Badge = 'dim';
      if (curLap && t >= curLap.startSec + curLap.s1 + curLap.s2) s2Val = curLap.s2;
      else if (lastLap) s2Val = lastLap.s2;
      if (s2Val != null && s2Val > 10.0) {
        if (s2Val <= sessionBests.sessionBestS2 + 0.0001) s2Badge = 'purple';
        else if (s2Val <= pBestS2 + 0.0001) s2Badge = 'green';
        else s2Badge = 'yellow';
      }

      let s3Val = null, s3Badge = 'dim';
      if (lastLap && lastLap.s3 > 10.0) {
        s3Val = lastLap.s3;
        if (s3Val <= sessionBests.sessionBestS3 + 0.0001) s3Badge = 'purple';
        else if (s3Val <= pBestS3 + 0.0001) s3Badge = 'green';
        else s3Badge = 'yellow';
      }

      let lastLapStr = '-', lastLapBadge = 'dim';
      if (lastLap && lastLap.dur > 50.0) {
        lastLapStr = formatLapTime(lastLap.dur);
        if (lastLap.dur <= sessionBests.sessionBestLap + 0.0001) lastLapBadge = 'purple';
        else if (lastLap.dur <= pBestLap + 0.0001) lastLapBadge = 'green';
        else lastLapBadge = 'yellow';
      }

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

    function getDriverVisualState(drvKey, t) {
      const gridIdx = GRID_ORDER.indexOf(drvKey);
      const pos = gridIdx >= 0 ? gridIdx : 21;
      const isRet = isDriverRetired(drvKey, t);
      const inPit = isDriverInPit(drvKey, t);

      // 1. Incidents
      const inc = SEPANG_INCIDENTS.find(i => {
        if (i.driver !== drvKey) return false;
        if (t < i.startSec) return false;
        if (i.type === 'OFF_TRACK_REJOIN') return t <= i.startSec + (i.durationSec || 14.0);
        return true;
      });

      if (inc) {
        const dt = t - inc.startSec;
        const targetPt = { px: inc.targetPx, py: inc.targetPy };

        if (inc.type === 'OFF_TRACK_REJOIN') {
          const tEntry = inc.entryDuration || 3.0;
          const tMan = inc.maneuverDuration || 8.0;
          const tRejoin = inc.rejoinDuration || 3.0;

          if (dt < tEntry) {
            const u = Math.min(1.0, Math.max(0.0, dt / tEntry));
            const s = u * u * (3 - 2 * u);
            const originPt = getTrackPointAtTime(inc.trackSec);
            return {
              px: Number((originPt.px * (1 - s) + targetPt.px * s).toFixed(2)),
              py: Number((originPt.py * (1 - s) + targetPt.py * s).toFixed(2)),
              speed: Math.round(originPt.speed * (1 - s) + 20 * s),
              gear: 1, throttle: 0, brake: 100,
              rpm: Math.round(11000 * (1 - s) + 4000 * s),
              drs: 0, inPit: false, isOffTrack: true, hazard: true, isRetired: false,
              status: 'OFF_TRACK_SLIDE', location: inc.locationName
            };
          } else if (dt < tEntry + tMan) {
            const u = (dt - tEntry) / tMan;
            return {
              px: Number((targetPt.px + Math.sin(u * Math.PI) * 2.0).toFixed(2)),
              py: Number((targetPt.py + Math.cos(u * Math.PI) * 1.5).toFixed(2)),
              speed: 18, gear: 1, throttle: 25, brake: 10, rpm: 4500,
              drs: 0, inPit: false, isOffTrack: true, hazard: true, isRetired: false,
              status: 'ESCAPE_ROAD_MANEUVER', location: inc.locationName + ' — Manovra in Via di Fuga'
            };
          } else {
            const u = Math.min(1.0, Math.max(0.0, (dt - tEntry - tMan) / tRejoin));
            const s = u * u * (3 - 2 * u);
            const rejoinPt = getTrackPointAtTime(inc.trackSec + 2.5);
            return {
              px: Number((targetPt.px * (1 - s) + rejoinPt.px * s).toFixed(2)),
              py: Number((targetPt.py * (1 - s) + rejoinPt.py * s).toFixed(2)),
              speed: Math.round(20 * (1 - s) + 140 * s),
              gear: u < 0.5 ? 2 : 3, throttle: 80, brake: 0,
              rpm: Math.round(5000 * (1 - s) + 10500 * s),
              drs: 0, inPit: false, isOffTrack: false, hazard: true, isRetired: false,
              status: 'REJOINING_TRACK', location: inc.locationName + ' — Rientro in Pista'
            };
          }
        }

        if (inc.type === 'STOPPED_RETIRED') {
          const tTrans = inc.transitionDuration || 3.0;
          const originPt = getTrackPointAtTime(inc.trackSec);
          if (dt < tTrans) {
            const u = Math.min(1.0, Math.max(0.0, dt / tTrans));
            const s = u * u * (3 - 2 * u);
            return {
              px: Number((originPt.px * (1 - s) + targetPt.px * s).toFixed(2)),
              py: Number((originPt.py * (1 - s) + targetPt.py * s).toFixed(2)),
              speed: Math.round(originPt.speed * (1 - s)),
              gear: 0, throttle: 0, brake: 100, rpm: Math.round(9000 * (1 - s)),
              drs: 0, inPit: false, isOffTrack: true, hazard: true, isRetired: true,
              status: 'CRASH_TRANSITION', location: inc.locationName
            };
          } else {
            return {
              px: targetPt.px, py: targetPt.py,
              speed: 0, gear: 0, throttle: 0, brake: 0, rpm: 0,
              drs: 0, inPit: false, isOffTrack: true, hazard: false, isRetired: true,
              status: 'RETIRED_STOPPED', location: inc.locationName
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
          px: Number(pitPx.toFixed(2)), py: Number(pitPy.toFixed(2)),
          speed: inBox ? 0 : 80, gear: inBox ? 1 : 2, rpm: inBox ? 3500 : 4500,
          throttle: inBox ? 0 : 35, brake: inBox ? 1 : 0, drs: 0,
          inPit: true, isOffTrack: false, hazard: false, isRetired: false,
          status: inBox ? 'PIT_BOX_STOP' : 'PIT_LANE_DRIVE',
          location: inBox ? 'Corsia Box — Piazzola Sostituzione Gomme' : 'Corsia Box — Limitatore di Velocità Attivo (80 km/h)'
        };
      }

      // Pit exit merge
      const recentExit = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= p.endSec + 2.0);
      if (recentExit) {
        const u = Math.min(1.0, Math.max(0.0, (t - recentExit.endSec) / 2.0));
        return {
          px: Number(((1 - u) * 141.0 + u * carPt.px).toFixed(2)),
          py: Number(((1 - u) * 585.5 + u * carPt.py).toFixed(2)),
          speed: Math.round(80 * (1 - u) + carPt.speed * u),
          gear: carPt.gear, rpm: carPt.rpm, throttle: carPt.throttle, brake: carPt.brake, drs: 0,
          inPit: false, isOffTrack: false, hazard: false, isRetired: false,
          status: 'PIT_EXIT_MERGE', location: 'Rientro in Pista da Pit Lane verso Curva 1'
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

      let speed, gear, rpm, throttle, brake, drs, status, loc;
      const isSC = isSafetyCarActive(t);
      const isVSC = isVSCActive(t);
      const isFinished = t >= RACE_FINISH_SEC;
      const drv = DRIVERS[drvKey] || {};

      if (t < 195.0) {
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

    // ══════════════════════════════════════════════════════════════
    // CANVOAS & RENDERING ENGINE (NO white background box!)
    // ══════════════════════════════════════════════════════════════
    const canvas = document.getElementById('trackCanvas');
    const ctx = canvas.getContext('2d');

    const bgImage = new Image();
    let bgLoaded = false;
    bgImage.crossOrigin = "anonymous";
    bgImage.onload = () => { bgLoaded = true; drawTrack(); };

    // Priority: track_minimal_layout from Bridge, otherwise local Sepang.svg.webp
    let bridgeCircuitImg = (window.FastestLapBridge && typeof window.FastestLapBridge.getCircuitImageUrl === 'function')
      ? window.FastestLapBridge.getCircuitImageUrl()
      : '';
    bgImage.src = bridgeCircuitImg || 'Sepang.svg.webp';

    // Allow dynamic update if Android loads track minimal outline asynchronously
    window.setCircuitImage = function(url) {
      if (!url) return;
      const img = new Image();
      img.crossOrigin = "anonymous";
      img.onload = function() {
        bgImage.src = url;
        bgLoaded = true;
        drawTrack();
      };
      img.src = url;
    };

    let currentSecond = 0;
    let isPlaying = true;
    let speedMult = 2;
    let focusedDriver = '3';
    let selectedDriver1 = '3';
    let selectedDriver2 = '16';
    let currentTab = 'tab-map-standings';
    let lastTableUpdateSec = -1;
    let lastEventText = '';
    const carScreenPositions = {};

    function resizeCanvas() {
      const pane = document.getElementById('trackPane');
      if (!pane) return;
      canvas.width = pane.clientWidth;
      canvas.height = pane.clientHeight;
      drawTrack();
    }
    window.addEventListener('resize', resizeCanvas);

    function getViewportBounds() {
      const pad = 16;
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

    // Canvas track drawing: pure dark background, ZERO background card/rectangle!
    function drawTrack() {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const bounds = getViewportBounds();

      // 1. Draw track image if loaded (NO WHITE BOX!), else clean centerline
      if (bgLoaded && bgImage) {
        ctx.save();
        ctx.drawImage(bgImage, bounds.ox, bounds.oy, bounds.drawW, bounds.drawH);
        ctx.restore();
      } else {
        ctx.save();
        ctx.strokeStyle = '#21262d';
        ctx.lineWidth = 10;
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

      const activeKey = (currentTab === 'tab-telemetry') ? selectedDriver1 : focusedDriver;
      const driverKeys = Object.keys(DRIVERS);

      const driverProgressMap = {};
      driverKeys.forEach(k => {
        driverProgressMap[k] = getDriverProgress(k, currentSecond);
      });

      // 2. Draw Pit Lane Path & Pit Boxes
      ctx.save();
      ctx.strokeStyle = '#388bfd';
      ctx.lineWidth = 2.0;
      ctx.lineCap = 'round';
      ctx.setLineDash([4, 3]);
      ctx.beginPath();
      for (let pi = 0; pi < PIT_LANE_NODES.length; pi++) {
        const pscr = pxToScreen(PIT_LANE_NODES[pi].px, PIT_LANE_NODES[pi].py);
        if (pi === 0) ctx.moveTo(pscr.x, pscr.y);
        else ctx.lineTo(pscr.x, pscr.y);
      }
      ctx.stroke();
      ctx.setLineDash([]);

      const pbScr = pxToScreen(714, 523.5);
      ctx.fillStyle = 'rgba(56, 139, 253, 0.12)';
      ctx.fillRect(pbScr.x - 42, pbScr.y - 6, 84, 12);
      ctx.strokeStyle = '#58a6ff';
      ctx.lineWidth = 1;
      ctx.strokeRect(pbScr.x - 42, pbScr.y - 6, 84, 12);
      ctx.fillStyle = '#58a6ff';
      ctx.font = 'bold 7.5px ui-monospace, sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('PIT BOX 80', pbScr.x, pbScr.y - 8);
      ctx.restore();

      // 3. Starting Gantry (5 Red Lights during Standing Start)
      if (currentSecond >= 210 && currentSecond <= 221) {
        ctx.save();
        const gCenter = pxToScreen(714, 485);
        const elapsedGantry = currentSecond - 210;
        const lightsOn = currentSecond < 216.9 ? Math.min(5, Math.floor(elapsedGantry / 1.2) + 1) : 0;
        ctx.fillStyle = '#0d1117';
        ctx.strokeStyle = '#30363d';
        ctx.lineWidth = 1.2;
        if (ctx.roundRect) ctx.roundRect(gCenter.x - 55, gCenter.y - 12, 110, 24, 5);
        else ctx.rect(gCenter.x - 55, gCenter.y - 12, 110, 24);
        ctx.fill();
        ctx.stroke();

        for (let li = 0; li < 5; li++) {
          const lx = (gCenter.x - 55) + 15 + li * 20;
          ctx.beginPath();
          ctx.arc(lx, gCenter.y, 5.0, 0, Math.PI * 2);
          if (li < lightsOn) {
            ctx.fillStyle = '#ff1801';
            ctx.shadowColor = '#ff1801';
            ctx.shadowBlur = 8;
          } else {
            ctx.fillStyle = '#21262d';
            ctx.shadowBlur = 0;
          }
          ctx.fill();
        }
        ctx.restore();
      }

      // 4. Draw Drivers
      driverKeys.forEach(drvKey => {
        const drv = DRIVERS[drvKey];
        if (!drv) return;

        const myProg = driverProgressMap[drvKey];
        const state = getDriverVisualState(drvKey, currentSecond);
        let carScr = pxToScreen(state.px, state.py);

        // Lateral separation during battles
        if (!state.inPit && !state.isOffTrack && !state.isRetired && currentSecond > 224.0) {
          let lateralOffset = 0;
          for (const otherKey of driverKeys) {
            if (otherKey === drvKey) continue;
            const diff = Math.abs(myProg - driverProgressMap[otherKey]);
            if (diff < 1.0) {
              const side = (myProg > driverProgressMap[otherKey]) ? 1 : -1;
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

        ctx.save();
        const baseRadius = isCurrentActive ? 8.5 : 6.5;

        // Glow
        ctx.beginPath();
        ctx.arc(carScr.x, carScr.y, baseRadius + (isCurrentActive ? 4 : (state.isOffTrack ? 3 : 1.5)), 0, Math.PI * 2);
        ctx.fillStyle = isCurrentActive 
          ? 'rgba(88, 166, 255, 0.45)' 
          : (state.inPit ? 'rgba(210, 153, 34, 0.3)' : (state.isOffTrack ? 'rgba(255, 170, 0, 0.5)' : 'rgba(0, 0, 0, 0.4)'));
        ctx.fill();

        // Dot
        ctx.beginPath();
        ctx.arc(carScr.x, carScr.y, baseRadius, 0, Math.PI * 2);
        ctx.fillStyle = state.inPit ? '#d29922' : (state.isOffTrack ? '#ffaa00' : drv.color);
        ctx.fill();
        ctx.lineWidth = isCurrentActive ? 2.5 : 1.5;
        ctx.strokeStyle = isCurrentActive ? '#ffffff' : (state.isOffTrack ? '#ff3b30' : '#0d1117');
        ctx.stroke();

        // Number
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold ' + (isCurrentActive ? '9px' : '7.5px') + ' ui-monospace, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(drv.number, carScr.x, carScr.y);

        // Code label
        ctx.font = 'bold 8.5px -apple-system, sans-serif';
        ctx.fillStyle = isCurrentActive ? '#ffffff' : '#c9d1d9';
        let label = drv.code;
        if (state.inPit) label += ' [PIT]';
        else if (state.isOffTrack) label += ' [OFF]';
        else if (state.status === 'REJOINING_TRACK') label += ' [REJOIN]';
        ctx.fillText(label, carScr.x, carScr.y - baseRadius - 4);
        ctx.restore();
      });

      // 5. Safety Car
      const scActive = isSafetyCarActive(currentSecond);
      let scScr;
      if (scActive) {
        const leaderKey = driverKeys.reduce((best, k) => (driverProgressMap[k] > (driverProgressMap[best] || -999) ? k : best), '3');
        const scSec = (getDriverTrackSec(leaderKey, currentSecond) + 2.5) % LAP_DURATION;
        const scPt = getTrackPointAtTime(scSec);
        scScr = pxToScreen(scPt.px, scPt.py);
      } else {
        scScr = pxToScreen(155, 578);
      }

      ctx.save();
      const isBeaconFlash = scActive && (Math.floor(Date.now() / 200) % 2 === 0);
      const scRadius = scActive ? 9.5 : 6.0;

      ctx.beginPath();
      ctx.arc(scScr.x, scScr.y, scRadius, 0, Math.PI * 2);
      ctx.fillStyle = scActive ? '#ff9800' : '#484f58';
      ctx.fill();
      ctx.lineWidth = 1.5;
      ctx.strokeStyle = scActive ? '#ffffff' : '#8b949e';
      ctx.stroke();

      if (scActive) {
        ctx.fillStyle = isBeaconFlash ? '#ffffff' : '#ffeb3b';
        ctx.beginPath();
        ctx.arc(scScr.x, scScr.y - 6, 2.5, 0, Math.PI * 2);
        ctx.fill();
      }

      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 7.5px ui-monospace, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText('SC', scScr.x, scScr.y);
      ctx.restore();

      // 6. Finish Line
      const s0 = pxToScreen(NODES[0][1], NODES[0][2]);
      ctx.save();
      ctx.fillStyle = '#238636';
      ctx.beginPath();
      ctx.arc(s0.x, s0.y, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // ══════════════════════════════════════════════════════════════
    // TIMING TOWER / STANDINGS TABLE (Ingrandita & Visibile)
    // ══════════════════════════════════════════════════════════════
    function updateTimingTower() {
      const tbody = document.getElementById('timingTableBody');
      if (!tbody) return;

      const driverKeys = Object.keys(DRIVERS);
      const sessionBests = getSessionBests(currentSecond);

      const ranked = driverKeys.map(k => {
        const d = DRIVERS[k];
        const prog = getDriverProgress(k, currentSecond);
        const gridIndex = GRID_ORDER.indexOf(k) + 1;
        const stats = getDriverTimingStats(k, currentSecond, sessionBests);
        const state = getDriverVisualState(k, currentSecond);

        return {
          key: k,
          driver: d,
          totalProgress: prog,
          inPit: state.inPit,
          isRetired: state.isRetired,
          retiredLap: RETIREMENTS[k]?.lap,
          tire: getDriverTire(k, currentSecond),
          gridPos: gridIndex,
          stats: stats,
          state: state
        };
      }).sort((a, b) => b.totalProgress - a.totalProgress);

      const leaderProgress = ranked[0].totalProgress;
      const activeKey = (currentTab === 'tab-telemetry') ? selectedDriver1 : focusedDriver;

      let html = '';
      ranked.forEach((r, idx) => {
        const d = r.driver;
        const isCurrentActive = r.key === activeKey;
        const currentPos = idx + 1;
        const posDelta = r.gridPos - currentPos;

        let deltaHtml = '<span style="color: #8b949e;">-</span>';
        if (!r.isRetired && posDelta > 0) {
          deltaHtml = '<span style="color: #2ea043; font-weight: 800;">▲' + posDelta + '</span>';
        } else if (!r.isRetired && posDelta < 0) {
          deltaHtml = '<span style="color: #da3633; font-weight: 800;">▼' + Math.abs(posDelta) + '</span>';
        }

        let gap;
        if (r.isRetired) {
          gap = '<span style="color:#da3633; font-weight:800;">DNF (L' + r.retiredLap + ')</span>';
        } else if (idx === 0) {
          gap = 'LEADER';
        } else {
          gap = '+' + (leaderProgress - r.totalProgress).toFixed(1) + 's';
        }

        const tireColor = r.tire === 'I' ? '#3fb950' : (r.tire === 'S' ? '#da3633' : (r.tire === 'M' ? '#e3b341' : (r.tire === 'W' ? '#58a6ff' : '#f0f6fc')));
        const aeroBadge = (r.state.drs === 1)
          ? '<span class="aero-badge aero-z">Z-MODE</span>'
          : '<span class="aero-badge aero-x">X-MODE</span>';

        html += '<tr class="timing-row ' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
          + '<td class="pos-cell">' + currentPos + '</td>'
          + '<td class="delta-cell">' + deltaHtml + '</td>'
          + '<td class="driver-cell"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span>#' + d.number + ' ' + d.code + '</span></span></td>'
          + '<td>' + renderBadge(r.stats.s1Str, r.stats.s1Badge) + '</td>'
          + '<td>' + renderBadge(r.stats.s2Str, r.stats.s2Badge) + '</td>'
          + '<td>' + renderBadge(r.stats.s3Str, r.stats.s3Badge) + '</td>'
          + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
          + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
          + '<td class="gap-cell" style="color: ' + (idx === 0 ? '#e3b341' : '#c9d1d9') + ';">' + gap + '</td>'
          + '<td><span style="color:' + tireColor + '; font-weight:800; font-size:10px;">[' + r.tire + ']</span></td>'
          + '<td>' + aeroBadge + '</td>'
          + '</tr>';
      });
      tbody.innerHTML = html;
    }

    function onDriverRowClick(drvKey) {
      focusedDriver = drvKey;
      selectedDriver1 = drvKey;
      const s1 = document.getElementById('drv1Select');
      if (s1) s1.value = drvKey;
      updateCockpit(1, selectedDriver1);
      updateTimingTower();
      drawTrack();
    }

    // ══════════════════════════════════════════════════════════════
    // DUAL COCKPIT TELEMETRY (Adattato per Landscape senza overflow)
    // ══════════════════════════════════════════════════════════════
    function buildLedBars() {
      ['c1LedBar', 'c2LedBar'].forEach(id => {
        const bar = document.getElementById(id);
        if (!bar) return;
        bar.innerHTML = '';
        for (let i = 0; i < 15; i++) {
          const led = document.createElement('div');
          led.className = 'shift-led';
          bar.appendChild(led);
        }
      });
    }

    function updateCockpit(idx, drvKey) {
      const drv = DRIVERS[drvKey];
      if (!drv) return;

      const state = getDriverVisualState(drvKey, currentSecond);
      const tire = getDriverTire(drvKey, currentSecond);
      const p = 'c' + idx;

      // Driver info
      const dot = document.getElementById(p + 'ColorDot');
      if (dot) dot.style.background = drv.color;
      const title = document.getElementById(p + 'Title');
      if (title) title.textContent = '#' + drv.number + ' ' + drv.code + ' — ' + drv.firstName + ' ' + drv.lastName;
      const team = document.getElementById(p + 'Team');
      if (team) team.textContent = drv.team + ' [' + tire + ']';

      // Speed & Gear (Strictly integer speeds)
      const spdEl = document.getElementById(p + 'Speed');
      if (spdEl) spdEl.textContent = Math.round(state.speed);
      const gearEl = document.getElementById(p + 'Gear');
      if (gearEl) gearEl.textContent = state.isRetired ? 'DNF' : (state.gear === 0 ? 'N' : state.gear);

      // RPM
      const rpmEl = document.getElementById(p + 'Rpm');
      if (rpmEl) rpmEl.textContent = Math.round(state.rpm).toLocaleString() + ' RPM';

      // Shift LEDs
      const leds = document.querySelectorAll('#' + p + 'LedBar .shift-led');
      const rpmFrac = Math.max(0, Math.min(1, (state.rpm - 9500) / 3000));
      const activeLeds = Math.round(rpmFrac * leds.length);
      leds.forEach((led, i) => {
        led.className = 'shift-led';
        if (i < activeLeds) {
          if (i < 5) led.classList.add('green-on');
          else if (i < 10) led.classList.add('yellow-on');
          else led.classList.add('red-on');
        }
      });

      // Throttle & Brake
      const thrVal = document.getElementById(p + 'ThrVal');
      if (thrVal) thrVal.textContent = Math.round(state.throttle) + '%';
      const thrBar = document.getElementById(p + 'ThrBar');
      if (thrBar) thrBar.style.width = Math.min(100, Math.max(0, state.throttle)) + '%';

      const brkVal = document.getElementById(p + 'BrkVal');
      const brkPercent = state.brake === 1 ? 100 : (state.brake > 1 ? Math.min(100, state.brake) : 0);
      if (brkVal) brkVal.textContent = Math.round(brkPercent) + '%';
      const brkBar = document.getElementById(p + 'BrkBar');
      if (brkBar) brkBar.style.width = brkPercent + '%';

      // Active Aero (2026 Regs)
      const aeroBadge = document.getElementById(p + 'AeroBadge');
      if (aeroBadge) {
        if (state.drs === 1) {
          aeroBadge.className = 'aero-mode-badge aero-zmode';
          aeroBadge.textContent = 'Z-MODE (LOW DRAG)';
        } else {
          aeroBadge.className = 'aero-mode-badge aero-xmode';
          aeroBadge.textContent = 'X-MODE (HIGH DF)';
        }
      }

      // Battery SoC (2026 Regs) - NO manual override text on battery!
      const soc = Math.round(68 + 24 * Math.sin(currentSecond * 0.15 + parseInt(drvKey, 10)) - (state.throttle > 80 ? 7 : -4));
      const batterySoc = Math.min(100, Math.max(15, soc));
      const socVal = document.getElementById(p + 'SocVal');
      if (socVal) socVal.textContent = batterySoc + '%';
      const socBar = document.getElementById(p + 'SocBar');
      if (socBar) socBar.style.width = batterySoc + '%';

      // Boost / Modalità Sorpasso (within 1.0s of car ahead)
      const boostBadge = document.getElementById(p + 'BoostBadge');
      if (boostBadge) {
        const gapVal = getDriverGap(drvKey, currentSecond);
        if (state.drs === 1 && state.throttle > 90) {
          boostBadge.className = 'boost-pill boost-active';
          boostBadge.textContent = '⚡ BOOST ATTIVO';
        } else if (gapVal > 0 && (gapVal % 1.2) <= 1.0) {
          boostBadge.className = 'boost-pill boost-avail';
          boostBadge.textContent = '🟢 DISPONIBILE (<1.0s)';
        } else {
          boostBadge.className = 'boost-pill boost-standby';
          boostBadge.textContent = 'STANDBY';
        }
      }

      // Location
      const locEl = document.getElementById(p + 'LocText');
      if (locEl) locEl.textContent = state.isRetired ? ('🛑 ' + state.location) : (state.isOffTrack ? ('⚠️ ' + state.location) : state.location);
    }

    function onDriverSelectChange(idx, val) {
      if (idx === 1) selectedDriver1 = val;
      else selectedDriver2 = val;
      updateCockpit(idx, val);
      drawTrack();
    }

    function swapSelectedDrivers() {
      const tmp = selectedDriver1;
      selectedDriver1 = selectedDriver2;
      selectedDriver2 = tmp;
      document.getElementById('drv1Select').value = selectedDriver1;
      document.getElementById('drv2Select').value = selectedDriver2;
      updateCockpit(1, selectedDriver1);
      updateCockpit(2, selectedDriver2);
      drawTrack();
    }

    // ══════════════════════════════════════════════════════════════
    // RACE CONTROL & EVENT TICKER
    // ══════════════════════════════════════════════════════════════
    function updateRaceControl() {
      const pill = document.getElementById('rcFlagPill');
      const trackBadge = document.getElementById('trackFlagBadge');
      const trackWeather = document.getElementById('trackWeatherBadge');

      if (currentSecond >= 6435) {
        if (pill) { pill.className = 'rc-pill flag-chequered'; pill.textContent = '🏁 BANDIERA A SCACCHI'; }
        if (trackBadge) { trackBadge.className = 'status-pill'; trackBadge.textContent = 'BANDIERA A SCACCHI'; }
      } else if (currentSecond < 205.0) {
        if (pill) { pill.className = 'rc-pill flag-sc'; pill.textContent = '🟠 GIRO DI FORMAZIONE (SC)'; }
        if (trackBadge) { trackBadge.className = 'status-pill sc'; trackBadge.textContent = 'FORMAZIONE (SC)'; }
        if (trackWeather) { trackWeather.textContent = 'PISTA BAGNATA'; }
      } else if (currentSecond < 216.9) {
        if (pill) { pill.className = 'rc-pill flag-yellow'; pill.textContent = '🔴 SCHIERAMENTO (SEMAFORI ROSSI)'; }
        if (trackBadge) { trackBadge.className = 'status-pill yellow'; trackBadge.textContent = 'SCHIERAMENTO'; }
      } else if (isSafetyCarActive(currentSecond)) {
        if (pill) { pill.className = 'rc-pill flag-sc'; pill.textContent = '🟠 SAFETY CAR DEPLOYED'; }
        if (trackBadge) { trackBadge.className = 'status-pill sc'; trackBadge.textContent = 'SAFETY CAR'; }
      } else if (isVSCActive(currentSecond)) {
        if (pill) { pill.className = 'rc-pill flag-vsc'; pill.textContent = '🟡 VSC DEPLOYED'; }
        if (trackBadge) { trackBadge.className = 'status-pill yellow'; trackBadge.textContent = 'VSC DELTA'; }
      } else {
        if (pill) { pill.className = 'rc-pill flag-green'; pill.textContent = '🟢 BANDIERA VERDE (GARA)'; }
        if (trackBadge) { trackBadge.className = 'status-pill green'; trackBadge.textContent = 'BANDIERA VERDE'; }
      }

      const activeMsgs = RACE_CONTROL_MESSAGES.filter(m => m.timeSec <= Math.floor(currentSecond));
      const totalCount = document.getElementById('rcTotalCount');
      if (totalCount) totalCount.textContent = activeMsgs.length + ' / ' + RACE_CONTROL_MESSAGES.length + ' Messaggi';

      const feedList = document.getElementById('rcFeedList');
      if (feedList) {
        const recent = activeMsgs.slice(-10).reverse();
        let html = '';
        recent.forEach(m => {
          let cardClass = 'rc-msg-card';
          if (m.category === 'Flag' || (m.flag && m.flag.includes('Yellow'))) cardClass += ' yellow';
          else if (m.category === 'SafetyCar') cardClass += ' sc';
          else if (m.category === 'Clear' || (m.flag && m.flag.includes('Green'))) cardClass += ' green';

          const timeStr = m.date ? m.date.substring(11, 19) : '08:33';
          const lapStr = m.lap_number ? ('Giro ' + m.lap_number) : 'Formazione';

          html += '<div class="' + cardClass + '" onclick="jumpToRace(' + Math.max(0, m.timeSec) + ')">'
            + '<div class="rc-msg-meta"><span>' + timeStr + ' (' + lapStr + ')</span><span>' + (m.category || 'FIA') + '</span></div>'
            + '<div class="rc-msg-text">' + m.message + '</div>'
            + '</div>';
        });
        feedList.innerHTML = html || '<div style="font-size:11px; color:#8b949e; padding:6px;">In attesa di comunicazioni ufficiali.</div>';
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
        const card = document.getElementById('rcTickerCard');
        if (card) {
          card.textContent = latest.text;
          card.style.color = '#ff8000';
          setTimeout(() => { card.style.color = '#f0f6fc'; }, 400);
        }
      }
    }

    function jumpToRace(sec) {
      currentSecond = sec;
      updateHUD();
      drawTrack();
    }
    window.jumpToRace = jumpToRace;

    // ══════════════════════════════════════════════════════════════
    // HUD & MASTER CLOCK
    // ══════════════════════════════════════════════════════════════
    function updateHUD() {
      const baseSec = 8 * 3600 + 33 * 60 + Math.floor(currentSecond);
      const hh = String(Math.floor(baseSec / 3600) % 24).padStart(2, '0');
      const mm = String(Math.floor((baseSec % 3600) / 60)).padStart(2, '0');
      const ss = String(baseSec % 60).padStart(2, '0');
      
      const curLap = getDriverLap(focusedDriver, currentSecond);
      let lapText = 'GIRO ' + curLap + '/55';
      if (currentSecond < 108.0) lapText = 'GIRO FORM. 1 (SC)';
      else if (currentSecond < 205.0) lapText = 'GIRO FORM. 2 (SC)';
      else if (currentSecond < 216.9) lapText = 'SCHIERAMENTO';
      else if (currentSecond >= 6435) lapText = '🏁 TRAGUARDO';

      document.getElementById('hdrLaps').textContent = lapText;
      document.getElementById('hdrClock').textContent = hh + ':' + mm + ':' + ss + ' UTC';

      const secFloor = Math.floor(currentSecond);
      if (secFloor !== lastTableUpdateSec) {
        lastTableUpdateSec = secFloor;
        updateTimingTower();
        updateRaceControl();
        updateEventTicker();
      }

      if (currentTab === 'tab-telemetry') {
        updateCockpit(1, selectedDriver1);
        updateCockpit(2, selectedDriver2);
      }
    }

    // ══════════════════════════════════════════════════════════════
    // TAB SWITCHING & RESIZING
    // ══════════════════════════════════════════════════════════════
    function switchTab(tabId) {
      currentTab = tabId;
      document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabId);
      });
      document.querySelectorAll('.view-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === tabId);
      });

      const trackPane = document.getElementById('trackPane');
      if (tabId === 'tab-telemetry') {
        trackPane.className = 'track-pane w-32';
      } else {
        trackPane.className = 'track-pane w-35';
      }

      resizeCanvas();
      updateHUD();
    }
    window.switchTab = switchTab;

    function setSimSpeed(mult) {
      speedMult = mult;
      ['btnSpd1x', 'btnSpd2x', 'btnSpd5x'].forEach(id => {
        const btn = document.getElementById(id);
        if (btn) btn.classList.toggle('active', id === ('btnSpd' + mult + 'x'));
      });
    }
    window.setSimSpeed = setSimSpeed;

    function closeSession() {
      if (window.FastestLapBridge && typeof window.FastestLapBridge.closeLive === 'function') {
        window.FastestLapBridge.closeLive();
      } else {
        window.history.back();
      }
    }
    window.closeSession = closeSession;

    // Canvas click on car
    canvas.addEventListener('click', (e) => {
      const rect = canvas.getBoundingClientRect();
      const mx = e.clientX - rect.left;
      const my = e.clientY - rect.top;

      for (const [key, item] of Object.entries(carScreenPositions)) {
        if (Math.hypot(mx - item.x, my - item.y) <= 14) {
          onDriverRowClick(key);
          break;
        }
      }
    });

    // ══════════════════════════════════════════════════════════════
    // TICK LOOP (60 FPS)
    // ══════════════════════════════════════════════════════════════
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
          }
        }

        drawTrack();
        updateHUD();
      } catch (err) {
        console.error('Animation error:', err);
      }
      requestAnimationFrame(tick);
    }

    // Initialize UI and start animation
    document.addEventListener('DOMContentLoaded', () => {
      // Check session status from bridge
      const isLiveSession = (window.FastestLapBridge && typeof window.FastestLapBridge.isSessionLive === 'function')
        ? window.FastestLapBridge.isSessionLive()
        : true;

      const eventTitle = (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function')
        ? window.FastestLapBridge.getEventTitle()
        : 'FORMULA 1 PETRONAS MALAYSIAN GP';

      const sessionName = (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionName === 'function')
        ? window.FastestLapBridge.getSessionName()
        : 'GARA (SIMULAZIONE)';

      document.getElementById('hdrEventTitle').textContent = eventTitle.toUpperCase();
      document.getElementById('hdrSessionName').textContent = sessionName.toUpperCase();
      document.getElementById('offlineEventTitle').textContent = eventTitle.toUpperCase();
      document.getElementById('offlineSessionName').textContent = sessionName.toUpperCase();

      if (!isLiveSession) {
        document.getElementById('offlineView').style.display = 'flex';
        document.getElementById('liveView').style.display = 'none';
        return;
      } else {
        document.getElementById('offlineView').style.display = 'none';
        document.getElementById('liveView').style.display = 'flex';
      }

      buildLedBars();
      resizeCanvas();
      updateTimingTower();
      updateCockpit(1, selectedDriver1);
      updateCockpit(2, selectedDriver2);
      requestAnimationFrame(tick);
    });
  </script>
</body>
</html>
`;

// Write to asset destination
fs.writeFileSync(path.join(assetsDir, 'track_map.html'), htmlContent, 'utf-8');
console.log('Successfully written production track_map.html! Size:', htmlContent.length);

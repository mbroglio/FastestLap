const fs = require('fs');
const path = require('path');

const dataDir = path.join(__dirname, '..', 'data');
const testSuiteDir = path.join(__dirname, '..');
const assetsDir = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'live_timing');

// Ensure destination directories exist
[
  path.join(assetsDir, 'css'),
  path.join(assetsDir, 'js'),
  path.join(assetsDir, 'simulations'),
  path.join(assetsDir, 'simulations', 'sepang'),
  path.join(assetsDir, 'simulations', 'baku'),
  path.join(testSuiteDir, 'css'),
  path.join(testSuiteDir, 'js'),
  path.join(testSuiteDir, 'simulations'),
  path.join(testSuiteDir, 'simulations', 'sepang'),
  path.join(testSuiteDir, 'simulations', 'baku')
].forEach(dir => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

// 1. Load authentic Sepang data
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
const sepangPitNodes = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_pit_lane_nodes.json'), 'utf-8'));

const rm = require('../src/race_model.js');
const sepangLapStarts = rm.LAP_STARTS;

const { generateSepangFpDataset, generateSepangQualifyingDataset } = require('../src/generate_sepang_sessions.js');
const sepangFpData = generateSepangFpDataset();
const sepangQualyData = generateSepangQualifyingDataset();

// 2. Load authentic Baku data
const bakuNodes = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_exact_track_full.json'), 'utf-8'));
const compactBakuNodes = bakuNodes.map(n => [
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

const bakuDrivers = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_drivers.json'), 'utf-8'));
const bakuKeyframes = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_keyframes.json'), 'utf-8'));
const bakuPitStops = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_pit_stops.json'), 'utf-8'));
const bakuRC = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_race_control_messages.json'), 'utf-8'));
const bakuRetirements = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_retirements.json'), 'utf-8'));
const bakuStints = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_driver_stints.json'), 'utf-8'));
const bakuLaps = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_driver_laps.json'), 'utf-8'));
const bakuEvents = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_race_events.json'), 'utf-8'));
const bakuIncidents = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_incidents.json'), 'utf-8'));
const bakuPitNodes = JSON.parse(fs.readFileSync(path.join(dataDir, 'baku_pit_lane_nodes.json'), 'utf-8'));

const bakuLapStarts = [0];
const rLaps = bakuLaps['63'] || [];
for (let l = 1; l <= 51; l++) {
  const match = rLaps.find(x => x.lap === l);
  bakuLapStarts[l] = match ? match.startSec : (l - 1) * 108.6;
}

console.log('Loaded Sepang data: ' + compactSepangNodes.length + ' nodes, ' + Object.keys(sepangDrivers).length + ' drivers.');
console.log('Loaded Baku data: ' + compactBakuNodes.length + ' nodes, ' + Object.keys(bakuDrivers).length + ' drivers.');

// Driver dropdown options (defaults to Sepang / 2026 grid)
const driverOptionsHtml = Object.entries(sepangDrivers).map(([num, d]) => {
  return `<option value="${num}">#${num} ${d.code} — ${d.firstName} ${d.lastName} (${d.team})</option>`;
}).join('\n                ');

// ══════════════════════════════════════════════════════════════
// 1. MODULAR CSS CONTENT (css/live_timing.css)
// ══════════════════════════════════════════════════════════════
const cssContent = `/**
 * FastestLap — Live Timing & Telemetry Stylesheet
 * Separated modular CSS for responsive F1 timing and telemetry views
 */

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
  --hdr-row1-h: 28px;
  --hdr-row2-h: 22px;
  --bottom-nav-h: 38px;
  --safe-top: 0px;
  --safe-left: max(6px, env(safe-area-inset-left));
  --safe-right: max(6px, env(safe-area-inset-right));
  --safe-bottom: 0px;
}

* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  -webkit-tap-highlight-color: transparent;
}

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
   PRODUCTION LIVE VIEW
   ══════════════════════════════════════════════════════════════ */
#liveView {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: var(--bg-dark);
}

/* Header Container with Safe-Area insets */
header {
  background: #11141a;
  border-bottom: 1px solid #21262d;
  padding-top: var(--safe-top);
  padding-left: var(--safe-left);
  padding-right: var(--safe-right);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

/* Row 1: Header Brand, Live Title, Sim Switcher and Multipliers */
.hdr-top-row {
  height: var(--hdr-row1-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.hdr-left {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  flex: 1;
}

.back-nav-btn {
  background: #21262d;
  border: 1px solid #30363d;
  color: #f0f6fc;
  width: 24px;
  height: 24px;
  border-radius: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
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
  font-size: 9px;
  font-weight: 800;
  padding: 1.5px 6px;
  border-radius: 3px;
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
  50% { opacity: 0.35; transform: scale(0.75); }
}

.hdr-event {
  font-size: 11.5px;
  font-weight: 800;
  color: #f0f6fc;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hdr-right-controls {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.test-switch-box {
  display: inline-flex;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}
.test-btn {
  background: transparent;
  border: none;
  color: #8b949e;
  font-size: 9px;
  font-weight: 800;
  padding: 2.5px 6px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.test-btn.active {
  background: #238636;
  color: #fff;
}

.speed-multiplier-box {
  display: inline-flex;
  background: #1c2128;
  border: 1px solid #30363d;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}
.spd-btn {
  background: transparent;
  border: none;
  color: #8b949e;
  font-size: 9px;
  font-weight: 800;
  padding: 2px 6px;
  cursor: pointer;
}
.spd-btn.active {
  background: #388bfd;
  color: #fff;
}

/* ══════════════════════════════════════════════════════════════
   ROW 2: PERSISTENT SESSION STATUS BAR (ABOVE ALL PAGES)
   ══════════════════════════════════════════════════════════════ */
.session-status-bar {
  height: var(--hdr-row2-h);
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #1c2128;
  padding: 1px 0;
  overflow-x: auto;
  overflow-y: hidden;
}
.session-status-bar::-webkit-scrollbar { display: none; }

.status-bar-flag-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 1.5px 8px;
  border-radius: 3px;
  font-size: 9.5px;
  font-weight: 800;
  letter-spacing: 0.4px;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all 0.2s ease;
}
.flag-green { background: #1b4728; border: 1px solid #238636; color: #3fb950; }
.flag-yellow { background: #3d3000; border: 1px solid #d29922; color: #e3b341; }
.flag-sc { background: #4a2800; border: 1px solid #f0883e; color: #ffa657; }
.flag-vsc { background: #3d3000; border: 1px solid #d29922; color: #e3b341; }
.flag-red { background: #490202; border: 1px solid #da3633; color: #f85149; }
.flag-chequered { background: #21262d; border: 1px solid #f0f6fc; color: #f0f6fc; }

.status-bar-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 9.5px;
  white-space: nowrap;
  flex-shrink: 0;
}
.status-bar-item .lbl {
  color: #8b949e;
  font-weight: 700;
  font-size: 9px;
}
.status-bar-item .val {
  font-weight: 800;
  color: #f0f6fc;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}
.status-bar-item.clock .val {
  color: var(--cyan-f1);
}

/* ══════════════════════════════════════════════════════════════
   MAIN CONTENT CONTAINER (SWAPPABLE VIEWS)
   ══════════════════════════════════════════════════════════════ */
.views-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
  background: #000000;
}

.main-view {
  display: none;
  width: 100%;
  height: 100%;
  overflow: hidden;
}
.main-view.active {
  display: flex;
}

/* ══════════════════════════════════════════════════════════════
   VIEW 1: CLASSIFICA LIVE (RESPONSIVE IN LANDSCAPE E PORTRAIT)
   ══════════════════════════════════════════════════════════════ */
.full-standings-wrapper {
  flex: 1;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: auto;
  background: #090d13;
}
.full-standings-wrapper::-webkit-scrollbar { width: 5px; height: 5px; }
.full-standings-wrapper::-webkit-scrollbar-thumb { background: #30363d; border-radius: 3px; }

.full-standings-table {
  width: 100%;
  min-width: 100%;
  border-collapse: collapse;
  font-size: 11px;
  table-layout: auto;
}
.full-standings-table th {
  background: #161b22;
  color: #8b949e;
  font-weight: 800;
  padding: 8px 6px;
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
.full-standings-table td {
  padding: 8px 6px;
  border-bottom: 1px solid #161b22;
  white-space: nowrap;
  text-align: center;
}
.full-standings-table tr {
  cursor: pointer;
  transition: background 0.12s;
}
.full-standings-table tr:hover { background: #161b22; }
.full-standings-table tr.active {
  background: rgba(88, 166, 255, 0.18);
  border-left: 3px solid #58a6ff;
}

/* RIQUADRO ACCORDION INTERTEMPI SETTORI */
.sector-accordion-row {
  background: #0d1117 !important;
}
.sector-accordion-cell {
  padding: 0 !important;
  border-bottom: 2px solid #30363d !important;
}
.sector-accordion-card {
  padding: 8px 14px;
  background: linear-gradient(180deg, rgba(22, 27, 34, 0.96), rgba(13, 17, 23, 0.98));
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-left: 3px solid #e10600;
  box-shadow: inset 0 2px 6px rgba(0, 0, 0, 0.5);
}
.sec-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10px;
  font-weight: 800;
  color: #c9d1d9;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.sec-card-sectors {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.sec-box {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #161b22;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #30363d;
}
.sec-label {
  font-size: 9.5px;
  font-weight: 900;
  color: #8b949e;
  letter-spacing: 0.4px;
}

/* STILE PILOTI RITIRATI (SFONDO GRIGIO NOTEVOLE + SCRITTE PIU TENUI) */
.full-standings-table tr.row-retired,
.compact-timing-table tr.row-retired {
  background: #252830 !important;
  color: #8b949e !important;
  opacity: 0.72;
}
.full-standings-table tr.row-retired:hover,
.compact-timing-table tr.row-retired:hover {
  background: #2e3340 !important;
}
.full-standings-table tr.row-retired td,
.compact-timing-table tr.row-retired td {
  color: #8b949e !important;
}
.full-standings-table tr.row-retired .driver-cell-full,
.full-standings-table tr.row-retired .pos-cell,
.full-standings-table tr.row-retired .gap-cell,
.compact-timing-table tr.row-retired .pos-cell,
.compact-timing-table tr.row-retired .gap-cell {
  color: #8b949e !important;
}

.pos-cell { font-weight: 900; color: #f0f6fc; width: 24px; text-align: center; font-size: 11.5px; }
.delta-cell { width: 22px; text-align: center; font-size: 9.5px; font-weight: 800; }

.driver-cell-full {
  text-align: left !important;
  font-weight: 700;
  color: #f0f6fc;
  padding-left: 6px !important;
  padding-right: 6px !important;
  width: 78px;
  max-width: 82px;
  min-width: 74px;
  white-space: nowrap;
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
.team-cell {
  text-align: left !important;
  font-size: 9.5px;
  color: #8b949e;
  white-space: nowrap;
  min-width: 90px;
}

/* Column alignment and width utility classes */
.col-pos { width: 26px; text-align: center; }
.col-gain { width: 22px; text-align: center; }
.col-driver { width: 78px; max-width: 82px; text-align: left !important; padding-left: 6px !important; }
.col-team { text-align: left !important; }
.col-gap { width: 72px; text-align: right !important; padding-right: 6px !important; }
.col-int { width: 70px; text-align: right !important; padding-right: 6px !important; }
.col-last { width: 66px; text-align: center; }
.col-best { width: 66px; text-align: center; }
.col-tyre { width: 56px; text-align: center; }
.col-pit { width: 32px; text-align: center; }

.col-side-eye { width: 26px; text-align: center; padding: 2px !important; }
.col-side-pos { width: 24px; text-align: center; }
.col-side-gain { width: 20px; text-align: center; }
.col-side-driver { width: 72px; max-width: 76px; white-space: nowrap; text-align: left !important; padding-left: 4px !important; padding-right: 4px !important; }
.col-side-gap { width: 58px; text-align: right !important; padding-right: 4px !important; }
.col-side-int { width: 56px; text-align: right !important; padding-right: 4px !important; }
.col-side-last { width: 56px; text-align: center; }
.col-side-best { width: 56px; text-align: center; }
.col-side-tyre { width: 42px; text-align: center; }

.dash-muted { color: #8b949e; }
.delta-none { color: #8b949e; }
.delta-up { color: #2ea043; font-weight: 800; }
.delta-down { color: #da3633; font-weight: 800; }
.dnf-badge { color: #da3633; font-weight: 800; }
.gap-leader { color: #e3b341 !important; font-weight: 800; }
.gap-retired { color: #8b949e !important; }
.pit-count-cell { font-weight: 700; color: #8b949e; font-size: 10px; text-align: center; }
.drv-code-bold { font-weight: 700; }

/* STILE QUALIFICHE ED ELIMINAZIONE */
.full-standings-table tr.row-eliminated,
.compact-timing-table tr.row-eliminated {
  background: rgba(22, 27, 34, 0.65) !important;
  color: #6e7681 !important;
  opacity: 0.65;
}
.full-standings-table tr.row-eliminated:hover,
.compact-timing-table tr.row-eliminated:hover {
  background: rgba(30, 36, 46, 0.8) !important;
}
.full-standings-table tr.row-eliminated td,
.compact-timing-table tr.row-eliminated td {
  color: #6e7681 !important;
}
.full-standings-table tr.row-eliminated .drv-code-bold,
.compact-timing-table tr.row-eliminated .drv-code-bold {
  color: #8b949e !important;
}

.elim-zone-separator td {
  padding: 3px 6px !important;
  background: rgba(218, 54, 51, 0.12) !important;
  border-top: 1.5px solid #da3633 !important;
  border-bottom: 1.5px solid #da3633 !important;
  text-align: center !important;
  font-size: 8.5px !important;
  font-weight: 800 !important;
  color: #f85149 !important;
  letter-spacing: 0.8px !important;
  text-transform: uppercase !important;
}

/* BADGE STATO PILOTA (IN PIT / ON TRACK / OUT-LAP / FLYING / IN-LAP) */
.status-badge {
  display: inline-block;
  padding: 1.5px 5px;
  border-radius: 3px;
  font-size: 8.5px;
  font-weight: 800;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}
.status-inpit {
  background: #21262d;
  color: #8b949e;
  border: 1px solid #30363d;
}
.status-ontrack {
  background: rgba(35, 134, 54, 0.2);
  color: #3fb950;
  border: 1px solid #238636;
}
.status-flying {
  background: rgba(163, 113, 247, 0.2);
  color: #d2a8ff;
  border: 1px solid #a371f7;
}
.status-outlap {
  background: rgba(210, 153, 34, 0.2);
  color: #e3b341;
  border: 1px solid #d29922;
}
.status-inlap {
  background: rgba(248, 81, 73, 0.2);
  color: #ff7b72;
  border: 1px solid #f85149;
}
.status-eliminated {
  background: rgba(110, 118, 129, 0.15);
  color: #6e7681;
  border: 1px solid #30363d;
}

.col-q1, .col-q2, .col-q3 { width: 64px; text-align: center; }
.col-laps { width: 40px; text-align: center; }
.col-status { width: 68px; text-align: center; }
.col-side-status { width: 56px; text-align: center; }

.side-rc-box { padding: 4px; }
.side-rc-feed-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
  flex: 1;
}
.rc-count-label {
  font-size: 10px;
  color: #8b949e;
}

/* Tyre Badges with Age */
.tyre-badge-box {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: ui-monospace, Menlo, Consolas, monospace;
}
.tyre-badge {
  font-size: 9.5px;
  font-weight: 900;
  padding: 1px 4px;
  border-radius: 3px;
  text-align: center;
  display: inline-block;
}
.tyre-S { color: #ff7b72; border: 1px solid #f85149; background: rgba(248, 81, 73, 0.15); }
.tyre-M { color: #f2cc60; border: 1px solid #d29922; background: rgba(210, 153, 34, 0.15); }
.tyre-H { color: #f0f6fc; border: 1px solid #8b949e; background: rgba(240, 246, 252, 0.12); }
.tyre-I { color: #56d364; border: 1px solid #238636; background: rgba(46, 160, 67, 0.15); }
.tyre-W { color: #79c0ff; border: 1px solid #1f6feb; background: rgba(31, 111, 235, 0.15); }
.tyre-age {
  font-size: 8.5px;
  color: #8b949e;
  font-weight: 700;
}

/* Gap and Timing */
.gap-cell {
  text-align: right !important;
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 10.5px;
  padding-right: 6px !important;
  font-weight: 600;
}
.int-cell {
  text-align: right !important;
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 10px;
  padding-right: 6px !important;
  color: #8b949e;
}

.f1-badge {
  display: inline-block;
  padding: 1.5px 5px;
  border-radius: 3px;
  font-family: ui-monospace, Menlo, Consolas, monospace;
  font-size: 10px;
  font-weight: 700;
  min-width: 38px;
  text-align: center;
}
.badge-purple { background: rgba(163, 113, 247, 0.22); color: #d2a8ff; border: 1px solid rgba(163, 113, 247, 0.5); }
.badge-green { background: rgba(35, 134, 54, 0.22); color: #56d364; border: 1px solid rgba(46, 160, 67, 0.5); }
.badge-yellow { background: rgba(210, 153, 34, 0.18); color: #e3b341; }
.badge-dim { color: #484f58; }

/* ══════════════════════════════════════════════════════════════
   VIEW 2: MAPPA DELLA PISTA (UNICAMENTE IN MODALITÀ LANDSCAPE)
   ══════════════════════════════════════════════════════════════ */
.track-layout-container {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #000000;
}

.landscape-only-prompt {
  display: none;
  width: 100%;
  height: 100%;
  background: #090d13;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
  gap: 12px;
}
.landscape-only-icon {
  font-size: 38px;
  animation: rotatePrompt 2s infinite ease-in-out;
}
@keyframes rotatePrompt {
  0%, 100% { transform: rotate(0deg); }
  50% { transform: rotate(-90deg); }
}
.landscape-only-title {
  font-size: 15px;
  font-weight: 800;
  color: #f0f6fc;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}
.landscape-only-desc {
  font-size: 12px;
  color: #8b949e;
  max-width: 340px;
  line-height: 1.45;
}

/* Track Map Pane (40% width in View 2) */
.track-pane {
  position: relative;
  flex: 0 0 40%;
  width: 40%;
  max-width: 40%;
  background: #070a0f;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

#trackCanvas {
  display: block;
  cursor: grab;
  touch-action: none;
  user-select: none;
}
#trackCanvas:active {
  cursor: grabbing;
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
  font-size: 9px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  backdrop-filter: blur(4px);
}
.status-pill.green { border-color: #238636; color: #3fb950; }
.status-pill.yellow { border-color: #d29922; color: #e3b341; }
.status-pill.sc { border-color: #f0883e; background: rgba(240, 136, 62, 0.25); color: #ffa657; }

/* Map Zoom Controls */
.map-zoom-controls {
  position: absolute;
  bottom: 8px;
  left: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 15;
  background: rgba(13, 17, 23, 0.85);
  backdrop-filter: blur(4px);
  border: 1px solid #30363d;
  border-radius: 6px;
  padding: 2px 5px;
}
.zoom-btn {
  width: 22px;
  height: 22px;
  background: #21262d;
  color: #f0f6fc;
  border: 1px solid #30363d;
  border-radius: 4px;
  font-weight: 800;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.12s;
}
.zoom-btn:active, .zoom-btn:hover {
  background: #30363d;
  border-color: #58a6ff;
  color: #58a6ff;
}
.zoom-btn.reset {
  font-size: 12px;
}
.zoom-level-pill {
  font-size: 9px;
  font-weight: 700;
  color: #8b949e;
  padding: 0 3px;
  min-width: 22px;
  text-align: center;
}

/* Side Panel (60% width in View 2) */
.side-panel {
  flex: 0 0 60%;
  min-width: 0;
  width: 60%;
  max-width: 60%;
  background: #0d1117;
  border-left: 1px solid #21262d;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.side-tabs-bar {
  display: flex;
  gap: 4px;
  padding: 3px 6px;
  background: #161b22;
  border-bottom: 1px solid #21262d;
  flex-shrink: 0;
}
.side-tab-btn {
  background: transparent;
  border: 1px solid transparent;
  color: #8b949e;
  padding: 2.5px 7px;
  font-size: 9.5px;
  font-weight: 700;
  border-radius: 4px;
  cursor: pointer;
  white-space: nowrap;
}
.side-tab-btn:hover { background: #21262d; color: #f0f6fc; }
.side-tab-btn.active {
  background: #21262d;
  color: #f0f6fc;
  border-color: #388bfd;
  box-shadow: inset 0 -2px 0 #388bfd;
}

.side-pane-content {
  display: none;
  flex: 1;
  height: 100%;
  overflow: hidden;
}
.side-pane-content.active {
  display: flex;
  flex-direction: column;
}

/* Lateral Table (Compact: NO S1/S2/S3, NO AERO, NO STATO) */
.compact-table-wrapper {
  flex: 1;
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: auto;
  background: #0d1117;
}
.compact-table-wrapper::-webkit-scrollbar { width: 4px; height: 4px; }
.compact-table-wrapper::-webkit-scrollbar-thumb { background: #30363d; border-radius: 2px; }

.compact-timing-table {
  width: max-content;
  border-collapse: collapse;
  font-size: 10.5px;
  table-layout: auto;
}
.compact-timing-table th {
  background: #161b22;
  color: #8b949e;
  font-weight: 800;
  padding: 4px 3px;
  text-align: center;
  position: sticky;
  top: 0;
  font-size: 8.5px;
  border-bottom: 1px solid #30363d;
  letter-spacing: 0.3px;
  text-transform: uppercase;
  z-index: 10;
  white-space: nowrap;
}
.compact-timing-table td {
  padding: 3px 3px;
  border-bottom: 1px solid #161b22;
  white-space: nowrap;
  text-align: center;
}
.compact-timing-table tr {
  cursor: pointer;
  transition: background 0.12s;
}
.compact-timing-table tr:hover { background: #161b22; }
.compact-timing-table tr.active {
  background: rgba(88, 166, 255, 0.18);
  border-left: 3px solid #58a6ff;
}

/* Eye button for driver focus tracking (View 2) */
.btn-side-eye {
  background: #161b22;
  border: 1px solid #30363d;
  color: #8b949e;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 0;
  transition: all 0.15s ease;
  flex-shrink: 0;
}
.btn-side-eye:hover {
  background: #21262d;
  color: #f0f6fc;
  border-color: #58a6ff;
}
.btn-side-eye.active {
  background: #b58900 !important;
  border-color: #d29922 !important;
  color: #ffffff !important;
  box-shadow: 0 0 6px rgba(210, 153, 34, 0.45);
}

/* Map vertical zoom badge (Bottom-left in track pane, above back button) */
.map-zoom-vertical-badge {
  position: absolute;
  bottom: 50px;
  left: 12px;
  z-index: 30;
  display: none;
  flex-direction: column;
  background: rgba(22, 27, 34, 0.92);
  border: 1px solid #30363d;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(6px);
}
.map-zoom-vbtn {
  width: 32px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  color: #f0f6fc;
  cursor: pointer;
  transition: all 0.15s ease;
}
.map-zoom-vbtn:first-child {
  border-bottom: 1px solid #30363d;
}
.map-zoom-vbtn:hover:not(.disabled) {
  background: #21262d;
  color: #58a6ff;
}
.map-zoom-vbtn.disabled {
  opacity: 0.28 !important;
  pointer-events: none !important;
  cursor: default !important;
}

/* Map reset focus button (Bottom-left in track pane) */
.map-reset-focus-btn {
  position: absolute;
  bottom: 12px;
  left: 12px;
  z-index: 30;
  display: none;
  align-items: center;
  justify-content: center;
  background: #161b22;
  border: 1px solid #30363d;
  color: #f0f6fc;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}
.map-reset-focus-btn:hover {
  background: #21262d;
  border-color: #58a6ff;
  color: #58a6ff;
}

/* Dual Cockpit in Side Panel */
.telemetry-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 4px;
  gap: 4px;
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
  gap: 5px;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 5px;
  padding: 3px 6px;
  flex-shrink: 0;
}
.sel-group {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}
.sel-label {
  font-size: 9.5px;
  font-weight: 800;
  color: #8b949e;
  text-transform: uppercase;
  white-space: nowrap;
}
.drv-dropdown {
  background: #0d1117;
  color: #f0f6fc;
  border: 1px solid #30363d;
  border-radius: 3px;
  padding: 2.5px 5px;
  font-size: 10px;
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
  width: 22px;
  height: 22px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11px;
  flex-shrink: 0;
}

.dual-cockpit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
  flex: 1;
  min-height: 0;
}

.cockpit-card {
  background: #11141a;
  border: 1px solid #30363d;
  border-radius: 5px;
  padding: 5px 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-sizing: border-box;
  overflow: hidden;
}

.card-top-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #21262d;
  padding-bottom: 3px;
}
.card-drv-name {
  font-size: 11px;
  font-weight: 800;
  color: #f0f6fc;
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-drv-team {
  font-size: 9px;
  color: #8b949e;
  font-weight: 600;
  white-space: nowrap;
}

.speed-gear-cluster {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #090d13;
  border: 1px solid #21262d;
  border-radius: 5px;
  padding: 3px 5px;
}
.speed-display { text-align: center; }
.speed-num {
  font-size: 22px;
  font-weight: 900;
  color: #f0f6fc;
  font-family: ui-monospace, Menlo, monospace;
  line-height: 1;
}
.speed-label {
  font-size: 8.5px;
  font-weight: 800;
  color: #8b949e;
}

.gear-display { text-align: center; min-width: 30px; }
.gear-num {
  font-size: 24px;
  font-weight: 900;
  color: var(--red-f1);
  font-family: ui-monospace, Menlo, monospace;
  line-height: 1;
}
.gear-label {
  font-size: 8.5px;
  font-weight: 800;
  color: #8b949e;
}

.rpm-container {
  display: flex;
  flex-direction: column;
  gap: 1.5px;
}
.rpm-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 8.5px;
  font-weight: 800;
  color: #8b949e;
}
.rpm-num {
  color: #f0f6fc;
  font-family: ui-monospace, Menlo, monospace;
}
.shift-led-bar {
  display: flex;
  gap: 2px;
  height: 5px;
  background: #090d13;
  padding: 1px;
  border-radius: 2px;
  border: 1px solid #21262d;
}
.shift-led {
  flex: 1;
  border-radius: 1px;
  background: #21262d;
  transition: background 0.05s;
}
.shift-led.green { background: #238636; box-shadow: 0 0 3px #2ea043; }
.shift-led.yellow { background: #d29922; box-shadow: 0 0 3px #e3b341; }
.shift-led.red { background: #da3633; box-shadow: 0 0 4px #f85149; }
.shift-led.blue { background: #388bfd; box-shadow: 0 0 4px #58a6ff; }

.pedals-box {
  display: flex;
  gap: 6px;
}
.pedal-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1.5px;
}
.pedal-label {
  display: flex;
  justify-content: space-between;
  font-size: 8px;
  font-weight: 800;
  color: #8b949e;
}
.pedal-track {
  height: 4.5px;
  background: #090d13;
  border-radius: 2px;
  border: 1px solid #21262d;
  overflow: hidden;
}
.pedal-bar-thr {
  height: 100%;
  background: #238636;
  width: 0%;
  transition: width 0.08s ease;
}
.pedal-bar-brk {
  height: 100%;
  background: #da3633;
  width: 0%;
  transition: width 0.08s ease;
}

.regs-2026-box {
  background: #090d13;
  border: 1px solid #21262d;
  border-radius: 4px;
  padding: 3px 5px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.aero-status-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 8.5px;
  font-weight: 800;
  color: #8b949e;
}
.aero-mode-badge {
  font-size: 8px;
  font-weight: 900;
  padding: 1px 5px;
  border-radius: 3px;
  letter-spacing: 0.4px;
}
.aero-zmode {
  background: rgba(35, 134, 54, 0.25);
  color: #3fb950;
  border: 1px solid #238636;
}
.aero-xmode {
  background: rgba(56, 189, 248, 0.25);
  color: #38bdf8;
  border: 1px solid #0284c7;
}

.battery-soc-box {
  display: flex;
  flex-direction: column;
  gap: 1.5px;
}
.battery-meta {
  display: flex;
  justify-content: space-between;
  font-size: 8px;
  font-weight: 800;
  color: #8b949e;
}
.battery-track {
  height: 4px;
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
  font-size: 8.5px;
  font-weight: 800;
  color: #8b949e;
}
.boost-pill {
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 8px;
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
  font-size: 8.5px;
  color: #8b949e;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ══════════════════════════════════════════════════════════════
   VIEW 3: RACE CONTROL FULLSCREEN
   ══════════════════════════════════════════════════════════════ */
.full-rc-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 8px 12px;
  gap: 6px;
  background: #090d13;
}

.full-rc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 6px;
  padding: 6px 10px;
  flex-shrink: 0;
  gap: 8px;
}

.rc-filters {
  display: flex;
  gap: 4px;
}
.rc-filter-btn {
  background: #21262d;
  border: 1px solid #30363d;
  color: #8b949e;
  font-size: 9.5px;
  font-weight: 700;
  padding: 2.5px 8px;
  border-radius: 4px;
  cursor: pointer;
}
.rc-filter-btn:hover { color: #f0f6fc; }
.rc-filter-btn.active {
  background: #388bfd;
  border-color: #388bfd;
  color: #fff;
}

.full-rc-messages-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.full-rc-messages-list::-webkit-scrollbar { width: 4px; }
.full-rc-messages-list::-webkit-scrollbar-thumb { background: #30363d; border-radius: 2px; }

.rc-ticker-card {
  background: #1c1408;
  border: 1px solid #d29922;
  border-radius: 5px;
  padding: 5px 8px;
  font-size: 10.5px;
  font-weight: 700;
  color: #f0f6fc;
  line-height: 1.35;
  flex-shrink: 0;
}

.rc-msg-card {
  background: #161b22;
  border: 1px solid #21262d;
  border-left: 3px solid #388bfd;
  border-radius: 4px;
  padding: 5px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  transition: background 0.12s;
}
.rc-msg-card:hover { background: #1c2128; }
.rc-msg-card.yellow { border-left-color: #d29922; }
.rc-msg-card.green { border-left-color: #238636; }
.rc-msg-card.sc { border-left-color: #f0883e; }
.rc-msg-card.red { border-left-color: #da3633; }
.rc-msg-meta {
  display: flex;
  justify-content: space-between;
  font-size: 9px;
  font-weight: 700;
  color: #8b949e;
}
.rc-msg-text {
  font-size: 10.5px;
  font-weight: 600;
  color: #f0f6fc;
  line-height: 1.3;
}

/* ══════════════════════════════════════════════════════════════
   BOTTOM NAVIGATION BAR (ALLA BASE DELLO SCHERMO)
   ══════════════════════════════════════════════════════════════ */
.bottom-nav-bar {
  flex-shrink: 0;
  height: var(--bottom-nav-h);
  background: #11141a;
  border-top: 1px solid #21262d;
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  padding-left: var(--safe-left);
  padding-right: var(--safe-right);
  padding-top: 0;
  padding-bottom: 2px;
  z-index: 100;
  box-sizing: border-box;
}

.bottom-nav-btn {
  background: transparent;
  border: none;
  color: #8b949e;
  padding: 2.5px 12px;
  font-size: 10.5px;
  font-weight: 700;
  border-radius: 5px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  transition: all 0.15s ease;
  white-space: nowrap;
  margin-bottom: 1px;
}
.bottom-nav-btn .bnav-icon {
  font-size: 13.5px;
}
.bottom-nav-btn:hover {
  background: #1c2128;
  color: #f0f6fc;
}
.bottom-nav-btn.active {
  background: #1c2128;
  color: #58a6ff;
  border-bottom: 2px solid #58a6ff;
}

/* ══════════════════════════════════════════════════════════════
   RESPONSIVE ORIENTATION RULES (PORTRAIT vs LANDSCAPE)
   ══════════════════════════════════════════════════════════════ */
@media (orientation: portrait) {
  :root {
    --hdr-row1-h: 30px;
    --hdr-row2-h: 24px;
    --bottom-nav-h: 42px;
  }
  .hdr-event {
    max-width: 140px;
    font-size: 10px;
  }
  .full-standings-table th, .full-standings-table td {
    padding: 3.5px 3px;
    font-size: 9.5px;
  }
  .driver-cell-full {
    width: 78px;
    max-width: 82px;
    min-width: 74px;
  }
  .team-cell {
    min-width: 75px;
  }
  .bottom-nav-btn {
    padding: 2px 8px;
    font-size: 9.5px;
    flex-direction: column;
    gap: 1px;
    margin-bottom: 1px;
  }
  .bottom-nav-btn .bnav-icon {
    font-size: 13px;
  }
  .bottom-nav-btn .bnav-label {
    font-size: 9px;
  }

  /* View 2: strictly landscape-only prompt */
  #view-track .track-layout-container {
    display: none !important;
  }
  #view-track .landscape-only-prompt {
    display: flex !important;
  }
}

@media (orientation: landscape) {
  #view-track .landscape-only-prompt {
    display: none !important;
  }
  #view-track .track-layout-container {
    display: flex !important;
  }

  /* Vista 1: Classifica Live stretchata lungo tutto lo schermo in landscape */
  #view-standings .full-standings-wrapper {
    width: 100%;
  }
  #view-standings .full-standings-table {
    width: 100%;
    min-width: 100%;
    table-layout: auto;
  }
  #view-standings .full-standings-table th,
  #view-standings .full-standings-table td {
    padding: 8px 10px;
    font-size: 11px;
  }
  #view-standings .full-standings-table .col-pos,
  #view-standings .full-standings-table .pos-cell {
    width: 36px;
  }
  #view-standings .full-standings-table .col-gain,
  #view-standings .full-standings-table .delta-cell {
    width: 36px;
  }
  #view-standings .full-standings-table .col-driver,
  #view-standings .full-standings-table .driver-cell-full {
    min-width: 120px;
  }
  #view-standings .full-standings-table .col-gap,
  #view-standings .full-standings-table .gap-cell {
    min-width: 85px;
  }
  #view-standings .full-standings-table .col-int,
  #view-standings .full-standings-table .int-cell {
    min-width: 85px;
  }
  #view-standings .full-standings-table .col-last,
  #view-standings .full-standings-table .col-best {
    min-width: 90px;
  }
  #view-standings .full-standings-table .col-tyre {
    min-width: 65px;
  }
  #view-standings .full-standings-table .col-pit,
  #view-standings .full-standings-table .pit-count-cell {
    width: 48px;
  }
  #view-standings .full-standings-table .col-q1,
  #view-standings .full-standings-table .col-q2,
  #view-standings .full-standings-table .col-q3 {
    min-width: 80px;
  }
  #view-standings .full-standings-table .col-status {
    min-width: 75px;
  }
  #view-standings .full-standings-table .col-laps {
    width: 48px;
  }
}

/* ══════════════════════════════════════════════════════════════
   SEPANG DEDICATED SIMULATION BAR & TIMELINE SCRUBBER
   ══════════════════════════════════════════════════════════════ */
.sepang-sim-header-bar {
  background: #161b22;
  border-bottom: 1px solid #30363d;
  padding: 5px 12px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
}
.sepang-session-pills {
  display: flex;
  gap: 6px;
  align-items: center;
}
.sepang-session-pill {
  background: #21262d;
  color: #c9d1d9;
  border: 1px solid #30363d;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;
}
.sepang-session-pill:hover {
  background: #30363d;
  color: #fff;
}
.sepang-session-pill.active {
  background: #e10600;
  color: #fff;
  border-color: #e10600;
  box-shadow: 0 0 8px rgba(225, 6, 0, 0.4);
}
.sepang-scrubber-box {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 440px;
}
.sepang-play-btn {
  background: #238636;
  color: #fff;
  border: 1px solid #2ea043;
  border-radius: 4px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.sepang-play-btn.paused {
  background: #d29922;
  border-color: #bb8009;
}
.sepang-time-slider {
  flex: 1;
  height: 6px;
  accent-color: #e10600;
  cursor: pointer;
}
.sepang-time-label {
  font-family: monospace;
  font-size: 11px;
  color: #8b949e;
  white-space: nowrap;
}
.sepang-speed-box {
  display: flex;
  gap: 4px;
  align-items: center;
}
.sepang-speed-btn {
  background: #21262d;
  color: #8b949e;
  border: 1px solid #30363d;
  border-radius: 4px;
  padding: 3px 7px;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
}
.sepang-speed-btn:hover {
  background: #30363d;
  color: #fff;
}
.sepang-speed-btn.active {
  background: #58a6ff;
  color: #0d1117;
  border-color: #58a6ff;
  font-weight: 700;
}
.sepang-jump-bar {
  background: #0d1117;
  border-bottom: 1px solid #21262d;
  padding: 4px 12px;
  display: flex;
  gap: 6px;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: thin;
}
.sepang-jump-btn {
  background: #161b22;
  color: #8b949e;
  border: 1px solid #30363d;
  border-radius: 4px;
  padding: 3px 8px;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}
.sepang-jump-btn:hover {
  background: #21262d;
  color: #f0f6fc;
  border-color: #58a6ff;
}
`;

// ══════════════════════════════════════════════════════════════
// 2. MODULAR DATASETS CONTENT (js/live_simulation_data.js)
// ══════════════════════════════════════════════════════════════
const dataJsContent = `/**
 * FastestLap — Simulation Datasets for Sepang and Baku Grand Prix
 * Embedded and pre-calibrated for high-fidelity offline verification
 */

const SIM_SEPANG = {
  circuitId: 'sepang',
  eventTitle: 'FORMULA 1 PETRONAS MALAYSIAN GP',
  svgWidth: 1280,
  svgHeight: 1057,
  lapDuration: 95.0,
  totalLaps: 55,
  raceStartTimeSec: 216.9,
  raceFinishSec: 6435.0,
  sessionDuration: 6550,
  pitExitTrackSec: 5.06,
  pitEntryTrackSec: 89.34,
  s1Track: 21.11,
  s2Track: 46.58,
  gridOrder: ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41'],
  nodes: ${JSON.stringify(compactSepangNodes)},
  pitNodes: ${JSON.stringify(sepangPitNodes)},
  drivers: ${JSON.stringify(sepangDrivers)},
  keyframes: ${JSON.stringify(sepangKeyframes)},
  pitStops: ${JSON.stringify(sepangPitStops)},
  rc: ${JSON.stringify(sepangRC)},
  retirements: ${JSON.stringify(sepangRetirements)},
  stints: ${JSON.stringify(sepangStints)},
  laps: ${JSON.stringify(sepangLaps)},
  events: ${JSON.stringify(sepangEvents)},
  incidents: ${JSON.stringify(sepangIncidents)},
  lapStarts: ${JSON.stringify(sepangLapStarts)}
};

const SIM_SEPANG_FP = Object.assign(${JSON.stringify(sepangFpData)}, {
  svgWidth: 1280,
  svgHeight: 1057,
  raceStartTimeSec: 0.0,
  raceFinishSec: 3600.0,
  nodes: SIM_SEPANG.nodes,
  pitNodes: SIM_SEPANG.pitNodes
});

const SIM_SEPANG_QUALIFYING = Object.assign(${JSON.stringify(sepangQualyData)}, {
  svgWidth: 1280,
  svgHeight: 1057,
  raceStartTimeSec: 0.0,
  raceFinishSec: 3600.0,
  nodes: SIM_SEPANG.nodes,
  pitNodes: SIM_SEPANG.pitNodes
});

const SIM_BAKU = {
  circuitId: 'baku',
  eventTitle: 'FORMULA 1 AZERBAIJAN GRAND PRIX',
  svgWidth: 500,
  svgHeight: 371,
  lapDuration: 108.6,
  totalLaps: 51,
  raceStartTimeSec: 0.0,
  raceFinishSec: 5881.3,
  sessionDuration: 5950,
  pitExitTrackSec: 4.12,
  pitEntryTrackSec: 87.2,
  s1Track: 38.85,
  s2Track: 83.5,
  gridOrder: ['63', '16', '81', '6', '1', '3', '44', '10', '55', '43', '87', '30', '23', '31', '41', '12', '5', '27', '14', '11', '77', '18'],
  nodes: ${JSON.stringify(compactBakuNodes)},
  pitNodes: ${JSON.stringify(bakuPitNodes)},
  drivers: ${JSON.stringify(bakuDrivers)},
  keyframes: ${JSON.stringify(bakuKeyframes)},
  pitStops: ${JSON.stringify(bakuPitStops)},
  rc: ${JSON.stringify(bakuRC)},
  retirements: ${JSON.stringify(bakuRetirements)},
  stints: ${JSON.stringify(bakuStints)},
  laps: ${JSON.stringify(bakuLaps)},
  events: ${JSON.stringify(bakuEvents)},
  incidents: ${JSON.stringify(bakuIncidents)},
  lapStarts: ${JSON.stringify(bakuLapStarts)}
};
`;

// ══════════════════════════════════════════════════════════════
// 3. MODULAR APP LOGIC (js/live_timing_app.js)
// ══════════════════════════════════════════════════════════════
const appJsContent = `/**
 * FastestLap — Live Timing & Telemetry Client Engine
 * Core animation loop, timing calculation, canvas drawing and Android Bridge integration
 */

// ACTIVE SESSION CONTEXT
let currentSim = SIM_SEPANG;
let NODES = currentSim.nodes;
let DRIVERS = currentSim.drivers;
let DRIVER_KEYFRAMES = currentSim.keyframes;
let PIT_STOPS = currentSim.pitStops;
let RACE_CONTROL_MESSAGES = currentSim.rc;
let RETIREMENTS = currentSim.retirements;
let DRIVER_STINTS = currentSim.stints;
let DRIVER_LAPS = currentSim.laps;
let RACE_EVENTS = currentSim.events;
let INCIDENTS = currentSim.incidents;
let LAP_STARTS = currentSim.lapStarts;
let GRID_ORDER = currentSim.gridOrder;
let PIT_LANE_NODES = currentSim.pitNodes;
let SVG_WIDTH = currentSim.svgWidth;
let SVG_HEIGHT = currentSim.svgHeight;
let LAP_DURATION = currentSim.lapDuration;
let TOTAL_LAPS = currentSim.totalLaps;
let RACE_FINISH_SEC = currentSim.raceFinishSec;
let SESSION_DURATION = currentSim.sessionDuration;
let PIT_EXIT_TRACK_SEC = currentSim.pitExitTrackSec;
let PIT_ENTRY_TRACK_SEC = currentSim.pitEntryTrackSec;

let PIT_LANE_DISTS = [0];
let PIT_LANE_TOTAL_DIST = 0;

function recalculatePitLaneDistances() {
  PIT_LANE_DISTS = [0];
  for (let i = 0; i < PIT_LANE_NODES.length - 1; i++) {
    const dx = PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px;
    const dy = PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py;
    PIT_LANE_DISTS.push(PIT_LANE_DISTS[i] + Math.sqrt(dx * dx + dy * dy));
  }
  PIT_LANE_TOTAL_DIST = PIT_LANE_DISTS[PIT_LANE_DISTS.length - 1] || 1;
}
recalculatePitLaneDistances();

// SIMULATION RUNTIME STATE
let currentCircuit = 'sepang';
let currentSecond = 0.0;
let simSpeedMultiplier = 1.0;
let isPlaying = true;
let lastAnimFrameTimestamp = null;
let focusedDriver = null;
let selectedDriver1 = '3';
let selectedDriver2 = '16';
let isMapFollowingDriver = false;
let currentMainView = 'view-standings';
let currentSideTab = 'side-standings';
let activeRcFilter = 'ALL';
let trackBgImage = null;
let isGenericRealMode = false;
let expandedDriverKey = null;
let sessionType = 'race';
let sessionPart = '';

function renderTableHeaders(type) {
  const fullThead = document.querySelector('#view-standings .full-standings-table thead');
  const compactThead = document.querySelector('#side-standings .compact-timing-table thead');
  if (!fullThead && !compactThead) return;

  if (type === 'practice') {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-best">MIGLIOR GIRO</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-int">INTERVALLO</th>'
        + '<th class="col-last">ULTIMO GIRO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-laps">GIRI</th>'
        + '<th class="col-status">STATO</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-best">MIGLIORE</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-last">ULTIMO</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '<th class="col-side-status">STATO</th>'
        + '</tr>';
    }
  } else if (type === 'qualifying') {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-q1">Q1</th>'
        + '<th class="col-q2">Q2</th>'
        + '<th class="col-q3">Q3</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-laps">GIRI</th>'
        + '<th class="col-status">STATO</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-best">TEMPO</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '<th class="col-side-status">STATO</th>'
        + '</tr>';
    }
  } else {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-gain">+/-</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-int">INTERVALLO</th>'
        + '<th class="col-last">ULTIMO GIRO</th>'
        + '<th class="col-best">MIGLIOR GIRO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-pit">PIT</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-gain">+/-</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-int">INTERVALLO</th>'
        + '<th class="col-side-last">ULTIMO</th>'
        + '<th class="col-side-best">MIGLIORE</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '</tr>';
    }
  }
}

function setSessionType(type) {
  sessionType = type;
  if (currentCircuit === 'sepang') {
    if (type === 'practice') {
      applySimulation(SIM_SEPANG_FP);
    } else if (type === 'qualifying') {
      applySimulation(SIM_SEPANG_QUALIFYING);
    } else {
      applySimulation(SIM_SEPANG);
    }
  }
  renderTableHeaders(type);
  renderSimulationControls(!isGenericRealMode);
  updateSessionStatusBar();
  updateTimingTables();
  drawTrack();
}
window.setSessionType = setSessionType;

function applySimulation(sim) {
  currentSim = sim;
  currentCircuit = sim.circuitId || 'sepang';
  NODES = currentSim.nodes;
  DRIVERS = currentSim.drivers;
  DRIVER_KEYFRAMES = currentSim.keyframes;
  PIT_STOPS = currentSim.pitStops;
  RACE_CONTROL_MESSAGES = currentSim.rc;
  RETIREMENTS = currentSim.retirements;
  DRIVER_STINTS = currentSim.stints;
  DRIVER_LAPS = currentSim.laps;
  RACE_EVENTS = currentSim.events;
  INCIDENTS = currentSim.incidents;
  LAP_STARTS = currentSim.lapStarts;
  GRID_ORDER = currentSim.gridOrder;
  PIT_LANE_NODES = currentSim.pitNodes;
  SVG_WIDTH = currentSim.svgWidth;
  SVG_HEIGHT = currentSim.svgHeight;
  LAP_DURATION = currentSim.lapDuration;
  TOTAL_LAPS = currentSim.totalLaps;
  RACE_FINISH_SEC = currentSim.raceFinishSec;
  SESSION_DURATION = currentSim.sessionDuration;
  PIT_EXIT_TRACK_SEC = currentSim.pitExitTrackSec;
  PIT_ENTRY_TRACK_SEC = currentSim.pitEntryTrackSec;

  recalculatePitLaneDistances();
  currentSecond = 0.0;

  populateDriverDropdowns();
  focusedDriver = currentSim.gridOrder[0] || '3';
  selectedDriver1 = currentSim.gridOrder[0] || '3';
  selectedDriver2 = currentSim.gridOrder[1] || '16';

  const hdrTitle = document.getElementById('hdrEventTitle');
  if (hdrTitle) hdrTitle.textContent = currentSim.eventTitle;

  ['btnSimSepang', 'btnSimBaku'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const activeBtn = document.getElementById(sim.circuitId === 'baku' ? 'btnSimBaku' : 'btnSimSepang');
  if (activeBtn) activeBtn.classList.add('active');

  ['btnTypeRace', 'btnTypeQual', 'btnTypeFP'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const actTypeBtn = document.getElementById(sessionType === 'practice' ? 'btnTypeFP' : (sessionType === 'qualifying' ? 'btnTypeQual' : 'btnTypeRace'));
  if (actTypeBtn) actTypeBtn.classList.add('active');

  renderTableHeaders(sessionType);
  resizeTrackCanvas();
  updateTimingTables();
}

function switchSimulationCircuit(circuitId) {
  if (typeof resetMapFocus === 'function') resetMapFocus();
  else if (typeof resetMapZoom === 'function') resetMapZoom();
  if (circuitId === 'baku') {
    applySimulation(SIM_BAKU);
  } else {
    if (sessionType === 'practice') {
      applySimulation(SIM_SEPANG_FP);
    } else if (sessionType === 'qualifying') {
      applySimulation(SIM_SEPANG_QUALIFYING);
    } else {
      applySimulation(SIM_SEPANG);
    }
  }
}

function populateDriverDropdowns() {
  const keys = Object.keys(DRIVERS);
  const opts = keys.map(k => {
    const d = DRIVERS[k];
    return '<option value="' + k + '">#' + d.number + ' ' + d.code + ' — ' + d.firstName + ' ' + d.lastName + ' (' + d.team + ')</option>';
  }).join('');

  const s1 = document.getElementById('drv1Select');
  const s2 = document.getElementById('drv2Select');
  if (s1) s1.innerHTML = opts;
  if (s2) s2.innerHTML = opts;
  if (s1) s1.value = selectedDriver1;
  if (s2) s2.value = selectedDriver2;
}

// ══════════════════════════════════════════════════════════════
// AUTHENTIC SIMULATION FUNCTIONS
// ══════════════════════════════════════════════════════════════
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
  if (currentSim.circuitId === 'sepang' && t < 216.9) {
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
  if (currentSim.circuitId === 'sepang' && t < 216.9) {
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
  if (currentSim.circuitId === 'sepang') {
    if (t < 108.0) return 0;
    if (t < 216.9) return 0;
  }
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
    const ret = RETIREMENTS[drvKey];
    if (ret && ret.timeSec) {
      const gapAtRet = getDriverGap(drvKey, ret.timeSec);
      return (((ret.timeSec - gapAtRet) % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
    }
    return 30.0;
  }

  const gridIdx = GRID_ORDER.indexOf(drvKey);
  const pos = gridIdx >= 0 ? gridIdx : 21;
  const slotOff = getSlotOffset(pos);
  const totalTarget = slotOff + 2 * LAP_DURATION;

  if (currentSim.circuitId === 'sepang' && t < 216.9) {
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
    return Math.min(LAP_DURATION, (t - RACE_FINISH_SEC) * 0.35);
  }

  // Pit stop check
  const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
  if (activeStop) {
    const frac = (t - activeStop.startSec) / Math.max(1, activeStop.endSec - activeStop.startSec);
    return frac * PIT_EXIT_TRACK_SEC;
  }

  // Out-lap check
  const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
  if (outStop) {
    const outEnd = outStop.outLapEndSec || (outStop.endSec + 80);
    const u = Math.min(1.0, Math.max(0.0, (t - outStop.endSec) / Math.max(1, outEnd - outStop.endSec)));
    return PIT_EXIT_TRACK_SEC + u * (LAP_DURATION - PIT_EXIT_TRACK_SEC);
  }

  // Lap timing progression
  const rec = getDriverLapRecord(drvKey, t);
  if (rec && rec.dur && rec.dur > 0) {
    const inStop = PIT_STOPS.find(p => p.driver === drvKey && p.startSec > rec.startSec && p.startSec <= rec.startSec + rec.dur && t < p.startSec && t >= rec.startSec);
    if (inStop) {
      const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(1, inStop.startSec - rec.startSec)));
      return u * PIT_ENTRY_TRACK_SEC;
    }

    const tS1 = rec.startSec + (rec.s1 || (rec.dur * 0.25));
    const tS2 = tS1 + (rec.s2 || (rec.dur * 0.35));
    const S1_TRACK = currentSim.s1Track;
    const S2_TRACK = currentSim.s2Track;

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

function getDriverTireInfo(drvKey, t) {
  const stints = DRIVER_STINTS[drvKey];
  const curLap = getDriverLap(drvKey, t);
  if (!stints || stints.length === 0) {
    return { code: 'M', name: 'MEDIUM', age: Math.max(1, curLap), stint: 1 };
  }
  if (curLap <= 1) {
    return { code: stints[0].code, name: stints[0].compound, age: 1, stint: 1 };
  }
  for (let i = 0; i < stints.length; i++) {
    const s = stints[i];
    if (curLap >= s.lapStart && curLap <= s.lapEnd) {
      return {
        code: s.code,
        name: s.compound,
        age: Math.max(1, curLap - s.lapStart + 1),
        stint: s.stint
      };
    }
  }
  const last = stints[stints.length - 1];
  return {
    code: last.code,
    name: last.compound,
    age: Math.max(1, curLap - last.lapStart + 1),
    stint: last.stint
  };
}

function interpolateTrackNode(trackSec) {
  const n = NODES.length;
  if (n === 0) {
    return { px: 0, py: 0, speed: 0, gear: 0, rpm: 0, throttle: 0, brake: 0, drs: 0, location: '' };
  }
  const s = ((trackSec % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;

  // Binary search for exact track node matching timestamp s (1:1 telemetry & physical position match)
  let low = 0;
  let high = n - 1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (NODES[mid][0] <= s) {
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }

  const i1 = Math.max(0, high);
  const i2 = (i1 + 1) % n;
  const n1 = NODES[i1];
  const n2 = NODES[i2];

  let dt = n2[0] - n1[0];
  if (dt <= 0) dt += LAP_DURATION;
  let u = 0;
  if (dt > 0) {
    let el = s - n1[0];
    if (el < 0) el += LAP_DURATION;
    u = Math.min(1.0, Math.max(0.0, el / dt));
  }

  const rawBrake = u < 0.5 ? n1[10] : n2[10];
  const brakeVal = rawBrake > 1 ? rawBrake : (rawBrake ? 100 : 0);

  return {
    px: n1[1] + (n2[1] - n1[1]) * u,
    py: n1[2] + (n2[2] - n1[2]) * u,
    speed: Math.round(n1[6] + (n2[6] - n1[6]) * u),
    gear: u < 0.5 ? n1[7] : n2[7],
    rpm: Math.round(n1[8] + (n2[8] - n1[8]) * u),
    throttle: Math.round(n1[9] + (n2[9] - n1[9]) * u),
    brake: brakeVal,
    drs: n1[11],
    location: (u < 0.5 ? n1[12] : n2[12]) || n1[12]
  };
}

function getDriverVisualState(drvKey, t) {
  const isRet = isDriverRetired(drvKey, t);
  const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
  const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
  const isOutLapMerge = Boolean(outStop && (t - outStop.endSec) <= 2.0);

  if (isRet) {
    const trackSec = getDriverTrackSec(drvKey, t);
    const originPt = interpolateTrackNode(trackSec);
    return {
      px: originPt.px + 6,
      py: originPt.py + 6,
      speed: 0,
      gear: 0,
      rpm: 0,
      throttle: 0,
      brake: 0,
      drs: 0,
      location: 'Ritiro — Fuori Traiettoria',
      isRetired: true,
      inPit: false,
      isOutLapMerge: false
    };
  }

  if (activeStop) {
    const dur = Math.max(1, activeStop.endSec - activeStop.startSec);
    const elapsed = t - activeStop.startSec;
    const progressFrac = Math.max(0, Math.min(1, elapsed / dur));
    const gridIdx = GRID_ORDER.indexOf(drvKey);
    const stallFrac = 0.35 + (gridIdx >= 0 ? gridIdx : 10) * 0.012;
    const stallDist = stallFrac * PIT_LANE_TOTAL_DIST;

    let curDist = 0;
    let speed = 80;
    let gear = 2;
    let rpm = 4500;
    let loc = 'Corsia Box (80 km/h)';

    if (progressFrac < 0.35) {
      const u = progressFrac / 0.35;
      curDist = u * stallDist;
      speed = Math.round(80 * (1 - u * 0.4));
      gear = 2;
    } else if (progressFrac <= 0.65) {
      curDist = stallDist;
      speed = 0;
      gear = 0;
      rpm = 3200;
      loc = 'Sosta ai Box (Cambio Gomme)';
    } else {
      const u = (progressFrac - 0.65) / 0.35;
      curDist = stallDist + u * (PIT_LANE_TOTAL_DIST - stallDist);
      speed = Math.round(40 + u * 40);
      gear = 2;
      loc = 'Uscita Box verso Curva 1';
    }

    const pt = getPitLaneCoordAtDist(curDist);
    return {
      px: pt.px,
      py: pt.py,
      speed: speed,
      gear: gear,
      rpm: rpm,
      throttle: speed > 0 ? 55 : 0,
      brake: speed === 0 ? 100 : 0,
      drs: 0,
      location: loc,
      isRetired: false,
      inPit: true,
      isOutLapMerge: false
    };
  }

  const trackSec = getDriverTrackSec(drvKey, t);
  const rawState = interpolateTrackNode(trackSec);

  if (isOutLapMerge && outStop) {
    const u = (t - outStop.endSec) / 2.0;
    const su = u * u * (3 - 2 * u);
    const pitExitNode = PIT_LANE_NODES[PIT_LANE_NODES.length - 1];
    return {
      px: pitExitNode.px * (1 - su) + rawState.px * su,
      py: pitExitNode.py * (1 - su) + rawState.py * su,
      speed: Math.round(80 + su * (rawState.speed - 80)),
      gear: Math.max(2, rawState.gear),
      rpm: rawState.rpm,
      throttle: rawState.throttle,
      brake: rawState.brake,
      drs: rawState.drs,
      location: 'Rientro in Pista — Curva 1',
      isRetired: false,
      inPit: false,
      isOutLapMerge: true
    };
  }

  return {
    ...rawState,
    speed: Math.round(rawState.speed),
    isRetired: false,
    inPit: false,
    isOutLapMerge: false
  };
}

function isSafetyCarActive(t) {
  if (currentSim.circuitId === 'sepang') {
    if (t < 216.9) return true;
    if (t >= 1150 && t <= 1580) return true;
  } else if (currentSim.circuitId === 'baku') {
    if (t >= 3285 && t <= 3924) return true;
    if (t >= 4130 && t <= 4396) return true;
  }
  return false;
}

function isVirtualSafetyCarActive(t) {
  if (currentSim.circuitId === 'sepang') {
    if (t >= 3200 && t <= 3350) return true;
  }
  return false;
}

function getSessionBests(t) {
  let bestLap = { val: Infinity, str: '-', drv: null };
  let bestS1 = { val: Infinity, str: '-' };
  let bestS2 = { val: Infinity, str: '-' };
  let bestS3 = { val: Infinity, str: '-' };

  Object.keys(DRIVERS).forEach(k => {
    const laps = DRIVER_LAPS[k] || [];
    laps.forEach(l => {
      if (l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50) {
        if (l.dur < bestLap.val) {
          bestLap = { val: l.dur, str: formatLapTime(l.dur), drv: k };
        }
        if (l.s1 && l.s1 < bestS1.val) bestS1 = { val: l.s1, str: l.s1.toFixed(3) };
        if (l.s2 && l.s2 < bestS2.val) bestS2 = { val: l.s2, str: l.s2.toFixed(3) };
        if (l.s3 && l.s3 < bestS3.val) bestS3 = { val: l.s3, str: l.s3.toFixed(3) };
      }
    });
  });

  return { bestLap, bestS1, bestS2, bestS3 };
}

function getDriverTimingStats(drvKey, t, sessionBests) {
  const laps = DRIVER_LAPS[drvKey] || [];
  const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50);

  let personalBest = Infinity;
  completedLaps.forEach(l => {
    if (l.dur < personalBest) personalBest = l.dur;
  });

  const lastLap = completedLaps.length > 0 ? completedLaps[completedLaps.length - 1] : null;
  let lastLapStr = '-';
  let lastLapBadge = 'badge-dim';
  if (lastLap) {
    lastLapStr = formatLapTime(lastLap.dur);
    if (sessionBests && Math.abs(lastLap.dur - sessionBests.bestLap.val) < 0.005) {
      lastLapBadge = 'badge-purple';
    } else if (Math.abs(lastLap.dur - personalBest) < 0.005) {
      lastLapBadge = 'badge-green';
    } else {
      lastLapBadge = 'badge-yellow';
    }
  }

  let bestLapStr = '-';
  let bestLapBadge = 'badge-dim';
  if (personalBest < Infinity) {
    bestLapStr = formatLapTime(personalBest);
    if (sessionBests && Math.abs(personalBest - sessionBests.bestLap.val) < 0.005) {
      bestLapBadge = 'badge-purple';
    } else {
      bestLapBadge = 'badge-green';
    }
  }

  return { lastLapStr, lastLapBadge, bestLapStr, bestLapBadge, personalBest };
}

function formatLapTime(sec) {
  if (!sec || sec === Infinity || sec <= 0) return '-';
  const m = Math.floor(sec / 60);
  const s = (sec % 60).toFixed(3);
  return m > 0 ? (m + ':' + (s < 10 ? '0' : '') + s) : s;
}

function renderBadge(text, badgeClass) {
  if (!text || text === '-') return '<span class="dash-muted">-</span>';
  return '<span class="f1-badge ' + badgeClass + '">' + text + '</span>';
}

function getDriverSectorTimes(drvKey, t, sessionBests) {
  const laps = DRIVER_LAPS[drvKey] || [];
  const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50);

  let pbS1 = Infinity, pbS2 = Infinity, pbS3 = Infinity;
  completedLaps.forEach(l => {
    if (l.s1 && l.s1 < pbS1) pbS1 = l.s1;
    if (l.s2 && l.s2 < pbS2) pbS2 = l.s2;
    if (l.s3 && l.s3 < pbS3) pbS3 = l.s3;
  });

  const targetLap = completedLaps.length > 0 ? completedLaps[completedLaps.length - 1] : (laps[0] || null);
  if (!targetLap) {
    return {
      lapNum: '-',
      s1: { str: '-', badge: 'badge-dim' },
      s2: { str: '-', badge: 'badge-dim' },
      s3: { str: '-', badge: 'badge-dim' }
    };
  }

  function formatSecBadge(val, pbVal, sessBestVal) {
    if (!val || val <= 0) return { str: '-', badge: 'badge-dim' };
    const str = val.toFixed(3);
    let badge = 'badge-yellow';
    if (sessBestVal && sessBestVal.val < Infinity && Math.abs(val - sessBestVal.val) < 0.005) {
      badge = 'badge-purple';
    } else if (pbVal < Infinity && Math.abs(val - pbVal) < 0.005) {
      badge = 'badge-green';
    }
    return { str, badge };
  }

  return {
    lapNum: targetLap.lap || '-',
    s1: formatSecBadge(targetLap.s1, pbS1, sessionBests ? sessionBests.bestS1 : null),
    s2: formatSecBadge(targetLap.s2, pbS2, sessionBests ? sessionBests.bestS2 : null),
    s3: formatSecBadge(targetLap.s3, pbS3, sessionBests ? sessionBests.bestS3 : null)
  };
}

// ══════════════════════════════════════════════════════════════
// TIMING TABLES UPDATE:
// View 1 (Full Standings) & View 2 (Compact Side Standings)
// ══════════════════════════════════════════════════════════════
function updateTimingTablesRace() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const ranked = driverKeys.map(k => {
    const d = DRIVERS[k];
    const prog = getDriverProgress(k, currentSecond);
    const gridIndex = GRID_ORDER.indexOf(k) + 1;
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const curLap = getDriverLap(k, currentSecond);
    const completedStops = PIT_STOPS.filter(p => p.driver === k && currentSecond >= p.endSec).length;
    const isCurrentStop = PIT_STOPS.some(p => p.driver === k && currentSecond >= p.startSec && currentSecond <= p.endSec);

    return {
      key: k,
      driver: d,
      totalProgress: prog,
      inPit: state.inPit,
      isRetired: state.isRetired,
      retiredLap: RETIREMENTS[k]?.lap,
      tireInfo: tireInfo,
      curLap: curLap,
      gridPos: gridIndex,
      stats: stats,
      state: state,
      pitCount: completedStops + (isCurrentStop ? 1 : 0)
    };
  }).sort((a, b) => b.totalProgress - a.totalProgress);

  const leaderProgress = ranked[0].totalProgress;
  const leaderLap = ranked[0].curLap;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;
    const posDelta = r.gridPos - currentPos;

    let deltaHtml = '<span class="delta-none">-</span>';
    if (!r.isRetired && posDelta > 0) {
      deltaHtml = '<span class="delta-up">▲' + posDelta + '</span>';
    } else if (!r.isRetired && posDelta < 0) {
      deltaHtml = '<span class="delta-down">▼' + Math.abs(posDelta) + '</span>';
    }

    // Gap to leader (CON GESTIONE DOPPIATI +X LAP)
    let gapStr;
    if (r.isRetired) {
      gapStr = '<span class="dnf-badge">DNF (L' + r.retiredLap + ')</span>';
    } else if (idx === 0) {
      gapStr = 'LEADER';
    } else {
      const lapDiff = Math.max(0, leaderLap - r.curLap);
      const diff = leaderProgress - r.totalProgress;
      const lapsBehind = Math.max(lapDiff, Math.floor(diff / LAP_DURATION));

      if (lapsBehind >= 1) {
        gapStr = '+' + lapsBehind + ' LAP';
      } else {
        gapStr = '+' + diff.toFixed(3) + 's';
      }
    }

    // Interval to car ahead
    let intStr = '-';
    if (idx === 0) {
      intStr = '-';
    } else if (r.isRetired) {
      intStr = 'DNF';
    } else {
      const prev = ranked[idx - 1];
      if (prev.isRetired) {
        intStr = '-';
      } else {
        const intDiff = prev.totalProgress - r.totalProgress;
        const prevLapDiff = Math.max(0, prev.curLap - r.curLap);
        const intLaps = Math.max(prevLapDiff, Math.floor(intDiff / LAP_DURATION));
        if (intLaps >= 1) {
          intStr = '+' + intLaps + ' LAP';
        } else {
          intStr = '+' + Math.max(0.001, intDiff).toFixed(3) + 's';
        }
      }
    }

    // Tyre badge HTML
    const tInfo = r.tireInfo;
    const tyreBadgeHtml = '<div class="tyre-badge-box">'
      + '<span class="tyre-badge tyre-' + tInfo.code + '">' + tInfo.code + '</span>'
      + '<span class="tyre-age">' + tInfo.age + 'g</span>'
      + '</div>';

    const rowRetiredClass = r.isRetired ? 'row-retired ' : '';
    const gapColorClass = idx === 0 ? 'gap-leader' : (r.isRetired ? 'gap-retired' : '');

    // 1. Build View 1 (Full Standings: NO FULL NAMES, COLUMNS: DISTACCO, INTERVALLO, ULTIMO, MIGLIOR, GOMMA, PIT)
    fullHtml += '<tr class="' + rowRetiredClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="delta-cell">' + deltaHtml + '</td>'
      + '<td class="driver-cell-full"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td class="gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-count-cell">' + r.pitCount + '</td>'
      + '</tr>';

    if (r.key === expandedDriverKey) {
      const secTimes = getDriverSectorTimes(r.key, currentSecond, sessionBests);
      fullHtml += '<tr class="sector-accordion-row">'
        + '<td colspan="9" class="sector-accordion-cell">'
        + '<div class="sector-accordion-card">'
        + '<div class="sec-card-header">'
        + '<span>⏱️ INTERTEMPI SETTORI — ' + d.code + ' (GIRO ' + secTimes.lapNum + ')</span>'
        + '</div>'
        + '<div class="sec-card-sectors">'
        + '<div class="sec-box"><span class="sec-label">S1:</span> ' + renderBadge(secTimes.s1.str, secTimes.s1.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S2:</span> ' + renderBadge(secTimes.s2.str, secTimes.s2.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S3:</span> ' + renderBadge(secTimes.s3.str, secTimes.s3.badge) + '</div>'
        + '</div>'
        + '</div>'
        + '</td>'
        + '</tr>';
    }

    // 2. Build View 2 (Compact Side Standings: EYE BUTTON, POS, +/-, PILOTA, DISTACCO, INTERVALLO, ULTIMO, MIGLIORE, GOMMA)
    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    compactHtml += '<tr class="' + rowRetiredClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \\'' + r.key + '\\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="delta-cell">' + deltaHtml + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="col-side-int int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTablesFP() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const ranked = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const laps = DRIVER_LAPS[k] || [];
    const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= currentSecond && l.dur && l.dur > 50).length;

    return {
      key: k,
      driver: d,
      personalBest: stats.personalBest,
      stats: stats,
      state: state,
      tireInfo: tireInfo,
      lapsCount: completedLaps
    };
  }).sort((a, b) => {
    if (a.personalBest < Infinity && b.personalBest < Infinity) {
      return a.personalBest - b.personalBest;
    }
    if (a.personalBest < Infinity) return -1;
    if (b.personalBest < Infinity) return 1;
    return parseInt(a.driver.number || '99', 10) - parseInt(b.driver.number || '99', 10);
  });

  const leaderBest = (ranked.length > 0 && ranked[0].personalBest < Infinity) ? ranked[0].personalBest : null;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;

    let gapStr = '-';
    let intStr = '-';
    if (r.personalBest < Infinity && leaderBest < Infinity) {
      if (idx === 0) {
        gapStr = 'LEADER';
        intStr = '-';
      } else {
        gapStr = '+' + (r.personalBest - leaderBest).toFixed(3) + 's';
        const prev = ranked[idx - 1];
        if (prev && prev.personalBest < Infinity) {
          intStr = '+' + Math.max(0.001, r.personalBest - prev.personalBest).toFixed(3) + 's';
        }
      }
    }

    let statusBadgeHtml = '';
    if (r.state.isRetired) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">STOP</span>';
    } else if (r.state.inPit) {
      statusBadgeHtml = '<span class="status-badge status-inpit">IN PIT</span>';
    } else if (r.state.isOutLapMerge) {
      statusBadgeHtml = '<span class="status-badge status-outlap">OUT-LAP</span>';
    } else {
      statusBadgeHtml = '<span class="status-badge status-ontrack">IN PISTA</span>';
    }

    const tInfo = r.tireInfo;
    const tyreBadgeHtml = '<div class="tyre-badge-box">'
      + '<span class="tyre-badge tyre-' + tInfo.code + '">' + tInfo.code + '</span>'
      + '<span class="tyre-age">' + tInfo.age + 'g</span>'
      + '</div>';

    const gapColorClass = idx === 0 ? 'gap-leader' : '';

    fullHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="driver-cell-full"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td class="gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-count-cell">' + r.lapsCount + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';

    if (r.key === expandedDriverKey) {
      const secTimes = getDriverSectorTimes(r.key, currentSecond, sessionBests);
      fullHtml += '<tr class="sector-accordion-row">'
        + '<td colspan="9" class="sector-accordion-cell">'
        + '<div class="sector-accordion-card">'
        + '<div class="sec-card-header">'
        + '<span>⏱️ INTERTEMPI SETTORI — ' + d.code + ' (GIRO ' + secTimes.lapNum + ')</span>'
        + '</div>'
        + '<div class="sec-card-sectors">'
        + '<div class="sec-box"><span class="sec-label">S1:</span> ' + renderBadge(secTimes.s1.str, secTimes.s1.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S2:</span> ' + renderBadge(secTimes.s2.str, secTimes.s2.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S3:</span> ' + renderBadge(secTimes.s3.str, secTimes.s3.badge) + '</div>'
        + '</div>'
        + '</div>'
        + '</td>'
        + '</tr>';
    }

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    compactHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \\'' + r.key + '\\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTablesQ() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const q1Cut = SESSION_DURATION * 0.35;
  const q2Cut = SESSION_DURATION * 0.70;
  let activeQ = 'Q1';
  if (sessionPart === 'Q3' || (sessionPart === '' && currentSecond >= q2Cut)) {
    activeQ = 'Q3';
  } else if (sessionPart === 'Q2' || (sessionPart === '' && currentSecond >= q1Cut)) {
    activeQ = 'Q2';
  }

  const qDrivers = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const laps = DRIVER_LAPS[k] || [];
    const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= currentSecond && l.dur && l.dur > 50);

    const q1Laps = completedLaps.filter(l => l.startSec < q1Cut);
    const q2Laps = completedLaps.filter(l => l.startSec >= q1Cut && l.startSec < q2Cut);
    const q3Laps = completedLaps.filter(l => l.startSec >= q2Cut);

    const q1Best = q1Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const q2Best = q2Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const q3Best = q3Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const overallBest = stats.personalBest;

    return {
      key: k,
      driver: d,
      q1Best: q1Best < Infinity ? q1Best : (completedLaps.length > 0 ? completedLaps[0].dur : Infinity),
      q2Best: q2Best,
      q3Best: q3Best,
      overallBest: overallBest,
      stats: stats,
      state: state,
      tireInfo: tireInfo,
      lapsCount: completedLaps.length
    };
  });

  const q1Sorted = [...qDrivers].sort((a, b) => a.q1Best - b.q1Best);

  let ranked = [];
  if (activeQ === 'Q1') {
    ranked = q1Sorted;
  } else if (activeQ === 'Q2') {
    const top15 = q1Sorted.slice(0, 15).sort((a, b) => {
      const aVal = a.q2Best < Infinity ? a.q2Best : a.q1Best;
      const bVal = b.q2Best < Infinity ? b.q2Best : b.q1Best;
      return aVal - bVal;
    });
    const elimQ1 = q1Sorted.slice(15);
    ranked = [...top15, ...elimQ1];
  } else {
    const q2Sorted = q1Sorted.slice(0, 15).sort((a, b) => {
      const aVal = a.q2Best < Infinity ? a.q2Best : a.q1Best;
      const bVal = b.q2Best < Infinity ? b.q2Best : b.q1Best;
      return aVal - bVal;
    });
    const top10 = q2Sorted.slice(0, 10).sort((a, b) => {
      const aVal = a.q3Best < Infinity ? a.q3Best : (a.q2Best < Infinity ? a.q2Best : a.q1Best);
      const bVal = b.q3Best < Infinity ? b.q3Best : (b.q2Best < Infinity ? b.q2Best : b.q1Best);
      return aVal - bVal;
    });
    const elimQ2 = q2Sorted.slice(10);
    const elimQ1 = q1Sorted.slice(15);
    ranked = [...top10, ...elimQ2, ...elimQ1];
  }

  let bestQ1Val = Infinity, bestQ2Val = Infinity, bestQ3Val = Infinity;
  ranked.forEach(r => {
    if (r.q1Best < bestQ1Val) bestQ1Val = r.q1Best;
    if (r.q2Best < bestQ2Val) bestQ2Val = r.q2Best;
    if (r.q3Best < bestQ3Val) bestQ3Val = r.q3Best;
  });

  const leaderTime = (ranked[0] && ranked[0].overallBest < Infinity) ? ranked[0].overallBest : null;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;

    let isEliminated = false;
    if (activeQ === 'Q1' && currentPos > 15) {
      isEliminated = true;
    } else if (activeQ === 'Q2' && currentPos > 10) {
      isEliminated = true;
    } else if (activeQ === 'Q3' && currentPos > 10) {
      isEliminated = true;
    }

    let gapStr = '-';
    if (r.overallBest < Infinity && leaderTime < Infinity) {
      if (idx === 0) {
        gapStr = 'LEADER';
      } else {
        gapStr = '+' + (r.overallBest - leaderTime).toFixed(3) + 's';
      }
    }

    let statusBadgeHtml = '';
    if (isEliminated) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">OUT</span>';
    } else if (r.state.isRetired) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">STOP</span>';
    } else if (r.state.inPit) {
      statusBadgeHtml = '<span class="status-badge status-inpit">IN PIT</span>';
    } else if (r.state.isOutLapMerge) {
      statusBadgeHtml = '<span class="status-badge status-outlap">OUT-LAP</span>';
    } else {
      statusBadgeHtml = '<span class="status-badge status-flying">IN PISTA</span>';
    }

    function formatQBadge(val, bestVal) {
      if (!val || val === Infinity) return '<span class="dash-muted">-</span>';
      const isPurple = Math.abs(val - bestVal) < 0.005;
      return renderBadge(formatLapTime(val), isPurple ? 'badge-purple' : 'badge-green');
    }

    const q1Html = formatQBadge(r.q1Best, bestQ1Val);
    const q2Html = (currentPos <= 15 || r.q2Best < Infinity) ? formatQBadge(r.q2Best, bestQ2Val) : '<span class="dash-muted">-</span>';
    const q3Html = (currentPos <= 10 || r.q3Best < Infinity) ? formatQBadge(r.q3Best, bestQ3Val) : '<span class="dash-muted">-</span>';

    const tInfo = r.tireInfo;
    const tyreBadgeHtml = '<div class="tyre-badge-box">'
      + '<span class="tyre-badge tyre-' + tInfo.code + '">' + tInfo.code + '</span>'
      + '<span class="tyre-age">' + tInfo.age + 'g</span>'
      + '</div>';

    const rowElimClass = isEliminated ? 'row-eliminated ' : '';
    const gapColorClass = idx === 0 ? 'gap-leader' : '';

    if (activeQ === 'Q1' && idx === 15) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">⚠️ ZONA ELIMINAZIONE Q1 (P16 - P22)</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">⚠️ ZONA ELIMINAZIONE Q1</td></tr>';
    } else if (activeQ === 'Q2' && idx === 10) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">⚠️ ZONA ELIMINAZIONE Q2 (P11 - P15)</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">⚠️ ZONA ELIMINAZIONE Q2</td></tr>';
    } else if (activeQ === 'Q2' && idx === 15) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">🛑 ELIMINATI IN Q1</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">🛑 ELIMINATI IN Q1</td></tr>';
    } else if (activeQ === 'Q3' && idx === 10) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">🛑 ELIMINATI IN Q2</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">🛑 ELIMINATI IN Q2</td></tr>';
    } else if (activeQ === 'Q3' && idx === 15) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">🛑 ELIMINATI IN Q1</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">🛑 ELIMINATI IN Q1</td></tr>';
    }

    fullHtml += '<tr class="' + rowElimClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="driver-cell-full"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + q1Html + '</td>'
      + '<td>' + q2Html + '</td>'
      + '<td>' + q3Html + '</td>'
      + '<td class="gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-count-cell">' + r.lapsCount + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';

    if (r.key === expandedDriverKey) {
      const secTimes = getDriverSectorTimes(r.key, currentSecond, sessionBests);
      fullHtml += '<tr class="sector-accordion-row">'
        + '<td colspan="9" class="sector-accordion-cell">'
        + '<div class="sector-accordion-card">'
        + '<div class="sec-card-header">'
        + '<span>⏱️ INTERTEMPI SETTORI — ' + d.code + ' (GIRO ' + secTimes.lapNum + ')</span>'
        + '</div>'
        + '<div class="sec-card-sectors">'
        + '<div class="sec-box"><span class="sec-label">S1:</span> ' + renderBadge(secTimes.s1.str, secTimes.s1.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S2:</span> ' + renderBadge(secTimes.s2.str, secTimes.s2.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S3:</span> ' + renderBadge(secTimes.s3.str, secTimes.s3.badge) + '</div>'
        + '</div>'
        + '</div>'
        + '</td>'
        + '</tr>';
    }

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    const displayTimeStr = r.overallBest < Infinity ? formatLapTime(r.overallBest) : '-';
    const displayBadge = (leaderTime && Math.abs(r.overallBest - leaderTime) < 0.005) ? 'badge-purple' : 'badge-green';

    compactHtml += '<tr class="' + rowElimClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\\'' + r.key + '\\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \\'' + r.key + '\\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(displayTimeStr, displayBadge) + '</td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTables() {
  if (sessionType === 'practice') {
    updateTimingTablesFP();
  } else if (sessionType === 'qualifying') {
    updateTimingTablesQ();
  } else {
    updateTimingTablesRace();
  }
}

function onDriverRowClick(drvKey) {
  expandedDriverKey = (expandedDriverKey === drvKey) ? null : drvKey;
  focusedDriver = drvKey;
  selectedDriver1 = drvKey;
  const s1 = document.getElementById('drv1Select');
  if (s1) s1.value = drvKey;
  updateCockpit(1, selectedDriver1);
  updateTimingTables();
  drawTrack();
}

function onEyeButtonClick(event, drvKey) {
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }

  // Se cliccato nuovamente sul bottone dell'occhio attualmente attivo, torna alla vista globale
  if (isMapFollowingDriver && focusedDriver === drvKey) {
    resetMapFocus();
    return;
  }

  // Passa in stato "selected" con focus sul placeholder del pilota
  focusedDriver = drvKey;
  selectedDriver1 = drvKey;
  isMapFollowingDriver = true;
  userZoom = 2.4;

  const resetBtn = document.getElementById('btnResetMapFocus');
  if (resetBtn) resetBtn.style.display = 'inline-flex';

  updateZoomButtonsState();
  centerMapOnDriver(drvKey);

  const s1 = document.getElementById('drv1Select');
  if (s1) s1.value = drvKey;
  updateCockpit(1, drvKey);

  updateTimingTables();
  drawTrack();
}
window.onEyeButtonClick = onEyeButtonClick;

function resetMapFocus() {
  isMapFollowingDriver = false;
  focusedDriver = null;
  userZoom = 1.0;
  userPanX = 0;
  userPanY = 0;

  const resetBtn = document.getElementById('btnResetMapFocus');
  if (resetBtn) resetBtn.style.display = 'none';

  updateZoomButtonsState();
  updateZoomPill();
  updateTimingTables();
  drawTrack();
}
window.resetMapFocus = resetMapFocus;

function centerMapOnDriver(drvKey) {
  const state = getDriverVisualState(drvKey, currentSecond);
  if (!state) return;
  const baseX = canvasOffsetX + state.px * canvasScale;
  const baseY = canvasOffsetY + state.py * canvasScale;
  const cx = (canvasCssWidth || 400) / 2;
  const cy = (canvasCssHeight || 300) / 2;
  userPanX = -(baseX - cx) * userZoom;
  userPanY = -(baseY - cy) * userZoom;
}

// ══════════════════════════════════════════════════════════════
// DUAL COCKPIT TELEMETRY
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
  const tireInfo = getDriverTireInfo(drvKey, currentSecond);
  const p = 'c' + idx;

  // Driver info
  const dot = document.getElementById(p + 'ColorDot');
  if (dot) dot.style.background = drv.color;
  const title = document.getElementById(p + 'Title');
  if (title) title.textContent = '#' + drv.number + ' ' + drv.code + ' — ' + drv.firstName + ' ' + drv.lastName;
  const team = document.getElementById(p + 'Team');
  if (team) team.textContent = drv.team + ' [' + tireInfo.code + ']';

  // Speed & Gear
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
      if (i < 5) led.classList.add('green');
      else if (i < 10) led.classList.add('yellow');
      else if (i < 13) led.classList.add('red');
      else led.classList.add('blue');
    }
  });

  // Throttle & Brake
  const thrBar = document.getElementById(p + 'ThrBar');
  const thrVal = document.getElementById(p + 'ThrVal');
  if (thrBar) thrBar.style.width = Math.round(state.throttle) + '%';
  if (thrVal) thrVal.textContent = Math.round(state.throttle) + '%';

  const brkBar = document.getElementById(p + 'BrkBar');
  const brkVal = document.getElementById(p + 'BrkVal');
  if (brkBar) brkBar.style.width = Math.round(state.brake) + '%';
  if (brkVal) brkVal.textContent = Math.round(state.brake) + '%';

  // 2026 Active Aero
  const aeroBadge = document.getElementById(p + 'AeroBadge');
  if (aeroBadge) {
    if (state.drs > 0 && state.speed > 250) {
      aeroBadge.textContent = 'X-MODE (LOW DRAG)';
      aeroBadge.className = 'aero-mode-badge aero-xmode';
    } else {
      aeroBadge.textContent = 'Z-MODE (HIGH DOWNFORCE)';
      aeroBadge.className = 'aero-mode-badge aero-zmode';
    }
  }

  // ERS Battery SoC%
  const socVal = document.getElementById(p + 'SocVal');
  const socBar = document.getElementById(p + 'SocBar');
  const curLap = getDriverLap(drvKey, currentSecond);
  const estSoc = Math.max(25, Math.min(98, 92 - ((curLap * 3) % 45) + (state.brake > 50 ? 12 : -5)));
  if (socVal) socVal.textContent = Math.round(estSoc) + '%';
  if (socBar) socBar.style.width = Math.round(estSoc) + '%';

  // 2026 Manual Override / Boost Mode
  const boostBadge = document.getElementById(p + 'BoostBadge');
  if (boostBadge) {
    const gap = getDriverGap(drvKey, currentSecond);
    if (gap > 0 && gap < 1.0 && estSoc > 35 && state.throttle > 90) {
      boostBadge.textContent = 'BOOST ATTIVO (MOM)';
      boostBadge.className = 'boost-pill boost-active';
    } else if (estSoc > 40) {
      boostBadge.textContent = 'DISPONIBILE';
      boostBadge.className = 'boost-pill boost-avail';
    } else {
      boostBadge.textContent = 'RICARICA ERS';
      boostBadge.className = 'boost-pill boost-standby';
    }
  }

  // Location
  const locEl = document.getElementById(p + 'LocText');
  if (locEl) locEl.textContent = '📍 ' + (state.location || 'Settore di Pista');
}

function onDriverSelectChange(idx, val) {
  if (idx === 1) selectedDriver1 = val;
  if (idx === 2) selectedDriver2 = val;
  updateCockpit(idx, val);
  updateTimingTables();
  drawTrack();
}

function swapSelectedDrivers() {
  const tmp = selectedDriver1;
  selectedDriver1 = selectedDriver2;
  selectedDriver2 = tmp;
  const s1 = document.getElementById('drv1Select');
  const s2 = document.getElementById('drv2Select');
  if (s1) s1.value = selectedDriver1;
  if (s2) s2.value = selectedDriver2;
  updateCockpit(1, selectedDriver1);
  updateCockpit(2, selectedDriver2);
  updateTimingTables();
  drawTrack();
}

// ══════════════════════════════════════════════════════════════
// RACE CONTROL & INCIDENT LOGS
// ══════════════════════════════════════════════════════════════
function updateRaceControl() {
  const activeMsgs = RACE_CONTROL_MESSAGES.filter(m => (m.timeSec || 0) <= currentSecond);
  const totalCount = document.getElementById('rcTotalCount');
  if (totalCount) totalCount.textContent = activeMsgs.length + ' Messaggi';

  const latest = activeMsgs.length > 0 ? activeMsgs[activeMsgs.length - 1] : null;
  const tickerText = latest
    ? ('🟡 ' + formatSimClock(latest.timeSec || currentSecond) + ' — ' + (latest.message || 'Direzione Gara'))
    : '🟢 08:33:00 — Inizio Sessione Ufficiale';

  const sTicker = document.getElementById('sideRcTickerCard');
  const fTicker = document.getElementById('fullRcTickerCard');
  if (sTicker) sTicker.textContent = tickerText;
  if (fTicker) fTicker.textContent = tickerText;

  const fList = document.getElementById('fullRcFeedList');
  const sList = document.getElementById('sideRcFeedList');
  if (!fList && !sList) return;

  const filtered = activeMsgs.filter(m => {
    if (activeRcFilter === 'ALL') return true;
    if (activeRcFilter === 'Flag') return m.flag || m.category === 'Flag';
    if (activeRcFilter === 'SafetyCar') return (m.category === 'SafetyCar') || (m.message && m.message.includes('SAFETY CAR'));
    if (activeRcFilter === 'Investigation') return (m.category === 'Investigation') || (m.message && m.message.includes('INVESTIGAT'));
    return true;
  }).reverse();

  const itemsHtml = filtered.slice(0, 40).map(m => {
    let flagClass = '';
    if (m.flag === 'YELLOW' || (m.message && m.message.includes('YELLOW'))) flagClass = 'yellow';
    else if (m.flag === 'GREEN' || (m.message && m.message.includes('GREEN'))) flagClass = 'green';
    else if (m.flag === 'RED' || (m.message && m.message.includes('RED'))) flagClass = 'red';
    else if (m.category === 'SafetyCar' || (m.message && m.message.includes('SAFETY CAR'))) flagClass = 'sc';

    return '<div class="rc-msg-card ' + flagClass + '">'
      + '<div class="rc-msg-meta">'
      + '<span>' + (m.category || 'DIREZIONE GARA') + (m.lap ? (' • GIRO ' + m.lap) : '') + '</span>'
      + '<span>' + formatSimClock(m.timeSec || currentSecond) + '</span>'
      + '</div>'
      + '<div class="rc-msg-text">' + m.message + '</div>'
      + '</div>';
  }).join('');

  if (fList) fList.innerHTML = itemsHtml;
  if (sList) sList.innerHTML = itemsHtml;
}

function setRcFilter(cat) {
  activeRcFilter = cat;
  document.querySelectorAll('.rc-filter-btn').forEach(b => b.classList.remove('active'));
  const activeBtn = document.getElementById({
    ALL: 'rcFilterAll',
    Flag: 'rcFilterFlags',
    SafetyCar: 'rcFilterSC',
    Investigation: 'rcFilterInv'
  }[cat]);
  if (activeBtn) activeBtn.classList.add('active');
  updateRaceControl();
}

// ══════════════════════════════════════════════════════════════
// PERSISTENT TOP STATUS BAR UPDATE
// ══════════════════════════════════════════════════════════════
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
      flagIcon.textContent = '🟠';
      flagText.textContent = 'SAFETY CAR';
    } else if (vscActive) {
      flagBadge.classList.add('flag-vsc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'VIRTUAL SAFETY CAR';
    } else if (currentSecond >= RACE_FINISH_SEC) {
      flagBadge.classList.add('flag-chequered');
      flagIcon.textContent = '🏁';
      flagText.textContent = 'BANDIERA A SCACCHI';
    } else {
      flagBadge.classList.add('flag-green');
      flagIcon.textContent = '🟢';
      flagText.textContent = 'BANDIERA VERDE';
    }
  }

  const trackFlagBadge = document.getElementById('trackFlagBadge');
  if (trackFlagBadge) {
    if (scActive) {
      trackFlagBadge.textContent = 'SAFETY CAR';
      trackFlagBadge.className = 'status-pill sc';
    } else if (vscActive) {
      trackFlagBadge.textContent = 'VSC ATTIVA';
      trackFlagBadge.className = 'status-pill yellow';
    } else {
      trackFlagBadge.textContent = 'BANDIERA VERDE';
      trackFlagBadge.className = 'status-pill green';
    }
  }

  // Session Type Label
  const sessVal = document.getElementById('statusBarSessionVal');
  if (sessVal) {
    if (sessionType === 'practice') {
      sessVal.textContent = sessionPart || 'PROVE LIBERE';
    } else if (sessionType === 'qualifying') {
      sessVal.textContent = sessionPart || 'QUALIFICHE';
    } else {
      sessVal.textContent = 'GARA';
    }
  }

  // Session Progress / Laps
  const lapVal = document.getElementById('statusBarLapVal');
  if (lapVal) {
    if (sessionType === 'practice') {
      lapVal.textContent = 'SESSIONE ATTIVA';
    } else if (sessionType === 'qualifying') {
      const q1Cut = SESSION_DURATION * 0.35;
      const q2Cut = SESSION_DURATION * 0.70;
      let qPhase = 'Q1';
      if (sessionPart) qPhase = sessionPart;
      else if (currentSecond >= q2Cut) qPhase = 'Q3';
      else if (currentSecond >= q1Cut) qPhase = 'Q2';
      lapVal.textContent = qPhase + ' ATTIVA';
    } else {
      if (currentSim.circuitId === 'sepang' && currentSecond < 216.9) {
        lapVal.textContent = 'GIRI FORMAZIONE';
      } else {
        const curLap = getDriverLap(focusedDriver, currentSecond);
        lapVal.textContent = 'GIRO ' + curLap + '/' + TOTAL_LAPS;
      }
    }
  }

  // UTC Clock
  const clockVal = document.getElementById('statusBarClockVal');
  if (clockVal) clockVal.textContent = formatSimClock(currentSecond) + ' UTC';
}

function formatSimClock(sec) {
  const baseHour = currentSim.circuitId === 'baku' ? 11 : 8;
  const baseMin = currentSim.circuitId === 'baku' ? 3 : 33;
  const totalSec = Math.floor(sec) + (baseHour * 3600) + (baseMin * 60);
  const h = Math.floor(totalSec / 3600) % 24;
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  return (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
}

// ══════════════════════════════════════════════════════════════
// CANVAS TRACK MAP RENDERING
// ══════════════════════════════════════════════════════════════
let canvas, ctx;
let canvasScale = 1.0;
let canvasOffsetX = 0;
let canvasOffsetY = 0;
let canvasCssWidth = 0;
let canvasCssHeight = 0;

let userZoom = 1.0;
let userPanX = 0;
let userPanY = 0;

function resizeTrackCanvas() {
  const pane = document.getElementById('trackPane');
  if (!pane || !canvas) return;

  const rect = pane.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  const w = Math.floor(rect.width);
  const h = Math.floor(rect.height);

  if (w <= 0 || h <= 0) return;

  canvasCssWidth = w;
  canvasCssHeight = h;

  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';

  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.scale(dpr, dpr);

  const margin = 16;
  const availW = w - margin * 2;
  const availH = h - margin * 2;
  canvasScale = Math.min(availW / SVG_WIDTH, availH / SVG_HEIGHT);
  canvasOffsetX = margin + (availW - SVG_WIDTH * canvasScale) / 2;
  canvasOffsetY = margin + (availH - SVG_HEIGHT * canvasScale) / 2;
}

function pxToScreen(px, py) {
  const baseX = canvasOffsetX + px * canvasScale;
  const baseY = canvasOffsetY + py * canvasScale;
  const cx = (canvasCssWidth || 400) / 2;
  const cy = (canvasCssHeight || 300) / 2;
  return {
    x: cx + (baseX - cx) * userZoom + userPanX,
    y: cy + (baseY - cy) * userZoom + userPanY
  };
}

function updateZoomPill() {
  const pill = document.getElementById('zoomLevelPill');
  if (pill) {
    pill.innerText = userZoom.toFixed(1) + 'x';
    pill.style.color = userZoom > 1.05 ? '#58a6ff' : '#8b949e';
  }
}

function updateZoomButtonsState() {
  const badge = document.getElementById('mapZoomVerticalBadge');
  if (badge) {
    badge.style.display = isMapFollowingDriver ? 'flex' : 'none';
  }
  const btnIn = document.getElementById('btnMapZoomIn');
  const btnOut = document.getElementById('btnMapZoomOut');
  if (btnIn) {
    if (userZoom >= 4.95) {
      btnIn.classList.add('disabled');
    } else {
      btnIn.classList.remove('disabled');
    }
  }
  if (btnOut) {
    if (userZoom <= 1.05) {
      btnOut.classList.add('disabled');
    } else {
      btnOut.classList.remove('disabled');
    }
  }
}
window.updateZoomButtonsState = updateZoomButtonsState;

function zoomMapStep(direction) {
  if (direction > 0) {
    userZoom = Math.min(5.0, userZoom + 0.6);
  } else {
    userZoom = Math.max(1.0, userZoom - 0.6);
  }
  if (userZoom <= 1.05) {
    userZoom = 1.0;
    userPanX = 0;
    userPanY = 0;
  }
  updateZoomButtonsState();
  updateZoomPill();
  drawTrack();
}
window.zoomMapStep = zoomMapStep;

function zoomMap(factor) {
  userZoom = Math.min(5.0, Math.max(1.0, userZoom * factor));
  if (userZoom <= 1.01) {
    userZoom = 1.0;
    userPanX = 0;
    userPanY = 0;
  }
  updateZoomButtonsState();
  updateZoomPill();
  drawTrack();
}
window.zoomMap = zoomMap;

function resetMapZoom() {
  userZoom = 1.0;
  userPanX = 0;
  userPanY = 0;
  updateZoomButtonsState();
  updateZoomPill();
  drawTrack();
}
window.resetMapZoom = resetMapZoom;

function setupTrackMapInteractions() {
  const cvs = document.getElementById('trackCanvas');
  if (!cvs) return;

  let initialTouchDist = null;
  let initialZoom = 1.0;
  let lastTouchPos = null;
  let lastTapTime = 0;
  let isDragging = false;
  let lastMousePos = null;

  // Touch handlers: pinch-to-zoom con due dita e pan con un dito se ingrandito
  cvs.addEventListener('touchstart', function(e) {
    if (e.touches.length === 2) {
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      initialTouchDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      initialZoom = userZoom;
    } else if (e.touches.length === 1) {
      const now = Date.now();
      if (now - lastTapTime < 300) {
        // Doppio tap: toggle rapido tra 1.0x e 2.2x
        if (userZoom > 1.05) {
          resetMapFocus();
        } else {
          userZoom = 2.2;
          userPanX = 0;
          userPanY = 0;
          updateZoomButtonsState();
          updateZoomPill();
          drawTrack();
        }
        lastTapTime = 0;
        return;
      }
      lastTapTime = now;
      lastTouchPos = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }
  }, { passive: false });

  cvs.addEventListener('touchmove', function(e) {
    if (e.touches.length === 2 && initialTouchDist) {
      e.preventDefault();
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      const currentDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      const scale = currentDist / initialTouchDist;
      userZoom = Math.min(5.0, Math.max(1.0, initialZoom * scale));
      if (userZoom <= 1.01) {
        userZoom = 1.0;
        userPanX = 0;
        userPanY = 0;
      }
      updateZoomButtonsState();
      updateZoomPill();
      drawTrack();
    } else if (e.touches.length === 1 && lastTouchPos) {
      if (userZoom > 1.05) {
        e.preventDefault();
        if (isMapFollowingDriver) {
          isMapFollowingDriver = false;
          updateTimingTables();
        }
        const dx = e.touches[0].clientX - lastTouchPos.x;
        const dy = e.touches[0].clientY - lastTouchPos.y;
        userPanX += dx;
        userPanY += dy;
        const maxPan = ((canvasCssWidth || 400) / 2) * (userZoom - 1.0) + 40;
        userPanX = Math.max(-maxPan, Math.min(maxPan, userPanX));
        userPanY = Math.max(-maxPan, Math.min(maxPan, userPanY));
        lastTouchPos = { x: e.touches[0].clientX, y: e.touches[0].clientY };
        drawTrack();
      }
    }
  }, { passive: false });

  cvs.addEventListener('touchend', function(e) {
    if (e.touches.length < 2) {
      initialTouchDist = null;
    }
    if (e.touches.length === 0) {
      lastTouchPos = null;
      if (userZoom <= 1.01) {
        userZoom = 1.0;
        userPanX = 0;
        userPanY = 0;
        updateZoomButtonsState();
        updateZoomPill();
        drawTrack();
      }
    }
  }, { passive: false });

  // Mouse wheel handler per test su computer desktop
  cvs.addEventListener('wheel', function(e) {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.15 : 0.85;
    zoomMap(factor);
  }, { passive: false });

  // Mouse drag handler per desktop
  cvs.addEventListener('mousedown', function(e) {
    if (userZoom > 1.05) {
      isDragging = true;
      lastMousePos = { x: e.clientX, y: e.clientY };
    }
  });

  window.addEventListener('mousemove', function(e) {
    if (isDragging && lastMousePos && userZoom > 1.05) {
      if (isMapFollowingDriver) {
        isMapFollowingDriver = false;
        updateTimingTables();
      }
      const dx = e.clientX - lastMousePos.x;
      const dy = e.clientY - lastMousePos.y;
      userPanX += dx;
      userPanY += dy;
      const maxPan = ((canvasCssWidth || 400) / 2) * (userZoom - 1.0) + 40;
      userPanX = Math.max(-maxPan, Math.min(maxPan, userPanX));
      userPanY = Math.max(-maxPan, Math.min(maxPan, userPanY));
      lastMousePos = { x: e.clientX, y: e.clientY };
      drawTrack();
    }
  });

  window.addEventListener('mouseup', function() {
    isDragging = false;
    lastMousePos = null;
  });

  // Click / Tap sui placeholder dei piloti sul tracciato per selezionare la telemetria
  cvs.addEventListener('click', function(e) {
    if (isDragging) return;
    const rect = cvs.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    const driverKeys = Object.keys(DRIVERS);
    let closestDrv = null;
    let closestDist = 28; // Raggio hit-test in pixel

    driverKeys.forEach(k => {
      const state = getDriverVisualState(k, currentSecond);
      if ((sessionType === 'practice' || sessionType === 'qualifying') && state.inPit) {
        return;
      }
      const scr = pxToScreen(state.px, state.py);
      const dist = Math.hypot(scr.x - clickX, scr.y - clickY);
      if (dist < closestDist) {
        closestDist = dist;
        closestDrv = k;
      }
    });

    if (closestDrv) {
      onDriverRowClick(closestDrv);
    }
  });
}

function drawTrack() {
  if (!ctx || !canvas) return;

  if (isMapFollowingDriver && focusedDriver) {
    centerMapOnDriver(focusedDriver);
  }

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Track Outline Path
  ctx.save();
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';

  const zoomScale = Math.min(2.5, Math.sqrt(userZoom));

  // Racing line roadbed
  ctx.strokeStyle = '#2b313a';
  ctx.lineWidth = Math.max(3, 8 * canvasScale * zoomScale);
  ctx.beginPath();
  for (let i = 0; i < NODES.length; i++) {
    const pt = pxToScreen(NODES[i][1], NODES[i][2]);
    if (i === 0) ctx.moveTo(pt.x, pt.y);
    else ctx.lineTo(pt.x, pt.y);
  }
  ctx.closePath();
  ctx.stroke();

  // White centerline
  ctx.strokeStyle = '#586069';
  ctx.lineWidth = Math.max(1, 2 * canvasScale * zoomScale);
  ctx.beginPath();
  for (let i = 0; i < NODES.length; i++) {
    const pt = pxToScreen(NODES[i][1], NODES[i][2]);
    if (i === 0) ctx.moveTo(pt.x, pt.y);
    else ctx.lineTo(pt.x, pt.y);
  }
  ctx.closePath();
  ctx.stroke();

  // Start/Finish Line
  if (NODES.length > 0) {
    const sfPt = pxToScreen(NODES[0][1], NODES[0][2]);
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(sfPt.x, sfPt.y, 4 * zoomScale, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.restore();

  // Draw Safety Car Marker ONLY when actively deployed on track
  const scActive = isSafetyCarActive(currentSecond);
  if (scActive) {
    const driverKeys = Object.keys(DRIVERS);
    let leaderKey = '3';
    let maxProg = -1;
    driverKeys.forEach(k => {
      const p = getDriverProgress(k, currentSecond);
      if (p > maxProg) {
        maxProg = p;
        leaderKey = k;
      }
    });

    const leaderTrackSec = getDriverTrackSec(leaderKey, currentSecond);
    const scTrackSec = (leaderTrackSec + 1.8) % LAP_DURATION;
    const scPt = interpolateTrackNode(scTrackSec);
    const scScr = pxToScreen(scPt.px, scPt.py);

    ctx.save();
    ctx.shadowColor = '#f0883e';
    ctx.shadowBlur = 8;

    const scRadius = 6.5 * Math.min(1.5, Math.sqrt(userZoom));
    ctx.fillStyle = '#f0883e';
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(scScr.x, scScr.y, scRadius, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold ' + Math.round(8 * Math.min(1.4, Math.sqrt(userZoom))) + 'px ui-monospace, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('SC', scScr.x, scScr.y);

    ctx.fillStyle = '#f0883e';
    ctx.font = 'bold ' + Math.round(7 * Math.min(1.3, Math.sqrt(userZoom))) + 'px sans-serif';
    ctx.textBaseline = 'bottom';
    ctx.fillText('SAFETY CAR', scScr.x, scScr.y - (scRadius + 2));
    ctx.restore();
  }

  // Draw 22 Drivers on track
  const driverKeys = Object.keys(DRIVERS);
  driverKeys.forEach(k => {
    const d = DRIVERS[k];
    const state = getDriverVisualState(k, currentSecond);

    // In FP and Qualifying sessions, hide placeholders of drivers who are currently in the pit
    if ((sessionType === 'practice' || sessionType === 'qualifying') && state.inPit) {
      return;
    }

    const scr = pxToScreen(state.px, state.py);
    let isHighlighted = false;
    if (currentSideTab === 'side-telemetry') {
      isHighlighted = (k === selectedDriver1 || k === selectedDriver2);
    } else {
      isHighlighted = Boolean(focusedDriver && k === focusedDriver);
    }

    ctx.save();

    const baseR = isHighlighted ? 5.5 : 4.5;
    const dotR = baseR * Math.min(1.5, Math.sqrt(userZoom));

    if (isHighlighted) {
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(scr.x, scr.y, dotR + 2.5, 0, Math.PI * 2);
      ctx.stroke();
    }

    ctx.fillStyle = state.isRetired ? '#484f58' : d.color;
    ctx.beginPath();
    ctx.arc(scr.x, scr.y, dotR, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#f0f6fc';
    const fontSize = Math.round((isHighlighted ? 9 : 7.5) * Math.min(1.4, Math.sqrt(userZoom)));
    ctx.font = 'bold ' + fontSize + 'px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'bottom';
    ctx.fillText(d.code, scr.x, scr.y - (dotR + 2));

    ctx.restore();
  });
}

// ══════════════════════════════════════════════════════════════
// MAIN ANIMATION LOOP (60 FPS)
// ══════════════════════════════════════════════════════════════
let lastUiUpdateTime = 0;

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
    if (typeof updateScrubber === 'function') updateScrubber();
  }

  requestAnimationFrame(animate);
}

function renderSimulationControls(isTest) {
  const container = document.getElementById('hdrRightControls');
  if (!container) return;
  if (!isTest) {
    container.innerHTML = '';
    return;
  }
  const circuit = currentCircuit || 'sepang';
  container.innerHTML = '<div class="test-switch-box" id="testSwitchBox">'
    + '<button class="test-btn ' + (circuit === 'sepang' ? 'active' : '') + '" id="btnSimSepang" onclick="switchSimulationCircuit(\\'sepang\\')">🇲🇾 SEPANG</button>'
    + '<button class="test-btn ' + (circuit === 'baku' ? 'active' : '') + '" id="btnSimBaku" onclick="switchSimulationCircuit(\\'baku\\')">🇦🇿 BAKU</button>'
    + '</div>'
    + '<div class="test-switch-box" id="sessionTypeSwitchBox">'
    + '<button class="test-btn ' + (sessionType === 'race' ? 'active' : '') + '" id="btnTypeRace" onclick="setSessionType(\\'race\\')">GARA</button>'
    + '<button class="test-btn ' + (sessionType === 'qualifying' ? 'active' : '') + '" id="btnTypeQual" onclick="setSessionType(\\'qualifying\\')">QUALIFICHE</button>'
    + '<button class="test-btn ' + (sessionType === 'practice' ? 'active' : '') + '" id="btnTypeFP" onclick="setSessionType(\\'practice\\')">FP</button>'
    + '</div>'
    + '<div class="speed-multiplier-box" id="speedMultiplierBox">'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 1 ? 'active' : '') + '" id="btnSpd1x" onclick="setSimSpeed(1)">1x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 2 ? 'active' : '') + '" id="btnSpd2x" onclick="setSimSpeed(2)">2x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 5 ? 'active' : '') + '" id="btnSpd5x" onclick="setSimSpeed(5)">5x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 10 ? 'active' : '') + '" id="btnSpd10x" onclick="setSimSpeed(10)">10x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 30 ? 'active' : '') + '" id="btnSpd30x" onclick="setSimSpeed(30)">30x</button>'
    + '</div>';
}
window.renderSimulationControls = renderSimulationControls;

function setSimSpeed(mult) {
  simSpeedMultiplier = mult;
  ['btnSpd1x', 'btnSpd2x', 'btnSpd5x', 'btnSpd10x', 'btnSpd30x'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const activeBtn = document.getElementById('btnSpd' + mult + 'x');
  if (activeBtn) activeBtn.classList.add('active');
}
window.setSimSpeed = setSimSpeed;

// ══════════════════════════════════════════════════════════════
// NAVIGATION & VIEW SWITCHING
// ══════════════════════════════════════════════════════════════
function switchMainView(viewId) {
  currentMainView = viewId;
  document.querySelectorAll('.main-view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.bottom-nav-btn').forEach(b => b.classList.remove('active'));

  const target = document.getElementById(viewId);
  if (target) target.classList.add('active');

  if (viewId === 'view-standings') {
    const b = document.getElementById('btnNavStandings');
    if (b) b.classList.add('active');
  } else if (viewId === 'view-track') {
    const b = document.getElementById('btnNavTrack');
    if (b) b.classList.add('active');
    setTimeout(resizeTrackCanvas, 60);
  } else if (viewId === 'view-race-control') {
    const b = document.getElementById('btnNavRaceControl');
    if (b) b.classList.add('active');
  }

  // Notifica il bridge nativo Android per la gestione dell'orientamento
  if (window.FastestLapBridge && typeof window.FastestLapBridge.onTabChanged === 'function') {
    try {
      window.FastestLapBridge.onTabChanged(viewId);
    } catch (ignored) {}
  }
}

function switchSideTab(tabId) {
  currentSideTab = tabId;
  document.querySelectorAll('.side-pane-content').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.side-tab-btn').forEach(b => b.classList.remove('active'));

  const target = document.getElementById(tabId);
  if (target) target.classList.add('active');

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

// ══════════════════════════════════════════════════════════════
// INITIALIZATION & APP BRIDGE INTEGRATION
// ══════════════════════════════════════════════════════════════
let appInitialized = false;
function initApp() {
  if (appInitialized) return;
  appInitialized = true;

  canvas = document.getElementById('trackCanvas');
  if (canvas) ctx = canvas.getContext('2d');

  buildLedBars();

  // Check Bridge Context
  let isTestMode = true;
  let sessionLive = true;
  let preferredCircuit = 'sepang';

  if (window.FastestLapBridge) {
    if (typeof window.FastestLapBridge.isTestMode === 'function') {
      isTestMode = window.FastestLapBridge.isTestMode();
    }
    if (typeof window.FastestLapBridge.isSessionLive === 'function') {
      sessionLive = window.FastestLapBridge.isSessionLive();
    }
    if (typeof window.FastestLapBridge.getTestCircuit === 'function') {
      preferredCircuit = window.FastestLapBridge.getTestCircuit().toLowerCase();
    } else if (typeof window.FastestLapBridge.getCircuitId === 'function') {
      preferredCircuit = window.FastestLapBridge.getCircuitId().toLowerCase();
    }
    if (typeof window.FastestLapBridge.getSessionType === 'function') {
      const st = window.FastestLapBridge.getSessionType();
      if (st) sessionType = st.toLowerCase().trim();
    }
    if (typeof window.FastestLapBridge.getSessionPart === 'function') {
      const sp = window.FastestLapBridge.getSessionPart();
      if (sp) sessionPart = sp.toUpperCase().trim();
    }
  }

  const offlineView = document.getElementById('offlineView');
  const liveView = document.getElementById('liveView');

  if (!sessionLive && !isTestMode) {
    if (offlineView) offlineView.style.display = 'flex';
    if (liveView) liveView.style.display = 'none';

    if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
      const offEvent = document.getElementById('offlineEventTitle');
      if (offEvent) offEvent.textContent = window.FastestLapBridge.getEventTitle();
    }
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionName === 'function') {
      const offSess = document.getElementById('offlineSessionName');
      if (offSess) offSess.textContent = window.FastestLapBridge.getSessionName();
    }
    return;
  }

  // Sessione attiva o modalità test
  if (offlineView) offlineView.style.display = 'none';
  if (liveView) liveView.style.display = 'flex';

  // Applica prima il circuito selezionato in modo che tutte le strutture dati siano pronte
  if (preferredCircuit === 'baku') {
    applySimulation(SIM_BAKU);
  } else {
    if (sessionType === 'practice') {
      applySimulation(SIM_SEPANG_FP);
    } else if (sessionType === 'qualifying') {
      applySimulation(SIM_SEPANG_QUALIFYING);
    } else {
      applySimulation(SIM_SEPANG);
    }
  }

  renderTableHeaders(sessionType);
  updateSessionStatusBar();

  if (!isTestMode) {
    renderSimulationControls(false);
    isGenericRealMode = true;
  } else {
    renderSimulationControls(true);
  }

  window.addEventListener('resize', () => {
    resizeTrackCanvas();
    drawTrack();
  });

  setupTrackMapInteractions();
  resizeTrackCanvas();

  if (typeof renderSepangJumpBar === 'function') {
    renderSepangJumpBar(sessionType || 'race');
    updateScrubber();
  }

  requestAnimationFrame(animate);
}

window.onload = initApp;
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}

// Ricezione dinamica dell'immagine del circuito da Firebase/Android
window.setCircuitImage = function(url) {
  if (!url) return;
  const img = new Image();
  img.onload = function() {
    trackBgImage = img;
    drawTrack();
  };
  img.src = url;
};
`;

// ══════════════════════════════════════════════════════════════
// 4. CLEAN SEMANTIC HTML CONTENT (track_map.html)
// ══════════════════════════════════════════════════════════════
const htmlContent = `<!DOCTYPE html>
<html lang="it">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
  <title>FastestLap — Live Timing & Telemetria</title>
  <link rel="stylesheet" href="css/live_timing.css">
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
      <div class="offline-session" id="offlineSessionName">NESSUNA SESSIONE IN CORSO</div>
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
       PRODUCTION LIVE VIEW
       ══════════════════════════════════════════════════════════════ -->
  <div id="liveView">
    <header>
      <!-- Row 1: Header Brand, Live Title, Sim Switcher and Multipliers -->
      <div class="hdr-top-row">
        <div class="hdr-left">
          <button class="back-nav-btn" onclick="closeSession()" title="Chiudi">←</button>
          <div class="live-badge" id="liveBadgeIndicator">
            <div class="live-dot"></div>
            LIVE
          </div>
          <span class="hdr-event" id="hdrEventTitle">MALAYSIAN GRAND PRIX</span>
        </div>

        <div class="hdr-right-controls" id="hdrRightControls">
          <!-- In real context, this remains empty. Simulation controls are injected dynamically only if test mode is enabled -->
        </div>
      </div>

      <!-- Row 2: Persistent Session Status Bar (Bandiere, Tipo Sessione, Conteggio Giri) -->
      <div class="session-status-bar" id="sessionStatusBar">
        <div class="status-bar-flag-badge flag-green" id="statusBarFlag">
          <span id="statusBarFlagIcon">🟢</span>
          <span id="statusBarFlagText">BANDIERA VERDE</span>
        </div>
        <div class="status-bar-item">
          <span class="lbl">SESSIONE:</span>
          <span class="val" id="statusBarSessionVal">GARA</span>
        </div>
        <div class="status-bar-item">
          <span class="lbl">PROGRESSO:</span>
          <span class="val" id="statusBarLapVal">GIRO 1/55</span>
        </div>
        <div class="status-bar-item clock">
          <span class="lbl">ORA:</span>
          <span class="val" id="statusBarClockVal">08:33:00 UTC</span>
        </div>
      </div>
    </header>

    <!-- Swappable Views Container -->
    <main class="views-container">

      <!-- ══════════════════════════════════════════════════════════════
           VIEW 1: CLASSIFICA LIVE A SCHERMO INTERO (SOLO CLASSIFICA IN PISTA)
           Rimosse colonne: STATO VETTURA, TELEMETRIA e GIRI.
           Visualizzabile sia in landscape che in portrait!
           ══════════════════════════════════════════════════════════════ -->
      <section class="main-view active" id="view-standings">
        <div class="full-standings-wrapper">
          <table class="full-standings-table">
            <thead>
              <tr>
                <th class="col-pos">POS</th>
                <th class="col-gain">+/-</th>
                <th class="col-driver">PILOTA</th>
                <th class="col-gap">DISTACCO</th>
                <th class="col-int">INTERVALLO</th>
                <th class="col-last">ULTIMO GIRO</th>
                <th class="col-best">MIGLIOR GIRO</th>
                <th class="col-tyre">GOMMA</th>
                <th class="col-pit">PIT</th>
              </tr>
            </thead>
            <tbody id="fullStandingsBody">
              <!-- Dynamically populated -->
            </tbody>
          </table>
        </div>
      </section>

      <!-- ══════════════════════════════════════════════════════════════
           VIEW 2: MAPPA & PISTA (LANDSCAPE ONLY) CON PANNELLO LATERALE
           Rimosso STATO dalla classifica laterale.
           Prompt di rotazione se visualizzata in portrait.
           ══════════════════════════════════════════════════════════════ -->
      <section class="main-view" id="view-track">
        
        <!-- Prompt visible when phone is held vertically -->
        <div class="landscape-only-prompt">
          <div class="landscape-only-icon">🔄</div>
          <div class="landscape-only-title">Modalità Landscape Richiesta</div>
          <div class="landscape-only-desc">
            La mappa del circuito e il cruscotto telemetrico sono visualizzabili unicamente in modalità orizzontale (Landscape). Ruota il tuo dispositivo per visualizzare la pista.
          </div>
        </div>

        <div class="track-layout-container">
          <!-- Track Map Pane (Pure dark canvas with bottom-left reset focus button) -->
          <div class="track-pane" id="trackPane">
            <canvas id="trackCanvas"></canvas>
            <div id="mapZoomVerticalBadge" class="map-zoom-vertical-badge">
              <button id="btnMapZoomIn" class="map-zoom-vbtn" onclick="zoomMapStep(1)" title="Zoom In">
                <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor">
                  <path d="M8 2a.75.75 0 0 1 .75.75v4.5h4.5a.75.75 0 0 1 0 1.5h-4.5v4.5a.75.75 0 0 1-1.5 0v-4.5h-4.5a.75.75 0 0 1 0-1.5h4.5v-4.5A.75.75 0 0 1 8 2z"/>
                </svg>
              </button>
              <button id="btnMapZoomOut" class="map-zoom-vbtn" onclick="zoomMapStep(-1)" title="Zoom Out">
                <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor">
                  <path d="M2.75 8a.75.75 0 0 1 .75-.75h9a.75.75 0 0 1 0 1.5h-9A.75.75 0 0 1 2.75 8z"/>
                </svg>
              </button>
            </div>
            <button id="btnResetMapFocus" class="map-reset-focus-btn" onclick="resetMapFocus()" title="Torna alla vista globale">
              <svg viewBox="0 0 16 16" width="14" height="14" fill="currentColor">
                <path fill-rule="evenodd" d="M15 8a.75.75 0 0 1-.75.75H4.06l4.72 4.72a.75.75 0 1 1-1.06 1.06l-6-6a.75.75 0 0 1 0-1.06l6-6a.75.75 0 0 1 1.06 1.06L4.06 7.25h10.19A.75.75 0 0 1 15 8z"/>
              </svg>
            </button>
          </div>

          <!-- Side Panel -->
          <div class="side-panel">
            <div class="side-tabs-bar">
              <button class="side-tab-btn active" id="btnSideStandings" onclick="switchSideTab('side-standings')">
                📊 Classifica
              </button>
              <button class="side-tab-btn" id="btnSideTelemetry" onclick="switchSideTab('side-telemetry')">
                🏎️ Telemetria (2 Piloti)
              </button>
              <button class="side-tab-btn" id="btnSideRaceControl" onclick="switchSideTab('side-rc')">
                🚩 Race Control
              </button>
            </div>

            <!-- Side Sub-Tab 1: Compact Standings (NO S1/S2/S3, NO AERO, NO STATO) -->
            <div class="side-pane-content active" id="side-standings">
              <div class="compact-table-wrapper">
                <table class="compact-timing-table">
                  <thead>
                    <tr>
                      <th class="col-side-eye"></th>
                      <th class="col-side-pos">POS</th>
                      <th class="col-side-gain">+/-</th>
                      <th class="col-side-driver">PILOTA</th>
                      <th class="col-side-gap">DISTACCO</th>
                      <th class="col-side-int">INTERVALLO</th>
                      <th class="col-side-last">ULTIMO</th>
                      <th class="col-side-best">MIGLIORE</th>
                      <th class="col-side-tyre">GOMMA</th>
                    </tr>
                  </thead>
                  <tbody id="compactStandingsBody">
                    <!-- Populated dynamically -->
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Side Sub-Tab 2: Dual Cockpit Telemetry -->
            <div class="side-pane-content" id="side-telemetry">
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

            <!-- Side Sub-Tab 3: Mini Race Control Feed -->
            <div class="side-pane-content" id="side-rc">
              <div class="full-rc-container side-rc-box">
                <div class="rc-ticker-card" id="sideRcTickerCard">
                  🟡 Inizio Sessione
                </div>
                <div class="side-rc-feed-list" id="sideRcFeedList">
                  <!-- Dynamically populated -->
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      <!-- ══════════════════════════════════════════════════════════════
           VIEW 3: RACE CONTROL FULLSCREEN
           ══════════════════════════════════════════════════════════════ -->
      <section class="main-view" id="view-race-control">
        <div class="full-rc-container">
          <div class="full-rc-header">
            <div class="rc-filters">
              <button class="rc-filter-btn active" id="rcFilterAll" onclick="setRcFilter('ALL')">TUTTI</button>
              <button class="rc-filter-btn" id="rcFilterFlags" onclick="setRcFilter('Flag')">BANDIERE</button>
              <button class="rc-filter-btn" id="rcFilterSC" onclick="setRcFilter('SafetyCar')">SAFETY CAR</button>
              <button class="rc-filter-btn" id="rcFilterInv" onclick="setRcFilter('Investigation')">INVESTIGAZIONI</button>
            </div>
            <span class="rc-count-label" id="rcTotalCount">0 Messaggi</span>
          </div>
          <div class="rc-ticker-card" id="fullRcTickerCard">
            🟡 Sessione Ufficiale FIA Race Control
          </div>
          <div class="full-rc-messages-list" id="fullRcFeedList">
            <!-- Dynamically populated -->
          </div>
        </div>
      </section>

    </main>

    <!-- ══════════════════════════════════════════════════════════════
         NAVBAR ALLA BASE DELLO SCHERMO (BOTTOM NAVIGATION BAR)
         ══════════════════════════════════════════════════════════════ -->
    <nav class="bottom-nav-bar" id="bottomNavBar">
      <button class="bottom-nav-btn active" id="btnNavStandings" onclick="switchMainView('view-standings')">
        <span class="bnav-icon">📊</span>
        <span class="bnav-label">Classifica Live</span>
      </button>
      <button class="bottom-nav-btn" id="btnNavTrack" onclick="switchMainView('view-track')">
        <span class="bnav-icon">🗺️</span>
        <span class="bnav-label">Mappa & Pista</span>
      </button>
      <button class="bottom-nav-btn" id="btnNavRaceControl" onclick="switchMainView('view-race-control')">
        <span class="bnav-icon">🚩</span>
        <span class="bnav-label">Race Control</span>
      </button>
    </nav>
  </div>

  <!-- Modular Scripts separated from page markup -->
  <script src="simulations/live_simulation_data.js"></script>
  <script src="js/live_timing_app.js"></script>
</body>
</html>
`;

// ══════════════════════════════════════════════════════════════
// 5. STANDALONE SEPANG SIMULATION SUITE GENERATOR
// ══════════════════════════════════════════════════════════════
const sepangTimingCss = cssContent;

const sepangSimulationDataJs = `/**
 * FastestLap — Simulation Datasets for Sepang Grand Prix
 * Free Practice (FP2), Qualifying (Q1, Q2, Q3) and Race (55 Laps)
 */

const SIM_SEPANG = {
  circuitId: 'sepang',
  eventTitle: 'FORMULA 1 PETRONAS MALAYSIAN GP',
  svgWidth: 1280,
  svgHeight: 1057,
  lapDuration: 95.0,
  totalLaps: 55,
  raceStartTimeSec: 216.9,
  raceFinishSec: 6435.0,
  sessionDuration: 6550,
  pitExitTrackSec: 5.06,
  pitEntryTrackSec: 89.34,
  s1Track: 21.11,
  s2Track: 46.58,
  gridOrder: ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41'],
  nodes: ${JSON.stringify(compactSepangNodes)},
  pitNodes: ${JSON.stringify(sepangPitNodes)},
  drivers: ${JSON.stringify(sepangDrivers)},
  keyframes: ${JSON.stringify(sepangKeyframes)},
  pitStops: ${JSON.stringify(sepangPitStops)},
  rc: ${JSON.stringify(sepangRC)},
  retirements: ${JSON.stringify(sepangRetirements)},
  stints: ${JSON.stringify(sepangStints)},
  laps: ${JSON.stringify(sepangLaps)},
  events: ${JSON.stringify(sepangEvents)},
  incidents: ${JSON.stringify(sepangIncidents)},
  lapStarts: ${JSON.stringify(sepangLapStarts)}
};

const SIM_SEPANG_FP = Object.assign(${JSON.stringify(sepangFpData)}, {
  svgWidth: 1280,
  svgHeight: 1057,
  raceStartTimeSec: 0.0,
  raceFinishSec: 3600.0,
  nodes: SIM_SEPANG.nodes,
  pitNodes: SIM_SEPANG.pitNodes
});

const SIM_SEPANG_QUALIFYING = Object.assign(${JSON.stringify(sepangQualyData)}, {
  svgWidth: 1280,
  svgHeight: 1057,
  raceStartTimeSec: 0.0,
  raceFinishSec: 3600.0,
  nodes: SIM_SEPANG.nodes,
  pitNodes: SIM_SEPANG.pitNodes
});
`;

const sepangTimingAppJs = `${appJsContent}

// ══════════════════════════════════════════════════════════════
// SEPANG STANDALONE SIMULATION CONTROLS & JUMPS
// ══════════════════════════════════════════════════════════════
function setSepangSession(type) {
  setSessionType(type);
  ['btnSessRace', 'btnSessQual', 'btnSessFP'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const actId = type === 'practice' ? 'btnSessFP' : (type === 'qualifying' ? 'btnSessQual' : 'btnSessRace');
  const actBtn = document.getElementById(actId);
  if (actBtn) actBtn.classList.add('active');

  const scrubber = document.getElementById('sepangTimeScrubber');
  if (scrubber) {
    scrubber.max = SESSION_DURATION;
    scrubber.value = 0;
  }
  renderSepangJumpBar(type);
  updateScrubber();
}
window.setSepangSession = setSepangSession;

function renderSepangJumpBar(type) {
  const container = document.getElementById('sepangJumpBar');
  if (!container) return;

  let jumps = [];
  if (type === 'qualifying') {
    jumps = [
      { label: '🟢 Inizio Q1 (0s)', sec: 0 },
      { label: '⏱️ Primi Tempi Q1 (180s)', sec: 180 },
      { label: '⚠️ Cutoff Finale Q1 (960s)', sec: 960 },
      { label: '🔴 Fine Q1 - Eliminati P16-P22 (1080s)', sec: 1080 },
      { label: '🟢 Inizio Q2 (1140s)', sec: 1140 },
      { label: '⚡ Run 2 Q2 (1750s)', sec: 1750 },
      { label: '🔴 Fine Q2 - Eliminati P11-P15 (2160s)', sec: 2160 },
      { label: '🟢 Inizio Q3 Top 10 (2220s)', sec: 2220 },
      { label: '⏱️ Provvisoria LEC 1:31.298 (2550s)', sec: 2550, drv: '16' },
      { label: '👑 Pole VER 1:31.215 (3140s)', sec: 3140, drv: '3' },
      { label: '🏁 Conclusione Qualifiche (3240s)', sec: 3240 }
    ];
  } else if (type === 'practice') {
    jumps = [
      { label: '🟢 Semaforo Verde FP2 (0s)', sec: 0 },
      { label: '🏎️ Primi Out-Lap (90s)', sec: 90 },
      { label: '⏱️ Stint 1 Medium (300s)', sec: 300 },
      { label: '🟡 Bandiera Gialla C4 - Albon (1350s)', sec: 1350, drv: '23' },
      { label: '🟢 Pista Libera (1410s)', sec: 1410 },
      { label: '⚡ Qualy Sim Soft - VER 1:32.145 (2020s)', sec: 2020, drv: '3' },
      { label: '⛽ Long Run Gomma Hard (2400s)', sec: 2400 },
      { label: '🏁 Bandiera a Scacchi FP2 (3600s)', sec: 3600 }
    ];
  } else {
    jumps = [
      { label: '🚥 Standing Start (216s)', sec: 216 },
      { label: '🏎️ Duello Verstappen - Hamilton (800s)', sec: 800 },
      { label: '🔧 Primi Pit Stop Intermedi (1200s)', sec: 1200 },
      { label: '🌧️ Pioggia Intensa (2500s)', sec: 2500 },
      { label: '🏁 Arrivo al Traguardo (6435s)', sec: 6435 }
    ];
  }

  container.innerHTML = jumps.map(j => {
    const drvArg = j.drv ? (", '" + j.drv + "'") : '';
    return '<button class="sepang-jump-btn" onclick="jumpToTime(' + j.sec + drvArg + ')">' + j.label + '</button>';
  }).join('');
}
window.renderSepangJumpBar = renderSepangJumpBar;

function togglePlayPause() {
  isPlaying = !isPlaying;
  const btn = document.getElementById('sepangPlayPauseBtn');
  if (btn) {
    btn.textContent = isPlaying ? '⏸️ Pausa' : '▶️ Play';
    btn.classList.toggle('paused', !isPlaying);
  }
}
window.togglePlayPause = togglePlayPause;

function onScrubberInput(e) {
  currentSecond = parseFloat(e.target.value);
  updateSessionStatusBar();
  updateTimingTables();
  updateCockpit(1, selectedDriver1);
  updateCockpit(2, selectedDriver2);
  updateRaceControl();
  drawTrack();
  updateScrubberTimeLabel();
}
window.onScrubberInput = onScrubberInput;

function updateScrubber() {
  const scrubber = document.getElementById('sepangTimeScrubber');
  if (scrubber) {
    scrubber.max = SESSION_DURATION;
    scrubber.value = Math.floor(currentSecond);
  }
  updateScrubberTimeLabel();
}

function updateScrubberTimeLabel() {
  const timeLabel = document.getElementById('sepangTimeLabel');
  if (timeLabel) {
    const curMin = Math.floor(currentSecond / 60);
    const curSec = Math.floor(currentSecond % 60);
    const totMin = Math.floor(SESSION_DURATION / 60);
    const totSec = Math.floor(SESSION_DURATION % 60);
    const pad = n => (n < 10 ? '0' + n : n);
    timeLabel.textContent = pad(curMin) + ':' + pad(curSec) + ' / ' + pad(totMin) + ':' + pad(totSec);
  }
}

function jumpToTime(sec, focusDriverKey) {
  currentSecond = Math.max(0, Math.min(SESSION_DURATION, sec));
  if (focusDriverKey) {
    focusedDriver = focusDriverKey;
    selectedDriver1 = focusDriverKey;
    const s1 = document.getElementById('drv1Select');
    if (s1) s1.value = focusDriverKey;
  }
  updateSessionStatusBar();
  updateTimingTables();
  updateCockpit(1, selectedDriver1);
  updateCockpit(2, selectedDriver2);
  updateRaceControl();
  drawTrack();
  updateScrubber();
}
window.jumpToTime = jumpToTime;
`;

const sepangHeaderHtml = `<header>
      <!-- Row 1: Brand & Back Link -->
      <div class="hdr-top-row">
        <div class="hdr-left">
          <a class="back-nav-btn" href="../../track_map.html" title="Torna indietro" style="text-decoration:none;display:inline-flex;align-items:center;justify-content:center;">←</a>
          <div class="live-badge" id="liveBadgeIndicator" style="background:#0093cc;">
            <div class="live-dot" style="background:#58a6ff;"></div>
            SIMULAZIONE SEPANG
          </div>
          <span class="hdr-event" id="hdrEventTitle">FORMULA 1 PETRONAS MALAYSIAN GP</span>
        </div>
        <div class="hdr-right-controls">
          <a href="../../track_map.html" class="test-btn" style="text-decoration:none;">📱 VISTA APP</a>
        </div>
      </div>

      <!-- Row 2: Sepang Dedicated Simulation Controls (Session pills, Scrubber, Speed) -->
      <div class="sepang-sim-header-bar">
        <div class="sepang-session-pills">
          <button class="sepang-session-pill active" id="btnSessRace" onclick="setSepangSession('race')">🏎️ GARA (55 Giri)</button>
          <button class="sepang-session-pill" id="btnSessQual" onclick="setSepangSession('qualifying')">⏱️ QUALIFICHE (Q1-Q2-Q3)</button>
          <button class="sepang-session-pill" id="btnSessFP" onclick="setSepangSession('practice')">🟢 PROVE LIBERE (FP2)</button>
        </div>

        <div class="sepang-scrubber-box">
          <button class="sepang-play-btn" id="sepangPlayPauseBtn" onclick="togglePlayPause()">⏸️ Pausa</button>
          <input type="range" class="sepang-time-slider" id="sepangTimeScrubber" min="0" max="6550" value="0" oninput="onScrubberInput(event)">
          <span class="sepang-time-label" id="sepangTimeLabel">00:00 / 109:10</span>
        </div>

        <div class="sepang-speed-box">
          <button class="sepang-speed-btn active" id="btnSpd1x" onclick="setSimSpeed(1)">1x</button>
          <button class="sepang-speed-btn" id="btnSpd2x" onclick="setSimSpeed(2)">2x</button>
          <button class="sepang-speed-btn" id="btnSpd5x" onclick="setSimSpeed(5)">5x</button>
          <button class="sepang-speed-btn" id="btnSpd10x" onclick="setSimSpeed(10)">10x</button>
          <button class="sepang-speed-btn" id="btnSpd30x" onclick="setSimSpeed(30)">30x</button>
        </div>
      </div>

      <!-- Row 3: Quick Key Moments Jump Bar -->
      <div class="sepang-jump-bar" id="sepangJumpBar">
        <!-- Dynamically populated based on active session -->
      </div>

      <!-- Row 4: Session Status Bar -->
      <div class="session-status-bar" id="sessionStatusBar">
        <div class="status-bar-flag-badge flag-green" id="statusBarFlag">
          <span id="statusBarFlagIcon">🟢</span>
          <span id="statusBarFlagText">BANDIERA VERDE</span>
        </div>
        <div class="status-bar-item">
          <span class="lbl">SESSIONE:</span>
          <span class="val" id="statusBarSessionVal">GARA</span>
        </div>
        <div class="status-bar-item">
          <span class="lbl">PROGRESSO:</span>
          <span class="val" id="statusBarLapVal">GIRO 1/55</span>
        </div>
        <div class="status-bar-item clock">
          <span class="lbl">TEMPO:</span>
          <span class="val" id="statusBarClockVal">00:00</span>
        </div>
      </div>
    </header>`;

const sepangHtmlContent = htmlContent
  .replace('<title>FastestLap — Live Timing & Telemetria</title>', '<title>Sepang International Circuit — Live Timing & Simulazioni (FastestLap)</title>')
  .replace('href="css/live_timing.css"', 'href="sepang_timing.css"')
  .replace(/<header>[\s\S]*?<\/header>/, sepangHeaderHtml)
  .replace('src="simulations/live_simulation_data.js"', 'src="sepang_simulation_data.js"')
  .replace('src="js/live_timing_app.js"', 'src="sepang_timing_app.js"');

// ══════════════════════════════════════════════════════════════
// WRITE MODULAR ASSETS TO BOTH APP ASSETS AND TEST SUITE
// ══════════════════════════════════════════════════════════════
const targets = [
  { dir: assetsDir, name: 'Android assets' },
  { dir: testSuiteDir, name: 'SignalR test suite' }
];

targets.forEach(t => {
  fs.writeFileSync(path.join(t.dir, 'css', 'live_timing.css'), cssContent, 'utf-8');
  fs.writeFileSync(path.join(t.dir, 'simulations', 'live_simulation_data.js'), dataJsContent, 'utf-8');
  fs.writeFileSync(path.join(t.dir, 'js', 'live_timing_app.js'), appJsContent, 'utf-8');
  fs.writeFileSync(path.join(t.dir, 'track_map.html'), htmlContent, 'utf-8');

  // Copy simulation images to isolated simulations directory
  const sepangSrc = path.join(testSuiteDir, 'Sepang.svg.webp');
  const bakuSrc = path.join(testSuiteDir, 'Baku_Formula_One_circuit_map.svg.webp');

  if (fs.existsSync(sepangSrc)) {
    fs.copyFileSync(sepangSrc, path.join(t.dir, 'simulations', 'Sepang.svg.webp'));
    fs.copyFileSync(sepangSrc, path.join(t.dir, 'simulations', 'sepang', 'Sepang.svg.webp'));
  }
  if (fs.existsSync(bakuSrc)) {
    fs.copyFileSync(bakuSrc, path.join(t.dir, 'simulations', 'Baku_Formula_One_circuit_map.svg.webp'));
    fs.copyFileSync(bakuSrc, path.join(t.dir, 'simulations', 'baku', 'Baku_Formula_One_circuit_map.svg.webp'));
  }

  // Write Sepang dedicated simulation suite
  const sepangDir = path.join(t.dir, 'simulations', 'sepang');
  if (!fs.existsSync(sepangDir)) fs.mkdirSync(sepangDir, { recursive: true });

  fs.writeFileSync(path.join(sepangDir, 'sepang_timing.css'), sepangTimingCss, 'utf-8');
  fs.writeFileSync(path.join(sepangDir, 'sepang_simulation_data.js'), sepangSimulationDataJs, 'utf-8');
  fs.writeFileSync(path.join(sepangDir, 'sepang_timing_app.js'), sepangTimingAppJs, 'utf-8');
  fs.writeFileSync(path.join(sepangDir, 'track_map_sepang.html'), sepangHtmlContent, 'utf-8');
  fs.writeFileSync(path.join(sepangDir, 'index.html'), sepangHtmlContent, 'utf-8');

  // Ensure Baku has index.html as well
  const bakuDir = path.join(t.dir, 'simulations', 'baku');
  const bakuTrackHtml = path.join(bakuDir, 'track_map_baku.html');
  const bakuIndexHtml = path.join(bakuDir, 'index.html');
  if (fs.existsSync(bakuTrackHtml) && !fs.existsSync(bakuIndexHtml)) {
    fs.copyFileSync(bakuTrackHtml, bakuIndexHtml);
  }

  // Remove old un-isolated simulation data file from js/ if present
  const oldDataPath = path.join(t.dir, 'js', 'live_simulation_data.js');
  if (fs.existsSync(oldDataPath)) {
    try { fs.unlinkSync(oldDataPath); } catch (e) {}
  }

  // In production Android assets, remove simulation-specific track images from root
  if (t.dir === assetsDir) {
    const oldSepang = path.join(assetsDir, 'Sepang.svg.webp');
    if (fs.existsSync(oldSepang)) {
      try { fs.unlinkSync(oldSepang); } catch (e) {}
    }
    const oldBaku = path.join(assetsDir, 'Baku_Formula_One_circuit_map.svg.webp');
    if (fs.existsSync(oldBaku)) {
      try { fs.unlinkSync(oldBaku); } catch (e) {}
    }
  }

  console.log(`Successfully written modular files to ${t.name}:`);
  console.log(`  - css/live_timing.css (${cssContent.length} bytes)`);
  console.log(`  - simulations/live_simulation_data.js (${dataJsContent.length} bytes)`);
  console.log(`  - js/live_timing_app.js (${appJsContent.length} bytes)`);
  console.log(`  - track_map.html (${htmlContent.length} bytes)`);
  console.log(`  - simulations/sepang/ (sepang_timing.css, sepang_simulation_data.js, sepang_timing_app.js, track_map_sepang.html, index.html)`);
  console.log(`  - simulations/sepang/ & simulations/baku/ assets isolated`);
});

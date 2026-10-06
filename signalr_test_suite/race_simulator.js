#!/usr/bin/env node
/**
 * Real-Time Race & Telemetry Dashboard Simulator
 * Simulates the progression of the Sepang Grand Prix race (10:30:00 to 12:30:00 UTC)
 * in real-time or accelerated time, updating telemetry gauges, shift lights,
 * gear, speed, pedals, and DRS as the SignalR stream packets advance.
 */

const readline = require('readline');
const SessionLoader = require('./src/session_loader');
const { getAsciiTrackMapLines } = require('./src/sepang_geometry');

// ANSI Color definitions
const C = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  dim: '\x1b[2m',
  red: '\x1b[31m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  magenta: '\x1b[35m',
  cyan: '\x1b[36m',
  white: '\x1b[37m',
  bgRed: '\x1b[41m',
  bgGreen: '\x1b[42m',
  bgYellow: '\x1b[43m',
  bgBlue: '\x1b[44m',
  bgDark: '\x1b[100m'
};

function getTrackLocation(pt, lapElapsedSec) {
  const speed = pt.speed || 0;
  const gear = pt.gear || 1;
  const brake = pt.brake || 0;
  const drs = pt.drsActive;

  if (drs && speed > 290) {
    return lapElapsedSec > 50 
      ? 'Back Straight (DRS Zone 2 — 920m)' 
      : 'Main Straight (Start/Finish DRS Zone 1 — 927m)';
  }
  if (brake === 1 && speed < 90) {
    if (lapElapsedSec < 15) return 'Sector 1 — Turn 1-2 Hairpin (Heavy Braking)';
    if (lapElapsedSec < 60) return 'Sector 2 — Turn 9 Uphill Hairpin';
    return 'Sector 3 — Turn 15 Final Hairpin';
  }
  if (brake === 1 && speed >= 90 && speed < 140) {
    return 'Sector 1 — Turn 4 Right-Hander';
  }
  if (speed > 220 && gear >= 6) {
    return 'Sector 2 — Turn 5-6 High-Speed Sweeping Esses';
  }
  if (speed >= 140 && speed <= 200) {
    return 'Sector 2 — Turn 10-11 Double Apex Right';
  }
  if (gear === 3 || gear === 4) {
    return 'Sector 2 — Turn 7-8 Medium Corner';
  }
  return 'Full Throttle Acceleration Zone';
}

function renderShiftLights(rpm, maxRpm = 12500) {
  const pct = Math.max(0, Math.min(1, rpm / maxRpm));
  // 10 LEDs total: 4 Green, 4 Yellow, 2 Red
  const ledsOn = Math.round(pct * 10);
  let str = '';
  for (let i = 1; i <= 10; i++) {
    if (i <= ledsOn) {
      if (i <= 4) str += `${C.green}●${C.reset} `;
      else if (i <= 8) str += `${C.yellow}●${C.reset} `;
      else str += `${C.red}${C.bold}●${C.reset} `;
    } else {
      str += `${C.dim}○${C.reset} `;
    }
  }
  return str.trim();
}

function renderBar(val, max, width = 16, color = C.green) {
  const ratio = Math.max(0, Math.min(1, val / max));
  const filled = Math.round(ratio * width);
  const empty = width - filled;
  return `${color}${'█'.repeat(filled)}${C.dim}${'░'.repeat(empty)}${C.reset}`;
}

async function runRaceSimulation(loader, driverInput, options = {}) {
  const result = loader.getDriverTelemetry(driverInput, {
    startTime: options.start || '2026-10-04T10:30:00.000Z',
    endTime: options.end || '2026-10-04T12:30:00.000Z'
  });

  const { driver, points, sessionInfo } = result;
  if (!points || points.length === 0) {
    console.error(`No telemetry points found for driver ${driverInput}`);
    return;
  }

  // Simulation controls: interval between clock updates (1 to 5 seconds, default 1s)
  let step = options.interval || options.step || 1;
  step = Math.max(1, Math.min(5, parseInt(step, 10) || 1));

  let speedMultiplier = options.speed || 1; // 1x real-time by default (or specified via --speed)
  speedMultiplier = Math.max(1, parseInt(speedMultiplier, 10) || 1);

  function calcDelay() {
    return Math.max(15, Math.round((1000 * step) / speedMultiplier));
  }
  let frameDelayMs = calcDelay();

  let isPaused = false;
  let isRunning = true;
  let currentIndex = 0;

  // Keypress listener for interactive controls (if running in interactive terminal)
  if (process.stdin.isTTY) {
    readline.emitKeypressEvents(process.stdin);
    process.stdin.setRawMode(true);
    process.stdin.on('keypress', (str, key) => {
      if (key.ctrl && key.name === 'c') {
        isRunning = false;
        process.stdin.setRawMode(false);
        process.exit(0);
      } else if (key.name === 'space') {
        isPaused = !isPaused;
      } else if (str === '+' || key.name === 'up' || str === 'f') {
        speedMultiplier = Math.min(64, speedMultiplier * 2);
        frameDelayMs = calcDelay();
      } else if (str === '-' || key.name === 'down' || str === 's') {
        speedMultiplier = Math.max(1, Math.round(speedMultiplier / 2));
        frameDelayMs = calcDelay();
      } else if (['1', '2', '3', '4', '5'].includes(str)) {
        step = parseInt(str, 10);
        frameDelayMs = calcDelay();
      } else if (key.name === 'q' || str === 'q') {
        isRunning = false;
      }
    });
  }

  const raceStartTime = new Date('2026-10-04T10:30:00.000Z').getTime();
  const lapDurationSec = 95; // Sepang lap time ~1m35s
  const recentHistory = [];
  const historyLimit = 6;

  console.clear();

  while (isRunning && currentIndex < points.length) {
    if (isPaused) {
      await new Promise(r => setTimeout(r, 100));
      continue;
    }

    const pt = points[currentIndex];
    const ptTime = new Date(pt.utc).getTime();
    const elapsedSec = Math.max(0, Math.floor((ptTime - raceStartTime) / 1000));
    const currentLap = Math.floor(elapsedSec / lapDurationSec) + 1;
    const lapElapsedSec = elapsedSec % lapDurationSec;
    const trackLocation = pt.location || getTrackLocation(pt, lapElapsedSec);

    recentHistory.unshift({
      time: pt.utc.substring(11, 19),
      speed: pt.speed,
      rpm: pt.rpm,
      gear: pt.gear,
      throttle: pt.throttle,
      brake: pt.brake,
      drs: pt.drsActive
    });
    if (recentHistory.length > historyLimit) {
      recentHistory.pop();
    }

    // Render Full-Screen Cockpit Dashboard
    let output = '';
    // Move cursor to top-left without clearing to prevent terminal flicker
    output += '\x1b[H';

    output += `${C.bold}${C.cyan}╔═════════════════════════════════════════════════════════════════════════════════════════════════════════╗${C.reset}\n`;
    output += `${C.bold}${C.cyan}║${C.reset}  ${C.bold}${C.yellow}🏎️  FASTESTLAP — SEPANG GRAND PRIX 2026 : LIVE RACE TELEMETRY STREAM SIMULATOR${C.reset}                      ${C.bold}${C.cyan}║${C.reset}\n`;
    output += `${C.bold}${C.cyan}╚═════════════════════════════════════════════════════════════════════════════════════════════════════════╝${C.reset}\n`;

    // Session Status Bar
    const progressPct = ((currentIndex / points.length) * 100).toFixed(1);
    output += ` ${C.bold}Session Clock:${C.reset} ${C.bold}${C.yellow}${pt.utc.substring(11, 19)} UTC${C.reset}  │  ${C.bold}Lap:${C.reset} ${C.bold}${C.cyan}${String(currentLap).padStart(2)}/56${C.reset}  │  ${C.bold}Race Progress:${C.reset} [${renderBar(currentIndex, points.length, 12, C.cyan)}] ${progressPct}%\n`;
    output += ` ${C.bold}Driver:${C.reset} ${C.bold}${C.green}#${driver.driverNumber} ${driver.firstName} ${driver.lastName} (${driver.code})${C.reset}  │  ${C.magenta}${driver.team}${C.reset}  │  ${C.bold}Clock Step:${C.reset} ${C.cyan}${step}s${C.reset}  │  ${C.bold}Sim Speed:${C.reset} ${speedMultiplier}x ${isPaused ? `${C.bgRed} PAUSED ${C.reset}` : `${C.bgGreen} LIVE ${C.reset}`}\n`;
    output += ` ${C.bold}Circuit Section:${C.reset} ${C.bold}${C.white}📍 ${trackLocation.padEnd(52)}${C.reset}\n`;
    output += `${C.dim}───────────────────────────────────────────────────────────────────────────────────────────────────────────${C.reset}\n\n`;

    // Cockpit Gauge Boxes
    const speedStr = String(pt.speed).padStart(3);
    const rpmStr = String(pt.rpm).padStart(5);
    const throttleStr = String(pt.throttle).padStart(3);
    const shiftLeds = renderShiftLights(pt.rpm);

    const brakeIndicator = pt.brake === 1 
      ? `${C.bgRed}${C.white}${C.bold}   BRAKE ENGAGED   ${C.reset}` 
      : `${C.dim}[       OFF       ]${C.reset}`;

    const drsIndicator = pt.drsActive 
      ? `${C.bgGreen}${C.white}${C.bold}  >>> DRS ACTIVE <<<  ${C.reset}` 
      : `${C.dim}[      DRS OFF      ]${C.reset}`;

    output += ` ┌───────────────┐   ┌──────────────────────────────────┐   ┌─────────────────────────────────────────┐\n`;
    output += ` │     GEAR      │   │        SPEED & REV LIGHTS        │   │                 PEDALS                  │\n`;
    output += ` │               │   │                                  │   │                                         │\n`;
    output += ` │     ╔═══╗     │   │   ${C.bold}${C.yellow}${speedStr} km/h${C.reset}                        │   │   THROTTLE: [${renderBar(pt.throttle, 100, 14, C.green)}] ${throttleStr}%    │\n`;
    output += ` │     ║ ${C.bold}${C.magenta}${pt.gear}${C.reset} ║     │   │   RPM: ${C.bold}${C.cyan}${rpmStr}${C.reset}                   │   │   BRAKE:    ${brakeIndicator} │\n`;
    output += ` │     ╚═══╝     │   │   LEDs: ${shiftLeds} │   │   DRS:      ${drsIndicator}   │\n`;
    output += ` └───────────────┘   └──────────────────────────────────┘   └─────────────────────────────────────────┘\n\n`;

    // Real-Time Incoming SignalR Packets Log + 2D ASCII Track Map
    output += ` ┌───────────────────────────────────────┬─────────────────────────────────────────────────────────────────┐\n`;
    output += ` │ ${C.bold}🗺️  SEPANG TRACK MAP (Live GPS Dot)${C.reset}    │ ${C.bold}📡 INCOMING SIGNALR FEED (CarData.z -> Decoded)${C.reset}                │\n`;
    output += ` ├───────────────────────────────────────┼─────────────────────────────────────────────────────────────────┤\n`;

    const mapLines = getAsciiTrackMapLines(pt.x !== undefined ? pt.x : 75, pt.y !== undefined ? pt.y : -9, driver.code, 37, 10);
    const feedHeader = `${C.dim}Time (UTC)  Speed        RPM     Gear  Thr   Brake    DRS${C.reset}`;
    const feedLines = [feedHeader];

    for (const h of recentHistory.slice(0, 9)) {
      const spdBar = renderBar(h.speed, 340, 6, C.yellow);
      const brkTag = h.brake === 1 ? `${C.red}${C.bold}BRK${C.reset}` : `${C.dim}off${C.reset}`;
      const drsTag = h.drs ? `${C.green}${C.bold}DRS${C.reset}` : `${C.dim}off${C.reset}`;
      feedLines.push(
        `${C.bold}${h.time}${C.reset} ${String(h.speed).padStart(3)} ${spdBar} ${String(h.rpm).padStart(5)}rpm  ${C.magenta}G${h.gear}${C.reset}  ${String(h.throttle).padStart(3)}%  ${brkTag}  ${drsTag}`
      );
    }

    const maxRows = Math.max(mapLines.length, feedLines.length, 10);
    for (let r = 0; r < maxRows; r++) {
      const mapLine = mapLines[r] || '';
      const displayLen = Array.from(mapLine).reduce((acc, ch) => acc + (ch === '🔴' ? 2 : 1), 0);
      const left = mapLine + ' '.repeat(Math.max(0, 37 - displayLen));
      const right = (feedLines[r] || '').padEnd(63);
      output += ` │ ${left} │ ${right} │\n`;
    }

    output += ` ├───────────────────────────────────────┴─────────────────────────────────────────────────────────────────┤\n`;
    output += ` │ 🔴 = #${driver.driverNumber} ${driver.lastName}  │ GPS Coordinates: ${C.bold}X: ${String(pt.x || 0).padStart(4)}m, Y: ${String(pt.y || 0).padStart(4)}m${C.reset} (Accuracy: 0.1m / Decimeters) │\n`;
    output += ` └─────────────────────────────────────────────────────────────────────────────────────────────────────────┘\n`;
    output += ` ${C.bold}Controls:${C.reset} [${C.bold}Space${C.reset}] Pause/Resume  │  [${C.bold}+${C.reset}/${C.bold}-${C.reset}] Speed (${speedMultiplier}x)  │  [${C.bold}1-5${C.reset}] Step (${step}s)  │  [${C.bold}Q${C.reset}] Quit\n`;

    process.stdout.write(output);

    currentIndex += step;
    await new Promise(r => setTimeout(r, frameDelayMs));
  }

  if (process.stdin.isTTY) {
    process.stdin.setRawMode(false);
  }

  console.log(`\n\n${C.green}🏁 Race simulation completed for #${driver.driverNumber} ${driver.lastName}!${C.reset}\n`);
}

function parseArgs() {
  const args = process.argv.slice(2);
  const options = {};
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--driver' || args[i] === '-d') {
      options.driver = args[++i];
    } else if (args[i] === '--speed' || args[i] === '-s') {
      options.speed = parseInt(args[++i], 10) || 1;
    } else if (args[i] === '--step' || args[i] === '--interval' || args[i] === '-i') {
      options.step = parseInt(args[++i], 10) || 1;
    }
  }
  return options;
}

async function main() {
  const loader = new SessionLoader();
  loader.load();
  const options = parseArgs();

  if (options.driver) {
    await runRaceSimulation(loader, options.driver, options);
  } else {
    // Default to Leclerc #16 if not passed, or prompt
    const defaultDriver = '16';
    console.log(`Starting live race dashboard for default driver #16 Charles Leclerc (Ferrari)...`);
    console.log(`(Tip: Use --driver <num|code> to select another driver, e.g. --driver 1)\n`);
    await new Promise(r => setTimeout(r, 1200));
    await runRaceSimulation(loader, defaultDriver, options);
  }
}

if (require.main === module) {
  main().catch(err => {
    console.error('Simulation error:', err.message);
    if (process.stdin.isTTY) {
      process.stdin.setRawMode(false);
    }
    process.exit(1);
  });
}

module.exports = runRaceSimulation;

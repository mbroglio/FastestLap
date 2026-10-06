#!/usr/bin/env node
/**
 * Interactive Console Telemetry Viewer for F1 SignalR Stream
 * Displays decoded telemetry (Speed, RPM, Gear, Throttle, Brake, DRS)
 * for a user-selected driver during the Sepang race (10:30 - 12:30 UTC).
 */

const readline = require('readline');
const SessionLoader = require('./src/session_loader');

// ANSI Color Codes for terminal
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
  bgDark: '\x1b[100m'
};

function renderProgressBar(val, max, width = 10, color = C.green) {
  const ratio = Math.max(0, Math.min(1, val / max));
  const filled = Math.round(ratio * width);
  const empty = width - filled;
  return `${color}${'█'.repeat(filled)}${C.dim}${'░'.repeat(empty)}${C.reset}`;
}

function renderBrakeTag(brake) {
  if (brake === 1 || brake > 0) {
    return `${C.bgRed}${C.white}${C.bold} BRAKE ${C.reset}`;
  }
  return `${C.dim}  off  ${C.reset}`;
}

function renderDrsTag(drsActive) {
  if (drsActive) {
    return `${C.bgGreen}${C.white}${C.bold}  DRS  ${C.reset}`;
  }
  return `${C.dim}  off  ${C.reset}`;
}

function displayDriverTelemetry(loader, driverInput, options = {}) {
  const result = loader.getDriverTelemetry(driverInput, {
    startTime: options.start || '2026-10-04T08:33:00.000Z',
    endTime: options.end || '2026-10-04T10:22:10.000Z'
  });

  const { driver, stats, points, sessionInfo } = result;

  console.clear();
  console.log(`\n${C.bold}${C.cyan}========================================================================================${C.reset}`);
  console.log(`${C.bold}${C.yellow}🏎️  FASTESTLAP — F1 SIGNALR TELEMETRY STREAM INSPECTOR${C.reset}`);
  console.log(`${C.bold}${C.cyan}========================================================================================${C.reset}`);
  console.log(`${C.bold}Session:${C.reset}  ${sessionInfo?.Meeting?.OfficialName || 'FORMULA 1 PETRONAS MALAYSIAN GRAND PRIX 2026'}`);
  console.log(`${C.bold}Circuit:${C.reset}  Sepang International Circuit (5.543 km)`);
  console.log(`${C.bold}Date:${C.reset}     Domenica 4 Ottobre 2026 (Direttore di Gara: 08:33:00 ➔ 10:20:15 UTC — 55 Giri)`);
  console.log(`${C.bold}Driver:${C.reset}   ${C.bold}${C.green}#${driver.driverNumber} ${driver.firstName} ${driver.lastName} (${driver.code})${C.reset} | ${C.magenta}${driver.team}${C.reset}`);
  console.log(`${C.cyan}----------------------------------------------------------------------------------------${C.reset}`);

  // Telemetry KPIs Card
  console.log(`\n${C.bold}${C.white}📊 TELEMETRY SUMMARY & RACE STATISTICS:${C.reset}`);
  console.log(`┌──────────────────────────────┬──────────────────────────────┬────────────────────────┐`);
  console.log(`│ Top Speed:    ${C.bold}${C.yellow}${String(stats.maxSpeed).padStart(3)} km/h${C.reset}        │ Max RPM:       ${C.bold}${C.yellow}${String(stats.maxRpm).padStart(5)} rpm${C.reset}     │ Full Throttle: ${C.bold}${C.green}${stats.throttleFullPct.padStart(6)}${C.reset} │`);
  console.log(`│ Avg Speed:    ${String(stats.avgSpeed).padStart(3)} km/h        │ Avg RPM:       ${String(stats.avgRpm).padStart(5)} rpm     │ Brake Events:  ${String(stats.brakeEventsCount).padStart(6)}   │`);
  console.log(`│ Samples:      ${String(stats.sampleCount).padStart(5)} frames       │ DRS Activations: ${String(stats.drsActivationCount).padStart(3)} times       │ Status:        FINISHED│`);
  console.log(`└──────────────────────────────┴──────────────────────────────┴────────────────────────┘`);

  console.log(`\n${C.bold}Gear Usage Distribution:${C.reset}`);
  const gearStr = Object.entries(stats.gearDistribution)
    .map(([g, pct]) => `${C.cyan}G${g}:${C.reset} ${pct}`)
    .join('  |  ');
  console.log(`  ${gearStr}`);

  // Display telemetry trace table
  console.log(`\n${C.bold}${C.white}📈 DECODED TELEMETRY STREAM TRACE (CarData.z -> Inflated Channels & GPS Position):${C.reset}`);
  console.log(`${C.dim}Time (UTC)      Speed (km/h)       RPM              Gear   Throttle      Brake    DRS   Circuit Location & Coordinates${C.reset}`);
  console.log(`${C.dim}───────────────────────────────────────────────────────────────────────────────────────────────────────────────────${C.reset}`);

  // Display a representative slice or all points
  const displayLimit = options.limit || 25;
  const step = Math.max(1, Math.floor(points.length / displayLimit));
  const sampledPoints = [];
  for (let i = 0; i < points.length; i += step) {
    sampledPoints.push(points[i]);
  }

  for (const pt of sampledPoints) {
    const timeStr = pt.utc.substring(11, 19);
    const speedStr = String(pt.speed).padStart(3);
    const speedBar = renderProgressBar(pt.speed, 340, 8, C.yellow);
    const rpmStr = String(pt.rpm).padStart(5);
    const rpmBar = renderProgressBar(pt.rpm, 13000, 6, C.cyan);
    const gearStr = `G${pt.gear}`;
    const throttleStr = `${String(pt.throttle).padStart(3)}%`;
    const throttleBar = renderProgressBar(pt.throttle, 100, 5, C.green);
    const brakeTag = renderBrakeTag(pt.brake);
    const drsTag = renderDrsTag(pt.drsActive);
    const coordsStr = pt.x !== null ? `(X:${String(pt.x).padStart(4)}m, Y:${String(pt.y).padStart(4)}m)` : '';
    const locStr = `${pt.location || 'Track'} ${C.cyan}${coordsStr}${C.reset}`;

    console.log(
      `${C.bold}${timeStr}${C.reset}   ` +
      `${speedStr} ${speedBar}   ` +
      `${rpmStr} ${rpmBar}   ` +
      `${C.bold}${C.magenta}${gearStr}${C.reset}   ` +
      `${throttleStr} ${throttleBar}  ` +
      `${brakeTag} ` +
      `${drsTag}  ` +
      `${locStr}`
    );
  }

  console.log(`${C.dim}───────────────────────────────────────────────────────────────────────────────────────────────────────────────────${C.reset}`);
  console.log(`${C.dim}Displaying ${sampledPoints.length} representative checkpoints across ${points.length} decoded stream frames.${C.reset}\n`);

  return result;
}

/**
 * Animated real-time simulation playback of the telemetry trace
 */
const runRaceSimulation = require('./race_simulator');

async function streamPlayback(loader, driverInput, options = {}) {
  await runRaceSimulation(loader, driverInput, options);
}

function interactivePrompt(loader) {
  const drivers = loader.getDrivers();
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });

  console.clear();
  console.log(`\n${C.bold}${C.cyan}╔══════════════════════════════════════════════════════════════════════════╗${C.reset}`);
  console.log(`${C.bold}${C.cyan}║${C.reset}  ${C.bold}${C.yellow}FASTESTLAP — F1 LIVETIMING SIGNALR TELEMETRY STREAM TEST SUITE${C.reset}      ${C.bold}${C.cyan}║${C.reset}`);
  console.log(`${C.bold}${C.cyan}╚══════════════════════════════════════════════════════════════════════════╝${C.reset}`);
  console.log(`${C.bold}Session:${C.reset}  FORMULA 1 PETRONAS MALAYSIAN GRAND PRIX 2026`);
  console.log(`${C.bold}Circuit:${C.reset}  Sepang International Circuit (5.543 km)`);
  console.log(`${C.bold}Date:${C.reset}     Domenica 4 Ottobre 2026 (09:00:00 ➔ 10:30:40 UTC — 56 Giri)`);
  console.log(`\n${C.bold}${C.white}Available Drivers in Telemetry Stream:${C.reset}`);

  const driverKeys = Object.keys(drivers);
  driverKeys.forEach((key, index) => {
    const d = drivers[key];
    console.log(`  [${C.bold}${C.cyan}${index + 1}${C.reset}] #${String(d.number).padEnd(2)} ${C.bold}${d.code.padEnd(3)}${C.reset} - ${(d.firstName + ' ' + d.lastName).padEnd(20)} (${d.team})`);
  });

  rl.question(`\n${C.bold}Select a driver (number, acronym, or list index 1-${driverKeys.length}): ${C.reset}`, (answer) => {
    let selected = answer.trim();
    const index = parseInt(selected, 10);
    if (!isNaN(index) && index >= 1 && index <= driverKeys.length) {
      selected = driverKeys[index - 1];
    }

    try {
      displayDriverTelemetry(loader, selected);
      
      rl.question(`\n[P] Live Playback stream  |  [Q] Exit : `, async (action) => {
        if (action.trim().toUpperCase() === 'P') {
          await streamPlayback(loader, selected);
        }
        rl.close();
      });
    } catch (err) {
      console.error(`\n${C.red}Error:${C.reset} ${err.message}`);
      rl.close();
    }
  });
}

function parseCliArgs() {
  const args = process.argv.slice(2);
  const options = {};

  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--driver' || args[i] === '-d') {
      options.driver = args[++i];
    } else if (args[i] === '--stream' || args[i] === '-s') {
      options.stream = true;
    } else if (args[i] === '--start') {
      options.start = args[++i];
    } else if (args[i] === '--end') {
      options.end = args[++i];
    } else if (args[i] === '--list' || args[i] === '-l') {
      options.list = true;
    } else if (args[i] === '--help' || args[i] === '-h') {
      options.help = true;
    } else if (!args[i].startsWith('-') && !options.driver) {
      options.driver = args[i];
    }
  }

  return options;
}

async function main() {
  const loader = new SessionLoader();
  loader.load();

  const options = parseCliArgs();

  if (options.help) {
    console.log(`
Usage: node telemetry_viewer.js [options]

Options:
  -d, --driver <num|code>   Specify driver number or code (e.g. 16, LEC, 1, VER)
  -s, --stream              Simulate live stream playback in console
  --start <iso-date>        Filter start time (e.g. 2026-10-04T10:30:00Z)
  --end <iso-date>          Filter end time (e.g. 2026-10-04T12:30:00Z)
  -l, --list                List all available drivers
  -h, --help                Show this help message
    `);
    process.exit(0);
  }

  if (options.list) {
    const drivers = loader.getDrivers();
    console.log('\nAvailable drivers:');
    for (const [key, d] of Object.entries(drivers)) {
      console.log(`  #${d.number} ${d.code} - ${d.firstName} ${d.lastName} (${d.team})`);
    }
    process.exit(0);
  }

  if (options.driver) {
    if (options.stream) {
      await streamPlayback(loader, options.driver);
    } else {
      displayDriverTelemetry(loader, options.driver, options);
    }
  } else {
    interactivePrompt(loader);
  }
}

if (require.main === module) {
  main().catch((err) => console.error('Fatal error:', err));
}

module.exports = {
  displayDriverTelemetry,
  streamPlayback
};

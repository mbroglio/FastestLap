/**
 * Sepang International Circuit 2D Track Geometry & GPS Coordinates
 * Circuit Length: 5.543 km | 15 Turns | Calibrated strictly to Sepang.svg.webp
 * Provides continuous (X, Y) Cartesian coordinates in meters, SVG pixel coordinates (1280x1057),
 * sector definitions, exact 453-node track centerline, and ASCII map rendering.
 */

const fs = require('fs');
const path = require('path');

// Image reference dimensions from Wikimedia Commons Sepang.svg.webp
const SVG_WIDTH = 1280;
const SVG_HEIGHT = 1057;

// Calibration: 5543m circuit length / ~5697px circuit path ≈ 0.973 meters per SVG pixel
const METERS_PER_PX = 0.973;
const CENTER_X = 640;
const CENTER_Y = 530;

// Load 453 exact track nodes directly sampled from Sepang.svg.webp dashed centerline
let EXACT_TRACK_NODES;
try {
  EXACT_TRACK_NODES = JSON.parse(
    fs.readFileSync(path.join(__dirname, '../data/sepang_exact_track_full.json'), 'utf-8')
  );
} catch (err) {
  EXACT_TRACK_NODES = [];
}

/**
 * Key corner turn annotations matching Sepang.svg.webp
 */
const CORNER_MARKERS = [
  { turn: 'S/F', name: 'Start/Finish Line', px: 714, py: 538, sector: 1 },
  { turn: 'T1',  name: 'Turn 1 Hairpin Right', px: 25, py: 570, sector: 1 },
  { turn: 'T2',  name: 'Turn 2 Left Switchback', px: 115, py: 515, sector: 1 },
  { turn: 'T3',  name: 'Turn 3 Long Sweeper', px: 75, py: 400, sector: 1 },
  { turn: 'T4',  name: 'Turn 4 90° Right (North Summit)', px: 597, py: 6, sector: 2 },
  { turn: 'T5',  name: 'Turn 5 Fast Left (Esses)', px: 770, py: 359, sector: 2 },
  { turn: 'T6',  name: 'Turn 6 Fast Right (Esses)', px: 970, py: 296, sector: 2 },
  { turn: 'T7',  name: 'Turn 7 Fast Right (East Apex)', px: 1254, py: 560, sector: 2 },
  { turn: 'T8',  name: 'Turn 8 Right-Hander', px: 1215, py: 665, sector: 2 },
  { turn: 'T9',  name: 'Turn 9 Uphill Hairpin', px: 751, py: 776, sector: 3 },
  { turn: 'T10', name: 'Turn 10 Medium Right', px: 820, py: 845, sector: 3 },
  { turn: 'T11', name: 'Turn 11 Hairpin Left (South Apex)', px: 631, py: 971, sector: 3 },
  { turn: 'T12', name: 'Turn 12 Sweeper Left', px: 450, py: 855, sector: 3 },
  { turn: 'T13', name: 'Turn 13 Medium Right', px: 255, py: 900, sector: 3 },
  { turn: 'T14', name: 'Turn 14 Entry to Back Straight', px: 128, py: 792, sector: 3 },
  { turn: 'T15', name: 'Turn 15 Final 180° Hairpin', px: 1088, py: 540, sector: 3 }
];

/**
 * Returns exact coordinates, SVG pixels, speed, and telemetry state for any second of the 95s lap
 */
function getTrackPointAtSecond(sec) {
  if (!EXACT_TRACK_NODES || EXACT_TRACK_NODES.length === 0) {
    return {
      x: 72, y: -8, px: 714, py: 538,
      location: 'Main Straight', sector: 1, turn: 0,
      speed: 280, gear: 8, rpm: 12100, throttle: 100, brake: 0, drs: 1
    };
  }

  const t = ((sec % 95) + 95) % 95;
  const N = EXACT_TRACK_NODES.length;

  // Binary search across 453 nodes
  let low = 0, high = N - 1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (EXACT_TRACK_NODES[mid].t <= t) {
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }

  const i1 = Math.max(0, high);
  const i2 = (i1 + 1) % N;
  const p1 = EXACT_TRACK_NODES[i1];
  const p2 = EXACT_TRACK_NODES[i2];

  let dt = p2.t - p1.t;
  if (dt <= 0) dt += 95.0;
  const u = dt === 0 ? 0 : Math.max(0, Math.min(1, (t - p1.t) / dt));

  const px = p1.px + (p2.px - p1.px) * u;
  const py = p1.py + (p2.py - p1.py) * u;
  const x = Math.round((px - CENTER_X) * METERS_PER_PX);
  const y = Math.round((CENTER_Y - py) * METERS_PER_PX);

  return {
    px,
    py,
    x,
    y,
    sector: u < 0.5 ? p1.sector : p2.sector,
    speed: Math.round(p1.speed + (p2.speed - p1.speed) * u),
    gear: u < 0.5 ? p1.gear : p2.gear,
    rpm: Math.round(p1.rpm + (p2.rpm - p1.rpm) * u),
    throttle: Math.round(p1.throttle + (p2.throttle - p1.throttle) * u),
    brake: u < 0.5 ? p1.brake : p2.brake,
    drs: u < 0.5 ? p1.drs : p2.drs,
    location: u < 0.5 ? p1.location : p2.location,
    turn: u < 0.5 ? (p1.turn || 0) : (p2.turn || 0)
  };
}

/**
 * Pre-computes integer second track path points (0..94)
 */
const TRACK_PATH = [];
for (let s = 0; s < 95; s++) {
  TRACK_PATH.push(getTrackPointAtSecond(s));
}

/**
 * Converts circuit coordinates to terminal grid (col, row) matching Sepang.svg.webp
 */
function toGridCoords(px, py, width = 37, height = 10) {
  const col = Math.max(0, Math.min(width - 1, Math.round((px / SVG_WIDTH) * (width - 1))));
  const row = Math.max(0, Math.min(height - 1, Math.round((py / SVG_HEIGHT) * (height - 1))));
  return { col, row };
}

/**
 * Renders ASCII 2D Track Map of Sepang with current car position highlighted
 */
function renderAsciiTrackMap(currentX, currentY, driverCode = '16', width = 37, height = 10) {
  const lines = getAsciiTrackMapLines(currentX, currentY, driverCode, width, height);
  return lines.join('\n');
}

/**
 * Returns lines array of the track map for multi-column terminal layout
 */
function getAsciiTrackMapLines(currentX, currentY, driverCode = '16', width = 37, height = 10) {
  const grid = Array.from({ length: height }, () => Array(width).fill(' '));

  // 1. Draw track path outline using all 453 exact nodes
  const nodes = EXACT_TRACK_NODES.length > 0 ? EXACT_TRACK_NODES : TRACK_PATH;
  for (const pt of nodes) {
    const { col, row } = toGridCoords(pt.px, pt.py, width, height);
    if (grid[row][col] === ' ') {
      grid[row][col] = '·';
    }
  }

  // 2. Draw Key Corner Labels matching Sepang.svg.webp
  const cornerLabels = [
    { text: 'T4', px: 597, py: 6 },
    { text: 'T6', px: 970, py: 296 },
    { text: 'T1', px: 25, py: 570 },
    { text: 'T7', px: 1254, py: 560 },
    { text: 'T9', px: 751, py: 776 },
    { text: 'T11', px: 631, py: 971 },
    { text: 'T15', px: 1088, py: 540 }
  ];

  for (const lbl of cornerLabels) {
    const { col, row } = toGridCoords(lbl.px, lbl.py, width, height);
    if (col + lbl.text.length < width) {
      for (let c = 0; c < lbl.text.length; c++) {
        grid[row][col + c] = lbl.text[c];
      }
    }
  }

  // 3. Draw current car dot (convert from Cartesian meters to SVG px)
  const carPx = CENTER_X + (currentX / METERS_PER_PX);
  const carPy = CENTER_Y - (currentY / METERS_PER_PX);
  const carPos = toGridCoords(carPx, carPy, width, height);
  grid[carPos.row][carPos.col] = '🔴';

  return grid.map(r => r.join(''));
}

module.exports = {
  SVG_WIDTH,
  SVG_HEIGHT,
  METERS_PER_PX,
  CORNER_MARKERS,
  EXACT_TRACK_NODES,
  TRACK_PATH,
  getTrackPointAtSecond,
  toGridCoords,
  renderAsciiTrackMap,
  getAsciiTrackMapLines
};

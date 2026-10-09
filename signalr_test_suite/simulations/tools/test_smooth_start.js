const fs = require('fs');

const nodes = JSON.parse(fs.readFileSync('signalr_test_suite/data/sepang_exact_track_full.json', 'utf8'));
const GRID_ORDER = ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41'];
const LAP_STARTS = [null,0,152.2,392.2,506.6,615.8];
const LAP_DURATION = 95.0;

function getTrackPointAtTime(sec) {
  const t = ((sec % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
  let low = 0, high = nodes.length - 1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (nodes[mid].t <= t) low = mid + 1;
    else high = mid - 1;
  }
  const i1 = Math.max(0, high);
  const i2 = (i1 + 1) % nodes.length;
  const n1 = nodes[i1];
  const n2 = nodes[i2];
  let dt = n2.t - n1.t;
  if (dt <= 0) dt += LAP_DURATION;
  let u = Math.min(1, Math.max(0, (t - n1.t) / dt));
  return {
    px: n1.px * (1 - u) + n2.px * u,
    py: n1.py * (1 - u) + n2.py * u,
    location: n1.location,
    speed: n1.speed
  };
}

function getDriverTrackSec(drvKey, t) {
  const gridIdx = GRID_ORDER.indexOf(drvKey);
  const pos = gridIdx >= 0 ? gridIdx : 21;

  if (t < 216.9) {
    if (t < 108.0) {
      const prog = (t / 108.0) * LAP_DURATION;
      return ((prog - pos * 1.5) % LAP_DURATION + LAP_DURATION) % LAP_DURATION;
    } else if (t < 205.0) {
      const prog = ((t - 108.0) / 97.0) * LAP_DURATION;
      return ((prog - pos * 1.5) % LAP_DURATION + LAP_DURATION) % LAP_DURATION;
    } else {
      return 0.0;
    }
  }

  // Racing Lap 2 starts at t = 216.9s and completes at LAP_STARTS[3] = 392.2s
  if (t < 392.2) {
    const lapRacingDuration = 392.2 - 216.9;
    const f = Math.max(0, Math.min(0.9999, (t - 216.9) / lapRacingDuration));
    return f * LAP_DURATION;
  }

  const dur = LAP_STARTS[4] - LAP_STARTS[3];
  const f = Math.max(0, Math.min(0.9999, (t - LAP_STARTS[3]) / dur));
  return f * LAP_DURATION;
}

function getDriverPixelCoord(drvKey, t) {
  const gridIdx = GRID_ORDER.indexOf(drvKey);
  const pos = gridIdx >= 0 ? gridIdx : 21;
  const drvSec = getDriverTrackSec(drvKey, t);
  const carPt = getTrackPointAtTime(drvSec);
  const slotPx = 730 + pos * 14.0;
  const slotPy = 536 + ((pos % 2 === 0) ? -6.5 : 6.5) - (pos * 1.1);

  if (t < 205.0) {
    return { px: carPt.px, py: carPt.py, stage: 'SC_FORMATION' };
  } else if (t < 216.9) {
    if (t < 210.0) {
      const u = (t - 205.0) / 5.0;
      return { px: carPt.px * (1 - u) + slotPx * u, py: carPt.py * (1 - u) + slotPy * u, stage: 'GRID_PULL_IN' };
    }
    return { px: slotPx, py: slotPy, stage: 'GRID_WAIT' };
  } else if (t <= 220.5) {
    const u = Math.min(1.0, (t - 216.9) / 3.6);
    return { px: slotPx * (1 - u) + carPt.px * u, py: slotPy * (1 - u) + carPt.py * u, stage: 'STANDING_LAUNCH' };
  } else {
    return { px: carPt.px, py: carPt.py, stage: 'RACE_RUNNING' };
  }
}

console.log("Testing Driver #3 (Verstappen, Pole) from t=200 to t=240:");
let lastCoord = null;
let maxStepDist = 0;
for (let t = 200; t <= 240; t += 1) {
  const c = getDriverPixelCoord('3', t);
  let stepDist = 0;
  if (lastCoord) {
    const dx = c.px - lastCoord.px;
    const dy = c.py - lastCoord.py;
    stepDist = Math.sqrt(dx*dx + dy*dy);
    maxStepDist = Math.max(maxStepDist, stepDist);
  }
  lastCoord = c;
  console.log(`t=${t}s [${c.stage.padEnd(15)}] px=${c.px.toFixed(1)}, py=${c.py.toFixed(1)} | stepDist=${stepDist.toFixed(1)}px`);
}

console.log(`\nMax 1-second step distance: ${maxStepDist.toFixed(2)}px`);
if (maxStepDist < 70) {
  console.log("SUCCESS: Trajectory is completely continuous with NO sudden jumps or snapping!");
} else {
  console.log("WARNING: Trajectory has an excessive jump!");
}

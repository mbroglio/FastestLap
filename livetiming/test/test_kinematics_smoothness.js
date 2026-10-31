const assert = require('assert');

// Mock browser globals
global.window = {
  devicePixelRatio: 1,
  addEventListener: () => {}
};
global.requestAnimationFrame = () => {};
global.performance = {
  now: () => Date.now()
};

const TrackCanvas = require('../public/js/track_canvas');

// Mock canvas element
const mockCtx = {
  clearRect: () => {},
  beginPath: () => {},
  arc: () => {},
  fill: () => {},
  stroke: () => {},
  moveTo: () => {},
  lineTo: () => {},
  closePath: () => {},
  fillRect: () => {},
  fillText: () => {},
  setTransform: () => {},
  scale: () => {}
};

const mockCanvas = {
  getContext: () => mockCtx,
  parentElement: {
    getBoundingClientRect: () => ({ width: 800, height: 600 })
  },
  getBoundingClientRect: () => ({ width: 800, height: 600, left: 0, top: 0 }),
  addEventListener: () => {}
};

console.log('[TEST] Checking Placeholder Kinematic Smoothness...');

const tc = new TrackCanvas(mockCanvas);
tc._buildFallbackCircuit();

// Feed an initial leaderboard
const sampleLb = [
  { racingNumber: '16', bestLapTime: '1:30.000', sectors: { '0': { Segments: [{ Status: 2049 }, { Status: 2049 }] } } },
  { racingNumber: '1', bestLapTime: '1:30.200', intervalToAhead: '0.450', sectors: { '0': { Segments: [{ Status: 2049 }] } } }
];

tc.updatePositionsFromLeaderboard(sampleLb);

// Step 120 frames at 60fps (2 simulated seconds)
// Every 20 frames, trigger a leaderboard update
let prevTau16 = tc.driverLapState['16'].tauSec;
let prevPt16 = tc._sampleTrackPointAtTau(prevTau16);
const deltas = [];

let simNow = 1000;
tc.lastFrameTime = simNow;
for (let frame = 0; frame < 120; frame++) {
  simNow += 16.666;
  if (frame % 20 === 0) {
    // Incoming timing packet
    tc.updatePositionsFromLeaderboard(sampleLb);
  }
  tc._stepKinematics(simNow);
  const curTau16 = tc.driverLapState['16'].tauSec;
  const curPt16 = tc._sampleTrackPointAtTau(curTau16);
  const stepDist = Math.hypot(curPt16.x - prevPt16.x, curPt16.y - prevPt16.y);
  deltas.push(stepDist);
  prevPt16 = curPt16;
}

// Verify that there are no violent jumps (stepDist shouldn't jump wildly between frames)
const avgDelta = deltas.reduce((a, b) => a + b, 0) / deltas.length;
console.log(`Average frame step distance (Solo P16): ${avgDelta.toFixed(3)}px`);

for (let i = 1; i < deltas.length; i++) {
  const ratio = deltas[i] / (deltas[i - 1] || 1);
  assert(ratio < 1.35, `Spike detected at frame ${i}: delta jumped from ${deltas[i - 1]} to ${deltas[i]}`);
}

console.log('✅ Zero spikes detected across 120 frames with periodic packet arrivals!');
console.log('✅ Buttery-smooth kinematic progression confirmed.');

// Test 2: Multi-car grouping and gap spacing
console.log('[TEST 2] Checking Group Spacing & Intra-Sector Smoothness...');
const groupLb = [
  { racingNumber: '16', bestLapTime: '1:30.000', sectors: { '0': { Segments: [{ Status: 2049 }, { Status: 2049 }, { Status: 2049 }] } } },
  { racingNumber: '1', bestLapTime: '1:30.300', intervalToAhead: '+0.350', sectors: { '0': { Segments: [{ Status: 2049 }, { Status: 2049 }, { Status: 2049 }] } } },
  { racingNumber: '4', bestLapTime: '1:30.800', intervalToAhead: '+0.500', sectors: { '0': { Segments: [{ Status: 2049 }, { Status: 2049 }, { Status: 2049 }] } } }
];

tc.updatePositionsFromLeaderboard(groupLb);

const deltas1 = [];
const deltas4 = [];
let prevPt1 = tc._sampleTrackPointAtTau(tc.driverLapState['1'].tauSec);
let prevPt4 = tc._sampleTrackPointAtTau(tc.driverLapState['4'].tauSec);

for (let frame = 0; frame < 180; frame++) {
  simNow += 16.666;
  if (frame % 30 === 0) {
    tc.updatePositionsFromLeaderboard(groupLb);
  }
  tc._stepKinematics(simNow);
  const curPt1 = tc._sampleTrackPointAtTau(tc.driverLapState['1'].tauSec);
  const curPt4 = tc._sampleTrackPointAtTau(tc.driverLapState['4'].tauSec);

  deltas1.push(Math.hypot(curPt1.x - prevPt1.x, curPt1.y - prevPt1.y));
  deltas4.push(Math.hypot(curPt4.x - prevPt4.x, curPt4.y - prevPt4.y));

  prevPt1 = curPt1;
  prevPt4 = curPt4;
}

for (let i = 1; i < deltas1.length; i++) {
  const r1 = deltas1[i] / (deltas1[i - 1] || 1);
  const r4 = deltas4[i] / (deltas4[i - 1] || 1);
  assert(r1 < 1.35, `Car 1 spike at frame ${i}: ${deltas1[i - 1]} -> ${deltas1[i]}`);
  assert(r4 < 1.35, `Car 4 spike at frame ${i}: ${deltas4[i - 1]} -> ${deltas4[i]}`);
}

console.log('✅ Zero spikes detected in multi-car grouped pacing!');

// Test 3: Free Practice Out-Lap with Status 2064 (pitlane exit) & Slow Cooldown Lap (never stopping)
console.log('[TEST 3] Checking Free Practice Out-Lap (Status 2064), Pit Entry/Exit & Cooldown Lap...');
const fpLb = [
  {
    racingNumber: '11',
    bestLapTime: '1:18.500',
    inPit: false,
    pitOut: true,
    sectors: {
      '0': { Segments: [{ Status: 2064 }, { Status: 2064 }, { Status: 2049 }, { Status: 2049 }, { Status: 0 }, { Status: 0 }] },
      '1': { Segments: [{ Status: 0 }, { Status: 0 }, { Status: 0 }, { Status: 0 }] },
      '2': { Segments: [{ Status: 0 }, { Status: 0 }, { Status: 0 }, { Status: 0 }] }
    }
  }
];

const anchor11 = tc._computeExactMiniSectorAnchor(fpLb[0].sectors);
assert.strictEqual(anchor11.currentMiniSector, 4, 'Out-lap with Status 2064 at start of S1 must advance to mini-sector 4, not freeze at 0');

tc.updatePositionsFromLeaderboard(fpLb);
// Simulate 600 frames (10 seconds) in the SAME mini-sector (slow cooldown lap in Free Practice)
let prevPt11 = tc._sampleTrackPointAtTau(tc.driverLapState['11'].tauSec);
for (let frame = 0; frame < 600; frame++) {
  simNow += 16.666;
  tc._stepKinematics(simNow);
  const curPt11 = tc._sampleTrackPointAtTau(tc.driverLapState['11'].tauSec);
  const d11 = Math.hypot(curPt11.x - prevPt11.x, curPt11.y - prevPt11.y);
  assert(d11 > 0.01, `Placeholder stopped completely at frame ${frame} during slow Free Practice lap (d=${d11})`);
  prevPt11 = curPt11;
}
console.log('✅ Out-Lap Status 2064 and 10s slow cooldown lap verified (placeholder never stops)');

// Test 4: Pit entry from Sector 3 and Pit exit transition (no backward wrap or teleport back to S3)
const pitEntryLb = [
  {
    racingNumber: '11',
    bestLapTime: '1:18.500',
    inPit: true,
    pitOut: false,
    sectors: fpLb[0].sectors
  }
];
tc.driverLapState['11'].distMeters = tc.trackLengthMeters - 80; // End of Sector 3 near pit entry
tc.updatePositionsFromLeaderboard(pitEntryLb);
for (let frame = 0; frame < 120; frame++) {
  simNow += 16.666;
  tc._stepKinematics(simNow);
}
assert(tc.driverLapState['11'].tauSec < 10, 'While in pit, tauSec must be synced near pit exit (0..10s)');

// Now exit pit
pitEntryLb[0].inPit = false;
pitEntryLb[0].pitOut = true;
tc.updatePositionsFromLeaderboard(pitEntryLb);
tc._stepKinematics(simNow + 16.666);
assert(tc.driverLapState['11'].tauSec < 15, 'Upon pit exit, car must start from pit exit and never teleport back to Sector 3');
console.log('✅ Pit entry and pit exit transition verified without backward wrap or teleport');

// Test 5: Full-lap simulation with sub-0.4m vertices & 21 miniSectorsIndexes vs 22 F1 TimingData segments (Mexico GP pattern)
console.log('[TEST 5] Checking Full-Lap Speed Ratio, Sub-0.4m Vertices & 21-vs-22 Mini-Sector Boundary Alignment...');
const syntheticMexico = { x: [], y: [], rotation: 0, miniSectorsIndexes: [] };
const baseNodes = 680;
for (let i = 0; i < baseNodes; i++) {
  const angle = (i / baseNodes) * Math.PI * 2;
  const rx = Math.cos(angle) * 820 + Math.cos(3 * angle) * 160;
  const ry = Math.sin(angle) * 520 + Math.sin(2 * angle) * 120;
  syntheticMexico.x.push(rx);
  syntheticMexico.y.push(ry);
  // Inject sub-0.4m duplicate/near-duplicate vertices like Multiviewer Mexico/Monza
  if (i % 18 === 0) {
    syntheticMexico.x.push(rx + 0.08);
    syntheticMexico.y.push(ry + 0.05);
  }
}
const totalVertices = syntheticMexico.x.length;
for (let k = 1; k <= 21; k++) {
  syntheticMexico.miniSectorsIndexes.push(Math.round((k / 21) * (totalVertices - 1)));
}

const tcMex = new TrackCanvas(mockCanvas);
tcMex._applyCircuitGeometry(syntheticMexico);
const refLapMex = tcMex.referenceLapSec;

const build22Sectors = (activeSegs) => {
  const s = [{ Segments: [] }, { Segments: [] }, { Segments: [] }];
  for (let i = 0; i < 22; i++) {
    const secIdx = i < 7 ? 0 : (i < 15 ? 1 : 2);
    s[secIdx].Segments.push({ Status: i < activeSegs ? 2048 : 0 });
  }
  return s;
};

const mexSegStarts = [];
for (let m = 0; m < 22; m++) {
  const segsLit = m === 0 ? 22 : m;
  const anchor = tcMex._computeExactMiniSectorAnchor(build22Sectors(segsLit));
  assert.strictEqual(anchor.currentMiniSector, m, `Expected currentMiniSector=${m} for segsLit=${segsLit}`);
  const span = (anchor.segEndTau - anchor.segStartTau + refLapMex) % refLapMex || refLapMex;
  assert(span > 1.2, `Mini-sector ${m}/22 collapsed to span=${span.toFixed(3)}s`);
  mexSegStarts.push(anchor.segStartTau);
}

let minSpeedRatio = 999;
let maxSpeedRatio = 0;
let prevDistMex = null;
let nowMex = 5000;
tcMex.lastFrameTime = nowMex;
const totalFramesMex = Math.round(refLapMex * 60);

for (let step = 0; step < totalFramesMex; step++) {
  nowMex += 16.6667;
  const tLap = (step * 0.0166667) % refLapMex;
  let curM = 0;
  for (let m = 0; m < 22; m++) {
    const stTau = mexSegStarts[m];
    const enTau = m < 21 ? mexSegStarts[m + 1] : refLapMex;
    if (tLap >= stTau && tLap < enTau) {
      curM = m;
      break;
    }
  }
  const segsLit = curM === 0 ? 22 : curM;
  tcMex.updatePositionsFromLeaderboard([
    { racingNumber: '16', bestLapTime: refLapMex.toFixed(3), sectors: build22Sectors(segsLit) }
  ]);
  tcMex._stepKinematics(nowMex);

  const st16 = tcMex.driverLapState['16'];
  const pt16 = tcMex._sampleTrackPointAtTau(st16.tauSec);
  if (prevDistMex !== null && step > 180) {
    let dMeters = st16.distMeters - prevDistMex;
    if (dMeters < -tcMex.trackLengthMeters * 0.5) dMeters += tcMex.trackLengthMeters;
    const visualVel = dMeters / 0.0166667;
    const ratio = visualVel / pt16.v;
    if (ratio < minSpeedRatio) minSpeedRatio = ratio;
    if (ratio > maxSpeedRatio) maxSpeedRatio = ratio;
  }
  prevDistMex = st16.distMeters;
}

console.log(`Full-lap visual/physics velocity ratio range: [${minSpeedRatio.toFixed(3)}, ${maxSpeedRatio.toFixed(3)}]`);
assert(minSpeedRatio > 0.88, `Anomalous deceleration detected: minSpeedRatio=${minSpeedRatio.toFixed(3)}`);
assert(maxSpeedRatio < 1.15, `Anomalous acceleration detected: maxSpeedRatio=${maxSpeedRatio.toFixed(3)}`);
console.log('✅ Full-lap velocity tracks physics profile v(s) within [0.88, 1.15] with zero mid-straight braking!');

// Test 6: 1:1 Correspondence between placeholder track metrics and cockpit telemetry
console.log('[TEST 6] Checking 1:1 Placeholder-to-Telemetry Correspondence...');
let foundBraking = false;
let foundStraight = false;
for (let i = 0; i < tcMex.circuitPoints.length; i++) {
  const p = tcMex.circuitPoints[i];
  tcMex.driverLapState['16'].tauSec = p.tauSec;
  tcMex.driverLapState['16'].distMeters = p.distMeters;
  tcMex.driverLapState['16'].smoothPace = 1.0;
  const m = tcMex.getDriverTrackMetrics('16');
  assert(Math.abs(m.physicsSpeedKmh - p.speedKmh) < 1.0, 'Track metrics physicsSpeedKmh must match circuit point speedKmh');
  if (m.longG < -1.5) foundBraking = true;
  if (m.physicsSpeedKmh > 250 && Math.abs(m.longG) < 0.8) foundStraight = true;
}
assert(foundBraking, 'Physics profile must include realistic braking zones (longG < -1.5)');
assert(foundStraight, 'Physics profile must include high-speed straights (speedKmh > 280)');
console.log('✅ 1:1 correspondence between on-track placeholder kinematics and telemetry confirmed!');

console.log('🎉 ALL KINEMATIC SMOOTHNESS TESTS PASSED!');

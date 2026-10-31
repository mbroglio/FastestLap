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

console.log('🎉 ALL KINEMATIC SMOOTHNESS TESTS PASSED!');

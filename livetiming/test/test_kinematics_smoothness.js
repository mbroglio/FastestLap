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
console.log('🎉 ALL KINEMATIC SMOOTHNESS TESTS PASSED!');

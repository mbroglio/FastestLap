/**
 * test_live_timing_engine.js
 * Unit Test Suite for Generic LiveTimingEngine
 */

const assert = require('assert');
const TrackModel = require('../src/engine/TrackModel');
const LiveTimingEngine = require('../src/engine/LiveTimingEngine');
const TrackMapAnalyzer = require('../src/engine/TrackMapAnalyzer');

async function testLiveTimingEngine() {
  console.log('\n===============================================================================');
  console.log('TEST SUITE: Generic LiveTimingEngine (Timing Rules, Colors & Standings)');
  console.log('===============================================================================');

  // [1] Initialize synthetic circular track for testing
  const rawCircle = [];
  for (let deg = 0; deg < 360; deg += 10) {
    const rad = (deg * Math.PI) / 180;
    rawCircle.push({ px: 500 + 200 * Math.cos(rad), py: 500 + 200 * Math.sin(rad) });
  }
  const nodes = TrackMapAnalyzer.sampleCenterline(rawCircle, { targetCount: 100, totalLapDuration: 90.0 });
  const sectors = TrackMapAnalyzer.configureSectors(nodes, 30.0, 60.0);
  const trackModel = new TrackModel({
    circuitId: 'test_circ',
    circuitName: 'Test Circuit',
    lapDuration: 90.0,
    trackNodes: nodes,
    sectors
  });

  const engine = new LiveTimingEngine(trackModel);

  // [2] Register Drivers & Grid
  const drivers = {
    '1': { number: '1', code: 'VER', name: 'Max Verstappen', team: 'Red Bull', color: '#3671c6' },
    '44': { number: '44', code: 'HAM', name: 'Lewis Hamilton', team: 'Ferrari', color: '#e8002d' },
    '63': { number: '63', code: 'RUS', name: 'George Russell', team: 'Mercedes', color: '#27f4d2' }
  };
  engine.setDrivers(drivers);
  engine.setGridOrder(['1', '44', '63']);
  engine.configureSession({
    totalLaps: 10,
    formationLapsCount: 1,
    raceStartTimeSec: 90.0,
    raceFinishTimeSec: 990.0
  });

  console.log('[1] Testing Session Phases...');
  assert.strictEqual(engine.getSessionPhase(45.0), 'FORMATION_LAP', 'Phase at t=45 should be FORMATION_LAP');
  assert.strictEqual(engine.getSessionPhase(150.0), 'RACING', 'Phase at t=150 should be RACING');
  assert.strictEqual(engine.getSessionPhase(1000.0), 'FINISHED', 'Phase at t=1000 should be FINISHED');
  console.log('  ✅ Session phases correctly identified (FORMATION -> RACING -> FINISHED)');

  // [3] Test Timing Color Determination
  console.log('\n[2] Testing Sector & Lap Color Rules (Purple / Green / Yellow)...');
  engine.loadLaps({
    '1': [
      { lap: 1, s1: 30.5, s2: 30.2, s3: 30.1, dur: 90.8 },
      { lap: 2, s1: 29.8, s2: 29.9, s3: 29.8, dur: 89.5 }
    ],
    '44': [
      { lap: 1, s1: 31.0, s2: 30.5, s3: 30.5, dur: 92.0 },
      { lap: 2, s1: 30.1, s2: 30.0, s3: 30.0, dur: 90.1 }
    ],
    '63': [
      { lap: 1, s1: 29.5, s2: 29.4, s3: 29.5, dur: 88.4 } // Overall fastest lap
    ]
  });

  // #63 Lap 1 (88.4) is overall fastest -> should be PURPLE
  const rusLapColor = engine.getTimingColor('63', 'lap', 88.4);
  assert.strictEqual(rusLapColor, '#d054fa', 'Overall fastest lap must be Purple');

  // #1 Lap 2 (89.5) is personal best for #1 but not overall best -> should be GREEN
  const verPbLapColor = engine.getTimingColor('1', 'lap', 89.5);
  assert.strictEqual(verPbLapColor, '#00e676', 'Personal best lap must be Green');

  // #1 Lap 1 (90.8) is slower than personal best -> should be YELLOW
  const verSlowerLapColor = engine.getTimingColor('1', 'lap', 90.8);
  assert.strictEqual(verSlowerLapColor, '#ffeb3b', 'Slower lap must be Yellow');

  console.log('  ✅ Color logic verified: Purple (88.4s overall best), Green (89.5s personal best), Yellow (90.8s slower)');

  // [4] Test Live Leaderboard Snapshot & Standings
  console.log('\n[3] Testing Leaderboard Ranking & Intervals...');
  const snapshot = engine.getSnapshot(270.0); // During active race
  assert(snapshot.leaderboard, 'Snapshot must include leaderboard');
  assert.strictEqual(snapshot.leaderboard.length, 3, 'All 3 drivers must be ranked');
  assert.strictEqual(snapshot.leaderboard[0].position, 1, 'P1 leader position');
  assert.strictEqual(snapshot.leaderboard[0].gap, 'LEADER', 'Leader gap must be LEADER');
  assert(snapshot.leaderboard[1].gap.startsWith('+'), 'P2 gap must start with +');
  console.log(`  ✅ Leaderboard snapshot generated: P1 ${snapshot.leaderboard[0].code}, P2 ${snapshot.leaderboard[1].code} (${snapshot.leaderboard[1].gap}), P3 ${snapshot.leaderboard[2].code} (${snapshot.leaderboard[2].gap})`);

  console.log('\n🎉 ALL LiveTimingEngine TESTS PASSED!\n');
  return true;
}

if (require.main === module) {
  testLiveTimingEngine().catch(err => {
    console.error('Test failed:', err);
    process.exit(1);
  });
}

module.exports = testLiveTimingEngine;

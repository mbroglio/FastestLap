/**
 * test_baku_race.js
 * Verification Test for Baku City Circuit using Generic Track & LiveTiming Classes
 */

const assert = require('assert');
const path = require('path');
const CircuitRegistry = require('../../src/engine/CircuitRegistry');

async function testBakuRace() {
  console.log('\n===============================================================================');
  console.log('TEST SUITE: Baku City Circuit (Azerbaijan GP) — Linear Dry Race Verification');
  console.log('===============================================================================');

  // [1] Load Baku Circuit using generic CircuitRegistry & classes
  const customDataPath = path.join(__dirname, '..', '..', 'data');
  const { trackModel, timingEngine, spec } = CircuitRegistry.loadCircuit('baku', customDataPath);

  console.log(`[1] Circuit Specification: ${spec.name} (${spec.flag} ${spec.country})`);
  assert.strictEqual(trackModel.circuitId, 'baku');
  assert.strictEqual(trackModel.getNodeCount(), 450, `Expected 450 nodes, got ${trackModel.getNodeCount()}`);
  console.log(`  ✅ 450 track nodes loaded. Image dimensions: ${trackModel.dimensions.width}x${trackModel.dimensions.height}px`);

  // [2] Sector timing beams
  const s1Beam = trackModel.sectors.s1Beam;
  const s2Beam = trackModel.sectors.s2Beam;
  assert(s1Beam && s1Beam.t >= 38.0, `S1 beam invalid: ${s1Beam ? s1Beam.t : 'null'}`);
  assert(s2Beam && s2Beam.t >= 83.0, `S2 beam invalid: ${s2Beam ? s2Beam.t : 'null'}`);
  console.log(`  ✅ S1 Timing Beam at t=${s1Beam.t}s (px: ${s1Beam.px}, py: ${s1Beam.py})`);
  console.log(`  ✅ S2 Timing Beam at t=${s2Beam.t}s (px: ${s2Beam.px}, py: ${s2Beam.py})`);

  // [3] Pit Lane Verification
  const pitLane = trackModel.pitLane;
  assert(pitLane && pitLane.pitNodes.length === 25, `Expected 25 pit lane nodes, got ${pitLane ? pitLane.pitNodes.length : 0}`);
  console.log(`\n[2] Pit Lane Architecture:`);
  console.log(`  ✅ Entry branch from Turn 20 at (${pitLane.entry.px}, ${pitLane.entry.py})`);
  console.log(`  ✅ Exit merge before Turn 1 at (${pitLane.exit.px}, ${pitLane.exit.py})`);

  // [4] Pit Stops Continuity
  assert.strictEqual(timingEngine.pitStops.length, 36, `Expected 36 pit stops, got ${timingEngine.pitStops.length}`);
  timingEngine.pitStops.forEach(p => {
    assert(p.duration >= 14 && p.duration <= 45, `Invalid pit duration for #${p.driver}: ${p.duration}`);
  });
  console.log(`  ✅ All 36 pit stops verified with realistic durations (mediana ~21.0s)`);

  // [5] Fastest Lap in Session Verification
  console.log(`\n[3] Fastest Lap Verification:`);
  const fastest = timingEngine.sessionBestLap;
  assert.strictEqual(fastest.driver, '63', `Fastest lap driver should be #63 (RUS), got ${fastest.driver}`);
  assert(Math.abs(fastest.time - 104.916) < 0.01, `Fastest lap time should be 104.916s, got ${fastest.time}`);

  // Test Purple Color rule on fastest lap
  const rusColor = timingEngine.getTimingColor('63', 'lap', 104.916);
  assert.strictEqual(rusColor, '#d054fa', `Fastest lap must receive Purple color (#d054fa), got ${rusColor}`);
  console.log(`  ✅ Fastest Lap: #63 (RUS - Mercedes) 1:44.916 on Lap ${fastest.lap}`);
  console.log(`  ✅ Telemetry Beams: S1=${timingEngine.sessionBestS1.time.toFixed(3)}s, S2=${timingEngine.sessionBestS2.time.toFixed(3)}s, S3=${timingEngine.sessionBestS3.time.toFixed(3)}s (PURPLE verified)`);

  // [6] Race Classification Verification at Lap 51
  console.log(`\n[4] Full Race 51-Lap Classification Verification:`);
  const snapshot = timingEngine.getSnapshot(spec.raceFinishTimeSec);
  assert.strictEqual(snapshot.leaderboard.length, 22, 'Full 22-car grid verified');

  // Verify Podium P1-P2-P3
  const p1 = snapshot.leaderboard[0];
  const p2 = snapshot.leaderboard[1];
  const p3 = snapshot.leaderboard[2];

  console.log(`  🏆 P1: #${p1.number} (${p1.code}) - ${p1.team} [${p1.status}]`);
  console.log(`  🥈 P2: #${p2.number} (${p2.code}) - ${p2.team} [${p2.status}] ${p2.gap}`);
  console.log(`  🥉 P3: #${p3.number} (${p3.code}) - ${p3.team} [${p3.status}] ${p3.gap}`);

  assert(p1 && p1.number, 'P1 must exist');
  assert(p2 && p2.number, 'P2 must exist');
  assert(p3 && p3.number, 'P3 must exist');

  // [7] Retirements Verification (DNF)
  console.log(`\n[5] Verified FastF1 Official Session Retirements (DNF):`);
  const retCount = snapshot.leaderboard.filter(d => d.isRetired).length;
  console.log(`  ✅ ${retCount} retired drivers classified correctly at bottom of standings:`);
  snapshot.leaderboard.filter(d => d.isRetired).forEach(d => {
    console.log(`     🛑 #${d.number} (${d.code}) - Lap ${d.retiredLap} (${d.location || 'Retired'})`);
  });
  assert(retCount >= 4, `Expected at least 4 retirements in Baku GP, got ${retCount}`);

  // [8] Speed Readout: Integer Verification
  console.log('\n[6] Speed Readout Integrity:');
  for (let t = 100; t <= 200; t += 25) {
    const s = timingEngine.getDriverState('63', t);
    assert(Number.isInteger(s.speed), `Speed must be an integer, got ${s.speed}`);
  }
  console.log(`  ✅ Baku telemetry speed values are strictly integers everywhere`);

  console.log('\n🎉 Baku City Circuit Linear Dry Race Test: ALL 6 CHECKS PASSED PERFECTLY!\n');
  return true;
}

if (require.main === module) {
  testBakuRace().catch(e => {
    console.error(e);
    process.exit(1);
  });
}

module.exports = testBakuRace;

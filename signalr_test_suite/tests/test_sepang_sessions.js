const fs = require('fs');
const path = require('path');
const assert = require('assert');

function testSepangSessions() {
  console.log('===============================================================================');
  console.log('TEST SUITE: Sepang GP Simulation — Free Practice & Qualifying Verification');
  console.log('===============================================================================');

  // 1. Load generator datasets
  const { generateSepangFpDataset, generateSepangQualifyingDataset } = require('../simulations/sepang/generate_sepang_sessions.js');

  const fpData = generateSepangFpDataset();
  const qData = generateSepangQualifyingDataset();

  // [1] Free Practice FP2 Verification
  console.log('[1] Testing Sepang Free Practice (FP2) Dataset...');
  assert.strictEqual(fpData.circuitId, 'sepang', 'Circuit ID must be sepang');
  assert.strictEqual(fpData.sessionType, 'practice', 'Session type must be practice');
  assert.strictEqual(fpData.sessionDuration, 3600, 'FP2 duration must be 3600s');
  assert.strictEqual(Object.keys(fpData.drivers).length, 22, 'Must have 22 registered drivers');
  assert.strictEqual(Object.keys(fpData.laps).length, 22, 'All 22 drivers must have recorded laps');

  // Verify FP pit stops / garage stints
  assert.ok(fpData.pitStops.length >= 80, `Expected at least 80 pit/garage entries, found ${fpData.pitStops.length}`);
  const verFpLaps = fpData.laps['3'];
  assert.ok(verFpLaps.length >= 5, 'Verstappen should have at least 5 completed laps');
  const verBestLap = verFpLaps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
  assert.ok(verBestLap < 93.0 && verBestLap > 91.0, `Expected best lap ~92.1s, got ${verBestLap}`);
  console.log(`  ✅ FP2 verified: 22 drivers, ${fpData.pitStops.length} garage stints, best lap #3 VER in ${verBestLap.toFixed(3)}s`);

  // [2] Qualifying (Q1, Q2, Q3) Progression
  console.log('\n[2] Testing Sepang Qualifying (Q1, Q2, Q3) Progression...');
  assert.strictEqual(qData.circuitId, 'sepang', 'Circuit ID must be sepang');
  assert.strictEqual(qData.sessionType, 'qualifying', 'Session type must be qualifying');
  assert.strictEqual(qData.sessionDuration, 3600, 'Qualifying duration must be 3600s');
  assert.strictEqual(Object.keys(qData.drivers).length, 22, 'Must have 22 drivers in qualifying');

  // Verify Q1 laps for all drivers
  Object.keys(qData.laps).forEach(k => {
    const laps = qData.laps[k];
    assert.ok(laps.length >= 2, `Driver ${k} must have completed at least 2 laps in Q1`);
  });

  // Verify elimination hierarchy
  // Top 10 drivers should have 6 laps (2 in Q1, 2 in Q2, 2 in Q3)
  const verQLaps = qData.laps['3'];
  assert.strictEqual(verQLaps.length, 6, 'Max Verstappen (Pole) must have 6 qualifying runs (Q1, Q2, Q3)');
  const verPoleTime = verQLaps[5].dur;
  assert.strictEqual(verPoleTime, 91.215, `Pole position lap must be 91.215 (1:31.215), got ${verPoleTime}`);

  // Q2 eliminated drivers (e.g. #30 Tsunoda) should have 4 laps
  const tsuQLaps = qData.laps['30'];
  assert.strictEqual(tsuQLaps.length, 4, 'Tsunoda (eliminated in Q2) must have 4 laps');

  // Q1 eliminated drivers (e.g. #31 Ocon) should have 2 laps
  const ocoQLaps = qData.laps['31'];
  assert.strictEqual(ocoQLaps.length, 2, 'Ocon (eliminated in Q1) must have 2 laps');
  console.log(`  ✅ Qualifying progression verified:`);
  console.log(`     👑 Pole Position: #3 VER in 1:31.215 (6 runs)`);
  console.log(`     🟡 Q2 Elimination: #30 TSU (4 runs)`);
  console.log(`     🔴 Q1 Elimination: #31 OCO (2 runs)`);

  // [3] Standalone Simulation Suite File Integrity
  console.log('\n[3] Testing Standalone Sepang Folder Files in Assets & Test Suite...');
  const testSuiteSepang = path.join(__dirname, '..', 'simulations', 'sepang');
  const assetSepang = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'live_timing', 'simulations', 'sepang');

  [testSuiteSepang, assetSepang].forEach(dir => {
    const label = dir.includes('assets') ? 'Android Assets' : 'SignalR Test Suite';
    assert.ok(fs.existsSync(dir), `${label}: simulations/sepang/ directory must exist`);
    
    const reqFiles = [
      'track_map_sepang.html',
      'index.html',
      'sepang_simulation_data.js',
      'sepang_timing.css',
      'sepang_timing_app.js',
      'Sepang.svg.webp'
    ];

    reqFiles.forEach(file => {
      const fp = path.join(dir, file);
      assert.ok(fs.existsSync(fp), `${label}: ${file} must exist`);
      const size = fs.statSync(fp).size;
      assert.ok(size > 1000, `${label}: ${file} must be non-empty (got ${size} bytes)`);
    });
    console.log(`  ✅ ${label}: All 6 simulation files present and non-empty`);
  });

  console.log('\n🎉 Sepang GP Free Practice & Qualifying Simulation: ALL VERIFICATIONS PASSED!\n');
  return true;
}

if (require.main === module) {
  testSepangSessions();
}

module.exports = testSepangSessions;

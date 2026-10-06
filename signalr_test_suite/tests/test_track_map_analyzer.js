/**
 * test_track_map_analyzer.js
 * Unit Test Suite for Generic TrackMapAnalyzer
 */

const assert = require('assert');
const TrackMapAnalyzer = require('../src/engine/TrackMapAnalyzer');

async function testTrackMapAnalyzer() {
  console.log('\n===============================================================================');
  console.log('TEST SUITE: Generic TrackMapAnalyzer (Calibration, Sampling & Geometry)');
  console.log('===============================================================================');

  // [1] Test 2D Affine Transform Fitting with Known Synthetic Ground-Truth
  console.log('[1] Testing 2D Affine Similarity Transform Least-Squares...');
  const trueScale = 0.05;
  const trueRotRad = 0.785398; // 45 degrees
  const trueTx = 250.0;
  const trueTy = 180.0;

  const a = trueScale * Math.cos(trueRotRad);
  const b = trueScale * Math.sin(trueRotRad);

  const srcPoints = [
    { x: 0, y: 0 },
    { x: 1000, y: 500 },
    { x: 2500, y: -800 },
    { x: -1200, y: 1500 },
    { x: 300, y: 2200 }
  ];

  const dstPoints = srcPoints.map(p => ({
    px: a * p.x - b * p.y + trueTx,
    py: b * p.x + a * p.y + trueTy
  }));

  const fit = TrackMapAnalyzer.fitAffineTransform(srcPoints, dstPoints);

  assert(Math.abs(fit.scale - trueScale) < 1e-5, `Scale mismatch: expected ${trueScale}, got ${fit.scale}`);
  assert(Math.abs(fit.rotationDeg - 45.0) < 1e-4, `Rotation mismatch: expected 45.0, got ${fit.rotationDeg}`);
  assert(Math.abs(fit.tx - trueTx) < 1e-4, `Tx mismatch: expected ${trueTx}, got ${fit.tx}`);
  assert(Math.abs(fit.ty - trueTy) < 1e-4, `Ty mismatch: expected ${trueTy}, got ${fit.ty}`);
  assert(fit.rmse < 1e-4, `RMSE too high: ${fit.rmse}`);
  console.log(`  ✅ Affine solver accurately recovered scale=${fit.scale.toFixed(4)}, rot=${fit.rotationDeg.toFixed(2)}°, RMSE=${fit.rmse.toExponential(2)}`);

  // [2] Test Centerline Resampling & Curvature Calculation
  console.log('\n[2] Testing Centerline Resampling & Speed Profiling...');
  // Oval track shape
  const rawOval = [];
  for (let deg = 0; deg < 360; deg += 10) {
    const rad = (deg * Math.PI) / 180;
    rawOval.push({
      px: 500 + 300 * Math.cos(rad),
      py: 300 + 150 * Math.sin(rad)
    });
  }

  const nodes = TrackMapAnalyzer.sampleCenterline(rawOval, { targetCount: 100, totalLapDuration: 60.0 });
  assert.strictEqual(nodes.length, 100, 'Should produce exactly 100 nodes');
  assert(nodes[0].t === 0, 'First node t should be 0');
  assert(nodes[nodes.length - 1].t < 60.0, 'Last node t should be under totalLapDuration');

  // Verify curvature and speeds are realistic
  const minSpeed = Math.min(...nodes.map(n => n.speed));
  const maxSpeed = Math.max(...nodes.map(n => n.speed));
  assert(minSpeed >= 75 && maxSpeed <= 330, `Speed bounds check failed: min=${minSpeed}, max=${maxSpeed}`);
  console.log(`  ✅ Sampled 100 nodes: speeds ranged from ${minSpeed} km/h (apex) to ${maxSpeed} km/h (straight)`);

  // [3] Test Sector Beam Configuration
  console.log('\n[3] Testing Sector Timing Beam Detection...');
  const sectorResult = TrackMapAnalyzer.configureSectors(nodes, 20.0, 40.0);
  assert(sectorResult.sfBeam, 'S/F beam must exist');
  assert(sectorResult.s1Beam, 'S1 beam must exist');
  assert(sectorResult.s2Beam, 'S2 beam must exist');
  assert.strictEqual(sectorResult.sfBeam.sector, 1, 'S/F beam should be sector 1');
  assert.strictEqual(sectorResult.s1Beam.sector, 2, 'S1 beam should be sector 2');
  assert.strictEqual(sectorResult.s2Beam.sector, 3, 'S2 beam should be sector 3');
  console.log(`  ✅ Sector beams configured: S1 at t=${sectorResult.s1Beam.t}s, S2 at t=${sectorResult.s2Beam.t}s`);

  // [4] Test Pit Lane Geometry & Hermite Blending
  console.log('\n[4] Testing Pit Lane Setup & Smooth Transition Curve...');
  const pitRaw = [
    { px: 200, py: 300 },
    { px: 350, py: 300 },
    { px: 500, py: 300 }
  ];
  const pitConfig = TrackMapAnalyzer.configurePitLane(pitRaw, nodes);
  assert(pitConfig.entry.distanceToTrack >= 0, 'Pit entry track distance valid');
  assert(pitConfig.exit.distanceToTrack >= 0, 'Pit exit track distance valid');

  // Test C1 smooth blending
  const midTrackPt = { px: 520, py: 310 };
  const blend0 = pitConfig.blendExitPosition(0.0, midTrackPt);
  const blend50 = pitConfig.blendExitPosition(0.5, midTrackPt);
  const blend100 = pitConfig.blendExitPosition(1.0, midTrackPt);

  assert.strictEqual(blend0.px, 500, 'At u=0 blend must be at pit exit node');
  assert.strictEqual(blend100.px, 520, 'At u=1 blend must be at track node');
  assert(blend50.px > 500 && blend50.px < 520, 'At u=0.5 blend must be smoothly intermediate');
  console.log(`  ✅ Pit exit Hermite blend verified: u=0 -> (${blend0.px}, ${blend0.py}), u=0.5 -> (${blend50.px.toFixed(1)}, ${blend50.py.toFixed(1)}), u=1 -> (${blend100.px}, ${blend100.py})`);

  console.log('\n🎉 ALL TrackMapAnalyzer TESTS PASSED!\n');
  return true;
}

if (require.main === module) {
  testTrackMapAnalyzer().catch(err => {
    console.error('Test failed:', err);
    process.exit(1);
  });
}

module.exports = testTrackMapAnalyzer;

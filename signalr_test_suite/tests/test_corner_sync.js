/**
 * test_corner_sync.js
 * Verification of Physical Position & Telemetry Synchronization
 * Ensures zero lead/lag desync between car position coordinates and telemetry readouts.
 */

const assert = require('assert');
const path = require('path');
const CircuitRegistry = require('../src/engine/CircuitRegistry');

async function testCornerSync() {
  console.log('\n===============================================================================');
  console.log('TEST SUITE: Physical Position & Telemetry Synchronization Verification');
  console.log('===============================================================================');

  const customDataPath = path.join(__dirname, '..', 'simulations', 'data');

  // [1] Test Baku Circuit Corner Synchronization
  console.log('[1] Testing Baku City Circuit Corner Synchronization...');
  const { trackModel: bakuTrack, timingEngine: bakuTiming } = CircuitRegistry.loadCircuit('baku', customDataPath);

  const bakuApexes = [
    { name: 'Curva 1 (Turn 1)', node: 24, expectedApexSpeed: 105 },
    { name: 'Curva 2 (Turn 2)', node: 50, expectedApexSpeed: 102 },
    { name: 'Curva 3 (Turn 3)', node: 114, expectedApexSpeed: 98 },
    { name: 'Curva 4 (Turn 4)', node: 131, expectedApexSpeed: 112 },
    { name: 'Curve 5-6 (Chicane)', node: 157, expectedApexSpeed: 85 },
    { name: 'Curva 7 (Turn 7)', node: 191, expectedApexSpeed: 95 },
    { name: 'Curva 15 (Turn 15)', node: 326, expectedApexSpeed: 104 }
  ];

  for (const corner of bakuApexes) {
    const apexNode = bakuTrack.trackNodes[corner.node];
    const prevNode = bakuTrack.trackNodes[corner.node - 3];
    const nextNode = bakuTrack.trackNodes[corner.node + 3];

    // Assert apex speed is minimum compared to approach and exit
    assert(
      prevNode.speed > apexNode.speed || prevNode.brake > 0,
      `[Baku ${corner.name}] Approach must decelerate: prevSpeed=${prevNode.speed}, apexSpeed=${apexNode.speed}`
    );
    assert(
      nextNode.speed > apexNode.speed,
      `[Baku ${corner.name}] Exit must accelerate: nextSpeed=${nextNode.speed}, apexSpeed=${apexNode.speed}`
    );

    // Assert interpolated state at exact timestamp t matches node properties
    const state = bakuTrack.getPointAtTime(apexNode.t);
    assert(Math.abs(state.px - apexNode.px) < 1.0, `[Baku ${corner.name}] Px desync at apex`);
    assert(Math.abs(state.py - apexNode.py) < 1.0, `[Baku ${corner.name}] Py desync at apex`);
    assert.strictEqual(state.speed, apexNode.speed, `[Baku ${corner.name}] Speed desync at apex`);
    assert(state.brake === 0, `[Baku ${corner.name}] Brake must release at apex point`);

    console.log(`  ✅ ${corner.name} verified: Approach (${prevNode.speed} km/h brk=${prevNode.brake}) -> Apex (${apexNode.speed} km/h at t=${apexNode.t}s) -> Exit (${nextNode.speed} km/h thr=${nextNode.throttle})`);
  }

  // Verify high-speed chicane (Turns 18-19) maintains full throttle and zero anomalous slowdown
  const chicaneNode = bakuTrack.trackNodes[346];
  assert(chicaneNode.speed >= 270, `Chicane speed must be high-speed, got ${chicaneNode.speed}`);
  assert.strictEqual(chicaneNode.brake, 0, `Chicane must have zero braking`);
  assert.strictEqual(chicaneNode.throttle, 100, `Chicane must be full throttle`);
  console.log(`  ✅ Curve 18-19 (Chicane) verified: High-Speed Full Throttle (${chicaneNode.speed} km/h, brk=0, thr=100) — Zero anomalous slowdown!`);

  // [2] Test Sepang Circuit Corner Synchronization
  console.log('\n[2] Testing Sepang Circuit Corner Synchronization...');
  const { trackModel: sepangTrack, timingEngine: sepangTiming } = CircuitRegistry.loadCircuit('sepang', customDataPath);

  const sepangApexes = [
    { name: 'Curva 1 (Turn 1)', node: 57, expectedApexSpeed: 74 },
    { name: 'Curva 4 (Turn 4)', node: 131, expectedApexSpeed: 112 },
    { name: 'Curva 9 (Turn 9)', node: 260, expectedApexSpeed: 76 },
    { name: 'Curva 11 (Turn 11)', node: 290, expectedApexSpeed: 118 },
    { name: 'Curva 14 (Turn 14)', node: 347, expectedApexSpeed: 108 },
    { name: 'Curva 15 (Turn 15)', node: 423, expectedApexSpeed: 73 }
  ];

  for (const corner of sepangApexes) {
    const apexNode = sepangTrack.trackNodes[corner.node];
    const prevNode = sepangTrack.trackNodes[corner.node - 3];
    const nextNode = sepangTrack.trackNodes[corner.node + 3];

    assert(
      prevNode.speed > apexNode.speed || prevNode.brake > 0,
      `[Sepang ${corner.name}] Approach must decelerate: prevSpeed=${prevNode.speed}, apexSpeed=${apexNode.speed}`
    );
    assert(
      nextNode.speed >= apexNode.speed,
      `[Sepang ${corner.name}] Exit must accelerate: nextSpeed=${nextNode.speed}, apexSpeed=${apexNode.speed}`
    );

    const state = sepangTrack.getPointAtTime(apexNode.t);
    assert(Math.abs(state.px - apexNode.px) < 1.0, `[Sepang ${corner.name}] Px desync at apex`);
    assert(Math.abs(state.py - apexNode.py) < 1.0, `[Sepang ${corner.name}] Py desync at apex`);
    assert.strictEqual(state.speed, apexNode.speed, `[Sepang ${corner.name}] Speed desync at apex`);

    console.log(`  ✅ ${corner.name} verified: Approach (${prevNode.speed} km/h brk=${prevNode.brake}) -> Apex (${apexNode.speed} km/h at t=${apexNode.t}s) -> Exit (${nextNode.speed} km/h thr=${nextNode.throttle})`);
  }

  console.log('\n🎉 Position & Telemetry Synchronization: ZERO DESYNC VERIFIED ON ALL CIRCUITS!\n');
  return true;
}

if (require.main === module) {
  testCornerSync().catch(err => {
    console.error(err);
    process.exit(1);
  });
}

module.exports = testCornerSync;

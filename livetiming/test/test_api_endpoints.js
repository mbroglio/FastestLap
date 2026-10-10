/**
 * Unit Test: REST API Endpoints & Contract Validation
 */

const assert = require('assert');
const { StateStore } = require('../src/backend/state_store');
const { MockStreamProvider } = require('../src/backend/mock_stream_provider');

console.log('--- [TEST 3] REST API Endpoints Contract Validation ---');

const store = new StateStore();
const mock = new MockStreamProvider(store);

// Seed simulation data
mock.start();

// 1. Check Status Endpoint
const status = store.getStatus();
assert.strictEqual(status.service, 'fastestlap-livetiming');
assert(status.driverCount >= 10, 'Driver count must be populated');
console.log('✅ /api/status structure verified');

// 2. Check Standings / Leaderboard Endpoint
const leaderboard = store.getLeaderboard();
assert(Array.isArray(leaderboard), 'Leaderboard must be an array');
assert(leaderboard.length >= 10, 'Leaderboard must contain all drivers');

const p1 = leaderboard[0];
assert(p1.racingNumber !== undefined, 'racingNumber present');
assert(p1.tla !== undefined, 'tla present');
assert(p1.teamName !== undefined, 'teamName present');
assert(p1.teamColour !== undefined, 'teamColour present');
assert(p1.gapToLeader !== undefined, 'gapToLeader present');
assert(p1.sectors !== undefined, 'sectors present');
assert(p1.tyreCompound !== undefined, 'tyreCompound present');
console.log('✅ /api/standings structure verified (F1 PascalCase mapping valid)');

// 3. Check Tyres / Stints Endpoint
const tyres = store.getTyresSummary();
assert(typeof tyres === 'object', 'Tyres summary is an object map');
assert(tyres['1'] && tyres['1'].stints.length > 0, 'Tyre stint present for driver 1');
console.log('✅ /api/tyres structure verified');

// 4. Check Telemetry & Cockpit Fields
const car1 = store.carData['1'];
assert(car1 !== undefined, 'Car telemetry present');
assert(car1.speed !== undefined, 'speed present');
assert(car1.rpm !== undefined, 'rpm present');
assert(car1.gear !== undefined, 'gear present');
assert(car1.throttle !== undefined, 'throttle present');
assert(car1.brake !== undefined, 'brake present');
assert(car1.drsState !== undefined, 'drsState present');
assert(car1.battery !== undefined, 'battery SoC present');
console.log('✅ /api/telemetry cockpit gauges data verified');

// 5. Check 2D Track Positions
const pos1 = store.positions['1'];
assert(pos1 !== undefined, 'Driver 1 position present');
assert(typeof pos1.x === 'number' && typeof pos1.y === 'number', 'Numeric track coordinates present');
console.log('✅ /api/positions track coordinates verified');

mock.stop();

console.log('🎉 REST API Endpoints contract validated successfully!\n');

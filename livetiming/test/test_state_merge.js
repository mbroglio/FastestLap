/**
 * Unit Test: StateStore & Deep Delta Merging
 */

const assert = require('assert');
const { StateStore, deepMerge } = require('../src/backend/state_store');

console.log('--- [TEST 1] StateStore & Deep Delta Merge ---');

// 1. Test deepMerge object preservation
const base = {
  Lines: {
    '1': {
      Position: '1',
      GapToLeader: { Value: 'LEADER' },
      Sectors: { '0': { Value: '28.1' } }
    }
  }
};

const delta = {
  Lines: {
    '1': {
      Sectors: { '1': { Value: '32.4' } }
    }
  }
};

deepMerge(base, delta);

assert.strictEqual(base.Lines['1'].Position, '1', 'Position must be retained');
assert.strictEqual(base.Lines['1'].Sectors['0'].Value, '28.1', 'Sector 0 must be retained');
assert.strictEqual(base.Lines['1'].Sectors['1'].Value, '32.4', 'Sector 1 must be added');
console.log('✅ Deep merge retains sibling fields and updates nested deltas');

// 2. Test Leaderboard Sorting and Retired Drivers handling
const store = new StateStore();

store.updateDrivers({
  '1': { RacingNumber: '1', Tla: 'VER', FullName: 'Max Verstappen', TeamColour: '#3671C6' },
  '16': { RacingNumber: '16', Tla: 'LEC', FullName: 'Charles Leclerc', TeamColour: '#E8002D' },
  '44': { RacingNumber: '44', Tla: 'HAM', FullName: 'Lewis Hamilton', TeamColour: '#27F4D2' }
});

store.updateTimingData({
  Lines: {
    '1': { Position: '2', GapToLeader: { Value: '+1.500' } },
    '16': { Position: '1', GapToLeader: { Value: 'LEADER' } },
    '44': { Position: '3', Retired: true }
  }
});

const leaderboard = store.getLeaderboard();

assert.strictEqual(leaderboard.length, 3, 'All 3 drivers present in leaderboard');
assert.strictEqual(leaderboard[0].racingNumber, '16', 'P1 must be Leclerc');
assert.strictEqual(leaderboard[1].racingNumber, '1', 'P2 must be Verstappen');
assert.strictEqual(leaderboard[2].racingNumber, '44', 'Retired driver must be at the bottom');
assert.strictEqual(leaderboard[2].status, 'RET', 'Hamilton status is RET');
console.log('✅ Leaderboard sorting and classification logic verified');

console.log('🎉 StateStore tests passed successfully!\n');

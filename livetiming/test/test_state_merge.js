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
assert.strictEqual(leaderboard[1].gapToLeader, '+1.500', 'P2 gapToLeader must be +1.500');
assert.strictEqual(leaderboard[1].intervalToAhead, '+1.500', 'P2 intervalToAhead dynamically calculated from P1');
assert.strictEqual(leaderboard[2].racingNumber, '44', 'Retired driver must be at the bottom');
assert.strictEqual(leaderboard[2].status, 'RET', 'Hamilton status is RET');
console.log('✅ Leaderboard sorting and classification logic verified');

// 3. Test Tyre Stints extraction with real F1 SignalR Object structure
store.updateTimingAppData({
  Lines: {
    '16': {
      Stints: {
        '0': { LapFlags: 0 },
        '1': {
          LapFlags: 1,
          Compound: 'SOFT',
          New: 'true',
          TotalLaps: 4
        }
      }
    },
    '1': {
      Stints: [
        { Compound: 'MEDIUM', TotalLaps: 12, New: false }
      ]
    }
  }
});

const lbTyres = store.getLeaderboard();
const lec = lbTyres.find(d => d.racingNumber === '16');
const ver = lbTyres.find(d => d.racingNumber === '1');

assert.strictEqual(lec.tyreCompound, 'SOFT', 'Driver 16 must have SOFT compound from object stints');
assert.strictEqual(lec.tyreAge, 4, 'Driver 16 tyre age must be 4');
assert.strictEqual(lec.tyreNew, true, 'Driver 16 tyreNew must be boolean true');
assert.strictEqual(ver.tyreCompound, 'MEDIUM', 'Driver 1 must have MEDIUM compound');
assert.strictEqual(ver.tyreAge, 12, 'Driver 1 tyre age must be 12');
assert.strictEqual(ver.tyreNew, false, 'Driver 1 tyreNew must be boolean false');
console.log('✅ Object-based and Array-based tyre stints extracted with exact compound & age');

// 4. Test Dynamic GAP (to P1) and INT (to car ahead) from lap times when stream omits gaps
const storeDynamic = new StateStore();
storeDynamic.updateDrivers({
  '3': { RacingNumber: '3', Tla: 'VER' },
  '1': { RacingNumber: '1', Tla: 'NOR' },
  '12': { RacingNumber: '12', Tla: 'ANT' }
});
storeDynamic.updateTimingData({
  Lines: {
    '3': { Position: '1', BestLapTime: { Value: '1:34.471' } },
    '1': { Position: '2', BestLapTime: { Value: '1:34.539' } },
    '12': { Position: '3', BestLapTime: { Value: '1:34.756' } }
  }
});

const lbDynamic = storeDynamic.getLeaderboard();
assert.strictEqual(lbDynamic[0].gapToLeader, 'LEADER', 'P1 GAP must be LEADER');
assert.strictEqual(lbDynamic[0].intervalToAhead, '--', 'P1 INT must be --');
assert.strictEqual(lbDynamic[1].gapToLeader, '+0.068', 'P2 GAP dynamically calculated vs P1');
assert.strictEqual(lbDynamic[1].intervalToAhead, '+0.068', 'P2 INT dynamically calculated vs P1');
assert.strictEqual(lbDynamic[2].gapToLeader, '+0.285', 'P3 GAP dynamically calculated vs P1 (1:34.756 - 1:34.471)');
assert.strictEqual(lbDynamic[2].intervalToAhead, '+0.217', 'P3 INT dynamically calculated vs P2 (1:34.756 - 1:34.539)');
console.log('✅ Dynamic on-the-fly GAP (to P1) and INT (to car ahead) calculation verified');

// 5. Test Qualifying SessionPart reset (Q1 -> Q2 -> Q3) and end-of-session reset
const storeQuali = new StateStore();
storeQuali.updateSessionInfo({ Type: 'Qualifying', Name: 'Qualifying' });
storeQuali.updateDrivers({
  '12': { RacingNumber: '12', Tla: 'ANT' },
  '30': { RacingNumber: '30', Tla: 'LAW' },
  '10': { RacingNumber: '10', Tla: 'GAS' }
});
storeQuali.updateTimingData({
  SessionPart: 3,
  Lines: {
    '12': {
      Position: '1',
      KnockedOut: false,
      BestLapTime: { Value: '1:34.035' },
      BestLapTimes: [{ Value: '1:35.390' }, { Value: '1:34.278' }, { Value: '1:34.035' }],
      Stats: [{}, {}, { TimeDiffToFastest: '', TimeDifftoPositionAhead: '' }]
    },
    '30': {
      Position: '10',
      KnockedOut: false,
      BestLapTime: { Value: '' },
      BestLapTimes: [{ Value: '1:35.662' }, { Value: '1:35.058' }, { Value: '' }],
      Stats: [{}, { TimeDiffToFastest: '+0.860', TimeDifftoPositionAhead: '+0.108' }, { TimeDiffToFastest: '', TimeDifftoPositionAhead: '' }]
    },
    '10': {
      Position: '11',
      KnockedOut: true,
      BestLapTime: { Value: '1:35.080' },
      BestLapTimes: [{ Value: '1:35.969' }, { Value: '1:35.080' }, {}],
      Stats: [{}, { TimeDiffToFastest: '+0.882', TimeDifftoPositionAhead: '+0.022' }, {}]
    }
  }
});

const lbQ3Active = storeQuali.getLeaderboard();
assert.strictEqual(lbQ3Active[0].bestLapTime, '1:34.035', 'Q3 P1 has Q3 time');
assert.strictEqual(lbQ3Active[0].gapToLeader, 'LEADER', 'Q3 P1 is LEADER');
assert.strictEqual(lbQ3Active[1].bestLapTime, '--', 'Driver in Q3 without Q3 lap must not inherit Q2 BEST');
assert.strictEqual(lbQ3Active[1].gapToLeader, '--', 'Driver in Q3 without Q3 lap must have GAP reset to --');
assert.strictEqual(lbQ3Active[1].intervalToAhead, '--', 'Driver in Q3 without Q3 lap must have INT reset to --');
assert.strictEqual(lbQ3Active[2].bestLapTime, '--', 'Knocked-out driver in Q3 must have BEST reset to --');
assert.strictEqual(lbQ3Active[2].gapToLeader, '--', 'Knocked-out driver in Q3 must have GAP reset to --');
assert.strictEqual(lbQ3Active[2].intervalToAhead, '--', 'Knocked-out driver in Q3 must have INT reset to --');

// End of Q3 session -> all BEST, GAP, INT must be reset
storeQuali.updateSessionStatus({ Status: 'Finalised' });
const lbQ3Ended = storeQuali.getLeaderboard();
assert.strictEqual(lbQ3Ended[0].bestLapTime, '--', 'At end of Q3, P1 BEST must be reset to --');
assert.strictEqual(lbQ3Ended[0].gapToLeader, '--', 'At end of Q3, P1 GAP must be reset to --');
assert.strictEqual(lbQ3Ended[0].intervalToAhead, '--', 'At end of Q3, P1 INT must be reset to --');
console.log('✅ Qualifying Q1/Q2/Q3 session isolation and end-of-session reset (BEST, GAP, INT) verified');

// 6. Test BEST and LAST overall fastest lap flags
const storeFastest = new StateStore();
storeFastest.updateDrivers({
  '16': { RacingNumber: '16', Tla: 'LEC', FullName: 'Charles Leclerc', TeamColour: '#E8002D' },
  '1': { RacingNumber: '1', Tla: 'VER', FullName: 'Max Verstappen', TeamColour: '#3671C6' },
  '44': { RacingNumber: '44', Tla: 'HAM', FullName: 'Lewis Hamilton', TeamColour: '#27F4D2' }
});
storeFastest.updateTimingData({
  Lines: {
    '16': { Position: '1', BestLapTime: { Value: '1:34.500', OverallFastest: true }, LastLapTime: { Value: '1:35.200' } },
    '1': { Position: '2', BestLapTime: { Value: '1:34.800' }, LastLapTime: { Value: '1:34.500', OverallFastest: true } },
    '44': { Position: '3', BestLapTime: { Value: '1:35.100' }, LastLapTime: { Value: '1:35.800' } }
  }
});
const lbFastest = storeFastest.getLeaderboard();
assert.strictEqual(lbFastest[0].bestLapOverallFastest, true, 'LEC best lap must be marked overall fastest');
assert.strictEqual(lbFastest[1].lastLapOverallFastest, true, 'VER last lap must be marked overall fastest');
console.log('✅ BEST and LAST overall fastest lap flags verified');

// 7. Test Session Terminated Detection
const storeTerminated = new StateStore();
assert.strictEqual(storeTerminated.isSessionEnded(), false, 'Initial state must not be ended');
storeTerminated.updateSessionStatus({ Status: 'Finalised' });
assert.strictEqual(storeTerminated.isSessionEnded(), true, 'Status Finalised must mark session ended');
assert.ok(storeTerminated.sessionEndedAt, 'sessionEndedAt must be populated');
const statusSnapshot = storeTerminated.getStatus();
assert.strictEqual(statusSnapshot.isSessionEnded, true, 'getStatus must expose isSessionEnded');
console.log('✅ Session termination detection and timestamping verified');

console.log('🎉 StateStore tests passed successfully!\n');

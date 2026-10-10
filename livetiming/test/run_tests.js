/**
 * FastestLap LiveTiming - Test Runner
 */

const { spawnSync } = require('child_process');
const path = require('path');

const tests = [
  'test_state_merge.js',
  'test_telemetry_decode.js',
  'test_api_endpoints.js',
  'test_signalr_connection.js'
];

console.log('================================================================');
console.log(' FastestLap Live Timing Suite — Running Verification Tests');
console.log('================================================================\n');

let allPassed = true;

for (const testFile of tests) {
  const filePath = path.join(__dirname, testFile);
  const result = spawnSync('node', [filePath], { stdio: 'inherit' });
  if (result.status !== 0) {
    allPassed = false;
    console.error(`❌ Test failed: ${testFile}`);
    break;
  }
}

if (allPassed) {
  console.log('================================================================');
  console.log('🎉 ALL TESTS PASSED SUCCESSFULLY!');
  console.log('================================================================');
  process.exit(0);
} else {
  console.error('\n❌ SOME TESTS FAILED.');
  process.exit(1);
}

#!/usr/bin/env node
/**
 * Master Test Suite Runner
 * Executes all Generic Engine, Circuit Model, and Telemetry tests sequentially.
 */

const {
  testTrackMapAnalyzer,
  testLiveTimingEngine,
  testBakuRace,
  testSepangRace,
  testTelemetryDecoder,
  testCornerSync,
  testSepangSessions,
  testAdaptiveUiAndSectors
} = require('./tests');

// ANSI Colors
const C = {
  reset: '\x1b[0m',
  bold: '\x1b[1m',
  green: '\x1b[32m',
  red: '\x1b[31m',
  yellow: '\x1b[33m',
  cyan: '\x1b[36m',
  white: '\x1b[37m'
};

async function runAllTests() {
  const startTime = Date.now();

  console.log(`${C.bold}${C.cyan}╔═══════════════════════════════════════════════════════════════════════════╗${C.reset}`);
  console.log(`${C.bold}${C.cyan}║${C.reset}  ${C.bold}${C.yellow}FASTESTLAP — GENERIC LIVE TIMING & CIRCUIT ENGINE TEST SUITE${C.reset}             ${C.bold}${C.cyan}║${C.reset}`);
  console.log(`${C.bold}${C.cyan}║${C.reset}  ${C.white}Testing Track Analyzer, LiveTimingEngine, Baku GP & Sepang GP tests${C.reset}     ${C.bold}${C.cyan}║${C.reset}`);
  console.log(`${C.bold}${C.cyan}╚═══════════════════════════════════════════════════════════════════════════╝${C.reset}`);

  const testResults = [];

  async function executeTest(name, testFn) {
    const tStart = Date.now();
    try {
      const passed = await testFn();
      testResults.push({ name, passed: Boolean(passed), durationMs: Date.now() - tStart });
    } catch (err) {
      console.error(`Unexpected test exception in "${name}":`, err.message);
      testResults.push({ name, passed: false, durationMs: Date.now() - tStart, error: err.message });
    }
  }

  // Execute test suites
  await executeTest('1. Generic TrackMapAnalyzer (Affine Fit & Geometry Sampling)', testTrackMapAnalyzer);
  await executeTest('2. Generic LiveTimingEngine (Timing Rules, Colors & Standings)', testLiveTimingEngine);
  await executeTest('3. Baku GP Verification (Linear Dry Race & FastF1 Session Calibration)', testBakuRace);
  await executeTest('4. Sepang GP Verification (Wet Race, 2 Formation Laps & Pit Rejoin)', testSepangRace);
  await executeTest('5. CarData.z DEFLATE Decoder & Telemetry Flow', testTelemetryDecoder);
  await executeTest('6. Physical Position & Telemetry Sync (Zero-Desync Calibration)', testCornerSync);
  await executeTest('7. Sepang GP Sessions (Free Practice & Qualifying Simulation)', testSepangSessions);
  await executeTest('8. Adaptive Session UI, Sector Colors & Cockpit Toggle', testAdaptiveUiAndSectors);

  const totalDuration = ((Date.now() - startTime) / 1000).toFixed(2);
  const totalPassed = testResults.filter(t => t.passed).length;
  const totalFailed = testResults.length - totalPassed;

  console.log(`\n${C.bold}${C.cyan}===========================================================================`);
  console.log(`🏁  MASTER TEST SUITE EXECUTION SUMMARY`);
  console.log(`===========================================================================${C.reset}`);

  testResults.forEach((t) => {
    const badge = t.passed ? `${C.green}✅ PASS${C.reset}` : `${C.red}❌ FAIL${C.reset}`;
    console.log(`  ${badge}  [${t.durationMs}ms]  ${C.bold}${t.name}${C.reset}`);
  });

  console.log(`───────────────────────────────────────────────────────────────────────────`);
  console.log(`Total Suites: ${testResults.length}  |  Passed: ${C.green}${totalPassed}${C.reset}  |  Failed: ${totalFailed > 0 ? C.red : C.green}${totalFailed}${C.reset}  |  Time: ${totalDuration}s`);

  if (totalFailed === 0) {
    console.log(`\n${C.bold}${C.green}🎉 ALL TESTS PASSED SUCCESSFULLY!${C.reset}\n`);
    process.exitCode = 0;
  } else {
    console.log(`\n${C.bold}${C.red}⚠️  SOME TESTS FAILED.${C.reset}\n`);
    process.exitCode = 1;
  }
}

if (require.main === module) {
  runAllTests();
}

module.exports = runAllTests;

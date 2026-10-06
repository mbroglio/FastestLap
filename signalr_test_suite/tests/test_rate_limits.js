/**
 * Test Suite: Rate Limits & High-Frequency Call Benchmark
 * Tests the F1 SignalR endpoint against rapid consecutive calls and concurrent bursts
 * to verify the presence or absence of rate limiting (HTTP 429 / IP throttling).
 */

const F1SignalRClient = require('../src/signalr_client');
const { SIGNALR_NEGOTIATE_URL, DEFAULT_HEADERS } = require('../src/config');

async function testRateLimits(options = {}) {
  const sequentialCount = options.sequentialCount || 20;
  const concurrentCount = options.concurrentCount || 15;

  console.log('\n--- [TEST 3] SignalR Rate Limiting & High-Frequency Call Stress Test ---');
  console.log(`Target: ${SIGNALR_NEGOTIATE_URL}`);
  console.log(`Strategy:`);
  console.log(`  Phase 1: Sequential rapid calls (${sequentialCount} requests with 0ms delay)`);
  console.log(`  Phase 2: Concurrent burst (${concurrentCount} simultaneous parallel requests)`);
  console.log(`  Phase 3: Consecutive WebSocket connect/disconnect cycles (3 cycles)\n`);

  const results = {
    totalRequests: 0,
    successfulRequests: 0,
    rateLimitedCount: 0, // HTTP 429
    errorCount: 0,
    latencies: [],
    statusCodes: {}
  };

  // Helper single request
  async function makeRequest(id, phaseName) {
    const start = Date.now();
    try {
      const res = await fetch(SIGNALR_NEGOTIATE_URL, {
        method: 'POST',
        headers: DEFAULT_HEADERS
      });
      const latency = Date.now() - start;
      const status = res.status;

      results.totalRequests++;
      results.statusCodes[status] = (results.statusCodes[status] || 0) + 1;
      results.latencies.push(latency);

      if (status === 200) {
        results.successfulRequests++;
      } else if (status === 429) {
        results.rateLimitedCount++;
      } else {
        results.errorCount++;
      }

      return { id, phase: phaseName, status, latency };
    } catch (err) {
      results.totalRequests++;
      results.errorCount++;
      return { id, phase: phaseName, error: err.message, latency: Date.now() - start };
    }
  }

  // Phase 1: Sequential rapid requests
  console.log(`⚡ Phase 1: Executing ${sequentialCount} rapid sequential negotiate calls...`);
  process.stdout.write('   Progress: ');
  for (let i = 1; i <= sequentialCount; i++) {
    const outcome = await makeRequest(i, 'Sequential');
    if (outcome.status === 200) {
      process.stdout.write('.');
    } else if (outcome.status === 429) {
      process.stdout.write('R'); // Rate limited
    } else {
      process.stdout.write('X'); // Other error
    }
  }
  process.stdout.write(' Done!\n');

  // Phase 2: Concurrent burst
  console.log(`\n💥 Phase 2: Executing ${concurrentCount} concurrent parallel negotiate requests...`);
  const burstPromises = [];
  for (let i = 1; i <= concurrentCount; i++) {
    burstPromises.push(makeRequest(i, 'Concurrent'));
  }
  const burstOutcomes = await Promise.all(burstPromises);
  const burstSuccess = burstOutcomes.filter(o => o.status === 200).length;
  console.log(`   Burst finished: ${burstSuccess}/${concurrentCount} requests returned 200 OK.`);

  // Phase 3: Consecutive WebSocket sessions
  console.log(`\n🔌 Phase 3: Testing 3 rapid WebSocket connect & disconnect cycles...`);
  let wsCyclesSuccess = 0;
  for (let w = 1; w <= 3; w++) {
    const wsClient = new F1SignalRClient();
    try {
      const connStart = Date.now();
      await wsClient.connect();
      const elapsed = Date.now() - connStart;
      console.log(`   Cycle ${w}/3: Connected & Handshake OK in ${elapsed}ms`);
      wsClient.disconnect();
      wsCyclesSuccess++;
    } catch (wsErr) {
      console.error(`   Cycle ${w}/3 failed: ${wsErr.message}`);
      wsClient.disconnect();
    }
  }

  // Statistical calculations
  results.latencies.sort((a, b) => a - b);
  const minLatency = results.latencies[0] || 0;
  const maxLatency = results.latencies[results.latencies.length - 1] || 0;
  const avgLatency = Math.round(results.latencies.reduce((a, b) => a + b, 0) / (results.latencies.length || 1));
  const p95Index = Math.floor(results.latencies.length * 0.95);
  const p95Latency = results.latencies[p95Index] || 0;
  const successRate = ((results.successfulRequests / results.totalRequests) * 100).toFixed(1);

  console.log(`\n📊 ═══════════════════════════════════════════════════════════`);
  console.log(`   RATE LIMIT & BENCHMARK TEST RESULTS SUMMARY`);
  console.log(`═══════════════════════════════════════════════════════════════`);
  console.log(`Total HTTP Requests Executed:    ${results.totalRequests}`);
  console.log(`Successful (200 OK):             ${results.successfulRequests} (${successRate}%)`);
  console.log(`Rate Limited (HTTP 429):         ${results.rateLimitedCount}`);
  console.log(`Errors / Drops:                  ${results.errorCount}`);
  console.log(`WebSocket Handshake Cycles:      ${wsCyclesSuccess}/3 passed`);
  console.log(`Status Codes:                    ${JSON.stringify(results.statusCodes)}`);
  console.log(`Latency - Min:                   ${minLatency}ms`);
  console.log(`Latency - Avg:                   ${avgLatency}ms`);
  console.log(`Latency - P95:                   ${p95Latency}ms`);
  console.log(`Latency - Max:                   ${maxLatency}ms`);
  console.log(`───────────────────────────────────────────────────────────────`);

  const noRateLimitsDetected = results.rateLimitedCount === 0 && results.successfulRequests === results.totalRequests;

  if (noRateLimitsDetected && wsCyclesSuccess === 3) {
    console.log(`🏆 VERDICT: NO CALL LIMITS / NO RATE-LIMITING DETECTED!`);
    console.log(`   The endpoint handled ${results.totalRequests} rapid & concurrent requests`);
    console.log(`   with 100% success rate without triggering HTTP 429 or connection drops.`);
    console.log(`🎉 PASSED: F1 SignalR infrastructure shows robust throughput.\n`);
    return true;
  } else {
    console.log(`⚠️ VERDICT: Rate limits or errors observed.`);
    console.log(`   Rate limits triggered: ${results.rateLimitedCount}, Other errors: ${results.errorCount}\n`);
    return false;
  }
}

if (require.main === module) {
  testRateLimits().then(passed => {
    process.exitCode = passed ? 0 : 1;
  });
}

module.exports = testRateLimits;

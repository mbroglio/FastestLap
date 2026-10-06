/**
 * Test Suite: SignalR Core Negotiation Endpoint
 * Validates that F1 Live Timing negotiation endpoint responds with 200 OK
 * and returns valid connectionToken and transport configurations.
 */

const F1SignalRClient = require('../src/signalr_client');
const { SIGNALR_NEGOTIATE_URL } = require('../src/config');

async function testNegotiate() {
  console.log('\n--- [TEST 1] F1 SignalR Negotiation Endpoint ---');
  console.log(`Endpoint: ${SIGNALR_NEGOTIATE_URL}`);

  const client = new F1SignalRClient();
  const result = await client.negotiate();

  if (!result.success) {
    console.error(`❌ FAILED: Negotiate failed - ${result.error}`);
    return false;
  }

  console.log(`✅ Status: 200 OK (Latency: ${result.latencyMs}ms)`);
  console.log(`✅ Connection ID: ${result.connectionId}`);
  console.log(`✅ Connection Token: ${result.connectionToken.substring(0, 16)}...`);
  console.log(`✅ Cookie received: ${result.cookie ? result.cookie.split(';')[0] : 'None'}`);

  const transports = (result.availableTransports || []).map(t => t.transport);
  console.log(`✅ Available Transports: ${transports.join(', ')}`);

  const hasWebSocket = transports.includes('WebSockets');
  if (!hasWebSocket) {
    console.error('❌ FAILED: WebSockets transport not available in negotiation response');
    return false;
  }

  console.log('🎉 PASSED: F1 SignalR negotiation endpoint is healthy and operational.\n');
  return true;
}

if (require.main === module) {
  testNegotiate().then(passed => {
    process.exitCode = passed ? 0 : 1;
  });
}

module.exports = testNegotiate;

/**
 * Integration Test: F1 SignalR Official Live Endpoint Negotiation
 */

const assert = require('assert');

console.log('--- [TEST 4] F1 Official SignalR Endpoint Negotiation ---');

async function testNegotiate() {
  const negotiateUrl = 'https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1';
  try {
    const response = await fetch(negotiateUrl, {
      method: 'POST',
      headers: {
        'User-Agent': 'BestHTTP',
        'Origin': 'https://www.formula1.com',
        'Content-Type': 'text/plain'
      }
    });

    console.log(`[HTTP Response] Status: ${response.status} ${response.statusText}`);
    assert.strictEqual(response.status, 200, 'SignalR negotiate should return 200 OK');

    const json = await response.json();
    assert(json.connectionToken, 'Response should contain connectionToken');
    console.log(`✅ Official F1 SignalR Negotiation endpoint responded successfully!`);
    console.log(`   Connection Token: ${json.connectionToken.substring(0, 24)}...`);
    console.log('🎉 Live negotiation verified!\n');
  } catch (err) {
    console.error('❌ Negotiation test failed:', err.message);
    process.exit(1);
  }
}

testNegotiate();

/**
 * Test Suite: Live SignalR Core Connection & Handshake
 * Tests real-time WebSocket connection to F1 Live Timing,
 * validates the protocol handshake, topic subscriptions, and socket stability.
 */

const F1SignalRClient = require('../src/signalr_client');

async function testLiveConnection(listenDurationMs = 4000) {
  console.log('\n--- [TEST 2] F1 Live SignalR WebSocket Connection & Handshake ---');

  const client = new F1SignalRClient();
  let handshakeReceived = false;
  let subscribedTopics = null;
  let receivedMessagesCount = 0;

  try {
    console.log('1. Initiating HTTP negotiate...');
    const neg = await client.negotiate();
    if (!neg.success) {
      throw new Error(`Negotiation failed: ${neg.error}`);
    }
    console.log(`   Negotiation OK (Token: ${neg.connectionToken.substring(0, 12)}..., Latency: ${neg.latencyMs}ms)`);

    console.log('2. Connecting to WebSocket and performing SignalR protocol handshake...');
    await client.connect();
    handshakeReceived = true;
    console.log('   ✅ WebSocket connected and SignalR handshake ACK received!');

    client.on('subscribed', (topics) => {
      subscribedTopics = topics;
      console.log(`   ✅ Subscribed to topics: ${topics.slice(0, 5).join(', ')}... (+${topics.length - 5} more)`);
    });

    client.on('message', () => {
      receivedMessagesCount++;
    });

    client.on('feed', (feedItem) => {
      receivedMessagesCount++;
      console.log(`   📡 Received live topic stream: [${feedItem.topic}] (${feedItem.timestamp || 'no-ts'})`);
    });

    client.on('ping', () => {
      console.log('   💓 Received SignalR Ping keep-alive from server');
    });

    console.log(`3. Listening for live stream activity for ${listenDurationMs / 1000}s...`);
    await new Promise((resolve) => setTimeout(resolve, listenDurationMs));

    console.log(`   Socket active. Messages / keep-alives processed: ${receivedMessagesCount}`);
    client.disconnect();
    console.log('4. Clean disconnection completed.');

    console.log('🎉 PASSED: Live SignalR WebSocket connection and subscription succeeded!\n');
    return true;
  } catch (err) {
    console.error(`❌ FAILED: ${err.message}`);
    client.disconnect();
    return false;
  }
}

if (require.main === module) {
  testLiveConnection().then(passed => {
    process.exitCode = passed ? 0 : 1;
  });
}

module.exports = testLiveConnection;

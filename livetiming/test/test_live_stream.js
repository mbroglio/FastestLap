// Using native WebSocket in Node 24

async function test() {
  console.log('Negotiating...');
  const resp = await fetch('https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1', {
    method: 'POST',
    headers: {
      'User-Agent': 'BestHTTP',
      'Origin': 'https://www.formula1.com'
    }
  });
  const data = await resp.json();
  const token = data.connectionToken;
  console.log('Got token:', token.substring(0, 16));

  const ws = new WebSocket('wss://livetiming.formula1.com/signalrcore?id=' + encodeURIComponent(token), {
    headers: { 'User-Agent': 'BestHTTP' }
  });

  ws.onopen = () => {
    console.log('WebSocket open! Sending protocol handshake...');
    ws.send('{"protocol":"json","version":1}\x1e');
  };

  let handshakeDone = false;
  ws.onmessage = (event) => {
    const text = typeof event.data === 'string' ? event.data : event.data.toString();
    console.log('Received frame length:', text.length);
    const frames = text.split('\x1e');
    for (const frame of frames) {
      if (!frame.trim()) continue;
      try {
        const parsed = JSON.parse(frame);
        if (!handshakeDone) {
          handshakeDone = true;
          console.log('Handshake ACK received:', parsed);
          console.log('Subscribing to topics...');
          const sub = JSON.stringify({
            type: 1,
            invocationId: "0",
            target: "Subscribe",
            arguments: [["Heartbeat", "CarData.z", "Position.z", "TimingData", "TimingAppData", "SessionInfo", "SessionStatus", "TrackStatus", "RaceControlMessages", "DriverList"]]
          }) + '\x1e';
          ws.send(sub);
        } else {
          if (parsed.type === 3) {
            console.log('Initial Snapshot keys:', Object.keys(parsed.result || {}));
            if (parsed.result && parsed.result.SessionInfo) {
              console.log('SessionInfo:', JSON.stringify(parsed.result.SessionInfo));
            }
            if (parsed.result && parsed.result.DriverList) {
              console.log('DriverList count:', Object.keys(parsed.result.DriverList).length);
            }
            if (parsed.result && parsed.result.TrackStatus) {
              console.log('TrackStatus:', JSON.stringify(parsed.result.TrackStatus));
            }
          } else if (parsed.type === 1 && parsed.target === 'feed') {
            console.log('Live Feed:', parsed.arguments[0]);
          } else {
            console.log('Other message type:', parsed.type);
          }
        }
      } catch (e) {
        console.error('Parse error:', e.message);
      }
    }
  };

  ws.onerror = (err) => {
    console.error('WS Error:', err);
  };

  setTimeout(() => {
    console.log('Test completed 6s timeout, closing.');
    ws.close();
    process.exit(0);
  }, 6000);
}

test();

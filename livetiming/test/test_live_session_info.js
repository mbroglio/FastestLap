async function inspect() {
  const resp = await fetch('https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1', {
    method: 'POST',
    headers: { 'User-Agent': 'BestHTTP', 'Origin': 'https://www.formula1.com' }
  });
  const data = await resp.json();
  const token = data.connectionToken;
  const ws = new WebSocket('wss://livetiming.formula1.com/signalrcore?id=' + encodeURIComponent(token), {
    headers: { 'User-Agent': 'BestHTTP' }
  });
  ws.onopen = () => ws.send('{"protocol":"json","version":1}\x1e');
  let subscribed = false;
  ws.onmessage = (e) => {
    const text = typeof e.data === 'string' ? e.data : e.data.toString();
    const frames = text.split('\x1e');
    for (const f of frames) {
      if (!f.trim()) continue;
      try {
        const j = JSON.parse(f);
        if (!subscribed) {
          subscribed = true;
          ws.send(JSON.stringify({
            type: 1,
            invocationId: "0",
            target: "Subscribe",
            arguments: [["SessionInfo"]]
          }) + '\x1e');
        } else if (j.type === 3 && j.result && j.result.SessionInfo) {
          console.log('SessionInfo from server:', JSON.stringify(j.result.SessionInfo, null, 2));
          ws.close();
          process.exit(0);
        }
      } catch (err) {}
    }
  };
}
inspect();

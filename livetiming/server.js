/**
 * FastestLap LiveTiming - Server
 *
 * REST API + SSE Server for F1 Live Timing, Leaderboard and Telemetry.
 * Compatible with julesr0y/f1-livetiming-api REST endpoints.
 * Native Node.js (zero external npm dependencies required).
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const { StateStore } = require('./src/backend/state_store');
const { SignalRStreamClient } = require('./src/backend/signalr_stream_client');
const { MockStreamProvider } = require('./src/backend/mock_stream_provider');

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');

// Initialize State and Providers
const stateStore = new StateStore();
const signalrClient = new SignalRStreamClient(stateStore);
const mockProvider = new MockStreamProvider(stateStore);

// Active streaming mode: 'live' or 'mock'
let currentMode = process.argv.includes('--live') ? 'live' : 'mock';

// SSE Clients collection
const sseClients = new Set();

function broadcastSSE(eventType, data) {
  const payload = `event: ${eventType}\ndata: ${JSON.stringify(data)}\n\n`;
  for (const res of sseClients) {
    try {
      res.write(payload);
    } catch (err) {
      sseClients.delete(res);
    }
  }
}

// Bind stream updates to SSE broadcast
signalrClient.onUpdate((type, data) => broadcastSSE(type, data));
mockProvider.onUpdate((type, data) => broadcastSSE(type, data));

function switchMode(mode) {
  if (mode === currentMode) return;
  console.log(`[Server] Switching mode from ${currentMode} to ${mode}...`);
  if (currentMode === 'mock') mockProvider.stop();
  if (currentMode === 'live') signalrClient.disconnect();

  currentMode = mode;
  stateStore.reset();

  if (currentMode === 'live') {
    signalrClient.connect();
  } else {
    mockProvider.start();
  }
  broadcastSSE('MODE_CHANGE', { mode: currentMode });
}

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
  const reqUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  const pathname = reqUrl.pathname;

  // Enable CORS headers on all endpoints
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // ─────────────────────────────────────────────────────────────
  // Server-Sent Events (SSE) Stream
  // ─────────────────────────────────────────────────────────────
  if (pathname === '/api/stream') {
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      'Connection': 'keep-alive'
    });
    res.write(`event: INIT\ndata: ${JSON.stringify(stateStore.getFullSnapshot())}\n\n`);
    sseClients.add(res);
    req.on('close', () => sseClients.delete(res));
    return;
  }

  // ─────────────────────────────────────────────────────────────
  // REST API Endpoints (julesr0y compatible)
  // ─────────────────────────────────────────────────────────────
  if (pathname === '/api/status') {
    const status = stateStore.getStatus();
    status.mode = currentMode;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(status, null, 2));
    return;
  }

  if (pathname === '/api/standings' || pathname === '/api/leaderboard') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.getLeaderboard(), null, 2));
    return;
  }

  if (pathname === '/api/tyres' || pathname === '/api/stints') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.getTyresSummary(), null, 2));
    return;
  }

  if (pathname === '/api/drivers') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.driverList, null, 2));
    return;
  }

  if (pathname === '/api/session') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.sessionInfo, null, 2));
    return;
  }

  if (pathname === '/api/weather') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ Weather: stateStore.weatherData }, null, 2));
    return;
  }

  if (pathname === '/api/race-control' || pathname === '/api/flags') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.raceControlMessages, null, 2));
    return;
  }

  if (pathname === '/api/lap-count' || pathname === '/api/laps') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.lapCount, null, 2));
    return;
  }

  if (pathname === '/api/telemetry') {
    const driverNum = reqUrl.searchParams.get('driver');
    const data = driverNum ? (stateStore.carData[driverNum] || null) : stateStore.carData;
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(data, null, 2));
    return;
  }

  if (pathname === '/api/positions') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.positions, null, 2));
    return;
  }

  if (pathname === '/api/snapshot') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(stateStore.getFullSnapshot(), null, 2));
    return;
  }

  if (pathname === '/api/mode' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const json = JSON.parse(body || '{}');
        if (json.mode === 'live' || json.mode === 'mock') {
          switchMode(json.mode);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true, mode: currentMode }));
          return;
        }
      } catch (e) {}
      res.writeHead(400, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Invalid mode. Use "live" or "mock"' }));
    });
    return;
  }

  // ─────────────────────────────────────────────────────────────
  // Static Files Serving
  // ─────────────────────────────────────────────────────────────
  let safePath = pathname === '/' ? '/index.html' : pathname;
  const filePath = path.join(PUBLIC_DIR, safePath);

  // Security check to avoid directory traversal
  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    res.end('Access Denied');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('File Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    res.writeHead(200, { 'Content-Type': contentType });
    const stream = fs.createReadStream(filePath);
    stream.pipe(res);
  });
});

server.listen(PORT, () => {
  console.log('================================================================');
  console.log(` FastestLap Live Timing Server running at http://localhost:${PORT}`);
  console.log(` Initial Mode: [${currentMode.toUpperCase()}]`);
  console.log(' Available REST Endpoints:');
  console.log(`  - GET  http://localhost:${PORT}/api/status`);
  console.log(`  - GET  http://localhost:${PORT}/api/standings`);
  console.log(`  - GET  http://localhost:${PORT}/api/leaderboard`);
  console.log(`  - GET  http://localhost:${PORT}/api/tyres`);
  console.log(`  - GET  http://localhost:${PORT}/api/drivers`);
  console.log(`  - GET  http://localhost:${PORT}/api/telemetry`);
  console.log(`  - GET  http://localhost:${PORT}/api/positions`);
  console.log(`  - GET  http://localhost:${PORT}/api/stream (SSE Push)`);
  console.log(`  - POST http://localhost:${PORT}/api/mode ({"mode":"live"|"mock"})`);
  console.log('================================================================');

  if (currentMode === 'live') {
    signalrClient.connect();
  } else {
    mockProvider.start();
  }
});

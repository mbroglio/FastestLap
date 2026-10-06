const http = require('http');
const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const PORT = 8080;
const DIR = __dirname;

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.json': 'application/json',
  '.js': 'application/javascript',
  '.css': 'text/css'
};

const server = http.createServer((req, res) => {
  let reqPath = req.url.split('?')[0];
  if (reqPath === '/' || reqPath === '') reqPath = '/index.html';

  const filePath = path.join(DIR, reqPath);
  if (!fs.existsSync(filePath)) {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('404 Not Found: ' + reqPath);
    return;
  }

  const ext = path.extname(filePath).toLowerCase();
  const contentType = MIME[ext] || 'application/octet-stream';

  res.writeHead(200, {
    'Content-Type': contentType,
    'Cache-Control': 'no-cache'
  });
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, () => {
  const url = `http://localhost:${PORT}`;
  console.log(`\n===========================================================`);
  console.log(`🏎️  FASTESTLAP — SEPANG GPS LIVE TRACK VISUALIZER`);
  console.log(`===========================================================`);
  console.log(`Server HTTP locale avviato con successo!`);
  console.log(`URL: ${url}`);
  console.log(`Apertura automatica del browser in corso...`);
  console.log(`(Premi Ctrl+C nel terminale per arrestare il server)`);
  console.log(`===========================================================\n`);

  // Open default browser on Windows
  exec(`start ${url}`);
});

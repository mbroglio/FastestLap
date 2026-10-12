/**
 * FastestLap LiveTiming - Track Canvas Renderer (Corretto)
 */

class TrackCanvas {
  constructor(canvasElement) {
    this.canvas = canvasElement;
    this.ctx = this.canvas.getContext('2d');
    this.positions = {};
    this.drivers = {};
    this.selectedDriver = null;
    this.animationId = null;
    this.trackImage = null;

    // Bounding Box dinamico per scalare le coordinate cartesiane reali di F1
    this.bounds = {
      minX: Infinity,
      maxX: -Infinity,
      minY: Infinity,
      maxY: -Infinity,
      isCalibrated: false
    };

    // Scia di punti per ricostruire visivamente il tracciato reale in live
    this.trackBreadcrumbs = [];
    this.maxBreadcrumbs = 600;

    this._resize();
    window.addEventListener('resize', () => this._resize());
    window.setCircuitImage = (url) => this.setCircuitImage(url);

    this.startRendering();
  }

  setCircuitImage(url) {
    if (!url || typeof url !== 'string' || !url.trim()) return;
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => { this.trackImage = img; };
    img.src = url.trim();
  }

  _resize() {
    const rect = this.canvas.parentElement.getBoundingClientRect();
    this.width = rect.width;
    this.height = rect.height;
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = this.width * dpr;
    this.canvas.height = this.height * dpr;
    this.ctx.setTransform(1, 0, 0, 1, 0, 0);
    this.ctx.scale(dpr, dpr);
  }

  updateDrivers(driverMap) {
    this.drivers = driverMap || {};
  }

  updatePositions(positionsMap) {
    if (!positionsMap) return;

    for (const [num, pos] of Object.entries(positionsMap)) {
      if (pos == null || typeof pos.x !== 'number' || typeof pos.y !== 'number') continue;
      if (!Number.isFinite(pos.x) || !Number.isFinite(pos.y)) continue;

      // Aggiornamento Bounding Box
      if (pos.x < this.bounds.minX) this.bounds.minX = pos.x;
      if (pos.x > this.bounds.maxX) this.bounds.maxX = pos.x;
      if (pos.y < this.bounds.minY) this.bounds.minY = pos.y;
      if (pos.y > this.bounds.maxY) this.bounds.maxY = pos.y;
      this.bounds.isCalibrated = true;

      // Memorizza la scia per disegnare la pista reale
      if (this.trackBreadcrumbs.length < this.maxBreadcrumbs) {
        this.trackBreadcrumbs.push({ x: pos.x, y: pos.y });
      }

      if (!this.positions[num]) {
        this.positions[num] = { x: pos.x, y: pos.y, curX: pos.x, curY: pos.y };
      } else {
        this.positions[num].x = pos.x;
        this.positions[num].y = pos.y;
        if (!Number.isFinite(this.positions[num].curX)) this.positions[num].curX = pos.x;
        if (!Number.isFinite(this.positions[num].curY)) this.positions[num].curY = pos.y;
      }
    }
  }

  setSelectedDriver(driverNum) {
    this.selectedDriver = String(driverNum);
  }

  startRendering() {
    const loop = () => {
      this.render();
      this.animationId = requestAnimationFrame(loop);
    };
    this.animationId = requestAnimationFrame(loop);
  }

  render() {
    const ctx = this.ctx;
    const w = this.width;
    const h = this.height;
    if (!w || !h) return;

    ctx.clearRect(0, 0, w, h);

    if (!this.bounds.isCalibrated) {
      ctx.fillStyle = '#64748b';
      ctx.font = '12px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('IN ATTESA DI COORDINATE TELEMETRICHE DALLA PISTA...', w / 2, h / 2);
      return;
    }

    const padding = 40;
    const spanX = Math.max(1, this.bounds.maxX - this.bounds.minX);
    const spanY = Math.max(1, this.bounds.maxY - this.bounds.minY);

    const scaleX = (w - padding * 2) / spanX;
    const scaleY = (h - padding * 2) / spanY;
    const scale = Math.min(scaleX, scaleY);

    const midX = (this.bounds.minX + this.bounds.maxX) / 2;
    const midY = (this.bounds.minY + this.bounds.maxY) / 2;

    const toScreenX = (x) => (w / 2) + (x - midX) * scale;
    // Inverti asse Y cartesiano in coordinate schermo
    const toScreenY = (y) => (h / 2) - (y - midY) * scale;

    // 1. Disegna tracciato tramite breadcrumbs se non c'è immagine di sfondo
    if (this.trackBreadcrumbs.length > 10) {
      ctx.beginPath();
      for (let i = 0; i < this.trackBreadcrumbs.length; i++) {
        const pt = this.trackBreadcrumbs[i];
        const sx = toScreenX(pt.x);
        const sy = toScreenY(pt.y);
        if (i === 0) ctx.moveTo(sx, sy);
        else ctx.lineTo(sx, sy);
      }
      ctx.strokeStyle = '#1e293b';
      ctx.lineWidth = 14;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.stroke();

      ctx.strokeStyle = '#0f172a';
      ctx.lineWidth = 8;
      ctx.stroke();
    }

    // 2. Rendering delle Monoposto
    for (const [num, pos] of Object.entries(this.positions)) {
      if (!Number.isFinite(pos.x) || !Number.isFinite(pos.y)) continue;

      // LERP protetto da NaN[cite: 2]
      pos.curX += (pos.x - pos.curX) * 0.25;
      pos.curY += (pos.y - pos.curY) * 0.25;

      const sx = toScreenX(pos.curX);
      const sy = toScreenY(pos.curY);

      if (!Number.isFinite(sx) || !Number.isFinite(sy)) continue;

      const driver = this.drivers[num] || {};
      const teamColor = driver.TeamColour ? (driver.TeamColour.startsWith('#') ? driver.TeamColour : `#${driver.TeamColour}`) : '#ffffff';
      const isSelected = this.selectedDriver === num;

      // Alone pilota selezionato
      if (isSelected) {
        ctx.beginPath();
        ctx.arc(sx, sy, 14, 0, 2 * Math.PI);
        ctx.fillStyle = 'rgba(225, 6, 0, 0.35)';
        ctx.fill();
        ctx.strokeStyle = '#e10600';
        ctx.lineWidth = 2;
        ctx.stroke();
      }

      // Vettura
      ctx.beginPath();
      ctx.arc(sx, sy, 7, 0, 2 * Math.PI);
      ctx.fillStyle = teamColor;
      ctx.fill();
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Etichetta TLA
      const tla = driver.Tla || num;
      ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
      ctx.fillRect(sx + 9, sy - 7, 24, 13);
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 8px monospace';
      ctx.textAlign = 'left';
      ctx.textBaseline = 'middle';
      ctx.fillText(tla, sx + 11, sy);
    }
  }
}

window.TrackCanvas = TrackCanvas;
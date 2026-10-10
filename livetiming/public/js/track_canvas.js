/**
 * FastestLap LiveTiming - Track Canvas Renderer
 * 2D Real-time Circuit Map & Driver Tracking with 60 FPS animation.
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

    // Default reference geometry (normalized parametric circuit fallback)
    this.circuitNodes = this._generateDefaultCircuit();

    this._resize();
    window.addEventListener('resize', () => this._resize());
    
    // Esponi per bridge Android LiveActivity
    window.setCircuitImage = (url) => this.setCircuitImage(url);
    
    this.startRendering();
  }

  setCircuitImage(url) {
    if (!url || typeof url !== 'string' || url.trim().length === 0) return;
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      this.trackImage = img;
      console.log('[TrackCanvas] Layout tracciato caricato:', url);
    };
    img.onerror = () => {
      console.warn('[TrackCanvas] Errore caricamento layout tracciato:', url);
    };
    img.src = url.trim();
  }

  _generateDefaultCircuit() {
    const nodes = [];
    const steps = 120;
    for (let i = 0; i < steps; i++) {
      const theta = (i / steps) * 2 * Math.PI;
      const x = Math.sin(theta) * 400 + Math.sin(theta * 3) * 60;
      const y = Math.cos(theta) * 220 + Math.cos(theta * 2) * 50;
      nodes.push({ x, y });
    }
    return nodes;
  }

  _resize() {
    const rect = this.canvas.parentElement.getBoundingClientRect();
    this.width = rect.width;
    this.height = rect.height;
    this.canvas.width = this.width * window.devicePixelRatio || this.width;
    this.canvas.height = this.height * window.devicePixelRatio || this.height;
    this.ctx.scale(window.devicePixelRatio || 1, window.devicePixelRatio || 1);
  }

  updateDrivers(driverMap) {
    this.drivers = driverMap || {};
  }

  updatePositions(positionsMap) {
    if (!positionsMap) return;
    for (const [num, pos] of Object.entries(positionsMap)) {
      if (!this.positions[num]) {
        this.positions[num] = { x: pos.x, y: pos.y, curX: pos.x, curY: pos.y };
      } else {
        this.positions[num].x = pos.x;
        this.positions[num].y = pos.y;
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

    const centerX = w / 2;
    const centerY = h / 2;
    const scale = Math.min(w / 1000, h / 650);

    // 1. Draw Track Surface
    ctx.save();
    ctx.translate(centerX, centerY);
    ctx.scale(scale, scale);

    if (this.trackImage && this.trackImage.complete && this.trackImage.naturalWidth > 0) {
      // Draw loaded official circuit SVG/WebP image
      const imgW = this.trackImage.naturalWidth;
      const imgH = this.trackImage.naturalHeight;
      const fitRatio = Math.min(800 / imgW, 550 / imgH);
      const dw = imgW * fitRatio;
      const dh = imgH * fitRatio;
      ctx.drawImage(this.trackImage, -dw / 2, -dh / 2, dw, dh);
    } else {
      // Fallback Vector Track Loop
      ctx.beginPath();
      this.circuitNodes.forEach((pt, i) => {
        if (i === 0) ctx.moveTo(pt.x, pt.y);
        else ctx.lineTo(pt.x, pt.y);
      });
      ctx.closePath();
      ctx.strokeStyle = '#262f3f';
      ctx.lineWidth = 18;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.stroke();

      // Track Centerline
      ctx.strokeStyle = '#10141c';
      ctx.lineWidth = 14;
      ctx.stroke();

      // Start/Finish Line marker
      if (this.circuitNodes.length > 0) {
        const sf = this.circuitNodes[0];
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(sf.x - 2, sf.y - 12, 4, 24);
      }
    }

    // 2. Draw Driver Positions
    for (const [num, pos] of Object.entries(this.positions)) {
      if (pos.x === null || pos.y === null) continue;

      // Smooth coordinate interpolation (lerp)
      pos.curX += (pos.x - pos.curX) * 0.25;
      pos.curY += (pos.y - pos.curY) * 0.25;

      const driver = this.drivers[num] || {};
      const teamColor = driver.TeamColour || '#ffffff';
      const isSelected = this.selectedDriver === num;

      // Halo for selected driver
      if (isSelected) {
        ctx.beginPath();
        ctx.arc(pos.curX, pos.curY, 14, 0, 2 * Math.PI);
        ctx.fillStyle = 'rgba(255, 23, 68, 0.35)';
        ctx.fill();
        ctx.strokeStyle = '#ff1744';
        ctx.lineWidth = 2;
        ctx.stroke();
      }

      // Driver Circle
      ctx.beginPath();
      ctx.arc(pos.curX, pos.curY, 8, 0, 2 * Math.PI);
      ctx.fillStyle = teamColor.startsWith('#') ? teamColor : `#${teamColor}`;
      ctx.fill();
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Number Label
      ctx.fillStyle = '#000000';
      ctx.font = 'bold 8px monospace';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(num, pos.curX, pos.curY);

      // Driver TLA pill
      if (isSelected || scale > 0.8) {
        const tla = driver.Tla || num;
        ctx.fillStyle = 'rgba(10, 12, 16, 0.85)';
        ctx.fillRect(pos.curX + 10, pos.curY - 7, 24, 12);
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 8px sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(tla, pos.curX + 12, pos.curY + 2);
      }
    }

    ctx.restore();
  }
}

window.TrackCanvas = TrackCanvas;

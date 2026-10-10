/**
 * FastestLap — track_canvas.js
 * Modulo Rendering Canvas Tracciato Mappa, Zoom, Pan e Tracciamento Posizioni
 */

let canvas = null;
let ctx = null;
let canvasScale = 1.0;
let canvasCssWidth = 0;
let canvasCssHeight = 0;

let userZoom = 1.0;
let userPanX = 0;
let userPanY = 0;
let isMapPanning = false;
let panStartX = 0;
let panStartY = 0;

let driverVisualSmoothCache = {};
let currentDeltaTime = 0.016;

window.setCircuitImage = function(url) {
  if (!url || typeof url !== 'string' || url.trim().length === 0) return;
  const cleanUrl = url.trim();
  console.log('[TrackCanvas] Caricamento layout tracciato da:', cleanUrl);
  const img = new Image();
  img.crossOrigin = 'anonymous';
  img.onload = function() {
    trackBgImage = img;
    SVG_WIDTH = (img.naturalWidth > 0) ? img.naturalWidth : 1000;
    SVG_HEIGHT = (img.naturalHeight > 0) ? img.naturalHeight : 800;
    resizeTrackCanvas();
    drawTrack();
  };
  img.onerror = function() {
    console.warn('[TrackCanvas] Retry caricamento tracciato senza crossOrigin:', cleanUrl);
    const retryImg = new Image();
    retryImg.onload = function() {
      trackBgImage = retryImg;
      SVG_WIDTH = (retryImg.naturalWidth > 0) ? retryImg.naturalWidth : 1000;
      SVG_HEIGHT = (retryImg.naturalHeight > 0) ? retryImg.naturalHeight : 800;
      resizeTrackCanvas();
      drawTrack();
    };
    retryImg.src = cleanUrl;
  };
  img.src = cleanUrl;
};

function resizeTrackCanvas() {
  const container = document.getElementById('trackPane') || document.querySelector('.track-pane') || document.querySelector('.track-canvas-container');
  if (!container || !canvas) return;

  const rect = container.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return;

  const dpr = window.devicePixelRatio || 1;
  canvasCssWidth = rect.width;
  canvasCssHeight = rect.height;

  canvas.width = Math.round(rect.width * dpr);
  canvas.height = Math.round(rect.height * dpr);
  canvas.style.width = rect.width + 'px';
  canvas.style.height = rect.height + 'px';

  if (ctx) {
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);
  }

  const scaleX = (rect.width - 16) / (SVG_WIDTH || 1000);
  const scaleY = (rect.height - 16) / (SVG_HEIGHT || 800);
  canvasScale = Math.min(scaleX, scaleY);
}

function pxToScreen(px, py) {
  const cx = canvasCssWidth / 2;
  const cy = canvasCssHeight / 2;
  const ox = (SVG_WIDTH || 1000) / 2;
  const oy = (SVG_HEIGHT || 800) / 2;
  return {
    x: cx + ((px - ox) * canvasScale * userZoom) + userPanX,
    y: cy + ((py - oy) * canvasScale * userZoom) + userPanY
  };
}

function updateZoomPill() {
  const p = document.getElementById('mapZoomPill');
  if (p) p.textContent = Math.round(userZoom * 100) + '%';
}

function updateZoomButtonsState() {
  const btnIn = document.getElementById('btnZoomIn');
  const btnOut = document.getElementById('btnZoomOut');
  if (btnIn) btnIn.disabled = userZoom >= 5.0;
  if (btnOut) btnOut.disabled = userZoom <= 0.6;
}

function zoomMapStep(direction) {
  const factor = direction > 0 ? 1.25 : 0.8;
  zoomMap(factor);
}

function zoomMap(factor) {
  const newZoom = Math.max(0.6, Math.min(5.0, userZoom * factor));
  if (Math.abs(newZoom - userZoom) < 0.01) return;
  userZoom = newZoom;
  updateZoomPill();
  updateZoomButtonsState();
  drawTrack();
}

function resetMapZoom() {
  userZoom = 1.0;
  userPanX = 0;
  userPanY = 0;
  isMapFollowingDriver = false;
  const r = document.getElementById('btnResetMapFocus');
  if (r) r.style.display = 'none';
  updateZoomPill();
  updateZoomButtonsState();
  drawTrack();
}

function resetMapFocus() {
  isMapFollowingDriver = false;
  userZoom = 1.0;
  userPanX = 0;
  userPanY = 0;
  const r = document.getElementById('btnResetMapFocus');
  if (r) r.style.display = 'none';
  updateZoomPill();
  updateZoomButtonsState();
  drawTrack();
}

function centerMapOnDriver(drvKey) {
  if (!NODES || NODES.length === 0) return;
  const state = getDriverVisualState(drvKey, currentSecond);
  if (!state || !state.px || !state.py) return;
  const cx = canvasCssWidth / 2;
  const cy = canvasCssHeight / 2;
  const ox = (SVG_WIDTH || 1000) / 2;
  const oy = (SVG_HEIGHT || 800) / 2;
  userPanX = cx - (cx + ((state.px - ox) * canvasScale * userZoom));
  userPanY = cy - (cy + ((state.py - oy) * canvasScale * userZoom));
}

function setupTrackMapInteractions() {
  const c = document.getElementById('trackCanvas');
  if (!c) return;

  const pane = document.getElementById('trackPane') || document.querySelector('.track-pane');
  if (pane && window.ResizeObserver) {
    try {
      const ro = new ResizeObserver(entries => {
        for (const entry of entries) {
          if (entry.contentRect.width > 0 && entry.contentRect.height > 0) {
            resizeTrackCanvas();
            drawTrack();
          }
        }
      });
      ro.observe(pane);
    } catch (e) {}
  }

  c.addEventListener('mousedown', e => {
    isMapPanning = true;
    panStartX = e.clientX - userPanX;
    panStartY = e.clientY - userPanY;
  });

  window.addEventListener('mousemove', e => {
    if (!isMapPanning) return;
    userPanX = e.clientX - panStartX;
    userPanY = e.clientY - panStartY;
    drawTrack();
  });

  window.addEventListener('mouseup', () => { isMapPanning = false; });

  c.addEventListener('touchstart', e => {
    if (e.touches.length === 1) {
      isMapPanning = true;
      panStartX = e.touches[0].clientX - userPanX;
      panStartY = e.touches[0].clientY - userPanY;
    }
  }, { passive: true });

  c.addEventListener('touchmove', e => {
    if (isMapPanning && e.touches.length === 1) {
      userPanX = e.touches[0].clientX - panStartX;
      userPanY = e.touches[0].clientY - panStartY;
      drawTrack();
    }
  }, { passive: true });

  c.addEventListener('touchend', () => { isMapPanning = false; });

  c.addEventListener('wheel', e => {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.15 : 0.85;
    zoomMap(factor);
  }, { passive: false });
}

function drawTrack() {
  if (!ctx || !canvas) return;

  if (canvasCssWidth <= 0 || canvasCssHeight <= 0) {
    resizeTrackCanvas();
  }

  if (isMapFollowingDriver && focusedDriver && NODES && NODES.length > 0) {
    centerMapOnDriver(focusedDriver);
  }

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // 1. Rendering Layout Tracciato Dinamico (Immagine PNG/SVG da Firebase o Intent Android)
  if (trackBgImage && (trackBgImage.complete || trackBgImage.naturalWidth > 0)) {
    ctx.save();
    const cx = canvasCssWidth / 2;
    const cy = canvasCssHeight / 2;
    const natW = trackBgImage.naturalWidth || SVG_WIDTH || 800;
    const natH = trackBgImage.naturalHeight || SVG_HEIGHT || 600;

    const drawW = natW * canvasScale * userZoom;
    const drawH = natH * canvasScale * userZoom;
    const drawX = cx - (drawW / 2) + userPanX;
    const drawY = cy - (drawH / 2) + userPanY;

    ctx.filter = 'brightness(0) invert(1)';
    ctx.drawImage(trackBgImage, drawX, drawY, drawW, drawH);
    ctx.restore();
  } else if (!NODES || NODES.length === 0) {
    // Placeholder dinamico quando l'immagine è in caricamento
    ctx.save();
    const cx = (canvasCssWidth || 400) / 2;
    const cy = (canvasCssHeight || 300) / 2;
    ctx.fillStyle = '#8b949e';
    ctx.font = 'bold 15px sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    const cName = (currentCircuit && currentCircuit !== 'generic')
      ? currentCircuit.toUpperCase().replace(/_/g, ' ')
      : 'TRACCIATO FORMULA 1';
    ctx.fillText('LAYOUT CIRCUITO: ' + cName, cx + userPanX, cy + userPanY - 12);
    ctx.font = '12px sans-serif';
    ctx.fillStyle = '#586069';
    ctx.fillText('Caricamento mappa tracciato in corso...', cx + userPanX, cy + userPanY + 14);
    ctx.restore();
  }

  // 2. Traiettoria Vettoriale (se disponibile per la sessione o come tracciato visivo)
  if (NODES && NODES.length > 0) {
    ctx.save();
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    const zoomScale = Math.min(2.5, Math.sqrt(userZoom));

    // Solo se non c'è già un'immagine ufficiale del circuito, disegna l'asfalto vettoriale
    if (!trackBgImage || !trackBgImage.complete) {
      ctx.strokeStyle = '#1e242d';
      ctx.lineWidth = Math.max(6, 16 * canvasScale * zoomScale);
      ctx.beginPath();
      for (let i = 0; i < NODES.length; i++) {
        const pt = pxToScreen(NODES[i][1], NODES[i][2]);
        if (i === 0) ctx.moveTo(pt.x, pt.y);
        else ctx.lineTo(pt.x, pt.y);
      }
      ctx.closePath();
      ctx.stroke();

      ctx.strokeStyle = '#384454';
      ctx.lineWidth = Math.max(2, 4 * canvasScale * zoomScale);
      ctx.beginPath();
      for (let i = 0; i < NODES.length; i++) {
        const pt = pxToScreen(NODES[i][1], NODES[i][2]);
        if (i === 0) ctx.moveTo(pt.x, pt.y);
        else ctx.lineTo(pt.x, pt.y);
      }
      ctx.closePath();
      ctx.stroke();
    }

    // Linea del traguardo
    const sfPt = pxToScreen(NODES[0][1], NODES[0][2]);
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(sfPt.x, sfPt.y, 4 * zoomScale, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }

  // 3. Rendering Fluido a 60 FPS dei 22 Piloti in pista con LERP esponenziale
    const driverKeys = Object.keys(DRIVERS);
    driverKeys.forEach(k => {
      const d = DRIVERS[k];
      if (!d) return;
      const state = getDriverVisualState(k, currentSecond);
      if (!state || state.px === undefined || state.py === undefined) return;
      const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType;
      if ((sType === 'practice' || sType === 'qualifying') && state.inPit) return;

      const targetScr = pxToScreen(state.px, state.py);

      // Interpolazione esponenziale anti-scatto continua 60/90/120 FPS
      if (!driverVisualSmoothCache[k]) {
        driverVisualSmoothCache[k] = { curX: targetScr.x, curY: targetScr.y };
      } else {
        const dt = (typeof currentDeltaTime !== 'undefined' && currentDeltaTime > 0) ? currentDeltaTime : 0.016;
        const alpha = 1.0 - Math.exp(-14.0 * dt);
        driverVisualSmoothCache[k].curX += (targetScr.x - driverVisualSmoothCache[k].curX) * alpha;
        driverVisualSmoothCache[k].curY += (targetScr.y - driverVisualSmoothCache[k].curY) * alpha;
      }
      const scr = driverVisualSmoothCache[k];

      let isHighlighted = false;
      if (currentSideTab === 'side-telemetry') {
        isHighlighted = (k === selectedDriver1 || k === selectedDriver2);
      } else {
        isHighlighted = Boolean(focusedDriver && k === focusedDriver);
      }

      ctx.save();
      const baseR = isHighlighted ? 6.5 : 5.0;
      const dotR = baseR * Math.min(1.5, Math.sqrt(userZoom));

      // Alone per pilota evidenziato
      if (isHighlighted) {
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        ctx.arc(scr.curX, scr.curY, dotR + 3, 0, Math.PI * 2);
        ctx.stroke();
      }

      // Corpo circolare del pilota con colore team
      ctx.fillStyle = state.isRetired ? '#484f58' : (d.color || '#e10600');
      ctx.beginPath();
      ctx.arc(scr.curX, scr.curY, dotR, 0, Math.PI * 2);
      ctx.fill();

      // Bordo nero di contrasto
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 1.2;
      ctx.beginPath();
      ctx.arc(scr.curX, scr.curY, dotR, 0, Math.PI * 2);
      ctx.stroke();

      // Numero pilota al centro del cerchio
      ctx.fillStyle = '#ffffff';
      const numFont = Math.max(6, Math.round(dotR * 1.0));
      ctx.font = 'bold ' + numFont + 'px monospace';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(d.number, scr.curX, scr.curY + 0.5);

      // Badge TLA pilota (es. LEC, VER, HAM)
      const fontSize = Math.round((isHighlighted ? 9.5 : 8.0) * Math.min(1.4, Math.sqrt(userZoom)));
      ctx.font = 'bold ' + fontSize + 'px sans-serif';
      const badgeText = d.code;
      const textW = ctx.measureText(badgeText).width;
      const pillW = textW + 6;
      const pillH = fontSize + 4;
      const pillX = scr.curX - pillW / 2;
      const pillY = scr.curY - dotR - pillH - 2;

      ctx.fillStyle = 'rgba(10, 14, 20, 0.88)';
      ctx.fillRect(pillX, pillY, pillW, pillH);

      ctx.strokeStyle = isHighlighted ? '#ffffff' : (d.color || '#484f58');
      ctx.lineWidth = 1;
      ctx.strokeRect(pillX, pillY, pillW, pillH);

      ctx.fillStyle = '#ffffff';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(badgeText, scr.curX, pillY + pillH / 2);

      // Indicatore PIT
      if (state.inPit) {
        ctx.fillStyle = '#ff9800';
        ctx.font = 'bold 7px sans-serif';
        ctx.fillText('PIT', scr.curX, scr.curY + dotR + 6);
      }

      ctx.restore();
    });
}

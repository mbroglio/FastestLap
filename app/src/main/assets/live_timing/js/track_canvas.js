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

window.setCircuitImage = function(url) {
  if (!url || typeof url !== 'string' || url.trim().length === 0) return;
  const cleanUrl = url.trim();
  const img = new Image();
  img.onload = function() {
    trackBgImage = img;
    drawTrack();
  };
  img.onerror = function() {
    console.warn('Impossibile caricare immagine tracciato:', cleanUrl);
  };
  img.src = cleanUrl;
};

function resizeTrackCanvas() {
  const container = document.querySelector('.track-canvas-container');
  if (!container || !canvas) return;

  const rect = container.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  canvasCssWidth = rect.width;
  canvasCssHeight = rect.height;

  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  canvas.style.width = rect.width + 'px';
  canvas.style.height = rect.height + 'px';

  if (ctx) {
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);
  }

  const scaleX = rect.width / (SVG_WIDTH || 1000);
  const scaleY = rect.height / (SVG_HEIGHT || 800);
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
  const r = document.getElementById('btnResetMapFocus');
  if (r) r.style.display = 'none';
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

  if (isMapFollowingDriver && focusedDriver && NODES && NODES.length > 0) {
    centerMapOnDriver(focusedDriver);
  }

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // 1. Rendering Layout Tracciato Dinamico (Immagine PNG/SVG da Firebase o Intent Android)
  if (trackBgImage && (trackBgImage.complete || trackBgImage.naturalWidth > 0)) {
    ctx.save();
    const cx = (canvasCssWidth || 400) / 2;
    const cy = (canvasCssHeight || 300) / 2;
    const margin = 24;
    const availW = Math.max(100, (canvasCssWidth || 400) - margin * 2);
    const availH = Math.max(100, (canvasCssHeight || 300) - margin * 2);
    const natW = trackBgImage.naturalWidth || trackBgImage.width || 800;
    const natH = trackBgImage.naturalHeight || trackBgImage.height || 600;
    const imgAspect = natW / natH;
    const containerAspect = availW / availH;
    let drawW, drawH;
    if (imgAspect > containerAspect) {
      drawW = availW;
      drawH = availW / imgAspect;
    } else {
      drawH = availH;
      drawW = availH * imgAspect;
    }
    const finalW = drawW * userZoom;
    const finalH = drawH * userZoom;
    const drawX = cx - finalW / 2 + userPanX;
    const drawY = cy - finalH / 2 + userPanY;

    ctx.drawImage(trackBgImage, drawX, drawY, finalW, finalH);
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

  // 2. Traiettoria Vettoriale (se disponibile per la sessione)
  if (NODES && NODES.length > 0) {
    ctx.save();
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    const zoomScale = Math.min(2.5, Math.sqrt(userZoom));

    ctx.strokeStyle = '#2b313a';
    ctx.lineWidth = Math.max(3, 8 * canvasScale * zoomScale);
    ctx.beginPath();
    for (let i = 0; i < NODES.length; i++) {
      const pt = pxToScreen(NODES[i][1], NODES[i][2]);
      if (i === 0) ctx.moveTo(pt.x, pt.y);
      else ctx.lineTo(pt.x, pt.y);
    }
    ctx.closePath();
    ctx.stroke();

    ctx.strokeStyle = '#586069';
    ctx.lineWidth = Math.max(1, 2 * canvasScale * zoomScale);
    ctx.beginPath();
    for (let i = 0; i < NODES.length; i++) {
      const pt = pxToScreen(NODES[i][1], NODES[i][2]);
      if (i === 0) ctx.moveTo(pt.x, pt.y);
      else ctx.lineTo(pt.x, pt.y);
    }
    ctx.closePath();
    ctx.stroke();

    // Linea del traguardo
    const sfPt = pxToScreen(NODES[0][1], NODES[0][2]);
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(sfPt.x, sfPt.y, 4 * zoomScale, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();

    // Rendering dei 22 Piloti in pista
    const driverKeys = Object.keys(DRIVERS);
    driverKeys.forEach(k => {
      const d = DRIVERS[k];
      const state = getDriverVisualState(k, currentSecond);
      if ((sessionType === 'practice' || sessionType === 'qualifying') && state.inPit) return;
      if (!state || state.px === undefined || state.py === undefined) return;

      const scr = pxToScreen(state.px, state.py);
      let isHighlighted = false;
      if (currentSideTab === 'side-telemetry') {
        isHighlighted = (k === selectedDriver1 || k === selectedDriver2);
      } else {
        isHighlighted = Boolean(focusedDriver && k === focusedDriver);
      }

      ctx.save();
      const baseR = isHighlighted ? 5.5 : 4.5;
      const dotR = baseR * Math.min(1.5, Math.sqrt(userZoom));

      if (isHighlighted) {
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(scr.x, scr.y, dotR + 2.5, 0, Math.PI * 2);
        ctx.stroke();
      }

      ctx.fillStyle = state.isRetired ? '#484f58' : d.color;
      ctx.beginPath();
      ctx.arc(scr.x, scr.y, dotR, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = '#f0f6fc';
      const fontSize = Math.round((isHighlighted ? 9 : 7.5) * Math.min(1.4, Math.sqrt(userZoom)));
      ctx.font = 'bold ' + fontSize + 'px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'bottom';
      ctx.fillText(d.code, scr.x, scr.y - (dotR + 2));
      ctx.restore();
    });
  }
}

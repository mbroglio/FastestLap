/**
 * FastestLap — Circuit Selector & Auto-Redirect Logic
 */
(function() {
  'use strict';

  // 1. Check URL parameters
  const params = new URLSearchParams(window.location.search);
  let targetCircuit = params.get('circuit') || params.get('c');

  // 2. Check Android Bridge if available
  if (!targetCircuit && window.FastestLapBridge && typeof window.FastestLapBridge.getCircuitId === 'function') {
    try {
      targetCircuit = window.FastestLapBridge.getCircuitId();
    } catch(e) {}
  }

  // 3. Check hash (#baku or #sepang)
  if (!targetCircuit && window.location.hash) {
    const hash = window.location.hash.toLowerCase().replace('#', '');
    if (hash === 'baku' || hash === 'sepang') targetCircuit = hash;
  }

  // 4. Auto-redirect if circuit specified
  if (targetCircuit) {
    const lower = targetCircuit.toLowerCase();
    const spinner = document.getElementById('loadingSpinner');
    const desc = document.getElementById('statusDesc');
    if (spinner) spinner.style.display = 'block';
    if (desc) desc.innerText = 'Caricamento tracciato ' + targetCircuit + ' in corso...';
    
    if (lower.includes('baku') || lower.includes('azerbaijan')) {
      window.location.replace('track_map.html?circuit=baku' + (window.location.search ? '&' + window.location.search.substring(1) : ''));
    } else if (lower.includes('sepang') || lower.includes('malaysia')) {
      window.location.replace('track_map.html?circuit=sepang' + (window.location.search ? '&' + window.location.search.substring(1) : ''));
    }
  }
})();

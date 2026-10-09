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

  // 3. Check hash
  if (!targetCircuit && window.location.hash) {
    targetCircuit = window.location.hash.toLowerCase().replace('#', '');
  }

  // 4. Auto-redirect if circuit specified
  if (targetCircuit) {
    const spinner = document.getElementById('loadingSpinner');
    const desc = document.getElementById('statusDesc');
    if (spinner) spinner.style.display = 'block';
    if (desc) desc.innerText = 'Caricamento tracciato in corso...';
    
    const query = window.location.search ? window.location.search : ('?circuit=' + encodeURIComponent(targetCircuit));
    window.location.replace('track_map.html' + query);
  }
})();

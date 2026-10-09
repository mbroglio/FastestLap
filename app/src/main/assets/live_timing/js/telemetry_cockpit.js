/**
 * FastestLap — telemetry_cockpit.js
 * Modulo Rendering Cockpit Pilota, Gauge Telemetria e Confronto Testa a Testa
 */

function buildLedBars() {
  ['c1LedBar', 'c2LedBar'].forEach(id => {
    const bar = document.getElementById(id);
    if (!bar) return;
    bar.innerHTML = '';
    for (let i = 0; i < 15; i++) {
      const led = document.createElement('div');
      led.className = 'shift-led';
      bar.appendChild(led);
    }
  });
}

function populateDriverDropdowns() {
  const keys = Object.keys(DRIVERS);
  const opts = keys.map(k => {
    const d = DRIVERS[k];
    return '<option value="' + k + '">#' + d.number + ' ' + d.code + ' - ' + d.firstName + ' ' + d.lastName + ' (' + d.team + ')</option>';
  }).join('');

  const s1 = document.getElementById('drv1Select');
  const s2 = document.getElementById('drv2Select');
  if (s1) s1.innerHTML = opts;
  if (s2) s2.innerHTML = opts;
  if (s1) s1.value = selectedDriver1;
  if (s2) s2.value = selectedDriver2;
}

function updateCockpit(idx, drvKey) {
  const drv = DRIVERS[drvKey];
  if (!drv) return;

  const state = getDriverVisualState(drvKey, currentSecond);
  const tireInfo = getDriverTireInfo(drvKey, currentSecond);
  const p = 'c' + idx;

  // Dati Anagrafici Pilota
  const dot = document.getElementById(p + 'ColorDot');
  if (dot) dot.style.background = drv.color;
  const title = document.getElementById(p + 'Title');
  if (title) title.textContent = '#' + drv.number + ' ' + drv.code + ' - ' + drv.firstName + ' ' + drv.lastName;
  const team = document.getElementById(p + 'Team');
  if (team) team.textContent = drv.team + ' [' + tireInfo.compound + ']';

  // Velocità e Marcia
  const spdEl = document.getElementById(p + 'Speed');
  if (spdEl) spdEl.textContent = Math.round(state.speed);
  const gearEl = document.getElementById(p + 'Gear');
  if (gearEl) gearEl.textContent = state.isRetired ? 'DNF' : (state.gear === 0 ? 'N' : state.gear);

  // Giri Motore (RPM)
  const rpmEl = document.getElementById(p + 'Rpm');
  if (rpmEl) rpmEl.textContent = Math.round(state.rpm).toLocaleString() + ' RPM';

  // Barra LED contagiri (Shift LEDs)
  const leds = document.querySelectorAll('#' + p + 'LedBar .shift-led');
  const rpmFrac = Math.max(0, Math.min(1, (state.rpm - 9500) / 3000));
  const activeLeds = Math.round(rpmFrac * leds.length);
  leds.forEach((led, i) => {
    led.className = 'shift-led';
    if (i < activeLeds) {
      if (i < 5) led.classList.add('green');
      else if (i < 10) led.classList.add('yellow');
      else if (i < 13) led.classList.add('red');
      else led.classList.add('blue');
    }
  });

  // Acceleratore e Freno
  const thrBar = document.getElementById(p + 'ThrBar');
  const thrVal = document.getElementById(p + 'ThrVal');
  if (thrBar) thrBar.style.width = Math.round(state.throttle || 75) + '%';
  if (thrVal) thrVal.textContent = Math.round(state.throttle || 75) + '%';

  const brkBar = document.getElementById(p + 'BrkBar');
  const brkVal = document.getElementById(p + 'BrkVal');
  if (brkBar) brkBar.style.width = Math.round(state.brake || 0) + '%';
  if (brkVal) brkVal.textContent = Math.round(state.brake || 0) + '%';

  // Configurazione Aerodinamica Attiva (Regolamento FIA 2026 X-Mode / Z-Mode)
  const aeroBadge = document.getElementById(p + 'AeroBadge');
  if (aeroBadge) {
    if (state.drs > 0 && state.speed > 250) {
      aeroBadge.textContent = 'X-MODE (LOW DRAG)';
      aeroBadge.className = 'aero-mode-badge aero-xmode';
    } else {
      aeroBadge.textContent = 'Z-MODE (HIGH DOWNFORCE)';
      aeroBadge.className = 'aero-mode-badge aero-zmode';
    }
  }

  const curLap = typeof getDriverLap === 'function' ? getDriverLap(drvKey, currentSecond) : 1;
  const isQualiOrPractice = (typeof sessionType !== 'undefined' && (sessionType === 'practice' || sessionType === 'qualifying'));
  const batteryBox = document.getElementById(p + 'BatteryBox');
  const boostBox = document.getElementById(p + 'BoostBox');
  const sectorsBox = document.getElementById(p + 'SectorsBox');

  if (isQualiOrPractice) {
    if (batteryBox) batteryBox.style.display = 'none';
    if (boostBox) boostBox.style.display = 'none';
    if (sectorsBox) {
      sectorsBox.style.display = 'flex';
      const sessionBests = typeof getSessionBests === 'function' ? getSessionBests(currentSecond) : null;
      const secTimes = typeof getDriverSectorTimes === 'function'
        ? getDriverSectorTimes(drvKey, currentSecond, sessionBests)
        : { s1: '-', s1Class: 'sector-gray', s2: '-', s2Class: 'sector-gray', s3: '-', s3Class: 'sector-gray' };

      const s1Val = document.getElementById(p + 'S1Val');
      const s2Val = document.getElementById(p + 'S2Val');
      const s3Val = document.getElementById(p + 'S3Val');
      const s1Cell = document.getElementById(p + 'S1Cell');
      const s2Cell = document.getElementById(p + 'S2Cell');
      const s3Cell = document.getElementById(p + 'S3Cell');

      if (s1Val) s1Val.textContent = secTimes.s1;
      if (s2Val) s2Val.textContent = secTimes.s2;
      if (s3Val) s3Val.textContent = secTimes.s3;

      if (s1Cell) s1Cell.className = 'sector-cell ' + (secTimes.s1Class || 'sector-gray');
      if (s2Cell) s2Cell.className = 'sector-cell ' + (secTimes.s2Class || 'sector-gray');
      if (s3Cell) s3Cell.className = 'sector-cell ' + (secTimes.s3Class || 'sector-gray');
    }
  } else {
    if (sectorsBox) sectorsBox.style.display = 'none';
    if (batteryBox) batteryBox.style.display = 'flex';
    if (boostBox) boostBox.style.display = 'flex';

    // Batteria ERS / Stato di Carica (SoC%)
    const socVal = document.getElementById(p + 'SocVal');
    const socBar = document.getElementById(p + 'SocBar');
    const estSoc = Math.max(25, Math.min(98, 92 - ((curLap * 3) % 45) + ((state.brake || 0) > 50 ? 12 : -5)));
    if (socVal) socVal.textContent = Math.round(estSoc) + '%';
    if (socBar) socBar.style.width = Math.round(estSoc) + '%';

    // Manual Override Mode (MOM / Boost 2026)
    const boostBadge = document.getElementById(p + 'BoostBadge');
    if (boostBadge) {
      const gap = getDriverGap(drvKey, currentSecond);
      if (gap !== '-' && gap !== 'LEADER' && estSoc > 35 && (state.throttle || 75) > 90) {
        boostBadge.textContent = 'BOOST ATTIVO (MOM)';
        boostBadge.className = 'boost-pill boost-active';
      } else if (estSoc > 40) {
        boostBadge.textContent = 'DISPONIBILE';
        boostBadge.className = 'boost-pill boost-avail';
      } else {
        boostBadge.textContent = 'RICARICA ERS';
        boostBadge.className = 'boost-pill boost-standby';
      }
    }
  }

  // Posizione in pista
  const locEl = document.getElementById(p + 'LocText');
  if (locEl) locEl.textContent = '📍 Settore Ufficiale ' + ((curLap % 3) + 1);
}

function onDriverSelectChange(idx, val) {
  if (idx === 1) selectedDriver1 = val;
  if (idx === 2) selectedDriver2 = val;
  updateCockpit(idx, val);
  if (typeof updateTimingTables === 'function') updateTimingTables();
  if (typeof drawTrack === 'function') drawTrack();
}

function swapSelectedDrivers() {
  const tmp = selectedDriver1;
  selectedDriver1 = selectedDriver2;
  selectedDriver2 = tmp;
  const s1 = document.getElementById('drv1Select');
  const s2 = document.getElementById('drv2Select');
  if (s1) s1.value = selectedDriver1;
  if (s2) s2.value = selectedDriver2;
  updateCockpit(1, selectedDriver1);
  updateCockpit(2, selectedDriver2);
  if (typeof updateTimingTables === 'function') updateTimingTables();
  if (typeof drawTrack === 'function') drawTrack();
}

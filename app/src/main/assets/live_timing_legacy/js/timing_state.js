/**
 * FastestLap — timing_state.js
 * Modulo Stato Globale, Roster Piloti 2026 e Funzioni di Calcolo Telemetria
 */

// Griglia Ufficiale Campionato Formula 1 2026 (22 Piloti, 11 Scuderie)
const GRID_2026_DRIVERS = {
  '1':  { code: 'NOR', number: 1,  firstName: 'Lando',     lastName: 'Norris',       team: 'McLaren Mastercard F1 Team',      color: '#FF8000' },
  '3':  { code: 'VER', number: 3,  firstName: 'Max',       lastName: 'Verstappen',   team: 'Oracle Red Bull Racing',          color: '#3671C6' },
  '16': { code: 'LEC', number: 16, firstName: 'Charles',   lastName: 'Leclerc',      team: 'Scuderia Ferrari HP',             color: '#E80020' },
  '81': { code: 'PIA', number: 81, firstName: 'Oscar',     lastName: 'Piastri',      team: 'McLaren Mastercard F1 Team',      color: '#FF8000' },
  '63': { code: 'RUS', number: 63, firstName: 'George',    lastName: 'Russell',      team: 'Mercedes-AMG Petronas F1 Team',   color: '#27F4D2' },
  '12': { code: 'ANT', number: 12, firstName: 'Kimi',      lastName: 'Antonelli',    team: 'Mercedes-AMG Petronas F1 Team',   color: '#27F4D2' },
  '44': { code: 'HAM', number: 44, firstName: 'Lewis',     lastName: 'Hamilton',     team: 'Scuderia Ferrari HP',             color: '#E80020' },
  '6':  { code: 'HAD', number: 6,  firstName: 'Isack',     lastName: 'Hadjar',       team: 'Oracle Red Bull Racing',          color: '#3671C6' },
  '55': { code: 'SAI', number: 55, firstName: 'Carlos',    lastName: 'Sainz Jr.',    team: 'Atlassian Williams F1 Team',      color: '#64C4FF' },
  '14': { code: 'ALO', number: 14, firstName: 'Fernando',  lastName: 'Alonso',       team: 'Aston Martin Aramco F1 Team',     color: '#229971' },
  '23': { code: 'ALB', number: 23, firstName: 'Alexander', lastName: 'Albon',        team: 'Atlassian Williams F1 Team',      color: '#64C4FF' },
  '18': { code: 'STR', number: 18, firstName: 'Lance',     lastName: 'Stroll',       team: 'Aston Martin Aramco F1 Team',     color: '#229971' },
  '10': { code: 'GAS', number: 10, firstName: 'Pierre',    lastName: 'Gasly',        team: 'BWT Alpine F1 Team',              color: '#FF87BC' },
  '43': { code: 'COL', number: 43, firstName: 'Franco',    lastName: 'Colapinto',    team: 'BWT Alpine F1 Team',              color: '#FF87BC' },
  '30': { code: 'LAW', number: 30, firstName: 'Liam',      lastName: 'Lawson',       team: 'Visa Cash App Racing Bulls',      color: '#6692FF' },
  '41': { code: 'LIN', number: 41, firstName: 'Arvid',     lastName: 'Lindblad',     team: 'Visa Cash App Racing Bulls',      color: '#6692FF' },
  '27': { code: 'HUL', number: 27, firstName: 'Nico',      lastName: 'Hülkenberg',   team: 'Audi Revolut F1 Team',            color: '#F50537' },
  '5':  { code: 'BOR', number: 5,  firstName: 'Gabriel',   lastName: 'Bortoleto',    team: 'Audi Revolut F1 Team',            color: '#F50537' },
  '31': { code: 'OCO', number: 31, firstName: 'Esteban',   lastName: 'Ocon',         team: 'TGR Haas F1 Team',                color: '#B6BABD' },
  '87': { code: 'BEA', number: 87, firstName: 'Oliver',    lastName: 'Bearman',      team: 'TGR Haas F1 Team',                color: '#B6BABD' },
  '11': { code: 'PER', number: 11, firstName: 'Sergio',    lastName: 'Pérez',        team: 'Cadillac Formula 1 Team',         color: '#DFB14E' },
  '77': { code: 'BOT', number: 77, firstName: 'Valtteri',  lastName: 'Bottas',       team: 'Cadillac Formula 1 Team',         color: '#DFB14E' }
};

// Variabili di stato della sessione live
let DRIVERS = { ...GRID_2026_DRIVERS };
let GRID_ORDER = Object.keys(DRIVERS);
let DRIVER_LAPS = {};
let DRIVER_STINTS = {};
let DRIVER_KEYFRAMES = {};
let PIT_STOPS = [];
let RETIREMENTS = [];
let INCIDENTS = [];
let RACE_EVENTS = [];
let NODES = [];
let PIT_LANE_NODES = [];
let PIT_LANE_DISTS = [0];
let PIT_LANE_TOTAL_DIST = 1;

let SVG_WIDTH = 1000;
let SVG_HEIGHT = 800;
let LAP_DURATION = 91.5;
let TOTAL_LAPS = null;
let SESSION_DURATION = 7200;
let RACE_FINISH_SEC = 7200;
let DRIVER_TELEMETRY = {};

let currentCircuit = 'generic';
let currentSecond = 0.0;
let simSpeedMultiplier = 1.0;
let isPlaying = true;
let lastAnimFrameTimestamp = null;
let lastUiUpdateTime = 0;

let focusedDriver = '16';
let selectedDriver1 = '16';
let selectedDriver2 = '1';
let isMapFollowingDriver = false;
let currentMainView = 'view-standings';
let currentSideTab = 'side-standings';
let trackBgImage = null;
let isGenericRealMode = true;
let expandedDriverKey = null;

let circuitTimeZone = '';

let sessionType = 'race';
let sessionPart = '';

function normalizeSessionType(type, part) {
  if (!type) return 'race';
  const t = type.toLowerCase().trim();
  const p = (part || '').toUpperCase().trim();

  // Prove libere
  if (t.includes('practice') || t.includes('prova') || t.includes('prove') || t.includes('libera') || t.includes('libere') || t.startsWith('fp')) {
    return 'practice';
  }
  // Qualifiche standard o Qualifiche Sprint / Shootout
  if (t.includes('qualif') || t.includes('shootout') || t === 'sq' || p.startsWith('SQ') || t.includes('sprint_qualifying') || t.includes('sprint qualifying')) {
    return 'qualifying';
  }
  // Gara Sprint (trattata come gara)
  if (t.includes('sprint')) {
    return 'sprint';
  }
  // Gara principale
  return 'race';
}

function isSprintQualifying(type, part) {
  const t = (type || '').toLowerCase().trim();
  const p = (part || '').toUpperCase().trim();
  return (t.includes('sprint') && (t.includes('qualif') || t.includes('shootout')))
    || t.includes('shootout')
    || t === 'sq'
    || p.startsWith('SQ');
}

function formatLapTime(sec) {
  if (!sec || isNaN(sec) || sec === Infinity || sec <= 0) return '-';
  const m = Math.floor(sec / 60);
  const s = (sec % 60).toFixed(3);
  return (m > 0 ? (m + ':' + (s < 10 ? '0' : '') + s) : s);
}

function formatSimClock(sec) {
  if (circuitTimeZone) {
    try {
      const now = new Date();
      const formatted = new Intl.DateTimeFormat('it-IT', {
        timeZone: circuitTimeZone,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      }).format(now);
      return formatted;
    } catch (e) {}
  }
  if (window.FastestLapBridge && typeof window.FastestLapBridge.getCircuitLocalTime === 'function') {
    try {
      const bridgeTime = window.FastestLapBridge.getCircuitLocalTime();
      if (bridgeTime && bridgeTime.trim().length > 0) return bridgeTime.trim();
    } catch (e) {}
  }
  const now = new Date();
  const h = String(now.getHours()).padStart(2, '0');
  const m = String(now.getMinutes()).padStart(2, '0');
  const s = String(now.getSeconds()).padStart(2, '0');
  return h + ':' + m + ':' + s;
}

function getDriverLap(drvKey, t) {
  const laps = DRIVER_LAPS[drvKey];
  if (!laps || laps.length === 0) return 1;
  for (let i = laps.length - 1; i >= 0; i--) {
    if (t >= laps[i].startSec) return laps[i].lap;
  }
  return 1;
}

function getDriverGap(drvKey, t) {
  if (typeof DRIVER_TELEMETRY !== 'undefined' && DRIVER_TELEMETRY[drvKey] && DRIVER_TELEMETRY[drvKey].gapLeader) {
    return DRIVER_TELEMETRY[drvKey].gapLeader;
  }
  const leaderKey = (GRID_ORDER && GRID_ORDER[0]) ? GRID_ORDER[0] : '1';
  if (drvKey === leaderKey) return 'LEADER';
  return '-';
}

function getDriverTireInfo(drvKey, t) {
  const stints = DRIVER_STINTS[drvKey];
  if (!stints || stints.length === 0) {
    return { compound: 'MED', age: 1, color: '#FCD116' };
  }
  const curLap = getDriverLap(drvKey, t);
  let activeStint = stints[0];
  for (let s of stints) {
    if (curLap >= s.lapStart && (!s.lapEnd || curLap <= s.lapEnd)) {
      activeStint = s;
      break;
    }
  }
  const code = (activeStint.code || activeStint.compound || 'MED').toUpperCase();
  let col = '#FCD116';
  if (code.includes('SOFT') || code === 'SFT' || code === 'S') col = '#E10600';
  else if (code.includes('HARD') || code === 'HRD' || code === 'H') col = '#FFFFFF';
  else if (code.includes('INTER') || code === 'INT' || code === 'I') col = '#43B02A';
  else if (code.includes('WET') || code === 'W') col = '#0072CE';

  const age = Math.max(1, curLap - activeStint.lapStart + 1);
  return { compound: code.substring(0, 3), age: age, color: col };
}

function getSessionBests(t) {
  let bestLap = Infinity, bestS1 = Infinity, bestS2 = Infinity, bestS3 = Infinity;
  Object.keys(DRIVERS).forEach(k => {
    const laps = DRIVER_LAPS[k] || [];
    laps.forEach(l => {
      if (l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50) {
        if (l.dur < bestLap) bestLap = l.dur;
        if (l.s1 && l.s1 < bestS1) bestS1 = l.s1;
        if (l.s2 && l.s2 < bestS2) bestS2 = l.s2;
        if (l.s3 && l.s3 < bestS3) bestS3 = l.s3;
      }
    });
  });
  return { bestLap, bestS1, bestS2, bestS3 };
}

function getDriverSectorTimes(drvKey, t, sessionBests) {
  const laps = DRIVER_LAPS[drvKey] || [];
  const completed = laps.filter(l => l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50);
  if (completed.length === 0) {
    return {
      s1: '-', s1Badge: 'badge-gray', s1Class: 'sector-gray',
      s2: '-', s2Badge: 'badge-gray', s2Class: 'sector-gray',
      s3: '-', s3Badge: 'badge-gray', s3Class: 'sector-gray'
    };
  }

  let pbS1 = Infinity, pbS2 = Infinity, pbS3 = Infinity;
  completed.forEach(l => {
    if (l.s1 && l.s1 < pbS1) pbS1 = l.s1;
    if (l.s2 && l.s2 < pbS2) pbS2 = l.s2;
    if (l.s3 && l.s3 < pbS3) pbS3 = l.s3;
  });

  const last = completed[completed.length - 1];

  function evalSector(val, sessionBest, driverPb) {
    if (!val || val <= 0) {
      return { str: '-', badge: 'badge-gray', sectorClass: 'sector-gray' };
    }
    const valStr = Number(val).toFixed(3);
    if (sessionBest && sessionBest < Infinity && val <= sessionBest + 0.005) {
      return { str: valStr, badge: 'badge-purple', sectorClass: 'sector-purple' };
    }
    if (driverPb && driverPb < Infinity && val <= driverPb + 0.005) {
      return { str: valStr, badge: 'badge-green', sectorClass: 'sector-green' };
    }
    return { str: valStr, badge: 'badge-yellow', sectorClass: 'sector-yellow' };
  }

  const s1Res = evalSector(last.s1, sessionBests ? sessionBests.bestS1 : Infinity, pbS1);
  const s2Res = evalSector(last.s2, sessionBests ? sessionBests.bestS2 : Infinity, pbS2);
  const s3Res = evalSector(last.s3, sessionBests ? sessionBests.bestS3 : Infinity, pbS3);

  return {
    s1: s1Res.str, s1Badge: s1Res.badge, s1Class: s1Res.sectorClass,
    s2: s2Res.str, s2Badge: s2Res.badge, s2Class: s2Res.sectorClass,
    s3: s3Res.str, s3Badge: s3Res.badge, s3Class: s3Res.sectorClass
  };
}

function getDriverTimingStats(drvKey, t, sessionBests) {
  const laps = DRIVER_LAPS[drvKey] || [];
  const completed = laps.filter(l => l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50);
  if (completed.length === 0) {
    return {
      personalBest: Infinity,
      bestLapStr: '-',
      bestLapBadge: 'badge-gray',
      lastLapStr: '-',
      lastLapBadge: 'badge-gray',
      lapsCount: 0
    };
  }
  let pBest = Infinity;
  completed.forEach(l => { if (l.dur < pBest) pBest = l.dur; });
  const last = completed[completed.length - 1];

  let bestBadge = 'badge-green';
  if (sessionBests && pBest <= sessionBests.bestLap + 0.005) {
    bestBadge = 'badge-purple';
  }
  let lastBadge = 'badge-yellow';
  if (sessionBests && last.dur <= sessionBests.bestLap + 0.005) {
    lastBadge = 'badge-purple';
  } else if (last.dur <= pBest + 0.005) {
    lastBadge = 'badge-green';
  }

  return {
    personalBest: pBest,
    bestLapStr: formatLapTime(pBest),
    bestLapBadge: bestBadge,
    lastLapStr: formatLapTime(last.dur),
    lastLapBadge: lastBadge,
    lapsCount: completed.length
  };
}

function getDriverPitCount(drvKey, t) {
  // 1. Lettura dal flusso telemetrico se specificato
  const tel = DRIVER_TELEMETRY[drvKey];
  if (tel && tel.pitCount !== undefined) {
    return parseInt(tel.pitCount, 10) || 0;
  }
  // 2. Lettura dagli eventi pit stop effettivi registrati
  if (Array.isArray(PIT_STOPS) && PIT_STOPS.length > 0) {
    return PIT_STOPS.filter(p => {
      const match = String(p.driver || p.driverNumber || p.driver_number) === String(drvKey);
      return match && (!p.endSec || t >= p.endSec);
    }).length;
  }
  // 3. Lettura dagli stint registrati
  const stints = DRIVER_STINTS[drvKey];
  if (Array.isArray(stints) && stints.length > 1) {
    const curLap = getDriverLap(drvKey, t);
    return stints.filter(s => s.stint > 1 && curLap >= s.lapStart).length;
  }
  return 0;
}

function getDriverVisualState(drvKey, t) {
  const drvIdx = GRID_ORDER.indexOf(drvKey);
  const tel = DRIVER_TELEMETRY[drvKey];

  // Recupero dello stato esclusivamente da telemetria, eventi o pit stop registrati
  let inPit = false;
  let isRetired = false;
  let isOutLap = false;

  if (tel) {
    if (tel.inPit !== undefined) inPit = Boolean(tel.inPit);
    if (tel.isRetired !== undefined) isRetired = Boolean(tel.isRetired);
    if (tel.isOutLap !== undefined) isOutLap = Boolean(tel.isOutLap);
  }

  if (!inPit && Array.isArray(PIT_STOPS) && PIT_STOPS.length > 0) {
    inPit = PIT_STOPS.some(p => {
      const match = String(p.driver || p.driverNumber || p.driver_number) === String(drvKey);
      return match && p.startSec !== undefined && t >= p.startSec && (p.endSec === undefined || t <= p.endSec);
    });
  }

  if (!isRetired && (RETIREMENTS[drvKey] || (Array.isArray(RETIREMENTS) && RETIREMENTS.includes(drvKey)))) {
    isRetired = true;
  }

  const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType;

  // Valori telemetrici prioritari dal flusso; fallback calcolato in base alla sessione
  const speed = (tel && tel.speed !== undefined) ? tel.speed : (isRetired ? 0 : (inPit ? 80 : (240 + ((drvIdx * 7) % 80))));
  const gear = (tel && tel.gear !== undefined) ? tel.gear : (isRetired ? 0 : (inPit ? 2 : 6));
  const rpm = (tel && tel.rpm !== undefined) ? tel.rpm : (isRetired ? 0 : (inPit ? 4200 : (11400 + ((drvIdx * 110) % 1100))));
  const throttle = (tel && tel.throttle !== undefined) ? tel.throttle : (isRetired ? 0 : (inPit ? 35 : 85));
  const brake = (tel && tel.brake !== undefined) ? tel.brake : (isRetired ? 0 : (inPit ? 10 : 0));
  const drs = (tel && tel.drs !== undefined) ? Boolean(tel.drs) : (sType !== 'practice' && !inPit && !isRetired && drvIdx < 10);
  const batterySoc = (tel && tel.batterySoc !== undefined) ? tel.batterySoc : null;
  const boostActive = (tel && tel.boostActive !== undefined) ? Boolean(tel.boostActive) : null;

  return {
    isRetired: isRetired,
    inPit: inPit,
    isOutLapMerge: isOutLap,
    speed: speed,
    gear: gear,
    rpm: rpm,
    throttle: throttle,
    brake: brake,
    drs: drs,
    batterySoc: batterySoc,
    boostActive: boostActive
  };
}

function isSafetyCarActive(t) {
  return false;
}

function isVirtualSafetyCarActive(t) {
  return false;
}

function buildGenericSessionLaps(sType) {
  DRIVER_LAPS = {};
  DRIVER_STINTS = {};
  const driverKeys = GRID_ORDER;
  const baseTime = 89.450;

  const compounds = ['MED', 'HRD', 'SFT'];
  const fullNames = { SFT: 'SOFT', MED: 'MEDIUM', HRD: 'HARD' };

  const normType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sType, sessionPart) : sType;

  // Se la sessione non è ancora iniziata o tempo trascorso è 0, nessun giro completato fittizio
  if (currentSecond <= 0) {
    driverKeys.forEach((key, idx) => {
      DRIVER_LAPS[key] = [];
      const cmp = compounds[idx % 3];
      DRIVER_STINTS[key] = [{
        stint: 1,
        compound: fullNames[cmp],
        code: cmp,
        lapStart: 1,
        lapEnd: 1
      }];
    });
    return;
  }

  if (normType === 'practice') {
    driverKeys.forEach((key, idx) => {
      const laps = [];
      const numLaps = 12 + (idx % 8);
      const pb = baseTime + (idx * 0.14) + (Math.sin(idx * 2) * 0.10);
      const pbS1 = parseFloat((pb * 0.32).toFixed(3));
      const pbS2 = parseFloat((pb * 0.39).toFixed(3));
      const pbS3 = parseFloat((pb - pbS1 - pbS2).toFixed(3));

      let curTime = 120 + (idx * 35);
      for (let l = 1; l <= numLaps; l++) {
        if (curTime > currentSecond) break;
        let dur, s1, s2, s3;
        if (l === 4) {
          dur = parseFloat(pb.toFixed(3));
          s1 = pbS1; s2 = pbS2; s3 = pbS3;
        } else {
          const delta = 0.35 + (l * 0.12) + ((idx % 3) * 0.15);
          dur = parseFloat((pb + delta).toFixed(3));
          s1 = parseFloat((pbS1 + delta * 0.3).toFixed(3));
          s2 = parseFloat((pbS2 + delta * 0.45).toFixed(3));
          s3 = parseFloat((dur - s1 - s2).toFixed(3));
        }
        laps.push({
          lap: l,
          startSec: curTime,
          dur: dur,
          s1: s1,
          s2: s2,
          s3: s3,
          isPit: (l === numLaps)
        });
        curTime += dur + (l % 4 === 0 ? 300 : 25);
      }
      DRIVER_LAPS[key] = laps;

      const cmp = compounds[idx % 3];
      DRIVER_STINTS[key] = [{
        stint: 1,
        compound: fullNames[cmp],
        code: cmp,
        lapStart: 1,
        lapEnd: Math.max(1, laps.length)
      }];
    });
  } else if (normType === 'qualifying') {
    driverKeys.forEach((key, idx) => {
      const laps = [];
      const numRuns = (idx < 10) ? 6 : ((idx < 15) ? 4 : 2);
      const pb = baseTime - 1.200 + (idx * 0.11);
      const pbS1 = parseFloat((pb * 0.32).toFixed(3));
      const pbS2 = parseFloat((pb * 0.39).toFixed(3));
      const pbS3 = parseFloat((pb - pbS1 - pbS2).toFixed(3));

      let curTime = 90 + (idx * 20);
      for (let r = 1; r <= numRuns; r++) {
        if (curTime > currentSecond) break;
        const delta = (r === numRuns ? 0.0 : (0.25 * (numRuns - r)));
        const dur = parseFloat((pb + delta).toFixed(3));
        const s1 = parseFloat((pbS1 + delta * 0.3).toFixed(3));
        const s2 = parseFloat((pbS2 + delta * 0.45).toFixed(3));
        const s3 = parseFloat((dur - s1 - s2).toFixed(3));

        laps.push({
          lap: r,
          startSec: curTime,
          dur: dur,
          s1: s1,
          s2: s2,
          s3: s3,
          isPit: true
        });
        curTime += dur + 420;
      }
      DRIVER_LAPS[key] = laps;

      DRIVER_STINTS[key] = [{
        stint: 1,
        compound: 'SOFT',
        code: 'SFT',
        lapStart: 1,
        lapEnd: Math.max(1, laps.length)
      }];
    });
  } else {
    // Gara o Sprint: nessun giro totale fittizio né soste/finestre hardcodate
    const maxCompletedLaps = Math.max(0, Math.floor(currentSecond / (LAP_DURATION || 90.0)));

    driverKeys.forEach((key, idx) => {
      const laps = [];
      const pb = baseTime + (idx * 0.18);
      const pbS1 = parseFloat((pb * 0.32).toFixed(3));
      const pbS2 = parseFloat((pb * 0.39).toFixed(3));
      const pbS3 = parseFloat((pb - pbS1 - pbS2).toFixed(3));

      let curTime = 0;
      for (let l = 1; l <= maxCompletedLaps; l++) {
        const delta = 0.4 + (l * 0.04) + ((idx % 3) * 0.08);
        const dur = parseFloat((pb + delta).toFixed(3));
        const s1 = parseFloat((pbS1 + delta * 0.3).toFixed(3));
        const s2 = parseFloat((pbS2 + delta * 0.45).toFixed(3));
        const s3 = parseFloat((dur - s1 - s2).toFixed(3));

        laps.push({
          lap: l,
          startSec: curTime,
          dur: dur,
          s1: s1,
          s2: s2,
          s3: s3,
          isPit: false
        });
        curTime += dur;
      }
      DRIVER_LAPS[key] = laps;

      const cmp = compounds[idx % 2];
      DRIVER_STINTS[key] = [
        { stint: 1, compound: fullNames[cmp], code: cmp, lapStart: 1, lapEnd: Math.max(1, maxCompletedLaps) }
      ];
    });
  }
}

// Ingestion API per aggiornamenti di telemetria e pit stop da flussi esterni/Android Bridge
window.updateDriverTelemetry = function(data) {
  if (!data) return;
  try {
    const list = Array.isArray(data) ? data : (typeof data === 'string' ? JSON.parse(data) : [data]);
    list.forEach(item => {
      const key = String(item.driverNumber || item.driver_number || item.key || item.driver || '');
      if (key) {
        DRIVER_TELEMETRY[key] = { ...(DRIVER_TELEMETRY[key] || {}), ...item };
        if (item.inPit !== undefined || item.in_pit !== undefined) {
          DRIVER_TELEMETRY[key].inPit = Boolean(item.inPit !== undefined ? item.inPit : item.in_pit);
        }
        if (item.isRetired !== undefined || item.retired !== undefined) {
          const ret = Boolean(item.isRetired !== undefined ? item.isRetired : item.retired);
          DRIVER_TELEMETRY[key].isRetired = ret;
          if (ret) RETIREMENTS[key] = true;
        }
      }
    });
  } catch (e) {
    console.error('Error updating driver telemetry:', e);
  }
};

window.updatePitStops = function(data) {
  if (!data) return;
  try {
    const list = Array.isArray(data) ? data : (typeof data === 'string' ? JSON.parse(data) : [data]);
    PIT_STOPS = list;
  } catch (e) {
    console.error('Error updating pit stops:', e);
  }
};

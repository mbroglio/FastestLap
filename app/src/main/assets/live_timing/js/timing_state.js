/**
 * FastestLap — timing_state.js
 * Modulo Stato Globale, Roster Piloti 2026 e Funzioni di Calcolo Telemetria
 */

// Griglia Ufficiale Campionato Formula 1 (20 Piloti Ufficiali 2024 / Griglia Attuale)
const GRID_OFFICIAL_DRIVERS = {
  '1':  { code: 'VER', number: 1,  firstName: 'Max',       lastName: 'Verstappen',   team: 'Red Bull Racing',                 color: '#3671C6' },
  '11': { code: 'PER', number: 11, firstName: 'Sergio',    lastName: 'Pérez',        team: 'Red Bull Racing',                 color: '#3671C6' },
  '16': { code: 'LEC', number: 16, firstName: 'Charles',   lastName: 'Leclerc',      team: 'Ferrari',                         color: '#E80020' },
  '55': { code: 'SAI', number: 55, firstName: 'Carlos',    lastName: 'Sainz Jr.',    team: 'Ferrari',                         color: '#E80020' },
  '4':  { code: 'NOR', number: 4,  firstName: 'Lando',     lastName: 'Norris',       team: 'McLaren',                         color: '#FF8000' },
  '81': { code: 'PIA', number: 81, firstName: 'Oscar',     lastName: 'Piastri',      team: 'McLaren',                         color: '#FF8000' },
  '44': { code: 'HAM', number: 44, firstName: 'Lewis',     lastName: 'Hamilton',     team: 'Mercedes',                        color: '#27F4D2' },
  '63': { code: 'RUS', number: 63, firstName: 'George',    lastName: 'Russell',      team: 'Mercedes',                        color: '#27F4D2' },
  '14': { code: 'ALO', number: 14, firstName: 'Fernando',  lastName: 'Alonso',       team: 'Aston Martin',                    color: '#229971' },
  '18': { code: 'STR', number: 18, firstName: 'Lance',     lastName: 'Stroll',       team: 'Aston Martin',                    color: '#229971' },
  '10': { code: 'GAS', number: 10, firstName: 'Pierre',    lastName: 'Gasly',        team: 'Alpine',                          color: '#0093CC' },
  '31': { code: 'OCO', number: 31, firstName: 'Esteban',   lastName: 'Ocon',         team: 'Alpine',                          color: '#0093CC' },
  '23': { code: 'ALB', number: 23, firstName: 'Alexander', lastName: 'Albon',        team: 'Williams',                        color: '#64C4FF' },
  '43': { code: 'COL', number: 43, firstName: 'Franco',    lastName: 'Colapinto',    team: 'Williams',                        color: '#64C4FF' },
  '22': { code: 'TSU', number: 22, firstName: 'Yuki',      lastName: 'Tsunoda',      team: 'RB',                              color: '#6692FF' },
  '30': { code: 'LAW', number: 30, firstName: 'Liam',      lastName: 'Lawson',       team: 'RB',                              color: '#6692FF' },
  '77': { code: 'BOT', number: 77, firstName: 'Valtteri',  lastName: 'Bottas',       team: 'Kick Sauber',                     color: '#52E252' },
  '24': { code: 'ZHO', number: 24, firstName: 'Guanyu',    lastName: 'Zhou',         team: 'Kick Sauber',                     color: '#52E252' },
  '27': { code: 'HUL', number: 27, firstName: 'Nico',      lastName: 'Hülkenberg',   team: 'Haas',                            color: '#B6BABD' },
  '20': { code: 'MAG', number: 20, firstName: 'Kevin',     lastName: 'Magnussen',    team: 'Haas',                            color: '#B6BABD' }
};
const GRID_2026_DRIVERS = GRID_OFFICIAL_DRIVERS;

// Variabili di stato della sessione live
let DRIVERS = { ...GRID_OFFICIAL_DRIVERS };
let GRID_ORDER = Object.keys(DRIVERS);
let DRIVER_LAPS = {};
let DRIVER_STINTS = {};
let DRIVER_KEYFRAMES = {};
let PIT_STOPS = [];
let RETIREMENTS = [];
let INCIDENTS = [];
let RACE_EVENTS = [];
let NODES = [];
let LIVE_DRIVER_POSITIONS = {};
window.LIVE_DRIVER_POSITIONS = LIVE_DRIVER_POSITIONS;
window.NODES = NODES;
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

function buildDefaultTrackNodes(w, h) {
  const cx = (w || 1000) / 2;
  const cy = (h || 800) / 2;
  const rx = (w || 1000) * 0.38;
  const ry = (h || 800) * 0.32;
  const totalSteps = 180;
  const lapDuration = LAP_DURATION || 90.0;
  const nodes = [];

  for (let i = 0; i < totalSteps; i++) {
    const progress = i / totalSteps;
    const t = Number((progress * lapDuration).toFixed(2));
    const theta = progress * 2 * Math.PI;

    // Tracciato grand prix armonico con rettilinei veloci, curvoni e staccate
    const r = 1.0 + 0.22 * Math.sin(theta * 2) - 0.14 * Math.cos(theta * 3) + 0.08 * Math.sin(theta * 5);
    const px = Number((cx + rx * r * Math.cos(theta)).toFixed(2));
    const py = Number((cy + ry * r * Math.sin(theta)).toFixed(2));

    let sector = 1;
    if (progress >= 0.68) sector = 3;
    else if (progress >= 0.30) sector = 2;

    const isBraking = (Math.sin(theta * 3) > 0.65 || Math.cos(theta * 4) > 0.72);
    const isStraight = (Math.sin(theta * 2) < -0.35);

    let speed = 210, gear = 5, throttle = 85, brake = 0, drs = 0, rpm = 11200;
    if (isBraking) {
      speed = 92; gear = 2; throttle = 20; brake = 100; drs = 0; rpm = 9600;
    } else if (isStraight) {
      speed = 328; gear = 8; throttle = 100; brake = 0; drs = (sector === 1 || sector === 3) ? 1 : 0; rpm = 12400;
    } else {
      speed = 185; gear = 4; throttle = 70; brake = 0; drs = 0; rpm = 10800;
    }

    nodes.push([t, px, py, null, null, sector, speed, gear, rpm, throttle, brake, drs, 'Pista']);
  }
  return nodes;
}

function getTrackPointAtTime(sec) {
  if (!NODES || NODES.length === 0) {
    NODES = buildDefaultTrackNodes(SVG_WIDTH, SVG_HEIGHT);
  }
  const dur = LAP_DURATION || 90.0;
  const t = ((sec % dur) + dur) % dur;
  let low = 0, high = NODES.length - 1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (NODES[mid][0] <= t) low = mid + 1;
    else high = mid - 1;
  }
  const i1 = Math.max(0, high);
  const i2 = (i1 + 1) % NODES.length;
  const n1 = NODES[i1];
  const n2 = NODES[i2];

  let dt = n2[0] - n1[0];
  if (dt <= 0) dt += dur;
  let u = 0;
  if (dt > 0) {
    let el = t - n1[0];
    if (el < 0) el += dur;
    u = Math.min(1.0, Math.max(0.0, el / dt));
  }

  return {
    px: Number((n1[1] + (n2[1] - n1[1]) * u).toFixed(2)),
    py: Number((n2[2] !== undefined ? (n1[2] + (n2[2] - n1[2]) * u) : n1[2]).toFixed(2)),
    sector: n1[5] || 1,
    speed: Math.round(n1[6] + (n2[6] - n1[6]) * u),
    gear: n1[7] || 4,
    rpm: Math.round(n1[8] + (n2[8] - n1[8]) * u),
    throttle: Math.round(n1[9] + (n2[9] - n1[9]) * u),
    brake: Math.round(n1[10] + (n2[10] - n1[10]) * u),
    drs: n1[11] || 0,
    location: n1[12] || 'Pista'
  };
}

function getDriverTrackSec(drvKey, t) {
  const drvIdx = GRID_ORDER.indexOf(drvKey);
  const dur = LAP_DURATION || 90.0;
  // Distacco realistico sfalsato tra i 22 piloti lungo il nastro d'asfalto
  const gapOffset = (drvIdx >= 0 ? drvIdx : 0) * (dur / 22.0) * 0.94;
  return ((t - gapOffset) % dur + dur) % dur;
}

function getDriverVisualState(drvKey, t) {
  const drvIdx = GRID_ORDER.indexOf(drvKey);
  const tel = DRIVER_TELEMETRY[drvKey];

  // Recupero dello stato esclusivamente da telemetria, eventi o pit stop registrati
  let inPit = false;
  let isRetired = false;
  let isOutLap = false;

  const laps = DRIVER_LAPS[drvKey] || [];
  if (laps.length === 0 && !window.IS_SYNTHETIC_SIMULATION) {
    inPit = true;
  }

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

  // 1. Coordinate di posizionamento: live streaming SignalR prioritario
  let px = undefined;
  let py = undefined;
  const livePos = window.LIVE_DRIVER_POSITIONS && window.LIVE_DRIVER_POSITIONS[drvKey];
  if (livePos) {
    if (livePos.status) {
      const st = String(livePos.status).toLowerCase();
      if (st.includes('pit') || st.includes('off')) inPit = true;
      else if (st.includes('track')) inPit = false;
    }
    if (livePos.x !== undefined && livePos.y !== undefined) {
      px = livePos.curX !== undefined ? livePos.curX : livePos.x;
      py = livePos.curY !== undefined ? livePos.curY : livePos.y;
    }
  }

  // 2. Coordinate e telemetria continua interpolata dal modello di pista
  const trackSec = getDriverTrackSec(drvKey, t);
  const pt = getTrackPointAtTime(trackSec);

  if (px === undefined || py === undefined) {
    px = pt.px;
    py = pt.py;
  }

  const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType;

  // 3. Valori telemetrici dinamici per Cockpit
  const speed = (tel && tel.speed !== undefined) ? tel.speed : (isRetired ? 0 : (inPit ? 80 : pt.speed));
  const gear = (tel && tel.gear !== undefined) ? tel.gear : (isRetired ? 0 : (inPit ? 2 : pt.gear));
  const rpm = (tel && tel.rpm !== undefined) ? tel.rpm : (isRetired ? 0 : (inPit ? 4200 : pt.rpm));
  const throttle = (tel && tel.throttle !== undefined) ? tel.throttle : (isRetired ? 0 : (inPit ? 35 : pt.throttle));
  const brake = (tel && tel.brake !== undefined) ? tel.brake : (isRetired ? 0 : (inPit ? 10 : pt.brake));
  const drs = (tel && tel.drs !== undefined) ? Boolean(tel.drs) : (sType !== 'practice' && !inPit && !isRetired && pt.drs > 0);
  const batterySoc = (tel && tel.batterySoc !== undefined) ? tel.batterySoc : Math.max(30, Math.min(98, 85 - (drvIdx * 2) + Math.sin(t * 0.1) * 12));
  const boostActive = (tel && tel.boostActive !== undefined) ? Boolean(tel.boostActive) : (pt.speed > 300 && drvIdx < 8);

  return {
    px: px,
    py: py,
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
    boostActive: boostActive,
    sector: pt.sector || 1
  };
}

function isSafetyCarActive(t) {
  return false;
}

function isVirtualSafetyCarActive(t) {
  return false;
}

window.IS_SYNTHETIC_SIMULATION = false;

function buildGenericSessionLaps(sType) {
  DRIVER_LAPS = {};
  DRIVER_STINTS = {};
  const driverKeys = GRID_ORDER;
  const baseTime = 89.450;

  const compounds = ['MED', 'HRD', 'SFT'];
  const fullNames = { SFT: 'SOFT', MED: 'MEDIUM', HRD: 'HARD' };

  const normType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sType, sessionPart) : sType;

  // In sessione reale/live (default), mai fabbricare giri o tempi fittizi sintetici
  if (!window.IS_SYNTHETIC_SIMULATION) {
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

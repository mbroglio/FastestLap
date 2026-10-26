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
  const tel = (typeof DRIVER_TELEMETRY !== 'undefined') ? DRIVER_TELEMETRY[drvKey] : null;
  if (tel && tel.lapsCount !== undefined && tel.lapsCount !== null) {
    const n = Number(tel.lapsCount);
    if (!isNaN(n) && n >= 0) {
      return tel.isRetired ? Math.max(1, n) : Math.max(1, n + 1);
    }
  }
  if (typeof window !== 'undefined' && window.CURRENT_SESSION_LAP && window.CURRENT_SESSION_LAP > 0) {
    return window.CURRENT_SESSION_LAP;
  }
  const laps = DRIVER_LAPS[drvKey];
  if (!laps || laps.length === 0) {
    const leaderKey = (typeof GRID_ORDER !== 'undefined' && GRID_ORDER[0]) ? GRID_ORDER[0] : null;
    if (leaderKey && leaderKey !== drvKey && DRIVER_TELEMETRY[leaderKey]?.lapsCount) {
      return Math.max(1, Number(DRIVER_TELEMETRY[leaderKey].lapsCount) + 1);
    }
    return 1;
  }
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
  const tel = (typeof DRIVER_TELEMETRY !== 'undefined') ? DRIVER_TELEMETRY[drvKey] : null;
  const curLap = getDriverLap(drvKey, t);

  let compound = 'MED';
  let age = null;

  if (tel) {
    if (tel.tyreCompound && tel.tyreCompound !== 'UNKNOWN') {
      compound = String(tel.tyreCompound).toUpperCase();
    }
    if (tel.tyreAge !== undefined && tel.tyreAge !== null && Number(tel.tyreAge) > 0) {
      age = Number(tel.tyreAge);
    } else if (tel.tyreTotalLaps !== undefined && tel.tyreTotalLaps > 0) {
      age = tel.tyreTotalLaps;
    } else if (tel.tyreStintLapNumber !== undefined && curLap >= tel.tyreStintLapNumber) {
      age = (tel.tyreStartLaps || 0) + (curLap - tel.tyreStintLapNumber);
    }
  }

  const stints = DRIVER_STINTS[drvKey];
  if (stints && stints.length > 0) {
    let activeStint = stints[stints.length - 1];
    for (let i = stints.length - 1; i >= 0; i--) {
      const s = stints[i];
      if (s.lapStart && curLap >= s.lapStart && (!s.lapEnd || curLap <= s.lapEnd)) {
        activeStint = s;
        break;
      }
    }
    if (!compound || compound === 'MED' || compound === 'UNKNOWN') {
      compound = (activeStint.code || activeStint.compound || 'MED').toUpperCase();
    }
    if (age === null || age === 0) {
      if (activeStint.totalLaps != null && activeStint.totalLaps > 0) {
        age = activeStint.totalLaps;
      } else if (activeStint.age != null && activeStint.age > 0) {
        age = activeStint.age;
      } else {
        const stintStart = activeStint.lapStart || 1;
        age = Math.max(1, curLap - stintStart + 1);
      }
    }
  }

  if (age === null || age <= 0) {
    age = Math.max(1, curLap);
  }

  let shortCode = 'MED';
  let col = '#FCD116';
  if (compound.includes('SOFT') || compound === 'SFT' || compound === 'S') { shortCode = 'SFT'; col = '#E10600'; }
  else if (compound.includes('HARD') || compound === 'HRD' || compound === 'H') { shortCode = 'HRD'; col = '#FFFFFF'; }
  else if (compound.includes('INTER') || compound === 'INT' || compound === 'I') { shortCode = 'INT'; col = '#43B02A'; }
  else if (compound.includes('WET') || compound === 'W') { shortCode = 'WET'; col = '#0072CE'; }

  return { compound: shortCode, age: age, color: col };
}

function parseLapTimeToSec(str) {
  if (!str || typeof str !== 'string') return 0;
  const clean = str.trim();
  if (!clean || clean === '--' || clean === '-') return 0;
  const parts = clean.split(':');
  if (parts.length === 2) {
    const mins = parseFloat(parts[0]);
    const secs = parseFloat(parts[1]);
    if (!isNaN(mins) && !isNaN(secs)) return (mins * 60) + secs;
  }
  const val = parseFloat(clean);
  return !isNaN(val) && val > 0 ? val : 0;
}

function getSessionBests(t) {
  let bestLap = Infinity, bestS1 = Infinity, bestS2 = Infinity, bestS3 = Infinity;
  const allKeys = new Set([
    ...Object.keys(DRIVERS || {}),
    ...Object.keys(DRIVER_LAPS || {}),
    ...Object.keys(DRIVER_TELEMETRY || {})
  ]);

  allKeys.forEach(k => {
    const laps = (typeof DRIVER_LAPS !== 'undefined' && DRIVER_LAPS[k]) ? DRIVER_LAPS[k] : [];
    laps.forEach(l => {
      if ((!window.IS_SYNTHETIC_SIMULATION || l.startSec + (l.dur || 0) <= t) && l.dur && l.dur > 50) {
        if (l.dur < bestLap) bestLap = l.dur;
        if (l.s1 && l.s1 < bestS1) bestS1 = l.s1;
        if (l.s2 && l.s2 < bestS2) bestS2 = l.s2;
        if (l.s3 && l.s3 < bestS3) bestS3 = l.s3;
      }
    });

    const tel = (typeof DRIVER_TELEMETRY !== 'undefined' && DRIVER_TELEMETRY[k]) ? DRIVER_TELEMETRY[k] : null;
    if (tel) {
      const telBest = tel.overallBest || (tel.bestLapTime ? parseLapTimeToSec(tel.bestLapTime) : 0);
      if (telBest && telBest > 50 && telBest < bestLap) {
        bestLap = telBest;
      }
      const telLast = tel.lastLapSec || (tel.lastLapTimeStr ? parseLapTimeToSec(tel.lastLapTimeStr) : 0);
      if (telLast && telLast > 50 && telLast < bestLap) {
        bestLap = telLast;
      }
      if (tel.q1Best && tel.q1Best > 50 && tel.q1Best < bestLap) bestLap = tel.q1Best;
      if (tel.q2Best && tel.q2Best > 50 && tel.q2Best < bestLap) bestLap = tel.q2Best;
      if (tel.q3Best && tel.q3Best > 50 && tel.q3Best < bestLap) bestLap = tel.q3Best;

      if (tel.sectorsState) {
        ['1', '2', '3'].forEach((secKey, idx) => {
          const sObj = tel.sectorsState[secKey];
          if (sObj && sObj.value) {
            const sVal = parseFloat(sObj.value);
            if (sVal > 0) {
              if (idx === 0 && sVal < bestS1) bestS1 = sVal;
              if (idx === 1 && sVal < bestS2) bestS2 = sVal;
              if (idx === 2 && sVal < bestS3) bestS3 = sVal;
            }
          }
        });
      }
    }
  });

  return { bestLap, bestS1, bestS2, bestS3 };
}

function getDriverSectorTimes(drvKey, t, sessionBests) {
  const laps = (typeof DRIVER_LAPS !== 'undefined' && DRIVER_LAPS[drvKey]) ? DRIVER_LAPS[drvKey] : [];
  const completed = laps.filter(l => (!window.IS_SYNTHETIC_SIMULATION || l.startSec + (l.dur || 0) <= t) && l.dur && l.dur > 50);
  if (completed.length === 0) {
    const tel = (typeof DRIVER_TELEMETRY !== 'undefined' && DRIVER_TELEMETRY[drvKey]) ? DRIVER_TELEMETRY[drvKey] : null;
    if (tel && tel.sectorsState) {
      const s1 = tel.sectorsState['1'] || {};
      const s2 = tel.sectorsState['2'] || {};
      const s3 = tel.sectorsState['3'] || {};
      const s1Class = s1.overallFastest ? 'badge-purple' : (s1.personalFastest ? 'badge-green' : (s1.value ? 'badge-yellow' : 'badge-gray'));
      const s2Class = s2.overallFastest ? 'badge-purple' : (s2.personalFastest ? 'badge-green' : (s2.value ? 'badge-yellow' : 'badge-gray'));
      const s3Class = s3.overallFastest ? 'badge-purple' : (s3.personalFastest ? 'badge-green' : (s3.value ? 'badge-yellow' : 'badge-gray'));
      return {
        s1: s1.value || '-', s1Badge: s1Class, s1Class: s1Class.replace('badge-', 'sector-'),
        s2: s2.value || '-', s2Badge: s2Class, s2Class: s2Class.replace('badge-', 'sector-'),
        s3: s3.value || '-', s3Badge: s3Class, s3Class: s3Class.replace('badge-', 'sector-')
      };
    }
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
  const laps = (typeof DRIVER_LAPS !== 'undefined' && DRIVER_LAPS[drvKey]) ? DRIVER_LAPS[drvKey] : [];
  const completed = laps.filter(l => (!window.IS_SYNTHETIC_SIMULATION || l.startSec + (l.dur || 0) <= t) && l.dur && l.dur > 50);
  const tel = (typeof DRIVER_TELEMETRY !== 'undefined' && DRIVER_TELEMETRY[drvKey]) ? DRIVER_TELEMETRY[drvKey] : null;

  const telLastStr = (tel && tel.lastLapTimeStr && tel.lastLapTimeStr !== '--' && tel.lastLapTimeStr !== '-') ? tel.lastLapTimeStr : null;
  const telLastSec = (tel && tel.lastLapSec && tel.lastLapSec > 0) ? tel.lastLapSec : (telLastStr ? parseLapTimeToSec(telLastStr) : 0);
  const telBestVal = (tel && tel.overallBest && tel.overallBest > 0) ? tel.overallBest : Infinity;

  let pBest = telBestVal;
  completed.forEach(l => { if (l.dur && l.dur < pBest) pBest = l.dur; });

  const bestLapStr = pBest < Infinity ? formatLapTime(pBest) : (tel && tel.bestLapTime ? tel.bestLapTime : '-');

  let lastDur = 0;
  let lastStr = '-';
  if (completed.length > 0) {
    const last = completed[completed.length - 1];
    lastDur = last.dur;
    lastStr = formatLapTime(last.dur);
  }
  if (telLastSec > 0) {
    lastDur = telLastSec;
  }
  if (telLastStr) {
    lastStr = telLastStr;
  }

  // BEST Lap Badge: purple se miglior tempo assoluto della sessione, verde se personal best
  let bestBadge = 'badge-gray';
  if (pBest < Infinity) {
    if (sessionBests && sessionBests.bestLap < Infinity && pBest <= sessionBests.bestLap + 0.005) {
      bestBadge = 'badge-purple';
    } else {
      bestBadge = 'badge-green';
    }
  }

  // LAST Lap Badge: purple se corrisponde al miglior tempo assoluto della sessione, verde se personal best, giallo altrimenti
  let lastBadge = 'badge-gray';
  if (lastDur > 0 || (lastStr && lastStr !== '-')) {
    if (sessionBests && sessionBests.bestLap < Infinity && lastDur > 0 && Math.abs(lastDur - sessionBests.bestLap) <= 0.005) {
      lastBadge = 'badge-purple';
    } else if (pBest < Infinity && lastDur > 0 && Math.abs(lastDur - pBest) <= 0.005) {
      lastBadge = 'badge-green';
    } else {
      lastBadge = 'badge-yellow';
    }
  }

  const lapsCnt = (tel && tel.lapsCount !== undefined) ? tel.lapsCount : (completed.length || 0);

  return {
    personalBest: pBest,
    bestLapStr: bestLapStr,
    bestLapBadge: bestBadge,
    lastLapStr: lastStr,
    lastLapBadge: lastBadge,
    lapsCount: lapsCnt
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

async function loadOfficialCircuitNodes(circuitKey) {
  if (!circuitKey || window._loadedCircuitKey === String(circuitKey)) return;
  window._loadedCircuitKey = String(circuitKey);

  const years = [2024, 2023];
  for (const year of years) {
    try {
      const resp = await fetch('https://api.multiviewer.app/api/v1/circuits/' + circuitKey + '/' + year);
      if (!resp.ok) continue;
      const data = await resp.json();
      if (data && Array.isArray(data.x) && Array.isArray(data.y) && data.x.length > 20) {
        applyOfficialCircuitToNodes(data);
        return;
      }
    } catch (e) {}
  }
}

function applyOfficialCircuitToNodes(data) {
  const rotDeg = Number(data.rotation) || 0;
  const rad = (rotDeg * Math.PI) / 180;
  const cos = Math.cos(rad);
  const sin = Math.sin(rad);

  const pts = [];
  const len = Math.min(data.x.length, data.y.length);
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;

  for (let i = 0; i < len; i++) {
    const rx = Number(data.x[i]);
    const ry = Number(data.y[i]);
    if (!Number.isFinite(rx) || !Number.isFinite(ry)) continue;
    const x = rx * cos - ry * sin;
    const y = rx * sin + ry * cos;
    if (x < minX) minX = x;
    if (x > maxX) maxX = x;
    if (y < minY) minY = y;
    if (y > maxY) maxY = y;
    pts.push({ x, y });
  }

  const n = pts.length;
  if (n < 10) return;

  window.OFFICIAL_MINI_SECTOR_INDEXES = Array.isArray(data.miniSectorsIndexes)
    ? data.miniSectorsIndexes.map(v => Number(v)).filter(v => Number.isFinite(v))
    : [];

  let totalRaw = 0;
  for (let i = 0; i < n; i++) {
    const next = pts[(i + 1) % n];
    pts[i].rawDs = Math.hypot(next.x - pts[i].x, next.y - pts[i].y);
    totalRaw += pts[i].rawDs;
  }
  const scaleToMeters = totalRaw > 15000 ? 0.1 : 1.0;
  let cumMeters = 0;
  for (let i = 0; i < n; i++) {
    pts[i].ds = Math.max(0.4, pts[i].rawDs * scaleToMeters);
    pts[i].distMeters = cumMeters;
    cumMeters += pts[i].ds;
  }

  const findIdxByOffsetMeters = (startIdx, offsetMeters) => {
    let acc = 0;
    let curr = startIdx;
    const step = offsetMeters >= 0 ? 1 : -1;
    const target = Math.abs(offsetMeters);
    let guard = 0;
    while (acc < target && guard < n) {
      curr = (curr + step + n) % n;
      acc += pts[curr].ds;
      guard++;
    }
    return curr;
  };

  const maxVms = 328 / 3.6;
  for (let i = 0; i < n; i++) {
    const calcKappa = (halfWindowM) => {
      const iP = findIdxByOffsetMeters(i, -halfWindowM);
      const iN = findIdxByOffsetMeters(i, halfWindowM);
      const a1 = Math.atan2(pts[i].y - pts[iP].y, pts[i].x - pts[iP].x);
      const a2 = Math.atan2(pts[iN].y - pts[i].y, pts[iN].x - pts[i].x);
      let dT = Math.abs(a2 - a1);
      if (dT > Math.PI) dT = 2 * Math.PI - dT;
      return dT / (halfWindowM * 2);
    };
    const kappa = Math.max(calcKappa(14), calcKappa(30));
    pts[i].kappa = kappa;
    if (kappa < 0.0012) {
      pts[i].vLimit = maxVms;
    } else {
      const aLat = 16 + Math.min(28, (0.006 / kappa) * 18);
      pts[i].vLimit = Math.max(84 / 3.6, Math.min(maxVms, Math.sqrt(aLat / kappa)));
    }
    pts[i].v = pts[i].vLimit;
  }

  // Backward pass (frenata)
  for (let pass = 0; pass < 2; pass++) {
    for (let i = n - 1; i >= 0; i--) {
      const next = pts[(i + 1) % n];
      const aBrake = 24 + 16 * Math.pow(next.v / maxVms, 2);
      const vMaxEntry = Math.sqrt(next.v * next.v + 2 * aBrake * pts[i].ds);
      if (vMaxEntry < pts[i].v) pts[i].v = vMaxEntry;
    }
  }

  // Forward pass (accelerazione)
  for (let pass = 0; pass < 2; pass++) {
    for (let i = 0; i < n; i++) {
      const next = pts[(i + 1) % n];
      const vRatio = pts[i].v / (330 / 3.6);
      const aAccel = Math.max(1.0, 14.0 * (1 - Math.pow(vRatio, 1.6)));
      const vMaxExit = Math.sqrt(pts[i].v * pts[i].v + 2 * aAccel * pts[i].ds);
      if (vMaxExit < next.v) next.v = vMaxExit;
    }
  }

  let totalSimSec = 0;
  for (let i = 0; i < n; i++) {
    pts[i].simTime = totalSimSec;
    totalSimSec += pts[i].ds / Math.max(15, pts[i].v);
  }

  SVG_WIDTH = 1000;
  SVG_HEIGHT = 800;
  const targetW = SVG_WIDTH;
  const targetH = SVG_HEIGHT;
  const pad = 60;
  const spanX = Math.max(1, maxX - minX);
  const spanY = Math.max(1, maxY - minY);
  const scale = Math.min((targetW - pad * 2) / spanX, (targetH - pad * 2) / spanY);
  const midX = (minX + maxX) / 2;
  const midY = (minY + maxY) / 2;
  const lapDur = LAP_DURATION || 90.0;

  window._officialCircuitLoaded = true;
  window.OFFICIAL_CIRCUIT_TRANSFORM = {
    cos,
    sin,
    midX,
    midY,
    scale,
    targetW,
    targetH,
    scaleToMeters
  };

  const builtNodes = [];
  const gearMinSpeeds = [0, 0, 78, 106, 140, 176, 212, 248, 286];
  const gearMaxSpeeds = [0, 82, 114, 148, 184, 220, 256, 294, 348];

  for (let i = 0; i < n; i++) {
    const p = pts[i];
    const next = pts[(i + 1) % n];
    const longG = ((next.v * next.v - p.v * p.v) / (2 * p.ds)) / 9.81;
    const speed = Math.round(p.v * 3.6);

    let gear = 8;
    if (speed < 106) gear = 2;
    else if (speed < 140) gear = 3;
    else if (speed < 176) gear = 4;
    else if (speed < 212) gear = 5;
    else if (speed < 248) gear = 6;
    else if (speed < 286) gear = 7;

    const gMin = gearMinSpeeds[gear] || 80;
    const gMax = gearMaxSpeeds[gear] || 340;
    const gRatio = Math.max(0, Math.min(1, (speed - gMin) / Math.max(1, gMax - gMin)));

    let brake = 0;
    let throttle = 100;
    if (longG < -0.28) {
      brake = Math.min(100, Math.max(15, Math.round((-longG / 2.8) * 100)));
      throttle = longG < -0.85 ? 0 : Math.max(0, Math.round(25 * (1 + longG)));
    } else if (p.kappa > 0.0085) {
      const apexLoad = Math.min(1, (p.kappa - 0.0085) / 0.025);
      throttle = Math.round(85 - apexLoad * 48);
    } else if (p.kappa > 0.003) {
      throttle = Math.round(92 - Math.min(1, (p.kappa - 0.003) / 0.006) * 15);
    }

    const rpm = Math.round((brake > 30 ? 9650 : 10050) + gRatio * (brake > 30 ? 1850 : 2350));
    const drs = (p.kappa < 0.0022 && brake === 0 && throttle >= 95 && speed > 276) ? 1 : 0;
    const prog = p.distMeters / Math.max(1, cumMeters);
    const sector = prog < 0.33 ? 1 : (prog < 0.67 ? 2 : 3);

    const px = Number(((targetW / 2) + (p.x - midX) * scale).toFixed(2));
    const py = Number(((targetH / 2) - (p.y - midY) * scale).toFixed(2));
    const t = Number(((p.simTime / Math.max(1, totalSimSec)) * lapDur).toFixed(3));

    builtNodes.push([t, px, py, null, null, sector, speed, gear, rpm, throttle, brake, drs, 'Pista']);
  }

  NODES = builtNodes;
  window.NODES = NODES;
  if (typeof resizeTrackCanvas === 'function') resizeTrackCanvas();
  if (typeof drawTrack === 'function') drawTrack();
}

function getDriverTrackSec(drvKey, t) {
  const dur = LAP_DURATION || 90.0;
  const tel = DRIVER_TELEMETRY[drvKey];
  if (tel && tel.anchorTrackSec !== undefined && tel.anchorSimSec !== undefined) {
    const elapsed = Math.max(0, t - tel.anchorSimSec);
    let sec = tel.anchorTrackSec + elapsed;
    if (tel.targetTrackSec !== undefined) {
      let tDiff = ((tel.targetTrackSec - sec + dur * 1.5) % dur) - dur * 0.5;
      if (Math.abs(tDiff) < dur * 0.35) {
        sec += Math.max(-0.25, Math.min(0.25, tDiff * 0.3)) * Math.min(1.0, elapsed * 0.5);
      }
    }
    if (tel.segStartSec !== undefined && tel.segEndSec !== undefined && tel.segEndSec > tel.segStartSec) {
      const maxSec = tel.segMaxSec !== undefined ? tel.segMaxSec : (tel.segEndSec - 0.04);
      if (sec > maxSec) {
        // Asintoto dolce progressivo: la vettura decelera dolcemente invece di bloccarsi contro un muro
        const over = sec - maxSec;
        const softHeadroom = Math.max(0.08, (tel.segEndSec - maxSec) * 0.7);
        sec = maxSec + (1 - Math.exp(-over / softHeadroom)) * softHeadroom;
      }
      if (sec < tel.segStartSec) sec = tel.segStartSec;
    }
    return ((sec % dur) + dur) % dur;
  }
  const drvIdx = GRID_ORDER.indexOf(drvKey);
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
  if (laps.length === 0 && !window.IS_SYNTHETIC_SIMULATION && (!tel || tel.inPit === undefined)) {
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
      const rawX = Number(livePos.curX !== undefined ? livePos.curX : livePos.x);
      const rawY = Number(livePos.curY !== undefined ? livePos.curY : livePos.y);
      const tr = window.OFFICIAL_CIRCUIT_TRANSFORM;
      if (tr && Number.isFinite(rawX) && Number.isFinite(rawY)) {
        const posMul = (tr.scaleToMeters === 0.1 && Math.max(Math.abs(rawX), Math.abs(rawY)) < 2500) ? 10.0 : 1.0;
        const rx = rawX * posMul;
        const ry = rawY * posMul;
        const rotX = rx * tr.cos - ry * tr.sin;
        const rotY = rx * tr.sin + ry * tr.cos;
        px = (tr.targetW / 2) + (rotX - tr.midX) * tr.scale;
        py = (tr.targetH / 2) - (rotY - tr.midY) * tr.scale;
      } else {
        px = rawX;
        py = rawY;
      }
    }
  }

  // 2. Coordinate e telemetria continua interpolata dal modello fisico di pista
  const trackSec = getDriverTrackSec(drvKey, t);
  const pt = getTrackPointAtTime(trackSec);

  if (px === undefined || py === undefined) {
    px = pt.px;
    py = pt.py;
  }

  const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType;

  // 3. Valori telemetrici dinamici per Cockpit con corrispondenza 1:1 rispetto al punto pista
  const topTrap = (tel && tel.topSpeedTrap > 290) ? tel.topSpeedTrap : 326;
  const scaledPtSpeed = pt.speed > 230 ? Math.round(pt.speed * (topTrap / 326)) : pt.speed;

  const speed = (tel && tel.speed !== undefined) ? tel.speed : (isRetired || inPit ? 0 : (isOutLap ? 80 : scaledPtSpeed));
  const gear = (tel && tel.gear !== undefined) ? tel.gear : (isRetired || inPit ? 0 : (isOutLap ? 2 : pt.gear));
  const rpm = (tel && tel.rpm !== undefined) ? tel.rpm : (isRetired || inPit ? 0 : (isOutLap ? 9650 : pt.rpm));
  const throttle = (tel && tel.throttle !== undefined) ? tel.throttle : (isRetired || inPit ? 0 : (isOutLap ? 42 : pt.throttle));
  const brake = (tel && tel.brake !== undefined) ? tel.brake : (isRetired || inPit || isOutLap ? 0 : pt.brake);
  const drs = (tel && tel.drs !== undefined) ? Boolean(tel.drs) : (!inPit && !isRetired && !isOutLap && pt.drs > 0);
  const batterySoc = (tel && tel.batterySoc !== undefined) ? tel.batterySoc : Math.max(30, Math.min(98, 85 - (drvIdx * 2) + Math.sin(t * 0.1) * 12));
  const boostActive = (tel && tel.boostActive !== undefined) ? Boolean(tel.boostActive) : (speed > 295 && !inPit && !isRetired);

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

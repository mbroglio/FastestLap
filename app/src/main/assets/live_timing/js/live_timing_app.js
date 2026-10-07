/**
 * FastestLap — Live Timing & Telemetry Client Engine
 * Core animation loop, timing calculation, canvas drawing and Android Bridge integration
 */

// ACTIVE SESSION CONTEXT
let currentSim = SIM_SEPANG;
let NODES = currentSim.nodes;
let DRIVERS = currentSim.drivers;
let DRIVER_KEYFRAMES = currentSim.keyframes;
let PIT_STOPS = currentSim.pitStops;
let RACE_CONTROL_MESSAGES = currentSim.rc;
let RETIREMENTS = currentSim.retirements;
let DRIVER_STINTS = currentSim.stints;
let DRIVER_LAPS = currentSim.laps;
let RACE_EVENTS = currentSim.events;
let INCIDENTS = currentSim.incidents;
let LAP_STARTS = currentSim.lapStarts;
let GRID_ORDER = currentSim.gridOrder;
let PIT_LANE_NODES = currentSim.pitNodes;
let SVG_WIDTH = currentSim.svgWidth;
let SVG_HEIGHT = currentSim.svgHeight;
let LAP_DURATION = currentSim.lapDuration;
let TOTAL_LAPS = currentSim.totalLaps;
let RACE_FINISH_SEC = currentSim.raceFinishSec;
let SESSION_DURATION = currentSim.sessionDuration;
let PIT_EXIT_TRACK_SEC = currentSim.pitExitTrackSec;
let PIT_ENTRY_TRACK_SEC = currentSim.pitEntryTrackSec;

let PIT_LANE_DISTS = [0];
let PIT_LANE_TOTAL_DIST = 0;

function recalculatePitLaneDistances() {
  PIT_LANE_DISTS = [0];
  for (let i = 0; i < PIT_LANE_NODES.length - 1; i++) {
    const dx = PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px;
    const dy = PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py;
    PIT_LANE_DISTS.push(PIT_LANE_DISTS[i] + Math.sqrt(dx * dx + dy * dy));
  }
  PIT_LANE_TOTAL_DIST = PIT_LANE_DISTS[PIT_LANE_DISTS.length - 1] || 1;
}
recalculatePitLaneDistances();

// SIMULATION RUNTIME STATE
let currentCircuit = 'sepang';
let currentSecond = 0.0;
let simSpeedMultiplier = 1.0;
let isPlaying = true;
let lastAnimFrameTimestamp = null;
let focusedDriver = null;
let selectedDriver1 = '3';
let selectedDriver2 = '16';
let isMapFollowingDriver = false;
let currentMainView = 'view-standings';
let currentSideTab = 'side-standings';
let activeRcFilter = 'ALL';
let trackBgImage = null;
let isGenericRealMode = false;
let expandedDriverKey = null;
let sessionType = 'race';
let sessionPart = '';

function renderTableHeaders(type) {
  const fullThead = document.querySelector('#view-standings .full-standings-table thead');
  const compactThead = document.querySelector('#side-standings .compact-timing-table thead');
  if (!fullThead && !compactThead) return;

  if (type === 'practice') {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-best">MIGLIOR GIRO</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-int">INTERVALLO</th>'
        + '<th class="col-last">ULTIMO GIRO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-laps">GIRI</th>'
        + '<th class="col-status">STATO</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-best">MIGLIORE</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-last">ULTIMO</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '<th class="col-side-status">STATO</th>'
        + '</tr>';
    }
  } else if (type === 'qualifying') {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-q1">Q1</th>'
        + '<th class="col-q2">Q2</th>'
        + '<th class="col-q3">Q3</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-laps">GIRI</th>'
        + '<th class="col-status">STATO</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-best">TEMPO</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '<th class="col-side-status">STATO</th>'
        + '</tr>';
    }
  } else {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-gain">+/-</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-int">INTERVALLO</th>'
        + '<th class="col-last">ULTIMO GIRO</th>'
        + '<th class="col-best">MIGLIOR GIRO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-pit">PIT</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-gain">+/-</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-int">INTERVALLO</th>'
        + '<th class="col-side-last">ULTIMO</th>'
        + '<th class="col-side-best">MIGLIORE</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '</tr>';
    }
  }
}

function setSessionType(type) {
  sessionType = type;
  if (currentCircuit === 'sepang') {
    if (type === 'practice') {
      applySimulation(SIM_SEPANG_FP);
    } else if (type === 'qualifying') {
      applySimulation(SIM_SEPANG_QUALIFYING);
    } else {
      applySimulation(SIM_SEPANG);
    }
  }
  renderTableHeaders(type);
  renderSimulationControls(!isGenericRealMode);
  updateSessionStatusBar();
  updateTimingTables();
  drawTrack();
}
window.setSessionType = setSessionType;

function applySimulation(sim) {
  currentSim = sim;
  currentCircuit = sim.circuitId || 'sepang';
  NODES = currentSim.nodes;
  DRIVERS = currentSim.drivers;
  DRIVER_KEYFRAMES = currentSim.keyframes;
  PIT_STOPS = currentSim.pitStops;
  RACE_CONTROL_MESSAGES = currentSim.rc;
  RETIREMENTS = currentSim.retirements;
  DRIVER_STINTS = currentSim.stints;
  DRIVER_LAPS = currentSim.laps;
  RACE_EVENTS = currentSim.events;
  INCIDENTS = currentSim.incidents;
  LAP_STARTS = currentSim.lapStarts;
  GRID_ORDER = currentSim.gridOrder;
  PIT_LANE_NODES = currentSim.pitNodes;
  SVG_WIDTH = currentSim.svgWidth;
  SVG_HEIGHT = currentSim.svgHeight;
  LAP_DURATION = currentSim.lapDuration;
  TOTAL_LAPS = currentSim.totalLaps;
  RACE_FINISH_SEC = currentSim.raceFinishSec;
  SESSION_DURATION = currentSim.sessionDuration;
  PIT_EXIT_TRACK_SEC = currentSim.pitExitTrackSec;
  PIT_ENTRY_TRACK_SEC = currentSim.pitEntryTrackSec;

  recalculatePitLaneDistances();
  currentSecond = 0.0;

  populateDriverDropdowns();
  focusedDriver = currentSim.gridOrder[0] || '3';
  selectedDriver1 = currentSim.gridOrder[0] || '3';
  selectedDriver2 = currentSim.gridOrder[1] || '16';

  const hdrTitle = document.getElementById('hdrEventTitle');
  if (hdrTitle) {
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
      const bTitle = window.FastestLapBridge.getEventTitle();
      if (bTitle && bTitle.trim().length > 0 && bTitle.toLowerCase() !== 'live timing') {
        hdrTitle.textContent = bTitle;
      } else {
        hdrTitle.textContent = currentSim.eventTitle;
      }
    } else {
      hdrTitle.textContent = currentSim.eventTitle;
    }
  }

  ['btnSimSepang', 'btnSimBaku'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const activeBtn = document.getElementById(sim.circuitId === 'baku' ? 'btnSimBaku' : 'btnSimSepang');
  if (activeBtn) activeBtn.classList.add('active');

  ['btnTypeRace', 'btnTypeQual', 'btnTypeFP'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const actTypeBtn = document.getElementById(sessionType === 'practice' ? 'btnTypeFP' : (sessionType === 'qualifying' ? 'btnTypeQual' : 'btnTypeRace'));
  if (actTypeBtn) actTypeBtn.classList.add('active');

  renderTableHeaders(sessionType);
  resizeTrackCanvas();
  updateTimingTables();
}

function switchSimulationCircuit(circuitId) {
  if (typeof resetMapFocus === 'function') resetMapFocus();
  else if (typeof resetMapZoom === 'function') resetMapZoom();
  if (circuitId === 'baku') {
    applySimulation(SIM_BAKU);
  } else {
    if (sessionType === 'practice') {
      applySimulation(SIM_SEPANG_FP);
    } else if (sessionType === 'qualifying') {
      applySimulation(SIM_SEPANG_QUALIFYING);
    } else {
      applySimulation(SIM_SEPANG);
    }
  }
}

function populateDriverDropdowns() {
  const keys = Object.keys(DRIVERS);
  const opts = keys.map(k => {
    const d = DRIVERS[k];
    return '<option value="' + k + '">#' + d.number + ' ' + d.code + ' — ' + d.firstName + ' ' + d.lastName + ' (' + d.team + ')</option>';
  }).join('');

  const s1 = document.getElementById('drv1Select');
  const s2 = document.getElementById('drv2Select');
  if (s1) s1.innerHTML = opts;
  if (s2) s2.innerHTML = opts;
  if (s1) s1.value = selectedDriver1;
  if (s2) s2.value = selectedDriver2;
}

// ══════════════════════════════════════════════════════════════
// AUTHENTIC SIMULATION FUNCTIONS
// ══════════════════════════════════════════════════════════════
function getDriverLapRecord(drvKey, t) {
  const dl = DRIVER_LAPS[drvKey];
  if (!dl || dl.length === 0) return null;
  for (let i = 0; i < dl.length; i++) {
    const l = dl[i];
    const nextStart = dl[i + 1] ? dl[i + 1].startSec : (l.startSec + (l.dur || LAP_DURATION));
    if (t >= l.startSec && t < nextStart) {
      return l;
    }
  }
  if (t < dl[0].startSec) return dl[0];
  return dl[dl.length - 1];
}

function getPitLaneCoordAtDist(dist) {
  if (dist <= 0) return { px: PIT_LANE_NODES[0].px, py: PIT_LANE_NODES[0].py };
  if (dist >= PIT_LANE_TOTAL_DIST) return { px: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].px, py: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].py };
  for (let i = 0; i < PIT_LANE_DISTS.length - 1; i++) {
    if (dist >= PIT_LANE_DISTS[i] && dist <= PIT_LANE_DISTS[i + 1]) {
      const segLen = PIT_LANE_DISTS[i + 1] - PIT_LANE_DISTS[i];
      const u = segLen > 0 ? (dist - PIT_LANE_DISTS[i]) / segLen : 0;
      return {
        px: PIT_LANE_NODES[i].px + (PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px) * u,
        py: PIT_LANE_NODES[i].py + (PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py) * u
      };
    }
  }
  return { px: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].px, py: PIT_LANE_NODES[PIT_LANE_NODES.length - 1].py };
}

function getDriverGap(drvKey, t) {
  if (currentSim.circuitId === 'sepang' && t < 216.9) {
    const gridIdx = GRID_ORDER.indexOf(drvKey);
    return (gridIdx >= 0 ? gridIdx : 21) * 0.15;
  }
  const kfs = DRIVER_KEYFRAMES[drvKey];
  if (!kfs) return 0;
  if (t <= kfs[0][0]) return kfs[0][1];
  if (t >= kfs[kfs.length - 1][0]) return kfs[kfs.length - 1][1];

  let low = 0, high = kfs.length - 1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (kfs[mid][0] <= t) low = mid + 1;
    else high = mid - 1;
  }
  const i1 = Math.max(0, high);
  const i2 = Math.min(kfs.length - 1, i1 + 1);
  const p1 = kfs[i1];
  const p2 = kfs[i2];
  const dt = p2[0] - p1[0];
  if (dt <= 0) return p1[1];
  const u = (t - p1[0]) / dt;
  return p1[1] + (p2[1] - p1[1]) * u;
}

function isDriverRetired(drvKey, t) {
  return Boolean(RETIREMENTS[drvKey] && t >= RETIREMENTS[drvKey].timeSec);
}

function getDriverProgress(drvKey, t) {
  if (isDriverRetired(drvKey, t)) {
    const ret = RETIREMENTS[drvKey];
    return ret.timeSec - getDriverGap(drvKey, ret.timeSec);
  }
  if (currentSim.circuitId === 'sepang' && t < 216.9) {
    const gridIdx = GRID_ORDER.indexOf(drvKey);
    const pos = gridIdx >= 0 ? gridIdx : 21;
    return t - (pos * 0.15);
  }
  if (t >= RACE_FINISH_SEC) {
    const finishProg = RACE_FINISH_SEC - getDriverGap(drvKey, RACE_FINISH_SEC);
    const inLapElapsed = Math.min(115, t - RACE_FINISH_SEC);
    return finishProg + inLapElapsed * 0.35;
  }
  return t - getDriverGap(drvKey, t);
}

function getDriverLap(drvKey, t) {
  if (isDriverRetired(drvKey, t)) {
    return RETIREMENTS[drvKey].lap;
  }
  if (t >= RACE_FINISH_SEC) return TOTAL_LAPS;
  if (currentSim.circuitId === 'sepang') {
    if (t < 108.0) return 0;
    if (t < 216.9) return 0;
  }
  const rec = getDriverLapRecord(drvKey, t);
  if (rec) {
    return rec.lap;
  }
  const gap = getDriverGap(drvKey, t);
  const tEff = Math.max(0, t - gap);
  for (let l = 1; l <= TOTAL_LAPS; l++) {
    if (tEff >= LAP_STARTS[l] && tEff < (LAP_STARTS[l + 1] || Infinity)) {
      return l;
    }
  }
  return TOTAL_LAPS;
}

function getSlotOffset(pos) {
  return -(0.24 + pos * 0.208);
}

function getDriverTrackSec(drvKey, t) {
  if (isDriverRetired(drvKey, t)) {
    const ret = RETIREMENTS[drvKey];
    if (ret && ret.timeSec) {
      const gapAtRet = getDriverGap(drvKey, ret.timeSec);
      return (((ret.timeSec - gapAtRet) % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
    }
    return 30.0;
  }

  const gridIdx = GRID_ORDER.indexOf(drvKey);
  const pos = gridIdx >= 0 ? gridIdx : 21;
  const slotOff = getSlotOffset(pos);
  const totalTarget = slotOff + 2 * LAP_DURATION;

  if (currentSim.circuitId === 'sepang' && t < 216.9) {
    const stopTime = 204.0 + pos * 0.42;
    if (t < stopTime) {
      const u = t / stopTime;
      const smoothU = u < 0.85 ? u : 0.85 + (1 - Math.cos((u - 0.85) / 0.15 * Math.PI / 2)) * 0.15;
      const unwrapDist = slotOff + (totalTarget - slotOff) * smoothU;
      return ((unwrapDist % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
    } else {
      return ((slotOff % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
    }
  }

  if (t >= RACE_FINISH_SEC) {
    return Math.min(LAP_DURATION, (t - RACE_FINISH_SEC) * 0.35);
  }

  // Pit stop check
  const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
  if (activeStop) {
    const frac = (t - activeStop.startSec) / Math.max(1, activeStop.endSec - activeStop.startSec);
    return frac * PIT_EXIT_TRACK_SEC;
  }

  // Out-lap check
  const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
  if (outStop) {
    const outEnd = outStop.outLapEndSec || (outStop.endSec + 80);
    const u = Math.min(1.0, Math.max(0.0, (t - outStop.endSec) / Math.max(1, outEnd - outStop.endSec)));
    return PIT_EXIT_TRACK_SEC + u * (LAP_DURATION - PIT_EXIT_TRACK_SEC);
  }

  // Lap timing progression
  const rec = getDriverLapRecord(drvKey, t);
  if (rec && rec.dur && rec.dur > 0) {
    const inStop = PIT_STOPS.find(p => p.driver === drvKey && p.startSec > rec.startSec && p.startSec <= rec.startSec + rec.dur && t < p.startSec && t >= rec.startSec);
    if (inStop) {
      const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(1, inStop.startSec - rec.startSec)));
      return u * PIT_ENTRY_TRACK_SEC;
    }

    const tS1 = rec.startSec + (rec.s1 || (rec.dur * 0.25));
    const tS2 = tS1 + (rec.s2 || (rec.dur * 0.35));
    const S1_TRACK = currentSim.s1Track;
    const S2_TRACK = currentSim.s2Track;

    if (t < tS1) {
      const u = Math.max(0, (t - rec.startSec) / Math.max(0.1, tS1 - rec.startSec));
      return u * S1_TRACK;
    } else if (t < tS2) {
      const u = Math.max(0, (t - tS1) / Math.max(0.1, tS2 - tS1));
      return S1_TRACK + u * (S2_TRACK - S1_TRACK);
    } else {
      const u = Math.max(0, (t - tS2) / Math.max(0.1, (rec.startSec + rec.dur) - tS2));
      return S2_TRACK + u * (LAP_DURATION - S2_TRACK);
    }
  }

  const prog = getDriverProgress(drvKey, t);
  return ((prog % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;
}

function getDriverTireInfo(drvKey, t) {
  const stints = DRIVER_STINTS[drvKey];
  const curLap = getDriverLap(drvKey, t);
  if (!stints || stints.length === 0) {
    return { code: 'M', name: 'MEDIUM', age: Math.max(1, curLap), stint: 1 };
  }
  if (curLap <= 1) {
    return { code: stints[0].code, name: stints[0].compound, age: 1, stint: 1 };
  }
  for (let i = 0; i < stints.length; i++) {
    const s = stints[i];
    if (curLap >= s.lapStart && curLap <= s.lapEnd) {
      return {
        code: s.code,
        name: s.compound,
        age: Math.max(1, curLap - s.lapStart + 1),
        stint: s.stint
      };
    }
  }
  const last = stints[stints.length - 1];
  return {
    code: last.code,
    name: last.compound,
    age: Math.max(1, curLap - last.lapStart + 1),
    stint: last.stint
  };
}

function interpolateTrackNode(trackSec) {
  const n = NODES.length;
  if (n === 0) {
    return { px: 0, py: 0, speed: 0, gear: 0, rpm: 0, throttle: 0, brake: 0, drs: 0, location: '' };
  }
  const s = ((trackSec % LAP_DURATION) + LAP_DURATION) % LAP_DURATION;

  // Binary search for exact track node matching timestamp s (1:1 telemetry & physical position match)
  let low = 0;
  let high = n - 1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (NODES[mid][0] <= s) {
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }

  const i1 = Math.max(0, high);
  const i2 = (i1 + 1) % n;
  const n1 = NODES[i1];
  const n2 = NODES[i2];

  let dt = n2[0] - n1[0];
  if (dt <= 0) dt += LAP_DURATION;
  let u = 0;
  if (dt > 0) {
    let el = s - n1[0];
    if (el < 0) el += LAP_DURATION;
    u = Math.min(1.0, Math.max(0.0, el / dt));
  }

  const rawBrake = u < 0.5 ? n1[10] : n2[10];
  const brakeVal = rawBrake > 1 ? rawBrake : (rawBrake ? 100 : 0);

  return {
    px: n1[1] + (n2[1] - n1[1]) * u,
    py: n1[2] + (n2[2] - n1[2]) * u,
    speed: Math.round(n1[6] + (n2[6] - n1[6]) * u),
    gear: u < 0.5 ? n1[7] : n2[7],
    rpm: Math.round(n1[8] + (n2[8] - n1[8]) * u),
    throttle: Math.round(n1[9] + (n2[9] - n1[9]) * u),
    brake: brakeVal,
    drs: n1[11],
    location: (u < 0.5 ? n1[12] : n2[12]) || n1[12]
  };
}

function getDriverVisualState(drvKey, t) {
  const isRet = isDriverRetired(drvKey, t);
  const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
  const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
  const isOutLapMerge = Boolean(outStop && (t - outStop.endSec) <= 2.0);

  if (isRet) {
    const trackSec = getDriverTrackSec(drvKey, t);
    const originPt = interpolateTrackNode(trackSec);
    return {
      px: originPt.px + 6,
      py: originPt.py + 6,
      speed: 0,
      gear: 0,
      rpm: 0,
      throttle: 0,
      brake: 0,
      drs: 0,
      location: 'Ritiro — Fuori Traiettoria',
      isRetired: true,
      inPit: false,
      isOutLapMerge: false
    };
  }

  if (activeStop) {
    const dur = Math.max(1, activeStop.endSec - activeStop.startSec);
    const elapsed = t - activeStop.startSec;
    const progressFrac = Math.max(0, Math.min(1, elapsed / dur));
    const gridIdx = GRID_ORDER.indexOf(drvKey);
    const stallFrac = 0.35 + (gridIdx >= 0 ? gridIdx : 10) * 0.012;
    const stallDist = stallFrac * PIT_LANE_TOTAL_DIST;

    let curDist = 0;
    let speed = 80;
    let gear = 2;
    let rpm = 4500;
    let loc = 'Corsia Box (80 km/h)';

    if (progressFrac < 0.35) {
      const u = progressFrac / 0.35;
      curDist = u * stallDist;
      speed = Math.round(80 * (1 - u * 0.4));
      gear = 2;
    } else if (progressFrac <= 0.65) {
      curDist = stallDist;
      speed = 0;
      gear = 0;
      rpm = 3200;
      loc = 'Sosta ai Box (Cambio Gomme)';
    } else {
      const u = (progressFrac - 0.65) / 0.35;
      curDist = stallDist + u * (PIT_LANE_TOTAL_DIST - stallDist);
      speed = Math.round(40 + u * 40);
      gear = 2;
      loc = 'Uscita Box verso Curva 1';
    }

    const pt = getPitLaneCoordAtDist(curDist);
    return {
      px: pt.px,
      py: pt.py,
      speed: speed,
      gear: gear,
      rpm: rpm,
      throttle: speed > 0 ? 55 : 0,
      brake: speed === 0 ? 100 : 0,
      drs: 0,
      location: loc,
      isRetired: false,
      inPit: true,
      isOutLapMerge: false
    };
  }

  const trackSec = getDriverTrackSec(drvKey, t);
  const rawState = interpolateTrackNode(trackSec);

  if (isOutLapMerge && outStop) {
    const u = (t - outStop.endSec) / 2.0;
    const su = u * u * (3 - 2 * u);
    const pitExitNode = PIT_LANE_NODES[PIT_LANE_NODES.length - 1];
    return {
      px: pitExitNode.px * (1 - su) + rawState.px * su,
      py: pitExitNode.py * (1 - su) + rawState.py * su,
      speed: Math.round(80 + su * (rawState.speed - 80)),
      gear: Math.max(2, rawState.gear),
      rpm: rawState.rpm,
      throttle: rawState.throttle,
      brake: rawState.brake,
      drs: rawState.drs,
      location: 'Rientro in Pista — Curva 1',
      isRetired: false,
      inPit: false,
      isOutLapMerge: true
    };
  }

  return {
    ...rawState,
    speed: Math.round(rawState.speed),
    isRetired: false,
    inPit: false,
    isOutLapMerge: false
  };
}

function isSafetyCarActive(t) {
  if (currentSim.circuitId === 'sepang') {
    if (t < 216.9) return true;
    if (t >= 1150 && t <= 1580) return true;
  } else if (currentSim.circuitId === 'baku') {
    if (t >= 3285 && t <= 3924) return true;
    if (t >= 4130 && t <= 4396) return true;
  }
  return false;
}

function isVirtualSafetyCarActive(t) {
  if (currentSim.circuitId === 'sepang') {
    if (t >= 3200 && t <= 3350) return true;
  }
  return false;
}

function getSessionBests(t) {
  let bestLap = { val: Infinity, str: '-', drv: null };
  let bestS1 = { val: Infinity, str: '-' };
  let bestS2 = { val: Infinity, str: '-' };
  let bestS3 = { val: Infinity, str: '-' };

  Object.keys(DRIVERS).forEach(k => {
    const laps = DRIVER_LAPS[k] || [];
    laps.forEach(l => {
      if (l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50) {
        if (l.dur < bestLap.val) {
          bestLap = { val: l.dur, str: formatLapTime(l.dur), drv: k };
        }
        if (l.s1 && l.s1 < bestS1.val) bestS1 = { val: l.s1, str: l.s1.toFixed(3) };
        if (l.s2 && l.s2 < bestS2.val) bestS2 = { val: l.s2, str: l.s2.toFixed(3) };
        if (l.s3 && l.s3 < bestS3.val) bestS3 = { val: l.s3, str: l.s3.toFixed(3) };
      }
    });
  });

  return { bestLap, bestS1, bestS2, bestS3 };
}

function getDriverTimingStats(drvKey, t, sessionBests) {
  const laps = DRIVER_LAPS[drvKey] || [];
  const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50);

  let personalBest = Infinity;
  completedLaps.forEach(l => {
    if (l.dur < personalBest) personalBest = l.dur;
  });

  const lastLap = completedLaps.length > 0 ? completedLaps[completedLaps.length - 1] : null;
  let lastLapStr = '-';
  let lastLapBadge = 'badge-dim';
  if (lastLap) {
    lastLapStr = formatLapTime(lastLap.dur);
    if (sessionBests && Math.abs(lastLap.dur - sessionBests.bestLap.val) < 0.005) {
      lastLapBadge = 'badge-purple';
    } else if (Math.abs(lastLap.dur - personalBest) < 0.005) {
      lastLapBadge = 'badge-green';
    } else {
      lastLapBadge = 'badge-yellow';
    }
  }

  let bestLapStr = '-';
  let bestLapBadge = 'badge-dim';
  if (personalBest < Infinity) {
    bestLapStr = formatLapTime(personalBest);
    if (sessionBests && Math.abs(personalBest - sessionBests.bestLap.val) < 0.005) {
      bestLapBadge = 'badge-purple';
    } else {
      bestLapBadge = 'badge-green';
    }
  }

  return { lastLapStr, lastLapBadge, bestLapStr, bestLapBadge, personalBest };
}

function formatLapTime(sec) {
  if (!sec || sec === Infinity || sec <= 0) return '-';
  const m = Math.floor(sec / 60);
  const s = (sec % 60).toFixed(3);
  return m > 0 ? (m + ':' + (s < 10 ? '0' : '') + s) : s;
}

function renderBadge(text, badgeClass) {
  if (!text || text === '-') return '<span class="dash-muted">-</span>';
  return '<span class="f1-badge ' + badgeClass + '">' + text + '</span>';
}

function getDriverSectorTimes(drvKey, t, sessionBests) {
  const laps = DRIVER_LAPS[drvKey] || [];
  const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= t && l.dur && l.dur > 50);

  let pbS1 = Infinity, pbS2 = Infinity, pbS3 = Infinity;
  completedLaps.forEach(l => {
    if (l.s1 && l.s1 < pbS1) pbS1 = l.s1;
    if (l.s2 && l.s2 < pbS2) pbS2 = l.s2;
    if (l.s3 && l.s3 < pbS3) pbS3 = l.s3;
  });

  const targetLap = completedLaps.length > 0 ? completedLaps[completedLaps.length - 1] : (laps[0] || null);
  if (!targetLap) {
    return {
      lapNum: '-',
      s1: { str: '-', badge: 'badge-dim' },
      s2: { str: '-', badge: 'badge-dim' },
      s3: { str: '-', badge: 'badge-dim' }
    };
  }

  function formatSecBadge(val, pbVal, sessBestVal) {
    if (!val || val <= 0) return { str: '-', badge: 'badge-dim' };
    const str = val.toFixed(3);
    let badge = 'badge-yellow';
    if (sessBestVal && sessBestVal.val < Infinity && Math.abs(val - sessBestVal.val) < 0.005) {
      badge = 'badge-purple';
    } else if (pbVal < Infinity && Math.abs(val - pbVal) < 0.005) {
      badge = 'badge-green';
    }
    return { str, badge };
  }

  return {
    lapNum: targetLap.lap || '-',
    s1: formatSecBadge(targetLap.s1, pbS1, sessionBests ? sessionBests.bestS1 : null),
    s2: formatSecBadge(targetLap.s2, pbS2, sessionBests ? sessionBests.bestS2 : null),
    s3: formatSecBadge(targetLap.s3, pbS3, sessionBests ? sessionBests.bestS3 : null)
  };
}

// ══════════════════════════════════════════════════════════════
// TIMING TABLES UPDATE:
// View 1 (Full Standings) & View 2 (Compact Side Standings)
// ══════════════════════════════════════════════════════════════
function updateTimingTablesRace() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const ranked = driverKeys.map(k => {
    const d = DRIVERS[k];
    const prog = getDriverProgress(k, currentSecond);
    const gridIndex = GRID_ORDER.indexOf(k) + 1;
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const curLap = getDriverLap(k, currentSecond);
    const completedStops = PIT_STOPS.filter(p => p.driver === k && currentSecond >= p.endSec).length;
    const isCurrentStop = PIT_STOPS.some(p => p.driver === k && currentSecond >= p.startSec && currentSecond <= p.endSec);

    return {
      key: k,
      driver: d,
      totalProgress: prog,
      inPit: state.inPit,
      isRetired: state.isRetired,
      retiredLap: RETIREMENTS[k]?.lap,
      tireInfo: tireInfo,
      curLap: curLap,
      gridPos: gridIndex,
      stats: stats,
      state: state,
      pitCount: completedStops + (isCurrentStop ? 1 : 0)
    };
  }).sort((a, b) => b.totalProgress - a.totalProgress);

  const leaderProgress = ranked[0].totalProgress;
  const leaderLap = ranked[0].curLap;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;
    const posDelta = r.gridPos - currentPos;

    let deltaHtml = '<span class="delta-none">-</span>';
    if (!r.isRetired && posDelta > 0) {
      deltaHtml = '<span class="delta-up">▲' + posDelta + '</span>';
    } else if (!r.isRetired && posDelta < 0) {
      deltaHtml = '<span class="delta-down">▼' + Math.abs(posDelta) + '</span>';
    }

    // Gap to leader (CON GESTIONE DOPPIATI +X LAP)
    let gapStr;
    if (r.isRetired) {
      gapStr = '<span class="dnf-badge">DNF (L' + r.retiredLap + ')</span>';
    } else if (idx === 0) {
      gapStr = 'LEADER';
    } else {
      const lapDiff = Math.max(0, leaderLap - r.curLap);
      const diff = leaderProgress - r.totalProgress;
      const lapsBehind = Math.max(lapDiff, Math.floor(diff / LAP_DURATION));

      if (lapsBehind >= 1) {
        gapStr = '+' + lapsBehind + ' LAP';
      } else {
        gapStr = '+' + diff.toFixed(3) + 's';
      }
    }

    // Interval to car ahead
    let intStr = '-';
    if (idx === 0) {
      intStr = '-';
    } else if (r.isRetired) {
      intStr = 'DNF';
    } else {
      const prev = ranked[idx - 1];
      if (prev.isRetired) {
        intStr = '-';
      } else {
        const intDiff = prev.totalProgress - r.totalProgress;
        const prevLapDiff = Math.max(0, prev.curLap - r.curLap);
        const intLaps = Math.max(prevLapDiff, Math.floor(intDiff / LAP_DURATION));
        if (intLaps >= 1) {
          intStr = '+' + intLaps + ' LAP';
        } else {
          intStr = '+' + Math.max(0.001, intDiff).toFixed(3) + 's';
        }
      }
    }

    // Tyre badge HTML
    const tInfo = r.tireInfo;
    const tyreBadgeHtml = '<div class="tyre-badge-box">'
      + '<span class="tyre-badge tyre-' + tInfo.code + '">' + tInfo.code + '</span>'
      + '<span class="tyre-age">' + tInfo.age + 'g</span>'
      + '</div>';

    const rowRetiredClass = r.isRetired ? 'row-retired ' : '';
    const gapColorClass = idx === 0 ? 'gap-leader' : (r.isRetired ? 'gap-retired' : '');

    // 1. Build View 1 (Full Standings: NO FULL NAMES, COLUMNS: DISTACCO, INTERVALLO, ULTIMO, MIGLIOR, GOMMA, PIT)
    fullHtml += '<tr class="' + rowRetiredClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="delta-cell">' + deltaHtml + '</td>'
      + '<td class="driver-cell-full"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td class="gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-count-cell">' + r.pitCount + '</td>'
      + '</tr>';

    if (r.key === expandedDriverKey) {
      const secTimes = getDriverSectorTimes(r.key, currentSecond, sessionBests);
      fullHtml += '<tr class="sector-accordion-row">'
        + '<td colspan="9" class="sector-accordion-cell">'
        + '<div class="sector-accordion-card">'
        + '<div class="sec-card-header">'
        + '<span>⏱️ INTERTEMPI SETTORI — ' + d.code + ' (GIRO ' + secTimes.lapNum + ')</span>'
        + '</div>'
        + '<div class="sec-card-sectors">'
        + '<div class="sec-box"><span class="sec-label">S1:</span> ' + renderBadge(secTimes.s1.str, secTimes.s1.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S2:</span> ' + renderBadge(secTimes.s2.str, secTimes.s2.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S3:</span> ' + renderBadge(secTimes.s3.str, secTimes.s3.badge) + '</div>'
        + '</div>'
        + '</div>'
        + '</td>'
        + '</tr>';
    }

    // 2. Build View 2 (Compact Side Standings: EYE BUTTON, POS, +/-, PILOTA, DISTACCO, INTERVALLO, ULTIMO, MIGLIORE, GOMMA)
    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    compactHtml += '<tr class="' + rowRetiredClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \'' + r.key + '\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="delta-cell">' + deltaHtml + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="col-side-int int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTablesFP() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const ranked = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const laps = DRIVER_LAPS[k] || [];
    const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= currentSecond && l.dur && l.dur > 50).length;

    return {
      key: k,
      driver: d,
      personalBest: stats.personalBest,
      stats: stats,
      state: state,
      tireInfo: tireInfo,
      lapsCount: completedLaps
    };
  }).sort((a, b) => {
    if (a.personalBest < Infinity && b.personalBest < Infinity) {
      return a.personalBest - b.personalBest;
    }
    if (a.personalBest < Infinity) return -1;
    if (b.personalBest < Infinity) return 1;
    return parseInt(a.driver.number || '99', 10) - parseInt(b.driver.number || '99', 10);
  });

  const leaderBest = (ranked.length > 0 && ranked[0].personalBest < Infinity) ? ranked[0].personalBest : null;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;

    let gapStr = '-';
    let intStr = '-';
    if (r.personalBest < Infinity && leaderBest < Infinity) {
      if (idx === 0) {
        gapStr = 'LEADER';
        intStr = '-';
      } else {
        gapStr = '+' + (r.personalBest - leaderBest).toFixed(3) + 's';
        const prev = ranked[idx - 1];
        if (prev && prev.personalBest < Infinity) {
          intStr = '+' + Math.max(0.001, r.personalBest - prev.personalBest).toFixed(3) + 's';
        }
      }
    }

    let statusBadgeHtml = '';
    if (r.state.isRetired) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">STOP</span>';
    } else if (r.state.inPit) {
      statusBadgeHtml = '<span class="status-badge status-inpit">IN PIT</span>';
    } else if (r.state.isOutLapMerge) {
      statusBadgeHtml = '<span class="status-badge status-outlap">OUT-LAP</span>';
    } else {
      statusBadgeHtml = '<span class="status-badge status-ontrack">IN PISTA</span>';
    }

    const tInfo = r.tireInfo;
    const tyreBadgeHtml = '<div class="tyre-badge-box">'
      + '<span class="tyre-badge tyre-' + tInfo.code + '">' + tInfo.code + '</span>'
      + '<span class="tyre-age">' + tInfo.age + 'g</span>'
      + '</div>';

    const gapColorClass = idx === 0 ? 'gap-leader' : '';

    fullHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="driver-cell-full"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td class="gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-count-cell">' + r.lapsCount + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';

    if (r.key === expandedDriverKey) {
      const secTimes = getDriverSectorTimes(r.key, currentSecond, sessionBests);
      fullHtml += '<tr class="sector-accordion-row">'
        + '<td colspan="9" class="sector-accordion-cell">'
        + '<div class="sector-accordion-card">'
        + '<div class="sec-card-header">'
        + '<span>⏱️ INTERTEMPI SETTORI — ' + d.code + ' (GIRO ' + secTimes.lapNum + ')</span>'
        + '</div>'
        + '<div class="sec-card-sectors">'
        + '<div class="sec-box"><span class="sec-label">S1:</span> ' + renderBadge(secTimes.s1.str, secTimes.s1.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S2:</span> ' + renderBadge(secTimes.s2.str, secTimes.s2.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S3:</span> ' + renderBadge(secTimes.s3.str, secTimes.s3.badge) + '</div>'
        + '</div>'
        + '</div>'
        + '</td>'
        + '</tr>';
    }

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    compactHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \'' + r.key + '\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTablesQ() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const q1Cut = SESSION_DURATION * 0.35;
  const q2Cut = SESSION_DURATION * 0.70;
  let activeQ = 'Q1';
  if (sessionPart === 'Q3' || (sessionPart === '' && currentSecond >= q2Cut)) {
    activeQ = 'Q3';
  } else if (sessionPart === 'Q2' || (sessionPart === '' && currentSecond >= q1Cut)) {
    activeQ = 'Q2';
  }

  const qDrivers = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const laps = DRIVER_LAPS[k] || [];
    const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= currentSecond && l.dur && l.dur > 50);

    const q1Laps = completedLaps.filter(l => l.startSec < q1Cut);
    const q2Laps = completedLaps.filter(l => l.startSec >= q1Cut && l.startSec < q2Cut);
    const q3Laps = completedLaps.filter(l => l.startSec >= q2Cut);

    const q1Best = q1Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const q2Best = q2Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const q3Best = q3Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const overallBest = stats.personalBest;

    return {
      key: k,
      driver: d,
      q1Best: q1Best < Infinity ? q1Best : (completedLaps.length > 0 ? completedLaps[0].dur : Infinity),
      q2Best: q2Best,
      q3Best: q3Best,
      overallBest: overallBest,
      stats: stats,
      state: state,
      tireInfo: tireInfo,
      lapsCount: completedLaps.length
    };
  });

  const q1Sorted = [...qDrivers].sort((a, b) => a.q1Best - b.q1Best);

  let ranked = [];
  if (activeQ === 'Q1') {
    ranked = q1Sorted;
  } else if (activeQ === 'Q2') {
    const top15 = q1Sorted.slice(0, 15).sort((a, b) => {
      const aVal = a.q2Best < Infinity ? a.q2Best : a.q1Best;
      const bVal = b.q2Best < Infinity ? b.q2Best : b.q1Best;
      return aVal - bVal;
    });
    const elimQ1 = q1Sorted.slice(15);
    ranked = [...top15, ...elimQ1];
  } else {
    const q2Sorted = q1Sorted.slice(0, 15).sort((a, b) => {
      const aVal = a.q2Best < Infinity ? a.q2Best : a.q1Best;
      const bVal = b.q2Best < Infinity ? b.q2Best : b.q1Best;
      return aVal - bVal;
    });
    const top10 = q2Sorted.slice(0, 10).sort((a, b) => {
      const aVal = a.q3Best < Infinity ? a.q3Best : (a.q2Best < Infinity ? a.q2Best : a.q1Best);
      const bVal = b.q3Best < Infinity ? b.q3Best : (b.q2Best < Infinity ? b.q2Best : b.q1Best);
      return aVal - bVal;
    });
    const elimQ2 = q2Sorted.slice(10);
    const elimQ1 = q1Sorted.slice(15);
    ranked = [...top10, ...elimQ2, ...elimQ1];
  }

  let bestQ1Val = Infinity, bestQ2Val = Infinity, bestQ3Val = Infinity;
  ranked.forEach(r => {
    if (r.q1Best < bestQ1Val) bestQ1Val = r.q1Best;
    if (r.q2Best < bestQ2Val) bestQ2Val = r.q2Best;
    if (r.q3Best < bestQ3Val) bestQ3Val = r.q3Best;
  });

  const leaderTime = (ranked[0] && ranked[0].overallBest < Infinity) ? ranked[0].overallBest : null;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;

    let isEliminated = false;
    if (activeQ === 'Q1' && currentPos > 15) {
      isEliminated = true;
    } else if (activeQ === 'Q2' && currentPos > 10) {
      isEliminated = true;
    } else if (activeQ === 'Q3' && currentPos > 10) {
      isEliminated = true;
    }

    let gapStr = '-';
    if (r.overallBest < Infinity && leaderTime < Infinity) {
      if (idx === 0) {
        gapStr = 'LEADER';
      } else {
        gapStr = '+' + (r.overallBest - leaderTime).toFixed(3) + 's';
      }
    }

    let statusBadgeHtml = '';
    if (isEliminated) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">OUT</span>';
    } else if (r.state.isRetired) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">STOP</span>';
    } else if (r.state.inPit) {
      statusBadgeHtml = '<span class="status-badge status-inpit">IN PIT</span>';
    } else if (r.state.isOutLapMerge) {
      statusBadgeHtml = '<span class="status-badge status-outlap">OUT-LAP</span>';
    } else {
      statusBadgeHtml = '<span class="status-badge status-flying">IN PISTA</span>';
    }

    function formatQBadge(val, bestVal) {
      if (!val || val === Infinity) return '<span class="dash-muted">-</span>';
      const isPurple = Math.abs(val - bestVal) < 0.005;
      return renderBadge(formatLapTime(val), isPurple ? 'badge-purple' : 'badge-green');
    }

    const q1Html = formatQBadge(r.q1Best, bestQ1Val);
    const q2Html = (currentPos <= 15 || r.q2Best < Infinity) ? formatQBadge(r.q2Best, bestQ2Val) : '<span class="dash-muted">-</span>';
    const q3Html = (currentPos <= 10 || r.q3Best < Infinity) ? formatQBadge(r.q3Best, bestQ3Val) : '<span class="dash-muted">-</span>';

    const tInfo = r.tireInfo;
    const tyreBadgeHtml = '<div class="tyre-badge-box">'
      + '<span class="tyre-badge tyre-' + tInfo.code + '">' + tInfo.code + '</span>'
      + '<span class="tyre-age">' + tInfo.age + 'g</span>'
      + '</div>';

    const rowElimClass = isEliminated ? 'row-eliminated ' : '';
    const gapColorClass = idx === 0 ? 'gap-leader' : '';

    if (activeQ === 'Q1' && idx === 15) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">⚠️ ZONA ELIMINAZIONE Q1 (P16 - P22)</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">⚠️ ZONA ELIMINAZIONE Q1</td></tr>';
    } else if (activeQ === 'Q2' && idx === 10) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">⚠️ ZONA ELIMINAZIONE Q2 (P11 - P15)</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">⚠️ ZONA ELIMINAZIONE Q2</td></tr>';
    } else if (activeQ === 'Q2' && idx === 15) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">🛑 ELIMINATI IN Q1</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">🛑 ELIMINATI IN Q1</td></tr>';
    } else if (activeQ === 'Q3' && idx === 10) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">🛑 ELIMINATI IN Q2</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">🛑 ELIMINATI IN Q2</td></tr>';
    } else if (activeQ === 'Q3' && idx === 15) {
      fullHtml += '<tr class="elim-zone-separator"><td colspan="9">🛑 ELIMINATI IN Q1</td></tr>';
      compactHtml += '<tr class="elim-zone-separator"><td colspan="8">🛑 ELIMINATI IN Q1</td></tr>';
    }

    fullHtml += '<tr class="' + rowElimClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="driver-cell-full"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + q1Html + '</td>'
      + '<td>' + q2Html + '</td>'
      + '<td>' + q3Html + '</td>'
      + '<td class="gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-count-cell">' + r.lapsCount + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';

    if (r.key === expandedDriverKey) {
      const secTimes = getDriverSectorTimes(r.key, currentSecond, sessionBests);
      fullHtml += '<tr class="sector-accordion-row">'
        + '<td colspan="9" class="sector-accordion-cell">'
        + '<div class="sector-accordion-card">'
        + '<div class="sec-card-header">'
        + '<span>⏱️ INTERTEMPI SETTORI — ' + d.code + ' (GIRO ' + secTimes.lapNum + ')</span>'
        + '</div>'
        + '<div class="sec-card-sectors">'
        + '<div class="sec-box"><span class="sec-label">S1:</span> ' + renderBadge(secTimes.s1.str, secTimes.s1.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S2:</span> ' + renderBadge(secTimes.s2.str, secTimes.s2.badge) + '</div>'
        + '<div class="sec-box"><span class="sec-label">S3:</span> ' + renderBadge(secTimes.s3.str, secTimes.s3.badge) + '</div>'
        + '</div>'
        + '</div>'
        + '</td>'
        + '</tr>';
    }

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    const displayTimeStr = r.overallBest < Infinity ? formatLapTime(r.overallBest) : '-';
    const displayBadge = (leaderTime && Math.abs(r.overallBest - leaderTime) < 0.005) ? 'badge-purple' : 'badge-green';

    compactHtml += '<tr class="' + rowElimClass + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \'' + r.key + '\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(displayTimeStr, displayBadge) + '</td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTables() {
  if (sessionType === 'practice') {
    updateTimingTablesFP();
  } else if (sessionType === 'qualifying') {
    updateTimingTablesQ();
  } else {
    updateTimingTablesRace();
  }
}

function onDriverRowClick(drvKey) {
  expandedDriverKey = (expandedDriverKey === drvKey) ? null : drvKey;
  focusedDriver = drvKey;
  selectedDriver1 = drvKey;
  const s1 = document.getElementById('drv1Select');
  if (s1) s1.value = drvKey;
  updateCockpit(1, selectedDriver1);
  updateTimingTables();
  drawTrack();
}

function onEyeButtonClick(event, drvKey) {
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }

  // Se cliccato nuovamente sul bottone dell'occhio attualmente attivo, torna alla vista globale
  if (isMapFollowingDriver && focusedDriver === drvKey) {
    resetMapFocus();
    return;
  }

  // Passa in stato "selected" con focus sul placeholder del pilota
  focusedDriver = drvKey;
  selectedDriver1 = drvKey;
  isMapFollowingDriver = true;
  userZoom = 2.4;

  const resetBtn = document.getElementById('btnResetMapFocus');
  if (resetBtn) resetBtn.style.display = 'inline-flex';

  updateZoomButtonsState();
  centerMapOnDriver(drvKey);

  const s1 = document.getElementById('drv1Select');
  if (s1) s1.value = drvKey;
  updateCockpit(1, drvKey);

  updateTimingTables();
  drawTrack();
}
window.onEyeButtonClick = onEyeButtonClick;

function resetMapFocus() {
  isMapFollowingDriver = false;
  focusedDriver = null;
  userZoom = 1.0;
  userPanX = 0;
  userPanY = 0;

  const resetBtn = document.getElementById('btnResetMapFocus');
  if (resetBtn) resetBtn.style.display = 'none';

  updateZoomButtonsState();
  updateZoomPill();
  updateTimingTables();
  drawTrack();
}
window.resetMapFocus = resetMapFocus;

function centerMapOnDriver(drvKey) {
  const state = getDriverVisualState(drvKey, currentSecond);
  if (!state) return;
  const baseX = canvasOffsetX + state.px * canvasScale;
  const baseY = canvasOffsetY + state.py * canvasScale;
  const cx = (canvasCssWidth || 400) / 2;
  const cy = (canvasCssHeight || 300) / 2;
  userPanX = -(baseX - cx) * userZoom;
  userPanY = -(baseY - cy) * userZoom;
}

// ══════════════════════════════════════════════════════════════
// DUAL COCKPIT TELEMETRY
// ══════════════════════════════════════════════════════════════
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

function updateCockpit(idx, drvKey) {
  const drv = DRIVERS[drvKey];
  if (!drv) return;

  const state = getDriverVisualState(drvKey, currentSecond);
  const tireInfo = getDriverTireInfo(drvKey, currentSecond);
  const p = 'c' + idx;

  // Driver info
  const dot = document.getElementById(p + 'ColorDot');
  if (dot) dot.style.background = drv.color;
  const title = document.getElementById(p + 'Title');
  if (title) title.textContent = '#' + drv.number + ' ' + drv.code + ' — ' + drv.firstName + ' ' + drv.lastName;
  const team = document.getElementById(p + 'Team');
  if (team) team.textContent = drv.team + ' [' + tireInfo.code + ']';

  // Speed & Gear
  const spdEl = document.getElementById(p + 'Speed');
  if (spdEl) spdEl.textContent = Math.round(state.speed);
  const gearEl = document.getElementById(p + 'Gear');
  if (gearEl) gearEl.textContent = state.isRetired ? 'DNF' : (state.gear === 0 ? 'N' : state.gear);

  // RPM
  const rpmEl = document.getElementById(p + 'Rpm');
  if (rpmEl) rpmEl.textContent = Math.round(state.rpm).toLocaleString() + ' RPM';

  // Shift LEDs
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

  // Throttle & Brake
  const thrBar = document.getElementById(p + 'ThrBar');
  const thrVal = document.getElementById(p + 'ThrVal');
  if (thrBar) thrBar.style.width = Math.round(state.throttle) + '%';
  if (thrVal) thrVal.textContent = Math.round(state.throttle) + '%';

  const brkBar = document.getElementById(p + 'BrkBar');
  const brkVal = document.getElementById(p + 'BrkVal');
  if (brkBar) brkBar.style.width = Math.round(state.brake) + '%';
  if (brkVal) brkVal.textContent = Math.round(state.brake) + '%';

  // 2026 Active Aero
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

  // ERS Battery SoC%
  const socVal = document.getElementById(p + 'SocVal');
  const socBar = document.getElementById(p + 'SocBar');
  const curLap = getDriverLap(drvKey, currentSecond);
  const estSoc = Math.max(25, Math.min(98, 92 - ((curLap * 3) % 45) + (state.brake > 50 ? 12 : -5)));
  if (socVal) socVal.textContent = Math.round(estSoc) + '%';
  if (socBar) socBar.style.width = Math.round(estSoc) + '%';

  // 2026 Manual Override / Boost Mode
  const boostBadge = document.getElementById(p + 'BoostBadge');
  if (boostBadge) {
    const gap = getDriverGap(drvKey, currentSecond);
    if (gap > 0 && gap < 1.0 && estSoc > 35 && state.throttle > 90) {
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

  // Location
  const locEl = document.getElementById(p + 'LocText');
  if (locEl) locEl.textContent = '📍 ' + (state.location || 'Settore di Pista');
}

function onDriverSelectChange(idx, val) {
  if (idx === 1) selectedDriver1 = val;
  if (idx === 2) selectedDriver2 = val;
  updateCockpit(idx, val);
  updateTimingTables();
  drawTrack();
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
  updateTimingTables();
  drawTrack();
}

// ══════════════════════════════════════════════════════════════
// RACE CONTROL & INCIDENT LOGS
// ══════════════════════════════════════════════════════════════
function updateRaceControl() {
  const activeMsgs = RACE_CONTROL_MESSAGES.filter(m => (m.timeSec || 0) <= currentSecond);
  const totalCount = document.getElementById('rcTotalCount');
  if (totalCount) totalCount.textContent = activeMsgs.length + ' Messaggi';

  const latest = activeMsgs.length > 0 ? activeMsgs[activeMsgs.length - 1] : null;
  const tickerText = latest
    ? ('🟡 ' + formatSimClock(latest.timeSec || currentSecond) + ' — ' + (latest.message || 'Direzione Gara'))
    : '🟢 08:33:00 — Inizio Sessione Ufficiale';

  const sTicker = document.getElementById('sideRcTickerCard');
  const fTicker = document.getElementById('fullRcTickerCard');
  if (sTicker) sTicker.textContent = tickerText;
  if (fTicker) fTicker.textContent = tickerText;

  const fList = document.getElementById('fullRcFeedList');
  const sList = document.getElementById('sideRcFeedList');
  if (!fList && !sList) return;

  const filtered = activeMsgs.filter(m => {
    if (activeRcFilter === 'ALL') return true;
    if (activeRcFilter === 'Flag') return m.flag || m.category === 'Flag';
    if (activeRcFilter === 'SafetyCar') return (m.category === 'SafetyCar') || (m.message && m.message.includes('SAFETY CAR'));
    if (activeRcFilter === 'Investigation') return (m.category === 'Investigation') || (m.message && m.message.includes('INVESTIGAT'));
    return true;
  }).reverse();

  const itemsHtml = filtered.slice(0, 40).map(m => {
    let flagClass = '';
    if (m.flag === 'YELLOW' || (m.message && m.message.includes('YELLOW'))) flagClass = 'yellow';
    else if (m.flag === 'GREEN' || (m.message && m.message.includes('GREEN'))) flagClass = 'green';
    else if (m.flag === 'RED' || (m.message && m.message.includes('RED'))) flagClass = 'red';
    else if (m.category === 'SafetyCar' || (m.message && m.message.includes('SAFETY CAR'))) flagClass = 'sc';

    return '<div class="rc-msg-card ' + flagClass + '">'
      + '<div class="rc-msg-meta">'
      + '<span>' + (m.category || 'DIREZIONE GARA') + (m.lap ? (' • GIRO ' + m.lap) : '') + '</span>'
      + '<span>' + formatSimClock(m.timeSec || currentSecond) + '</span>'
      + '</div>'
      + '<div class="rc-msg-text">' + m.message + '</div>'
      + '</div>';
  }).join('');

  if (fList) fList.innerHTML = itemsHtml;
  if (sList) sList.innerHTML = itemsHtml;
}

function setRcFilter(cat) {
  activeRcFilter = cat;
  document.querySelectorAll('.rc-filter-btn').forEach(b => b.classList.remove('active'));
  const activeBtn = document.getElementById({
    ALL: 'rcFilterAll',
    Flag: 'rcFilterFlags',
    SafetyCar: 'rcFilterSC',
    Investigation: 'rcFilterInv'
  }[cat]);
  if (activeBtn) activeBtn.classList.add('active');
  updateRaceControl();
}

// ══════════════════════════════════════════════════════════════
// PERSISTENT TOP STATUS BAR UPDATE
// ══════════════════════════════════════════════════════════════
function updateSessionStatusBar() {
  const scActive = isSafetyCarActive(currentSecond);
  const vscActive = isVirtualSafetyCarActive(currentSecond);

  const flagBadge = document.getElementById('statusBarFlag');
  const flagIcon = document.getElementById('statusBarFlagIcon');
  const flagText = document.getElementById('statusBarFlagText');

  if (flagBadge && flagText && flagIcon) {
    flagBadge.className = 'status-bar-flag-badge';
    if (scActive) {
      flagBadge.classList.add('flag-sc');
      flagIcon.textContent = '🟠';
      flagText.textContent = 'SAFETY CAR';
    } else if (vscActive) {
      flagBadge.classList.add('flag-vsc');
      flagIcon.textContent = '🟡';
      flagText.textContent = 'VIRTUAL SAFETY CAR';
    } else if (currentSecond >= RACE_FINISH_SEC) {
      flagBadge.classList.add('flag-chequered');
      flagIcon.textContent = '🏁';
      flagText.textContent = 'BANDIERA A SCACCHI';
    } else {
      flagBadge.classList.add('flag-green');
      flagIcon.textContent = '🟢';
      flagText.textContent = 'BANDIERA VERDE';
    }
  }

  const trackFlagBadge = document.getElementById('trackFlagBadge');
  if (trackFlagBadge) {
    if (scActive) {
      trackFlagBadge.textContent = 'SAFETY CAR';
      trackFlagBadge.className = 'status-pill sc';
    } else if (vscActive) {
      trackFlagBadge.textContent = 'VSC ATTIVA';
      trackFlagBadge.className = 'status-pill yellow';
    } else {
      trackFlagBadge.textContent = 'BANDIERA VERDE';
      trackFlagBadge.className = 'status-pill green';
    }
  }

  // Session Type Label
  const sessVal = document.getElementById('statusBarSessionVal');
  if (sessVal) {
    if (sessionType === 'practice') {
      sessVal.textContent = sessionPart || 'PROVE LIBERE';
    } else if (sessionType === 'qualifying') {
      sessVal.textContent = sessionPart || 'QUALIFICHE';
    } else {
      sessVal.textContent = 'GARA';
    }
  }

  // Session Progress / Laps
  const lapVal = document.getElementById('statusBarLapVal');
  if (lapVal) {
    if (sessionType === 'practice') {
      lapVal.textContent = 'SESSIONE ATTIVA';
    } else if (sessionType === 'qualifying') {
      const q1Cut = SESSION_DURATION * 0.35;
      const q2Cut = SESSION_DURATION * 0.70;
      let qPhase = 'Q1';
      if (sessionPart) qPhase = sessionPart;
      else if (currentSecond >= q2Cut) qPhase = 'Q3';
      else if (currentSecond >= q1Cut) qPhase = 'Q2';
      lapVal.textContent = qPhase + ' ATTIVA';
    } else {
      if (currentSim.circuitId === 'sepang' && currentSecond < 216.9) {
        lapVal.textContent = 'GIRI FORMAZIONE';
      } else {
        const curLap = getDriverLap(focusedDriver, currentSecond);
        lapVal.textContent = 'GIRO ' + curLap + '/' + TOTAL_LAPS;
      }
    }
  }

  // UTC Clock
  const clockVal = document.getElementById('statusBarClockVal');
  if (clockVal) clockVal.textContent = formatSimClock(currentSecond) + ' UTC';
}

function formatSimClock(sec) {
  const baseHour = currentSim.circuitId === 'baku' ? 11 : 8;
  const baseMin = currentSim.circuitId === 'baku' ? 3 : 33;
  const totalSec = Math.floor(sec) + (baseHour * 3600) + (baseMin * 60);
  const h = Math.floor(totalSec / 3600) % 24;
  const m = Math.floor((totalSec % 3600) / 60);
  const s = totalSec % 60;
  return (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
}

// ══════════════════════════════════════════════════════════════
// CANVAS TRACK MAP RENDERING
// ══════════════════════════════════════════════════════════════
let canvas, ctx;
let canvasScale = 1.0;
let canvasOffsetX = 0;
let canvasOffsetY = 0;
let canvasCssWidth = 0;
let canvasCssHeight = 0;

let userZoom = 1.0;
let userPanX = 0;
let userPanY = 0;

function resizeTrackCanvas() {
  const pane = document.getElementById('trackPane');
  if (!pane || !canvas) return;

  const rect = pane.getBoundingClientRect();
  const dpr = window.devicePixelRatio || 1;
  const w = Math.floor(rect.width);
  const h = Math.floor(rect.height);

  if (w <= 0 || h <= 0) return;

  canvasCssWidth = w;
  canvasCssHeight = h;

  canvas.width = w * dpr;
  canvas.height = h * dpr;
  canvas.style.width = w + 'px';
  canvas.style.height = h + 'px';

  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.scale(dpr, dpr);

  const margin = 16;
  const availW = w - margin * 2;
  const availH = h - margin * 2;
  canvasScale = Math.min(availW / SVG_WIDTH, availH / SVG_HEIGHT);
  canvasOffsetX = margin + (availW - SVG_WIDTH * canvasScale) / 2;
  canvasOffsetY = margin + (availH - SVG_HEIGHT * canvasScale) / 2;
}

function pxToScreen(px, py) {
  const baseX = canvasOffsetX + px * canvasScale;
  const baseY = canvasOffsetY + py * canvasScale;
  const cx = (canvasCssWidth || 400) / 2;
  const cy = (canvasCssHeight || 300) / 2;
  return {
    x: cx + (baseX - cx) * userZoom + userPanX,
    y: cy + (baseY - cy) * userZoom + userPanY
  };
}

function updateZoomPill() {
  const pill = document.getElementById('zoomLevelPill');
  if (pill) {
    pill.innerText = userZoom.toFixed(1) + 'x';
    pill.style.color = userZoom > 1.05 ? '#58a6ff' : '#8b949e';
  }
}

function updateZoomButtonsState() {
  const badge = document.getElementById('mapZoomVerticalBadge');
  if (badge) {
    badge.style.display = isMapFollowingDriver ? 'flex' : 'none';
  }
  const btnIn = document.getElementById('btnMapZoomIn');
  const btnOut = document.getElementById('btnMapZoomOut');
  if (btnIn) {
    if (userZoom >= 4.95) {
      btnIn.classList.add('disabled');
    } else {
      btnIn.classList.remove('disabled');
    }
  }
  if (btnOut) {
    if (userZoom <= 1.05) {
      btnOut.classList.add('disabled');
    } else {
      btnOut.classList.remove('disabled');
    }
  }
}
window.updateZoomButtonsState = updateZoomButtonsState;

function zoomMapStep(direction) {
  if (direction > 0) {
    userZoom = Math.min(5.0, userZoom + 0.6);
  } else {
    userZoom = Math.max(1.0, userZoom - 0.6);
  }
  if (userZoom <= 1.05) {
    userZoom = 1.0;
    userPanX = 0;
    userPanY = 0;
  }
  updateZoomButtonsState();
  updateZoomPill();
  drawTrack();
}
window.zoomMapStep = zoomMapStep;

function zoomMap(factor) {
  userZoom = Math.min(5.0, Math.max(1.0, userZoom * factor));
  if (userZoom <= 1.01) {
    userZoom = 1.0;
    userPanX = 0;
    userPanY = 0;
  }
  updateZoomButtonsState();
  updateZoomPill();
  drawTrack();
}
window.zoomMap = zoomMap;

function resetMapZoom() {
  userZoom = 1.0;
  userPanX = 0;
  userPanY = 0;
  updateZoomButtonsState();
  updateZoomPill();
  drawTrack();
}
window.resetMapZoom = resetMapZoom;

function setupTrackMapInteractions() {
  const cvs = document.getElementById('trackCanvas');
  if (!cvs) return;

  let initialTouchDist = null;
  let initialZoom = 1.0;
  let lastTouchPos = null;
  let lastTapTime = 0;
  let isDragging = false;
  let lastMousePos = null;

  // Touch handlers: pinch-to-zoom con due dita e pan con un dito se ingrandito
  cvs.addEventListener('touchstart', function(e) {
    if (e.touches.length === 2) {
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      initialTouchDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      initialZoom = userZoom;
    } else if (e.touches.length === 1) {
      const now = Date.now();
      if (now - lastTapTime < 300) {
        // Doppio tap: toggle rapido tra 1.0x e 2.2x
        if (userZoom > 1.05) {
          resetMapFocus();
        } else {
          userZoom = 2.2;
          userPanX = 0;
          userPanY = 0;
          updateZoomButtonsState();
          updateZoomPill();
          drawTrack();
        }
        lastTapTime = 0;
        return;
      }
      lastTapTime = now;
      lastTouchPos = { x: e.touches[0].clientX, y: e.touches[0].clientY };
    }
  }, { passive: false });

  cvs.addEventListener('touchmove', function(e) {
    if (e.touches.length === 2 && initialTouchDist) {
      e.preventDefault();
      const t1 = e.touches[0];
      const t2 = e.touches[1];
      const currentDist = Math.hypot(t1.clientX - t2.clientX, t1.clientY - t2.clientY);
      const scale = currentDist / initialTouchDist;
      userZoom = Math.min(5.0, Math.max(1.0, initialZoom * scale));
      if (userZoom <= 1.01) {
        userZoom = 1.0;
        userPanX = 0;
        userPanY = 0;
      }
      updateZoomButtonsState();
      updateZoomPill();
      drawTrack();
    } else if (e.touches.length === 1 && lastTouchPos) {
      if (userZoom > 1.05) {
        e.preventDefault();
        if (isMapFollowingDriver) {
          isMapFollowingDriver = false;
          updateTimingTables();
        }
        const dx = e.touches[0].clientX - lastTouchPos.x;
        const dy = e.touches[0].clientY - lastTouchPos.y;
        userPanX += dx;
        userPanY += dy;
        const maxPan = ((canvasCssWidth || 400) / 2) * (userZoom - 1.0) + 40;
        userPanX = Math.max(-maxPan, Math.min(maxPan, userPanX));
        userPanY = Math.max(-maxPan, Math.min(maxPan, userPanY));
        lastTouchPos = { x: e.touches[0].clientX, y: e.touches[0].clientY };
        drawTrack();
      }
    }
  }, { passive: false });

  cvs.addEventListener('touchend', function(e) {
    if (e.touches.length < 2) {
      initialTouchDist = null;
    }
    if (e.touches.length === 0) {
      lastTouchPos = null;
      if (userZoom <= 1.01) {
        userZoom = 1.0;
        userPanX = 0;
        userPanY = 0;
        updateZoomButtonsState();
        updateZoomPill();
        drawTrack();
      }
    }
  }, { passive: false });

  // Mouse wheel handler per test su computer desktop
  cvs.addEventListener('wheel', function(e) {
    e.preventDefault();
    const factor = e.deltaY < 0 ? 1.15 : 0.85;
    zoomMap(factor);
  }, { passive: false });

  // Mouse drag handler per desktop
  cvs.addEventListener('mousedown', function(e) {
    if (userZoom > 1.05) {
      isDragging = true;
      lastMousePos = { x: e.clientX, y: e.clientY };
    }
  });

  window.addEventListener('mousemove', function(e) {
    if (isDragging && lastMousePos && userZoom > 1.05) {
      if (isMapFollowingDriver) {
        isMapFollowingDriver = false;
        updateTimingTables();
      }
      const dx = e.clientX - lastMousePos.x;
      const dy = e.clientY - lastMousePos.y;
      userPanX += dx;
      userPanY += dy;
      const maxPan = ((canvasCssWidth || 400) / 2) * (userZoom - 1.0) + 40;
      userPanX = Math.max(-maxPan, Math.min(maxPan, userPanX));
      userPanY = Math.max(-maxPan, Math.min(maxPan, userPanY));
      lastMousePos = { x: e.clientX, y: e.clientY };
      drawTrack();
    }
  });

  window.addEventListener('mouseup', function() {
    isDragging = false;
    lastMousePos = null;
  });

  // Click / Tap sui placeholder dei piloti sul tracciato per selezionare la telemetria
  cvs.addEventListener('click', function(e) {
    if (isDragging) return;
    const rect = cvs.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    const driverKeys = Object.keys(DRIVERS);
    let closestDrv = null;
    let closestDist = 28; // Raggio hit-test in pixel

    driverKeys.forEach(k => {
      const state = getDriverVisualState(k, currentSecond);
      if ((sessionType === 'practice' || sessionType === 'qualifying') && state.inPit) {
        return;
      }
      const scr = pxToScreen(state.px, state.py);
      const dist = Math.hypot(scr.x - clickX, scr.y - clickY);
      if (dist < closestDist) {
        closestDist = dist;
        closestDrv = k;
      }
    });

    if (closestDrv) {
      onDriverRowClick(closestDrv);
    }
  });
}

function drawTrack() {
  if (!ctx || !canvas) return;

  if (isMapFollowingDriver && focusedDriver) {
    centerMapOnDriver(focusedDriver);
  }

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  // Track Outline Path
  ctx.save();
  ctx.lineJoin = 'round';
  ctx.lineCap = 'round';

  const zoomScale = Math.min(2.5, Math.sqrt(userZoom));

  // Racing line roadbed
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

  // White centerline
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

  // Start/Finish Line
  if (NODES.length > 0) {
    const sfPt = pxToScreen(NODES[0][1], NODES[0][2]);
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(sfPt.x, sfPt.y, 4 * zoomScale, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.restore();

  // Draw Safety Car Marker ONLY when actively deployed on track
  const scActive = isSafetyCarActive(currentSecond);
  if (scActive) {
    const driverKeys = Object.keys(DRIVERS);
    let leaderKey = '3';
    let maxProg = -1;
    driverKeys.forEach(k => {
      const p = getDriverProgress(k, currentSecond);
      if (p > maxProg) {
        maxProg = p;
        leaderKey = k;
      }
    });

    const leaderTrackSec = getDriverTrackSec(leaderKey, currentSecond);
    const scTrackSec = (leaderTrackSec + 1.8) % LAP_DURATION;
    const scPt = interpolateTrackNode(scTrackSec);
    const scScr = pxToScreen(scPt.px, scPt.py);

    ctx.save();
    ctx.shadowColor = '#f0883e';
    ctx.shadowBlur = 8;

    const scRadius = 6.5 * Math.min(1.5, Math.sqrt(userZoom));
    ctx.fillStyle = '#f0883e';
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(scScr.x, scScr.y, scRadius, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    ctx.shadowBlur = 0;
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold ' + Math.round(8 * Math.min(1.4, Math.sqrt(userZoom))) + 'px ui-monospace, sans-serif';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('SC', scScr.x, scScr.y);

    ctx.fillStyle = '#f0883e';
    ctx.font = 'bold ' + Math.round(7 * Math.min(1.3, Math.sqrt(userZoom))) + 'px sans-serif';
    ctx.textBaseline = 'bottom';
    ctx.fillText('SAFETY CAR', scScr.x, scScr.y - (scRadius + 2));
    ctx.restore();
  }

  // Draw 22 Drivers on track
  const driverKeys = Object.keys(DRIVERS);
  driverKeys.forEach(k => {
    const d = DRIVERS[k];
    const state = getDriverVisualState(k, currentSecond);

    // In FP and Qualifying sessions, hide placeholders of drivers who are currently in the pit
    if ((sessionType === 'practice' || sessionType === 'qualifying') && state.inPit) {
      return;
    }

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

// ══════════════════════════════════════════════════════════════
// MAIN ANIMATION LOOP (60 FPS)
// ══════════════════════════════════════════════════════════════
let lastUiUpdateTime = 0;

function animate(timestamp) {
  if (!lastAnimFrameTimestamp) lastAnimFrameTimestamp = timestamp;
  const deltaSec = (timestamp - lastAnimFrameTimestamp) / 1000;
  lastAnimFrameTimestamp = timestamp;

  if (isPlaying) {
    currentSecond += deltaSec * simSpeedMultiplier;
    if (currentSecond >= SESSION_DURATION) {
      currentSecond = 0.0;
    }
  }

  drawTrack();

  if (timestamp - lastUiUpdateTime > 100) {
    lastUiUpdateTime = timestamp;
    updateSessionStatusBar();
    updateTimingTables();
    updateCockpit(1, selectedDriver1);
    updateCockpit(2, selectedDriver2);
    updateRaceControl();
    if (typeof updateScrubber === 'function') updateScrubber();
  }

  requestAnimationFrame(animate);
}

function renderSimulationControls(isTest) {
  const container = document.getElementById('hdrRightControls');
  if (!container) return;
  if (!isTest) {
    container.innerHTML = '';
    return;
  }
  const circuit = currentCircuit || 'sepang';
  container.innerHTML = '<div class="test-switch-box" id="testSwitchBox">'
    + '<button class="test-btn ' + (circuit === 'sepang' ? 'active' : '') + '" id="btnSimSepang" onclick="switchSimulationCircuit(\'sepang\')">🇲🇾 SEPANG</button>'
    + '<button class="test-btn ' + (circuit === 'baku' ? 'active' : '') + '" id="btnSimBaku" onclick="switchSimulationCircuit(\'baku\')">🇦🇿 BAKU</button>'
    + '</div>'
    + '<div class="test-switch-box" id="sessionTypeSwitchBox">'
    + '<button class="test-btn ' + (sessionType === 'race' ? 'active' : '') + '" id="btnTypeRace" onclick="setSessionType(\'race\')">GARA</button>'
    + '<button class="test-btn ' + (sessionType === 'qualifying' ? 'active' : '') + '" id="btnTypeQual" onclick="setSessionType(\'qualifying\')">QUALIFICHE</button>'
    + '<button class="test-btn ' + (sessionType === 'practice' ? 'active' : '') + '" id="btnTypeFP" onclick="setSessionType(\'practice\')">FP</button>'
    + '</div>'
    + '<div class="speed-multiplier-box" id="speedMultiplierBox">'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 1 ? 'active' : '') + '" id="btnSpd1x" onclick="setSimSpeed(1)">1x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 2 ? 'active' : '') + '" id="btnSpd2x" onclick="setSimSpeed(2)">2x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 5 ? 'active' : '') + '" id="btnSpd5x" onclick="setSimSpeed(5)">5x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 10 ? 'active' : '') + '" id="btnSpd10x" onclick="setSimSpeed(10)">10x</button>'
    + '<button class="spd-btn ' + (simSpeedMultiplier === 30 ? 'active' : '') + '" id="btnSpd30x" onclick="setSimSpeed(30)">30x</button>'
    + '</div>';
}
window.renderSimulationControls = renderSimulationControls;

function setSimSpeed(mult) {
  simSpeedMultiplier = mult;
  ['btnSpd1x', 'btnSpd2x', 'btnSpd5x', 'btnSpd10x', 'btnSpd30x'].forEach(id => {
    const b = document.getElementById(id);
    if (b) b.classList.remove('active');
  });
  const activeBtn = document.getElementById('btnSpd' + mult + 'x');
  if (activeBtn) activeBtn.classList.add('active');
}
window.setSimSpeed = setSimSpeed;

// ══════════════════════════════════════════════════════════════
// NAVIGATION & VIEW SWITCHING
// ══════════════════════════════════════════════════════════════
function switchMainView(viewId) {
  currentMainView = viewId;
  document.querySelectorAll('.main-view').forEach(v => v.classList.remove('active'));
  document.querySelectorAll('.bottom-nav-btn').forEach(b => b.classList.remove('active'));

  const target = document.getElementById(viewId);
  if (target) target.classList.add('active');

  if (viewId === 'view-standings') {
    const b = document.getElementById('btnNavStandings');
    if (b) b.classList.add('active');
  } else if (viewId === 'view-track') {
    const b = document.getElementById('btnNavTrack');
    if (b) b.classList.add('active');
    setTimeout(resizeTrackCanvas, 60);
  } else if (viewId === 'view-race-control') {
    const b = document.getElementById('btnNavRaceControl');
    if (b) b.classList.add('active');
  }

  // Notifica il bridge nativo Android per la gestione dell'orientamento
  if (window.FastestLapBridge && typeof window.FastestLapBridge.onTabChanged === 'function') {
    try {
      window.FastestLapBridge.onTabChanged(viewId);
    } catch (ignored) {}
  }
}

function switchSideTab(tabId) {
  currentSideTab = tabId;
  document.querySelectorAll('.side-pane-content').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.side-tab-btn').forEach(b => b.classList.remove('active'));

  const target = document.getElementById(tabId);
  if (target) target.classList.add('active');

  if (tabId === 'side-standings') {
    const b = document.getElementById('btnSideStandings');
    if (b) b.classList.add('active');
  } else if (tabId === 'side-telemetry') {
    const b = document.getElementById('btnSideTelemetry');
    if (b) b.classList.add('active');
    updateCockpit(1, selectedDriver1);
    updateCockpit(2, selectedDriver2);
  } else if (tabId === 'side-rc') {
    const b = document.getElementById('btnSideRaceControl');
    if (b) b.classList.add('active');
    updateRaceControl();
  }
  updateTimingTables();
  drawTrack();
}

function closeSession() {
  if (window.FastestLapBridge && typeof window.FastestLapBridge.closeLive === 'function') {
    window.FastestLapBridge.closeLive();
  } else {
    window.history.back();
  }
}

// ══════════════════════════════════════════════════════════════
// INITIALIZATION & APP BRIDGE INTEGRATION
// ══════════════════════════════════════════════════════════════
let appInitialized = false;
function initApp() {
  if (appInitialized) return;
  appInitialized = true;

  canvas = document.getElementById('trackCanvas');
  if (canvas) ctx = canvas.getContext('2d');

  buildLedBars();

  // Check Bridge Context
  let sessionLive = false;
  let preferredCircuit = 'sepang';

  if (window.FastestLapBridge) {
    if (typeof window.FastestLapBridge.isSessionLive === 'function') {
      sessionLive = window.FastestLapBridge.isSessionLive();
    }
    if (typeof window.FastestLapBridge.getCircuitId === 'function') {
      const cid = window.FastestLapBridge.getCircuitId();
      if (cid && cid.trim().length > 0) preferredCircuit = cid.toLowerCase();
    }
    if (typeof window.FastestLapBridge.getSessionType === 'function') {
      const st = window.FastestLapBridge.getSessionType();
      if (st) sessionType = st.toLowerCase().trim();
    }
    if (typeof window.FastestLapBridge.getSessionPart === 'function') {
      const sp = window.FastestLapBridge.getSessionPart();
      if (sp) sessionPart = sp.toUpperCase().trim();
    }
  }

  const offlineView = document.getElementById('offlineView');
  const liveView = document.getElementById('liveView');

  if (!sessionLive) {
    if (offlineView) offlineView.style.display = 'flex';
    if (liveView) liveView.style.display = 'none';

    if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
      const offEvent = document.getElementById('offlineEventTitle');
      if (offEvent) offEvent.textContent = window.FastestLapBridge.getEventTitle();
    }
    if (window.FastestLapBridge && typeof window.FastestLapBridge.getSessionName === 'function') {
      const offSess = document.getElementById('offlineSessionName');
      if (offSess) offSess.textContent = window.FastestLapBridge.getSessionName();
    }
    return;
  }

  // Sessione attiva in corso
  if (offlineView) offlineView.style.display = 'none';
  if (liveView) liveView.style.display = 'flex';

  // Applica il circuito selezionato e la modalità di sessione corrispondente
  if (preferredCircuit.includes('baku') || preferredCircuit.includes('azerbaijan')) {
    applySimulation(SIM_BAKU);
  } else {
    if (sessionType === 'practice') {
      applySimulation(SIM_SEPANG_FP);
    } else if (sessionType === 'qualifying') {
      applySimulation(SIM_SEPANG_QUALIFYING);
    } else {
      applySimulation(SIM_SEPANG);
    }
  }

  if (window.FastestLapBridge && typeof window.FastestLapBridge.getEventTitle === 'function') {
    const hdrEvent = document.getElementById('hdrEventTitle');
    if (hdrEvent) {
      const bTitle = window.FastestLapBridge.getEventTitle();
      if (bTitle && bTitle.trim().length > 0 && bTitle.toLowerCase() !== 'live timing') {
        hdrEvent.textContent = bTitle;
      }
    }
  }

  renderTableHeaders(sessionType);
  updateSessionStatusBar();

  // Modalità reale: nessun controllo di test / simulazione mostrato
  renderSimulationControls(false);
  isGenericRealMode = true;

  window.addEventListener('resize', () => {
    resizeTrackCanvas();
    drawTrack();
  });

  setupTrackMapInteractions();
  resizeTrackCanvas();

  if (typeof renderSepangJumpBar === 'function') {
    renderSepangJumpBar(sessionType || 'race');
    updateScrubber();
  }

  requestAnimationFrame(animate);
}

window.onload = initApp;
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}

// Ricezione dinamica dell'immagine del circuito da Firebase/Android
window.setCircuitImage = function(url) {
  if (!url) return;
  const img = new Image();
  img.onload = function() {
    trackBgImage = img;
    drawTrack();
  };
  img.src = url;
};

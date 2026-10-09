const fs = require('fs');
const path = require('path');
const { DRIVER_LAPS } = require('./src/driver_laps');

const filePath = path.join(__dirname, 'src/race_model.js');
let content = fs.readFileSync(filePath, 'utf8');

// 1. Add require and constants
if (!content.includes('driver_laps')) {
  content = content.replace(
    "const { DRIVER_STINTS, getDriverStintInfo } = require('./driver_stints');",
    "const { DRIVER_STINTS, getDriverStintInfo } = require('./driver_stints');\nconst { DRIVER_LAPS, getDriverLapRecord } = require('./driver_laps');\n\nconst PIT_EXIT_TRACK_SEC = 5.06;\nconst PIT_ENTRY_TRACK_SEC = 89.34;"
  );
}

// 2. Parse PIT_STOPS and enrich with outLapEndSec
const oldPitStopsMatch = content.match(/const PIT_STOPS = (\[[\s\S]*?\n\];)/);
if (!oldPitStopsMatch) throw new Error("Could not find PIT_STOPS in race_model.js");

const pitStops = eval(oldPitStopsMatch[1]);
const enrichedStops = pitStops.map(p => {
  const dl = DRIVER_LAPS[p.driver] || [];
  const match = dl.find(l => p.endSec >= l.startSec && (l.dur ? p.endSec <= l.startSec + l.dur : true));
  const outEnd = match ? match.startSec + (match.dur || 80) : p.endSec + 80;
  return {
    ...p,
    outLapEndSec: Math.round(outEnd * 10) / 10
  };
});

const newPitStopsCode = `const PIT_STOPS = ${JSON.stringify(enrichedStops, null, 2)};`;
content = content.replace(oldPitStopsMatch[0], newPitStopsCode);

// 3. Replace getDriverLap and getDriverTrackSec
const oldFuncsRegex = /function getDriverLap\(drvKey, t\) \{[\s\S]*?return f \* LAP_DURATION;\n\}/;
const newFuncsCode = `function getDriverLap(drvKey, t) {
  if (isDriverRetired(drvKey, t)) {
    return RETIREMENTS[drvKey].lap;
  }
  if (t >= RACE_FINISH_SEC) return TOTAL_LAPS;
  if (t < 108.0) return 1; // Formation Lap 1 (Behind SC)
  if (t < 216.9) return 2; // Formation Lap 2 (Behind SC & Grid Lineup)
  const rec = getDriverLapRecord(drvKey, t);
  if (rec) {
    return rec.lap;
  }
  const gap = getDriverGap(drvKey, t);
  const tEff = Math.max(0, t - gap);
  for (let l = 1; l <= TOTAL_LAPS; l++) {
    if (tEff >= LAP_STARTS[l] && tEff < LAP_STARTS[l + 1]) {
      return l;
    }
  }
  return TOTAL_LAPS;
}

function getDriverTrackSec(drvKey, t) {
  if (isDriverRetired(drvKey, t)) {
    return 30.0; // Parked safely off track
  }
  if (t < 216.9) {
    // 2 Formation Laps behind Safety Car & Standing Start grid line-up
    const gridIdx = GRID_ORDER.indexOf(drvKey);
    const pos = gridIdx >= 0 ? gridIdx : 21;
    if (t < 108.0) {
      // Formation Lap 1 behind SC
      const prog = (t / 108.0) * LAP_DURATION;
      return ((prog - pos * 1.5) % LAP_DURATION + LAP_DURATION) % LAP_DURATION;
    } else if (t < 205.0) {
      // Formation Lap 2 behind SC
      const prog = ((t - 108.0) / 97.0) * LAP_DURATION;
      return ((prog - pos * 1.5) % LAP_DURATION + LAP_DURATION) % LAP_DURATION;
    } else {
      // Grid slots on main straight (around t=0 / 95s)
      return 0.0;
    }
  }
  if (t >= RACE_FINISH_SEC) {
    const finProg = Math.min(LAP_DURATION, (t - RACE_FINISH_SEC) * 0.35);
    return finProg;
  }

  // 1. Is driver currently inside the pit lane?
  const activeStop = PIT_STOPS.find(p => p.driver === drvKey && t >= p.startSec && t <= p.endSec);
  if (activeStop) {
    const frac = (t - activeStop.startSec) / Math.max(1, activeStop.endSec - activeStop.startSec);
    return frac * PIT_EXIT_TRACK_SEC;
  }

  // 2. Out-lap: has driver just exited the pit lane?
  const outStop = PIT_STOPS.find(p => p.driver === drvKey && t > p.endSec && t <= (p.outLapEndSec || (p.endSec + 80)));
  if (outStop) {
    const outEnd = outStop.outLapEndSec || (outStop.endSec + 80);
    const u = Math.min(1.0, Math.max(0.0, (t - outStop.endSec) / Math.max(1, outEnd - outStop.endSec)));
    // Continuous forward progress from Curva 1 pit exit (5.06) all the way to Start/Finish line (95.0)
    return PIT_EXIT_TRACK_SEC + u * (LAP_DURATION - PIT_EXIT_TRACK_SEC);
  }

  // 3. Driver authentic lap progress from OpenF1 / FastF1 telemetry
  const rec = getDriverLapRecord(drvKey, t);
  if (rec && rec.dur && rec.dur > 0) {
    // In-lap check: is this driver entering the pits later in this lap?
    const inStop = PIT_STOPS.find(p => p.driver === drvKey && p.startSec > rec.startSec && p.startSec <= rec.startSec + rec.dur && t < p.startSec && t >= rec.startSec);
    if (inStop) {
      const u = Math.min(1.0, Math.max(0.0, (t - rec.startSec) / Math.max(1, inStop.startSec - rec.startSec)));
      return u * PIT_ENTRY_TRACK_SEC;
    }
    const u = Math.min(0.9999, Math.max(0.0, (t - rec.startSec) / rec.dur));
    return u * LAP_DURATION;
  }

  // Fallback
  const gap = getDriverGap(drvKey, t);
  const tEff = Math.max(0, t - gap);
  const l = getDriverLap(drvKey, t);

  if (l === 2) {
    const lapRacingDuration = (LAP_STARTS[3] || 392.2) - 216.9;
    const f = Math.max(0, Math.min(0.9999, (tEff - 216.9) / lapRacingDuration));
    return f * LAP_DURATION;
  }

  const dur = (LAP_STARTS[l + 1] || (RACE_FINISH_SEC + 0.4)) - LAP_STARTS[l];
  const f = Math.max(0, Math.min(0.9999, (tEff - LAP_STARTS[l]) / (dur || LAP_DURATION)));
  return f * LAP_DURATION;
}`;

content = content.replace(oldFuncsRegex, newFuncsCode);

// 4. Update exports
if (!content.includes('PIT_EXIT_TRACK_SEC,')) {
  content = content.replace(
    'TOTAL_LAPS,\n  LAP_DURATION,',
    'TOTAL_LAPS,\n  LAP_DURATION,\n  PIT_EXIT_TRACK_SEC,\n  PIT_ENTRY_TRACK_SEC,'
  );
  content = content.replace(
    'DRIVER_STINTS,\n  getDriverStintInfo,',
    'DRIVER_STINTS,\n  DRIVER_LAPS,\n  getDriverStintInfo,\n  getDriverLapRecord,'
  );
}

fs.writeFileSync(filePath, content, 'utf8');
console.log('Successfully updated src/race_model.js with pit stop continuity and driver laps!');

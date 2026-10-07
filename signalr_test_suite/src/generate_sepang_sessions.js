const fs = require('fs');
const path = require('path');

const dataDir = path.join(__dirname, '..', 'data');
const sepangDrivers = JSON.parse(fs.readFileSync(path.join(dataDir, 'sepang_drivers.json'), 'utf-8'));

// Driver base performance hierarchy for dry Sepang (2026 ground effect)
// Pole reference: 1:31.215 (VER)
const driverProfiles = [
  { key: '3',  code: 'VER', q1: 92.080, q2: 91.650, q3: 91.215, fp: 92.145, s1: 20.31, s2: 24.52, team: 'Red Bull Racing' },
  { key: '16', code: 'LEC', q1: 92.150, q2: 91.720, q3: 91.298, fp: 92.230, s1: 20.34, s2: 24.56, team: 'Ferrari' },
  { key: '44', code: 'HAM', q1: 92.350, q2: 91.820, q3: 91.420, fp: 92.420, s1: 20.39, s2: 24.59, team: 'Ferrari' },
  { key: '1',  code: 'NOR', q1: 92.210, q2: 91.810, q3: 91.480, fp: 92.310, s1: 20.38, s2: 24.62, team: 'McLaren' },
  { key: '81', code: 'PIA', q1: 92.410, q2: 91.950, q3: 91.550, fp: 92.620, s1: 20.42, s2: 24.65, team: 'McLaren' },
  { key: '63', code: 'RUS', q1: 92.380, q2: 91.900, q3: 91.620, fp: 92.510, s1: 20.40, s2: 24.68, team: 'Mercedes' },
  { key: '12', code: 'ANT', q1: 92.520, q2: 92.050, q3: 91.750, fp: 92.740, s1: 20.45, s2: 24.71, team: 'Mercedes' },
  { key: '6',  code: 'HAD', q1: 92.650, q2: 92.180, q3: 91.890, fp: 92.890, s1: 20.48, s2: 24.75, team: 'Red Bull Racing' },
  { key: '10', code: 'GAS', q1: 92.780, q2: 92.290, q3: 92.050, fp: 93.020, s1: 20.52, s2: 24.81, team: 'Alpine' },
  { key: '5',  code: 'BOR', q1: 92.890, q2: 92.380, q3: 92.180, fp: 94.520, s1: 20.56, s2: 24.85, team: 'Kick Sauber' },
  // Eliminated in Q2 (P11 - P15)
  { key: '30', code: 'TSU', q1: 92.950, q2: 92.720, q3: null,   fp: 93.280, s1: 20.65, s2: 25.02, team: 'RB' },
  { key: '14', code: 'ALO', q1: 93.020, q2: 92.780, q3: null,   fp: 93.360, s1: 20.68, s2: 25.05, team: 'Aston Martin' },
  { key: '55', code: 'SAI', q1: 93.110, q2: 92.810, q3: null,   fp: 93.150, s1: 20.70, s2: 25.06, team: 'Williams' },
  { key: '18', code: 'STR', q1: 93.220, q2: 92.890, q3: null,   fp: 93.480, s1: 20.73, s2: 25.09, team: 'Aston Martin' },
  { key: '27', code: 'HUL', q1: 93.310, q2: 92.950, q3: null,   fp: 93.590, s1: 20.75, s2: 25.12, team: 'Kick Sauber' },
  // Eliminated in Q1 (P16 - P22)
  { key: '31', code: 'OCO', q1: 93.720, q2: null,   q3: null,   fp: 93.910, s1: 20.90, s2: 25.28, team: 'Haas' },
  { key: '23', code: 'ALB', q1: 93.780, q2: null,   q3: null,   fp: 93.820, s1: 20.92, s2: 25.30, team: 'Williams' },
  { key: '77', code: 'BOT', q1: 93.850, q2: null,   q3: null,   fp: 93.710, s1: 20.94, s2: 25.32, team: 'Kick Sauber' },
  { key: '11', code: 'PER', q1: 93.910, q2: null,   q3: null,   fp: 94.250, s1: 20.96, s2: 25.34, team: 'RB' },
  { key: '43', code: 'COL', q1: 94.020, q2: null,   q3: null,   fp: 94.130, s1: 20.98, s2: 25.38, team: 'Alpine' },
  { key: '41', code: 'LAW', q1: 94.150, q2: null,   q3: null,   fp: 94.380, s1: 21.02, s2: 25.42, team: 'RB' },
  { key: '87', code: 'BEA', q1: 94.220, q2: null,   q3: null,   fp: 94.020, s1: 21.05, s2: 25.45, team: 'Haas' }
];

// Helper to calculate s3 from dur, s1, s2
function makeSectors(dur) {
  const s1 = Math.round((dur * 0.222) * 1000) / 1000;
  const s2 = Math.round((dur * 0.268) * 1000) / 1000;
  const s3 = Math.round((dur - s1 - s2) * 1000) / 1000;
  return { s1, s2, s3 };
}

// ══════════════════════════════════════════════════════════════
// 1. GENERATE SEPANG FREE PRACTICE (FP) DATASET
// ══════════════════════════════════════════════════════════════
function generateSepangFpDataset() {
  const laps = {};
  const pitStops = [];
  const stints = {};
  const keyframes = {};

  driverProfiles.forEach((p, idx) => {
    laps[p.key] = [];
    stints[p.key] = [
      { stint: 1, compound: 'MEDIUM', code: 'M', lapStart: 1, lapEnd: 3 },
      { stint: 2, compound: 'SOFT',   code: 'S', lapStart: 4, lapEnd: 6 },
      { stint: 3, compound: 'HARD',   code: 'H', lapStart: 7, lapEnd: 11 }
    ];

    // Staggered release: groups leave at different times
    const groupDelay = (idx % 6) * 45; // 0, 45, 90, 135, 180, 225
    const tStartStint1 = 60 + groupDelay;
    
    // Initial wait in garage
    pitStops.push({
      driver: p.key,
      lap: 0,
      startSec: 0,
      endSec: tStartStint1,
      duration: tStartStint1,
      tireFrom: 'M',
      tireTo: 'M',
      outLapEndSec: tStartStint1 + 95
    });

    // Stint 1: Out-lap + Lap 1 + Lap 2 + In-lap
    const dur1 = p.fp + 1.25;
    const dur2 = p.fp + 0.65;
    const sec1 = makeSectors(dur1);
    const sec2 = makeSectors(dur2);

    const lap1Start = tStartStint1 + 95;
    laps[p.key].push({
      lap: 1,
      startSec: lap1Start,
      dur: dur1,
      s1: sec1.s1,
      s2: sec1.s2,
      s3: sec1.s3,
      isPit: false
    });

    const lap2Start = lap1Start + dur1;
    laps[p.key].push({
      lap: 2,
      startSec: lap2Start,
      dur: dur2,
      s1: sec2.s1,
      s2: sec2.s2,
      s3: sec2.s3,
      isPit: false
    });

    // In-lap finishes around lap2Start + dur2 + 95
    const inPit1Time = lap2Start + dur2 + 95;
    const tStartStint2 = 1200 + (idx % 8) * 35; // Garage pause ~ 700s

    pitStops.push({
      driver: p.key,
      lap: 3,
      startSec: inPit1Time,
      endSec: tStartStint2,
      duration: tStartStint2 - inPit1Time,
      tireFrom: 'M',
      tireTo: 'S',
      outLapEndSec: tStartStint2 + 95
    });

    // Stint 2 (Qualifying simulation on Soft: FASTEST LAP!)
    const bestDur = p.fp;
    const secBest = makeSectors(bestDur);
    const lap3Start = tStartStint2 + 95;

    laps[p.key].push({
      lap: 3,
      startSec: lap3Start,
      dur: bestDur,
      s1: secBest.s1,
      s2: secBest.s2,
      s3: secBest.s3,
      isPit: false
    });

    // In-lap
    const inPit2Time = lap3Start + bestDur + 95;
    const tStartStint3 = 2300 + (idx % 7) * 40;

    pitStops.push({
      driver: p.key,
      lap: 4,
      startSec: inPit2Time,
      endSec: tStartStint3,
      duration: tStartStint3 - inPit2Time,
      tireFrom: 'S',
      tireTo: 'H',
      outLapEndSec: tStartStint3 + 95
    });

    // Stint 3 (Long run on Hard: 3 laps)
    let curT = tStartStint3 + 95;
    for (let l = 4; l <= 6; l++) {
      const longDur = p.fp + 2.4 + (l - 4) * 0.3;
      const sLong = makeSectors(longDur);
      laps[p.key].push({
        lap: l,
        startSec: curT,
        dur: longDur,
        s1: sLong.s1,
        s2: sLong.s2,
        s3: sLong.s3,
        isPit: false
      });
      curT += longDur;
    }

    // Final stay in garage until session end
    const inPit3Time = curT + 95;
    pitStops.push({
      driver: p.key,
      lap: 7,
      startSec: inPit3Time,
      endSec: 3600,
      duration: 3600 - inPit3Time,
      tireFrom: 'H',
      tireTo: 'H',
      outLapEndSec: 3600
    });

    // Keyframes
    keyframes[p.key] = [
      [0, (idx * 0.15)],
      [3600, (idx * 0.15)]
    ];
  });

  const rc = [
    { timeSec: 0, message: "🟢 Bandiera Verde: Inizio Sessione Prove Libere (FP2)", category: "Flag", flag: "GREEN" },
    { timeSec: 120, message: "DRS abilitato dalla Direzione Gara", category: "DRS" },
    { timeSec: 850, message: "Track limits alla Curva 9: Tempo cancellato per #18 Stroll", category: "Investigation" },
    { timeSec: 1350, message: "🟡 Bandiera Gialla Settore 2: Testacoda #23 Albon alla Curva 4", category: "Flag", flag: "YELLOW" },
    { timeSec: 1410, message: "🟢 Bandiera Verde Settore 2: Pista libera", category: "Flag", flag: "GREEN" },
    { timeSec: 2020, message: "Miglior tempo provvisorio: #3 Verstappen in 1:32.145 su gomma Soft", category: "Timing" },
    { timeSec: 3540, message: "Prove di partenza autorizzate sul rettilineo dei box", category: "RaceControl" },
    { timeSec: 3600, message: "🏁 Bandiera a Scacchi: Fine sessione Prove Libere FP2", category: "Flag", flag: "CHEQUERED" }
  ];

  return {
    circuitId: 'sepang',
    sessionType: 'practice',
    sessionName: 'Prove Libere 2 (FP2)',
    eventTitle: 'FORMULA 1 PETRONAS MALAYSIAN GP — FP2',
    lapDuration: 95.0,
    totalLaps: 12,
    sessionDuration: 3600,
    pitExitTrackSec: 5.06,
    pitEntryTrackSec: 89.34,
    s1Track: 21.11,
    s2Track: 46.58,
    gridOrder: driverProfiles.map(p => p.key),
    drivers: sepangDrivers,
    keyframes,
    pitStops,
    rc,
    retirements: {},
    stints,
    laps,
    events: [],
    incidents: [],
    lapStarts: [0]
  };
}

// ══════════════════════════════════════════════════════════════
// 2. GENERATE SEPANG QUALIFYING (Q1, Q2, Q3) DATASET
// ══════════════════════════════════════════════════════════════
function generateSepangQualifyingDataset() {
  const laps = {};
  const pitStops = [];
  const stints = {};
  const keyframes = {};

  driverProfiles.forEach((p, idx) => {
    laps[p.key] = [];
    stints[p.key] = [
      { stint: 1, compound: 'SOFT', code: 'S', lapStart: 1, lapEnd: 2 },
      { stint: 2, compound: 'SOFT', code: 'S', lapStart: 3, lapEnd: 4 },
      { stint: 3, compound: 'SOFT', code: 'S', lapStart: 5, lapEnd: 6 }
    ];

    // Q1 PHASE (0 to 1080s)
    // Run 1: Q1 Banker
    const q1Delay = (idx % 6) * 35;
    const q1Run1Out = 70 + q1Delay;
    
    // Initial wait
    pitStops.push({
      driver: p.key,
      lap: 0,
      startSec: 0,
      endSec: q1Run1Out,
      duration: q1Run1Out,
      tireFrom: 'S',
      tireTo: 'S',
      outLapEndSec: q1Run1Out + 95
    });

    const q1Dur1 = p.q1 + 0.45;
    const sQ1_1 = makeSectors(q1Dur1);
    const q1Lap1Start = q1Run1Out + 95;
    laps[p.key].push({
      lap: 1,
      startSec: q1Lap1Start,
      dur: q1Dur1,
      s1: sQ1_1.s1,
      s2: sQ1_1.s2,
      s3: sQ1_1.s3,
      isPit: false
    });

    // In-lap & garage pause before Q1 push 2
    const q1In1 = q1Lap1Start + q1Dur1 + 95;
    const q1Run2Out = 650 + (idx % 5) * 30;

    pitStops.push({
      driver: p.key,
      lap: 1,
      startSec: q1In1,
      endSec: q1Run2Out,
      duration: q1Run2Out - q1In1,
      tireFrom: 'S',
      tireTo: 'S',
      outLapEndSec: q1Run2Out + 95
    });

    // Run 2: Q1 Final Push (Best Q1 lap!)
    const q1Dur2 = p.q1;
    const sQ1_2 = makeSectors(q1Dur2);
    const q1Lap2Start = q1Run2Out + 95;
    laps[p.key].push({
      lap: 2,
      startSec: q1Lap2Start,
      dur: q1Dur2,
      s1: sQ1_2.s1,
      s2: sQ1_2.s2,
      s3: sQ1_2.s3,
      isPit: false
    });

    const q1In2 = q1Lap2Start + q1Dur2 + 95;

    // Is driver eliminated in Q1? (P16 - P22)
    const isElimQ1 = idx >= 15;
    if (isElimQ1) {
      // Stay in garage for the rest of qualifying!
      pitStops.push({
        driver: p.key,
        lap: 2,
        startSec: q1In2,
        endSec: 3600,
        duration: 3600 - q1In2,
        tireFrom: 'S',
        tireTo: 'S',
        outLapEndSec: 3600
      });
    } else {
      // Q2 PHASE (1080s to 2160s) for Top 15
      const q2Run1Out = 1200 + (idx % 5) * 30;
      pitStops.push({
        driver: p.key,
        lap: 2,
        startSec: q1In2,
        endSec: q2Run1Out,
        duration: q2Run1Out - q1In2,
        tireFrom: 'S',
        tireTo: 'S',
        outLapEndSec: q2Run1Out + 95
      });

      const q2Dur1 = p.q2 + 0.35;
      const sQ2_1 = makeSectors(q2Dur1);
      const q2Lap1Start = q2Run1Out + 95;
      laps[p.key].push({
        lap: 3,
        startSec: q2Lap1Start,
        dur: q2Dur1,
        s1: sQ2_1.s1,
        s2: sQ2_1.s2,
        s3: sQ2_1.s3,
        isPit: false
      });

      const q2In1 = q2Lap1Start + q2Dur1 + 95;
      const q2Run2Out = 1750 + (idx % 4) * 35;

      pitStops.push({
        driver: p.key,
        lap: 3,
        startSec: q2In1,
        endSec: q2Run2Out,
        duration: q2Run2Out - q2In1,
        tireFrom: 'S',
        tireTo: 'S',
        outLapEndSec: q2Run2Out + 95
      });

      // Q2 Final Push
      const q2Dur2 = p.q2;
      const sQ2_2 = makeSectors(q2Dur2);
      const q2Lap2Start = q2Run2Out + 95;
      laps[p.key].push({
        lap: 4,
        startSec: q2Lap2Start,
        dur: q2Dur2,
        s1: sQ2_2.s1,
        s2: sQ2_2.s2,
        s3: sQ2_2.s3,
        isPit: false
      });

      const q2In2 = q2Lap2Start + q2Dur2 + 95;

      // Is driver eliminated in Q2? (P11 - P15)
      const isElimQ2 = idx >= 10;
      if (isElimQ2) {
        pitStops.push({
          driver: p.key,
          lap: 4,
          startSec: q2In2,
          endSec: 3600,
          duration: 3600 - q2In2,
          tireFrom: 'S',
          tireTo: 'S',
          outLapEndSec: 3600
        });
      } else {
        // Q3 PHASE (2160s to 3240s) for Top 10
        const q3Run1Out = 2320 + (idx % 4) * 25;
        pitStops.push({
          driver: p.key,
          lap: 4,
          startSec: q2In2,
          endSec: q3Run1Out,
          duration: q3Run1Out - q2In2,
          tireFrom: 'S',
          tireTo: 'S',
          outLapEndSec: q3Run1Out + 95
        });

        // Q3 Banker
        const q3Dur1 = p.q3 + 0.32;
        const sQ3_1 = makeSectors(q3Dur1);
        const q3Lap1Start = q3Run1Out + 95;
        laps[p.key].push({
          lap: 5,
          startSec: q3Lap1Start,
          dur: q3Dur1,
          s1: sQ3_1.s1,
          s2: sQ3_1.s2,
          s3: sQ3_1.s3,
          isPit: false
        });

        const q3In1 = q3Lap1Start + q3Dur1 + 95;
        // Final Pole Shootout Run!
        const q3Run2Out = 2920 + (idx % 3) * 20;

        pitStops.push({
          driver: p.key,
          lap: 5,
          startSec: q3In1,
          endSec: q3Run2Out,
          duration: q3Run2Out - q3In1,
          tireFrom: 'S',
          tireTo: 'S',
          outLapEndSec: q3Run2Out + 95
        });

        // Final Pole Lap!
        const q3Dur2 = p.q3;
        const sQ3_2 = makeSectors(q3Dur2);
        const q3Lap2Start = q3Run2Out + 95;
        laps[p.key].push({
          lap: 6,
          startSec: q3Lap2Start,
          dur: q3Dur2,
          s1: sQ3_2.s1,
          s2: sQ3_2.s2,
          s3: sQ3_2.s3,
          isPit: false
        });

        const q3In2 = q3Lap2Start + q3Dur2 + 95;
        pitStops.push({
          driver: p.key,
          lap: 6,
          startSec: q3In2,
          endSec: 3600,
          duration: 3600 - q3In2,
          tireFrom: 'S',
          tireTo: 'S',
          outLapEndSec: 3600
        });
      }
    }

    keyframes[p.key] = [
      [0, (idx * 0.1)],
      [3600, (idx * 0.1)]
    ];
  });

  const rc = [
    { timeSec: 0, message: "🟢 Bandiera Verde: Inizio Sessione Qualifiche — Q1 (18 Minuti)", category: "Flag", flag: "GREEN" },
    { timeSec: 1080, message: "🔴 Fine Q1: Eliminati #31 Ocon, #23 Albon, #77 Bottas, #11 Perez, #43 Colapinto, #41 Lawson, #87 Bearman", category: "Flag", flag: "RED" },
    { timeSec: 1140, message: "🟢 Bandiera Verde: Inizio Sessione Qualifiche — Q2 (15 Minuti)", category: "Flag", flag: "GREEN" },
    { timeSec: 2160, message: "🔴 Fine Q2: Eliminati #30 Tsunoda, #14 Alonso, #55 Sainz, #18 Stroll, #27 Hulkenberg", category: "Flag", flag: "RED" },
    { timeSec: 2220, message: "🟢 Bandiera Verde: Inizio Sessione Qualifiche — Q3 Top 10 Shootout (12 Minuti)", category: "Flag", flag: "GREEN" },
    { timeSec: 2550, message: "Pole Provvisoria: #16 Charles Leclerc (Ferrari) 1:31.298", category: "Timing" },
    { timeSec: 3140, message: "👑 POLE POSITION: #3 Max Verstappen (Red Bull Racing) strappa la pole con 1:31.215!", category: "Timing" },
    { timeSec: 3240, message: "🏁 Bandiera a Scacchi: Conclusione Sessione di Qualifica", category: "Flag", flag: "CHEQUERED" }
  ];

  return {
    circuitId: 'sepang',
    sessionType: 'qualifying',
    sessionName: 'Qualifiche Ufficiali (Q1, Q2, Q3)',
    eventTitle: 'FORMULA 1 PETRONAS MALAYSIAN GP — QUALIFICHE',
    lapDuration: 95.0,
    totalLaps: 6,
    sessionDuration: 3600,
    pitExitTrackSec: 5.06,
    pitEntryTrackSec: 89.34,
    s1Track: 21.11,
    s2Track: 46.58,
    gridOrder: driverProfiles.map(p => p.key),
    drivers: sepangDrivers,
    keyframes,
    pitStops,
    rc,
    retirements: {},
    stints,
    laps,
    events: [],
    incidents: [],
    lapStarts: [0]
  };
}

module.exports = {
  generateSepangFpDataset,
  generateSepangQualifyingDataset
};

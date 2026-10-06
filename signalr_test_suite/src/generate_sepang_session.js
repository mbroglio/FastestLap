/**
 * Sepang Grand Prix SignalR Stream Generator (1-Second High-Resolution Telemetry)
 * Generates continuous second-by-second telemetry from 10:30:00 to 12:30:00 UTC (7,200 seconds).
 * Every single second is compressed using real raw DEFLATE + Base64 matching FastF1 protocol.
 */

const fs = require('fs');
const path = require('path');
const TelemetryDecoder = require('./decoder');
const { DEFAULT_DRIVERS } = require('./config');
const { getTrackPointAtSecond } = require('./sepang_geometry');
const raceModel = require('./race_model');
const { RACE_CONTROL_MESSAGES } = require('./race_control_events');

/**
 * Calculates continuous, realistic telemetry physics for Sepang circuit at any second t (0 to 94s)
 * Delegates to getTrackPointAtSecond from sepang_geometry which loads the calibrated 453-node exact track.
 */
function getTelemetryForSecond(t) {
  return getTrackPointAtSecond(t);
}

const PIT_LANE_NODES = [
  { px: 1103.0, py: 526.0 }, // Branching off track before Turn 15
  { px: 1096.0, py: 508.0 },
  { px: 1085.0, py: 484.0 },
  { px: 1070.0, py: 466.0 },
  { px: 1050.0, py: 451.0 },
  { px: 1030.0, py: 446.0 }, // Crest of pit entry inside Turn 15
  { px: 1005.0, py: 451.0 },
  { px: 980.0,  py: 460.0 },
  { px: 950.0,  py: 471.0 },
  { px: 920.0,  py: 482.0 },
  { px: 885.0,  py: 493.0 },
  { px: 850.0,  py: 504.0 },
  { px: 800.0,  py: 514.5 },
  { px: 750.0,  py: 520.0 },
  { px: 714.0,  py: 523.5 }, // Central Pit Box Zone opposite S/F line
  { px: 650.0,  py: 528.0 },
  { px: 550.0,  py: 536.0 },
  { px: 450.0,  py: 544.0 },
  { px: 350.0,  py: 552.5 },
  { px: 250.0,  py: 562.0 },
  { px: 200.0,  py: 570.5 },
  { px: 175.0,  py: 578.0 },
  { px: 155.0,  py: 583.5 },
  { px: 141.0,  py: 585.5 }  // Rejoining track edge before Turn 1
];

const PIT_LANE_DISTS = [0];
for (let i = 0; i < PIT_LANE_NODES.length - 1; i++) {
  const dx = PIT_LANE_NODES[i + 1].px - PIT_LANE_NODES[i].px;
  const dy = PIT_LANE_NODES[i + 1].py - PIT_LANE_NODES[i].py;
  PIT_LANE_DISTS.push(PIT_LANE_DISTS[i] + Math.sqrt(dx * dx + dy * dy));
}
const PIT_LANE_TOTAL_DIST = PIT_LANE_DISTS[PIT_LANE_DISTS.length - 1];

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

function generateSepangRaceStream(stepSeconds = 1) {

  console.log(`Generating Sepang Grand Prix SignalR stream at ${stepSeconds}-second resolution (Official Race Director: 08:33:00 - 10:20:15 UTC, 55 Laps — OpenF1 & Jolpica Aligned)...`);

  const streamEvents = [];
  const startTime = new Date(raceModel.RACE_START_UTC).getTime();
  const endTime = new Date(startTime + raceModel.SESSION_END_SEC * 1000).getTime();

  // 1. Initial DriverList message
  streamEvents.push({
    topic: 'DriverList',
    utc: new Date(startTime).toISOString(),
    data: DEFAULT_DRIVERS
  });

  // 2. SessionData / SessionInfo
  streamEvents.push({
    topic: 'SessionData',
    utc: new Date(startTime).toISOString(),
    data: {
      Meeting: {
        Name: 'Malaysian Grand Prix',
        OfficialName: 'FORMULA 1 PETRONAS MALAYSIAN GRAND PRIX 2026',
        Location: 'Kuala Lumpur',
        Country: { Key: 30, Code: 'MAS', Name: 'Malaysia' },
        Circuit: { Key: 24, ShortName: 'Sepang' }
      },
      SessionType: 'Race',
      SessionStatus: 'Started',
      TotalLaps: raceModel.TOTAL_LAPS,
      StartTime: raceModel.RACE_START_UTC,
      EndTime: raceModel.RACE_FINISH_UTC
    }
  });

  // 3. Emit pre-race Race Control messages (before official race start)
  const preRaceMessages = RACE_CONTROL_MESSAGES.filter(m => m.timeSec <= 0);
  for (const rc of preRaceMessages) {
    streamEvents.push({
      topic: 'RaceControlMessages',
      utc: rc.date,
      data: rc
    });
  }

  // 4. Initial TimingAppData: Starting tyre compound for all 22 drivers (FastF1 Tyre Stints)
  const initialLinesAppData = {};
  for (const drvNum of Object.keys(DEFAULT_DRIVERS)) {
    const stintInfo = raceModel.getDriverTireDetails(drvNum, 0);
    initialLinesAppData[drvNum] = {
      Stints: {
        "0": {
          Compound: stintInfo.compound,
          New: "true",
          TyresNotChanged: "0",
          TotalLaps: 0,
          StartLaps: 0
        }
      }
    };
  }
  streamEvents.push({
    topic: 'TimingAppData',
    utc: new Date(startTime).toISOString(),
    data: {
      Lines: initialLinesAppData
    }
  });

  // 22-Driver Grid from config
  const driverProfiles = DEFAULT_DRIVERS;
  const lapDurationSec = 95;
  let currentTime = startTime;
  let lastReportedLap = 1;

  // Generate an exact packet for every stepSeconds (default: 1 second)
  while (currentTime <= endTime) {
    const elapsedTotalSec = Math.floor((currentTime - startTime) / 1000);
    const currentUtcStr = new Date(currentTime).toISOString();

    // Inject Race Control messages that fire at this second
    const rcThisSec = RACE_CONTROL_MESSAGES.filter(m => m.timeSec === elapsedTotalSec && m.timeSec > 0);
    for (const rc of rcThisSec) {
      streamEvents.push({
        topic: 'RaceControlMessages',
        utc: currentUtcStr,
        data: rc
      });
    }

    const isVSC = raceModel.isVSCActive(elapsedTotalSec);
    const isFinished = raceModel.isRaceFinished(elapsedTotalSec); // t >= 5320s
    const isSessionEnd = raceModel.isSessionTerminated(elapsedTotalSec); // t >= 5440s

    const carsData = {};
    const posEntries = {};

    for (const [drvNum, profile] of Object.entries(driverProfiles)) {
      // Dynamic continuous race progression with real overtakes, gaps, and pit stops
      const drvSec = raceModel.getDriverTrackSec(drvNum, elapsedTotalSec);
      const inPit = raceModel.isDriverInPit(drvNum, elapsedTotalSec);
      const isRetired = raceModel.isDriverRetired(drvNum, elapsedTotalSec);
      const geo = getTrackPointAtSecond(drvSec);
      const gridIdx = raceModel.GRID_ORDER.indexOf(drvNum);

      let speed, rpm, throttle, brake, gear, drs, loc;
      let finalPx = geo.px, finalPy = geo.py;

      if (isRetired) {
        speed = 0;
        rpm = 0;
        throttle = 0;
        brake = 1;
        gear = 0;
        drs = 0;
        loc = '🛑 Ritiro dalla Corsa (DNF - Retired)';
      } else if (elapsedTotalSec < 195) {
        // Formation Lap 1 & 2 behind Safety Car (Wet Conditions & Controlled Pace)
        // Cornering speed down in turns, downshifts, braking zones, acceleration on straights
        const scMaxSpeed = elapsedTotalSec < 108 ? 152 : 148;
        speed = Math.round(geo.speed > scMaxSpeed ? scMaxSpeed + (geo.speed - scMaxSpeed) * 0.12 : geo.speed * 0.92);
        gear = speed < 90 ? 2 : (speed < 130 ? 3 : (speed < 170 ? 4 : (speed < 220 ? 5 : 6)));
        brake = geo.brake;
        throttle = geo.brake === 1 ? 0 : Math.round(Math.min(75, (speed / scMaxSpeed) * 75));
        rpm = Math.round(5200 + (speed / scMaxSpeed) * 5800 + (profile.rpmDelta || 0));
        drs = 0;
        loc = (elapsedTotalSec < 108 ? '🟡 Giro di Formazione 1 (Dietro Safety Car) — ' : '🟡 Giro di Formazione 2 (Dietro Safety Car) — ') + geo.location;
      } else if (elapsedTotalSec < 216.9) {
        // Grid Lineup before Standing Start
        const stopTime = 204.0 + gridIdx * 0.42;
        const rawOffset = (gridIdx % 2 === 0) ? -5.5 : +5.5;
        let latOffset = 0;
        if (elapsedTotalSec < stopTime) {
          const u = Math.min(1.0, Math.max(0.0, (elapsedTotalSec - 195.0) / Math.max(1.0, stopTime - 195.0)));
          latOffset = rawOffset * u;
          speed = Math.round(Math.max(0, 50 * (1 - u)));
          gear = speed > 20 ? 2 : 1;
          rpm = Math.round(4500 + speed * 35);
          throttle = speed > 20 ? 15 : 0;
          brake = speed < 25 ? 1 : 0;
          drs = 0;
          loc = '🏁 Rientro Safety Car — Posizionamento sulla Griglia di Partenza (P' + (gridIdx + 1) + ')';
        } else {
          latOffset = rawOffset;
          speed = 0;
          gear = 1;
          drs = 0;
          brake = 1;
          if (elapsedTotalSec >= 212) {
            rpm = 11600;
            throttle = 100;
            loc = '🚦 Semafori Accesi — Launch Control Attivo (P' + (gridIdx + 1) + ')';
          } else {
            rpm = 5000;
            throttle = 0;
            loc = '🏁 Griglia di Partenza — Fermo in Casella (P' + (gridIdx + 1) + ')';
          }
        }
        finalPx = geo.px - latOffset * 0.07;
        finalPy = geo.py - latOffset * 0.99;
      } else if (elapsedTotalSec < 224.0) {
        // Standing Start Launch Phase
        const dt = elapsedTotalSec - 216.9;
        const rawOffset = (gridIdx % 2 === 0) ? -5.5 : +5.5;
        const latOffset = rawOffset * Math.max(0, 1.0 - dt / 6.0);
        finalPx = geo.px - latOffset * 0.07;
        finalPy = geo.py - latOffset * 0.99;
        speed = Math.round(Math.min(280, dt * 45));
        gear = speed < 80 ? 1 : (speed < 140 ? 2 : (speed < 190 ? 3 : (speed < 235 ? 4 : (speed < 265 ? 5 : 6))));
        rpm = Math.min(12500, Math.round(10500 + dt * 250));
        throttle = 100;
        brake = 0;
        drs = 0;
        loc = '⚡ Partenza da Fermo (Standing Start) — Scatto dai Blocchi';
      } else if (inPit) {
        // Authentic Pit Lane Transit along the Black Line
        const activeStop = raceModel.PIT_STOPS.find(p => p.driver === drvNum && elapsedTotalSec >= p.startSec && elapsedTotalSec <= p.endSec);
        let pitPx = 714, pitPy = 523.5;
        if (activeStop) {
          const frac = Math.max(0, Math.min(1, (elapsedTotalSec - activeStop.startSec) / Math.max(1, (activeStop.endSec - activeStop.startSec))));
          const teamBoxDist = Math.max(100, Math.min(PIT_LANE_TOTAL_DIST - 100, 441.7 + (gridIdx - 10) * 8.5));
          if (frac < 0.35) {
            const u = frac / 0.35;
            const pt = getPitLaneCoordAtDist(u * teamBoxDist);
            pitPx = pt.px;
            pitPy = pt.py;
            speed = 80;
            rpm = 4500;
            gear = 2;
            throttle = 30;
            brake = 0;
            loc = '🔧 Corsia Box — Limitatore 80 km/h (Ingresso)';
          } else if (frac <= 0.65) {
            const pt = getPitLaneCoordAtDist(teamBoxDist);
            pitPx = pt.px;
            pitPy = pt.py;
            speed = 0;
            rpm = 5000;
            gear = 1;
            throttle = 0;
            brake = 1;
            loc = '🔧 Pit Box — Sosta Cambio Gomme (Sollevatori)';
          } else {
            const u = (frac - 0.65) / 0.35;
            const pt = getPitLaneCoordAtDist(teamBoxDist + u * (PIT_LANE_TOTAL_DIST - teamBoxDist));
            pitPx = pt.px;
            pitPy = pt.py;
            speed = 80;
            rpm = 4500;
            gear = 2;
            throttle = 35;
            brake = 0;
            loc = '🔧 Corsia Box — Limitatore 80 km/h (Uscita)';
          }
        } else {
          const pt = getPitLaneCoordAtDist(441.7);
          pitPx = pt.px;
          pitPy = pt.py;
          speed = 80;
          rpm = 4500;
          gear = 2;
          throttle = 30;
          brake = 0;
          loc = '🔧 Corsia Box (80 km/h)';
        }
        drs = 0;
        finalPx = pitPx;
        finalPy = pitPy;

      } else if (isFinished) {
        // Slow in-lap / cool-down lap waving to grandstands
        speed = Math.min(120, Math.round(geo.speed * 0.45));
        rpm = 7200;
        throttle = 35;
        brake = geo.brake;
        gear = 4;
        drs = 0;
        loc = '🏁 In-Lap / Cooldown — Rientro in Parc Fermé';
      } else if (isVSC) {
        // Virtual Safety Car delta speed limit
        speed = Math.min(160, Math.round(geo.speed * 0.62));
        rpm = Math.min(9600, Math.round(geo.rpm * 0.72));
        throttle = Math.min(50, geo.throttle);
        brake = geo.brake;
        gear = Math.min(6, geo.gear);
        drs = 0;
        loc = '🟡 VSC DELTA — ' + geo.location;
      } else {
        speed = Math.max(50, Math.round(geo.speed * (profile.throttleAggression || 1.0) + (profile.speedDelta || 0)));
        rpm = Math.max(9500, Math.round(geo.rpm + (profile.rpmDelta || 0)));
        throttle = geo.brake === 1 ? 0 : Math.min(100, Math.round(geo.throttle * (profile.throttleAggression || 1.0)));
        brake = geo.brake;
        gear = geo.gear;
        drs = geo.drs;
        loc = geo.location;
      }

      const finalX = Math.round((finalPx - 640) * 0.973);
      const finalY = Math.round((530 - finalPy) * 0.973);

      carsData[drvNum] = {
        Channels: {
          '0': Math.round(rpm),
          '2': Math.round(speed),
          '3': gear,
          '4': Math.round(throttle),
          '5': brake,
          '45': drs
        },
        x: finalX,
        y: finalY,
        px: finalPx,
        py: finalPy,
        location: loc,
        sector: geo.sector,
        turn: geo.turn
      };

      posEntries[drvNum] = {
        Status: isRetired ? 'Retired' : (inPit ? 'InPit' : 'OnTrack'),
        X: finalX * 10,
        Y: finalY * 10,
        Z: isRetired ? 0 : 100
      };
    }

    // Safety Car Position Packet (Official F1 SignalR format)
    if (raceModel.isSafetyCarActive(elapsedTotalSec)) {
      const scSec = (raceModel.getDriverTrackSec('3', elapsedTotalSec) + 2.5) % raceModel.LAP_DURATION;
      const scGeo = getTrackPointAtSecond(scSec);
      posEntries['SC'] = {
        Status: 'SafetyCar',
        X: scGeo.x * 10,
        Y: scGeo.y * 10,
        Z: 100
      };
    }

    const packetPayload = {
      Entries: [
        {
          Utc: currentUtcStr,
          Cars: carsData
        }
      ]
    };

    // Compress CarData.z using raw DEFLATE + Base64
    const compressedCarData = TelemetryDecoder.encodeCarData(packetPayload);
    streamEvents.push({
      topic: 'CarData.z',
      utc: currentUtcStr,
      data: compressedCarData
    });

    // Compress Position.z using raw DEFLATE + Base64 (FastF1 Position Stream)
    const compressedPos = TelemetryDecoder.encodePosition({
      Position: [
        {
          Timestamp: currentUtcStr,
          Entries: posEntries
        }
      ]
    });
    streamEvents.push({
      topic: 'Position.z',
      utc: currentUtcStr,
      data: compressedPos
    });

    // Lap timing event at the end of each lap (strictly capped at 55 laps!)
    const currentLap = raceModel.getDriverLap('3', elapsedTotalSec);
    if (currentLap !== lastReportedLap && currentLap <= raceModel.TOTAL_LAPS) {
      lastReportedLap = currentLap;
      streamEvents.push({
        topic: 'TimingData',
        utc: currentUtcStr,
        data: {
          Lap: currentLap,
          TotalLaps: raceModel.TOTAL_LAPS,
          TrackStatus: isVSC ? '4' : '1',
          ChequeredFlag: currentLap === raceModel.TOTAL_LAPS && elapsedTotalSec >= raceModel.RACE_FINISH_SEC,
          SessionTime: `${Math.floor(elapsedTotalSec / 60)}m ${elapsedTotalSec % 60}s`
        }
      });

      // Emit updated TimingAppData with active tire compounds & stints (FastF1 Tyre Stints)
      const linesAppData = {};
      for (const drvNum of Object.keys(driverProfiles)) {
        const drvLap = raceModel.getDriverLap(drvNum, elapsedTotalSec);
        const allStints = raceModel.DRIVER_STINTS[drvNum] || [];
        const stintsObj = {};
        for (let i = 0; i < allStints.length; i++) {
          const s = allStints[i];
          if (drvLap >= s.lapStart) {
            const lapsInStint = drvLap <= s.lapEnd ? (drvLap - s.lapStart + 1) : (s.lapEnd - s.lapStart + 1);
            stintsObj[String(s.stint - 1)] = {
              Compound: s.compound,
              New: 'true',
              TyresNotChanged: '0',
              TotalLaps: s.ageAtStart + lapsInStint,
              LapNumber: drvLap,
              StartLaps: s.ageAtStart
            };
          }
        }
        linesAppData[drvNum] = {
          Stints: stintsObj
        };
      }
      streamEvents.push({
        topic: 'TimingAppData',
        utc: currentUtcStr,
        data: {
          Lines: linesAppData
        }
      });
    }

    currentTime += stepSeconds * 1000;
  }

  // Final SessionStatus: Ended (Telemetry stream officially terminated at Parc Fermé)
  streamEvents.push({
    topic: 'SessionStatus',
    utc: new Date(endTime).toISOString(),
    data: {
      Status: 'Ended',
      ChequeredFlag: true,
      TotalLaps: 55,
      ParcFerme: true,
      Winner: '3 (Max Verstappen)',
      Team: 'Oracle Red Bull Racing',
      Margin: '+2.307s',
      FastestLap: '1:38.220 (Max Verstappen, Lap 55)',
      Podium: [
        { Position: 1, Number: '3', Driver: 'Max Verstappen', Team: 'Red Bull Racing' },
        { Position: 2, Number: '12', Driver: 'Andrea Kimi Antonelli', Team: 'Mercedes' },
        { Position: 3, Number: '44', Driver: 'Lewis Hamilton', Team: 'Ferrari' }
      ],
      Source: 'OpenF1 Session 11731 & Jolpica F1 API (2026 Round 16 Sepang)',
      TelemetryStatus: 'Terminated'
    }
  });

  const outPath = path.join(__dirname, '..', 'data', 'sepang_race_stream.json');
  fs.writeFileSync(outPath, JSON.stringify(streamEvents), 'utf-8');
  console.log(`Stream successfully generated! Saved ${streamEvents.length} events (08:33:00 to 10:20:15 UTC, strictly 55 laps) to ${outPath}`);
  return { eventCount: streamEvents.length, outPath };
}

if (require.main === module) {
  const step = parseInt(process.argv[2], 10) || 1;
  generateSepangRaceStream(step);
}

module.exports = generateSepangRaceStream;

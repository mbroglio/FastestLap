/**
 * FastestLap LiveTiming - State Store & Deep Delta Merger
 *
 * Maintains the single source of truth for all F1 session channels:
 * DriverList, TimingData, TimingAppData, SessionInfo, SessionStatus, SessionData,
 * WeatherData, LapCount, RaceControlMessages, CarData, Position.
 */

function isPlainObject(obj) {
  return obj !== null && typeof obj === 'object' && !Array.isArray(obj);
}

/**
 * Deeply merges F1 SignalR delta objects into target state.
 * F1 sends indexed objects like { "0": { "Value": "28.1" } } for arrays (Sectors, Stints, Segments).
 */
function deepMerge(target, source) {
  if (!isPlainObject(source)) {
    return source;
  }

  for (const key of Object.keys(source)) {
    const srcVal = source[key];
    const tgtVal = target[key];

    if (Array.isArray(tgtVal) && isPlainObject(srcVal)) {
      // Merge indexed object delta into existing array
      for (const idxKey of Object.keys(srcVal)) {
        const idx = Number(idxKey);
        if (!Number.isNaN(idx)) {
          if (isPlainObject(tgtVal[idx]) && isPlainObject(srcVal[idxKey])) {
            deepMerge(tgtVal[idx], srcVal[idxKey]);
          } else {
            tgtVal[idx] = srcVal[idxKey];
          }
        }
      }
    } else if (isPlainObject(srcVal)) {
      if (!isPlainObject(tgtVal)) {
        target[key] = {};
      }
      deepMerge(target[key], srcVal);
    } else if (Array.isArray(srcVal)) {
      target[key] = srcVal.slice();
    } else {
      target[key] = srcVal;
    }
  }
  return target;
}

function parseLapTimeToSeconds(str) {
  if (!str || typeof str !== 'string') return 0;
  const clean = str.trim();
  if (!clean || clean === '--' || clean === '-') return 0;
  const parts = clean.split(':');
  if (parts.length === 2) {
    const mins = parseFloat(parts[0]);
    const secs = parseFloat(parts[1]);
    if (!isNaN(mins) && !isNaN(secs)) return mins * 60 + secs;
  }
  const val = parseFloat(clean);
  return !isNaN(val) && val > 0 ? val : 0;
}

function parseDeltaToSeconds(str) {
  if (!str || typeof str !== 'string') return null;
  const clean = str.trim().toUpperCase();
  if (!clean || clean === '--' || clean === '-') return null;
  if (clean === 'LEADER' || clean === '0' || clean === '0.000' || clean === '+0.000') return 0;
  if (clean.includes('LAP') || clean.includes('L')) return null;
  const num = parseFloat(clean.replace('+', '').replace('S', ''));
  return !isNaN(num) && num >= 0 ? num : null;
}

const F1_CIRCUIT_TOTAL_LAPS = {
  '9': 56, '63': 57, '149': 50, '1': 58, '10': 53, '7': 56, '151': 57,
  '6': 63, '22': 78, '23': 70, '4': 66, '19': 71, '2': 52, '14': 70,
  '20': 44, '55': 72, '39': 53, '144': 51, '61': 62, '65': 71, '15': 71,
  '152': 50, '150': 57, '70': 58,
  'austin': 56, 'cota': 56, 'united states': 56, 'usa': 56,
  'bahrain': 57, 'sakhir': 57,
  'jeddah': 50, 'saudi': 50,
  'melbourne': 58, 'albert park': 58, 'australia': 58,
  'suzuka': 53, 'japan': 53,
  'shanghai': 56, 'china': 56,
  'miami': 57,
  'imola': 63, 'emilia': 63,
  'monaco': 78, 'monte carlo': 78,
  'montreal': 70, 'canada': 70, 'villeneuve': 70,
  'barcelona': 66, 'catalunya': 66, 'spain': 66,
  'spielberg': 71, 'austria': 71, 'red bull ring': 71,
  'silverstone': 52, 'great britain': 52, 'britain': 52,
  'hungaroring': 70, 'hungary': 70, 'budapest': 70,
  'spa': 44, 'belgium': 44, 'francorchamps': 44,
  'zandvoort': 72, 'netherlands': 72, 'dutch': 72,
  'monza': 53, 'italy': 53,
  'baku': 51, 'azerbaijan': 51,
  'singapore': 62, 'marina bay': 62,
  'mexico': 71, 'rodriguez': 71,
  'interlagos': 71, 'brazil': 71, 'sao paulo': 71,
  'las vegas': 50, 'vegas': 50,
  'lusail': 57, 'losail': 57, 'qatar': 57,
  'yas marina': 58, 'abu dhabi': 58
};

function resolveF1CircuitTotalLaps(circuitKey, circuitName, sessionType) {
  const sType = String(sessionType || '').toLowerCase();
  const isSprint = sType.includes('sprint') || sType.includes('shootout');
  const key = String(circuitKey || '').trim();
  let baseLaps = F1_CIRCUIT_TOTAL_LAPS[key] || 0;
  if (!baseLaps && circuitName) {
    const cStr = String(circuitName).toLowerCase();
    for (const [k, v] of Object.entries(F1_CIRCUIT_TOTAL_LAPS)) {
      if (cStr.includes(k)) {
        baseLaps = v;
        break;
      }
    }
  }
  if (!baseLaps) baseLaps = 56;
  return isSprint ? Math.round(baseLaps / 3) : baseLaps;
}

class StateStore {
  constructor() {
    this.reset();
  }

  reset() {
    this.driverList = {};
    this.timingData = { Lines: {} };
    this.timingAppData = { Lines: {} };
    this.sessionInfo = {};
    this.sessionStatus = {};
    this.sessionData = {};
    this.trackStatus = { Status: '1', Message: 'AllClear' };
    this.currentSessionPart = null;
    this.weatherData = {};
    this.lapCount = { CurrentLap: 0, TotalLaps: 0 };
    this.raceControlMessages = [];
    this.carData = {};      // Latest telemetry per driver { "1": { speed, rpm, gear, throttle, brake, drs, battery } }
    this.positions = {};    // Latest X,Y,Z per driver { "1": { x, y, z, status } }
    this.lastUpdate = new Date().toISOString();
    this.connectionStatus = 'INITIALIZING';
    this.sessionEndedAt = null;
  }

  setConnectionStatus(status) {
    this.connectionStatus = status;
    this.lastUpdate = new Date().toISOString();
  }

  /**
   * Azzera i migliori tempi (BEST), i distacchi (GAP / INT) e i settori
   * al termine o al cambio di ogni sessione di qualifica (Q1, Q2, Q3).
   */
  resetQualifyingTimes() {
    const lines = this.timingData.Lines || {};
    for (const line of Object.values(lines)) {
      if (!line || typeof line !== 'object') continue;
      if (line.BestLapTime && typeof line.BestLapTime === 'object') {
        line.BestLapTime.Value = '';
      } else {
        line.BestLapTime = { Value: '' };
      }
      line.GapToLeader = '';
      line.IntervalToPositionAhead = '';
      line.TimeDiffToFastest = '';
      line.TimeDifftoPositionAhead = '';
      line.TimeDiffToPositionAhead = '';
      if (line.Sectors && typeof line.Sectors === 'object') {
        const secList = Array.isArray(line.Sectors) ? line.Sectors : Object.values(line.Sectors);
        for (const s of secList) {
          if (s && typeof s === 'object') {
            s.Value = '';
            s.OverallFastest = false;
            s.PersonalFastest = false;
          }
        }
      }
    }
  }

  isQualifyingSession() {
    if (this.timingData && this.timingData.SessionPart !== undefined && Number(this.timingData.SessionPart) > 0) {
      return true;
    }
    const sType = String((this.sessionInfo && (this.sessionInfo.Type || this.sessionInfo.Name)) || '').toLowerCase();
    return sType.includes('qualifying') || sType.includes('shootout') || sType.includes('qualifica');
  }

  isQualifyingPartEnded() {
    if (!this.isQualifyingSession()) return false;
    const st = String((this.sessionStatus && this.sessionStatus.Status) || '').toUpperCase();
    if (st === 'INACTIVE' || st === 'FINALISED' || st === 'ENDS' || st === 'ABORTED') {
      return true;
    }

    // Verifica se la bandiera a scacchi è stata sventolata e nessuna vettura sta più completando un giro lanciato
    const lastRc = this.raceControlMessages.length > 0
      ? this.raceControlMessages[this.raceControlMessages.length - 1]
      : null;
    const isChequered = st === 'FINISHED' || (lastRc && String(lastRc.Flag || '').toUpperCase() === 'CHEQUERED');

    if (isChequered) {
      const lines = Object.values(this.timingData.Lines || {});
      if (lines.length === 0) return true;
      const anyFlying = lines.some(l => {
        if (!l || l.Retired || l.Stopped || l.InPit || l.KnockedOut) return false;
        const code = Number(l.Status) || 0;
        // Status 1088 = ha già preso la bandiera a scacchi; 64 = ancora in giro lanciato
        if ((code & 1024) === 1024) return false;
        return true;
      });
      return !anyFlying;
    }

    return false;
  }

  isSessionEnded() {
    const st = String((this.sessionStatus && this.sessionStatus.Status) || '').toUpperCase();
    if (st === 'FINALISED' || st === 'FINISHED' || st === 'ENDS' || st === 'INACTIVE' || st === 'ABORTED') {
      return true;
    }
    const started = String((this.sessionStatus && this.sessionStatus.Started) || '').toUpperCase();
    if (started === 'FINISHED') return true;

    const archive = String((this.sessionInfo && this.sessionInfo.ArchiveStatus && this.sessionInfo.ArchiveStatus.Status) || '').toUpperCase();
    if (archive === 'GENERATING' || archive === 'COMPLETE') return true;

    const curLap = Number(this.lapCount && this.lapCount.CurrentLap) || 0;
    const totLaps = Number(this.lapCount && this.lapCount.TotalLaps) || 0;
    if (totLaps > 0 && curLap >= totLaps) {
      const hasChequered = Array.isArray(this.raceControlMessages) &&
        this.raceControlMessages.slice(-10).some(m => m && String(m.Flag || '').toUpperCase() === 'CHEQUERED');
      if (hasChequered) return true;
    }

    return false;
  }

  _checkAndUpdateSessionEnded() {
    if (this.isSessionEnded() && !this.sessionEndedAt) {
      this.sessionEndedAt = this.lastUpdate || new Date().toISOString();
    }
  }

  updateDrivers(data) {
    if (!data) return;
    deepMerge(this.driverList, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateTimingData(data) {
    if (!data) return;
    if (data.SessionPart !== undefined) {
      const nextPart = Number(data.SessionPart);
      if (this.currentSessionPart !== null && !isNaN(nextPart) && nextPart !== this.currentSessionPart) {
        this.resetQualifyingTimes();
      }
      if (!isNaN(nextPart) && nextPart > 0) {
        this.currentSessionPart = nextPart;
      }
    }
    deepMerge(this.timingData, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateTimingAppData(data) {
    if (!data) return;
    deepMerge(this.timingAppData, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateSessionInfo(data) {
    if (!data) return;
    deepMerge(this.sessionInfo, data);
    if (!this.lapCount.TotalLaps || this.lapCount.TotalLaps <= 0) {
      const cKey = data.Meeting?.Circuit?.Key || this.sessionInfo.Meeting?.Circuit?.Key;
      const cName = data.Meeting?.Circuit?.ShortName || data.Meeting?.Location || data.Meeting?.Name;
      const total = resolveF1CircuitTotalLaps(cKey, cName, data.Type || this.sessionInfo.Type);
      if (total > 0) {
        this.lapCount.TotalLaps = total;
      }
    }
    this._checkAndUpdateSessionEnded();
    this.lastUpdate = new Date().toISOString();
  }

  updateSessionStatus(data) {
    if (!data) return;
    deepMerge(this.sessionStatus, data);
    if (this.isQualifyingPartEnded()) {
      this.resetQualifyingTimes();
    }
    this._checkAndUpdateSessionEnded();
    this.lastUpdate = new Date().toISOString();
  }

  updateTrackStatus(data) {
    if (!data) return;
    deepMerge(this.trackStatus, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateSessionData(data) {
    if (!data) return;
    deepMerge(this.sessionData, data);
    if (data.StatusSeries) {
      const list = Array.isArray(data.StatusSeries) ? data.StatusSeries : Object.values(data.StatusSeries);
      for (let i = list.length - 1; i >= 0; i--) {
        if (list[i] && list[i].SessionStatus) {
          this.sessionStatus.Status = list[i].SessionStatus;
          break;
        }
      }
    }
    if (this.isQualifyingPartEnded()) {
      this.resetQualifyingTimes();
    }
    this._checkAndUpdateSessionEnded();
    this.lastUpdate = new Date().toISOString();
  }

  updateWeather(data) {
    if (!data) return;
    deepMerge(this.weatherData, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateLapCount(data) {
    if (!data) return;
    if (data.CurrentLap !== undefined) this.lapCount.CurrentLap = Number(data.CurrentLap);
    if (data.TotalLaps !== undefined && Number(data.TotalLaps) > 0) {
      this.lapCount.TotalLaps = Number(data.TotalLaps);
    }
    if (!this.lapCount.TotalLaps || this.lapCount.TotalLaps <= 0) {
      const cKey = this.sessionInfo.Meeting?.Circuit?.Key;
      const cName = this.sessionInfo.Meeting?.Circuit?.ShortName || this.sessionInfo.Meeting?.Location || this.sessionInfo.Meeting?.Name;
      const total = resolveF1CircuitTotalLaps(cKey, cName, this.sessionInfo.Type);
      if (total > 0) {
        this.lapCount.TotalLaps = total;
      }
    }
    this._checkAndUpdateSessionEnded();
    this.lastUpdate = new Date().toISOString();
  }

  addRaceControlMessage(message) {
    if (!message) return;
    const msgs = Array.isArray(message) ? message : [message];
    for (const m of msgs) {
      this.raceControlMessages.push(m);
    }
    // Keep last 100 messages
    if (this.raceControlMessages.length > 100) {
      this.raceControlMessages = this.raceControlMessages.slice(-100);
    }
    if (this.isQualifyingPartEnded()) {
      this.resetQualifyingTimes();
    }
    this._checkAndUpdateSessionEnded();
    this.lastUpdate = new Date().toISOString();
  }

  updateCarTelemetry(driverNum, telemetry) {
    const key = String(driverNum);
    if (!this.carData[key]) {
      this.carData[key] = {};
    }
    Object.assign(this.carData[key], telemetry);
    this.lastUpdate = new Date().toISOString();
  }

  updateDriverPosition(driverNum, pos) {
    const key = String(driverNum);
    if (!this.positions[key]) {
      this.positions[key] = {};
    }
    Object.assign(this.positions[key], pos);
    this.lastUpdate = new Date().toISOString();
  }

  // ─────────────────────────────────────────────────────────────
  // Query & View Transformers
  // ─────────────────────────────────────────────────────────────

  getLeaderboard() {
    const lines = this.timingData.Lines || {};
    const appLines = this.timingAppData.Lines || {};
    const drivers = this.driverList || {};

    const isQuali = this.isQualifyingSession();
    const isQualiEnded = this.isQualifyingPartEnded();
    const sessionPart = Number(this.timingData.SessionPart) || 0;
    const partIdx = sessionPart > 0 ? sessionPart - 1 : -1;

    const list = Object.keys(lines).map(num => {
      const line = lines[num] || {};
      const appLine = appLines[num] || {};
      const driver = drivers[num] || {};
      const car = this.carData[num] || {};
      const pos = this.positions[num] || {};

      const posNum = parseInt(line.Position, 10);
      const isRetired = Boolean(line.Retired || line.Stopped);
      const isKnockedOut = Boolean(line.KnockedOut);
      const inPit = Boolean(line.InPit);
      const pitOut = Boolean(line.PitOut);

      // Extract tyre stints (supports Array or indexed Object {"0": ..., "1": ...})
      const stintsRaw = appLine.Stints;
      let currentStint = null;
      if (Array.isArray(stintsRaw)) {
        for (let i = stintsRaw.length - 1; i >= 0; i--) {
          const s = stintsRaw[i];
          if (s && (s.Compound || s.compound)) {
            currentStint = s;
            break;
          }
        }
        if (!currentStint && stintsRaw.length > 0) {
          currentStint = stintsRaw[stintsRaw.length - 1];
        }
      } else if (stintsRaw && typeof stintsRaw === 'object') {
        const keys = Object.keys(stintsRaw).sort((a, b) => Number(a) - Number(b));
        for (let i = keys.length - 1; i >= 0; i--) {
          const s = stintsRaw[keys[i]];
          if (s && (s.Compound || s.compound)) {
            currentStint = s;
            break;
          }
        }
        if (!currentStint && keys.length > 0) {
          currentStint = stintsRaw[keys[keys.length - 1]];
        }
      }

      let bestLap = null;
      let gapVal = null;
      let intVal = null;

      if (isQualiEnded || (isQuali && isKnockedOut)) {
        // Al termine di Q1/Q2/Q3 o per piloti eliminati nella sessione precedente: azzera BEST, GAP e INT
        bestLap = null;
        gapVal = null;
        intVal = null;
      } else if (isQuali && partIdx >= 0 && line.BestLapTimes) {
        // In Qualifica con SessionPart (1=Q1, 2=Q2, 3=Q3): leggi SOLO il tempo della sessione corrente
        const blPart = Array.isArray(line.BestLapTimes)
          ? line.BestLapTimes[partIdx]
          : line.BestLapTimes[String(partIdx)];
        bestLap = (blPart && blPart.Value) ? blPart.Value : null;

        if (bestLap) {
          if (line.Stats) {
            const stPart = Array.isArray(line.Stats)
              ? line.Stats[partIdx]
              : line.Stats[String(partIdx)];
            if (stPart) {
              if (stPart.TimeDiffToFastest) {
                gapVal = typeof stPart.TimeDiffToFastest === 'object' ? stPart.TimeDiffToFastest.Value : stPart.TimeDiffToFastest;
              }
              const rawInt = stPart.TimeDifftoPositionAhead || stPart.TimeDiffToPositionAhead;
              if (rawInt) {
                intVal = typeof rawInt === 'object' ? rawInt.Value : rawInt;
              }
            }
          }
          if (!gapVal && line.TimeDiffToFastest) {
            gapVal = typeof line.TimeDiffToFastest === 'object' ? line.TimeDiffToFastest.Value : line.TimeDiffToFastest;
          }
          if (!intVal && (line.TimeDifftoPositionAhead || line.TimeDiffToPositionAhead)) {
            const rawInt = line.TimeDifftoPositionAhead || line.TimeDiffToPositionAhead;
            intVal = typeof rawInt === 'object' ? rawInt.Value : rawInt;
          }
        }
      } else {
        // Prove Libere, Gara o Qualifica senza array BestLapTimes indicizzato per SessionPart
        bestLap = line.BestLapTime ? (typeof line.BestLapTime === 'object' ? line.BestLapTime.Value : line.BestLapTime) : null;
        if (!bestLap && line.BestLapTimes) {
          const blList = Array.isArray(line.BestLapTimes) ? line.BestLapTimes : Object.values(line.BestLapTimes);
          for (let i = blList.length - 1; i >= 0; i--) {
            const bl = blList[i];
            if (bl && bl.Value) {
              bestLap = bl.Value;
              break;
            }
          }
        }

        gapVal = line.GapToLeader ? (typeof line.GapToLeader === 'object' ? line.GapToLeader.Value : line.GapToLeader) : null;
        if (!gapVal && line.TimeDiffToFastest) {
          gapVal = typeof line.TimeDiffToFastest === 'object' ? line.TimeDiffToFastest.Value : line.TimeDiffToFastest;
        }
        if (!gapVal && line.Stats) {
          const statsList = Array.isArray(line.Stats) ? line.Stats : Object.values(line.Stats);
          for (let i = statsList.length - 1; i >= 0; i--) {
            const st = statsList[i];
            if (st && st.TimeDiffToFastest) {
              gapVal = typeof st.TimeDiffToFastest === 'object' ? st.TimeDiffToFastest.Value : st.TimeDiffToFastest;
              if (gapVal) break;
            }
          }
        }

        intVal = line.IntervalToPositionAhead ? (typeof line.IntervalToPositionAhead === 'object' ? line.IntervalToPositionAhead.Value : line.IntervalToPositionAhead) : null;
        if (!intVal && (line.TimeDifftoPositionAhead || line.TimeDiffToPositionAhead)) {
          const rawInt = line.TimeDifftoPositionAhead || line.TimeDiffToPositionAhead;
          intVal = typeof rawInt === 'object' ? rawInt.Value : rawInt;
        }
        if (!intVal && line.Stats) {
          const statsList = Array.isArray(line.Stats) ? line.Stats : Object.values(line.Stats);
          for (let i = statsList.length - 1; i >= 0; i--) {
            const st = statsList[i];
            const rawInt = st && (st.TimeDifftoPositionAhead || st.TimeDiffToPositionAhead);
            if (rawInt) {
              intVal = typeof rawInt === 'object' ? rawInt.Value : rawInt;
              if (intVal) break;
            }
          }
        }
      }

      const rawCompound = currentStint ? (currentStint.Compound || currentStint.compound) : null;
      const tyreCompound = rawCompound ? String(rawCompound).toUpperCase() : 'UNKNOWN';
      const stintTot = currentStint ? (Number(currentStint.TotalLaps) || 0) : 0;
      const startLaps = currentStint ? (Number(currentStint.StartLaps) || 0) : 0;
      const stintLapNum = currentStint && currentStint.LapNumber !== undefined ? Number(currentStint.LapNumber) : 0;
      const driverCompletedLaps = line.NumberOfLaps !== undefined ? Number(line.NumberOfLaps) : (this.lapCount.CurrentLap || 0);

      let tyreAge = 0;
      if (stintTot > 0) {
        tyreAge = stintTot;
      } else if (driverCompletedLaps > 0 && stintLapNum > 0 && driverCompletedLaps >= stintLapNum) {
        tyreAge = startLaps + (driverCompletedLaps - stintLapNum);
      } else if (driverCompletedLaps > 0) {
        tyreAge = Math.max(1, driverCompletedLaps);
      } else {
        tyreAge = startLaps > 0 ? startLaps : 0;
      }
      const tyreNew = currentStint ? (currentStint.New === true || currentStint.New === 'true') : false;

      return {
        racingNumber: num,
        position: isNaN(posNum) ? 99 : posNum,
        displayPosition: line.Position || '--',
        tla: driver.Tla || line.Tla || num,
        broadcastName: driver.BroadcastName || line.BroadcastName || `DRIVER ${num}`,
        fullName: driver.FullName || driver.BroadcastName || `Driver ${num}`,
        teamName: driver.TeamName || 'Unknown Team',
        teamColour: driver.TeamColour ? (driver.TeamColour.startsWith('#') ? driver.TeamColour : `#${driver.TeamColour}`) : '#FFFFFF',
        gapToLeader: gapVal || '--',
        intervalToAhead: intVal || '--',
        bestLapTime: bestLap || '--',
        lastLapTime: line.LastLapTime ? (typeof line.LastLapTime === 'object' ? line.LastLapTime.Value : line.LastLapTime) : '--',
        bestLapOverallFastest: Boolean(line.BestLapTime && line.BestLapTime.OverallFastest),
        bestLapPersonalFastest: Boolean(line.BestLapTime && line.BestLapTime.PersonalFastest),
        lastLapOverallFastest: Boolean(line.LastLapTime && line.LastLapTime.OverallFastest),
        lastLapPersonalFastest: Boolean(line.LastLapTime && line.LastLapTime.PersonalFastest),
        sectors: line.Sectors || {},
        speeds: line.Speeds || {},
        numberOfLaps: driverCompletedLaps,
        numberOfPitStops: line.NumberOfPitStops !== undefined ? line.NumberOfPitStops : 0,
        inPit,
        pitOut,
        retired: isRetired,
        knockedOut: isKnockedOut,
        status: isRetired ? 'RET' : (inPit ? 'PIT' : (pitOut ? 'OUT' : 'TRACK')),
        tyreCompound,
        tyreAge,
        tyreNew,
        telemetry: car,
        trackCoords: pos
      };
    });

    // Sort: Active cars by position (or bestLapTime if tied), then retired at bottom
    list.sort((a, b) => {
      if (a.retired && !b.retired) return 1;
      if (!a.retired && b.retired) return -1;
      if (a.position !== b.position) return a.position - b.position;
      const aSec = parseLapTimeToSeconds(a.bestLapTime) || Infinity;
      const bSec = parseLapTimeToSeconds(b.bestLapTime) || Infinity;
      return aSec - bSec;
    });

    // Dynamic calculation of GAP (distacco dal primo) and INT (distacco dal pilota davanti)
    if (list.length > 0) {
      if (isQualiEnded) {
        for (const row of list) {
          row.bestLapTime = '--';
          row.gapToLeader = '--';
          row.intervalToAhead = '--';
        }
        return list;
      }

      const leader = list[0];
      const leaderLapSec = parseLapTimeToSeconds(leader.bestLapTime);

      if (isQuali && !leaderLapSec) {
        leader.gapToLeader = '--';
        leader.intervalToAhead = '--';
      } else {
        leader.gapToLeader = 'LEADER';
        leader.intervalToAhead = '--';
      }

      for (let i = 1; i < list.length; i++) {
        const curr = list[i];
        const ahead = list[i - 1];

        const currLapSec = parseLapTimeToSeconds(curr.bestLapTime);
        const aheadLapSec = parseLapTimeToSeconds(ahead.bestLapTime);

        const isGapMissing = !curr.gapToLeader || curr.gapToLeader === '--' || curr.gapToLeader === '-' || curr.gapToLeader === 'LEADER';
        const isIntMissing = !curr.intervalToAhead || curr.intervalToAhead === '--' || curr.intervalToAhead === '-' || curr.intervalToAhead === 'LEADER';

        // Se in Qualifica (o senza distacco gara) il pilota non ha un BEST valido, GAP e INT restano azzerati ('--')
        if (!currLapSec && isGapMissing && isIntMissing) {
          curr.gapToLeader = '--';
          curr.intervalToAhead = '--';
          continue;
        }

        // 1. Calcolo dinamico GAP (distacco dal primo)
        if (isGapMissing) {
          if (currLapSec > 0 && leaderLapSec > 0 && currLapSec >= leaderLapSec) {
            curr.gapToLeader = '+' + (currLapSec - leaderLapSec).toFixed(3);
          } else {
            const aheadGapSec = i === 1 ? 0 : parseDeltaToSeconds(ahead.gapToLeader);
            const currIntSec = parseDeltaToSeconds(curr.intervalToAhead);
            if (aheadGapSec !== null && currIntSec !== null) {
              curr.gapToLeader = '+' + (aheadGapSec + currIntSec).toFixed(3);
            }
          }
        }

        // 2. Calcolo dinamico INT (distacco dal pilota davanti)
        if (isIntMissing) {
          const currGapSec = parseDeltaToSeconds(curr.gapToLeader);
          const aheadGapSec = i === 1 ? 0 : parseDeltaToSeconds(ahead.gapToLeader);

          if (currGapSec !== null && aheadGapSec !== null && currGapSec >= aheadGapSec) {
            curr.intervalToAhead = '+' + (currGapSec - aheadGapSec).toFixed(3);
          } else if (currLapSec > 0 && aheadLapSec > 0 && currLapSec >= aheadLapSec) {
            curr.intervalToAhead = '+' + (currLapSec - aheadLapSec).toFixed(3);
          }
        }

        // 3. Fallback secondario per GAP tramite accumulo INT
        if (!curr.gapToLeader || curr.gapToLeader === '--' || curr.gapToLeader === '-') {
          const aheadGapSec = i === 1 ? 0 : parseDeltaToSeconds(ahead.gapToLeader);
          const currIntSec = parseDeltaToSeconds(curr.intervalToAhead);
          if (aheadGapSec !== null && currIntSec !== null) {
            curr.gapToLeader = '+' + (aheadGapSec + currIntSec).toFixed(3);
          }
        }
      }
    }

    return list;
  }

  getTyresSummary() {
    const appLines = this.timingAppData.Lines || {};
    const drivers = this.driverList || {};
    const result = {};

    for (const [num, appLine] of Object.entries(appLines)) {
      const driver = drivers[num] || {};
      result[num] = {
        racingNumber: num,
        tla: driver.Tla || num,
        teamColour: driver.TeamColour || '#FFFFFF',
        gridPos: appLine.GridPos || null,
        stints: appLine.Stints || []
      };
    }
    return result;
  }

  getStatus() {
    return {
      service: 'fastestlap-livetiming',
      status: this.connectionStatus,
      lastUpdate: this.lastUpdate,
      currentLap: this.lapCount.CurrentLap || 0,
      totalLaps: this.lapCount.TotalLaps || 0,
      driverCount: Object.keys(this.driverList).length,
      sessionPart: this.timingData.SessionPart || null,
      sessionStatus: this.sessionStatus.Status || null,
      trackStatus: this.trackStatus.Status || '1',
      isSessionEnded: this.isSessionEnded(),
      sessionEndedAt: this.sessionEndedAt || (this.isSessionEnded() ? this.lastUpdate : null)
    };
  }

  getFullSnapshot() {
    return {
      status: this.getStatus(),
      session: this.sessionInfo,
      sessionStatus: this.sessionStatus,
      trackStatus: this.trackStatus,
      weather: this.weatherData,
      lapCount: this.lapCount,
      raceControl: this.raceControlMessages.slice(-20),
      leaderboard: this.getLeaderboard(),
      drivers: this.driverList,
      telemetry: this.carData,
      positions: this.positions,
      isSessionEnded: this.isSessionEnded(),
      sessionEndedAt: this.sessionEndedAt || (this.isSessionEnded() ? this.lastUpdate : null)
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    StateStore,
    deepMerge,
    isPlainObject,
    parseLapTimeToSeconds,
    parseDeltaToSeconds
  };
}
if (typeof window !== 'undefined') {
  window.StateStore = StateStore;
  window.deepMerge = deepMerge;
  window.parseLapTimeToSeconds = parseLapTimeToSeconds;
  window.parseDeltaToSeconds = parseDeltaToSeconds;
}

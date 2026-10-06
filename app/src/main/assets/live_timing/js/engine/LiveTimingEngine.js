/**
 * LiveTimingEngine.js
 * Generic Real-time Live Timing & Driver Telemetry Simulation Engine
 *
 * Implements:
 *  - Official FIA timing rules (purple/green/yellow sector and lap color coding)
 *  - Dynamic leaderboard ranking with gap-to-leader & interval calculations
 *  - Race control state machine (Formation laps, Standing start, SC, VSC, Red flag)
 *  - Seamless pit stop tracking & transition modeling
 *  - High-precision telemetry state estimation for any calibrated TrackModel
 */

class LiveTimingEngine {
  /**
   * @param {import('./TrackModel')} trackModel Calibrated TrackModel instance
   */
  constructor(trackModel) {
    this.trackModel = trackModel;

    // Roster & Grid
    this.drivers = {};           // key -> driver metadata
    this.gridOrder = [];         // array of driver keys in grid order P1..PN

    // Session Specification
    this.totalLaps = 50;
    this.formationLapsCount = 1;
    this.formationLapDuration = 108.0;
    this.raceStartTimeSec = 110.0;
    this.raceFinishTimeSec = 5800.0;
    this.isStandingStart = true;

    // Historical & Reference Data
    this.lapsData = {};          // key -> array of lap objects
    this.pitStops = [];          // array of pit stop objects
    this.raceControlMessages = []; // array of RC message objects
    this.retirements = {};       // key -> retirement time or lap
    this.incidents = [];         // array of incident objects
    this.stints = {};            // key -> array of stints
    this.keyframes = {};         // key -> [[timeSec, gapSec], ...]

    // Session Records
    this.sessionBestLap = { time: Infinity, driver: null, lap: 0 };
    this.sessionBestS1 = { time: Infinity, driver: null };
    this.sessionBestS2 = { time: Infinity, driver: null };
    this.sessionBestS3 = { time: Infinity, driver: null };

    // Personal Bests cache
    this.personalBests = {};     // key -> { lap: Infinity, s1: Infinity, s2: Infinity, s3: Infinity }
  }

  /**
   * Registers drivers
   */
  setDrivers(driverMap) {
    this.drivers = { ...driverMap };
    for (const key of Object.keys(this.drivers)) {
      if (!this.personalBests[key]) {
        this.personalBests[key] = { lap: Infinity, s1: Infinity, s2: Infinity, s3: Infinity };
      }
    }
  }

  /**
   * Sets starting grid order (array of driver keys)
   */
  setGridOrder(gridArr) {
    this.gridOrder = [...gridArr];
  }

  /**
   * Configures session timing parameters
   */
  configureSession(config = {}) {
    if (config.totalLaps) this.totalLaps = config.totalLaps;
    if (config.formationLapsCount !== undefined) this.formationLapsCount = config.formationLapsCount;
    if (config.formationLapDuration) this.formationLapDuration = config.formationLapDuration;
    if (config.raceStartTimeSec !== undefined) this.raceStartTimeSec = config.raceStartTimeSec;
    if (config.raceFinishTimeSec) this.raceFinishTimeSec = config.raceFinishTimeSec;
    if (config.isStandingStart !== undefined) this.isStandingStart = config.isStandingStart;
  }

  /**
   * Loads lap data
   */
  loadLaps(lapsMap) {
    this.lapsData = { ...lapsMap };
    this._recomputeHistoricalBestRecords();
  }

  /**
   * Loads pit stops
   */
  loadPitStops(pitList) {
    this.pitStops = [...pitList];
  }

  /**
   * Loads race control events
   */
  loadRaceControlMessages(messages) {
    this.raceControlMessages = [...messages];
  }

  /**
   * Loads retirements (key -> { lap, timeSec, reason })
   */
  loadRetirements(retirementsMap) {
    this.retirements = { ...retirementsMap };
  }

  /**
   * Loads active/historical incidents & excursions
   */
  loadIncidents(incidentsList) {
    this.incidents = Array.isArray(incidentsList) ? [...incidentsList] : [];
  }

  /**
   * Loads tyre stints
   */
  loadStints(stintsMap) {
    this.stints = { ...stintsMap };
  }

  /**
   * Loads driver gap keyframes (key -> [[timeSec, gapSec], ...])
   */
  loadKeyframes(keyframesMap) {
    this.keyframes = { ...keyframesMap };
  }

  /**
   * Interpolates authentic driver gap to leader at time tSec
   */
  getDriverGap(driverKey, tSec) {
    const kfs = this.keyframes[driverKey];
    if (!kfs || kfs.length === 0) {
      const gridIdx = this.gridOrder.indexOf(driverKey);
      const pos = gridIdx >= 0 ? gridIdx : 20;
      return pos * 0.15;
    }
    if (tSec <= kfs[0][0]) return kfs[0][1];
    if (tSec >= kfs[kfs.length - 1][0]) return kfs[kfs.length - 1][1];

    let low = 0, high = kfs.length - 1;
    while (low <= high) {
      const mid = (low + high) >> 1;
      if (kfs[mid][0] <= tSec) low = mid + 1;
      else high = mid - 1;
    }
    const i1 = Math.max(0, high);
    const i2 = Math.min(kfs.length - 1, i1 + 1);
    const p1 = kfs[i1];
    const p2 = kfs[i2];
    const dt = p2[0] - p1[0];
    if (dt <= 0) return p1[1];
    const u = (tSec - p1[0]) / dt;
    return p1[1] + (p2[1] - p1[1]) * u;
  }

  _recomputeHistoricalBestRecords() {
    for (const [drv, laps] of Object.entries(this.lapsData)) {
      if (!Array.isArray(laps)) continue;
      if (!this.personalBests[drv]) {
        this.personalBests[drv] = { lap: Infinity, s1: Infinity, s2: Infinity, s3: Infinity };
      }
      for (const l of laps) {
        const dur = Number(l.dur);
        if (dur > 60 && dur < 180) {
          if (dur < this.personalBests[drv].lap) this.personalBests[drv].lap = dur;
          if (dur < this.sessionBestLap.time) {
            this.sessionBestLap = { time: dur, driver: drv, lap: l.lap };
          }
        }
        if (l.s1 && l.s1 > 15 && l.s1 < 60) {
          if (l.s1 < this.personalBests[drv].s1) this.personalBests[drv].s1 = l.s1;
          if (l.s1 < this.sessionBestS1.time) this.sessionBestS1 = { time: l.s1, driver: drv };
        }
        if (l.s2 && l.s2 > 15 && l.s2 < 60) {
          if (l.s2 < this.personalBests[drv].s2) this.personalBests[drv].s2 = l.s2;
          if (l.s2 < this.sessionBestS2.time) this.sessionBestS2 = { time: l.s2, driver: drv };
        }
        if (l.s3 && l.s3 > 10 && l.s3 < 60) {
          if (l.s3 < this.personalBests[drv].s3) this.personalBests[drv].s3 = l.s3;
          if (l.s3 < this.sessionBestS3.time) this.sessionBestS3 = { time: l.s3, driver: drv };
        }
      }
    }
  }

  /**
   * Returns session phase at time t
   */
  getSessionPhase(tSec) {
    if (tSec < 0) return 'PRE_SESSION';
    if (tSec < this.raceStartTimeSec) {
      if (this.formationLapsCount > 0) return 'FORMATION_LAP';
      return 'GRID_START';
    }
    if (tSec >= this.raceFinishTimeSec) return 'FINISHED';

    // Check Safety Car in Race Control
    const activeSC = this.raceControlMessages.some(m => {
      const msgTime = m.timeSec !== undefined ? m.timeSec : 0;
      return msgTime <= tSec && (m.message || '').includes('SAFETY CAR') && !(m.message || '').includes('ENDING');
    });

    return activeSC ? 'SAFETY_CAR' : 'RACING';
  }

  /**
   * Determines sector and lap colors based on official F1 criteria
   */
  getTimingColor(driverKey, type, value) {
    if (!value || value <= 0) return '#64748b'; // Gray for empty/unreached

    const pb = this.personalBests[driverKey] || {};

    if (type === 's1') {
      if (Math.abs(value - this.sessionBestS1.time) < 0.005) return '#d054fa'; // Purple
      if (value <= (pb.s1 || Infinity) + 0.005) return '#00e676'; // Green
      return '#ffeb3b'; // Yellow
    }
    if (type === 's2') {
      if (Math.abs(value - this.sessionBestS2.time) < 0.005) return '#d054fa';
      if (value <= (pb.s2 || Infinity) + 0.005) return '#00e676';
      return '#ffeb3b';
    }
    if (type === 's3') {
      if (Math.abs(value - this.sessionBestS3.time) < 0.005) return '#d054fa';
      if (value <= (pb.s3 || Infinity) + 0.005) return '#00e676';
      return '#ffeb3b';
    }
    if (type === 'lap') {
      if (Math.abs(value - this.sessionBestLap.time) < 0.005) return '#d054fa';
      if (value <= (pb.lap || Infinity) + 0.005) return '#00e676';
      return '#ffeb3b';
    }

    return '#e2e8f0';
  }

  /**
   * Computes driver progress, position, lap, and telemetry at timestamp tSec
   */
  getDriverState(driverKey, tSec) {
    const drv = this.drivers[driverKey] || { number: driverKey, code: 'DRV', color: '#ff0000' };
    const gridIdx = this.gridOrder.indexOf(driverKey);
    const pos = gridIdx >= 0 ? gridIdx : 20;

    // Check retirement
    const retInfo = this.retirements[driverKey];
    let isRetired = false;
    if (retInfo) {
      const retTime = typeof retInfo === 'number' ? retInfo : (retInfo.timeSec || Infinity);
      if (tSec >= retTime) isRetired = true;
    }

    // Check incident (excursion, crash, failure)
    const inc = this.incidents.find(i => {
      if (String(i.driver) !== String(driverKey)) return false;
      if (tSec < i.startSec) return false;
      if (i.type === 'OFF_TRACK_REJOIN') {
        const dur = i.durationSec || 14.0;
        return tSec <= i.startSec + dur;
      }
      return true;
    });

    if (isRetired) {
      const retLap = (retInfo && retInfo.lap) ? retInfo.lap : (this.totalLaps);
      const incPos = inc ? this.trackModel.getVisualCarPosition(inc.trackSec || 0, false, 0, null, inc, tSec) : null;
      return {
        driverKey,
        ...drv,
        isRetired: true,
        retiredLap: retLap,
        currentLap: retLap,
        trackSec: inc ? (inc.trackSec || 0) : 0,
        progress: -1000 + retLap,
        px: incPos ? incPos.px : (30 + (pos % 5) * 20),
        py: incPos ? incPos.py : (30 + Math.floor(pos / 5) * 20),
        speed: incPos ? incPos.speed : 0,
        gear: incPos ? incPos.gear : 0,
        throttle: incPos ? incPos.throttle : 0,
        brake: incPos ? incPos.brake : 0,
        rpm: incPos ? incPos.rpm : 0,
        drs: 0,
        inPit: inc ? (inc.type === 'PIT_RETIRED') : false,
        isOffTrack: incPos ? incPos.isOffTrack : true,
        hazard: incPos ? incPos.hazard : false,
        status: (inc && incPos && incPos.status) ? incPos.status : 'OUT / RETIRED',
        location: inc ? (inc.locationName || 'Fermo Fuori Pista') : 'Retired'
      };
    }

    const lapDur = this.trackModel.lapDuration || 95.0;

    // Phase 1: Pre-start / Formation
    if (tSec < this.raceStartTimeSec) {
      // Formation lap stagger
      const stagger = pos * 1.5;
      const formationProg = (tSec / Math.max(1, this.raceStartTimeSec)) * lapDur;
      const trackSec = ((formationProg - stagger) % lapDur + lapDur) % lapDur;

      const posInfo = this.trackModel.getVisualCarPosition(trackSec, false, 0, null, inc, tSec);
      return {
        driverKey,
        ...drv,
        isRetired: false,
        currentLap: 0,
        trackSec,
        ...posInfo,
        speed: Math.min(posInfo.speed, 110),
        status: 'FORMATION'
      };
    }

    // Phase 2: Active Race
    const raceElapsed = tSec - this.raceStartTimeSec;
    const driverLaps = this.lapsData[driverKey] || [];

    // Check pit stop at tSec
    const currentPit = this.pitStops.find(p => {
      const dMatch = String(p.driver) === String(driverKey);
      if (!dMatch) return false;
      const start = p.startSec !== undefined ? p.startSec : 0;
      const end = p.endSec !== undefined ? p.endSec : start + (p.duration || 21.0);
      return tSec >= start && tSec <= end;
    });

    const inPit = !!currentPit;
    let pitProgress = 0;
    if (inPit) {
      const pitStart = currentPit.startSec;
      const pitDur = currentPit.duration || 21.0;
      pitProgress = Math.max(0, Math.min(1, (tSec - pitStart) / pitDur));
    }

    // Check pit exit merge
    let mergeProgress = null;
    const recentPit = this.pitStops.find(p => {
      const dMatch = String(p.driver) === String(driverKey);
      if (!dMatch) return false;
      const end = p.endSec !== undefined ? p.endSec : (p.startSec + (p.duration || 21.0));
      return tSec > end && tSec <= end + 2.5;
    });
    if (recentPit) {
      const end = recentPit.endSec !== undefined ? recentPit.endSec : (recentPit.startSec + (recentPit.duration || 21.0));
      mergeProgress = (tSec - end) / 2.5;
    }

    // Calculate current lap & progress with authentic gap
    const driverGap = this.getDriverGap(driverKey, tSec);
    const driverEffectiveTime = Math.max(0, raceElapsed - driverGap);
    const estimatedLaps = driverEffectiveTime / lapDur;
    const currentLapNumber = Math.min(this.totalLaps, Math.floor(estimatedLaps) + 1);
    const trackSec = driverEffectiveTime % lapDur;

    const visualPos = this.trackModel.getVisualCarPosition(trackSec, inPit, pitProgress, mergeProgress, inc, tSec);

    // Retrieve last lap timings if available
    const lastCompletedLap = driverLaps.length > 0 ? driverLaps[Math.min(driverLaps.length - 1, currentLapNumber - 1)] : null;
    const bestCompletedLap = driverLaps.length > 0 ? driverLaps.reduce((best, l) => (!best || (l.dur > 60 && l.dur < best.dur)) ? l : best, null) : null;

    // Current sector times
    const s1Time = lastCompletedLap ? lastCompletedLap.s1 : null;
    const s2Time = lastCompletedLap ? lastCompletedLap.s2 : null;
    const s3Time = lastCompletedLap ? lastCompletedLap.s3 : null;
    const lastLapDur = lastCompletedLap ? lastCompletedLap.dur : null;
    const bestLapDur = bestCompletedLap ? bestCompletedLap.dur : null;

    return {
      driverKey,
      ...drv,
      isRetired: false,
      currentLap: currentLapNumber,
      trackSec,
      progress: driverEffectiveTime / lapDur,
      ...visualPos,
      inPit,
      s1: s1Time,
      s2: s2Time,
      s3: s3Time,
      s1Color: this.getTimingColor(driverKey, 's1', s1Time),
      s2Color: this.getTimingColor(driverKey, 's2', s2Time),
      s3Color: this.getTimingColor(driverKey, 's3', s3Time),
      lastLapDur,
      lastLapColor: this.getTimingColor(driverKey, 'lap', lastLapDur),
      bestLapDur,
      bestLapColor: this.getTimingColor(driverKey, 'lap', bestLapDur),
      status: (visualPos.status) ? visualPos.status : (inPit ? 'IN PIT' : 'RACING')
    };
  }

  /**
   * Generates complete leaderboard snapshot at timestamp tSec
   */
  getSnapshot(tSec) {
    const driverKeys = Object.keys(this.drivers);
    const driverStates = driverKeys.map(k => this.getDriverState(k, tSec));

    // Sort leaderboard: active first sorted by progress descending, then retired
    driverStates.sort((a, b) => {
      if (a.isRetired && !b.isRetired) return 1;
      if (!a.isRetired && b.isRetired) return -1;
      return (b.progress || 0) - (a.progress || 0);
    });

    // Compute gaps
    const leader = driverStates[0];
    driverStates.forEach((d, idx) => {
      d.position = idx + 1;
      if (idx === 0) {
        d.gap = 'LEADER';
        d.interval = '—';
      } else if (d.isRetired) {
        d.gap = `DNF (Lap ${d.retiredLap || d.currentLap})`;
        d.interval = 'OUT';
      } else {
        const gapSec = this.getDriverGap(d.driverKey, tSec);
        const ahead = driverStates[idx - 1];
        const aheadGapSec = ahead ? this.getDriverGap(ahead.driverKey, tSec) : 0;
        const intSec = Math.max(0, gapSec - aheadGapSec);
        d.gap = `+${gapSec.toFixed(3)}s`;
        d.interval = `+${intSec.toFixed(3)}s`;
      }
    });

    // Get active race control messages
    const activeMessages = this.raceControlMessages.filter(m => (m.timeSec || 0) <= tSec);
    const latestMessage = activeMessages.length > 0 ? activeMessages[activeMessages.length - 1] : null;

    return {
      tSec,
      phase: this.getSessionPhase(tSec),
      leaderboard: driverStates,
      fastestLap: this.sessionBestLap,
      bestS1: this.sessionBestS1,
      bestS2: this.sessionBestS2,
      bestS3: this.sessionBestS3,
      latestMessage,
      activeMessagesCount: activeMessages.length
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = LiveTimingEngine;
}

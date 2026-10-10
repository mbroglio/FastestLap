/**
 * FastestLap LiveTiming - State Store
 *
 * In-memory state manager with deep-delta merging for F1 SignalR Core streams.
 * Preserves 1:1 PascalCase field names while providing query APIs for the frontend.
 */

function isPlainObject(item) {
  return item !== null && typeof item === 'object' && !Array.isArray(item) && !(item instanceof Date);
}

function deepMerge(target, source) {
  if (!isPlainObject(target) || !isPlainObject(source)) {
    return source !== undefined ? source : target;
  }

  for (const key of Object.keys(source)) {
    const sourceVal = source[key];
    if (sourceVal !== undefined) {
      if (isPlainObject(sourceVal)) {
        if (!isPlainObject(target[key])) {
          target[key] = {};
        }
        deepMerge(target[key], sourceVal);
      } else if (Array.isArray(sourceVal)) {
        target[key] = sourceVal.slice();
      } else {
        target[key] = sourceVal;
      }
    }
  }

  return target;
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
    this.weatherData = {};
    this.lapCount = { CurrentLap: 0, TotalLaps: 0 };
    this.raceControlMessages = [];
    this.carData = {};      // Latest telemetry per driver { "1": { speed, rpm, gear, throttle, brake, drs, battery } }
    this.positions = {};    // Latest X,Y,Z per driver { "1": { x, y, z, status } }
    this.lastUpdate = new Date().toISOString();
    this.connectionStatus = 'INITIALIZING';
  }

  setConnectionStatus(status) {
    this.connectionStatus = status;
    this.lastUpdate = new Date().toISOString();
  }

  updateDrivers(data) {
    if (!data) return;
    deepMerge(this.driverList, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateTimingData(data) {
    if (!data) return;
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
    this.lastUpdate = new Date().toISOString();
  }

  updateWeather(data) {
    if (!data) return;
    deepMerge(this.weatherData, data);
    this.lastUpdate = new Date().toISOString();
  }

  updateLapCount(data) {
    if (!data) return;
    deepMerge(this.lapCount, data);
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

    const list = Object.keys(lines).map(num => {
      const line = lines[num] || {};
      const appLine = appLines[num] || {};
      const driver = drivers[num] || {};
      const car = this.carData[num] || {};
      const pos = this.positions[num] || {};

      const posNum = parseInt(line.Position, 10);
      const isRetired = Boolean(line.Retired || line.Stopped);
      const inPit = Boolean(line.InPit);
      const pitOut = Boolean(line.PitOut);

      // Extract tyre stints
      const stints = appLine.Stints || [];
      const currentStint = stints.length > 0 ? stints[stints.length - 1] : null;

      return {
        racingNumber: num,
        position: isNaN(posNum) ? 99 : posNum,
        displayPosition: line.Position || '--',
        tla: driver.Tla || line.Tla || num,
        broadcastName: driver.BroadcastName || line.BroadcastName || `DRIVER ${num}`,
        fullName: driver.FullName || driver.BroadcastName || `Driver ${num}`,
        teamName: driver.TeamName || 'Unknown Team',
        teamColour: driver.TeamColour ? (driver.TeamColour.startsWith('#') ? driver.TeamColour : `#${driver.TeamColour}`) : '#FFFFFF',
        gapToLeader: line.GapToLeader ? (typeof line.GapToLeader === 'object' ? line.GapToLeader.Value : line.GapToLeader) : '--',
        intervalToAhead: line.IntervalToPositionAhead ? (typeof line.IntervalToPositionAhead === 'object' ? line.IntervalToPositionAhead.Value : line.IntervalToPositionAhead) : '--',
        bestLapTime: line.BestLapTime ? (typeof line.BestLapTime === 'object' ? line.BestLapTime.Value : line.BestLapTime) : '--',
        lastLapTime: line.LastLapTime ? (typeof line.LastLapTime === 'object' ? line.LastLapTime.Value : line.LastLapTime) : '--',
        sectors: line.Sectors || {},
        speeds: line.Speeds || {},
        numberOfPitStops: line.NumberOfPitStops !== undefined ? line.NumberOfPitStops : 0,
        inPit,
        pitOut,
        retired: isRetired,
        status: isRetired ? 'RET' : (inPit ? 'PIT' : (pitOut ? 'OUT' : 'TRACK')),
        tyreCompound: currentStint ? (currentStint.Compound || 'MEDIUM') : 'MEDIUM',
        tyreAge: currentStint ? (currentStint.TotalLaps || 0) : 0,
        tyreNew: currentStint ? Boolean(currentStint.New) : false,
        telemetry: car,
        trackCoords: pos
      };
    });

    // Sort: Active cars by position, then retired at bottom
    list.sort((a, b) => {
      if (a.retired && !b.retired) return 1;
      if (!a.retired && b.retired) return -1;
      return a.position - b.position;
    });

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
      driverCount: Object.keys(this.driverList).length
    };
  }

  getFullSnapshot() {
    return {
      status: this.getStatus(),
      session: this.sessionInfo,
      weather: this.weatherData,
      lapCount: this.lapCount,
      raceControl: this.raceControlMessages.slice(-20),
      leaderboard: this.getLeaderboard(),
      drivers: this.driverList,
      telemetry: this.carData,
      positions: this.positions
    };
  }
}

module.exports = {
  StateStore,
  deepMerge,
  isPlainObject
};

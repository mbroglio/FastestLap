/**
 * SignalR Session Stream Loader & Telemetry Analyzer
 */

const fs = require('fs');
const path = require('path');
const TelemetryDecoder = require('./decoder');
const { DEFAULT_DRIVERS } = require('./config');

class SessionLoader {
  constructor(streamFilePath = null) {
    this.filePath = streamFilePath || path.join(__dirname, '..', 'data', 'sepang_race_stream.json');
    this.rawEvents = [];
    this.drivers = { ...DEFAULT_DRIVERS };
    this.sessionInfo = null;
    this.isLoaded = false;
  }

  load() {
    if (!fs.existsSync(this.filePath)) {
      throw new Error(`Session stream file not found at ${this.filePath}`);
    }

    const raw = fs.readFileSync(this.filePath, 'utf-8');
    this.rawEvents = JSON.parse(raw);

    for (const evt of this.rawEvents) {
      if (evt.topic === 'DriverList' && evt.data) {
        this.drivers = { ...this.drivers, ...evt.data };
      } else if (evt.topic === 'SessionData' && evt.data) {
        this.sessionInfo = evt.data;
      }
    }

    this.isLoaded = true;
    return this;
  }

  getDrivers() {
    if (!this.isLoaded) this.load();
    return this.drivers;
  }

  findDriver(query) {
    if (!this.isLoaded) this.load();
    const q = String(query).trim().toLowerCase();

    for (const [key, drv] of Object.entries(this.drivers)) {
      if (
        key === q ||
        String(drv.number) === q ||
        (drv.code && drv.code.toLowerCase() === q) ||
        (drv.lastName && drv.lastName.toLowerCase() === q) ||
        (drv.firstName && drv.firstName.toLowerCase() === q)
      ) {
        return { driverNumber: key, ...drv };
      }
    }
    return null;
  }

  /**
   * Retrieves and decodes all telemetry records for a driver within a time range
   * @param {string|number} driverIdentifier - e.g. "16", "LEC", "Leclerc"
   * @param {Object} options - { startTime, endTime }
   * @returns {Object} { driver, points, stats, sessionInfo }
   */
  getDriverTelemetry(driverIdentifier, options = {}) {
    if (!this.isLoaded) this.load();

    const driver = this.findDriver(driverIdentifier);
    if (!driver) {
      throw new Error(`Driver "${driverIdentifier}" not found in session.`);
    }

    const targetNumber = String(driver.driverNumber);
    const startTime = options.startTime ? new Date(options.startTime).getTime() : 0;
    const endTime = options.endTime ? new Date(options.endTime).getTime() : Infinity;

    const points = [];

    for (const evt of this.rawEvents) {
      if (evt.topic !== 'CarData.z' || !evt.data) continue;

      const evtTime = new Date(evt.utc).getTime();
      if (evtTime < startTime || evtTime > endTime) continue;

      try {
        const decodedEntries = TelemetryDecoder.decodeCarData(evt.data);
        const drvPoints = TelemetryDecoder.extractDriverTelemetry(decodedEntries, targetNumber);
        for (const p of drvPoints) {
          const ptTime = new Date(p.utc).getTime();
          if (ptTime >= startTime && ptTime <= endTime) {
            points.push(p);
          }
        }
      } catch (err) {
        console.warn(`Warning: failed to decode packet at ${evt.utc}: ${err.message}`);
      }
    }

    // Sort by timestamp
    points.sort((a, b) => new Date(a.utc).getTime() - new Date(b.utc).getTime());

    // Compute telemetry statistics
    const stats = this._computeStatistics(points);

    return {
      driver,
      sessionInfo: this.sessionInfo,
      totalPoints: points.length,
      stats,
      points
    };
  }

  _computeStatistics(points) {
    if (points.length === 0) {
      return {
        sampleCount: 0,
        maxSpeed: 0,
        avgSpeed: 0,
        maxRpm: 0,
        avgRpm: 0,
        throttleFullPct: 0,
        brakeEventsCount: 0,
        drsActivationCount: 0,
        gearDistribution: {}
      };
    }

    let maxSpeed = 0;
    let totalSpeed = 0;
    let maxRpm = 0;
    let totalRpm = 0;
    let fullThrottleCount = 0;
    let brakeEvents = 0;
    let drsActivations = 0;
    let prevBrake = 0;
    let prevDrs = false;
    const gearDist = {};

    for (const pt of points) {
      if (pt.speed !== null) {
        if (pt.speed > maxSpeed) maxSpeed = pt.speed;
        totalSpeed += pt.speed;
      }
      if (pt.rpm !== null) {
        if (pt.rpm > maxRpm) maxRpm = pt.rpm;
        totalRpm += pt.rpm;
      }
      if (pt.throttle >= 98) {
        fullThrottleCount++;
      }
      if (pt.brake === 1 && prevBrake === 0) {
        brakeEvents++;
      }
      prevBrake = pt.brake || 0;

      if (pt.drsActive && !prevDrs) {
        drsActivations++;
      }
      prevDrs = pt.drsActive;

      if (pt.gear !== null) {
        gearDist[pt.gear] = (gearDist[pt.gear] || 0) + 1;
      }
    }

    // Convert gear distribution to percentages
    const gearPct = {};
    for (const [gear, count] of Object.entries(gearDist)) {
      gearPct[gear] = ((count / points.length) * 100).toFixed(1) + '%';
    }

    return {
      sampleCount: points.length,
      maxSpeed: Math.round(maxSpeed),
      avgSpeed: Math.round(totalSpeed / points.length),
      maxRpm: Math.round(maxRpm),
      avgRpm: Math.round(totalRpm / points.length),
      throttleFullPct: ((fullThrottleCount / points.length) * 100).toFixed(1) + '%',
      brakeEventsCount: brakeEvents,
      drsActivationCount: drsActivations,
      gearDistribution: gearPct
    };
  }
}

module.exports = SessionLoader;

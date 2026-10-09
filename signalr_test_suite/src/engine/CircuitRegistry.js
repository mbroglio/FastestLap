/**
 * CircuitRegistry.js
 * Central Registry & Factory for Circuit Geometries and Session Configurations
 */

const fs = (typeof require !== 'undefined' && typeof window === 'undefined') ? require('fs') : null;
const path = (typeof require !== 'undefined' && typeof window === 'undefined') ? require('path') : null;

const TrackModel = (typeof require !== 'undefined' && typeof window === 'undefined') ? require('./TrackModel') : (window.TrackModel || class {});
const TrackMapAnalyzer = (typeof require !== 'undefined' && typeof window === 'undefined') ? require('./TrackMapAnalyzer') : (window.TrackMapAnalyzer || class {});
const LiveTimingEngine = (typeof require !== 'undefined' && typeof window === 'undefined') ? require('./LiveTimingEngine') : (window.LiveTimingEngine || class {});

class CircuitRegistry {
  static _circuits = {};

  /**
   * Registers a circuit definition
   */
  static register(circuitId, spec) {
    this._circuits[circuitId.toLowerCase()] = spec;
  }

  /**
   * Retrieves a registered circuit specification
   */
  static getSpec(circuitId) {
    const cid = circuitId.toLowerCase();
    if (!this._circuits[cid]) {
      try {
        if (typeof require !== 'undefined' && path) {
          const simPath = path.join(__dirname, '..', '..', 'simulations', 'simulation_circuits.js');
          const altSimPath = path.join(__dirname, '..', 'simulations', 'simulation_circuits.js');
          const target = (fs && fs.existsSync(simPath)) ? simPath : ((fs && fs.existsSync(altSimPath)) ? altSimPath : null);
          if (target) {
            const { registerSimulationCircuits } = require(target);
            if (typeof registerSimulationCircuits === 'function') {
              registerSimulationCircuits(CircuitRegistry);
            }
          }
        } else if (typeof window !== 'undefined' && typeof window.registerSimulationCircuits === 'function') {
          window.registerSimulationCircuits(CircuitRegistry);
        }
      } catch (ignored) {}
    }
    return this._circuits[cid] || null;
  }

  /**
   * Lists all available circuit IDs and metadata
   */
  static list() {
    return Object.entries(this._circuits).map(([id, spec]) => ({
      id,
      name: spec.name,
      country: spec.country,
      flag: spec.flag,
      imageFile: spec.imageFile,
      totalLaps: spec.totalLaps,
      lapDuration: spec.lapDuration
    }));
  }

  /**
   * Builds and returns initialized { trackModel, liveTimingEngine } for a given circuit
   */
  static loadCircuit(circuitId, customDataPath = null) {
    const spec = this.getSpec(circuitId);
    if (!spec) {
      throw new Error(`Circuit "${circuitId}" not found in CircuitRegistry.`);
    }

    let trackNodes = spec.trackNodes || [];
    let pitNodes = spec.pitNodes || [];
    let drivers = spec.drivers || {};
    let laps = spec.laps || {};
    let pits = spec.pits || [];
    let raceControl = spec.raceControl || [];
    let retirements = spec.retirements || {};
    let keyframes = spec.keyframes || {};
    let incidents = spec.incidents || [];

    // In Node.js environment, load from files if available
    if (fs && path && spec.dataFiles) {
      const baseDir = customDataPath || ((fs && fs.existsSync(path.join(__dirname, '..', '..', 'simulations', 'data')))
        ? path.join(__dirname, '..', '..', 'simulations', 'data')
        : path.join(__dirname, '..', '..', 'data'));
      if (spec.dataFiles.track && fs.existsSync(path.join(baseDir, spec.dataFiles.track))) {
        trackNodes = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.track), 'utf-8'));
      }
      if (spec.dataFiles.pit && fs.existsSync(path.join(baseDir, spec.dataFiles.pit))) {
        pitNodes = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.pit), 'utf-8'));
      }
      if (spec.dataFiles.drivers && fs.existsSync(path.join(baseDir, spec.dataFiles.drivers))) {
        drivers = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.drivers), 'utf-8'));
      }
      if (spec.dataFiles.laps && fs.existsSync(path.join(baseDir, spec.dataFiles.laps))) {
        laps = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.laps), 'utf-8'));
      }
      if (spec.dataFiles.pits && fs.existsSync(path.join(baseDir, spec.dataFiles.pits))) {
        pits = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.pits), 'utf-8'));
      }
      if (spec.dataFiles.raceControl && fs.existsSync(path.join(baseDir, spec.dataFiles.raceControl))) {
        raceControl = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.raceControl), 'utf-8'));
      }
      if (spec.dataFiles.retirements && fs.existsSync(path.join(baseDir, spec.dataFiles.retirements))) {
        retirements = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.retirements), 'utf-8'));
      }
      if (spec.dataFiles.keyframes && fs.existsSync(path.join(baseDir, spec.dataFiles.keyframes))) {
        keyframes = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.keyframes), 'utf-8'));
      }
      if (spec.dataFiles.incidents && fs.existsSync(path.join(baseDir, spec.dataFiles.incidents))) {
        incidents = JSON.parse(fs.readFileSync(path.join(baseDir, spec.dataFiles.incidents), 'utf-8'));
      }
    }

    // Configure sectors and pit lane
    const sectorConfig = TrackMapAnalyzer.configureSectors(
      trackNodes,
      spec.sectorsConfig.s1Time,
      spec.sectorsConfig.s2Time
    );

    const pitLaneConfig = TrackMapAnalyzer.configurePitLane(
      pitNodes,
      trackNodes,
      { pitSpeedLimit: spec.pitSpeedLimit || 80, mergeDurationSec: 2.0 }
    );

    const trackModel = new TrackModel({
      circuitId,
      circuitName: spec.name,
      imageFile: spec.imageFile,
      dimensions: spec.dimensions,
      lapDuration: spec.lapDuration,
      trackNodes,
      pitLane: pitLaneConfig,
      sectors: sectorConfig,
      turns: spec.turns || []
    });

    const timingEngine = new LiveTimingEngine(trackModel);
    timingEngine.setDrivers(drivers);
    if (spec.gridOrder) timingEngine.setGridOrder(spec.gridOrder);
    timingEngine.configureSession({
      totalLaps: spec.totalLaps,
      formationLapsCount: spec.formationLapsCount,
      raceStartTimeSec: spec.raceStartTimeSec,
      raceFinishTimeSec: spec.raceFinishTimeSec,
      isStandingStart: spec.isStandingStart
    });
    timingEngine.loadLaps(laps);
    timingEngine.loadPitStops(pits);
    timingEngine.loadRaceControlMessages(raceControl);
    timingEngine.loadRetirements(retirements);
    timingEngine.loadIncidents(incidents);
    timingEngine.loadKeyframes(keyframes);

    return {
      spec,
      trackModel,
      timingEngine
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = CircuitRegistry;
}

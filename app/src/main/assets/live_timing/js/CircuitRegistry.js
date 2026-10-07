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
    return this._circuits[circuitId.toLowerCase()] || null;
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
      const baseDir = customDataPath || path.join(__dirname, '..', '..', 'data');
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

// Register Baku City Circuit
CircuitRegistry.register('baku', {
  name: 'Baku City Circuit',
  country: 'Azerbaijan',
  flag: '🇦🇿',
  imageFile: 'simulations/baku/Baku_Formula_One_circuit_map.svg.webp',
  dimensions: { width: 500, height: 371 },
  lapDuration: 108.6,
  totalLaps: 51,
  formationLapsCount: 0,
  raceStartTimeSec: 0.0,
  raceFinishTimeSec: 5881.3,
  isStandingStart: true,
  pitSpeedLimit: 80,
  sectorsConfig: { s1Time: 38.85, s2Time: 83.5 },
  dataFiles: {
    track: 'baku_exact_track_full.json',
    pit: 'baku_pit_lane_nodes.json',
    drivers: 'baku_drivers.json',
    laps: 'baku_driver_laps.json',
    pits: 'baku_pit_stops.json',
    raceControl: 'baku_race_control_messages.json',
    retirements: 'baku_retirements.json',
    keyframes: 'baku_keyframes.json',
    incidents: 'baku_incidents.json'
  },
  gridOrder: ['63', '16', '81', '6', '1', '3', '44', '10', '55', '43', '87', '30', '23', '31', '41', '12', '5', '27', '14', '11', '77', '18']
});

// Register Sepang International Circuit
CircuitRegistry.register('sepang', {
  name: 'Sepang International Circuit',
  country: 'Malaysia',
  flag: '🇲🇾',
  imageFile: 'simulations/sepang/Sepang.svg.webp',
  dimensions: { width: 1280, height: 1057 },
  lapDuration: 95.0,
  totalLaps: 55,
  formationLapsCount: 2, // Wet anomalous start
  raceStartTimeSec: 216.9, // 2 wet formation laps before standing start
  raceFinishTimeSec: 5742.0,
  isStandingStart: true,
  pitSpeedLimit: 80,
  sectorsConfig: { s1Time: 25.5, s2Time: 59.8 },
  dataFiles: {
    track: 'sepang_exact_track_full.json',
    pit: 'sepang_pit_lane_nodes.json',
    drivers: 'baku_drivers.json', // 2026 drivers
    laps: 'baku_driver_laps.json',
    pits: 'formatted_pit_stops.json',
    raceControl: 'baku_race_control_messages.json',
    retirements: 'baku_retirements.json',
    keyframes: 'test_keyframes.json'
  },
  gridOrder: ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41']
});

if (typeof module !== 'undefined' && module.exports) {
  module.exports = CircuitRegistry;
}

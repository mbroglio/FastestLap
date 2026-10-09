/**
 * FastestLap — simulation_circuits.js
 * Registrazione circuiti specifici e configurazioni per ambiente di test / simulazioni
 */

function registerSimulationCircuits(CircuitRegistry) {
  if (!CircuitRegistry || typeof CircuitRegistry.register !== 'function') return;

  // Register Baku City Circuit Simulation
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

  // Register Sepang International Circuit Simulation
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
      drivers: 'baku_drivers.json',
      laps: 'baku_driver_laps.json',
      pits: 'formatted_pit_stops.json',
      raceControl: 'baku_race_control_messages.json',
      retirements: 'baku_retirements.json',
      keyframes: 'test_keyframes.json'
    },
    gridOrder: ['3', '44', '12', '16', '1', '81', '63', '6', '10', '5', '30', '14', '55', '18', '27', '87', '31', '23', '77', '11', '43', '41']
  });
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { registerSimulationCircuits };
}

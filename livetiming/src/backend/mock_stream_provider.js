/**
 * FastestLap LiveTiming - Mock Stream Provider
 *
 * Generates high-fidelity realistic telemetry and live timing stream deltas
 * for testing dashboards and APIs when no official F1 session is live.
 */

const DRIVER_DB = {
  '1': { RacingNumber: '1', Tla: 'VER', BroadcastName: 'M VERSTAPPEN', FullName: 'Max Verstappen', TeamName: 'Red Bull Racing', TeamColour: '#3671C6' },
  '16': { RacingNumber: '16', Tla: 'LEC', BroadcastName: 'C LECLERC', FullName: 'Charles Leclerc', TeamName: 'Ferrari', TeamColour: '#E8002D' },
  '4': { RacingNumber: '4', Tla: 'NOR', BroadcastName: 'L NORRIS', FullName: 'Lando Norris', TeamName: 'McLaren', TeamColour: '#FF8000' },
  '81': { RacingNumber: '81', Tla: 'PIA', BroadcastName: 'O PIASTRI', FullName: 'Oscar Piastri', TeamName: 'McLaren', TeamColour: '#FF8000' },
  '44': { RacingNumber: '44', Tla: 'HAM', BroadcastName: 'L HAMILTON', FullName: 'Lewis Hamilton', TeamName: 'Mercedes', TeamColour: '#27F4D2' },
  '63': { RacingNumber: '63', Tla: 'RUS', BroadcastName: 'G RUSSELL', FullName: 'George Russell', TeamName: 'Mercedes', TeamColour: '#27F4D2' },
  '55': { RacingNumber: '55', Tla: 'SAI', BroadcastName: 'C SAINZ', FullName: 'Carlos Sainz', TeamName: 'Williams', TeamColour: '#64C4FF' },
  '14': { RacingNumber: '14', Tla: 'ALO', BroadcastName: 'F ALONSO', FullName: 'Fernando Alonso', TeamName: 'Aston Martin', TeamColour: '#229971' },
  '18': { RacingNumber: '18', Tla: 'STR', BroadcastName: 'L STROLL', FullName: 'Lance Stroll', TeamName: 'Aston Martin', TeamColour: '#229971' },
  '10': { RacingNumber: '10', Tla: 'GAS', BroadcastName: 'P GASLY', FullName: 'Pierre Gasly', TeamName: 'Alpine', TeamColour: '#0093CC' }
};

// Parametric Monza / Generic Circuit Path (normalized 0..1 loop)
function getCircuitCoordinates(progress) {
  const theta = progress * 2 * Math.PI;
  // Elliptical track with chicane kinks
  const x = Math.sin(theta) * 400 + Math.sin(theta * 3) * 60;
  const y = Math.cos(theta) * 220 + Math.cos(theta * 2) * 50;
  return { x: Math.round(x * 10) / 10, y: Math.round(y * 10) / 10 };
}

class MockStreamProvider {
  constructor(stateStore) {
    this.stateStore = stateStore;
    this.timer = null;
    this.lap = 14;
    this.totalLaps = 53;
    this.progress = {};
    this.listeners = new Set();

    // Initialize progress offsets for drivers
    let offset = 0;
    for (const num of Object.keys(DRIVER_DB)) {
      this.progress[num] = (1.0 - offset) % 1.0;
      offset += 0.035;
    }
  }

  onUpdate(callback) {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);
  }

  notifyUpdate(type, data) {
    for (const cb of this.listeners) {
      try {
        cb(type, data);
      } catch (e) {}
    }
  }

  start() {
    this.stateStore.setConnectionStatus('SIMULATION');

    // Seed Driver List
    this.stateStore.updateDrivers(DRIVER_DB);

    // Seed Session Info
    this.stateStore.updateSessionInfo({
      Meeting: { Name: 'Italian Grand Prix', Circuit: { ShortName: 'Monza' } },
      Name: 'Race',
      Type: 'Race',
      Status: 'Started',
      GmtOffset: '+02:00'
    });

    // Seed Weather
    this.stateStore.updateWeather({
      AirTemp: '27.4',
      TrackTemp: '41.8',
      Humidity: '46',
      Pressure: '1013.8',
      WindSpeed: '2.1',
      Rainfall: '0'
    });

    // Seed Tyre Stints
    const appLines = {};
    const compounds = ['SOFT', 'MEDIUM', 'HARD'];
    let idx = 0;
    for (const num of Object.keys(DRIVER_DB)) {
      appLines[num] = {
        GridPos: String(idx + 1),
        Stints: [
          { Compound: compounds[idx % 2], TotalLaps: 14 + (idx % 3), New: true }
        ]
      };
      idx++;
    }
    this.stateStore.updateTimingAppData({ Lines: appLines });

    // Seed Initial Race Control Message
    this.stateStore.addRaceControlMessage([
      { Utc: new Date().toISOString(), Lap: this.lap, Category: 'Flag', Flag: 'GREEN', Scope: 'Track', Message: 'TRACK CLEAR - GREEN FLAG' },
      { Utc: new Date().toISOString(), Lap: this.lap, Category: 'DRS', Message: 'DRS ENABLED' }
    ]);

    // Perform initial tick immediately
    this._tick();

    // Tick every 500ms (2Hz high-refresh)
    this.timer = setInterval(() => this._tick(), 500);
    console.log('[MockStream] Simulation engine active. Streaming at 2Hz.');
  }

  _tick() {
    const driverKeys = Object.keys(DRIVER_DB);
    const timingLines = {};
    const telemetryBatch = {};
    const positionBatch = {};

    driverKeys.forEach((num, index) => {
      // Advance progress
      this.progress[num] = (this.progress[num] + 0.008) % 1.0;
      const prog = this.progress[num];

      // Physical track coordinates
      const coords = getCircuitCoordinates(prog);
      positionBatch[num] = {
        status: 'OnTrack',
        x: coords.x,
        y: coords.y,
        z: 0
      };
      this.stateStore.updateDriverPosition(num, positionBatch[num]);

      // Telemetry dynamics based on track sector
      const isStraight = (prog >= 0.1 && prog <= 0.35) || (prog >= 0.6 && prog <= 0.85);
      const isCorner = !isStraight;
      const isDrsZone = prog >= 0.12 && prog <= 0.32;

      let speed, rpm, gear, throttle, brake, drsRaw;
      if (isStraight) {
        speed = Math.floor(280 + Math.sin(prog * 20) * 45);
        gear = 8;
        rpm = Math.floor(11500 + Math.sin(prog * 20) * 800);
        throttle = 100;
        brake = 0;
        drsRaw = isDrsZone ? 8 : 1;
      } else {
        speed = Math.floor(110 + Math.cos(prog * 20) * 35);
        gear = speed > 130 ? 4 : (speed > 90 ? 3 : 2);
        rpm = Math.floor(9200 + Math.cos(prog * 20) * 600);
        throttle = Math.floor(35 + Math.sin(prog * 10) * 25);
        brake = throttle < 45 ? 1 : 0;
        drsRaw = 0;
      }

      // Battery level
      const battery = Math.min(100, Math.max(30, Math.floor(75 + (brake ? 12 : -8) + Math.sin(prog * 5) * 10)));

      const carTel = {
        speed,
        rpm,
        gear,
        throttle,
        brake,
        brakeRaw: brake ? 100 : 0,
        drs: drsRaw,
        drsState: drsRaw === 8 ? 'OPEN' : (drsRaw === 1 ? 'AVAILABLE' : 'OFF'),
        drsActive: drsRaw === 8,
        battery,
        ersMode: drsRaw === 8 ? 'OVERTAKE' : (throttle > 90 ? 'HOTLAP' : 'BALANCED')
      };

      telemetryBatch[num] = carTel;
      this.stateStore.updateCarTelemetry(num, carTel);

      // Timing tower deltas
      const gapSec = (index * 1.34).toFixed(3);
      timingLines[num] = {
        Position: String(index + 1),
        GapToLeader: index === 0 ? { Value: 'LEADER' } : { Value: `+${gapSec}` },
        IntervalToPositionAhead: index === 0 ? { Value: 'LEADER' } : { Value: '+1.340' },
        BestLapTime: { Value: `1:21.${(450 + index * 120)}` },
        LastLapTime: { Value: `1:22.${(100 + index * 95)}` },
        Sectors: {
          '0': { Value: '27.4', OverallFastest: index === 0, PersonalFastest: true },
          '1': { Value: '26.8', OverallFastest: index === 1, PersonalFastest: true },
          '2': { Value: '27.9', OverallFastest: false, PersonalFastest: false }
        },
        Speeds: {
          FL: { Value: String(speed) },
          ST: { Value: '342' }
        },
        InPit: false,
        NumberOfPitStops: 1,
        Retired: false
      };
    });

    this.stateStore.updateTimingData({ Lines: timingLines });
    this.stateStore.updateLapCount({ CurrentLap: this.lap, TotalLaps: this.totalLaps });

    this.notifyUpdate('TICK', {
      positions: positionBatch,
      telemetry: telemetryBatch,
      leaderboard: this.stateStore.getLeaderboard()
    });
  }

  stop() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.stateStore.setConnectionStatus('STOPPED');
  }
}

module.exports = {
  MockStreamProvider
};

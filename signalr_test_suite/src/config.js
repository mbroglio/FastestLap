/**
 * SignalR & FastF1 Configuration Constants
 * Official 2026 Formula One World Championship 22-Driver Grid (Wikipedia 2026)
 * 11 Constructors x 2 Drivers
 */

module.exports = {
  // SignalR Core endpoints used by Formula 1 official live timing
  SIGNALR_NEGOTIATE_URL: 'https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1',
  SIGNALR_WS_BASE_URL: 'wss://livetiming.formula1.com/signalrcore',
  
  // Headers required by CloudFront / Kestrel server
  DEFAULT_HEADERS: {
    'User-Agent': 'BestHTTP',
    'Accept': '*/*'
  },

  // Topics streamed by F1 Live Timing
  TOPICS: [
    'Heartbeat',
    'CarData.z',
    'Position.z',
    'TimingData',
    'DriverList',
    'SessionData',
    'SessionStatus',
    'TrackStatus',
    'RaceControlMessages'
  ],

  // CarData channel IDs mapping (as used by FastF1 & F1 Live Timing stream)
  CHANNELS: {
    RPM: '0',        // Engine RPM
    SPEED: '2',      // Speed in km/h
    GEAR: '3',       // Current engaged gear (0-8)
    THROTTLE: '4',   // Throttle percentage (0-100)
    BRAKE: '5',      // Brake activation (0 or 1, or 0-100)
    DRS: '45'        // DRS status (0: Off, 1: Available, 8: Deployed/Open, etc.)
  },

  // Official 2026 Formula 1 Grid from Wikipedia (11 Teams, 22 Drivers)
  // - Lando Norris (#1) as reigning 2025 World Drivers' Champion
  // - Max Verstappen (#3) switched to #3
  // - Audi Revolut F1 Team enters as works team (acquiring Sauber) with Bortoleto (#5) and Hülkenberg (#27)
  // - Cadillac Formula 1 Team enters with Pérez (#11) and Bottas (#77)
  // - Red Bull Racing with Verstappen (#3) and Hadjar (#6)
  // - Racing Bulls with Lawson (#30) and Lindblad (#41)
  // - Alpine with Gasly (#10) and Colapinto (#43)
  // - Ferrari with Leclerc (#16) and Hamilton (#44)
  // - Mercedes with Antonelli (#12) and Russell (#63)
  // - McLaren with Norris (#1) and Piastri (#81)
  // - Aston Martin with Alonso (#14) and Stroll (#18)
  // - Williams with Albon (#23) and Sainz Jr. (#55)
  // - Haas with Ocon (#31) and Bearman (#87)
  DEFAULT_DRIVERS: {
    '1':  { code: 'NOR', number: 1,  firstName: 'Lando',     lastName: 'Norris',       team: 'McLaren Mastercard F1 Team',      color: '#FF8000', offsetSec: 0.0,  speedDelta: +1.0, rpmDelta: +70, throttleAggression: 1.01 },
    '3':  { code: 'VER', number: 3,  firstName: 'Max',       lastName: 'Verstappen',   team: 'Oracle Red Bull Racing',          color: '#3671C6', offsetSec: 1.2,  speedDelta: +0.9, rpmDelta: +70, throttleAggression: 1.01 },
    '16': { code: 'LEC', number: 16, firstName: 'Charles',   lastName: 'Leclerc',      team: 'Scuderia Ferrari HP',             color: '#E80020', offsetSec: 2.6,  speedDelta: +0.7, rpmDelta: +50, throttleAggression: 1.00 },
    '81': { code: 'PIA', number: 81, firstName: 'Oscar',     lastName: 'Piastri',      team: 'McLaren Mastercard F1 Team',      color: '#FF8000', offsetSec: 4.1,  speedDelta: +0.6, rpmDelta: +40, throttleAggression: 1.00 },
    '63': { code: 'RUS', number: 63, firstName: 'George',    lastName: 'Russell',      team: 'Mercedes-AMG Petronas F1 Team',   color: '#27F4D2', offsetSec: 5.9,  speedDelta: +0.5, rpmDelta: +30, throttleAggression: 1.00 },
    '12': { code: 'ANT', number: 12, firstName: 'Kimi',      lastName: 'Antonelli',    team: 'Mercedes-AMG Petronas F1 Team',   color: '#27F4D2', offsetSec: 7.8,  speedDelta: +0.4, rpmDelta: +20, throttleAggression: 0.99 },
    '44': { code: 'HAM', number: 44, firstName: 'Lewis',     lastName: 'Hamilton',     team: 'Scuderia Ferrari HP',             color: '#E80020', offsetSec: 10.0, speedDelta: +0.3, rpmDelta: +20, throttleAggression: 0.99 },
    '6':  { code: 'HAD', number: 6,  firstName: 'Isack',     lastName: 'Hadjar',       team: 'Oracle Red Bull Racing',          color: '#3671C6', offsetSec: 12.5, speedDelta: +0.2, rpmDelta: +10, throttleAggression: 0.99 },
    '55': { code: 'SAI', number: 55, firstName: 'Carlos',    lastName: 'Sainz Jr.',    team: 'Atlassian Williams F1 Team',      color: '#64C4FF', offsetSec: 15.2, speedDelta: +0.1, rpmDelta:   0, throttleAggression: 0.99 },
    '14': { code: 'ALO', number: 14, firstName: 'Fernando',  lastName: 'Alonso',       team: 'Aston Martin Aramco F1 Team',     color: '#229971', offsetSec: 18.1, speedDelta:  0.0, rpmDelta:   0, throttleAggression: 0.99 },
    '23': { code: 'ALB', number: 23, firstName: 'Alexander', lastName: 'Albon',        team: 'Atlassian Williams F1 Team',      color: '#64C4FF', offsetSec: 21.3, speedDelta: -0.1, rpmDelta: -10, throttleAggression: 0.98 },
    '18': { code: 'STR', number: 18, firstName: 'Lance',     lastName: 'Stroll',       team: 'Aston Martin Aramco F1 Team',     color: '#229971', offsetSec: 24.8, speedDelta: -0.2, rpmDelta: -20, throttleAggression: 0.98 },
    '10': { code: 'GAS', number: 10, firstName: 'Pierre',    lastName: 'Gasly',        team: 'BWT Alpine F1 Team',              color: '#FF87BC', offsetSec: 28.5, speedDelta: -0.3, rpmDelta: -20, throttleAggression: 0.98 },
    '43': { code: 'COL', number: 43, firstName: 'Franco',    lastName: 'Colapinto',    team: 'BWT Alpine F1 Team',              color: '#FF87BC', offsetSec: 32.4, speedDelta: -0.3, rpmDelta: -30, throttleAggression: 0.98 },
    '30': { code: 'LAW', number: 30, firstName: 'Liam',      lastName: 'Lawson',       team: 'Visa Cash App Racing Bulls',      color: '#6692FF', offsetSec: 36.6, speedDelta: -0.4, rpmDelta: -30, throttleAggression: 0.98 },
    '41': { code: 'LIN', number: 41, firstName: 'Arvid',     lastName: 'Lindblad',     team: 'Visa Cash App Racing Bulls',      color: '#6692FF', offsetSec: 41.0, speedDelta: -0.5, rpmDelta: -40, throttleAggression: 0.97 },
    '27': { code: 'HUL', number: 27, firstName: 'Nico',      lastName: 'Hülkenberg',   team: 'Audi Revolut F1 Team',            color: '#F50537', offsetSec: 45.7, speedDelta: -0.5, rpmDelta: -40, throttleAggression: 0.97 },
    '5':  { code: 'BOR', number: 5,  firstName: 'Gabriel',   lastName: 'Bortoleto',    team: 'Audi Revolut F1 Team',            color: '#F50537', offsetSec: 50.6, speedDelta: -0.6, rpmDelta: -50, throttleAggression: 0.97 },
    '31': { code: 'OCO', number: 31, firstName: 'Esteban',   lastName: 'Ocon',         team: 'TGR Haas F1 Team',                color: '#B6BABD', offsetSec: 55.8, speedDelta: -0.6, rpmDelta: -50, throttleAggression: 0.96 },
    '87': { code: 'BEA', number: 87, firstName: 'Oliver',    lastName: 'Bearman',      team: 'TGR Haas F1 Team',                color: '#B6BABD', offsetSec: 61.2, speedDelta: -0.7, rpmDelta: -60, throttleAggression: 0.96 },
    '11': { code: 'PER', number: 11, firstName: 'Sergio',    lastName: 'Pérez',        team: 'Cadillac Formula 1 Team',         color: '#DFB14E', offsetSec: 67.0, speedDelta: -0.8, rpmDelta: -70, throttleAggression: 0.96 },
    '77': { code: 'BOT', number: 77, firstName: 'Valtteri',  lastName: 'Bottas',       team: 'Cadillac Formula 1 Team',         color: '#DFB14E', offsetSec: 73.2, speedDelta: -0.9, rpmDelta: -80, throttleAggression: 0.95 }
  },

  // Record Separator character used in SignalR Core protocol
  RECORD_SEPARATOR: String.fromCharCode(0x1e)
};

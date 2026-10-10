/**
 * Unit Test: TelemetryDecoder (CarData.z & Position.z)
 */

const assert = require('assert');
const { TelemetryDecoder, CHANNELS } = require('../src/backend/telemetry_decoder');

console.log('--- [TEST 2] TelemetryDecoder (CarData.z & Position.z) ---');

// 1. Test CarData round-trip encoding and decoding
const rawCarPayload = {
  Entries: [
    {
      Utc: '2026-10-10T14:00:00Z',
      Cars: {
        '16': {
          Channels: {
            [CHANNELS.RPM]: 11850,
            [CHANNELS.SPEED]: 294,
            [CHANNELS.GEAR]: 7,
            [CHANNELS.THROTTLE]: 98,
            [CHANNELS.BRAKE]: 0,
            [CHANNELS.DRS]: 8
          }
        }
      }
    }
  ]
};

const encodedCar = TelemetryDecoder.encodeCarData(rawCarPayload);
assert(typeof encodedCar === 'string' && encodedCar.length > 0, 'CarData encoding failed');

const decodedCar = TelemetryDecoder.decodeCarData(encodedCar);
assert.strictEqual(decodedCar.length, 1, 'One entry decoded');
const car16 = decodedCar[0].cars['16'];

assert.strictEqual(car16.rpm, 11850, 'RPM decoded correctly');
assert.strictEqual(car16.speed, 294, 'Speed decoded correctly');
assert.strictEqual(car16.gear, 7, 'Gear decoded correctly');
assert.strictEqual(car16.throttle, 98, 'Throttle decoded correctly');
assert.strictEqual(car16.brake, 0, 'Brake decoded correctly');
assert.strictEqual(car16.drsState, 'OPEN', 'DRS open recognized');
assert.strictEqual(car16.drsActive, true, 'DRS active flag set');
assert(car16.battery >= 0 && car16.battery <= 100, 'Battery charge valid');
console.log('✅ CarData.z DEFLATE decompression and channel parsing verified');

// 2. Test Position.z round-trip encoding and decoding
const rawPosPayload = {
  Position: [
    {
      Timestamp: '2026-10-10T14:00:01Z',
      Entries: {
        '16': {
          X: 12500, // 1250.0 meters in decimeters
          Y: -4500, // -450.0 meters in decimeters
          Z: 150,
          Status: 'OnTrack'
        }
      }
    }
  ]
};

const encodedPos = TelemetryDecoder.encodePosition(rawPosPayload);
const decodedPos = TelemetryDecoder.decodePosition(encodedPos);

assert.strictEqual(decodedPos.length, 1, 'One position entry decoded');
const pos16 = decodedPos[0].entries['16'];

assert.strictEqual(pos16.x, 1250, 'Decimeters converted to meters for X');
assert.strictEqual(pos16.y, -450, 'Decimeters converted to meters for Y');
assert.strictEqual(pos16.status, 'OnTrack', 'Status preserved');
console.log('✅ Position.z DEFLATE decompression and metric coordinate scaling verified');

console.log('🎉 TelemetryDecoder tests passed successfully!\n');

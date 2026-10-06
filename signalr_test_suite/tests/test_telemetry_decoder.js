/**
 * Test Suite: FastF1 Telemetry Decoder & Sepang Session Verification
 * Validates DEFLATE decompression, CarData channel mapping,
 * driver filtering, and telemetry statistics extraction.
 */

const TelemetryDecoder = require('../src/decoder');
const SessionLoader = require('../src/session_loader');

function testTelemetryDecoder() {
  console.log('\n--- [TEST 4] Telemetry Decoder & Sepang Stream Integrity ---');

  // Subtest A: Encoding & Decoding Round-Trip
  console.log('1. Testing raw DEFLATE round-trip for CarData.z format...');
  const sampleData = {
    Entries: [
      {
        Utc: '2026-10-04T10:30:15.500Z',
        Cars: {
          '16': {
            Channels: {
              '0': 12250, // RPM
              '2': 318,   // Speed
              '3': 8,     // Gear
              '4': 100,   // Throttle
              '5': 0,     // Brake
              '45': 1     // DRS
            }
          }
        }
      }
    ]
  };

  const encodedB64 = TelemetryDecoder.encodeCarData(sampleData);
  if (!encodedB64 || typeof encodedB64 !== 'string') {
    console.error('❌ Subtest A Failed: Encoding produced empty or invalid result');
    return false;
  }
  console.log(`   Encoded Base64 length: ${encodedB64.length} chars`);

  const decodedEntries = TelemetryDecoder.decodeCarData(encodedB64);
  if (!decodedEntries || decodedEntries.length !== 1) {
    console.error('❌ Subtest A Failed: Decoded entry count mismatch');
    return false;
  }

  const car16 = decodedEntries[0].cars['16'];
  if (!car16 || car16.rpm !== 12250 || car16.speed !== 318 || car16.gear !== 8 || car16.throttle !== 100 || car16.brake !== 0 || !car16.drsActive) {
    console.error('❌ Subtest A Failed: Decoded channel values mismatch', car16);
    return false;
  }
  console.log('   ✅ DEFLATE roundtrip and channel mapping verified successfully!');

  // Subtest B: Error resilience
  console.log('2. Testing decoder error resilience on malformed payloads...');
  try {
    TelemetryDecoder.decodeCarData('NOT_VALID_BASE64_OR_DEFLATE!!!');
    console.error('❌ Subtest B Failed: Malformed payload did not throw error');
    return false;
  } catch {
    console.log('   ✅ Malformed payload handled safely and raised descriptive error');
  }

  // Subtest C: Loading Sepang race stream (4 Ottobre 2026, 09:00:00 - 10:30:40 UTC)
  console.log('3. Loading Sepang race stream and validating driver telemetry...');
  const loader = new SessionLoader();
  loader.load();

  const drivers = loader.getDrivers();
  const driverKeys = Object.keys(drivers);
  console.log(`   Found ${driverKeys.length} registered drivers in session: ${driverKeys.join(', ')}`);

  // Test extraction for a few key drivers (VER #1, LEC #16, HAM #44)
  const testDrivers = ['1', '16', '44'];
  for (const drvId of testDrivers) {
    const tel = loader.getDriverTelemetry(drvId);
    console.log(`   Driver #${drvId} (${tel.driver.code}): ${tel.totalPoints} points, Top Speed: ${tel.stats.maxSpeed} km/h, Max RPM: ${tel.stats.maxRpm}`);
    
    if (tel.totalPoints === 0) {
      console.error(`❌ Subtest C Failed: Driver #${drvId} returned 0 telemetry points`);
      return false;
    }
    if (tel.stats.maxSpeed < 250) {
      console.error(`❌ Subtest C Failed: Driver #${drvId} max speed too low for F1 (${tel.stats.maxSpeed} km/h)`);
      return false;
    }
  }

  console.log('🎉 PASSED: Telemetry decoder and Sepang session stream validated!\n');
  return true;
}

if (require.main === module) {
  const passed = testTelemetryDecoder();
  process.exitCode = passed ? 0 : 1;
}

module.exports = testTelemetryDecoder;

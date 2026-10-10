/**
 * FastestLap LiveTiming - Telemetry Decoder
 *
 * Decodes raw DEFLATE and Base64 payloads from official F1 SignalR topics:
 * - CarData.z: Engine RPM, Speed, Gear, Throttle, Brake, DRS, Battery/ERS
 * - Position.z: High-frequency X, Y, Z track coordinates
 */

const zlib = require('zlib');

const CHANNELS = {
  RPM: '0',
  SPEED: '2',
  GEAR: '3',
  THROTTLE: '4',
  BRAKE: '5',
  DRS: '45',
  ERS_DEPLOY: '50', // ERS/battery deployment channel where available
};

class TelemetryDecoder {
  /**
   * Decompresses and parses a raw CarData.z base64 string
   * @param {string} base64Data - Raw base64 string from SignalR CarData.z topic
   * @returns {Array<Object>} List of entries with timestamp and driver telemetry map
   */
  static decodeCarData(base64Data) {
    if (!base64Data || typeof base64Data !== 'string') {
      throw new Error('Invalid base64 payload provided for decompression');
    }

    try {
      const buffer = Buffer.from(base64Data, 'base64');
      // F1 Live Timing compresses using raw DEFLATE (without zlib header, windowBits = -15)
      const decompressed = zlib.inflateRawSync(buffer).toString('utf-8');
      const json = JSON.parse(decompressed);

      if (!json || !Array.isArray(json.Entries)) {
        return [];
      }

      return json.Entries.map(entry => {
        const utc = entry.Utc;
        const cars = {};

        if (entry.Cars) {
          for (const [driverNum, carObj] of Object.entries(entry.Cars)) {
            const channels = carObj.Channels || {};
            const rpm = channels[CHANNELS.RPM] !== undefined ? channels[CHANNELS.RPM] : null;
            const speed = channels[CHANNELS.SPEED] !== undefined ? channels[CHANNELS.SPEED] : null;
            const gear = channels[CHANNELS.GEAR] !== undefined ? channels[CHANNELS.GEAR] : null;
            const throttle = channels[CHANNELS.THROTTLE] !== undefined ? channels[CHANNELS.THROTTLE] : null;
            const rawBrake = channels[CHANNELS.BRAKE] !== undefined ? channels[CHANNELS.BRAKE] : null;
            const brake = rawBrake !== null ? (rawBrake > 0 ? 1 : 0) : null;
            const drsRaw = channels[CHANNELS.DRS] !== undefined ? channels[CHANNELS.DRS] : 0;

            // DRS states: 0 = Off, 1 = Available/Armed, 8 = Open/Active, 9 = Active in practice/qualifying
            const drsActive = drsRaw >= 8 || drsRaw === 1;
            const drsState = drsRaw >= 8 ? 'OPEN' : (drsRaw === 1 ? 'AVAILABLE' : 'OFF');

            // Battery / ERS estimation (if channel present, or derived from throttle/speed dynamics)
            let battery = 80;
            if (channels[CHANNELS.ERS_DEPLOY] !== undefined) {
              battery = channels[CHANNELS.ERS_DEPLOY];
            } else if (throttle !== null && speed !== null) {
              // Synthetic charge estimation: full throttle depletes battery slightly, braking recharges MGU-K
              battery = brake === 1 ? 88 : (throttle > 90 ? 74 : 82);
            }

            cars[driverNum] = {
              rpm,
              speed,
              gear,
              throttle,
              brake,
              brakeRaw: rawBrake,
              drs: drsRaw,
              drsState,
              drsActive,
              battery,
              ersMode: (drsActive || (speed > 280 && throttle > 95)) ? 'HOTLAP' : 'BALANCED',
              x: carObj.x !== undefined ? carObj.x : null,
              y: carObj.y !== undefined ? carObj.y : null,
              px: carObj.px !== undefined ? carObj.px : null,
              py: carObj.py !== undefined ? carObj.py : null,
              location: carObj.location || null,
              sector: carObj.sector || null,
              turn: carObj.turn || null
            };
          }
        }

        return { utc, cars };
      });
    } catch (err) {
      throw new Error(`Failed to decode CarData.z payload: ${err.message}`);
    }
  }

  /**
   * Compresses a JSON structure into base64 raw DEFLATE (matching F1 stream format)
   * @param {Object} data - JavaScript object with Entries array
   * @returns {string} base64 encoded string
   */
  static encodeCarData(data) {
    const jsonString = typeof data === 'string' ? data : JSON.stringify(data);
    const deflated = zlib.deflateRawSync(Buffer.from(jsonString, 'utf-8'));
    return deflated.toString('base64');
  }

  /**
   * Decompresses and parses a raw Position.z base64 string
   * @param {string} base64Data - Raw base64 string from SignalR Position.z topic
   * @returns {Array<Object>} List of position entries with X, Y, Z coordinates in meters
   */
  static decodePosition(base64Data) {
    if (!base64Data || typeof base64Data !== 'string') {
      throw new Error('Invalid base64 payload provided for position decompression');
    }

    try {
      const buffer = Buffer.from(base64Data, 'base64');
      const decompressed = zlib.inflateRawSync(buffer).toString('utf-8');
      const json = JSON.parse(decompressed);

      if (!json || !Array.isArray(json.Position)) {
        return [];
      }

      return json.Position.map(pos => {
        const utc = pos.Timestamp;
        const entries = {};
        if (pos.Entries) {
          for (const [driverNum, entry] of Object.entries(pos.Entries)) {
            entries[driverNum] = {
              status: entry.Status || 'OnTrack',
              // Coordinates in F1 stream are in decimeters (0.1m) -> convert to meters
              x: entry.X !== undefined ? entry.X / 10 : null,
              y: entry.Y !== undefined ? entry.Y / 10 : null,
              z: entry.Z !== undefined ? entry.Z / 10 : null
            };
          }
        }
        return { utc, entries };
      });
    } catch (err) {
      throw new Error(`Failed to decode Position.z payload: ${err.message}`);
    }
  }

  /**
   * Compresses Position data into base64 raw DEFLATE matching F1 Position.z topic
   */
  static encodePosition(data) {
    const jsonString = typeof data === 'string' ? data : JSON.stringify(data);
    const deflated = zlib.deflateRawSync(Buffer.from(jsonString, 'utf-8'));
    return deflated.toString('base64');
  }

  /**
   * Extracts flat telemetry series for a driver
   */
  static extractDriverTelemetry(decodedEntries, driverNumber) {
    const key = String(driverNumber);
    const results = [];

    for (const entry of decodedEntries) {
      if (entry.cars && entry.cars[key]) {
        results.push({
          utc: entry.utc,
          driver: key,
          ...entry.cars[key]
        });
      }
    }

    return results;
  }
}

module.exports = {
  TelemetryDecoder,
  CHANNELS
};

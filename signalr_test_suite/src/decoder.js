/**
 * FastF1 / SignalR CarData.z Decompressor & Channel Parser
 */

const zlib = require('zlib');
const { CHANNELS } = require('./config');

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
      // F1 Live Timing compresses using raw DEFLATE (without zlib header, equivalent to windowBits = -15)
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
            cars[driverNum] = {
              rpm: channels[CHANNELS.RPM] !== undefined ? channels[CHANNELS.RPM] : null,
              speed: channels[CHANNELS.SPEED] !== undefined ? channels[CHANNELS.SPEED] : null,
              gear: channels[CHANNELS.GEAR] !== undefined ? channels[CHANNELS.GEAR] : null,
              throttle: channels[CHANNELS.THROTTLE] !== undefined ? channels[CHANNELS.THROTTLE] : null,
              brake: channels[CHANNELS.BRAKE] !== undefined ? (channels[CHANNELS.BRAKE] > 0 ? 1 : 0) : null,
              brakeRaw: channels[CHANNELS.BRAKE] !== undefined ? channels[CHANNELS.BRAKE] : null,
              drs: channels[CHANNELS.DRS] !== undefined ? channels[CHANNELS.DRS] : 0,
              drsActive: channels[CHANNELS.DRS] !== undefined ? (channels[CHANNELS.DRS] >= 8 || channels[CHANNELS.DRS] === 1) : false,
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

        return {
          utc,
          cars
        };
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
              x: entry.X !== undefined ? entry.X / 10 : null, // Decimeters to meters
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
   * Filters telemetry for a specific driver number from a decoded entries list
   * @param {Array<Object>} decodedEntries - Output from decodeCarData
   * @param {string|number} driverNumber - e.g. "1", "16", "44"
   * @returns {Array<Object>} Flat array of driver telemetry points
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

module.exports = TelemetryDecoder;

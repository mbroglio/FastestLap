/**
 * FastestLap LiveTiming - Telemetry Decoder (Corretto)
 */

const zlib = require('zlib');

const CHANNELS = {
  RPM: '0',
  SPEED: '2',
  GEAR: '3',
  THROTTLE: '4',
  BRAKE: '5',
  DRS: '45',
  ERS_DEPLOY: '50'
};

class TelemetryDecoder {
  static decompressPayload(base64Data) {
    if (!base64Data || typeof base64Data !== 'string') return null;
    const buffer = Buffer.from(base64Data, 'base64');
    try {
      return zlib.inflateRawSync(buffer).toString('utf-8');
    } catch (e1) {
      try {
        return zlib.inflateSync(buffer).toString('utf-8');
      } catch (e2) {
        return null;
      }
    }
  }

  static decodeCarData(base64Data) {
    const decompressed = this.decompressPayload(base64Data);
    if (!decompressed) return [];

    try {
      const json = JSON.parse(decompressed);
      if (!json) return [];

      const entries = Array.isArray(json)
        ? json
        : (Array.isArray(json.Entries) ? json.Entries : (Array.isArray(json.Cars) ? json.Cars : [json]));

      return entries.map(entry => {
        const utc = entry.Utc || entry.Timestamp;
        const cars = {};
        const carsMap = entry.Cars || entry.cars || (entry.Channels ? { '0': entry } : {});

        for (const [driverNum, carObj] of Object.entries(carsMap)) {
          if (!carObj) continue;
          const channels = carObj.Channels || carObj.channels || carObj;

          const rpm = channels[CHANNELS.RPM] != null ? Number(channels[CHANNELS.RPM]) : null;
          const speed = channels[CHANNELS.SPEED] != null ? Number(channels[CHANNELS.SPEED]) : null;
          const gear = channels[CHANNELS.GEAR] != null ? Number(channels[CHANNELS.GEAR]) : null;
          const throttle = channels[CHANNELS.THROTTLE] != null ? Number(channels[CHANNELS.THROTTLE]) : null;
          const rawBrake = channels[CHANNELS.BRAKE] != null ? Number(channels[CHANNELS.BRAKE]) : null;
          const brake = rawBrake !== null ? (rawBrake > 0 ? 1 : 0) : null;
          const drsRaw = channels[CHANNELS.DRS] != null ? Number(channels[CHANNELS.DRS]) : 0;

          const drsActive = drsRaw >= 8 || drsRaw === 1;
          const drsState = drsRaw >= 8 ? 'OPEN' : (drsRaw === 1 ? 'AVAILABLE' : 'OFF');

          let battery = 80;
          if (channels[CHANNELS.ERS_DEPLOY] != null) {
            battery = Number(channels[CHANNELS.ERS_DEPLOY]);
          } else if (throttle !== null && speed !== null) {
            battery = brake === 1 ? 88 : (throttle > 90 ? 74 : 82);
          }

          cars[driverNum] = {
            rpm: Number.isFinite(rpm) ? rpm : 0,
            speed: Number.isFinite(speed) ? speed : 0,
            gear: Number.isFinite(gear) ? gear : 0,
            throttle: Number.isFinite(throttle) ? throttle : 0,
            brake: brake ?? 0,
            brakeRaw: rawBrake ?? 0,
            drs: drsRaw,
            drsState,
            drsActive,
            battery: Number.isFinite(battery) ? battery : 80,
            ersMode: (drsActive || (speed > 280 && throttle > 95)) ? 'HOTLAP' : 'BALANCED'
          };
        }

        return { utc, cars };
      });
    } catch (err) {
      return [];
    }
  }

  static decodePosition(base64Data) {
    const decompressed = this.decompressPayload(base64Data);
    if (!decompressed) return [];

    try {
      const json = JSON.parse(decompressed);
      if (!json) return [];

      const posList = Array.isArray(json)
        ? json
        : (Array.isArray(json.Position) ? json.Position : (json.Position ? [json.Position] : [json]));

      return posList.map(pos => {
        const utc = pos.Timestamp || pos.Utc;
        const entries = {};
        const rawEntries = pos.Entries || pos.entries || pos.Cars || pos.cars || {};

        for (const [driverNum, entry] of Object.entries(rawEntries)) {
          if (!entry) continue;
          const rawX = entry.X != null ? Number(entry.X) : (entry.x != null ? Number(entry.x) : null);
          const rawY = entry.Y != null ? Number(entry.Y) : (entry.y != null ? Number(entry.y) : null);
          const rawZ = entry.Z != null ? Number(entry.Z) : (entry.z != null ? Number(entry.z) : null);

          // Decimetri convertiti in metri; controllo sui valori finiti
          if (Number.isFinite(rawX) && Number.isFinite(rawY)) {
            entries[driverNum] = {
              status: entry.Status || entry.status || 'OnTrack',
              x: rawX / 10,
              y: rawY / 10,
              z: Number.isFinite(rawZ) ? rawZ / 10 : 0
            };
          }
        }
        return { utc, entries };
      });
    } catch (err) {
      return [];
    }
  }

  static encodeCarData(data) {
    const jsonString = typeof data === 'string' ? data : JSON.stringify(data);
    return zlib.deflateRawSync(Buffer.from(jsonString, 'utf-8')).toString('base64');
  }

  static encodePosition(data) {
    const jsonString = typeof data === 'string' ? data : JSON.stringify(data);
    return zlib.deflateRawSync(Buffer.from(jsonString, 'utf-8')).toString('base64');
  }
}

module.exports = { TelemetryDecoder, CHANNELS };

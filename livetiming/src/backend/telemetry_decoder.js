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
  static decodeCarData(base64Data) {
    if (!base64Data || typeof base64Data !== 'string') return [];

    try {
      const buffer = Buffer.from(base64Data, 'base64');
      const decompressed = zlib.inflateRawSync(buffer).toString('utf-8');
      const json = JSON.parse(decompressed);

      if (!json || !Array.isArray(json.Entries)) return [];

      return json.Entries.map(entry => {
        const utc = entry.Utc;
        const cars = {};

        if (entry.Cars) {
          for (const [driverNum, carObj] of Object.entries(entry.Cars)) {
            const channels = carObj.Channels || {};

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
        }

        return { utc, cars };
      });
    } catch (err) {
      return [];
    }
  }

  static decodePosition(base64Data) {
    if (!base64Data || typeof base64Data !== 'string') return [];

    try {
      const buffer = Buffer.from(base64Data, 'base64');
      const decompressed = zlib.inflateRawSync(buffer).toString('utf-8');
      const json = JSON.parse(decompressed);

      if (!json || !Array.isArray(json.Position)) return [];

      return json.Position.map(pos => {
        const utc = pos.Timestamp;
        const entries = {};

        if (pos.Entries) {
          for (const [driverNum, entry] of Object.entries(pos.Entries)) {
            const rawX = entry.X != null ? Number(entry.X) : null;
            const rawY = entry.Y != null ? Number(entry.Y) : null;
            const rawZ = entry.Z != null ? Number(entry.Z) : null;

            // Decimetri convertiti in metri; controllo sui valori finiti
            if (Number.isFinite(rawX) && Number.isFinite(rawY)) {
              entries[driverNum] = {
                status: entry.Status || 'OnTrack',
                x: rawX / 10,
                y: rawY / 10,
                z: Number.isFinite(rawZ) ? rawZ / 10 : 0
              };
            }
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
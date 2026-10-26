/**
 * FastestLap LiveTiming - Telemetry Decoder (Browser & Node.js Universal)
 * Decodes compressed CarData.z and Position.z channels using native DecompressionStream.
 */
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
  static async decompressPayloadAsync(base64Data) {
    if (!base64Data || typeof base64Data !== 'string') return null;

    // 1. Browser native DecompressionStream (Zero dependencies, fast WebAssembly/C++ engine)
    if (typeof DecompressionStream !== 'undefined') {
      try {
        const bin = atob(base64Data);
        const bytes = Uint8Array.from(bin, c => c.charCodeAt(0));
        const ds = new DecompressionStream('deflate-raw');
        const writer = ds.writable.getWriter();
        writer.write(bytes);
        writer.close();
        return await new Response(ds.readable).text();
      } catch (e1) {
        try {
          const bin = atob(base64Data);
          const bytes = Uint8Array.from(bin, c => c.charCodeAt(0));
          const ds = new DecompressionStream('deflate');
          const writer = ds.writable.getWriter();
          writer.write(bytes);
          writer.close();
          return await new Response(ds.readable).text();
        } catch (e2) {
          return null;
        }
      }
    }

    // 2. Node.js fallback (zlib)
    if (typeof Buffer !== 'undefined') {
      try {
        const zlib = require('zlib');
        const buffer = Buffer.from(base64Data, 'base64');
        return zlib.inflateRawSync(buffer).toString('utf-8');
      } catch (e) {
        return null;
      }
    }

    return null;
  }

  static async decodeCarData(base64Data) {
    const decompressed = await this.decompressPayloadAsync(base64Data);
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
          const ch = carObj.Channels || carObj.channels || carObj;
          const drsVal = ch[CHANNELS.DRS];
          const isDrsOpen = drsVal === 8 || drsVal === 10 || drsVal === 12 || drsVal === 14;
          const isDrsAvailable = drsVal === 1 || drsVal === 9;
          const throttleVal = ch[CHANNELS.THROTTLE] !== undefined ? ch[CHANNELS.THROTTLE] : 0;
          const brakeRaw = ch[CHANNELS.BRAKE] !== undefined ? ch[CHANNELS.BRAKE] : 0;

          cars[driverNum] = {
            rpm: ch[CHANNELS.RPM] !== undefined ? ch[CHANNELS.RPM] : 0,
            speed: ch[CHANNELS.SPEED] !== undefined ? ch[CHANNELS.SPEED] : 0,
            gear: ch[CHANNELS.GEAR] !== undefined ? ch[CHANNELS.GEAR] : 0,
            throttle: throttleVal,
            brake: brakeRaw > 0 ? 1 : 0,
            brakeRaw: brakeRaw,
            drsState: isDrsOpen ? 'OPEN' : (isDrsAvailable ? 'AVAILABLE' : 'OFF'),
            drsActive: isDrsOpen,
            battery: 80,
            ersDeployMode: 'BALANCED'
          };
        }
        return { utc, cars };
      });
    } catch (e) {
      return [];
    }
  }

  static async decodePositions(base64Data) {
    const decompressed = await this.decompressPayloadAsync(base64Data);
    if (!decompressed) return [];

    try {
      const json = JSON.parse(decompressed);
      if (!json) return [];

      const rootList = Array.isArray(json) ? json : (json.Position || [json]);

      return rootList.map(item => {
        const timestamp = item.Timestamp || item.Utc || new Date().toISOString();
        const entries = item.Entries || item.entries || item;
        const positions = {};

        for (const [driverNum, pos] of Object.entries(entries)) {
          if (!pos || typeof pos !== 'object' || pos.X === undefined || pos.Y === undefined) continue;
          positions[driverNum] = {
            x: (pos.X || 0) / 10.0,
            y: (pos.Y || 0) / 10.0,
            z: (pos.Z || 0) / 10.0,
            status: pos.Status || 'OnTrack'
          };
        }
        return { timestamp, positions };
      });
    } catch (e) {
      return [];
    }
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = { TelemetryDecoder, CHANNELS };
}
if (typeof window !== 'undefined') {
  window.TelemetryDecoder = TelemetryDecoder;
  window.CHANNELS = CHANNELS;
}

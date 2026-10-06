import base64
import zlib
import json
from fastf1.livetiming.data import LiveTimingData

def decode_z(raw_str):
    raw_bytes = base64.b64decode(raw_str)
    decompressed = zlib.decompress(raw_bytes, -zlib.MAX_WBITS)
    return json.loads(decompressed.decode('utf-8-sig'))

livedata = LiveTimingData('signalr_test_suite/data/sepang_fastf1_recording.txt')
pos_records = livedata.get('Position.z')

print(f"Total Position.z records: {len(pos_records)}")

# Let's inspect Driver 3 (Verstappen) positions at t = 0, 50, 100, 150, 200, 216, 220, 250, 392
target_secs = [0, 50, 100, 150, 189, 205, 216, 217, 220, 240, 300, 392]

for sec in target_secs:
    if sec < len(pos_records):
        delta, raw_str = pos_records[sec]
        decoded = decode_z(raw_str)
        p_list = decoded.get('Position', [])
        if p_list:
            entries = p_list[0].get('Entries', {})
            d3 = entries.get('3', {})
            sc = entries.get('SC', {})
            sc_x = sc.get('X') if sc else None
            sc_y = sc.get('Y') if sc else None
            sc_st = sc.get('Status') if sc else 'Inactive'
            print(f"t={sec:3d}s ({delta}): Driver 3: X={d3.get('X')}, Y={d3.get('Y')}, Status={d3.get('Status')} | SC: X={sc_x}, Y={sc_y}, Status={sc_st}")

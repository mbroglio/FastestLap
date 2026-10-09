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

# Let's inspect the X, Y range of Position.z across the session
min_x, max_x = 999999, -999999
min_y, max_y = 999999, -999999

for sec in range(0, min(500, len(pos_records)), 5):
    delta, raw_str = pos_records[sec]
    decoded = decode_z(raw_str)
    p_list = decoded.get('Position', [])
    if p_list:
        entries = p_list[0].get('Entries', {})
        for drv, d in entries.items():
            x, y = d.get('X'), d.get('Y')
            if x is not None and y is not None:
                min_x = min(min_x, x)
                max_x = max(max_x, x)
                min_y = min(min_y, y)
                max_y = max(max_y, y)

print(f"Position.z bounds: X: [{min_x}, {max_x}], Y: [{min_y}, {max_y}]")

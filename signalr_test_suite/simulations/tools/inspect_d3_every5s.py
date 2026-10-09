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

print("Driver 3 progress from t=0 to t=225:")
for sec in range(0, 226, 5):
    delta, raw_str = pos_records[sec]
    decoded = decode_z(raw_str)
    p_list = decoded.get('Position', [])
    if p_list:
        entries = p_list[0].get('Entries', {})
        d3 = entries.get('3', {})
        print(f"t={sec:3d}s: X={d3.get('X'):5d}, Y={d3.get('Y'):5d}")

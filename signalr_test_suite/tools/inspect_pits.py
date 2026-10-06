import json
import base64
import zlib
from datetime import datetime
from fastf1.livetiming.data import LiveTimingData

def decode_z(raw_str):
    raw_bytes = base64.b64decode(raw_str)
    decompressed = zlib.decompress(raw_bytes, -zlib.MAX_WBITS)
    return json.loads(decompressed.decode('utf-8-sig'))

with open('signalr_test_suite/data/openf1_pit_11731.json', 'r', encoding='utf-8') as f:
    pits = json.load(f)

print(f"Total raw pit stops in openf1: {len(pits)}")
for p in pits[:5]:
    print(f"Driver {p.get('driver_number')}: lap={p.get('lap_number')}, duration={p.get('pit_duration')}, date={p.get('date')}")

with open('signalr_test_suite/data/formatted_pit_stops.json', 'r', encoding='utf-8') as f:
    fmt_pits = json.load(f)

print(f"\nTotal formatted pit stops: {len(fmt_pits)}")
for p in fmt_pits[:5]:
    print(f"Driver {p.get('driver')}: startSec={p.get('startSec')}, endSec={p.get('endSec')}, dur={p.get('duration')}, lap={p.get('lap')}")

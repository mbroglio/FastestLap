import base64
import zlib
import json
from fastf1.livetiming.data import LiveTimingData

livedata = LiveTimingData('signalr_test_suite/data/sepang_fastf1_recording.txt')
car_records = livedata.get('CarData.z')
print('Total CarData.z records:', len(car_records))

def decode_z(raw_str):
    raw_bytes = base64.b64decode(raw_str)
    decompressed = zlib.decompress(raw_bytes, -zlib.MAX_WBITS)
    return json.loads(decompressed.decode('utf-8-sig'))

for sec in [0, 50, 100, 200, 215, 217, 220, 250, 500, 1000]:
    if sec < len(car_records):
        delta, raw_str = car_records[sec]
        d = decode_z(raw_str)
        cars = d.get('Entries', [{}])[0].get('Cars', {})
        d3 = cars.get('3', {}).get('Channels', {})
        d1 = cars.get('1', {}).get('Channels', {})
        print(f"t={sec:4d}s | D3 (VER): Spd={d3.get('2')}, RPM={d3.get('0')}, Gear={d3.get('3')}, Thr={d3.get('4')}, Brk={d3.get('5')} | D1 (NOR): Spd={d1.get('2')}, Gear={d1.get('3')}")

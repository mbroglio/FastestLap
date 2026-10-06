import base64
import zlib
import json
from fastf1.livetiming.data import LiveTimingData

def decode_z(raw_str):
    raw_bytes = base64.b64decode(raw_str)
    # raw deflate without header
    decompressed = zlib.decompress(raw_bytes, -zlib.MAX_WBITS)
    return json.loads(decompressed.decode('utf-8-sig'))

livedata = LiveTimingData('signalr_test_suite/data/sepang_fastf1_recording.txt')
pos_records = livedata.get('Position.z')
car_records = livedata.get('CarData.z')

print(f"Decoding first 5 Position.z records:")
for i, (delta, raw_str) in enumerate(pos_records[:5]):
    decoded = decode_z(raw_str)
    print(f"Record {i} | delta={delta} | Position.z keys: {list(decoded.keys()) if isinstance(decoded, dict) else type(decoded)}")
    if isinstance(decoded, dict) and 'Position' in decoded:
        p_list = decoded['Position']
        print(f"   Position list len={len(p_list)}, first item={p_list[0] if len(p_list)>0 else None}")

print(f"\nDecoding first 3 CarData.z records:")
for i, (delta, raw_str) in enumerate(car_records[:3]):
    decoded = decode_z(raw_str)
    print(f"Record {i} | delta={delta} | CarData.z entries: {len(decoded.get('Entries', [])) if isinstance(decoded, dict) else type(decoded)}")
    if isinstance(decoded, dict) and 'Entries' in decoded and len(decoded['Entries']) > 0:
        cars = decoded['Entries'][0].get('Cars', {})
        print(f"   Driver 3 channels: {cars.get('3', {}).get('Channels', {})}")

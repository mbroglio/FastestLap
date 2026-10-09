import json

with open('signalr_test_suite/data/openf1_laps_11731.json', 'r', encoding='utf-8') as f:
    laps = json.load(f)

d81_laps = [l for l in laps if l.get('driver_number') == 81]
print("Driver 81 (Piastri) Laps 1 to 5:")
for l in d81_laps[:6]:
    print(f"Lap {l.get('lap_number')}: start={l.get('date_start')}, dur={l.get('lap_duration')}, s1={l.get('duration_sector_1')}, s2={l.get('duration_sector_2')}, s3={l.get('duration_sector_3')}, is_pit={l.get('is_pit_out_lap')}")

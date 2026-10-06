import json

with open('signalr_test_suite/data/openf1_laps_11731.json', 'r', encoding='utf-8') as f:
    laps = json.load(f)

# Find laps for driver 81
d81 = [l for l in laps if l.get('driver_number') == 81]
for l in d81:
    num = l.get('lap_number')
    if num in [2, 3, 4, 5]:
        print(f"Lap {num}: start={l.get('date_start')}, dur={l.get('lap_duration')}, is_pit={l.get('is_pit_out_lap')}")

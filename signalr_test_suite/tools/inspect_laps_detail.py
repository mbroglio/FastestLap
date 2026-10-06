import json

with open('signalr_test_suite/data/openf1_laps_11731.json', 'r', encoding='utf-8') as f:
    laps = json.load(f)

print(f"Total laps entries in openf1: {len(laps)}")
for d in [3, 16, 44]:
    dLaps = [l for l in laps if l.get('driver_number') == d]
    print(f"\nDriver {d} has {len(dLaps)} laps. First 5 laps:")
    for l in dLaps[:5]:
        print(f"  Lap {l.get('lap_number')}: start={l.get('date_start')}, duration={l.get('lap_duration')}, s1={l.get('duration_sector_1')}, s2={l.get('duration_sector_2')}, s3={l.get('duration_sector_3')}, is_pit={l.get('is_pit_out_lap')}")

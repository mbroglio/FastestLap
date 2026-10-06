import json
from collections import defaultdict

with open('signalr_test_suite/data/openf1_stints_11731.json', 'r', encoding='utf-8') as f:
    stints = json.load(f)

print(f"Total entries: {len(stints)}")

driver_stints = defaultdict(list)
for s in stints:
    driver_stints[s['driver_number']].append(s)

# Sort drivers by number
for d_num in sorted(driver_stints.keys()):
    d_list = sorted(driver_stints[d_num], key=lambda x: x['stint_number'])
    print(f"\nDriver #{d_num}:")
    for s in d_list:
        print(f"  Stint {s['stint_number']}: Laps {s['lap_start']}-{s.get('lap_end')} -> Compound: {s['compound']}, Age at start: {s['tyre_age_at_start']}")

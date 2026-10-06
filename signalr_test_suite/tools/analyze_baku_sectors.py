import json
import numpy as np

DATA_DIR = 'signalr_test_suite/data'

with open(f'{DATA_DIR}/openf1_laps_11377.json', 'r', encoding='utf-8') as f:
    laps = json.load(f)

with open(f'{DATA_DIR}/openf1_drivers_11377.json', 'r', encoding='utf-8') as f:
    drivers = json.load(f)

with open(f'{DATA_DIR}/openf1_pit_11377.json', 'r', encoding='utf-8') as f:
    pits = json.load(f)

with open(f'{DATA_DIR}/openf1_race_control_11377.json', 'r', encoding='utf-8') as f:
    rc = json.load(f)

# Valid flying laps (exclude pit in/out laps and SC laps)
valid_laps = [l for l in laps if l.get('lap_duration') and 100 < l['lap_duration'] < 112]

s1_times = [l['duration_sector_1'] for l in valid_laps if l.get('duration_sector_1')]
s2_times = [l['duration_sector_2'] for l in valid_laps if l.get('duration_sector_2')]
s3_times = [l['duration_sector_3'] for l in valid_laps if l.get('duration_sector_3')]

print(f"Total valid flying laps analyzed: {len(valid_laps)}")
print(f"Sector 1: median={np.median(s1_times):.3f}s, min={np.min(s1_times):.3f}s, max={np.max(s1_times):.3f}s")
print(f"Sector 2: median={np.median(s2_times):.3f}s, min={np.min(s2_times):.3f}s, max={np.max(s2_times):.3f}s")
print(f"Sector 3: median={np.median(s3_times):.3f}s, min={np.min(s3_times):.3f}s, max={np.max(s3_times):.3f}s")
print(f"Total Lap: median={np.median(s1_times)+np.median(s2_times)+np.median(s3_times):.3f}s")

# Let's inspect Session Start / Standing Start from RC
print("\n--- RACE START IN RACE CONTROL ---")
for m in rc:
    msg = m.get('message', '').upper()
    if any(k in msg for k in ['START', 'FORMATION', 'GREEN', 'LIGHTS', 'OUT']):
        print(f"[{m['date']}] L{m.get('lap_number')} {m.get('category')}: {m.get('message')}")

# Lap 1 start times across drivers
lap1_starts = [l['date_start'] for l in laps if l.get('lap_number') == 1 and l.get('date_start')]
print(f"\nLap 1 start dates sample: {lap1_starts[:4]}")

# Pit stops summary
print(f"\nTotal pit stops: {len(pits)}")
for p in pits[:8]:
    print(f"Driver #{p['driver_number']:>2}: Lap {p['lap_number']} - Pit Duration: {p['pit_duration']}s at {p['date']}")

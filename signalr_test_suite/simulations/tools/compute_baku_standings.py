import json
import datetime

with open('signalr_test_suite/data/openf1_laps_11377.json', 'r', encoding='utf-8') as f:
    laps = json.load(f)

with open('signalr_test_suite/data/openf1_drivers_11377.json', 'r', encoding='utf-8') as f:
    drivers_raw = json.load(f)

drivers_map = {}
for d in drivers_raw:
    num = str(d['driver_number'])
    if num not in drivers_map:
        drivers_map[num] = d

# Group laps by driver
driver_laps = {}
for l in laps:
    d = str(l['driver_number'])
    if d not in driver_laps:
        driver_laps[d] = []
    driver_laps[d].append(l)

# Check each driver's final completed lap
final_status = []
T0 = datetime.datetime.fromisoformat("2026-09-26T11:03:50.711000+00:00")

for d, d_laps in driver_laps.items():
    d_laps.sort(key=lambda x: x['lap_number'])
    max_lap = d_laps[-1]['lap_number']
    last_lap = d_laps[-1]
    
    # End time of last lap
    if last_lap.get('date_start') and last_lap.get('lap_duration'):
        t_start = datetime.datetime.fromisoformat(last_lap['date_start'])
        t_end = t_start + datetime.timedelta(seconds=last_lap['lap_duration'])
        total_sec = (t_end - T0).total_seconds()
    else:
        total_sec = 999999
        
    final_status.append({
        'driver': d,
        'laps': max_lap,
        'total_sec': total_sec,
        'last_lap': last_lap
    })

# Sort by laps completed (descending), then total_sec (ascending)
final_status.sort(key=lambda x: (-x['laps'], x['total_sec']))

print("=== BAKU 2026 PROVISIONAL RACE CLASSIFICATION ===")
for pos, s in enumerate(final_status, 1):
    d_info = drivers_map.get(s['driver'], {})
    code = d_info.get('name_acronym', s['driver'])
    name = d_info.get('full_name', '')
    team = d_info.get('team_name', '')
    gap = f"+{s['total_sec'] - final_status[0]['total_sec']:.3f}s" if pos > 1 else "WINNER"
    print(f"P{pos:2d}: #{s['driver']:>2} {code} ({name:20s}) - {team:20s} | Laps: {s['laps']:2d} | Gap: {gap}")

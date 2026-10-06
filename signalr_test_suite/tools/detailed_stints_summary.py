import json

stints = json.load(open('signalr_test_suite/data/openf1_stints_11731.json', 'r', encoding='utf-8'))
laps = json.load(open('signalr_test_suite/data/openf1_laps_11731.json', 'r', encoding='utf-8'))

# Map lap times per driver
driver_lap_times = {}
for l in laps:
    drv = l['driver_number']
    lap_num = l['lap_number']
    date_start = l.get('date_start')
    if drv not in driver_lap_times:
        driver_lap_times[drv] = {}
    driver_lap_times[drv][lap_num] = {
        'date_start': date_start,
        'lap_duration': l.get('lap_duration'),
        'is_pit_out': l.get('is_pit_out_lap', False)
    }

print("=== DETTAGLIO STINT E GOMME PER TUTTI I PILOTI ===")
drivers = sorted(list(set(s['driver_number'] for s in stints)))
for d in drivers:
    d_stints = [s for s in stints if s['driver_number'] == d]
    d_stints.sort(key=lambda x: x['stint_number'])
    print(f"\nPilota #{d:2d}:")
    for s in d_stints:
        l_start = s['lap_start']
        l_end = s['lap_end']
        comp = s['compound']
        age = s['tyre_age_at_start']
        print(f"  Stint {s['stint_number']}: Giri {l_start} -> {l_end} | Gomma: {comp:<12} (Età: {age})")

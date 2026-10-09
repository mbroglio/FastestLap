import json
import datetime

T0 = datetime.datetime.fromisoformat("2026-09-26T11:03:50.711000+00:00")

with open('signalr_test_suite/data/openf1_pit_11377.json', 'r', encoding='utf-8') as f:
    pits_raw = json.load(f)

with open('signalr_test_suite/data/openf1_laps_11377.json', 'r', encoding='utf-8') as f:
    laps_raw = json.load(f)

# Group laps by driver and lap number
laps_by_drv_lap = {}
for l in laps_raw:
    d = str(l['driver_number'])
    lap_num = l['lap_number']
    laps_by_drv_lap[(d, lap_num)] = l

clean_pits = []
for p in pits_raw:
    d = str(p['driver_number'])
    lap_num = p['lap_number']
    dur = float(p.get('pit_duration', 21.0))
    
    # Date of pit stop
    p_date = datetime.datetime.fromisoformat(p['date'])
    pit_time_sec = (p_date - T0).total_seconds()
    
    # Check out lap end
    next_lap = laps_by_drv_lap.get((d, lap_num + 1))
    if next_lap and next_lap.get('date_start') and next_lap.get('lap_duration'):
        out_start = datetime.datetime.fromisoformat(next_lap['date_start'])
        out_end = out_start + datetime.timedelta(seconds=next_lap['lap_duration'])
        out_end_sec = (out_end - T0).total_seconds()
    else:
        out_end_sec = pit_time_sec + 85.0
        
    start_sec = round(pit_time_sec - dur * 0.7, 1) # entered pit lane
    end_sec = round(pit_time_sec + dur * 0.3, 1)   # exited pit lane
    
    clean_pits.append({
        "driver": d,
        "lap": lap_num,
        "startSec": start_sec,
        "endSec": end_sec,
        "duration": round(dur, 2),
        "outLapEndSec": round(out_end_sec, 1)
    })

clean_pits.sort(key=lambda x: x['startSec'])
print(f"Processed {len(clean_pits)} authentic pit stops for Baku!")
for p in clean_pits[:8]:
    print(f"Driver #{p['driver']:>2} (Lap {p['lap']:2d}): {p['startSec']:.1f}s -> {p['endSec']:.1f}s (dur: {p['duration']}s, out-lap ends at {p['outLapEndSec']:.1f}s)")

with open('signalr_test_suite/data/baku_pit_stops.json', 'w', encoding='utf-8') as f:
    json.dump(clean_pits, f, indent=2)

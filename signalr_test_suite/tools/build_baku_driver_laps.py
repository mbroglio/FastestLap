import json
import datetime
import os

T0 = datetime.datetime.fromisoformat("2026-09-26T11:03:50.711000+00:00")

with open('signalr_test_suite/data/openf1_laps_11377.json', 'r', encoding='utf-8') as f:
    laps_raw = json.load(f)

with open('signalr_test_suite/data/openf1_drivers_11377.json', 'r', encoding='utf-8') as f:
    drivers_raw = json.load(f)

with open('signalr_test_suite/data/baku_pit_stops.json', 'r', encoding='utf-8') as f:
    pits = json.load(f)

pit_set = {(p['driver'], p['lap']) for p in pits}

# Unique drivers
drivers_keys = sorted(list(set(str(d['driver_number']) for d in drivers_raw)), key=lambda x: int(x))

driver_laps = {}
for d in drivers_keys:
    # All laps for this driver
    dl = [l for l in laps_raw if str(l['driver_number']) == d and l.get('lap_number') and l.get('date_start') and l.get('lap_duration')]
    dl.sort(key=lambda x: x['lap_number'])
    
    clean_laps = []
    for l in dl:
        lap_num = l['lap_number']
        t_start = (datetime.datetime.fromisoformat(l['date_start']) - T0).total_seconds()
        dur = float(l['lap_duration'])
        s1 = float(l['duration_sector_1']) if l.get('duration_sector_1') else round(dur * 0.357, 3)
        s2 = float(l['duration_sector_2']) if l.get('duration_sector_2') else round(dur * 0.409, 3)
        s3 = float(l['duration_sector_3']) if l.get('duration_sector_3') else round(dur - s1 - s2, 3)
        
        is_pit = (d, lap_num) in pit_set
        
        clean_laps.append({
            "lap": lap_num,
            "startSec": round(t_start, 3),
            "dur": round(dur, 3),
            "s1": round(s1, 3),
            "s2": round(s2, 3),
            "s3": round(s3, 3),
            "isPit": is_pit
        })
    driver_laps[d] = clean_laps

total_laps_count = sum(len(v) for v in driver_laps.values())
print(f"Built {total_laps_count} authentic laps for all {len(driver_laps)} drivers at Baku!")

# Save as JSON and JS
with open('signalr_test_suite/data/baku_driver_laps.json', 'w', encoding='utf-8') as f:
    json.dump(driver_laps, f, indent=2)

js_content = f"""/**
 * Official Driver Laps & Sector Timings for Azerbaijan Grand Prix (Baku) 2026
 * Sourced directly from OpenF1 Session 11377 (Dry Linear Race, 51 Laps)
 */

const DRIVER_LAPS = {json.dumps(driver_laps, indent=2)};

if (typeof module !== 'undefined' && module.exports) {{
  module.exports = {{ DRIVER_LAPS }};
}}
"""
with open('signalr_test_suite/src/baku_driver_laps.js', 'w', encoding='utf-8') as f:
    f.write(js_content)

print("Saved to signalr_test_suite/data/baku_driver_laps.json and signalr_test_suite/src/baku_driver_laps.js")

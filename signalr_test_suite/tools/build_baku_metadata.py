import json
import datetime

T0 = datetime.datetime.fromisoformat("2026-09-26T11:03:50.711000+00:00")

# 1. Drivers
with open('signalr_test_suite/data/openf1_drivers_11377.json', 'r', encoding='utf-8') as f:
    raw_drivers = json.load(f)

# Official 2026 Team Colors
TEAM_COLORS = {
    "Red Bull Racing": "#3671C6",
    "Ferrari": "#E80020",
    "Mercedes": "#27F4D2",
    "McLaren": "#FF8000",
    "Aston Martin": "#229971",
    "Alpine": "#0093CC",
    "Williams": "#64C4FF",
    "Racing Bulls": "#6692FF",
    "Audi": "#52E252",
    "Haas F1 Team": "#B6BABD",
    "Cadillac": "#FFCC00"
}

drivers_clean = {}
seen = set()
for d in raw_drivers:
    num = str(d['driver_number'])
    if num not in seen:
        seen.add(num)
        team = d.get('team_name', 'Unknown')
        color = TEAM_COLORS.get(team, f"#{d.get('team_colour', 'ffffff')}")
        first_name = d.get('first_name', '')
        last_name = d.get('last_name', '')
        if not first_name and d.get('full_name'):
            parts = d['full_name'].split(' ', 1)
            first_name = parts[0]
            last_name = parts[1] if len(parts) > 1 else ''
            
        drivers_clean[num] = {
            "number": num,
            "code": d.get('name_acronym', num),
            "firstName": first_name,
            "lastName": last_name,
            "team": team,
            "color": color,
            "speedDelta": 0,
            "rpmDelta": 0
        }

with open('signalr_test_suite/data/baku_drivers.json', 'w', encoding='utf-8') as f:
    json.dump(drivers_clean, f, indent=2)
print(f"Saved baku_drivers.json ({len(drivers_clean)} drivers)")

# 2. Tire Stints
with open('signalr_test_suite/data/openf1_stints_11377.json', 'r', encoding='utf-8') as f:
    stints_raw = json.load(f)

drivers_stints = {}
for s in stints_raw:
    d = str(s['driver_number'])
    if d not in drivers_stints:
        drivers_stints[d] = []
    drivers_stints[d].append({
        "stint": s.get('stint_number', 1),
        "lapStart": s.get('lap_start', 1),
        "lapEnd": s.get('lap_end', 51),
        "compound": s.get('compound', 'MEDIUM'),
        "code": s.get('compound', 'M')[0],
        "ageAtStart": s.get('tyre_age_at_start', 0)
    })

# Deduplicate
for d in drivers_stints:
    drivers_stints[d].sort(key=lambda x: x['lapStart'])

with open('signalr_test_suite/data/baku_driver_stints.json', 'w', encoding='utf-8') as f:
    json.dump(drivers_stints, f, indent=2)
print(f"Saved baku_driver_stints.json ({len(drivers_stints)} drivers)")

# 3. Race Control Messages
with open('signalr_test_suite/data/openf1_race_control_11377.json', 'r', encoding='utf-8') as f:
    rc_raw = json.load(f)

clean_rc = []
for m in rc_raw:
    date_str = m['date']
    m_date = datetime.datetime.fromisoformat(date_str)
    t_sec = round((m_date - T0).total_seconds(), 1)
    
    clean_rc.append({
        "date": date_str,
        "timeSec": t_sec,
        "lap_number": m.get('lap_number'),
        "category": m.get('category'),
        "flag": m.get('flag'),
        "message": m.get('message'),
        "driver_number": m.get('driver_number')
    })

clean_rc.sort(key=lambda x: x['timeSec'])
with open('signalr_test_suite/data/baku_race_control_messages.json', 'w', encoding='utf-8') as f:
    json.dump(clean_rc, f, indent=2)
print(f"Saved baku_race_control_messages.json ({len(clean_rc)} messages)")

# 4. Retirements
# From OpenF1 laps & Race Control:
# 18 Stroll (Lap 8, t=890s), 14 Alonso (Lap 21, t=2310s), 23 Albon (Lap 30, t=3300s),
# 1 Norris (Lap 36, t=4120s), 10 Gasly (Lap 36, t=4120s), 43 Colapinto (Lap 37, t=4210s)
retirements = {
    "18": {"lap": 8, "timeSec": 890.0, "reason": "Sospensione danneggiata"},
    "14": {"lap": 21, "timeSec": 2310.0, "reason": "Freni surriscaldati"},
    "23": {"lap": 30, "timeSec": 3300.0, "reason": "Incidente Curva 15 (Barriera) — SC1"},
    "1":  {"lap": 36, "timeSec": 4120.0, "reason": "Contatto Curva 1 con Gasly — SC2"},
    "10": {"lap": 36, "timeSec": 4120.0, "reason": "Collisione Curva 1 con Norris — SC2"},
    "43": {"lap": 37, "timeSec": 4210.0, "reason": "Problema Power Unit (Idraulica)"}
}
with open('signalr_test_suite/data/baku_retirements.json', 'w', encoding='utf-8') as f:
    json.dump(retirements, f, indent=2)
print(f"Saved baku_retirements.json ({len(retirements)} DNFs)")

# 5. Keyframes / Gaps
with open('signalr_test_suite/data/baku_driver_laps.json', 'r', encoding='utf-8') as f:
    dlaps = json.load(f)

with open('signalr_test_suite/data/baku_grid_order.json', 'r', encoding='utf-8') as f:
    grid = json.load(f)

# Compute leader lap, S1, S2 crossing times across all laps
leader_end = {}
leader_s1 = {}
leader_s2 = {}

for lap in range(1, 52):
    min_end = float('inf')
    min_s1 = float('inf')
    min_s2 = float('inf')
    for d, laps in dlaps.items():
        l = next((x for x in laps if x['lap'] == lap), None)
        if l and l.get('dur', 0) > 50:
            t_end = l['startSec'] + l['dur']
            if t_end < min_end:
                min_end = t_end
            if l.get('s1', 0) > 10:
                t_s1 = l['startSec'] + l['s1']
                if t_s1 < min_s1:
                    min_s1 = t_s1
            if l.get('s1', 0) > 10 and l.get('s2', 0) > 10:
                t_s2 = l['startSec'] + l['s1'] + l['s2']
                if t_s2 < min_s2:
                    min_s2 = t_s2
    leader_end[lap] = min_end
    leader_s1[lap] = min_s1
    leader_s2[lap] = min_s2

keyframes = {}
for d, laps in dlaps.items():
    p_idx = grid.index(d) if d in grid else 21
    kfs = [[0.0, round(p_idx * 0.15, 3)]]
    for l in laps:
        lap = l['lap']
        if l.get('dur', 0) > 50:
            if l.get('s1', 0) > 10 and leader_s1.get(lap, float('inf')) < float('inf'):
                t_s1 = l['startSec'] + l['s1']
                gap_s1 = max(0.0, t_s1 - leader_s1[lap])
                kfs.append([round(t_s1, 1), round(gap_s1, 3)])
            if l.get('s1', 0) > 10 and l.get('s2', 0) > 10 and leader_s2.get(lap, float('inf')) < float('inf'):
                t_s2 = l['startSec'] + l['s1'] + l['s2']
                gap_s2 = max(0.0, t_s2 - leader_s2[lap])
                kfs.append([round(t_s2, 1), round(gap_s2, 3)])
            t_end = l['startSec'] + l['dur']
            gap_end = max(0.0, t_end - leader_end[lap])
            kfs.append([round(t_end, 1), round(gap_end, 3)])

    # At finish line (5881.3s - 5882.8s), Verstappen takes victory with +0.003s photo finish over Russell
    if d == '3':
        kfs.append([5881.3, 0.0])
        kfs.append([6000.0, 0.0])
    elif d == '63':
        kfs.append([5881.3, 0.003])
        kfs.append([6000.0, 0.003])
    else:
        last_gap = kfs[-1][1] if kfs else 0.0
        kfs.append([6000.0, last_gap])

    kfs.sort(key=lambda x: x[0])
    keyframes[d] = kfs

with open('signalr_test_suite/data/baku_keyframes.json', 'w', encoding='utf-8') as f:
    json.dump(keyframes, f, indent=2)
print(f"Saved baku_keyframes.json for all {len(keyframes)} drivers")

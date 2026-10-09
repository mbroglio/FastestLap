import json
import os

DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')

def load_json(name):
    with open(os.path.join(DATA_DIR, name), 'r', encoding='utf-8') as f:
        return json.load(f)

session_info = load_json('openf1_session_11377.json')[0]
drivers = load_json('openf1_drivers_11377.json')
rc = load_json('openf1_race_control_11377.json')
pit = load_json('openf1_pit_11377.json')
stints = load_json('openf1_stints_11377.json')
laps = load_json('openf1_laps_11377.json')

print("=== BAKU 2026 (SESSION 11377) OVERVIEW ===")
print(f"Session: {session_info['session_name']} | Date Start: {session_info['date_start']} | Date End: {session_info['date_end']}")
print(f"Circuit: {session_info['circuit_short_name']} ({session_info['location']}, {session_info['country_name']})")
print(f"Total drivers: {len(drivers)}")
print(f"Total laps recorded in OpenF1: {len(laps)}")
print(f"Total pit stops: {len(pit)}")
print(f"Total stints: {len(stints)}")

# Race Control Key Messages
print("\n--- RACE CONTROL HIGHLIGHTS ---")
key_events = []
for m in rc:
    msg = m.get('message', '').upper()
    cat = m.get('category', '')
    flag = m.get('flag', '')
    if any(k in msg for k in ['SAFETY CAR', 'VSC', 'START', 'STANDING', 'FORMATION', 'SUSPENDED', 'CHEQUERED', 'CLEAR', 'GREEN']):
        key_events.append(m)

for e in key_events:
    print(f"[{e['date']}] L{e.get('lap_number')} {e.get('category')} ({e.get('flag')}): {e['message']}")

# Driver Laps Summary & Winner
max_laps = {}
fastest_laps = {}
for l in laps:
    d = l['driver_number']
    lap_num = l['lap_number']
    max_laps[d] = max(max_laps.get(d, 0), lap_num)
    dur = l.get('lap_duration')
    if dur and dur > 60:
        if d not in fastest_laps or dur < fastest_laps[d]['dur']:
            fastest_laps[d] = {'lap': lap_num, 'dur': dur, 's1': l.get('duration_sector_1'), 's2': l.get('duration_sector_2'), 's3': l.get('duration_sector_3')}

print(f"\nMax laps achieved by cars: {max(max_laps.values()) if max_laps else 0}")
print("\n--- FASTEST LAPS TOP 5 ---")
sorted_fastest = sorted(fastest_laps.items(), key=lambda x: x[1]['dur'])
for d, fl in sorted_fastest[:5]:
    # find driver acronym
    d_obj = next((drv for drv in drivers if drv['driver_number'] == d), {})
    code = d_obj.get('name_acronym', str(d))
    team = d_obj.get('team_name', '')
    m = int(fl['dur'] // 60)
    s = fl['dur'] % 60
    print(f"#{d:>2} {code} ({team}): {m}:{s:06.3f} (Lap {fl['lap']}, S1:{fl['s1']}, S2:{fl['s2']}, S3:{fl['s3']})")

# Tire compounds used
compounds = set(s.get('compound') for s in stints)
print(f"\nTire compounds observed: {compounds}")

import json

with open('signalr_test_suite/data/openf1_race_control_raw.json', 'r', encoding='utf-8') as f:
    rc_msgs = json.load(f)

print("Key Procedure & Status Messages BEFORE Lap 10:")
for e in rc_msgs:
    msg = e.get('message', '')
    cat = e.get('category', '')
    flag = e.get('flag', '')
    if 'IN TRACK SECTOR' in msg:
        continue
    d = e.get('date', '')
    lap = e.get('lap_number')
    if lap is not None and lap > 8:
        continue
    print(f"[{d}] Lap {lap:2d} | {cat:15s} | {str(flag):10s} | {msg}")

import json

with open('signalr_test_suite/data/openf1_race_control_raw.json', 'r', encoding='utf-8') as f:
    msgs = json.load(f)

print(f"Total raw race control messages: {len(msgs)}")
print("\n--- All messages up to 08:45:00 UTC ---")
for m in msgs:
    d = m.get('date', '')
    if d < '2026-10-04T08:45:00':
        lap = m.get('lap_number')
        flag = m.get('flag')
        cat = m.get('category')
        print(f"[{d}] (Lap {lap}) [{flag or 'NO_FLAG'}] [{cat}]: {m.get('message')}")

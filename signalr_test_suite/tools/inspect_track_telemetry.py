import json
import math

with open('signalr_test_suite/data/sepang_exact_track_full.json', 'r', encoding='utf-8') as f:
    nodes = json.load(f)

print(f"Total track nodes: {len(nodes)}")

# Let's inspect coordinates, curvature, and telemetry matching
for i in range(0, len(nodes), 15):
    n = nodes[i]
    t = n.get('t', 0)
    px = n.get('px', 0)
    py = n.get('py', 0)
    sec = n.get('sector', 1)
    spd = n.get('speed', 0)
    gear = n.get('gear', 0)
    thr = n.get('throttle', 0)
    brk = n.get('brake', 0)
    drs = n.get('drs', 0)
    loc = n.get('location', '')
    print(f"t={t:5.1f}s | (px={px:6.1f}, py={py:6.1f}) | Sec {sec} | Spd={spd:3d} km/h | G{gear} | Thr={thr:3d}% | Brk={brk} | DRS={drs} | {loc}")

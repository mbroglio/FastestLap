import urllib.request
import json

url = 'https://api.openf1.org/v1/laps?session_key=11373'
print("Fetching Baku Qualifying laps (11373)...")
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'FastestLap/1.0'})
    with urllib.request.urlopen(req) as resp:
        q_laps = json.loads(resp.read().decode())
    
    # Best lap per driver
    best = {}
    for l in q_laps:
        d = str(l['driver_number'])
        dur = l.get('lap_duration')
        if dur and dur > 80:
            if d not in best or dur < best[d]:
                best[d] = dur
                
    grid = sorted(best.items(), key=lambda x: x[1])
    print(f"Total drivers qualified: {len(grid)}")
    grid_order = [g[0] for g in grid]
    print("Official Starting Grid Order:", grid_order)
    for i, (d, t) in enumerate(grid, 1):
        print(f"P{i:2d}: #{d} - {t:.3f}s")
        
    with open('signalr_test_suite/data/baku_grid_order.json', 'w') as f:
        json.dump(grid_order, f, indent=2)
except Exception as e:
    print("Error:", e)

import json
import math
import numpy as np

with open('signalr_test_suite/data/sepang_exact_track_full.json', 'r', encoding='utf-8') as f:
    nodes = json.load(f)

N = len(nodes)
print(f"Total nodes: {N}")

# Calculate distances and curvatures
pts = np.array([[n['px'], n['py']] for n in nodes])

# Arc lengths
dists = []
for i in range(N):
    p1 = pts[i]
    p2 = pts[(i + 1) % N]
    d = np.linalg.norm(p2 - p1)
    dists.append(d)

total_px_length = sum(dists)
print(f"Total track pixel length: {total_px_length:.2f} px")

# Calculate local curvature for each point i (using window of +/- 3 points for smoothing)
curvatures = []
for i in range(N):
    i_prev = (i - 3) % N
    i_next = (i + 3) % N
    p_prev = pts[i_prev]
    p_curr = pts[i]
    p_next = pts[i_next]
    
    v1 = p_curr - p_prev
    v2 = p_next - p_curr
    
    l1 = np.linalg.norm(v1)
    l2 = np.linalg.norm(v2)
    
    if l1 * l2 == 0:
        curvatures.append(0)
        continue
        
    dot = np.dot(v1, v2) / (l1 * l2)
    dot = max(-1.0, min(1.0, dot))
    angle = math.acos(dot)
    
    # Arc length between prev and next
    arc = l1 + l2
    curv = angle / arc if arc > 0 else 0
    curvatures.append(curv)

# Find local curvature peaks (corners/apexes)
peaks = []
for i in range(N):
    prev_c = curvatures[(i - 1) % N]
    curr_c = curvatures[i]
    next_c = curvatures[(i + 1) % N]
    if curr_c > prev_c and curr_c > next_c and curr_c > 0.005:
        peaks.append((i, curr_c, nodes[i]))

print(f"\nFound {len(peaks)} curvature peaks (turn apexes):")
for idx, c, n in peaks:
    print(f"Node {idx:3d} (t={n['t']:4.1f}s, px={n['px']:6.1f}, py={n['py']:6.1f}): Curv={c:.4f} | Current Spd={n['speed']} km/h | Current Thr={n['throttle']}% | Current Brk={n['brake']} | Loc={n['location']}")

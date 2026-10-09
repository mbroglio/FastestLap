from svg.path import parse_path
import xml.etree.ElementTree as ET
import numpy as np

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()
p = root.find('.//*[@id="path1403"]')
po = parse_path(p.get('d'))

ns = {'svg': 'http://www.w3.org/2000/svg'}
texts = root.findall('.//svg:text', ns)
turns = {}
for t in texts:
    c = "".join(t.itertext()).strip()
    if c.isdigit() and 1 <= int(c) <= 20:
        turns[int(c)] = np.array([float(t.get('x')), float(t.get('y'))])

# Sample 500 points along path
N = 500
path_pts = np.array([[po.point(i/N).real, po.point(i/N).imag] for i in range(N)])

# For each turn, find which u index is closest
print("Closest u to each Turn:")
turn_u = {}
for t_num in sorted(turns.keys()):
    pos = turns[t_num]
    dists = np.linalg.norm(path_pts - pos, axis=1)
    min_idx = np.argmin(dists)
    u_val = min_idx / N
    turn_u[t_num] = u_val
    print(f"Turn {t_num:2d}: closest u={u_val:.3f} (dist={dists[min_idx]:.1f})")

# Also find Start/Finish line position (path1407: start=(369.0, 35.5))
sf_pos = np.array([369.0, 35.5])
sf_dists = np.linalg.norm(path_pts - sf_pos, axis=1)
sf_idx = np.argmin(sf_dists)
sf_u = sf_idx / N
print(f"\nStart/Finish Line: closest u={sf_u:.3f} (dist={sf_dists[sf_idx]:.1f})")

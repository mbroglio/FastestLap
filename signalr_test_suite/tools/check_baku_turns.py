import xml.etree.ElementTree as ET
from svg.path import parse_path
import numpy as np

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()

# Find texts for turns 1 to 20
ns = {'svg': 'http://www.w3.org/2000/svg'}
texts = root.findall('.//svg:text', ns)
turn_positions = {}
for t in texts:
    c = "".join(t.itertext()).strip()
    if c.isdigit() and 1 <= int(c) <= 20:
        # text x, y
        tx = float(t.get('x'))
        ty = float(t.get('y'))
        # Is text inside layer1 or transformed?
        # Check parent of text
        turn_positions[int(c)] = (tx, ty)

print("Found turn text positions:")
for k in sorted(turn_positions.keys()):
    print(f"Turn {k:2d}: {turn_positions[k]}")

# Sample path at 50 points
p = root.find('.//*[@id="path1403"]')
path_obj = parse_path(p.get('d'))

print("\nSampling path along u:")
for u in [0.0, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.95, 1.0]:
    pt = path_obj.point(u)
    # raw coords in path space (before layer1 translate)
    print(f"u={u:4.2f}: raw=({pt.real:7.2f}, {pt.imag:7.2f}) | layer1=({pt.real+241.05:7.2f}, {pt.imag+102.19:7.2f})")

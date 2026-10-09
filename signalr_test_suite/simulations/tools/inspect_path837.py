import xml.etree.ElementTree as ET
from svg.path import parse_path
import numpy as np

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()
p = root.find('.//*[@id="path837"]')
po = parse_path(p.get('d'))

print(f"Path837 length: {po.length():.1f}")
N = 30
pts = []
for i in range(N):
    u = i / (N - 1)
    pt = po.point(u)
    px = (pt.real + 241.05) * 0.727
    py = (pt.imag + 102.19) * 0.728
    pts.append([px, py])

pts = np.array(pts)
print(f"X bounds: [{pts[:, 0].min():.1f}, {pts[:, 0].max():.1f}]")
print(f"Y bounds: [{pts[:, 1].min():.1f}, {pts[:, 1].max():.1f}]")

for i in range(0, N, 3):
    print(f"u={i/(N-1):.2f}: px={pts[i, 0]:.1f}, py={pts[i, 1]:.1f}")

from svg.path import parse_path
import xml.etree.ElementTree as ET
import numpy as np

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()
p = root.find('.//*[@id="path1403"]')
po = parse_path(p.get('d'))

# Check distance between point(u) and point(1-u)
print("Comparing point(u) and point(1-u):")
for u in [0.05, 0.1, 0.2, 0.3, 0.4]:
    p1 = po.point(u)
    p2 = po.point(1.0 - u)
    dist = abs(p1 - p2)
    print(f"u={u:4.2f}: p(u)=({p1.real:6.1f}, {p1.imag:6.1f}) | p(1-u)=({p2.real:6.1f}, {p2.imag:6.1f}) | dist={dist:5.2f}")

import xml.etree.ElementTree as ET
from svg.path import parse_path

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()

def inspect_path(p_id):
    p = root.find(f'.//*[@id="{p_id}"]')
    if p is None: return
    d = p.get('d')
    po = parse_path(d)
    dist = abs(po.point(0) - po.point(1))
    print(f"Path {p_id}: start=({po.point(0).real:.1f}, {po.point(0).imag:.1f}), end=({po.point(1).real:.1f}, {po.point(1).imag:.1f}), len={po.length():.1f}, closed={dist < 0.1}")

for pid in ['path1403', 'path837', 'path5102', 'path1407', 'path3977', 'path1093']:
    inspect_path(pid)

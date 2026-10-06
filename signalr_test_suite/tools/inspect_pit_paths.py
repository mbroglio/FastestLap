import xml.etree.ElementTree as ET
from svg.path import parse_path

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()

for pid in ['path1091', 'path1093', 'path1095']:
    p = root.find(f'.//*[@id="{pid}"]')
    if p is not None:
        po = parse_path(p.get('d'))
        # Layer1 translate is (241.05, 102.19)
        # Webp scale is scale_x=0.727, scale_y=0.728
        pts_x = [(po.point(u).real + 241.05) * 0.727 for u in [0, 0.25, 0.5, 0.75, 1]]
        pts_y = [(po.point(u).imag + 102.19) * 0.728 for u in [0, 0.25, 0.5, 0.75, 1]]
        print(f'{pid}: len={po.length():.1f}')
        for k in range(len(pts_x)):
            print(f'   u={k*0.25:.2f}: px={pts_x[k]:.1f}, py={pts_y[k]:.1f}')

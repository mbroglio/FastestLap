from svg.path import parse_path
import xml.etree.ElementTree as ET

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()
p = root.find('.//*[@id="path1403"]')
po = parse_path(p.get('d'))

print('Total segments in po:', len(po))
for i in range(min(20, len(po))):
    seg = po[i]
    print(f'Seg {i:2d}: {type(seg).__name__:6s} start=({seg.start.real:6.1f}, {seg.start.imag:6.1f}) end=({seg.end.real:6.1f}, {seg.end.imag:6.1f}) len={seg.length():5.1f}')

# Check last segment
last_seg = po[-1]
print(f'Last seg: {type(last_seg).__name__:6s} start=({last_seg.start.real:6.1f}, {last_seg.start.imag:6.1f}) end=({last_seg.end.real:6.1f}, {last_seg.end.imag:6.1f}) len={last_seg.length():5.1f}')

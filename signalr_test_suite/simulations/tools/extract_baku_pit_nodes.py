import xml.etree.ElementTree as ET
from svg.path import parse_path
import json

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()
p = root.find('.//*[@id="path837"]')
po = parse_path(p.get('d'))

# Pit lane runs from Turn 20 approach towards Turn 1
# Let's sample 25 points along path837
# In path837, let's see which half goes forward towards Turn 1
# u from 0.0 to 0.41 goes from px=348 (Turn 20 end) to px=475 (Turn 1)!
N = 25
pit_nodes = []
for i in range(N):
    u = (i / (N - 1)) * 0.41
    pt = po.point(u)
    px = round((pt.real + 241.05) * 0.727, 2)
    py = round((pt.imag + 102.19) * 0.728, 2)
    pit_nodes.append({"px": px, "py": py})

print(f"Generated {len(pit_nodes)} pit lane nodes for Baku:")
for i in [0, 5, 12, 18, 24]:
    print(f"Node {i:2d}: px={pit_nodes[i]['px']}, py={pit_nodes[i]['py']}")

with open('signalr_test_suite/data/baku_pit_lane_nodes.json', 'w', encoding='utf-8') as f:
    json.dump(pit_nodes, f, indent=2)
print("Saved to signalr_test_suite/data/baku_pit_lane_nodes.json")

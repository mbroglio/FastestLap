import xml.etree.ElementTree as ET

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()

for p in root.findall('.//{http://www.w3.org/2000/svg}path'):
    p_id = p.get('id', '')
    d_len = len(p.get('d', ''))
    style = p.get('style', '')
    print(f"{p_id:12s} | len={d_len:<5d} | {style[:75]}")

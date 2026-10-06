import xml.etree.ElementTree as ET

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()

ns = {'svg': 'http://www.w3.org/2000/svg'}
paths = root.findall('.//svg:path', ns)
for p in paths:
    d = p.get('d', '')
    p_id = p.get('id', '')
    stroke = p.get('stroke', '')
    fill = p.get('fill', '')
    style = p.get('style', '')
    # Check if stroke width is large or if len(d) > 500
    if len(d) > 300:
        print(f"Path id={p_id}, len(d)={len(d)}, stroke={stroke}, fill={fill}")
        print(f"   style: {style}")
        print(f"   preview: {d[:100]}")

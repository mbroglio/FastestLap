import xml.etree.ElementTree as ET

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()
print('Root attribs:', root.attrib)

ns = {'svg': 'http://www.w3.org/2000/svg'}
paths = root.findall('.//svg:path', ns) or root.findall('.//path')
print(f'Total paths found: {len(paths)}')
for i, p in enumerate(paths):
    d = p.get('d', '')
    p_id = p.get('id', '')
    cls = p.get('class', '')
    stroke = p.get('stroke', '')
    fill = p.get('fill', '')
    style = p.get('style', '')
    print(f'Path {i}: id="{p_id}", class="{cls}", stroke="{stroke}", fill="{fill}", style="{style}", len(d)={len(d)}')
    if len(d) > 100:
        print(f'   d preview: {d[:150]}...')

# Find text elements
texts = root.findall('.//svg:text', ns) or root.findall('.//text')
print(f'\nTotal texts: {len(texts)}')
for t in texts[:20]:
    content = "".join(t.itertext()).strip()
    print(f'Text ({t.get("x")}, {t.get("y")}): "{content}"')

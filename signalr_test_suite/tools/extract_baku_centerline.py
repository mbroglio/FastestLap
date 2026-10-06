import xml.etree.ElementTree as ET
from svg.path import parse_path
import numpy as np
import json
import math

tree = ET.parse('signalr_test_suite/baku_circuit.svg')
root = tree.getroot()

# Find path1403
ns = {'svg': 'http://www.w3.org/2000/svg'}
p = root.find('.//*[@id="path1403"]')
d = p.get('d')

path_obj = parse_path(d)
total_len = path_obj.length()
print(f"Total path length: {total_len:.3f}")

# Sample 500 points
N = 500
pts = []
for i in range(N):
    u = i / N
    pt = path_obj.point(u)
    # Apply layer1 transform: translate(241.04808, 102.18643)
    px = pt.real + 241.04808
    py = pt.imag + 102.18643
    pts.append([px, py])

pts = np.array(pts)
print(f"Sampled {N} points.")
print(f"X bounds: [{pts[:, 0].min():.2f}, {pts[:, 0].max():.2f}]")
print(f"Y bounds: [{pts[:, 1].min():.2f}, {pts[:, 1].max():.2f}]")

# Check SVG viewBox
vb = root.get('viewBox').split()
vb_w, vb_h = float(vb[2]), float(vb[3])
print(f"SVG ViewBox: {vb_w} x {vb_h}")

# Check aspect ratios
print(f"ViewBox aspect: {vb_w / vb_h:.4f}")
print(f"WebP (500x371) aspect: {500 / 371:.4f}")

# Scale factor from SVG viewBox (687.715 x 509.599) to WebP bitmap (500 x 371)
scale_x = 500.0 / vb_w
scale_y = 371.0 / vb_h
print(f"Scale to WebP: scale_x={scale_x:.4f}, scale_y={scale_y:.4f}")

pts_webp = pts.copy()
pts_webp[:, 0] *= scale_x
pts_webp[:, 1] *= scale_y
print(f"WebP pixel bounds: X=[{pts_webp[:, 0].min():.2f}, {pts_webp[:, 0].max():.2f}], Y=[{pts_webp[:, 1].min():.2f}, {pts_webp[:, 1].max():.2f}]")

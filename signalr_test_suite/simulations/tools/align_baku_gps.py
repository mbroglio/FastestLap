import json
import numpy as np
from PIL import Image, ImageDraw

# Load GPS points
with open('signalr_test_suite/data/baku_lap2_telemetry_gps.json', 'r', encoding='utf-8') as f:
    locs = json.load(f)

gps_pts = np.array([[p['x'], p['y']] for p in locs])
N = len(gps_pts)

# Load Baku webp image
img = Image.open('signalr_test_suite/Baku_Formula_One_circuit_map.svg.webp').convert('RGBA')
w, h = img.size
print(f"Image size: {w}x{h}")

gx = gps_pts[:, 0]
gy = gps_pts[:, 1]

# Center GPS around mean
gx_c = gx - np.mean(gx)
gy_c = gy - np.mean(gy)

for angle_deg in [0, 45, 90, 135, 180, 225, 270, 315]:
    theta = np.radians(angle_deg)
    cos_t, sin_t = np.cos(theta), np.sin(theta)
    rot_x = gx_c * cos_t - gy_c * sin_t
    rot_y = gx_c * sin_t + gy_c * cos_t
    
    rx_span = rot_x.max() - rot_x.min()
    ry_span = rot_y.max() - rot_y.min()
    aspect = rx_span / ry_span if ry_span > 0 else 0
    print(f"Angle {angle_deg:3d}°: aspect={aspect:.3f} (span: {rx_span:.0f} x {ry_span:.0f})")

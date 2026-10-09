import json
import numpy as np
from PIL import Image, ImageDraw

with open('signalr_test_suite/data/baku_lap2_telemetry_gps.json', 'r', encoding='utf-8') as f:
    locs = json.load(f)

gps_pts = np.array([[p['x'], p['y']] for p in locs])

# In Baku City Circuit map (500x371):
# The track goes:
# Bottom straight (Neftchilar Ave) is at the bottom, going from left to right (eastwards)
# Then Turn 1 turns left (northwards) at the bottom-right/mid-right
# Then Turn 2 turns left (westwards)
# Then Turn 3 turns right (northwards)
# Then castle section in the middle/upper-middle
# Then downhill back to the straight at the bottom-left!
# In OpenF1 GPS:
# Let's inspect x and y direction:
gx = gps_pts[:, 0]
gy = gps_pts[:, 1]

# Let's test flips:
# Case 1: x -> px, -y -> py (standard cartesian to screen Y)
# Case 2: -x -> px, -y -> py
# Case 3: x -> px, y -> py
# Case 4: -x -> px, y -> py

for flip_x in [1, -1]:
    for flip_y in [1, -1]:
        tx = gx * flip_x
        ty = gy * flip_y
        
        # Scale to fit comfortably in 500x371 with a 20px margin
        pad = 25
        target_w = 500 - 2 * pad
        target_h = 371 - 2 * pad
        
        scale_x = target_w / (tx.max() - tx.min())
        scale_y = target_h / (ty.max() - ty.min())
        scale = min(scale_x, scale_y)
        
        px = pad + (tx - tx.min()) * scale + (target_w - (tx.max() - tx.min()) * scale) / 2
        py = pad + (ty - ty.min()) * scale + (target_h - (ty.max() - ty.min()) * scale) / 2
        
        # Create overlay
        img = Image.open('signalr_test_suite/Baku_Formula_One_circuit_map.svg.webp').convert('RGBA')
        draw = ImageDraw.Draw(img)
        
        pts_list = list(zip(px, py))
        draw.line(pts_list, fill=(255, 0, 0, 255), width=3)
        # Mark start with green circle
        draw.ellipse([px[0]-5, py[0]-5, px[0]+5, py[0]+5], fill=(0, 255, 0, 255))
        # Mark mid with blue circle
        draw.ellipse([px[200]-5, py[200]-5, px[200]+5, py[200]+5], fill=(0, 0, 255, 255))
        
        out_name = f'signalr_test_suite/debug_overlay_fx{flip_x}_fy{flip_y}.png'
        img.save(out_name)
        print(f"Saved {out_name}")

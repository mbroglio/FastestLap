import json
import numpy as np
from PIL import Image
from scipy.ndimage import binary_dilation, distance_transform_edt
from scipy.optimize import minimize

# 1. Load authentic GPS points
with open('signalr_test_suite/data/baku_lap2_telemetry_gps.json', 'r', encoding='utf-8') as f:
    locs = json.load(f)

gps_pts = np.array([[p['x'], p['y']] for p in locs], dtype=float)

# 2. Load track image mask
img = Image.open('signalr_test_suite/Baku_Formula_One_circuit_map.svg.webp').convert('RGBA')
w, h = img.size
arr = np.array(img)
# True track stroke (black/dark grey and colored sector lines, excluding background and white)
is_track = (arr[:, :, 3] > 128) & ((arr[:, :, 0] < 50) & (arr[:, :, 1] < 50) & (arr[:, :, 2] < 50))
is_sector = (arr[:, :, 3] > 128) & (arr[:, :, 0] != 240) & ((arr[:, :, 0] > 60) | (arr[:, :, 1] > 60) | (arr[:, :, 2] > 60))
track_mask = is_track | is_sector

# Distance transform: dist[y, x] is Euclidean distance in pixels to nearest track pixel
dist_map = distance_transform_edt(~track_mask)

# Initial guess parameters:
# fx = 1, fy = -1
gx = gps_pts[:, 0]
gy = -gps_pts[:, 1] # invert y

gx_min, gx_max = gx.min(), gx.max()
gy_min, gy_max = gy.min(), gy.max()

# Center around (0, 0)
gx_c = gx - (gx_min + gx_max) / 2
gy_c = gy - (gy_min + gy_max) / 2

# Scale
init_s = 460.0 / (gx_max - gx_min)
init_tx = 250.0
init_ty = 185.0
init_rot = 0.0 # radians

def loss(params):
    s, rot, tx, ty = params
    cos_r = np.cos(rot)
    sin_r = np.sin(rot)
    
    rx = gx_c * cos_r - gy_c * sin_r
    ry = gx_c * sin_r + gy_c * cos_r
    
    px = rx * s + tx
    py = ry * s + ty
    
    # Clip to image bounds
    px_cl = np.clip(px, 0, w - 1).astype(int)
    py_cl = np.clip(py, 0, h - 1).astype(int)
    
    # Penalize out of bounds heavily
    oob = (px < 0) | (px >= w) | (py < 0) | (py >= h)
    oob_penalty = np.sum(oob) * 100.0
    
    dists = dist_map[py_cl, px_cl]
    return np.mean(dists) + oob_penalty

res = minimize(loss, [init_s, init_rot, init_tx, init_ty], method='Nelder-Mead', options={'maxiter': 2000})
print("Optimization result:", res.message)
s_opt, rot_opt, tx_opt, ty_opt = res.x
print(f"Optimal parameters: s={s_opt:.6f}, rot={np.degrees(rot_opt):.3f} deg, tx={tx_opt:.2f}, ty={ty_opt:.2f}")

# Calculate transformed coordinates
cos_r = np.cos(rot_opt)
sin_r = np.sin(rot_opt)
rx = gx_c * cos_r - gy_c * sin_r
ry = gx_c * sin_r + gy_c * cos_r
px_opt = rx * s_opt + tx_opt
py_opt = ry * s_opt + ty_opt

px_cl = np.clip(px_opt, 0, w - 1).astype(int)
py_cl = np.clip(py_opt, 0, h - 1).astype(int)
final_dists = dist_map[py_cl, px_cl]
print(f"Final mean pixel distance to track centerline: {np.mean(final_dists):.2f} px!")
print(f"Percentage of points within 2 pixels: {np.mean(final_dists <= 2.0)*100:.1f}%")
print(f"Percentage of points within 4 pixels: {np.mean(final_dists <= 4.0)*100:.1f}%")

# Save overlay verification image
from PIL import ImageDraw
vis = img.copy()
draw = ImageDraw.Draw(vis)
pts_list = list(zip(px_opt, py_opt))
draw.line(pts_list, fill=(0, 255, 0, 255), width=2)
# Start/finish point (green dot)
draw.ellipse([px_opt[0]-4, py_opt[0]-4, px_opt[0]+4, py_opt[0]+4], fill=(255, 0, 0, 255))
vis.save('signalr_test_suite/baku_exact_fitted_track.png')
print("Saved verification image: signalr_test_suite/baku_exact_fitted_track.png")

# Save transformation parameters
transform_meta = {
    "scale": s_opt,
    "rotation_deg": float(np.degrees(rot_opt)),
    "rotation_rad": rot_opt,
    "tx": tx_opt,
    "ty": ty_opt,
    "gx_center": float((gx_min + gx_max) / 2),
    "gy_center": float((gy_min + gy_max) / 2),
    "img_width": w,
    "img_height": h
}
with open('signalr_test_suite/data/baku_gps_transform.json', 'w', encoding='utf-8') as f:
    json.dump(transform_meta, f, indent=2)
print("Saved transformation metadata: signalr_test_suite/data/baku_gps_transform.json")

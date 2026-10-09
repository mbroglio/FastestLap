import json
import numpy as np
from scipy.interpolate import interp1d

# 1. Load fitted transformation metadata
with open('signalr_test_suite/data/baku_gps_transform.json', 'r', encoding='utf-8') as f:
    tf = json.load(f)

# 2. Load GPS points
with open('signalr_test_suite/data/baku_lap2_telemetry_gps.json', 'r', encoding='utf-8') as f:
    locs = json.load(f)

gps_pts = np.array([[p['x'], p['y']] for p in locs], dtype=float)
N_raw = len(gps_pts)

# Apply exact affine transform to get (px, py)
gx = gps_pts[:, 0]
gy = -gps_pts[:, 1]
gx_c = gx - tf['gx_center']
gy_c = gy - tf['gy_center']

rot_rad = tf['rotation_rad']
cos_r = np.cos(rot_rad)
sin_r = np.sin(rot_rad)
rx = gx_c * cos_r - gy_c * sin_r
ry = gx_c * sin_r + gy_c * cos_r
px = rx * tf['scale'] + tf['tx']
py = ry * tf['scale'] + tf['ty']

# The start point (index 0) in the lap2 telemetry is right after the Start/Finish line
# Let's compute cumulative arc length
dists = [0.0]
for i in range(N_raw - 1):
    d = np.hypot(px[i+1] - px[i], py[i+1] - py[i])
    dists.append(dists[-1] + d)

# Close loop to start point
d_close = np.hypot(px[0] - px[-1], py[0] - py[-1])
dists.append(dists[-1] + d_close)

px_loop = np.append(px, px[0])
py_loop = np.append(py, py[0])
gx_loop = np.append(gps_pts[:, 0], gps_pts[0, 0])
gy_loop = np.append(gps_pts[:, 1], gps_pts[0, 1])

total_dist = dists[-1]
print(f"Total loop distance in pixels: {total_dist:.2f} px")

# Resample into N=450 uniform nodes along lap time T=108.6s
N_NODES = 450
LAP_DURATION = 108.6

s_uniform = np.linspace(0, total_dist, N_NODES, endpoint=False)
fx = interp1d(dists, px_loop, kind='cubic')
fy = interp1d(dists, py_loop, kind='cubic')
fgx = interp1d(dists, gx_loop, kind='linear')
fgy = interp1d(dists, gy_loop, kind='linear')

px_nodes = fx(s_uniform)
py_nodes = fy(s_uniform)
gx_nodes = fgx(s_uniform)
gy_nodes = fgy(s_uniform)

# Baku official sector beam crossings:
# S1 ends at T_S1 = 38.8s
# S2 ends at T_S2 = 38.8 + 44.5 = 83.3s
# S3 ends at T_S3 = 108.6s
T_S1 = 38.8
T_S2 = 83.3

nodes = []
for i in range(N_NODES):
    t = (i / N_NODES) * LAP_DURATION
    p_x = float(px_nodes[i])
    p_y = float(py_nodes[i])
    x_dec = int(round(gx_nodes[i]))
    y_dec = int(round(gy_nodes[i]))
    
    # Determine sector
    if t < T_S1:
        sector = 1
    elif t < T_S2:
        sector = 2
    else:
        sector = 3
        
    # Realistic Baku telemetry based on track location & corners
    # S1:
    if t < 5.0:
        # S/F straight DRS zone 1 approach to Turn 1
        speed = int(320 + (t / 5.0) * 15)
        gear = 8
        rpm = 12400
        throttle = 100
        brake = 0
        drs = 1
        loc = "Rettifilo Arrivo (DRS Zona 1 — 335 km/h)"
    elif t < 8.5:
        # Turn 1 braking & apex
        frac = (t - 5.0) / 3.5
        speed = int(335 - frac * 230) # 335 -> 105 km/h
        gear = 3 if speed < 130 else (4 if speed < 180 else (5 if speed < 240 else 7))
        rpm = 10800
        throttle = 0 if frac < 0.7 else 30
        brake = 1 if frac < 0.7 else 0
        drs = 0
        loc = "Curva 1 — Staccata Violenta 90° a Sinistra"
    elif t < 12.0:
        # Straight to Turn 2
        frac = (t - 8.5) / 3.5
        speed = int(105 + frac * 165) # 105 -> 270 km/h
        gear = 4 if speed < 160 else (5 if speed < 210 else 6)
        rpm = 11800
        throttle = 100
        brake = 0
        drs = 0
        loc = "Pushkin Street — Allungo verso Curva 2"
    elif t < 15.5:
        # Turn 2 braking & apex
        frac = (t - 12.0) / 3.5
        speed = int(270 - frac * 170) # 270 -> 100 km/h
        gear = 3
        rpm = 10500
        throttle = 0 if frac < 0.7 else 35
        brake = 1 if frac < 0.7 else 0
        drs = 0
        loc = "Curva 2 — 90° a Sinistra verso Khagani Street"
    elif t < 27.0:
        # Khagani Street DRS Zone 2
        frac = (t - 15.5) / 11.5
        speed = int(100 + frac * 215) # 100 -> 315 km/h
        gear = 4 if speed < 160 else (5 if speed < 210 else (6 if speed < 260 else (7 if speed < 300 else 8)))
        rpm = 12200
        throttle = 100
        brake = 0
        drs = 1
        loc = "Khagani Street (DRS Zona 2 — 315 km/h)"
    elif t < 31.0:
        # Turn 3 braking & apex
        frac = (t - 27.0) / 4.0
        speed = int(315 - frac * 217) # 315 -> 98 km/h
        gear = 3
        rpm = 10400
        throttle = 0 if frac < 0.7 else 30
        brake = 1 if frac < 0.7 else 0
        drs = 0
        loc = "Curva 3 — Staccata 90° a Destra"
    elif t < 35.0:
        # Short straight to Turn 4
        frac = (t - 31.0) / 4.0
        speed = int(98 + frac * 105) # 98 -> 203 km/h
        gear = 4
        rpm = 11200
        throttle = 80
        brake = 0
        drs = 0
        loc = "Curva 4 — Allineamento e Ingresso 90° Sinistra"
    elif t < T_S1:
        # Exit Turn 4 towards Sector 1 beam
        speed = 210
        gear = 5
        rpm = 11500
        throttle = 100
        brake = 0
        drs = 0
        loc = "Fine Settore 1 — Traguardo Fascio S1"
    # S2:
    elif t < 46.0:
        # Turns 5-6 chicane
        frac = (t - T_S1) / 7.2
        speed = int(140 + np.sin(frac * np.pi) * 20)
        gear = 3
        rpm = 10600
        throttle = 45
        brake = 0
        drs = 0
        loc = "Curve 5-6 — Chicane Destra-Sinistra"
    elif t < 51.0:
        # Turn 7 90° right into Castle
        speed = 95
        gear = 2
        rpm = 10200
        throttle = 35
        brake = 1 if (t - 46.0) < 2.0 else 0
        drs = 0
        loc = "Curva 7 — Ingresso Zona Castello (Icherisheher)"
    elif t < 68.0:
        # Castle Section Turns 8, 9, 10, 11, 12
        frac = (t - 51.0) / 17.0
        speed = int(88 + np.sin(frac * 4 * np.pi) * 35) # 88 to 125 km/h
        gear = 2 if speed < 105 else 3
        rpm = 10500
        throttle = int(40 + np.sin(frac * 4 * np.pi) * 30)
        brake = 0
        drs = 0
        loc = "Curve 8-12 — Sezione del Castello (Larghezza 7.6m!)"
    elif t < 75.0:
        # Turns 13-14 fast downhill sweeps
        frac = (t - 68.0) / 7.0
        speed = int(125 + frac * 95) # 125 -> 220 km/h
        gear = 4 if speed < 165 else 5
        rpm = 11600
        throttle = 95
        brake = 0
        drs = 0
        loc = "Curve 13-14 — Discesa Veloce verso Curva 15"
    elif t < 80.0:
        # Turn 15 downhill braking
        frac = (t - 75.0) / 5.0
        speed = int(220 - frac * 118) # 220 -> 102 km/h
        gear = 3
        rpm = 10500
        throttle = 0 if frac < 0.7 else 40
        brake = 1 if frac < 0.7 else 0
        drs = 0
        loc = "Curva 15 — Staccata Cieca in Discesa contro le Barriere"
    elif t < T_S2:
        # Towards Turn 16 / S2 beam
        speed = 135
        gear = 3
        rpm = 10800
        throttle = 70
        brake = 0
        drs = 0
        loc = "Fine Settore 2 — Traguardo Fascio S2"
    # S3:
    elif t < 87.0:
        # Turn 16 apex & launch onto long straight
        frac = (t - T_S2) / 3.7
        speed = int(120 + frac * 50) # 120 -> 170 km/h
        gear = 3 if speed < 145 else 4
        rpm = 11200
        throttle = 100
        brake = 0
        drs = 0
        loc = "Curva 16 — Svolta Cruciale a Sinistra verso il Maxi-Rettifilo"
    elif t < 95.0:
        # Turns 17, 18, 19, 20 high speed sweeps
        frac = (t - 87.0) / 8.0
        speed = int(170 + frac * 140) # 170 -> 310 km/h
        gear = 5 if speed < 210 else (6 if speed < 260 else 7)
        rpm = 12000
        throttle = 100
        brake = 0
        drs = 0
        loc = "Curve 17-20 — Esse Veloci in Piena Accelerazione (Full Throttle)"
    else:
        # Turn 20 exit onto Neftchilar Avenue (DRS Zone 1, 2.2 km long!)
        frac = (t - 95.0) / (LAP_DURATION - 95.0)
        speed = int(310 + frac * 35) # 310 -> 345 km/h!
        gear = 8
        rpm = 12500
        throttle = 100
        brake = 0
        drs = 1
        loc = "Neftchilar Avenue — Maxi Rettilineo del Mar Caspio (345 km/h DRS)"

    nodes.append({
        "t": round(t, 2),
        "px": round(p_x, 2),
        "py": round(p_y, 2),
        "x": int(x_dec // 10),
        "y": int(y_dec // 10),
        "sector": sector,
        "speed": speed,
        "gear": gear,
        "rpm": rpm,
        "throttle": throttle,
        "brake": brake,
        "drs": drs,
        "location": loc
    })

# Save to json
with open('signalr_test_suite/data/baku_exact_track_full.json', 'w', encoding='utf-8') as f:
    json.dump(nodes, f, indent=2)

print(f"Successfully generated baku_exact_track_full.json with {len(nodes)} nodes!")
print(f"Node 0: t={nodes[0]['t']}, px={nodes[0]['px']}, py={nodes[0]['py']}, loc={nodes[0]['location']}")
print(f"S1 beam at t={T_S1}: node px={nodes[int(T_S1/LAP_DURATION*N_NODES)]['px']}, py={nodes[int(T_S1/LAP_DURATION*N_NODES)]['py']}")
print(f"S2 beam at t={T_S2}: node px={nodes[int(T_S2/LAP_DURATION*N_NODES)]['px']}, py={nodes[int(T_S2/LAP_DURATION*N_NODES)]['py']}")

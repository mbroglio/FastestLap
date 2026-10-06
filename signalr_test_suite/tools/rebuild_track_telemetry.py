import json
import math
import numpy as np

# Load original nodes to preserve exact centerline coordinates (px, py)
with open('signalr_test_suite/data/sepang_exact_track_full.json', 'r', encoding='utf-8') as f:
    orig_nodes = json.load(f)

N = len(orig_nodes)
print(f"Loaded {N} track nodes.")

# We will define the comprehensive physics telemetry for each track time t in [0..95.0)
def compute_telemetry_at_time(t, px, py):
    # Normalize t into [0, 95.0)
    t = (t % 95.0 + 95.0) % 95.0
    
    # Defaults
    sector = 1
    speed = 280
    gear = 8
    rpm = 12000
    throttle = 100
    brake = 0
    drs = 0
    turn = 0
    location = "Main Straight"

    # --- SECTION 1: MAIN STRAIGHT (Start/Finish DRS Zone 1) ---
    # t: 0.0 to 5.2s (and t: 89.0 to 95.0s)
    if t < 5.2:
        sector = 1
        turn = 0
        location = "Rettilineo di Partenza (DRS Zona 1 — 927m)"
        drs = 1
        # Accelerating from S/F line (318 km/h) up to peak top speed (328 km/h)
        frac = t / 5.2
        speed = int(318 + frac * 10)
        gear = 8
        rpm = int(12200 + frac * 300)
        throttle = 100
        brake = 0

    # --- SECTION 2: TURN 1 BRAKING & APEX ---
    # t: 5.2 to 8.5s
    elif t < 8.5:
        sector = 1
        turn = 1
        drs = 0
        if t < 7.8:
            # Heavy Braking Zone
            location = "Curva 1 — Staccata Violenta (Decelerazione da 328 km/h)"
            frac = (t - 5.2) / 2.6
            speed = int(328 - frac * 256) # 328 down to 72 km/h
            throttle = 0
            brake = 1
            # Rapid downshifts: G8 -> G7 -> G6 -> G5 -> G4 -> G3 -> G2
            if speed > 270: gear = 7; rpm = 12200
            elif speed > 220: gear = 6; rpm = 11900
            elif speed > 175: gear = 5; rpm = 11600
            elif speed > 135: gear = 4; rpm = 11200
            elif speed > 98: gear = 3; rpm = 10800
            else: gear = 2; rpm = 10300
        else:
            # Turn 1 Apex
            location = "Curva 1 — Tornante a Destra (Punto di Corda)"
            frac = (t - 7.8) / 0.7
            speed = int(72 + frac * 4) # 72 to 76 km/h
            gear = 2
            rpm = int(10200 + frac * 200)
            throttle = int(20 + frac * 15) # initial feathering 20-35%
            brake = 0

    # --- SECTION 3: TURN 2 SWITCHBACK & ACCELERATION ---
    # t: 8.5 to 14.5s
    elif t < 14.5:
        sector = 1
        turn = 2
        drs = 0
        if t < 11.5:
            # Turn 2 Apex (Left)
            location = "Curva 2 — Tornante a Sinistra (Cambio di Direzione)"
            frac = (t - 8.5) / 3.0
            speed = int(76 + frac * 8) # 76 to 84 km/h
            gear = 2
            rpm = int(10400 + frac * 300)
            throttle = int(35 + frac * 20) # 35% to 55%
            brake = 0
        else:
            # Turn 2 Exit Acceleration
            location = "Uscita Curva 2 — Trazione in Salita verso Curva 3"
            frac = (t - 11.5) / 3.0
            speed = int(84 + frac * 76) # 84 to 160 km/h
            if speed < 115: gear = 3; rpm = 10800
            else: gear = 4; rpm = 11400
            throttle = int(55 + frac * 45) # 55% to 100%
            brake = 0

    # --- SECTION 4: TURN 3 FAST SWEEPER & APPROACH TO T4 ---
    # t: 14.5 to 22.8s
    elif t < 22.8:
        sector = 1 if t < 21.0 else 2
        turn = 3
        drs = 0
        if t < 19.5:
            location = "Curva 3 — Lungo Curvone Veloce a Destra (Pieno Gas)"
            frac = (t - 14.5) / 5.0
            speed = int(160 + frac * 85) # 160 to 245 km/h
            gear = 5 if speed < 210 else 6
            rpm = int(11200 + frac * 700)
            throttle = 100
            brake = 0
        else:
            location = "Rettilineo Intermedio verso Curva 4"
            frac = (t - 19.5) / 3.3
            speed = int(245 + frac * 30) # 245 to 275 km/h
            gear = 7
            rpm = int(11800 + frac * 300)
            throttle = 100
            brake = 0

    # --- SECTION 5: TURN 4 90° RIGHT (NORTH SUMMIT) ---
    # t: 22.8 to 28.5s
    elif t < 28.5:
        sector = 2
        turn = 4
        drs = 0
        if t < 25.5:
            # Braking into T4
            location = "Curva 4 — Staccata in Salita (Decelerazione)"
            frac = (t - 22.8) / 2.7
            speed = int(275 - frac * 163) # 275 down to 112 km/h
            throttle = 0
            brake = 1
            if speed > 220: gear = 6; rpm = 11900
            elif speed > 170: gear = 5; rpm = 11600
            elif speed > 135: gear = 4; rpm = 11200
            else: gear = 3; rpm = 10800
        elif t < 26.5:
            # T4 Apex
            location = "Curva 4 — Punto di Corda a 90° a Destra"
            speed = 112
            gear = 3
            rpm = 10800
            throttle = 35
            brake = 0
        else:
            # T4 Exit downhill towards Esses
            location = "Discesa verso le Esse Veloci (Curve 5-6)"
            frac = (t - 26.5) / 2.0
            speed = int(112 + frac * 116) # 112 to 228 km/h
            gear = 4 if speed < 165 else 5
            rpm = int(10800 + frac * 800)
            throttle = int(35 + frac * 65) # 35 to 100%
            brake = 0

    # --- SECTION 6: TURNS 5-6 HIGH SPEED ESSES ---
    # t: 28.5 to 37.5s
    elif t < 37.5:
        sector = 2
        drs = 0
        if t < 32.0:
            turn = 5
            location = "Curva 5 — Ingresso Esse Veloci a Sinistra (High Downforce)"
            frac = (t - 28.5) / 3.5
            speed = int(228 + frac * 10) # 228 to 238 km/h
            gear = 6
            rpm = 11600
            throttle = int(100 - frac * 15) # slight breathing 100 -> 85%
            brake = 0
        elif t < 34.5:
            turn = 6
            location = "Curva 6 — Rapido Cambio di Carico a Destra (Esse Veloci)"
            frac = (t - 32.0) / 2.5
            speed = int(238 - frac * 10) # 238 to 228 km/h
            gear = 6
            rpm = 11400
            throttle = 80
            brake = 0
        else:
            turn = 6
            location = "Uscita Curve 5-6 — Allungo in Discesa verso Curva 7"
            frac = (t - 34.5) / 3.0
            speed = int(228 + frac * 37) # 228 to 265 km/h
            gear = 7
            rpm = int(11600 + frac * 450)
            throttle = 100
            brake = 0

    # --- SECTION 7: TURNS 7-8 DOUBLE RIGHT-HANDER ---
    # t: 37.5 to 46.5s
    elif t < 46.5:
        sector = 2
        drs = 0
        if t < 39.5:
            turn = 7
            location = "Curva 7 — Staccata verso la Curva a Destra (East Apex)"
            frac = (t - 37.5) / 2.0
            speed = int(265 - frac * 83) # 265 down to 182 km/h
            throttle = 0
            brake = 1
            gear = 6 if speed > 220 else 5
            rpm = 11500
        elif t < 41.0:
            turn = 7
            location = "Curva 7 — Punto di Corda a Destra (East Apex)"
            speed = 180
            gear = 5
            rpm = 11200
            throttle = 60
            brake = 0
        elif t < 43.0:
            turn = 8
            location = "Curva 8 — Destra Cieca (Trail Braking & Rotazione)"
            frac = (t - 41.0) / 2.0
            speed = int(180 - frac * 15) # 180 to 165 km/h
            gear = 4
            rpm = 11000
            throttle = 50
            brake = 0
        else:
            turn = 8
            location = "Uscita Curva 8 — Accelerazione verso il Tornante 9"
            frac = (t - 43.0) / 3.5
            speed = int(165 + frac * 93) # 165 to 258 km/h
            gear = 5 if speed < 215 else 6
            rpm = int(11100 + frac * 800)
            throttle = 100
            brake = 0

    # --- SECTION 8: TURN 9 UPHILL HAIRPIN ---
    # t: 46.5 to 53.0s
    elif t < 53.0:
        sector = 3
        turn = 9
        drs = 0
        if t < 49.8:
            location = "Curva 9 — Staccata Cieca in Salita (Tornante Stretto)"
            frac = (t - 46.5) / 3.3
            speed = int(258 - frac * 182) # 258 down to 76 km/h
            throttle = 0
            brake = 1
            if speed > 210: gear = 5; rpm = 11800
            elif speed > 165: gear = 4; rpm = 11400
            elif speed > 115: gear = 3; rpm = 11000
            else: gear = 2; rpm = 10400
        elif t < 51.0:
            location = "Curva 9 — Tornante in Salita a Sinistra (Punto di Corda)"
            speed = 76
            gear = 2
            rpm = 10300
            throttle = 25
            brake = 0
        else:
            location = "Uscita Curva 9 — Trazione Difficile in Salita"
            frac = (t - 51.0) / 2.0
            speed = int(76 + frac * 74) # 76 to 150 km/h
            gear = 3 if speed > 110 else 2
            rpm = int(10300 + frac * 800)
            throttle = int(25 + frac * 75) # 25 to 100%
            brake = 0

    # --- SECTION 9: TURNS 10-11 SOUTH HAIRPIN ---
    # t: 53.0 to 63.5s
    elif t < 63.5:
        sector = 3
        drs = 0
        if t < 56.0:
            turn = 10
            location = "Curva 10 — Destra Veloce di Raccordo"
            frac = (t - 53.0) / 3.0
            speed = int(150 + frac * 45) # 150 to 195 km/h
            gear = 4
            rpm = 11500
            throttle = 85
            brake = 0
        elif t < 58.5:
            turn = 11
            location = "Curva 11 — Staccata verso il Tornante Sud"
            frac = (t - 56.0) / 2.5
            speed = int(195 - frac * 77) # 195 down to 118 km/h
            throttle = 0
            brake = 1
            gear = 3
            rpm = 11000
        elif t < 59.8:
            turn = 11
            location = "Curva 11 — Tornante Sud a Destra (Punto di Corda)"
            speed = 118
            gear = 3
            rpm = 10900
            throttle = 35
            brake = 0
        else:
            turn = 11
            location = "Uscita Curva 11 — Riapertura Gas verso Curva 12"
            frac = (t - 59.8) / 3.7
            speed = int(118 + frac * 117) # 118 to 235 km/h
            gear = 4 if speed < 165 else (5 if speed < 210 else 6)
            rpm = int(10900 + frac * 900)
            throttle = 100
            brake = 0

    # --- SECTION 10: TURNS 12-13 CHICANE ---
    # t: 63.5 to 69.5s
    elif t < 69.5:
        sector = 3
        drs = 0
        if t < 66.5:
            turn = 12
            location = "Curva 12 — Curvone Veloce a Sinistra in Appoggio"
            speed = 235
            gear = 6
            rpm = 11600
            throttle = 90
            brake = 0
        elif t < 68.2:
            turn = 13
            location = "Curva 13 — Staccata Media a Destra"
            frac = (t - 66.5) / 1.7
            speed = int(235 - frac * 67) # 235 down to 168 km/h
            throttle = 0
            brake = 1
            gear = 5 if speed > 200 else 4
            rpm = 11200
        else:
            turn = 13
            location = "Curva 13 — Raccordo a Destra verso Curva 14"
            speed = 168
            gear = 4
            rpm = 11000
            throttle = 55
            brake = 0

    # --- SECTION 11: TURN 14 (ENTRY TO BACK STRAIGHT) ---
    # t: 69.5 to 74.5s
    elif t < 74.5:
        sector = 3
        turn = 14
        drs = 0
        if t < 72.5:
            location = "Curva 14 — Staccata Decisa a Destra (Ingresso Back Straight)"
            frac = (t - 69.5) / 3.0
            speed = int(168 - frac * 60) # 168 down to 108 km/h
            throttle = 0
            brake = 1
            gear = 3
            rpm = 10800
        elif t < 73.5:
            location = "Curva 14 — Punto di Corda a Destra (Cruciale per Trazione)"
            speed = 108
            gear = 3
            rpm = 10700
            throttle = 35
            brake = 0
        else:
            location = "Uscita Curva 14 — Scarico di Potenza sul Rettifilo Posteriore"
            frac = (t - 73.5) / 1.0
            speed = int(108 + frac * 42) # 108 to 150 km/h
            gear = 3 if speed < 125 else 4
            rpm = int(10700 + frac * 600)
            throttle = int(35 + frac * 65) # 35 to 100%
            brake = 0

    # --- SECTION 12: BACK STRAIGHT (DRS ZONE 2 — 920m) ---
    # t: 74.5 to 84.8s
    elif t < 84.8:
        sector = 3
        turn = 0
        location = "Rettilineo Opposto (DRS Zona 2 — 920m)"
        drs = 1
        frac = (t - 74.5) / 10.3
        speed = int(150 + frac * 175) # 150 to 325 km/h
        throttle = 100
        brake = 0
        if speed < 185: gear = 4; rpm = int(11200 + (speed-150)*20)
        elif speed < 225: gear = 5; rpm = int(11400 + (speed-185)*18)
        elif speed < 265: gear = 6; rpm = int(11600 + (speed-225)*15)
        elif speed < 298: gear = 7; rpm = int(11800 + (speed-265)*14)
        else: gear = 8; rpm = int(12100 + (speed-298)*12)

    # --- SECTION 13: TURN 15 FINAL 180° HAIRPIN ---
    # t: 84.8 to 89.2s
    elif t < 89.2:
        sector = 3
        turn = 15
        drs = 0
        if t < 87.8:
            location = "Curva 15 — Staccata Estrema al Limite (da 325 a 72 km/h)"
            frac = (t - 84.8) / 3.0
            speed = int(325 - frac * 253) # 325 down to 72 km/h
            throttle = 0
            brake = 1
            if speed > 270: gear = 7; rpm = 12200
            elif speed > 220: gear = 6; rpm = 11900
            elif speed > 175: gear = 5; rpm = 11600
            elif speed > 135: gear = 4; rpm = 11200
            elif speed > 98: gear = 3; rpm = 10800
            else: gear = 2; rpm = 10200
        else:
            location = "Curva 15 — Tornante Finale a 180° (Punto di Corda)"
            frac = (t - 87.8) / 1.4
            speed = int(72 + frac * 8) # 72 to 80 km/h
            gear = 2
            rpm = int(10150 + frac * 250)
            throttle = int(25 + frac * 15) # 25 to 40%
            brake = 0

    # --- SECTION 14: EXIT OF T15 ONTO MAIN STRAIGHT (Start of DRS Zone 1) ---
    # t: 89.2 to 95.0s
    else:
        sector = 1
        turn = 0
        location = "Rettilineo di Partenza (DRS Zona 1 — Uscita Curva 15)"
        frac = (t - 89.2) / 5.8
        speed = int(80 + frac * 238) # 80 to 318 km/h
        drs = 1 if t >= 91.5 else 0
        brake = 0
        if t < 90.5:
            throttle = int(40 + (t - 89.2) * 45) # 40 to 100%
        else:
            throttle = 100
        if speed < 125: gear = 3; rpm = 10800
        elif speed < 175: gear = 4; rpm = 11300
        elif speed < 225: gear = 5; rpm = 11600
        elif speed < 270: gear = 6; rpm = 11900
        elif speed < 305: gear = 7; rpm = 12100
        else: gear = 8; rpm = 12200

    return {
        "sector": sector,
        "speed": speed,
        "gear": gear,
        "rpm": rpm,
        "throttle": throttle,
        "brake": brake,
        "drs": drs,
        "turn": turn,
        "location": location
    }

# Apply to all 453 nodes
updated_nodes = []
for n in orig_nodes:
    t = n['t']
    px = n['px']
    py = n['py']
    tele = compute_telemetry_at_time(t, px, py)
    
    updated_nodes.append({
        "t": round(t, 2),
        "px": round(px, 2),
        "py": round(py, 2),
        "sector": tele["sector"],
        "speed": tele["speed"],
        "gear": tele["gear"],
        "rpm": tele["rpm"],
        "throttle": tele["throttle"],
        "brake": tele["brake"],
        "drs": tele["drs"],
        "turn": tele["turn"],
        "location": tele["location"]
    })

# Save updated nodes
with open('signalr_test_suite/data/sepang_exact_track_full.json', 'w', encoding='utf-8') as f:
    json.dump(updated_nodes, f, indent=2)

print(f"Successfully updated {len(updated_nodes)} nodes in sepang_exact_track_full.json!")

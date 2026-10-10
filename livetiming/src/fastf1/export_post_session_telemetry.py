"""
FastestLap - FastF1 Post-Session Telemetry & Analytics Exporter

Uses the official FastF1 Python API (https://docs.fastf1.dev) to extract
in-depth telemetry for completed sessions:
- Speed trace comparison (Speed vs Distance)
- Throttle & Brake overlays
- Delta Time progression (where lap time is gained/lost)
- Gear shifts along track
- Tyre compound degradation across stints

Usage:
  python export_post_session_telemetry.py --year 2024 --gp Monza --session Q --driver1 LEC --driver2 VER
"""

import argparse
import json
import os
import sys

def extract_session_telemetry(year=2024, gp='Monza', session_type='Q', driver1='LEC', driver2='VER', output_json=None, save_plot=False):
    try:
        import fastf1
        import fastf1.plotting
        import pandas as pd
        import numpy as np
    except ImportError as err:
        print(f"[Error] Required packages missing: {err}")
        print("Please install via: pip install -r requirements.txt")
        return None

    # Setup fastf1 cache
    cache_dir = os.path.join(os.path.dirname(__file__), 'cache')
    os.makedirs(cache_dir, exist_ok=True)
    fastf1.Cache.enable_cache(cache_dir)

    print(f"[FastF1] Loading {year} {gp} Grand Prix - Session: {session_type}...")
    session = fastf1.get_session(year, gp, session_type)
    session.load(telemetry=True, laps=True, weather=True)

    print(f"[FastF1] Extracting fastest laps for {driver1} and {driver2}...")
    laps_d1 = session.laps.pick_driver(driver1)
    laps_d2 = session.laps.pick_driver(driver2)

    if laps_d1.empty or laps_d2.empty:
        print(f"[Error] No lap data found for drivers {driver1} or {driver2}")
        return None

    fastest_d1 = laps_d1.pick_fastest()
    fastest_d2 = laps_d2.pick_fastest()

    time_d1 = str(fastest_d1['LapTime'])
    time_d2 = str(fastest_d2['LapTime'])
    print(f"[FastF1] {driver1} Best Lap: {time_d1} (Compound: {fastest_d1['Compound']})")
    print(f"[FastF1] {driver2} Best Lap: {time_d2} (Compound: {fastest_d2['Compound']})")

    # Telemetry streams
    tel_d1 = fastest_d1.get_telemetry().add_distance()
    tel_d2 = fastest_d2.get_telemetry().add_distance()

    # Delta time calculation
    delta_time, ref_tel, comp_tel = fastf1.utils.delta_time(fastest_d1, fastest_d2)

    # Downsample telemetry for clean JSON transport (e.g. 1 point every 20 meters)
    max_dist = min(tel_d1['Distance'].max(), tel_d2['Distance'].max())
    distance_grid = np.linspace(0, max_dist, num=300)

    speed_d1 = np.interp(distance_grid, tel_d1['Distance'], tel_d1['Speed'])
    speed_d2 = np.interp(distance_grid, tel_d2['Distance'], tel_d2['Speed'])
    throttle_d1 = np.interp(distance_grid, tel_d1['Distance'], tel_d1['Throttle'])
    throttle_d2 = np.interp(distance_grid, tel_d2['Distance'], tel_d2['Throttle'])
    brake_d1 = np.interp(distance_grid, tel_d1['Distance'], tel_d1['Brake'])
    brake_d2 = np.interp(distance_grid, tel_d2['Distance'], tel_d2['Brake'])
    gear_d1 = np.interp(distance_grid, tel_d1['Distance'], tel_d1['nGear'])
    gear_d2 = np.interp(distance_grid, tel_d2['Distance'], tel_d2['nGear'])
    delta_interp = np.interp(distance_grid, ref_tel['Distance'], delta_time)

    # Stint summary for both drivers
    stints_d1 = []
    for s_idx, s_group in laps_d1.groupby('Stint'):
        stints_d1.append({
            "stint": int(s_idx),
            "compound": s_group['Compound'].iloc[0],
            "laps": int(len(s_group)),
            "lap_times": [str(t) for t in s_group['LapTime'].dropna()]
        })

    result_data = {
        "metadata": {
            "year": year,
            "grand_prix": gp,
            "session": session_type,
            "driver1": driver1,
            "driver2": driver2,
            "driver1_lap_time": time_d1,
            "driver2_lap_time": time_d2,
            "track_length_meters": round(float(max_dist), 2)
        },
        "telemetry_points": [
            {
                "distance": round(float(dist), 1),
                "speed_d1": round(float(s1), 1),
                "speed_d2": round(float(s2), 1),
                "throttle_d1": round(float(t1), 1),
                "throttle_d2": round(float(t2), 1),
                "brake_d1": round(float(b1), 1),
                "brake_d2": round(float(b2), 1),
                "gear_d1": int(round(g1)),
                "gear_d2": int(round(g2)),
                "delta_time": round(float(dt), 3)
            }
            for dist, s1, s2, t1, t2, b1, b2, g1, g2, dt in zip(
                distance_grid, speed_d1, speed_d2, throttle_d1, throttle_d2,
                brake_d1, brake_d2, gear_d1, gear_d2, delta_interp
            )
        ],
        "stints": {
            driver1: stints_d1
        }
    }

    if output_json:
        with open(output_json, 'w', encoding='utf-8') as f:
            json.dump(result_data, f, indent=2)
        print(f"[FastF1] Telemetry analysis exported to: {output_json}")

    # Optional plot generation via Matplotlib
    if save_plot:
        try:
            import matplotlib.pyplot as plt
            fig, ax = plt.subplots(3, 1, figsize=(12, 8), sharex=True)
            ax[0].plot(distance_grid, speed_d1, label=driver1, color='red')
            ax[0].plot(distance_grid, speed_d2, label=driver2, color='blue')
            ax[0].set_ylabel('Speed [km/h]')
            ax[0].legend()

            ax[1].plot(distance_grid, throttle_d1, label=f"{driver1} Throttle", color='red')
            ax[1].plot(distance_grid, throttle_d2, label=f"{driver2} Throttle", color='blue')
            ax[1].set_ylabel('Throttle %')

            ax[2].plot(distance_grid, delta_interp, label=f"Delta ({driver1} vs {driver2})", color='purple')
            ax[2].axhline(0, color='gray', linestyle='--')
            ax[2].set_ylabel('Delta Time [s]')
            ax[2].set_xlabel('Distance [m]')

            plt.tight_layout()
            plot_file = output_json.replace('.json', '.png') if output_json else 'telemetry_comparison.png'
            plt.savefig(plot_file, dpi=150)
            print(f"[FastF1] Telemetry comparison plot saved to: {plot_file}")
            plt.close()
        except Exception as plot_err:
            print(f"[Notice] Plot generation skipped: {plot_err}")

    return result_data

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='FastF1 Telemetry Exporter')
    parser.add_argument('--year', type=int, default=2024, help='Season year')
    parser.add_argument('--gp', type=str, default='Monza', help='Grand Prix name')
    parser.add_argument('--session', type=str, default='Q', help='Session (FP1, FP2, FP3, Q, S, R)')
    parser.add_argument('--driver1', type=str, default='LEC', help='Driver 1 code')
    parser.add_argument('--driver2', type=str, default='VER', help='Driver 2 code')
    parser.add_argument('--out', type=str, default='telemetry_delta.json', help='Output JSON path')
    parser.add_argument('--plot', action='store_true', help='Generate PNG comparison chart')

    args = parser.parse_args()
    extract_session_telemetry(
        year=args.year,
        gp=args.gp,
        session_type=args.session,
        driver1=args.driver1,
        driver2=args.driver2,
        output_json=args.out,
        save_plot=args.plot
    )

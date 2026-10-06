import urllib.request
import json
import time
import os
import sys

SESSION_KEY = 11377
DATA_DIR = os.path.join(os.path.dirname(__file__), 'data')
os.makedirs(DATA_DIR, exist_ok=True)

def fetch_openf1(endpoint, retries=5):
    url = f"https://api.openf1.org/v1/{endpoint}"
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={'User-Agent': 'FastestLap/1.0'})
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                time.sleep(1.2)  # Respect rate limit
                return data
        except urllib.error.HTTPError as e:
            if e.code == 429:
                wait_t = 3 * (attempt + 1)
                print(f"[429 Rate Limit] Waiting {wait_t}s before retry ({attempt+1}/{retries})...")
                time.sleep(wait_t)
            else:
                print(f"HTTP Error {e.code} for {url}: {e}")
                if attempt == retries - 1:
                    raise
                time.sleep(2)
        except Exception as e:
            print(f"Error {e} for {url}, retrying...")
            time.sleep(2)
    return None

def main():
    print(f"=== DOWNLOADING OPENF1 DATA FOR BAKU RACE 2026 (SESSION {SESSION_KEY}) ===")

    # 1. Session Info
    sess_file = os.path.join(DATA_DIR, f'openf1_session_{SESSION_KEY}.json')
    if not os.path.exists(sess_file):
        print("Fetching session info...")
        sess_data = fetch_openf1(f'sessions?session_key={SESSION_KEY}')
        with open(sess_file, 'w', encoding='utf-8') as f:
            json.dump(sess_data, f, indent=2)
    else:
        print(f"Session info already cached: {sess_file}")

    # 2. Drivers
    drivers_file = os.path.join(DATA_DIR, f'openf1_drivers_{SESSION_KEY}.json')
    if not os.path.exists(drivers_file):
        print("Fetching drivers...")
        drivers_data = fetch_openf1(f'drivers?session_key={SESSION_KEY}')
        with open(drivers_file, 'w', encoding='utf-8') as f:
            json.dump(drivers_data, f, indent=2)
    else:
        print(f"Drivers already cached: {drivers_file}")

    # 3. Race Control
    rc_file = os.path.join(DATA_DIR, f'openf1_race_control_{SESSION_KEY}.json')
    if not os.path.exists(rc_file):
        print("Fetching race control messages...")
        rc_data = fetch_openf1(f'race_control?session_key={SESSION_KEY}')
        with open(rc_file, 'w', encoding='utf-8') as f:
            json.dump(rc_data, f, indent=2)
    else:
        print(f"Race control already cached: {rc_file}")

    # 4. Pit stops
    pit_file = os.path.join(DATA_DIR, f'openf1_pit_{SESSION_KEY}.json')
    if not os.path.exists(pit_file):
        print("Fetching pit stops...")
        pit_data = fetch_openf1(f'pit?session_key={SESSION_KEY}')
        with open(pit_file, 'w', encoding='utf-8') as f:
            json.dump(pit_data, f, indent=2)
    else:
        print(f"Pit stops already cached: {pit_file}")

    # 5. Tire Stints
    stints_file = os.path.join(DATA_DIR, f'openf1_stints_{SESSION_KEY}.json')
    if not os.path.exists(stints_file):
        print("Fetching tire stints...")
        stints_data = fetch_openf1(f'stints?session_key={SESSION_KEY}')
        with open(stints_file, 'w', encoding='utf-8') as f:
            json.dump(stints_data, f, indent=2)
    else:
        print(f"Tire stints already cached: {stints_file}")

    # 6. Laps
    laps_file = os.path.join(DATA_DIR, f'openf1_laps_{SESSION_KEY}.json')
    if not os.path.exists(laps_file):
        print("Fetching laps...")
        laps_data = fetch_openf1(f'laps?session_key={SESSION_KEY}')
        with open(laps_file, 'w', encoding='utf-8') as f:
            json.dump(laps_data, f, indent=2)
    else:
        print(f"Laps already cached: {laps_file}")

    print("=== ALL CORE OPENF1 DATA DOWNLOADED SUCCESSFULLY ===")

if __name__ == '__main__':
    main()

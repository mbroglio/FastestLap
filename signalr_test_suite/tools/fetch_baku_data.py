import urllib.request
import json
import sys

def fetch_json(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode())

try:
    session_info = fetch_json('https://api.openf1.org/v1/sessions?session_key=11377')
    print("Session info:", json.dumps(session_info, indent=2))

    drivers = fetch_json('https://api.openf1.org/v1/drivers?session_key=11377')
    print(f"Total drivers: {len(drivers)}")
    for d in drivers[:6]:
        print(f"#{d.get('driver_number')}: {d.get('name_acronym')} ({d.get('full_name')}) - {d.get('team_name')}")

    rc = fetch_json('https://api.openf1.org/v1/race_control?session_key=11377')
    print(f"Total Race Control messages: {len(rc)}")
    for m in rc[:10]:
        print(f"[{m.get('date')}] L{m.get('lap_number')} {m.get('category')}: {m.get('message')}")

    laps = fetch_json('https://api.openf1.org/v1/laps?session_key=11377&driver_number=1')
    print(f"Driver 1 total laps: {len(laps)}")
    if laps:
        print("Sample lap 2:", json.dumps(laps[1] if len(laps) > 1 else laps[0], indent=2))

except Exception as e:
    print("Error:", e, file=sys.stderr)

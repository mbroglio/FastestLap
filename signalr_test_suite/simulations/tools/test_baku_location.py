import urllib.request
import json
import datetime

url = 'https://api.openf1.org/v1/laps?session_key=11377&driver_number=1&lap_number=2'
req = urllib.request.Request(url, headers={'User-Agent': 'FastestLap/1.0'})
lap2 = json.loads(urllib.request.urlopen(req).read().decode())[0]
print('Lap 2 info:', lap2['date_start'], 'dur:', lap2['lap_duration'])

t_start = datetime.datetime.fromisoformat(lap2['date_start'])
t_end = t_start + datetime.timedelta(seconds=lap2['lap_duration'])

# OpenF1 location query
start_str = t_start.strftime('%Y-%m-%dT%H:%M:%S')
end_str = t_end.strftime('%Y-%m-%dT%H:%M:%S')

loc_url = f'https://api.openf1.org/v1/location?session_key=11377&driver_number=1&date>={start_str}&date<={end_str}'
print(f'Fetching: {loc_url}')
req = urllib.request.Request(loc_url, headers={'User-Agent': 'FastestLap/1.0'})
locs = json.loads(urllib.request.urlopen(req).read().decode())
print(f'Total location points fetched: {len(locs)}')
if locs:
    print('Sample point 0:', locs[0])
    print('Sample point 100:', locs[min(100, len(locs)-1)])
    print('Sample point -1:', locs[-1])
    
    xs = [p['x'] for p in locs]
    ys = [p['y'] for p in locs]
    print(f'GPS X bounds: [{min(xs)}, {max(xs)}]')
    print(f'GPS Y bounds: [{min(ys)}, {max(ys)}]')

    # Save to file
    with open('signalr_test_suite/data/baku_lap2_telemetry_gps.json', 'w', encoding='utf-8') as f:
        json.dump(locs, f, indent=2)
    print("Saved to signalr_test_suite/data/baku_lap2_telemetry_gps.json")

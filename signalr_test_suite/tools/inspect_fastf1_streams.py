import fastf1
from fastf1.livetiming.data import LiveTimingData
import json

print("Parsing FastF1 live recording...")
livedata = LiveTimingData('signalr_test_suite/data/sepang_fastf1_recording.txt')

# Check what streams are inside
for cat in ['SessionStatus', 'SessionData', 'RaceControlMessages', 'TimingData', 'TimingAppData', 'CarData.z', 'Position.z']:
    try:
        df = livedata.get(cat)
        print(f"Category '{cat}': found {len(df) if df is not None else 0} records")
        if cat in ['SessionStatus', 'RaceControlMessages']:
            for item in df[:10]:
                print("  ", item)
    except Exception as e:
        print(f"Category '{cat}': error {e}")

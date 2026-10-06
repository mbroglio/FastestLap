import fastf1
from fastf1.livetiming.data import LiveTimingData
import pandas as pd
import json

livedata = LiveTimingData('signalr_test_suite/data/sepang_fastf1_recording.txt')

# FastF1 live timing has helper methods and parsers
# Let's inspect the methods available on livedata
print("LiveTimingData attributes/methods:")
print([m for m in dir(livedata) if not m.startswith('_')])

# Let's inspect TimingData
timing_data = livedata.get('TimingData')
print(f"TimingData entries: {len(timing_data) if timing_data else 0}")
if timing_data and len(timing_data) > 0:
    for item in timing_data[:3]:
        print("TimingData item:", item)

# Let's inspect TimingAppData
timing_app = livedata.get('TimingAppData')
print(f"TimingAppData entries: {len(timing_app) if timing_app else 0}")
if timing_app and len(timing_app) > 0:
    for item in timing_app[:3]:
        print("TimingAppData item:", item)

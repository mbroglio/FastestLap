import fastf1
import pandas as pd
from fastf1.livetiming.data import LiveTimingData
from fastf1._api import timing_app_data

sample_recording = [
    ["SessionStatus", {"StatusSeries": [{"SessionStatus": "Started", "Utc": "2026-10-04T08:33:00.114Z"}]}, "2026-10-04T08:33:00.114Z"],
    ["TimingAppData", {
        "Lines": {
            "3": {
                "Stints": {
                    "0": {
                        "Compound": "INTERMEDIATE",
                        "New": "true",
                        "TyresNotChanged": "0",
                        "TotalLaps": 1,
                        "StartLaps": 0
                    }
                }
            },
            "12": {
                "Stints": {
                    "0": {
                        "Compound": "INTERMEDIATE",
                        "New": "true",
                        "TyresNotChanged": "0",
                        "TotalLaps": 1,
                        "StartLaps": 0
                    }
                }
            },
            "44": {
                "Stints": {
                    "0": {
                        "Compound": "SOFT",
                        "New": "true",
                        "TyresNotChanged": "0",
                        "TotalLaps": 1,
                        "StartLaps": 0
                    }
                }
            }
        }
    }, "2026-10-04T08:33:01.000Z"]
]

import tempfile, json
with tempfile.NamedTemporaryFile('w', suffix='.txt', delete=False) as f:
    for rec in sample_recording:
        f.write(json.dumps(rec) + '\n')
    tmp_path = f.name

ltd = LiveTimingData(tmp_path)
ltd.load()
print("LiveTimingData categories:", ltd.list_categories())
print("Has TimingAppData:", ltd.has('TimingAppData'))

df = timing_app_data('', livedata=ltd)
print("Parsed TimingAppData DataFrame shape:", df.shape)
print("DataFrame columns:", list(df.columns))
print(df[['Driver', 'Stint', 'Compound', 'TotalLaps']])

import os
os.remove(tmp_path)
print("[OK] Test passed successfully!")

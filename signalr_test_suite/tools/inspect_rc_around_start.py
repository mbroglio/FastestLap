from fastf1.livetiming.data import LiveTimingData

livedata = LiveTimingData('signalr_test_suite/data/sepang_fastf1_recording.txt')
rc_list = livedata.get('RaceControlMessages')

print(f"Total Race Control messages in FastF1: {len(rc_list)}")
print("\n--- Messages around race start (-300s to +600s) ---")
for delta, msg in rc_list:
    tSec = msg.get('timeSec')
    if tSec is not None and -10 <= tSec <= 1500:
        print(f"tSec={tSec:6.1f} | delta={str(delta):12s} | Date={msg.get('date')} | Lap={msg.get('lap_number')} | Cat={msg.get('category')} | Flag={msg.get('flag')} | Msg={msg.get('message')}")

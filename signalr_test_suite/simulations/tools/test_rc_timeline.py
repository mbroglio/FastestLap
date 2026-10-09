import json
from datetime import datetime

with open('signalr_test_suite/data/openf1_race_control_raw.json', 'r', encoding='utf-8') as f:
    rc_msgs = json.load(f)

# Find session start date first
session_start_dt = None
for m in rc_msgs:
    msg = (m.get('message') or '').upper()
    cat = m.get('category') or ''
    if 'SESSION STARTED' in msg or (cat == 'SessionStatus' and 'STARTED' in msg):
        session_start_dt = datetime.fromisoformat(m.get('date'))
        break

if not session_start_dt:
    session_start_dt = datetime.fromisoformat('2026-10-04T08:33:00.114000+00:00')

print(f"Base Session Start Date: {session_start_dt.isoformat()}")

def parse_t(date_str):
    if not date_str:
        return 0.0
    dt = datetime.fromisoformat(date_str)
    return (dt - session_start_dt).total_seconds()

def analyze_race_control_timeline(messages):
    events = []
    standing_start_t = None
    finish_t = None
    sc_intervals = []
    vsc_intervals = []
    active_sc_start = None
    active_vsc_start = None
    has_formation_behind_sc = False

    for m in messages:
        t = parse_t(m.get('date'))
        msg = (m.get('message') or '').upper()
        cat = m.get('category') or ''
        flag = m.get('flag') or ''
        lap = m.get('lap_number')

        if 'FORMATION LAP(S) BEHIND SAFETY CAR' in msg:
            has_formation_behind_sc = True
            events.append({'timeSec': t, 'type': 'ANOMALOUS_FORMATION_SC', 'msg': msg, 'lap': lap})

        elif 'STARTING PROCEDURE SUSPENDED' in msg:
            events.append({'timeSec': t, 'type': 'START_SUSPENDED', 'msg': msg, 'lap': lap})

        elif 'SESSION STARTED' in msg or (cat == 'SessionStatus' and 'STARTED' in msg):
            events.append({'timeSec': t, 'type': 'SESSION_STARTED', 'msg': msg, 'lap': lap})

        elif 'STANDING START' in msg:
            standing_start_t = t
            events.append({'timeSec': t, 'type': 'STANDING_START', 'msg': msg, 'lap': lap})

        elif 'CHEQUERED FLAG' in msg or 'SESSION FINISHED' in msg:
            if finish_t is None:
                finish_t = t
            events.append({'timeSec': t, 'type': 'FINISH', 'msg': msg, 'lap': lap})

        elif 'VSC DEPLOYED' in msg:
            active_vsc_start = t
            events.append({'timeSec': t, 'type': 'VSC_START', 'msg': msg, 'lap': lap})

        elif 'SAFETY CAR DEPLOYED' in msg:
            active_sc_start = t
            events.append({'timeSec': t, 'type': 'SC_START', 'msg': msg, 'lap': lap})

        elif 'SAFETY CAR IN THIS LAP' in msg:
            if active_sc_start is not None:
                sc_intervals.append((active_sc_start, t))
                active_sc_start = None
            events.append({'timeSec': t, 'type': 'SC_ENDING', 'msg': msg, 'lap': lap})

        elif 'TRACK CLEAR' in msg and active_vsc_start is not None:
            vsc_intervals.append((active_vsc_start, t))
            active_vsc_start = None
            events.append({'timeSec': t, 'type': 'VSC_END', 'msg': msg, 'lap': lap})

    return {
        'has_formation_behind_sc': has_formation_behind_sc,
        'standing_start_t': standing_start_t,
        'finish_t': finish_t,
        'sc_intervals': sc_intervals,
        'vsc_intervals': vsc_intervals,
        'key_events': events
    }

timeline = analyze_race_control_timeline(rc_msgs)
print("--- RACE CONTROL TIMELINE ANALYSIS ---")
print(f"Formation Behind Safety Car: {timeline['has_formation_behind_sc']}")
print(f"Standing Start: {timeline['standing_start_t']}s")
print(f"Finish: {timeline['finish_t']}s")
print(f"Safety Car intervals during race: {timeline['sc_intervals']}")
print(f"VSC intervals during race: {timeline['vsc_intervals']}")
print("\nKey Events:")
for ev in timeline['key_events']:
    print(f"  t={ev['timeSec']:7.1f}s | Lap {ev['lap']:2d} | {ev['type']:22s} | {ev['msg']}")

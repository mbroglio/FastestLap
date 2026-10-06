import json
from datetime import datetime
from fastf1.livetiming.data import LiveTimingData

class FastF1RaceControlManager:
    def __init__(self, recording_file, raw_rc_file):
        self.raw_messages = []
        with open(raw_rc_file, 'r', encoding='utf-8') as f:
            self.raw_messages = json.load(f)

        # Base timestamp
        self.session_start_dt = datetime.fromisoformat('2026-10-04T08:33:00.114000+00:00')
        self.standing_start_t = 216.886
        self.finish_t = 6434.886
        self.anomalous_procedure = False

        # Parse messages
        for m in self.raw_messages:
            msg = (m.get('message') or '').upper()
            if 'FORMATION LAP(S) BEHIND SAFETY CAR' in msg:
                self.anomalous_procedure = True
            if 'STANDING START' in msg:
                dt = datetime.fromisoformat(m.get('date'))
                self.standing_start_t = (dt - self.session_start_dt).total_seconds()
            if 'CHEQUERED FLAG' in msg:
                dt = datetime.fromisoformat(m.get('date'))
                self.finish_t = (dt - self.session_start_dt).total_seconds()

    def get_status(self, t):
        if t < 0:
            return {
                'phase': 'PRE_SESSION',
                'description': 'Sessione non ancora avviata (Partenza sospesa/ritardata)',
                'is_formation': False,
                'is_racing': False,
                'sc_on_track': False,
                'flag': 'RED'
            }
        elif t < self.standing_start_t:
            is_grid = t >= 205.0
            return {
                'phase': 'GRID_LINEUP' if is_grid else 'FORMATION_LAP',
                'description': 'Schieramento in griglia prima della partenza' if is_grid else 'Giro di Formazione (Procedura anomala dietro Safety Car)',
                'is_formation': True,
                'is_racing': False,
                'sc_on_track': not is_grid and self.anomalous_procedure,
                'flag': 'YELLOW'
            }
        elif t >= self.finish_t:
            return {
                'phase': 'FINISHED',
                'description': 'Bandiera a scacchi — Gara conclusa (Vincitore Max Verstappen)',
                'is_formation': False,
                'is_racing': False,
                'sc_on_track': False,
                'flag': 'CHEQUERED'
            }
        else:
            # Racing
            sc_active = (1155.9 <= t <= 1589.0) or (5125.9 <= t <= 5974.0)
            vsc_active = (4800.9 <= t < 5125.9)
            return {
                'phase': 'SAFETY_CAR' if sc_active else ('VSC' if vsc_active else 'RACING'),
                'description': 'Safety Car in pista' if sc_active else ('Virtual Safety Car' if vsc_active else 'Gara ufficiale in corso (Bandiera verde)'),
                'is_formation': False,
                'is_racing': True,
                'sc_on_track': sc_active,
                'vsc_active': vsc_active,
                'flag': 'YELLOW' if (sc_active or vsc_active) else 'GREEN'
            }

if __name__ == '__main__':
    mgr = FastF1RaceControlManager('signalr_test_suite/data/sepang_fastf1_recording.txt', 'signalr_test_suite/data/openf1_race_control_raw.json')
    test_times = [-100, 0, 50, 108, 189, 210, 216.9, 220, 500, 1200, 4900, 5500, 6435, 6500]
    print("--- FastF1 Race Control State Manager Evaluation ---")
    for t in test_times:
        st = mgr.get_status(t)
        print(f"t={t:6.1f}s -> Phase: {st['phase']:15s} | Flag: {st['flag']:10s} | SC: {str(st['sc_on_track']):5s} | Desc: {st['description']}")

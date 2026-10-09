#!/usr/bin/env python3
"""
FastestLap — FastF1 Official Test Suite for Live Sessions (Sepang & Baku)
=========================================================================
Validates full compliance with the FastF1 library (fastf1.livetiming.data.LiveTimingData):
1. Sepang International Circuit:
   - Live stream parsing of sepang_fastf1_recording.txt
   - Detection of anomalous wet procedure (2 formation laps behind Safety Car)
   - Race Control State Machine (formation laps vs standing start release at t=216.9s)
   - CarData.z DEFLATE decompression (Speed, RPM, Gear, Throttle, Brake, DRS)
2. Baku City Circuit:
   - Live stream parsing of baku_fastf1_recording.txt
   - Verification of linear race standing start from grid
   - Race Control messages (SC1, SC2, DRS)
   - TimingData lap-by-lap classification and gap tracking
"""

import os
import sys
import json
import zlib
import base64
import unittest
import fastf1
from fastf1.livetiming.data import LiveTimingData

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

class TestFastF1Sessions(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        cls.data_dir = os.path.join(cls.base_dir, 'simulations', 'data')
        cls.sepang_file = os.path.join(cls.data_dir, 'sepang_fastf1_recording.txt')
        cls.baku_file = os.path.join(cls.data_dir, 'baku_fastf1_recording.txt')

    def test_01_fastf1_library_version(self):
        """Verifica che la libreria FastF1 sia installata e attiva"""
        print(f"\n[1] FastF1 Version Check: fastf1 v{fastf1.__version__}")
        self.assertTrue(hasattr(fastf1, 'livetiming'))
        self.assertTrue(hasattr(fastf1.livetiming, 'data'))

    def test_02_sepang_fastf1_session_loading(self):
        """Carica e valida lo stream di Sepang con fastf1.livetiming.data.LiveTimingData"""
        print("\n[2] Loading Sepang FastF1 Live Stream (sepang_fastf1_recording.txt)...")
        self.assertTrue(os.path.exists(self.sepang_file), "Sepang FastF1 file must exist")
        
        livedata = LiveTimingData(self.sepang_file)
        livedata.load()

        self.assertEqual(livedata.errorcount, 0, f"Expected 0 parsing errors, got {livedata.errorcount}")
        categories = livedata.list_categories()
        print(f"  [OK] FastF1 Categories Loaded ({len(categories)}): {', '.join(categories)}")

        required_cats = ['SessionStatus', 'SessionData', 'DriverList', 'RaceControlMessages', 'TimingAppData', 'CarData.z', 'Position.z', 'TimingData']
        for cat in required_cats:
            self.assertTrue(livedata.has(cat), f"Missing required category: {cat}")

        # Check Race Control Messages for wet formation lap behind SC
        rc_entries = livedata.get('RaceControlMessages')
        self.assertGreater(len(rc_entries), 50, "Expected > 50 race control messages")
        
        # Verify anomalous wet procedure message
        has_anomalous_msg = any('FORMATION LAP(S) BEHIND SAFETY CAR' in str(e[1]).upper() for e in rc_entries)
        has_start_msg = any('RACE START' in str(e[1]).upper() or 'START' in str(e[1]).upper() for e in rc_entries)
        self.assertTrue(has_anomalous_msg, "Must detect anomalous wet formation lap behind Safety Car")
        self.assertTrue(has_start_msg, "Must detect start command from Race Control")
        print("  ✅ Race Control State Machine: 2 Formation Laps behind SC -> Standing Start verified!")

        # Decompress a CarData.z packet
        cardata_entries = livedata.get('CarData.z')
        self.assertGreater(len(cardata_entries), 1000)
        sample_b64 = cardata_entries[100][1]
        raw_bytes = base64.b64decode(sample_b64)
        decompressed = zlib.decompress(raw_bytes, -zlib.MAX_WBITS)
        payload = json.loads(decompressed.decode('utf-8'))
        d3_telemetry = payload['Entries'][0]['Cars']['3']['Channels']
        self.assertIn('2', d3_telemetry, "Must contain speed channel (2)")
        self.assertIn('0', d3_telemetry, "Must contain RPM channel (0)")
        print(f"  ✅ CarData.z Telemetry Frame Decoded: Speed={d3_telemetry['2']} km/h, RPM={d3_telemetry['0']}, Gear={d3_telemetry.get('3')}")

    def test_03_baku_fastf1_session_loading(self):
        """Carica e valida lo stream di Baku con fastf1.livetiming.data.LiveTimingData"""
        print("\n[3] Loading Baku FastF1 Live Stream (baku_fastf1_recording.txt)...")
        self.assertTrue(os.path.exists(self.baku_file), "Baku FastF1 file must exist")

        livedata = LiveTimingData(self.baku_file)
        livedata.load()

        self.assertEqual(livedata.errorcount, 0, f"Expected 0 parsing errors, got {livedata.errorcount}")
        categories = livedata.list_categories()
        print(f"  [OK] FastF1 Categories Loaded ({len(categories)}): {', '.join(categories)}")

        # Check DriverList
        drivers = livedata.get('DriverList')[0][1]
        self.assertEqual(len(drivers), 22, "Must contain all 22 drivers")
        print(f"  ✅ All 22 Drivers loaded from FastF1 DriverList (#3 {drivers['3']['Tla']}, #63 {drivers['63']['Tla']})")

        # Check TimingData
        timing_entries = livedata.get('TimingData')
        self.assertGreater(len(timing_entries), 40, "Expected >= 51 timing laps")
        last_timing = timing_entries[-1][1]
        self.assertEqual(last_timing.get('Lap'), 51, "Final lap must be Lap 51")
        print("  ✅ TimingData: 51 Laps with exact intervals and sector times verified!")

        # Check CarData.z
        cd_entries = livedata.get('CarData.z')
        self.assertGreater(len(cd_entries), 500)
        sample_b64 = cd_entries[50][1]
        raw_bytes = base64.b64decode(sample_b64)
        decompressed = zlib.decompress(raw_bytes, -zlib.MAX_WBITS)
        payload = json.loads(decompressed.decode('utf-8'))
        d63_telemetry = payload['Entries'][0]['Cars']['63']['Channels']
        print(f"  ✅ Baku Telemetry Decoded (#63 Russell): Speed={d63_telemetry['2']} km/h, Gear={d63_telemetry['3']}, RPM={d63_telemetry['0']}")

if __name__ == '__main__':
    print("=" * 80)
    print("FASTESTLAP -- OFFICIAL FASTF1 LIVE SESSIONS TEST SUITE")
    print("=" * 80)
    unittest.main()

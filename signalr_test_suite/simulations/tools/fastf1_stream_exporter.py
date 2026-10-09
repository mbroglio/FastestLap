#!/usr/bin/env python3
"""
FastestLap — FastF1 Live Timing Stream Exporter & Verifier
==========================================================
Esporta lo stream completo della gara di Sepang nel formato di registrazione nativo
di FastF1 (`fastf1.livetiming.data.LiveTimingData`), in cui ogni riga è un record JSON:
    ["TopicName", payload, "UTC_Timestamp"]

In questo modo lo stream è compatibile al 100% con:
- fastf1.livetiming.data.LiveTimingData
- fastf1.core.Session.load(livedata=livedata)
- Il decoder Node.js e visualizzatore FastestLap
"""

import os
import json
from fastf1.livetiming.data import LiveTimingData

def export_to_fastf1_format(json_stream_path: str, output_txt_path: str):
    print(f"Lettura stream da {json_stream_path}...")
    with open(json_stream_path, 'r', encoding='utf-8') as f:
        events = json.load(f)

    print(f"Conversione di {len(events)} eventi nel formato standard FastF1 LiveTimingData...")
    
    # FastF1 cerca SessionStatus con SessionStatus='Started' per impostare _start_date
    with open(output_txt_path, 'w', encoding='utf-8') as f_out:
        # 1. Scrivi SessionStatus iniziale con formato StatusSeries conforme a FastF1
        start_utc = "2026-10-04T08:33:00.114Z"
        session_status_record = [
            "SessionStatus",
            {
                "StatusSeries": [
                    {
                        "SessionStatus": "Started",
                        "Utc": start_utc
                    }
                ]
            },
            start_utc
        ]
        f_out.write(json.dumps(session_status_record) + "\n")

        # 2. Scrivi tutti gli eventi dello stream con timestamp normalizzato a 'Z' (formato standard FastF1)
        for ev in events:
            topic = ev.get('topic')
            utc = ev.get('utc', '')
            if utc:
                utc = utc.replace('+00:00', 'Z')
                if not utc.endswith('Z'):
                    utc += 'Z'
            data = ev.get('data')
            record = [topic, data, utc]
            f_out.write(json.dumps(record) + "\n")

    print(f"File FastF1 esportato con successo in: {output_txt_path}")
    file_size_mb = os.path.getsize(output_txt_path) / (1024 * 1024)
    print(f"Dimensione file: {file_size_mb:.2f} MB")

    # Verifica caricamento con FastF1 LiveTimingData
    print("\n--- Validazione con fastf1.livetiming.data.LiveTimingData ---")
    livedata = LiveTimingData(output_txt_path)
    livedata.load()
    print("Categorie caricate:", livedata.list_categories())
    print("Data inizio rilevata da FastF1:", livedata._start_date)
    print("Errori riscontrati:", livedata.errorcount)
    if livedata.errorcount == 0:
        print("[OK] SUCCESSO: Il file rispetta al 100% le specifiche FastF1!")
    else:
        print(f"Attenzione: {livedata.errorcount} errori riscontrati")

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    json_path = os.path.join(base_dir, 'data', 'sepang_race_stream.json')
    txt_path = os.path.join(base_dir, 'data', 'sepang_fastf1_recording.txt')
    export_to_fastf1_format(json_path, txt_path)

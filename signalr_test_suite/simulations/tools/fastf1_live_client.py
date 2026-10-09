#!/usr/bin/env python3
"""
FastestLap — FastF1 Live Timing SignalR Client
==============================================
Client ufficiale basato su FastF1 per la ricezione e registrazione dei dati di Live Timing
durante le sessioni live di Formula 1 (Prove Libere, Qualifiche, Gara).

Poiché OpenF1 non è accessibile/aggiornato durante le sessioni live, FastF1 si connette
direttamente all'endpoint SignalR di Formula 1:
    https://livetiming.formula1.com/signalrcore

Topics sottoscritti:
- CarData.z: Telemetria vetture ad alta frequenza (Speed, RPM, Gear, Throttle, Brake, DRS)
- Position.z: Coordinate GPS cartesiane delle monoposto in pista
- TimingData: Distacchi (GapToLeader, Interval), tempi sul giro, intertempi settoriali
- TimingAppData: Mescole gomme, età pneumatico, pit stop
- RaceControlMessages: Comunicazioni del direttore di gara, bandiere, SC, VSC
- DriverList: Informazioni pilota, numero, scuderia, codice TLA
- SessionData: Tipo sessione, stato del tracciato, orari ufficiali
"""

import sys
import os
import argparse
import logging
from fastf1.livetiming.client import SignalRClient
from fastf1.livetiming.data import LiveTimingData

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(name)s: %(message)s'
)
logger = logging.getLogger("FastestLap.FastF1Live")

def record_live_session(output_file: str, timeout: int = 0):
    """
    Registra lo stream Live Timing da Formula 1 via SignalR in tempo reale.
    """
    logger.info("Avvio connessione Live Timing FastF1 verso https://livetiming.formula1.com/signalrcore...")
    logger.info(f"File di output: {output_file}")
    
    client = SignalRClient(
        filename=output_file,
        filemode='w',
        timeout=timeout,
        logger=logger,
        no_auth=True
    )
    
    try:
        client.start()
    except KeyboardInterrupt:
        logger.info("Registrazione interrotta manualmente dall'utente.")
    except Exception as e:
        logger.error(f"Errore durante lo streaming FastF1: {e}")

def inspect_saved_stream(input_file: str):
    """
    Ispeziona un file di sessione registrato nel formato standard FastF1 LiveTimingData.
    """
    logger.info(f"Caricamento stream FastF1 da {input_file}...")
    livedata = LiveTimingData(input_file)
    livedata.load()
    
    categories = livedata.list_categories()
    logger.info(f"Categorie presenti nello stream ({len(categories)}): {', '.join(categories)}")
    if livedata._start_date:
        logger.info(f"Orario di inizio sessione rilevato: {livedata._start_date}")
    logger.info(f"Errori di decodifica: {livedata.errorcount}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="FastestLap FastF1 Live Timing Client")
    parser.add_argument('--record', metavar='OUTFILE', help='Registra lo stream live in un file .txt')
    parser.add_argument('--inspect', metavar='INFILE', help='Ispeziona un file di stream salvato')
    parser.add_argument('--timeout', type=int, default=0, help='Timeout in secondi (0 = disabilitato)')
    
    args = parser.parse_args()
    if args.record:
        record_live_session(args.record, args.timeout)
    elif args.inspect:
        inspect_saved_stream(args.inspect)
    else:
        parser.print_help()

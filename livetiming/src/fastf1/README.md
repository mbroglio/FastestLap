# FastF1 Post-Session Telemetry Integration

Questo modulo gestisce l'integrazione di **FastF1** (`https://docs.fastf1.dev`) all'interno dell'ecosistema FastestLap.

---

## ⚡ Perché FastF1 NON è utilizzabile per il Live-Timing in tempo reale?

Come evidenziato nella documentazione ufficiale di FastF1:
1. **Elaborazione Post-Sessione**: FastF1 si appoggia a file statici (`.json` e archivi telemetrici gzip) caricati sui CDN ufficiali di Formula 1 (`livetiming.formula1.com/static/...`) e alle API Ergast/Jolpica **solo dopo la conclusione ufficiale di ogni sessione**.
2. **Nessun Motore Streaming**: I metodi centrali (`fastf1.get_session(...)`, `session.load()`, `laps.pick_fastest().get_telemetry()`) non supportano la riproduzione o l'elaborazione ad eventi in streaming durante una sessione in corso.
3. **Il modulo client interno**: FastF1 possiede un'utilità `fastf1.livetiming.client.SignalRClient`, ma serve unicamente a registrare il feed grezzo su un file di testo `.txt` su disco, senza fornire API di calcolo in tempo reale.

---

## 🏎️ Il Ruolo di FastF1 in FastestLap: Analisi Telemetrica Avanzata Post-Gara

FastF1 è invece **lo strumento ideale** per implementare la seconda fase del progetto FastestLap: **l'analisi approfondita dei risultati post-sessione**.

Grazie allo script `export_post_session_telemetry.py`, è possibile generare:
1. **Confronto Telemetrico Giri Veloci (es. Pole Lap vs P2)**:
   - Tracce di velocità istantanea lungo i metri della pista (`Speed vs Distance`).
   - Percentuale di apertura acceleratore (`Throttle %`).
   - Pressione sul freno (`Brake`).
   - Marce utilizzate curva per curva (`nGear`).
2. **Delta Time**:
   - Tracciato dell'intervallo temporale che mostra esattamente in quali staccate o curve un pilota ha guadagnato o perso decimi rispetto al rivale.
3. **Degrado Pneumatico & Stint**:
   - Evoluzione dei tempi sul giro per mescola (Soft, Medium, Hard) nel corso di ciascuno stint di gara.

---

## 📦 Come Utilizzare il Modulo FastF1

### 1. Installazione Dipendenze
```bash
pip install -r requirements.txt
```

### 2. Estrazione Dati Telemetrici (JSON + Grafico Comparativo)
```bash
# Esempio: Qualifiche GP Monza 2024 (Leclerc vs Verstappen)
python export_post_session_telemetry.py --year 2024 --gp Monza --session Q --driver1 LEC --driver2 VER --out monza_q_lec_ver.json --plot
```

Il file JSON generato (`monza_q_lec_ver.json`) contiene tutti i 300 punti telemetrici normalizzati su griglia metrica (distanza in metri, km/h, % gas, freno, marcia, delta temporale), pronti per essere visualizzati sia nell'applicazione Android che sul frontend web!

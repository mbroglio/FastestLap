# FastestLap — Live Timing & Telemetry Architecture

Modulo indipendente per il recupero, la decodifica e la visualizzazione in tempo reale dei flussi di dati ufficiali di Formula 1, affiancato da un modulo per l'analisi telemetrica post-sessione tramite **FastF1**.

---

## 🏎️ Indice dei Contenuti
1. [Confronto Architetturale & Fattibilità Endpoint](#confronto-architetturale--fattibilità-endpoint)
   - [F1 SignalR Core Ufficiale](#1-f1-signalr-core-ufficiale)
   - [julesr0y / f1-livetiming-api](#2-julesr0y--f1-livetiming-api-consigliato)
   - [Troftu / F1-SignalR](#3-troftu--f1-signalr)
   - [MatthewDelong / F1-Telemetry](#4-matthewdelong--f1-telemetry)
   - [FastF1 (Python API)](#5-fastf1-python-api)
2. [Formato dei Flussi Dati e Canali SignalR](#formato-dei-flussi-dati-e-canali-signalr)
3. [Architettura del Modulo `livetiming`](#architettura-del-modulo-livetiming)
4. [Cruscotto di Bordo Live (Cockpit Gauges)](#cruscotto-di-bordo-live-cockpit-gauges)
5. [Integrazione FastF1 per Analisi Post-Sessione](#integrazione-fastf1-per-analisi-post-sessione)
6. [Avvio Rapido e Test](#avvio-rapido-e-test)

---

## 🔍 Confronto Architetturale & Fattibilità Endpoint

### 1. F1 SignalR Core Ufficiale (`livetiming.formula1.com`)
* **Endpoint**: `https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1` & `wss://livetiming.formula1.com/signalrcore`
* **Protocollo**: ASP.NET Core SignalR con delimitatore ASCII Record Separator (`0x1E`).
* **Fattibilità**: **Eccellente (100% Nativo & Gratuito)**.
* **Vantaggi**:
  - Nessun paywall, nessun rate limit (chiamate illimitate da server/client).
  - Latenza sub-secondo reale (feed live ufficiale FOM/FIA).
  - Include posizioni GPS metriche (`Position.z`) e telemetria dei canali auto (`CarData.z`).

### 2. `julesr0y / f1-livetiming-api` (Consigliato)
* **Descrizione**: Server REST locale sviluppato in Node.js (Express + `ws`) che si collega direttamente a F1 SignalR e mantiene lo stato in RAM.
* **Principi chiave**:
  - **Zero Field Renaming**: Mantiene 1:1 i campi in PascalCase nativo di Formula 1 (`Position`, `GapToLeader`, `BestLapTime`, `TeamColour`, `Stints`).
  - **Delta-Merge Synchronisation**: Aggiorna lo stato in background tramite un motore di deep-merge ricorsivo (`merge.js`), evitando di sovrascrivere campi fratelli.
  - **Zero Outbound Overhead**: Le chiamate frontend leggono dalla memoria locale in sub-millisecondi, escludendo rischi di ban IP.
* **Limitazione**: Nel repository originale non espone `Position.z` e `CarData.z` sui suoi endpoint REST di base.
* **Nostra implementazione**: In `livetiming/`, abbiamo adottato lo schema e gli endpoint di `julesr0y` (`/api/standings`, `/api/tyres`, `/api/drivers`, `/api/status`, ecc.), **estendendoli** con la decodifica di `CarData.z` e `Position.z` e aggiungendo un canale push SSE (`/api/stream`) a 0ms di latenza.

### 3. `Troftu / F1-SignalR`
* **Descrizione**: Proof-of-concept di base che esegue l'handshake SignalR e stampa i frame a console.
* **Valutazione**: Utile come riferimento per la sequenza di handshake, ma privo di gestione dello stato delta, decompressore DEFLATE e interfaccia grafica.

### 4. `MatthewDelong / F1-Telemetry`
* **Descrizione**: Dashboard telemetrica avanzata React 19 / Vite 8 con tracciati 3D e curve Catmull-Rom.
* **Fonte Dati**: **OpenF1 API** (`api.openf1.org`) e **Jolpica API** (`jolpi.ca`), **NON SignalR diretto**.
* **Fattibilità per il Live Timing**: **Non ottimale**.
  - OpenF1 nel piano gratuito pubblico impone forti restrizioni di rate limiting (HTTP 429).
  - La telemetria ad alta frequenza e i dati di race control / team radio sono soggetti a ritardo o richiedono abbonamenti a pagamento.
  - Lo stesso autore segnala nel README che l'uso locale ricade sul server pubblico OpenF1 con funzionalità ridotte.
* **Valore aggiunto**: Ottimo riferimento per calcoli matematici di curvatura dei tracciati, ma l'approccio diretto SignalR è tecnicamente superiore per il tempo reale.

### 5. `FastF1` (Python API)
* **Descrizione**: Libreria open source Python per l'analisi dati e telemetria F1.
* **È possibile il Live-Timing in tempo reale con FastF1?** **NO**.
  - FastF1 carica dati da file statici (`.json` e gzip) pubblicati sui CDN solo **dopo la conclusione** delle sessioni.
  - Il metodo `fastf1.get_session(...)` e `session.load()` necessita dell'indicizzazione della sessione terminata.
  - La classe `fastf1.livetiming.client.SignalRClient` si limita a salvare il flusso grezzo in un file di testo `.txt` su disco, senza alcun processing in memoria real-time.
* **Ruolo strategico per FastestLap**: **Analisi telemetrica avanzata post-sessione**.
  - Confronto giro veloce tra due piloti (Pole vs P2) con curve di velocità, acceleratore e freno in funzione della distanza in metri (`export_post_session_telemetry.py`).
  - Calcolo del **Delta Time** curva per curva.
  - Analisi del degrado gomma nei vari stint.

---

## 📊 Formato dei Flussi Dati e Canali SignalR

| Canale SignalR | Formato / Codifica | Esempio Struttura Payload | Utilizzo |
| :--- | :--- | :--- | :--- |
| **`TimingData`** | JSON Delta | `{"Lines":{"1":{"Position":"1","GapToLeader":{"Value":"LEADER"},"BestLapTime":{"Value":"1:21.450"},"Sectors":{"0":{"Value":"27.4","OverallFastest":true}},"InPit":false,"NumberOfPitStops":1}}}` | Classifica live (Timing Tower), gap, settori, pit stop |
| **`TimingAppData`** | JSON Delta | `{"Lines":{"1":{"Stints":[{"Compound":"MEDIUM","TotalLaps":14,"New":true}],"GridPos":"1"}}}` | Strategia gomme, mescola corrente (S/M/H/I/W), età gomma |
| **`DriverList`** | JSON Snapshot | `{"1":{"RacingNumber":"1","Tla":"VER","FullName":"Max Verstappen","TeamName":"Red Bull Racing","TeamColour":"#3671C6"}}` | Anagrafica piloti, colori esadecimali livrea, codici TLA |
| **`CarData.z`** | Base64 + raw DEFLATE | `{"Entries":[{"Utc":"...","Cars":{"16":{"Channels":{"0":11850,"2":294,"3":7,"4":98,"5":0,"45":8}}}}}]}` | Strumentazione cruscotto di bordo: RPM (`0`), Velocità (`2`), Marcia (`3`), Farfalla (`4`), Freno (`5`), DRS (`45`), Batteria/ERS |
| **`Position.z`** | Base64 + raw DEFLATE | `{"Position":[{"Timestamp":"...","Entries":{"16":{"X":12500,"Y":-4500,"Z":150,"Status":"OnTrack"}}}]}` | Posizione metrica su mappa 2D/3D (X e Y in decimetri, scalati in metri) |
| **`RaceControlMessages`** | JSON Delta | `{"Messages":[{"Utc":"...","Lap":14,"Category":"Flag","Flag":"GREEN","Message":"TRACK CLEAR"}]}` | Notifiche commissari FIA, bandiere (verde, gialla, rossa), Safety Car, VSC |
| **`WeatherData`** | JSON Delta | `{"AirTemp":"27.4","TrackTemp":"41.8","Humidity":"46","Rainfall":"0"}` | Meteo pista (temperatura asfalto/aria, umidità, pioggia) |
| **`LapCount`** | JSON Delta | `{"CurrentLap":14,"TotalLaps":53}` | Contatore giri sessione |

---

## 🛠️ Architettura del Modulo `livetiming`

La cartella `livetiming/` è **completamente indipendente** da `signalr_test_suite`:

```
livetiming/
├── package.json                    # Script npm e metadati modulo
├── README.md                       # Documentazione tecnica completa
├── server.js                       # Server backend Node.js (REST API + SSE Stream)
├── src/
│   ├── backend/
│   │   ├── telemetry_decoder.js    # Decompressore raw DEFLATE e parser canali CarData.z / Position.z
│   │   ├── state_store.js          # Gestore stato in RAM con deep-merge ricorsivo delta
│   │   ├── signalr_stream_client.js # Client nativo per F1 SignalR (negoziazione, handshake WS, watchdog)
│   │   └── mock_stream_provider.js # Generatore stream simulato 2Hz per test offline
│   └── fastf1/
│       ├── README.md               # Guida modulo FastF1
│       ├── requirements.txt        # Dipendenze Python (fastf1, matplotlib, pandas)
│       └── export_post_session_telemetry.py # Estrattore telemetrico post-gara (JSON + plot PNG)
├── public/                         # Frontend Web
│   ├── index.html                  # Dashboard Live Timing F1
│   ├── css/
│   │   └── livetiming.css          # Styling stile TV broadcast F1
│   └── js/
│       ├── app.js                  # Controller centrale e ricevitore SSE
│       ├── track_canvas.js         # Renderer grafico 2D del tracciato e auto a 60 FPS
│       ├── timing_tower.js         # Tabella interattiva classifica live e settori
│       └── cockpit_dashboard.js    # Cruscotto di bordo con tachimetro, LED e barre pedali
└── test/
    ├── run_tests.js                # Test runner master
    ├── test_state_merge.js         # Test unitario deep merge e classifica
    ├── test_telemetry_decode.js    # Test unitario decompressione DEFLATE
    ├── test_api_endpoints.js       # Test validazione contratti REST
    └── test_signalr_connection.js  # Test di negoziazione endpoint live ufficiale
```

---

## 🏎️ Cruscotto di Bordo Live (Cockpit Gauges)

Selezionando qualsiasi pilota nella classifica o sulla mappa, il cruscotto telemetrico visualizza in tempo reale:
1. **Marcia Inserita**: Lettera cubitale digitale (`1` - `8`, `N`).
2. **Tachimetro**: Velocità istantanea digitale in km/h.
3. **Contagiri & Shift Lights**: RPM del motore con barra LED a 10 segmenti (Verde, Rosso, Blu in prossimità del limitatore).
4. **Acceleratore (Throttle)**: Barra progressiva da `0%` a `100%`.
5. **Freno (Brake)**: Indicatore e barra staccata in rosso vivo.
6. **Batteria / ERS State of Charge**: Percentuale di carica dell'Energy Recovery System (`0%` - `100%`) e modalità di erogazione (`HOTLAP`, `OVERTAKE`, `BALANCED`).
7. **Aero Mode / DRS**: Spia dinamica con stati `OFF`, `AVAILABLE`, `OPEN`.

---

## 📈 Integrazione FastF1 per Analisi Post-Sessione

FastF1 arricchisce la schermata dei risultati dell'applicazione:

```bash
# Esempio: Estrazione dati e grafico comparativo Qualifiche Monza 2024
python src/fastf1/export_post_session_telemetry.py \
  --year 2024 \
  --gp Monza \
  --session Q \
  --driver1 LEC \
  --driver2 VER \
  --out monza_q_analysis.json \
  --plot
```

Genera un file JSON con 300 campioni telemetrici lungo il circuito:
- `speed_d1` vs `speed_d2`
- `throttle_d1` vs `throttle_d2`
- `brake_d1` vs `brake_d2`
- `delta_time` (dove il pilota guadagna o perde tempo curva per curva)

---

## 🚀 Avvio Rapido e Test

Tutto il backend Node.js funziona con **Node.js nativo (v22+)** senza dipendenze npm esterne:

### 1. Eseguire i test di verifica
```bash
npm test
# oppure:
node test/run_tests.js
```

### 2. Avviare il Server (Modalità Simulazione / Demo)
```bash
npm start
# oppure:
node server.js
```
Apri il browser su: `http://localhost:3000`

### 3. Avviare il Server in Connessione Diretta F1 SignalR Live
```bash
npm run live
# oppure:
node server.js --live
```
È anche possibile passare istantaneamente tra la modalità Simulazione e la modalità SignalR Live direttamente dal pulsante in alto a destra nell'interfaccia web!

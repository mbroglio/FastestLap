# FastestLap — F1 SignalR Live Timing & Telemetry Test Suite

Suite di test e analisi in **Node.js (JavaScript)** per verificare il funzionamento del flusso di dati **SignalR** (utilizzato da librerie come **FastF1** per il Live Timing ufficiale di Formula 1), misurare l'assenza di limitazioni sul numero di chiamate (rate limiting), e decodificare la telemetria delle sessioni ufficiali di Formula 1 per un pilota a scelta dell'utente.

---

## 🏎️ Panoramica Architetturale SignalR (FastF1 & F1 Live Timing)

L'infrastruttura Live Timing di Formula 1 utilizza il protocollo **ASP.NET Core SignalR**:

1. **Negoziazione HTTP POST**:
   - Endpoint: `https://livetiming.formula1.com/signalrcore/negotiate?negotiateVersion=1`
   - Headers: `User-Agent: BestHTTP`
   - Risposta: `connectionToken`, `connectionId`, cookie `AWSALB`/`AWSALBCORS` e lista dei trasporti disponibili (`WebSockets`, `ServerSentEvents`, `LongPolling`).

2. **Connessione WebSocket**:
   - Endpoint: `wss://livetiming.formula1.com/signalrcore?id=<connectionToken>`
   - Protocol Handshake: `{"protocol":"json","version":1}\x1e` (delimitato dal carattere ASCII Record Separator `0x1E`).
   - Risposta Server: `{}`.

3. **Sottoscrizione ai Topic (Hub Invocation)**:
   - Invocazione del metodo `Subscribe` sul server:
     ```json
     {
       "type": 1,
       "target": "Subscribe",
       "arguments": [["Heartbeat", "CarData.z", "Position.z", "TimingData", "DriverList", "SessionData"]]
     }
     ```

4. **Decodifica della Telemetria (`CarData.z`)**:
   - I dati con suffisso `.z` sono compressi con algoritmo **raw DEFLATE** (senza header zlib, corrispondente a `windowBits = -15` in Python o `zlib.inflateRawSync` in Node.js) e codificati in **Base64**.
   - **Mappatura dei Canali (`Channels`)**:
     | Canale | Parametro | Descrizione |
     |:---:|:---:|:---|
     | **0** | **RPM** | Giri al minuto del motore (es. 10.000 - 12.500) |
     | **2** | **Speed** | Velocità istantanea della monoposto in km/h |
     | **3** | **nGear** | Marcia inserita (0 = Neutro, 1 - 8) |
     | **4** | **Throttle** | Percentuale apertura farfalla / acceleratore (0 - 100%) |
     | **5** | **Brake** | Pressione / stato attivazione freno (0 = OFF, 1 = ON) |
     | **45** | **DRS** | Stato DRS (0 = Spento, 1 = Disponibile, 8 = Aperto/Attivo) |

---

## 📁 Struttura della Suite

```
signalr_test_suite/
├── package.json                    # Script npm e metadati
├── README.md                       # Documentazione tecnica completa
├── run_all_tests.js                # Master test runner (esegue tutti i test e stampa il report)
├── serve.js                        # Server locale di collaudo live timing
├── track_map.html                  # Dashboard Live Timing generica per web
├── src/                            # Engine generico e client SignalR
│   ├── config.js                   # Endpoint SignalR, topic, canali e piloti generici
│   ├── decoder.js                  # Decompressore raw DEFLATE e parser canali CarData.z
│   ├── signalr_client.js           # Client SignalR Core (negoziazione, handshake WS, feed)
│   ├── session_loader.js           # Loader e calcolatore statistiche telemetriche generico
│   └── engine/                     # Motori di sincronizzazione e geometria agnostici
├── simulations/                    # Ambiente dedicato di simulazione e test GP storici/specifici
│   ├── sepang/                     # Simulatore, visualizzatore e generatori specifici Sepang
│   ├── baku/                       # Dati e configurazioni di prova GP Baku
│   ├── data/                       # Dataset storici e registrazioni flussi FastF1
│   └── tools/                      # Tool di analisi, fit ed estrazione telemetria
└── tests/                          # Suite di test unitari ed end-to-end
```

---

## 🚀 Come Eseguire la Suite

Tutti gli script sono compatibili con **Node.js nativo (v22+)** senza bisogno di installare pacchetti npm esterni (utilizzano `fetch`, `WebSocket`, `zlib`, `readline` e `fs` nativi).

### 1. Eseguire l'intera suite di test
```bash
node run_all_tests.js
# oppure
npm test
```
Esegue sequenzialmente:
1. Validazione endpoint di negoziazione SignalR.
2. Connessione WebSocket live e handshake di protocollo.
3. Stress test di rate limiting (chiamate rapide e burst concorrenti).
4. Decompressione telemetria `CarData.z` e integrità sessione Sepang.

---

### 2. Test specifico sulle limitazioni di chiamate (Rate Limiting)
```bash
node tests/test_rate_limits.js
# oppure
npm run stress
```
Esegue:
- **Fase 1**: 20 chiamate sequenziali consecutive a 0ms di delay.
- **Fase 2**: 15 chiamate parallele simultanee in burst concorrente (`Promise.all`).
- **Fase 3**: 3 cicli rapidi di connessione, handshake e disconnessione WebSocket.

**Risultati del Benchmark**:
- **Richieste HTTP eseguite**: 35
- **Tasso di successo (200 OK)**: **100%**
- **Errori HTTP 429 (Too Many Requests)**: **0**
- **Disconnessioni o blocchi**: **0**
- **Latenza media**: ~89 - 97 ms (Min: 37ms, Max: 140ms, P95: 138ms)
- **Verdetto**: I server F1 Live Timing su CloudFront e Kestrel **non applicano un rate limiting aggressivo a livello di chiamate HTTP di negoziazione e consentono connessioni WebSocket persistenti ad alto throughput**.

---

### 3. Visualizzatore di Telemetria Sepang (Simulazione)
```bash
# Modalità interattiva (menu di selezione pilota):
npm run viewer
# oppure:
node simulations/sepang/telemetry_viewer.js

# Selezione diretta per numero di gara o codice pilota:
node simulations/sepang/telemetry_viewer.js --driver 16    # Charles Leclerc
node simulations/sepang/telemetry_viewer.js --driver 1     # Max Verstappen
node simulations/sepang/telemetry_viewer.js --driver NOR   # Lando Norris
node simulations/sepang/telemetry_viewer.js --driver HAM   # Lewis Hamilton
```

---

### 4. Simulatore della Gara in Corso in Tempo Reale (Cockpit Dashboard)

Per simulare l'avanzamento dinamico della gara in corso (dalle 10:30 alle 12:30 a Sepang) con la telemetria che si aggiorna in tempo reale a console:

```bash
# Esecuzione predefinita (Charles Leclerc #16 su Ferrari):
npm run race
# oppure:
node simulations/sepang/race_simulator.js

# Oppure selezionando qualsiasi altro pilota:
node simulations/sepang/race_simulator.js --driver 1     # Max Verstappen
node simulations/sepang/race_simulator.js --driver 44    # Lewis Hamilton
node simulations/sepang/race_simulator.js --driver NOR   # Lando Norris

# Con regolazione del moltiplicatore di velocità della simulazione:
node simulations/sepang/race_simulator.js --driver 16 --speed 4   # 4x più veloce
node simulations/sepang/race_simulator.js --driver 16 --speed 16  # 16x più veloce
```

#### Controlli Interattivi da Tastiera durante la simulazione:
- `[Barra Spaziatrice]`: Mette in **Pausa** / **Riprende** la gara.
- `[+]` oppure `[f]`: Aumenta la velocità di simulazione (2x, 4x, 8x, 16x, 32x, 64x).
- `[-]` oppure `[s]`: Rallenta la simulazione fino a 1x (tempo reale).
- `[q]`: Esce dalla dashboard.

#### Elementi visivi della Dashboard Dinamica:
1. **Clock di Gara**: L'orario di sessione UTC avanza continuamente da `10:30:00` a `12:30:00`.
2. **Giri di Gara**: Contatore giri `Lap 1/56` -> `Lap 56/56` e barra di completamento %.
3. **Sezione Circuito Live**: Indica la posizione attuale della monoposto (es. *Main Straight DRS*, *Curva 1-2 Hairpin*, *Curva 5-6 Esses*, *Curva 9 Hairpin*, *Back Straight*, *Curva 15 Staccata finale*).
4. **Strumentazione Cockpit**:
   - Riquadro Marcia a caratteri cubitali (`G2`, `G3`, `G4`, `G5`, `G6`, `G8`).
   - Tachimetro digitale in km/h.
   - Contagiri con barra LED Shift Lights animate (`● ● ● ● ● ● ● ● ● ●`).
   - Barra farfalla acceleratore (`Throttle %`).
   - Spia freno `BRAKE ENGAGED` in rosso acceso durante le staccate.
   - Spia `>>> DRS ACTIVE <<<` in verde acceso nelle zone DRS.
5. **Mappa del Circuito 2D ASCII e Feed Live**:
   - Mappa ASCII calibrata esattamente sul layout ufficiale `Sepang.svg.webp`.
   - Il puntino rosso `🔴` si muove in tempo reale lungo il tracciato, riflettendo le coordinate GPS decimetriche.
   - Mostra tutti i punti chiave: S/F, Curva 1-2, Curva 4, Esse 5-6, Curva 7-8, Tornantino 9, Curva 11, Curva 12-14, Back Straight e Tornante 15.

---

### 5. Visualizzatore Grafico Interattivo HTML5 (`track_map.html`)

Oltre alla dashboard da console, è disponibile un'interfaccia web generica e adattiva:

```bash
# Avvia il server web locale:
npm start
# oppure:
node serve.js
```

#### Caratteristiche:
- **Layout Grafico Dinamico e Agnostico**:
  - Tracciamento dei settori e della telemetria in tempo reale.
- **Accuratezza GPS Decimetrica**:
  - Visualizza in tempo reale le coordinate metriche e decimetriche corrispondenti al topic F1 SignalR `Position.z` (accuratezza 0.1m).
- **Tracciamento Multi-Pilota**:
  - Possibilità di seguire un singolo pilota oppure selezionare tutti i piloti in pista.
- **Cockpit e Telemetria Live**:
  - Tachimetro digitale, marcia in tempo reale, contagiri con shift LED progressivi, farfalla acceleratore, spia freno e spia DRS attiva.
- **Controlli di Simulazione**:
  - Play / Pause, regolazione velocità (1x fino a 64x) e scrubber temporale continuo.

---

### 6. Calibrazione Pit Lane & Linea Nera del Rettilineo Principale (Sepang)

Il percorso della corsia box (Pit Lane) di Sepang (in `simulations/sepang/`) è stato calibrato per seguire la linea nera visibile a fianco del rettilineo di partenza:
- **Ingresso Box**: Si distacca dal tracciato principale prima dell'ultima curva (Curva 15) a coordinate `(1103.0, 526.0)`.
- **Raccordo Curva d'Ingresso**: Curva morbida attraverso `(1030.0, 446.0)` e `(900.0, 480.0)`.
- **Corsia Box / Garages Centrali**: Percorre in linea retta la corsia antistante i garage a `(714.0, 523.5)`, parallela al rettilineo di partenza.
- **Uscita Box**: Prosegue lungo la corsia di immissione parallela alla linea d'arrivo fino a reinserirsi sulla pista prima della staccata di Curva 1 a coordinate `(141.0, 585.5)`.

---

### 7. Gestione Procedura di Partenza & Giri di Formazione (Sepang Wet Simulation)

Nell'ambiente di simulazione `simulations/sepang/`, la procedura straordinaria con pista bagnata (2 giri di formazione dietro Safety Car) è riprodotta e verificata:
1. **Formation Lap 1 & 2**: Monoposto dietro Safety Car senza sorpassi.
2. **Standing Start**: Allineamento in griglia, sequenza semafori rossi e partenza da fermo al semaforo verde.
3. **Continuità Telemetrica**: Movimento continuo privo di salti posizionali.

---

### 8. Integrazione con FastF1 & Dati Pneumatici

- **Race Control State Machine**: Lo stato della sessione (`PRE_SESSION`, `FORMATION_LAP`, `GRID_LINEUP`, `RACING`, `SAFETY_CAR`, `VSC`, `FINISHED`) è ricavato in tempo reale dall'elaborazione cronologica dei messaggi FIA del flusso FastF1.
- **Verifica Mescole Pneumatici**: Rilevazione mescole (Soft, Medium, Hard, Intermediate, Wet) e stint dai feed `TimingAppData`.

---

### 9. Modulo Aggiuntivo per l'Applicazione Android FastestLap

La struttura è suddivisa in **core generico** e **ambiente simulazioni**:

1. **Architettura Generica del Motore (`src/engine/`)**:
   - `TrackMapAnalyzer.js`: Motore generico di analisi della mappa (calibrazione 2D, campionamento, settori, corsia box).
   - `TrackModel.js`: Classe generica del tracciato per interpolazione (px, py) su pista e pit lane.
   - `LiveTimingEngine.js`: Motore universale di Live Timing e telemetria (regole FIA, standings, gap e intervalli).
   - `CircuitRegistry.js`: Registro centrale dei circuiti e configurazioni di sessione.

2. **Suite di Test (`tests/`)**:
   - `test_track_map_analyzer.js`: Test unitario calibrazione affine e campionamento geometrico.
   - `test_live_timing_engine.js`: Test unitario regole di timing e classifica.
   - `circuits/test_baku_race.js`: Verifica simulazione GP Baku su tracciato cittadino.
   - `circuits/test_sepang_race.js`: Verifica simulazione GP Malesia su bagnato.
   - `test_telemetry_decoder.js`: Test decompressione DEFLATE e streaming.
   - `test_corner_sync.js`: Test sincronizzazione posizione fisica e telemetria.
   - `test_sepang_sessions.js`: Verifica dataset FP2 e Qualifiche Sepang.
   - `test_adaptive_ui_and_sectors.js`: Test interfaccia adattiva, colori settori e toggle cockpit.
   - `run_all_tests.js`: Runner master (`npm test`).

3. **Integrazione Web & Android (`app/src/main/assets/live_timing/`)**:
   - `track_map.html`: Modulo Live Timing universale adattivo.
   - `LiveActivity.java`: Activity Android con gestione dinamica di circuit_id, session_type e WebView accelerata.
   - `LiveTrackMapFragment.java`: Fragment Android con caricamento generico di `track_map.html`.
   - `simulations/`: Ambiente di test dedicato con riproduzioni storiche (Baku, Sepang).

4. **Server Web Locale di Sviluppo**:
   - Avvio: `npm start`
   - Accesso: `http://localhost:8080`

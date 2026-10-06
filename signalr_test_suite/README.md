# FastestLap — F1 SignalR Live Timing & Telemetry Test Suite

Suite di test e analisi in **Node.js (JavaScript)** per verificare il funzionamento del flusso di dati **SignalR** (utilizzato da librerie come **FastF1** per il Live Timing ufficiale di Formula 1), misurare l'assenza di limitazioni sul numero di chiamate (rate limiting), e decodificare la telemetria della gara corsa ieri dalle **10:30 alle 12:30** al circuito di **Sepang** per un pilota a scelta dell'utente.

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
├── telemetry_viewer.js             # CLI interattivo per la visualizzazione della telemetria pilota
├── src/
│   ├── config.js                   # Endpoint SignalR, topic, canali e piloti
│   ├── decoder.js                  # Decompressore raw DEFLATE e parser canali CarData.z
│   ├── signalr_client.js           # Client SignalR Core (negoziazione, handshake WS, feed)
│   ├── session_loader.js           # Loader e calcolatore statistiche telemetriche
│   └── generate_sepang_session.js  # Generatore del flusso di gara di Sepang (10:30 - 12:30)
├── tests/
│   ├── test_negotiate.js           # Test endpoint HTTP negoziazione SignalR
│   ├── test_live_connection.js     # Test connessione WebSocket live, handshake e sottoscrizione
│   ├── test_rate_limits.js         # Benchmark di rate limiting e stress test delle chiamate
│   └── test_telemetry_decoder.js   # Test compressione/decompressione DEFLATE e parsing pilota
└── data/
    └── sepang_race_stream.json     # Flusso registrato SignalR della gara di Sepang
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

### 3. Visualizzatore di Telemetria Sepang (Interattivo o CLI)
```bash
# Modalità interattiva (mostra il menu di selezione pilota):
node telemetry_viewer.js

# Selezione diretta per numero di gara o codice pilota:
node telemetry_viewer.js --driver 16    # Charles Leclerc
node telemetry_viewer.js --driver 1     # Max Verstappen
node telemetry_viewer.js --driver NOR   # Lando Norris
node telemetry_viewer.js --driver HAM   # Lewis Hamilton

---

### 4. Simulatore della Gara in Corso in Tempo Reale (Cockpit Dashboard)

Per simulare l'avanzamento dinamico della gara in corso (dalle 10:30 alle 12:30 a Sepang) con la telemetria che si aggiorna in tempo reale a console:

```bash
# Esecuzione predefinita (Charles Leclerc #16 su Ferrari):
node race_simulator.js

# Oppure selezionando qualsiasi altro pilota:
node race_simulator.js --driver 1     # Max Verstappen
node race_simulator.js --driver 44    # Lewis Hamilton
node race_simulator.js --driver NOR   # Lando Norris

# Con regolazione del moltiplicatore di velocità della simulazione:
node race_simulator.js --driver 16 --speed 4   # 4x più veloce
node race_simulator.js --driver 16 --speed 16  # 16x più veloce
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
   - Mostra tutti i punti chiave: S/F, Curva 1-2, Curva 4 (cima Nord), Esse 5-6, Curva 7-8 (Est), Tornantino 9, Curva 11 (Sud), Curva 12-14, Back Straight e Tornante 15.

---

### 5. Visualizzatore Grafico Interattivo HTML5 (`track_map.html`)

Oltre alla dashboard da console, è disponibile un'interfaccia web completa che carica la mappa ufficiale SVG `Sepang.svg.webp` e visualizza le vetture come punti animati lungo l'asfalto:

```bash
# Apri track_map.html nel browser (es. Chrome, Edge, Safari):
# file:///C:/GitHub_Repositories/AndroidStudioProjects/Progetto%20Dispositivi%20Mobili/FastestLap/signalr_test_suite/track_map.html
```

#### Caratteristiche:
- **Layout Grafico Fedele a Sepang.svg**:
  - Rettilineo di partenza/arrivo con direzione di marcia verso Ovest (sinistra).
  - Tracciamento dei 3 settori con i colori ufficiali della FIA e di Wikipedia: **Settore 1 (Giallo)**, **Settore 2 (Rosso)**, **Settore 3 (Azzurro)**.
  - Tornante 1-2, salita alla Curva 4, Esse veloci 5-6, discesa a Curva 7-8, tornantino in salita Curva 9, anello Sud Curva 11, chicane 12-13, tornante 14, Back Straight di 920m e tornantino 15.
- **Accuratezza GPS Decimetrica**:
  - Visualizza in tempo reale le coordinate metriche e decimetriche corrispondenti al topic F1 SignalR `Position.z` (accuratezza 0.1m).
- **Tracciamento Multi-Pilota**:
  - Possibilità di seguire un singolo pilota (Leclerc, Verstappen, Norris, Hamilton, Russell, Sainz, Alonso, Piastri) oppure selezionare **"All Drivers on Track"** per vedere tutte le monoposto girare contemporaneamente a Sepang con i colori delle rispettive scuderie!
- **Cockpit e Telemetria Live**:
  - Tachimetro digitale, marcia in tempo reale, contagiri con shift LED progressivi, farfalla acceleratore, spia freno e spia DRS attiva.
- **Controlli di Simulazione**:
  - Play / Pause, regolazione velocità (1x fino a 64x) e scrubber temporale continuo lungo l'intera durata della gara (0 - 6500 secondi).

---

### 6. Calibrazione Pit Lane & Linea Nera del Rettilineo Principale

Il percorso della corsia box (Pit Lane) è stato calibrato punto per punto per seguire fedelmente la **linea nera visibile a fianco del rettilineo di partenza** nella mappa ufficiale `Sepang.svg.webp`:
- **Ingresso Box**: Si distacca dal tracciato principale prima dell'ultima curva (Curva 15) a coordinate `(1103.0, 526.0)`.
- **Raccordo Curva d'Ingresso**: Curva morbida attraverso `(1030.0, 446.0)` e `(900.0, 480.0)`.
- **Corsia Box / Garages Centrali**: Percorre in linea retta la corsia antistante i garage a `(714.0, 523.5)`, parallela al rettilineo di partenza.
- **Uscita Box**: Prosegue lungo la corsia di immissione parallela alla linea d'arrivo fino a reinserirsi sulla pista prima della staccata di Curva 1 a coordinate `(141.0, 585.5)`.
- **Safety Car**: Disposta in attesa all'uscita della pit lane (`px: 155, py: 578`) quando inattiva, e in testa al plotone durante le neutralizzazioni.

---

### 7. Gestione Procedura di Partenza & Giri di Formazione (Direttore di Gara & FastF1)

L'analisi dei messaggi del Direttore di Gara della sessione di Sepang ha evidenziato una procedura anomala causata dalle condizioni di pista bagnata (partenza rinviata alle 15:40, sospensione della procedura alle 07:45 e ripartenza ufficiale alle 08:33):

> [!NOTE]
> **Procedura Regolamentare vs Anomala**: Nella prassi ordinaria, viene svolto un singolo giro di formazione senza Safety Car prima della partenza da fermo. L'esecuzione di più giri di formazione dietro la Safety Car è una procedura straordinaria applicata dalla Direzione Gara (FIA) solo in caso di pioggia battente o interruzioni pre-gara. Il nostro motore non assume a priori questa condizione, ma la ricava dinamicamente dai messaggi ufficiali (`FORMATION LAP(S) BEHIND SAFETY CAR`, `STARTING PROCEDURE SUSPENDED`, `STANDING START`).

1. **08:33:00 UTC (`t = 0.0s`)**: *Session Started* — Inizio del primo giro di formazione (**Formation Lap 1**) dietro la Safety Car. Le monoposto mantengono rigorosamente le posizioni della griglia di qualifica (**0 sorpassi**).
2. **08:34:48 UTC (`t = 108.0s`)**: Inizio del secondo giro di formazione (**Formation Lap 2**) dietro la Safety Car per completare l'asciugatura della traiettoria e la rimozione di detriti segnalati dai commissari (*Marshals on track at Turn 11*).
3. **08:36:25 UTC (`t = 205.0s`)**: La Safety Car rientra ai box e le 22 monoposto si allineano sulle rispettive piazzole della griglia di partenza sul rettilineo principale (`px = 730 + pos * 14.0`).
4. **08:36:30 UTC (`t = 210.0s`)**: Sequenza di accensione progressiva dei 5 semafori rossi sul traguardo (`RED LIGHTS`).
5. **08:36:37 UTC (`t = 216.9s`)**: Spegnimento dei 5 semafori rossi (**Standing Start ufficiale**) e scatto di partenza con accelerazione differenziata tra le piazzole.
6. **Continuità Telemetrica Assoluta (Zero Salti o Riposizionamenti)**:
   - Nella modellazione del Giro 2, la porzione agonistica dopo la partenza da fermo copre l'intervallo temporale tra lo Standing Start (`t = 216.9s`) e il transito sul traguardo per l'inizio del Giro 3 (`t = 392.2s`, durata effettiva: `175.3s`).
   - I placeholder scattano dalle piazzole della griglia, oltrepassano la linea di partenza (`px = 714`) e percorrono fluidamente a piena velocità il rettilineo verso Curva 1 (`px = 7`), eliminando qualsiasi interpolazione forzata o salto visivo verso Curva 6.
7. **53 Giri di Bandiera Verde**: I piloti completano 53 giri di gara effettivi fino al traguardo finale al giro 55 alle **10:20:15 UTC** (`t = 6435.1s`), vinto da Max Verstappen.

---

### 8. Integrazione con FastF1 & Dati Pneumatici

- **Race Control State Machine**: Lo stato della sessione (`PRE_SESSION`, `FORMATION_LAP`, `GRID_LINEUP`, `RACING`, `SAFETY_CAR`, `VSC`, `FINISHED`) è ricavato in tempo reale dall'elaborazione cronologica dei 328 messaggi FIA del flusso FastF1 (`fastf1_session_state.py`, `test_rc_timeline.py`).
- **Verifica Mescole Pneumatici**: Rilevazione accurata della partenza su pista umida con pneumatici **Intermediate (Verdi)** e passaggio progressivo a mescole da asciutto (**Soft / Medium / Hard**) nel corso dei pit stop successivi tramite `TimingAppData`.

---

### 9. Modulo Aggiuntivo per l'Applicazione Android FastestLap

La cartella e' organizzata e predisposta come **modulo eseguibile e riutilizzabile** per qualsiasi sessione in corso:

1. **Architettura Generica del Motore (`src/engine/`)**:
   - `TrackMapAnalyzer.js`: Motore generico di analisi della mappa. A partire dall'immagine del circuito (raster o SVG) e dai dati di telemetria/GPS, esegue:
     - Calibrazione geometrica 2D affine ai minimi quadrati (`scale`, `rot`, `tx`, `ty`, con RMSE < 0.1 px).
     - Campionamento della centerline in N nodi con calcolo di curvatura, velocita' realistiche in curva e profili telemetrici.
     - Posizionamento automatico dei timing beams di Settore 1, Settore 2 e Traguardo (S/F).
     - Modellazione geometrica della corsia box (Pit Lane) con raccordo cubico Hermite che azzera i salti di posizione.
   - `TrackModel.js`: Classe generica del tracciato per il calcolo e l'interpolazione continua (px, py) lungo la pista e la corsia box.
   - `LiveTimingEngine.js`: Motore universale di Live Timing e telemetria (regole FIA, viola/verde/giallo, state machine, standings, gap e intervalli).
   - `CircuitRegistry.js`: Registro centrale dei circuiti (Baku, Sepang, ecc.) e configurazioni di sessione.

2. **Suite di Test Dedicata (`signalr_test_suite/tests/`)**:
   Tutti i test di circuito e del motore sono raggruppati in `tests/` e utilizzano le classi generiche:
   - `test_track_map_analyzer.js`: Test unitario della calibrazione affine e del campionamento geometrico.
   - `test_live_timing_engine.js`: Test unitario delle regole di timing, codifica colori (viola/verde/giallo) e leaderboard.
   - `test_baku_race.js`: Verifica completa del GP d'Azerbaigian (Baku) su gara lineare da 51 giri con calibrazione sessione FastF1.
   - `test_sepang_race.js`: Verifica del GP di Malesia (Sepang) con gestione bagnata (2 giri di formazione dietro SC) e pit lane lungo la linea nera.
   - `run_all_tests.js`: Runner master per l'esecuzione di tutti i test (eseguibile con `npm test`).

3. **Integrazione Web & Android (`app/src/main/assets/live_timing/`)**:
   - `index.html`: Hub universale di visualizzazione live timing che accetta il circuito tramite query string (`?circuit=baku` o `?circuit=sepang`) o JavaScript Bridge.
   - `track_map_baku.html`: Modulo Live Timing autonomo per Baku City Circuit.
   - `track_map.html`: Modulo Live Timing autonomo per Sepang International Circuit.
   - `LiveTrackMapFragment.java`: Fragment Android con `WebView` hardware-accelerated, rilevamento automatico del circuito dall'evento corrente e bridge bidirezionale `FastestLapBridge`.
   - `LivePagerAdapter.java` e `LiveActivity.java`: Tab `MAPPA LIVE` integrata nel pager principale con pulsante di accesso rapido.

4. **Server Web Locale di Sviluppo**:
   - Avvio: `node signalr_test_suite/serve.js`
   - Accesso: `http://localhost:8080` (reindirizza all'hub di selezione o al circuito attivo)

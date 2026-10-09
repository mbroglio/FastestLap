const fs = require('fs');
const path = require('path');

const config = require('../src/config');
const { DRIVER_LAPS } = require('../src/driver_laps');
const { DRIVER_STINTS } = require('../src/driver_stints');
const { RACE_CONTROL_MESSAGES } = require('../src/race_control_events');
const raceModel = require('../src/race_model');

const dataDir = path.join(__dirname, '..', 'data');

const SEPANG_INCIDENTS = [
  {
    id: "bottas_engine_t9",
    driver: "77",
    driverCode: "BOT",
    startSec: 1050.0,
    type: "STOPPED_RETIRED",
    trackSec: 42.5,
    targetPx: 1075.0,
    targetPy: 635.0,
    turnNumber: 9,
    locationName: "Curva 9 — Bordo Pista Fuori Traiettoria (Avaria Motore)",
    reason: "Engine Failure (DNF)",
    transitionDuration: 3.5
  },
  {
    id: "sainz_lockup_t1",
    driver: "55",
    driverCode: "SAI",
    startSec: 1480.0,
    durationSec: 14.0,
    type: "OFF_TRACK_REJOIN",
    trackSec: 4.5,
    targetPx: 120.0,
    targetPy: 460.0,
    turnNumber: 1,
    locationName: "Curva 1 — Via di Fuga Asfaltata (Lungo in Staccata & Rientro)",
    reason: "Lungo in frenata Curva 1",
    entryDuration: 3.0,
    maneuverDuration: 8.0,
    rejoinDuration: 3.0
  },
  {
    id: "albon_hydraulics_t4",
    driver: "23",
    driverCode: "ALB",
    startSec: 4760.0,
    type: "STOPPED_RETIRED",
    trackSec: 18.0,
    targetPx: 785.0,
    targetPy: 215.0,
    turnNumber: 4,
    locationName: "Curva 4 — Via di Fuga (Guasto Idraulico)",
    reason: "Hydraulics (DNF)",
    transitionDuration: 3.0
  },
  {
    id: "russell_powerunit_t15",
    driver: "63",
    driverCode: "RUS",
    startSec: 5840.0,
    type: "STOPPED_RETIRED",
    trackSec: 88.0,
    targetPx: 825.0,
    targetPy: 565.0,
    turnNumber: 15,
    locationName: "Curva 15 — Bordo Pista Fuori Traiettoria (Fumo Power Unit)",
    reason: "Power Unit Smoke (DNF)",
    transitionDuration: 3.0
  }
];

const raceEvents = [
  { timeSec: 0, text: '🟡 08:33:00 — Inizio Sessione: Giro di Formazione 1 dietro Safety Car (Pista Bagnata)' },
  { timeSec: 108, text: '🟡 08:34:48 — Giro di Formazione 2: Gruppo compatto dietro Safety Car in preparazione allo start' },
  { timeSec: 202, text: '🏎️ 08:36:22 — Schieramento in Griglia: Vetture posizionate nelle caselle di partenza' },
  { timeSec: 217, text: '⚡ 08:36:37 — STANDING START: Semafori spenti! Partenza da fermo! Verstappen scatta dalla Pole!' },
  { timeSec: 250, text: '🔧 08:37:10 (Giro 1): Sergio Pérez rientra ai box per montare gomme Intermedie a causa dell\'asfalto bagnato.' },
  { timeSec: 405, text: '🔧 08:39:45 (Giro 2): Sosta ai box per Piastri, Bortoleto, Stroll e Hülkenberg.' },
  { timeSec: 1050, text: '🛑 08:50:30 (Giro 7): Problema al motore per Valtteri Bottas (#77 Cadillac) - RITIRO (DNF)!' },
  { timeSec: 1156, text: '🟡 08:52:16 (Giro 9): FIA Race Control: SAFETY CAR DEPLOYED per vettura #77 ferma nel secondo settore!' },
  { timeSec: 1550, text: '🟢 08:58:50 (Giro 12): FIA Race Control: SAFETY CAR IN THIS LAP!' },
  { timeSec: 1589, text: '🟢 08:59:29 (Giro 12): SAFETY CAR LIGHTS OFF! Kimi Antonelli guida il gruppo verso il restart!' },
  { timeSec: 1620, text: '🟢 09:00:00 (Giro 13): RIPARTE LA GARA! Bandiera verde, Antonelli al comando inseguito da Verstappen e Russell!' },
  { timeSec: 2460, text: '👑 09:14:00 (Giro 21): Max Verstappen supera Antonelli e si riprende la leadership (P1)!' },
  { timeSec: 3820, text: '🔧 09:36:40 (Giro 33): Secondo pit stop per i battistrada con passaggio alle mescole Hard da asciutto!' },
  { timeSec: 4760, text: '⚠️ 09:52:20 (Giro 41): RITIRO (DNF) per Alexander Albon (#23 Williams) per perdita idraulica.' },
  { timeSec: 4801, text: '🟡 09:53:01 (Giro 43): FIA Race Control: VSC DEPLOYED (Virtual Safety Car)!' },
  { timeSec: 5126, text: '🟡 09:58:26 (Giro 45): FIA Race Control: SAFETY CAR DEPLOYED per recupero vettura di Albon!' },
  { timeSec: 5436, text: '🔄 10:03:36 (Giro 48): FIA Race Control: Le vetture doppiate #55, #5, #11 possono sdoppiarsi!' },
  { timeSec: 5840, text: '💥 10:10:20 (Giro 49): CLAMOROSO COLPO DI SCENA! Fumo dalla Mercedes di George Russell! RITIRO (DNF) dal podio!' },
  { timeSec: 5914, text: '🟢 10:11:34 (Giro 51): FIA Race Control: SAFETY CAR IN THIS LAP! Sprint finale di 4 giri!' },
  { timeSec: 5974, text: '🟢 10:12:34 (Giro 51): SAFETY CAR LIGHTS OFF! Verstappen al comando davanti ad Antonelli e Hamilton!' },
  { timeSec: 6337, text: '⚪ 10:18:37 (Giro 54): Bandiera Bianca: Ultimo giro (55/55) del Gran Premio di Malesia a Sepang!' },
  { timeSec: 6435, text: '🏁 10:20:15 (Giro 55): BANDIERA A SCACCHI! MAX VERSTAPPEN VINCE IL GP DI MALESIA 2026! FASTEST LAP: 1:38.220! Podio: 1° Verstappen, 2° Kimi Antonelli (+2.3s), 3° Lewis Hamilton (+4.9s)!' },
  { timeSec: 6550, text: '🛑 10:22:10 (Parc Fermé): Max Verstappen e tutte le vetture in Parc Fermé. Risultati ufficiali confermati con Jolpica & OpenF1 API.' }
];

console.log('Exporting Sepang Grand Prix data files to signalr_test_suite/data...');

fs.writeFileSync(path.join(dataDir, 'sepang_drivers.json'), JSON.stringify(config.DEFAULT_DRIVERS, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_drivers.json');

fs.writeFileSync(path.join(dataDir, 'sepang_driver_laps.json'), JSON.stringify(DRIVER_LAPS, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_driver_laps.json');

fs.writeFileSync(path.join(dataDir, 'sepang_driver_stints.json'), JSON.stringify(DRIVER_STINTS, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_driver_stints.json');

fs.writeFileSync(path.join(dataDir, 'sepang_race_control_messages.json'), JSON.stringify(RACE_CONTROL_MESSAGES, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_race_control_messages.json');

fs.writeFileSync(path.join(dataDir, 'sepang_retirements.json'), JSON.stringify(raceModel.RETIREMENTS, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_retirements.json');

fs.writeFileSync(path.join(dataDir, 'sepang_keyframes.json'), JSON.stringify(raceModel.DRIVER_KEYFRAMES, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_keyframes.json');

fs.writeFileSync(path.join(dataDir, 'sepang_pit_stops.json'), JSON.stringify(raceModel.PIT_STOPS, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_pit_stops.json');

fs.writeFileSync(path.join(dataDir, 'sepang_incidents.json'), JSON.stringify(SEPANG_INCIDENTS, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_incidents.json');

fs.writeFileSync(path.join(dataDir, 'sepang_race_events.json'), JSON.stringify(raceEvents, null, 2), 'utf-8');
console.log('  ✅ Saved sepang_race_events.json');

console.log('All 9 Sepang data files exported successfully!');

// Importa i moduli necessari (Sintassi V2)
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");
const fs = require('fs'); // <-- 1. AGGIUNTO per leggere file
const path = require('path'); // <-- 2. AGGIUNTO per trovare i file

// Inizializza l'SDK di Firebase Admin
admin.initializeApp();
const db = admin.database(); 

// ------------------------------------------------------------------
const PATH_PILOTI = "drivers"; 
const PATH_COSTRUTTORI = "teams"; 
const TRACKER_NODE_PATH = "app_config/stats_tracker";
// ------------------------------------------------------------------


/**
 * ===================================================================
 * FUNZIONE 1: Aggiornamento Statistiche Post-Gara (per Realtime DB)
 * ===================================================================
 */
exports.updateRaceStats = onSchedule(
  {
    schedule: "every monday 20:00",
    timeZone: "Europe/Rome",
    timeoutSeconds: 180
  },
  async (event) => {
    console.log("Inizio controllo statistiche post-gara...");

    try {
      // --- MODIFICA PER TEST ---
      let response;
      //if (process.env.FUNCTIONS_EMULATOR === 'true') {
        // Modalità Test: Leggi il file locale
        //console.log("--- MODALITÀ EMULATORE: Carico 'results.json' locale ---");
        //const localJson = fs.readFileSync(path.join(__dirname, 'results.json'), 'utf8');
        //response = { data: JSON.parse(localJson) }; // Simuliamo la risposta di axios
      //} else {
        // Modalità Live: Chiama l'API
      response = await axios.get("https://api.jolpi.ca/ergast/f1/current/last/results/?format=json"); // URL Reale
      //response = await axios.get("https://firebasestorage.googleapis.com/v0/b/fastest-lap-ac540.firebasestorage.app/o/results.json?alt=media&token=4803df85-af48-4311-88f2-ab03a4d3d31a");
      //}
      // --- FINE MODIFICA ---

      const resultsData = response.data.MRData;
      
      // ... (Tutta la logica di controllo "lock" e aggiornamento è IDENTICA a prima) ...
      
      if (parseInt(resultsData.total) === 0 || !resultsData.RaceTable.Races[0]) {
        console.log("Nessuna gara recente trovata. Termino.");
        return null;
      }
      const raceInfo = resultsData.RaceTable.Races[0];
      const newSeason = parseInt(raceInfo.season);
      const newRound = parseInt(raceInfo.round);
      const trackerRef = db.ref(TRACKER_NODE_PATH);
      const trackerSnapshot = await trackerRef.once("value");
      const trackerData = trackerSnapshot.val() || {};
      const lastSeason = trackerData.last_season_updated || 0;
      const lastRound = trackerData.last_race_updated || 0;
      if (newSeason < lastSeason || (newSeason === lastSeason && newRound <= lastRound)) {
        console.log(`Gara ${newSeason}-${newRound} già processata. Termino.`);
        return null;
      }
      console.log(`NUOVA GARA RILEVATA: ${newSeason}-${newRound}. Inizio aggiornamento...`);
      const multiPathUpdates = {};
      const results = raceInfo.Results;
      const constructorUpdates = {};
      for (const result of results) {
        const driverId = result.Driver.driverId;
        const constructorId = result.Constructor.constructorId;
        const position = parseInt(result.position);
        const driverRef = db.ref(`${PATH_PILOTI}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");
        console.log(`Elaborazione risultato per il pilota: ${driverId}`);
        if (driverSnapshot.exists()) {
          const driverData = driverSnapshot.val();
          if (position <= 3) {
            const currentPodiums = parseInt(driverData.podiums) || 0;
            multiPathUpdates[`${PATH_PILOTI}/${driverId}/podiums`] = (currentPodiums + 1).toString();
          }
          const bestResultString = driverData.best_result || "99 (x0)";
          console.log(`Best result attuale per ${driverId}: ${bestResultString}`);
          let driverBestResult;
          if (typeof bestResultString !== "string") {
            driverBestResult = bestResultString.toString();
          } else {
            driverBestResult = bestResultString;
          }

          const currentBestPos = parseInt(driverBestResult.match(/(\d+)/)[0]);
          let currentBestCount = 0;
          const countMatch = driverBestResult.match(/x(\d+)/);
          if (countMatch && countMatch[1]) {
             currentBestCount = parseInt(countMatch[1]);
          }
          if (position < currentBestPos) {
            multiPathUpdates[`${PATH_PILOTI}/${driverId}/best_result`] = `${position} (x1)`; 
          } else if (position === currentBestPos && position <= 99) {
            multiPathUpdates[`${PATH_PILOTI}/${driverId}/best_result`] = `${position} (x${currentBestCount + 1})`; 
          }
        }
        if (!constructorUpdates[constructorId]) {
          constructorUpdates[constructorId] = { podiums: 0, wins: 0 };
        }
        if (position === 1) {
          constructorUpdates[constructorId].wins += 1;
        }
        if (position <= 3) {
          constructorUpdates[constructorId].podiums += 1;
        }
      } 
      for (const constructorId in constructorUpdates) {
        const updates = constructorUpdates[constructorId];
        if (updates.podiums > 0 || updates.wins > 0) {
          const constructorRef = db.ref(`${PATH_COSTRUTTORI}/${constructorId}`);
          
          console.log(`Elaborazione aggiornamenti per il costruttore: ${constructorId}`);
          console.log(`Aggiornamenti da applicare: ${JSON.stringify(updates)} per ${constructorId}`);

          const constructorSnapshot = await constructorRef.once("value");

          // --- BLOCCO DI DEBUG ---
          if (constructorSnapshot.exists()) {
            console.log(`SUCCESSO: Trovato '${constructorId}' nel DB. Preparo l'aggiornamento.`); // <-- LOG DI SUCCESSO
            const constructorData = constructorSnapshot.val();
            
            if (updates.wins > 0) {
              const currentWins = parseInt(constructorData.wins) || 0;
              multiPathUpdates[`${PATH_COSTRUTTORI}/${constructorId}/wins`] = (currentWins + updates.wins).toString();
            }
            if (updates.podiums > 0) {
              const currentPodiums = parseInt(constructorData.podiums) || 0;
              multiPathUpdates[`${PATH_COSTRUTTORI}/${constructorId}/podiums`] = (currentPodiums + updates.podiums).toString();
            }
          } else {
            // Se vedi questo log, abbiamo trovato il problema
            console.warn(`ATTENZIONE: Costruttore non trovato! ID JSON '${constructorId}' non esiste in Realtime Database. Percorso cercato: ${constructorRef.toString()}`);
          }
          // --- FINE BLOCCO DI DEBUG ---
        }
      }
      multiPathUpdates[`${TRACKER_NODE_PATH}/last_season_updated`] = newSeason;
      multiPathUpdates[`${TRACKER_NODE_PATH}/last_race_updated`] = newRound;
      await db.ref().update(multiPathUpdates);
      console.log(`Aggiornamento post-gara ${newSeason}-${newRound} completato.`);
    } catch (error) {
      console.error("Errore durante l'aggiornamento delle statistiche:", error);
    }
    return null;
  }
);

/**
 * ===================================================================
 * FUNZIONE 2: Aggiornamento Campionati (per Realtime DB)
 * ===================================================================
 */
exports.updateChampionships = onSchedule(
  {
    schedule: "0 12 15 12 *", // 15 Dicembre
    timeZone: "Europe/Rome",
    timeoutSeconds: 180
  },
  async (event) => {
    console.log("Inizio controllo campionati di fine stagione...");
    
    try {
      // --- MODIFICA PER TEST (PILOTI) ---
      let driverStandingsRes;
      //if (process.env.FUNCTIONS_EMULATOR === 'true') {
        //console.log("--- MODALITÀ EMULATORE: Carico 'driverstandings.json' locale ---");
        //const localJson = fs.readFileSync(path.join(__dirname, 'driverstandings.json'), 'utf8');
        //driverStandingsRes = { data: JSON.parse(localJson) };
      //} else {
      driverStandingsRes = await axios.get("https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json"); // URL Reale
      //}
      // --- FINE MODIFICA ---

      const standingsData = driverStandingsRes.data.MRData.StandingsTable;
      const newSeason = parseInt(standingsData.season);
      const trackerRef = db.ref(TRACKER_NODE_PATH);
      const trackerSnapshot = await trackerRef.once("value");
      const trackerData = trackerSnapshot.val() || {};
      const lastChampSeason = trackerData.last_champ_season || 0;
      if (newSeason <= lastChampSeason) {
        console.log(`Campionati stagione ${newSeason} già processati. Termino.`);
        return null;
      }
      console.log(`NUOVA STAGIONE RILEVATA: ${newSeason}. Aggiorno campionati...`);
      const multiPathUpdates = {};
      const driverStandings = standingsData.StandingsLists[0].DriverStandings;
      const driverWinner = driverStandings.find(d => parseInt(d.position) === 1);
      if (driverWinner) {
        const driverId = driverWinner.Driver.driverId;
        const driverRef = db.ref(`${PATH_PILOTI}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");
        if (driverSnapshot.exists()) {
            const currentChamps = parseInt(driverSnapshot.val().championships) || 0;
            multiPathUpdates[`${PATH_PILOTI}/${driverId}/championships`] = (currentChamps + 1).toString();
        }
      }

      // --- MODIFICA PER TEST (COSTRUTTORI) ---
      let constructorStandingsRes;
      //if (process.env.FUNCTIONS_EMULATOR === 'true') {
        //console.log("--- MODALITÀ EMULATORE: Carico 'constructorstandings.json' locale ---");
        //const localJson = fs.readFileSync(path.join(__dirname, 'constructorstandings.json'), 'utf8');
        //constructorStandingsRes = { data: JSON.parse(localJson) };
      //} else {
      constructorStandingsRes = await axios.get("https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json"); // URL Reale
      //}
      // --- FINE MODIFICA ---

      const constructorStandings = constructorStandingsRes.data.MRData.StandingsTable.StandingsLists[0].ConstructorStandings;
      const constructorWinner = constructorStandings.find(c => parseInt(c.position) === 1);
      
      if (constructorWinner) {
        const constructorId = constructorWinner.Constructor.constructorId;
        const constructorRef = db.ref(`${PATH_COSTRUTTORI}/${constructorId}`);
        const constructorSnapshot = await constructorRef.once("value");
        if (constructorSnapshot.exists()) {
            const currentChamps = parseInt(constructorSnapshot.val().world_championships) || 0;
            multiPathUpdates[`${PATH_COSTRUTTORI}/${constructorId}/world_championships`] = (currentChamps + 1).toString();
        }
      }
      multiPathUpdates[`${TRACKER_NODE_PATH}/last_champ_season`] = newSeason;
      await db.ref().update(multiPathUpdates);
      console.log(`Aggiornamento campionati stagione ${newSeason} completato.`);
    } catch (error) {
      console.error("Errore durante l'aggiornamento dei campionati:", error);
    }
    return null;
  }
);
// Importa i moduli necessari (Sintassi V2)
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");

// Inizializza l'SDK di Firebase Admin
admin.initializeApp();
const db = admin.firestore();

// COLLEZIONI------------------------------------------------------------------
const COLL_PILOTI = "drivers";
const COLL_COSTRUTTORI = "teams";
// ------------------------------------------------------------------


/**
 * ===================================================================
 * FUNZIONE 1: Aggiornamento Statistiche Post-Gara (V2)
 * ===================================================================
 */
exports.updateRaceStats = onSchedule(
  {
    schedule: "every monday 22:00",
    timeZone: "Europe/Rome",
  },
  async (event) => {
    console.log("Inizio aggiornamento statistiche post-gara...");

    try {
      // 1. Chiamata all'API
      const response = await axios.get(
        "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json" // ⚠️ SOSTITUISCI CON IL TUO URL 1
      );

      // Il codice è basato sulla struttura di results.json
      const resultsData = response.data.MRData;
      
      if (parseInt(resultsData.total) === 0 || !resultsData.RaceTable.Races[0]) {
        console.log("Nessuna gara recente trovata. Termino.");
        return null;
      }

      const results = resultsData.RaceTable.Races[0].Results;
      const batch = db.batch();
      
      const constructorUpdates = {}; 

      // --- 1° CICLO: Aggiorna Piloti e Aggrega Costruttori ---
      for (const result of results) {
        const driverId = result.Driver.driverId;
        const constructorId = result.Constructor.constructorId;
        const position = parseInt(result.position);

        const driverRef = db.collection(COLL_PILOTI).doc(driverId);

        // --- Logica di aggiornamento Pilota ---
        const driverDoc = await driverRef.get();
        if (driverDoc.exists) {
          const driverData = driverDoc.data();
          const updates = {};

          if (position <= 3) {
            const currentPodiums = parseInt(driverData.podiums) || 0;
            updates.podiums = (currentPodiums + 1).toString();
          }

          const bestResultString = driverData.best_result || "99 (x0)";
          const currentBestPos = parseInt(bestResultString.match(/(\d+)/)[0]); 
          let currentBestCount = 0;
          
          const countMatch = bestResultString.match(/x(\d+)/);
          if (countMatch && countMatch[1]) {
             currentBestCount = parseInt(countMatch[1]);
          }

          if (position < currentBestPos) {
            updates.best_result = `${position} (x1)`; 
          } else if (position === currentBestPos && position <= 99) {
            updates.best_result = `${position} (x${currentBestCount + 1})`; 
          }
          
          if (Object.keys(updates).length > 0) {
            batch.update(driverRef, updates);
            console.log(`Aggiornamento Pilota ${driverId}:`, updates);
          }
        }

        // --- Logica di aggregazione Costruttore ---
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

      // --- 2° CICLO: Applica aggiornamenti Costruttori ---
      for (const constructorId in constructorUpdates) {
        const updates = constructorUpdates[constructorId];
        if (updates.podiums > 0 || updates.wins > 0) {
          const constructorRef = db.collection(COLL_COSTRUTTORI).doc(constructorId);
          const constructorDoc = await constructorRef.get();
          
          if (constructorDoc.exists) {
            const constructorData = constructorDoc.data();
            const finalUpdates = {};
            
            if (updates.wins > 0) {
              const currentWins = parseInt(constructorData.wins) || 0;
              finalUpdates.wins = (currentWins + updates.wins).toString();
            }
            if (updates.podiums > 0) {
              const currentPodiums = parseInt(constructorData.podiums) || 0;
              finalUpdates.podiums = (currentPodiums + updates.podiums).toString();
            }
            
            batch.update(constructorRef, finalUpdates);
            console.log(`Aggiorn. Costruttore ${constructorId}:`, finalUpdates);
          }
        }
      } 

      // --- 3. Esegui tutti gli aggiornamenti ---
      await batch.commit();
      console.log("Aggiornamento post-gara completato.");
      
    } catch (error) {
      console.error("Errore durante l'aggiornamento delle statistiche:", error);
    }
    return null;
  }
);

/**
 * ===================================================================
 * FUNZIONE 2: Aggiornamento Campionati (V2)
 * ===================================================================
 */
exports.updateChampionships = onSchedule(
  {
    schedule: "0 12 15 12 *", // cron: "alle 12:00 del 15 Dicembre"
    timeZone: "Europe/Rome",
  },
  async (event) => {
    console.log("Inizio controllo campionati di fine stagione...");
    const batch = db.batch();

    try {
      // --- 1. Controllo Campionato Piloti ---
      const driverStandingsRes = await axios.get(
        "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json" // ⚠️ SOSTITUISCI CON IL TUO URL 2
      );
      
      // Il codice è basato sulla struttura di driverstandings.json
      const driverStandings = driverStandingsRes.data.MRData.StandingsTable.StandingsLists[0].DriverStandings;
      const driverWinner = driverStandings.find(d => parseInt(d.position) === 1);
      
      if (driverWinner) {
        const driverId = driverWinner.Driver.driverId;
        const driverRef = db.collection(COLL_PILOTI).doc(driverId);
        const driverDoc = await driverRef.get();
        
        if (driverDoc.exists) {
            const currentChamps = parseInt(driverDoc.data().championships) || 0;
            batch.update(driverRef, {
              championships: (currentChamps + 1).toString()
            });
            console.log(`Campione Piloti: ${driverId}. Incremento campionati.`);
        }
      }

      // --- 2. Controllo Campionato Costruttori ---
      const constructorStandingsRes = await axios.get(
        "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json" // ⚠️ SOSTITUISCI CON IL TUO URL 3
      );
      
      // Il codice è basato sulla struttura di constructorstandings.json
      const constructorStandings = constructorStandingsRes.data.MRData.StandingsTable.StandingsLists[0].ConstructorStandings;
      const constructorWinner = constructorStandings.find(c => parseInt(c.position) === 1);
      
      if (constructorWinner) {
        const constructorId = constructorWinner.Constructor.constructorId;
        const constructorRef = db.collection(COLL_COSTRUTTORI).doc(constructorId);
        const constructorDoc = await constructorRef.get();
        
        if (constructorDoc.exists) {
            const currentChamps = parseInt(constructorDoc.data().world_championships) || 0;
            batch.update(constructorRef, {
              world_championships: (currentChamps + 1).toString()
            });
            console.log(`Campione Costruttori: ${constructorId}. Incremento campionati.`);
        }
      }

      await batch.commit();
      console.log("Aggiornamento campionati completato.");

    } catch (error) {
      console.error("Errore durante l'aggiornamento dei campionati:", error);
    }
    return null;
  }
);
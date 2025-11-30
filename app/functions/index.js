const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");

// Importa la logica dai file separati
const f1Logic = require("./f1_logic");
const juniorLogic = require("./junior_categories_logic");

// Inizializza Firebase UNA sola volta qui
admin.initializeApp();
const db = admin.database();

/**
 * SCHEDULER 1: F1 Race Stats (Lun 20:00)
 */
exports.updateRaceStats = onSchedule(
  {
    schedule: "every monday 20:00",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
  },
  async (event) => {
    try {
        await f1Logic.executeRaceStatsUpdate(db);
    } catch (error) {
        console.error("Critical Error in updateRaceStats:", error);
        throw error;
    }
  }
);

/**
 * SCHEDULER 2: F1 Championships (15 Dic 12:00)
 */
exports.updateChampionships = onSchedule(
  {
    schedule: "0 12 15 12 *",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
  },
  async (event) => {
    try {
        await f1Logic.executeChampionshipsUpdate(db);
    } catch (error) {
        console.error("Critical Error in updateChampionships:", error);
        throw error;
    }
  }
);

/**
 * SCHEDULER 3: Junior Series Update (Sun & Mon 13:00)
 */
exports.updateJuniorSeries = onSchedule(
  {
    schedule: "0 13 * * 0,1",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
  },
  async (event) => {
    try {
        await juniorLogic.executeJuniorSeriesUpdate(db);
    } catch (error) {
        console.error("Critical Error in updateJuniorSeries:", error);
        throw error;
    }
  }
);

/**
 * SCHEDULER 4: Junior Series Reset (31 Dic 22:00)
 */
exports.resetJuniorSeries = onSchedule(
  {
    schedule: "0 22 31 12 *",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
  },
  async (event) => {
    try {
        await juniorLogic.executeJuniorReset(db);
    } catch (error) {
        console.error("Error in resetJuniorSeries:", error);
    }
  }
);
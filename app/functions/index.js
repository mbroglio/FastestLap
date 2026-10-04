const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

// Importa la logica dai file separati
const f1Logic = require("./f1_logic");
const juniorLogic = require("./junior_categories_logic");
const notificationLogic = require("./notification_logic");
const calendarLogic = require("./calendar_logic");

// Inizializza Firebase UNA sola volta qui
admin.initializeApp({
  databaseURL: "https://fastest-lap-ac540-default-rtdb.europe-west1.firebasedatabase.app"
});
const db = admin.database();
const messaging = admin.messaging();

/**
 * SCHEDULER 1: F1 Race Stats & Post-Race Penalty Verification (Dynamic per DB Calendar)
 * Attinge al calendario F1 memorizzato nel DB (app_config/db_calendar/f1/{year}).
 * Si attiva a circa 2 ore dall'orario di inizio della gara (T+2h).
 * Se l'aggiornamento fallisce o i risultati sono ancora pendenti su Jolpica, ritenta a ogni ora per le successive 12 ore.
 * Se l'aggiornamento va a buon fine, nei successivi controlli orari (durante le 12 ore di finestra)
 * verifica che non ci siano state variazioni a causa di eventuali sanzioni/penalità inflitte post-gara.
 */
exports.updateRaceStats = onSchedule(
  {
    schedule: "every 30 minutes",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
    retryConfig: {
      retryCount: 3,
      minBackoffDuration: "60s",
      maxBackoffDuration: "300s"
    }
  },
  async (event) => {
    try {
        await f1Logic.executeScheduledRaceStatsCheck(db, messaging);
    } catch (error) {
        console.error("Critical Error in updateRaceStats:", error);
    }
  }
);

/**
 * SCHEDULER 2: F1 Championships (12 Dic 12:00)
 */
exports.updateChampionships = onSchedule(
  {
    schedule: "0 12 12 12 *",
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
 * SCHEDULER 3: Junior Series Update (Sun & Mon 14:00)
 */
exports.updateJuniorSeries = onSchedule(
  {
    schedule: "0 14 * * 0,1", // every Sunday and Monday at 14:00
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

/**
 * SCHEDULER 5: F1 News Push Notification Check (Every 15 minutes)
 * Scrapes F1 RSS feed and pushes to topic "news" when a new article appears.
 */
exports.checkAndPushNews = onSchedule(
  {
    schedule: "every 15 minutes",
    timeZone: "Europe/Rome",
    timeoutSeconds: 120,
    retryConfig: {
      retryCount: 3,
      minBackoffDuration: "60s",
      maxBackoffDuration: "300s"
    }
  },
  async (event) => {
    try {
      await notificationLogic.executeNewsCheckAndPush(db, messaging);
    } catch (error) {
      console.error("Error in checkAndPushNews:", error);
    }
  }
);

/**
 * SCHEDULER 6: F1 & Junior Categories Session Reminders Check (Every 5 minutes)
 * Checks stored Firebase calendars and sends 30-min and 5-min alerts to dedicated topics.
 */
exports.checkAndPushSessions = onSchedule(
  {
    schedule: "every 5 minutes",
    timeZone: "Europe/Rome",
    timeoutSeconds: 180,
    retryConfig: {
      retryCount: 3,
      minBackoffDuration: "60s",
      maxBackoffDuration: "300s"
    }
  },
  async (event) => {
    try {
      await notificationLogic.executeSessionCheckAndPush(db, messaging);
    } catch (error) {
      console.error("Error in checkAndPushSessions:", error);
    }
  }
);

/**
 * SCHEDULER 7: Daily Calendar Synchronization (Daily at 04:00 Rome time)
 * Downloads current year calendar for F1 (Jolpica) and Junior categories (F2, F3)
 * into Firebase RTDB under app_config/db_calendar/{f1,f2,f3}/{year}.
 */
exports.syncCalendars = onSchedule(
  {
    schedule: "0 4 * * *",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
    retryConfig: {
      retryCount: 3,
      minBackoffDuration: "60s",
      maxBackoffDuration: "300s"
    }
  },
  async (event) => {
    try {
      await calendarLogic.syncAllCalendars(db);
    } catch (error) {
      console.error("Error in syncCalendars:", error);
    }
  }
);

/**
 * HTTP ENDPOINT: Manual Calendar Sync Trigger
 * Allows immediate manual sync of F1, F2, and F3 calendars from browser or curl.
 * GET https://.../syncCalendarsNow
 */
exports.syncCalendarsNow = onRequest(
  {
    cors: true,
    timeoutSeconds: 300
  },
  async (req, res) => {
    try {
      const result = await calendarLogic.syncAllCalendars(db);
      res.status(200).json({ status: "success", result });
    } catch (error) {
      console.error("Error in syncCalendarsNow:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);

/**
 * HTTP ENDPOINT: Manual Test Push Notification
 * Allows manual testing from browser or curl:
 * e.g. GET https://.../sendTestPush?topic=news or ?topic=sessions
 */
exports.sendTestPush = onRequest(
  {
    cors: true,
    timeoutSeconds: 60
  },
  async (req, res) => {
    try {
      const topic = req.query.topic || req.body?.topic || "news";
      const title = req.query.title || req.body?.title || null;
      const body = req.query.body || req.body?.body || null;

      const result = await notificationLogic.sendTestNotification(messaging, topic, title, body);
      res.status(200).json({ status: "success", result });
    } catch (error) {
      console.error("Error in sendTestPush:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);

/**
 * HTTP ENDPOINT: Manual Driver Season Stats Sync Trigger
 * Allows immediate manual sync of all drivers' season_stats from browser or curl:
 * GET https://.../syncDriverSeasonStatsNow (optionally ?season=2026)
 */
exports.syncDriverSeasonStatsNow = onRequest(
  {
    cors: true,
    timeoutSeconds: 300
  },
  async (req, res) => {
    try {
      const season = req.query.season || req.body?.season || null;
      const result = await f1Logic.syncDriverSeasonStats(db, season);
      res.status(200).json({ status: "success", result });
    } catch (error) {
      console.error("Error in syncDriverSeasonStatsNow:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);
exports.updateDriverSeasonStatsNow = exports.syncDriverSeasonStatsNow;

/**
 * HTTP ENDPOINT: Manual F1 Post-Race Stats Update Trigger
 * Allows immediate manual trigger of post-race stats update from browser or curl:
 * GET https://.../updateRaceStatsNow (optionally ?season=2026&round=15&force=true)
 */
exports.updateRaceStatsNow = onRequest(
  {
    cors: true,
    timeoutSeconds: 300
  },
  async (req, res) => {
    try {
      const season = req.query.season ? parseInt(req.query.season) : null;
      const round = req.query.round ? parseInt(req.query.round) : null;
      const force = req.query.force === "true" || req.query.force === true || (!season && !round);

      const result = await f1Logic.executeScheduledRaceStatsCheck(db, messaging, {
        season,
        round,
        force
      });
      res.status(200).json({ status: "success", result });
    } catch (error) {
      console.error("Error in updateRaceStatsNow:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);

/**
 * HTTP ENDPOINT: Manual Constructor Season Stats Sync Trigger
 * Allows immediate manual sync of all constructors' season_stats from browser or curl:
 * GET https://.../syncConstructorSeasonStatsNow (optionally ?season=2026 or ?inspect=true)
 */
exports.syncConstructorSeasonStatsNow = onRequest(
  {
    cors: true,
    timeoutSeconds: 300
  },
  async (req, res) => {
    try {
      if (req.query.inspect) {
        const snap = await db.ref("teams").once("value");
        const val = snap.val() || {};
        const summary = {};
        for (const k of Object.keys(val)) {
          summary[k] = val[k].season_stats;
        }
        return res.status(200).json({ status: "success", season_stats: summary });
      }
      const season = req.query.season || req.body?.season || null;
      const result = await f1Logic.syncConstructorSeasonStats(db, season);
      res.status(200).json({ status: "success", result });
    } catch (error) {
      console.error("Error in syncConstructorSeasonStatsNow:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);

/**
 * HTTP ENDPOINT: Manual Junior Series Update Trigger
 * Allows immediate manual update of Junior categories (F2, F3) from browser or curl:
 * GET https://.../updateJuniorSeriesNow (optionally ?force=true/false)
 */
exports.updateJuniorSeriesNow = onRequest(
  {
    cors: true,
    timeoutSeconds: 300
  },
  async (req, res) => {
    try {
      const force = req.query.force !== "false";
      await juniorLogic.executeJuniorSeriesUpdate(db, { force });
      res.status(200).json({ status: "success", message: "Junior series update executed successfully", force });
    } catch (error) {
      console.error("Error in updateJuniorSeriesNow:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);

/**
 * HTTP ENDPOINT: Manual All Career Stats Verification and Synchronization Trigger
 * Reconciles accurate career stats (podiums, wins, gps_entered) from Jolpica for all drivers and teams
 * and writes them directly to Firebase RTDB.
 * GET https://.../syncAllCareerStatsNow
 */
exports.syncAllCareerStatsNow = onRequest(
  {
    cors: true,
    timeoutSeconds: 540
  },
  async (req, res) => {
    try {
      const report = await f1Logic.syncAllCareerStats(db);
      res.status(200).json({ status: "success", report });
    } catch (error) {
      console.error("Error in syncAllCareerStatsNow:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);

/**
 * HTTP ENDPOINT: Clear Sent Sessions Registry
 * Deletes all entries in app_config/notifications/sent_sessions so the next
 * checkAndPushSessions execution will re-send pending session notifications
 * (useful after fixing the FCM payload or when a session was missed).
 * Optionally pass ?round=16 or ?category=f1 to clear only specific entries.
 * GET https://.../clearSentSessions
 */
exports.clearSentSessions = onRequest(
  {
    cors: true,
    timeoutSeconds: 60
  },
  async (req, res) => {
    try {
      const filterRound = req.query.round || null;
      const filterCategory = req.query.category || null;
      const filterYear = req.query.year || new Date().getFullYear().toString();

      const sentRef = db.ref("app_config/notifications/sent_sessions");
      const snap = await sentRef.once("value");
      const sentData = snap.val() || {};

      const keysToDelete = [];
      for (const key of Object.keys(sentData)) {
        // key format: {year}_{category}_round{round}_{sessionId}_{alertType}
        const matchesYear = key.startsWith(filterYear + "_");
        const matchesRound = !filterRound || key.includes(`_round${filterRound}_`);
        const matchesCategory = !filterCategory || key.includes(`_${filterCategory}_`);
        if (matchesYear && matchesRound && matchesCategory) {
          keysToDelete.push(key);
        }
      }

      const updates = {};
      for (const key of keysToDelete) {
        updates[key] = null;
      }

      if (keysToDelete.length > 0) {
        await sentRef.update(updates);
      }

      console.log(`✅ Cleared ${keysToDelete.length} sent session entries (year=${filterYear}, round=${filterRound || "all"}, category=${filterCategory || "all"})`);
      res.status(200).json({
        status: "success",
        cleared: keysToDelete.length,
        keys: keysToDelete,
        filters: { year: filterYear, round: filterRound, category: filterCategory }
      });
    } catch (error) {
      console.error("Error in clearSentSessions:", error);
      res.status(500).json({ status: "error", message: error.message });
    }
  }
);
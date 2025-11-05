// Import necessary modules
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");

// Initialize Firebase Admin SDK
admin.initializeApp();
// Get a reference to the Realtime Database
const db = admin.database();

// Define constant paths for database nodes
const DRIVERS_PATH = "drivers"; // Root path for all drivers
const TEAMS_PATH = "teams"; // Root path for all teams
const TRACKER_NODE_PATH = "app_config/stats_tracker"; // Path to the lock/tracker node
const RACE_RESULTS_API = "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json";
const DRIVER_STANDINGS_API = "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json";
const CONSTRUCTOR_STANDINGS_API = "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json";

//create a map<String, String> for team id and team name
const TEAM_ID_NAME_MAP = {
  "mercedes": "Mercedes",
  "red_bull": "Red Bull",
  "ferrari": "Ferrari",
  "mclaren": "McLaren",
  "alpine": "Alpine",
  "rb": "Racing Bulls",
  "aston_martin": "Aston Martin",
  "williams": "Williams",
  "haas": "Haas",
  "sauber": "KICK Sauber"
};


/**
 * ===================================================================
 * Function 1: updateRaceStats
 * Runs on a schedule to check for and process new race results.
 * It updates driver/team podiums, wins, and best results.
 * ===================================================================
 */
exports.updateRaceStats = onSchedule(
  {
    schedule: "every monday 20:00", // Schedule trigger
    timeZone: "Europe/Rome",
    timeoutSeconds: 180, // Allow 3 minutes for slow API responses

    retryConfig: {
      retryCount: 20, // Retry up to 20 times on failure
      maxRetryDuration: "86400s", // Maximum retry duration of 24 hours
      minBackoffDuration: "300s", // Minimum backoff of 5 minutes
      maxBackoffDuration: "7200s" // Maximum backoff of 2 hours
    }
  },
  async (event) => {
    console.log("Starting post-race stats check...");

    try {
      // Fetch Last Race Results from the external API
      const response = await axios.get(RACE_RESULTS_API);

      const resultsData = response.data.MRData;

      // Exit if the API returns no race data (e.g., off-season)
      if (parseInt(resultsData.total) === 0 || !resultsData.RaceTable.Races[0]) {
        console.log("No recent race found. Exiting.");
        return null;
      }

      // Lock Check: Prevents processing the same race multiple times.
      const raceInfo = resultsData.RaceTable.Races[0];
      const newSeason = parseInt(raceInfo.season);
      const newRound = parseInt(raceInfo.round);

      // Get the last processed race info from DB
      const trackerRef = db.ref(TRACKER_NODE_PATH);
      const trackerSnapshot = await trackerRef.once("value");
      const trackerData = trackerSnapshot.val() || {};
      const lastSeason = trackerData.last_season_updated || 0;
      const lastRound = trackerData.last_race_updated || 0;

      // If the fetched race is the same or older, stop execution.
      if (newSeason < lastSeason || (newSeason === lastSeason && newRound <= lastRound)) {
        console.log(`Race ${newSeason}-${newRound} already processed. Exiting.`);
        return null;
      }

      console.log(`New race detected: ${newSeason}-${newRound}. Processing...`);

      // Prepare Updates: collect all DB updates in one object for a single atomic write.
      const multiPathUpdates = {};
      const results = raceInfo.Results;

      const constructorUpdates = {}; //aggregate stats for constructors (wins and podiums)

      // 4. Loop 1: Process Drivers and Aggregate Constructors
      for (const result of results) {
        const driverId = result.Driver.driverId;
        const constructorId = result.Constructor.constructorId;
        const position = parseInt(result.position);

        const driverRef = db.ref(`${DRIVERS_PATH}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");

        // --- Process Driver Stats ---
        if (driverSnapshot.exists()) {
          const driverData = driverSnapshot.val();

          // Handle driver podiums
          if (position <= 3) {
            const careerPodiums = parseInt(driverData.podiums) || 0;
            const seasonPodiums = parseInt(driverData.season_podiums) || 0;
            // Update season podiums
            multiPathUpdates[`${DRIVERS_PATH}/${driverId}/season_podiums`] = (seasonPodiums + 1).toString();
            // Update career podiums
            multiPathUpdates[`${DRIVERS_PATH}/${driverId}/podiums`] = (careerPodiums + 1).toString();

            if(position === 1) {
              // Handle season wins
              const seasonWins = parseInt(driverData.season_wins) || 0;
              multiPathUpdates[`${DRIVERS_PATH}/${driverId}/season_wins`] = (seasonWins + 1).toString();
            }
          }

          // Handle 'best_result' field (e.g., "1 (x32)")
          // Ensure the value is a string before using .match()
          const bestResultString = driverData.best_result || "99 (x0)";
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
            // New personal best
            multiPathUpdates[`${DRIVERS_PATH}/${driverId}/best_result`] = `${position} (x1)`;
          } else if (position === currentBestPos && position <= 99) {
            // Matched personal best
            multiPathUpdates[`${DRIVERS_PATH}/${driverId}/best_result`] = `${position} (x${currentBestCount + 1})`;
          }
        }

        // --- Aggregate Constructor Stats ---
        // Initialize if this is the first driver for this team
        if (!constructorUpdates[constructorId]) {
          constructorUpdates[constructorId] = { podiums: 0, wins: 0 };
        }
        // Add stats based on this driver's result
        if (position === 1) {
          constructorUpdates[constructorId].wins += 1;
        }
        if (position <= 3) {
          constructorUpdates[constructorId].podiums += 1;
        }
      }

      // Read Constructor Data and Prepare Updates: apply the aggregated constructor stats to the database
      for (const constructorId in constructorUpdates) {
        const updates = constructorUpdates[constructorId];
        if (updates.podiums > 0 || updates.wins > 0) {
          const constructorRef = db.ref(`${TEAMS_PATH}/${constructorId}`);
          const constructorSnapshot = await constructorRef.once("value");

          if (constructorSnapshot.exists()) {
            const constructorData = constructorSnapshot.val();

            // Apply wins update (if any)
            if (updates.wins > 0) {
              const careerWins = parseInt(constructorData.wins) || 0;
              multiPathUpdates[`${TEAMS_PATH}/${constructorId}/wins`] = (careerWins + updates.wins).toString();

              const seasonWins = parseInt(constructorData.season_wins) || 0;
              multiPathUpdates[`${TEAMS_PATH}/${constructorId}/season_wins`] = (seasonWins + updates.wins).toString();
            }
            // Apply podiums update (handles +1 or +2)
            if (updates.podiums > 0) {
              const currentPodiums = parseInt(constructorData.podiums) || 0;
              multiPathUpdates[`${TEAMS_PATH}/${constructorId}/podiums`] = (currentPodiums + updates.podiums).toString();

              const seasonPodiums = parseInt(constructorData.season_podiums) || 0;
              multiPathUpdates[`${TEAMS_PATH}/${constructorId}/season_podiums`] = (seasonPodiums + updates.podiums).toString();
            }
          } else {
            console.warn(`Warning: Constructor ID '${constructorId}' from API was not found in Database at path: ${constructorRef.toString()}`);
          }
        }
      }

      multiPathUpdates[`${TRACKER_NODE_PATH}/last_season_updated`] = newSeason;
      multiPathUpdates[`${TRACKER_NODE_PATH}/last_race_updated`] = newRound;

      // Perform one atomic update for all changes
      await db.ref().update(multiPathUpdates);
      console.log(`Post-race update for ${newSeason}-${newRound} complete.`);
      return null; // Acknowledge the schedule trigger

    } catch (error) {
      console.error("Error during 'updateRaceStats':", error);
      throw error; // Rethrow to trigger retry logic
    }
  }
);

/**
 * ===================================================================
 * Function 2: updateChampionships
 * Runs once at the end of the year to update championship totals.
 * Updates driver/team histories and resets season stats.
 * ===================================================================
 */
exports.updateChampionships = onSchedule(
  {
    schedule: "0 12 20 12 *", // December 20th at 12:00 
    timeZone: "Europe/Rome",
    timeoutSeconds: 180,

    retryConfig: {
      retryCount: 20, // Retry up to 20 times on failure
      maxRetryDuration: "86400s", // Maximum retry duration of 24 hours
      minBackoffDuration: "300s", // Minimum backoff of 5 minutes
      maxBackoffDuration: "7200s" // Maximum backoff of 2 hours
    }
  },
  async (event) => {
    console.log("Starting end-of-season championship check...");

    try {
      // Fetch Standings
      const driverStandingsRes = await axios.get(DRIVER_STANDINGS_API);

      const standingsData = driverStandingsRes.data.MRData.StandingsTable;
      const newSeason = parseInt(standingsData.season);

      // Lock Check
      const trackerRef = db.ref(TRACKER_NODE_PATH);
      const trackerSnapshot = await trackerRef.once("value");
      const trackerData = trackerSnapshot.val() || {};
      const lastChampSeason = trackerData.last_champ_season || 0;

      if (newSeason <= lastChampSeason) {
        console.log(`Championships for season ${newSeason} already processed. Exiting.`);
        return null;
      }

      console.log(`New season detected: ${newSeason}. Updating champions...`);
      const multiPathUpdates = {};

      // DRIVERS
      const driverStandings = standingsData.StandingsLists[0].DriverStandings;
    
      for(const driver of driverStandings) {
        const driverId = driver.Driver.driverId;
        const driverRef = db.ref(`${DRIVERS_PATH}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");

        if (driverSnapshot.exists()) {
          const driverData = driverSnapshot.val();

          //creating new history entry for the season
         const driverTeams = driver.Constructors.map(constructor => {
           return TEAM_ID_NAME_MAP[constructor.constructorId] || constructor.name; // Use map, fallback to API name
         }).join(' / ');
          const season_wins = parseInt(driverData.season_wins) || 0;
          const season_podiums = parseInt(driverData.season_podiums) || 0;

          const newHistoryEntry = {
            year: newSeason.toString(),
            position: driver.position,
            points: driver.points,
            team: driverTeams,
            wins: season_wins,
            podiums: season_podiums
          };

          let history = driverData.driver_history || [];
          history.push(newHistoryEntry);
          if(history.length > 10) {
            history = history.slice(1);
          }
          multiPathUpdates[`${DRIVERS_PATH}/${driverId}/driver_history`] = history;

          // Reset season stats
          multiPathUpdates[`${DRIVERS_PATH}/${driverId}/season_wins`] = "0";
          multiPathUpdates[`${DRIVERS_PATH}/${driverId}/season_podiums`] = "0";

          // Update Driver Champion
          if (driver.position === "1") {
            const currentChamps = parseInt(driverSnapshot.val().championships) || 0;
            multiPathUpdates[`${DRIVERS_PATH}/${driverId}/championships`] = (currentChamps + 1).toString();
          }

        }
      }


      // CONSTRUCTORS
      const constructorStandingsRes = await axios.get(CONSTRUCTOR_STANDINGS_API);
      const constructorStandings = constructorStandingsRes.data.MRData.StandingsTable.StandingsLists[0].ConstructorStandings;

      for(const constructor of constructorStandings) {
        const constructorId = constructor.Constructor.constructorId;
        const constructorRef = db.ref(`${TEAMS_PATH}/${constructorId}`);
        const constructorSnapshot = await constructorRef.once("value");

        if (constructorSnapshot.exists()) {
          const constructorData = constructorSnapshot.val();

          //creating new history entry for the season
          const season_wins = parseInt(constructorData.season_wins) || 0;
          const season_podiums = parseInt(constructorData.season_podiums) || 0;

          const newHistoryEntry = {
            year: newSeason.toString(),
            position: constructor.position,
            points: constructor.points,
            wins: season_wins,
            podiums: season_podiums
          };

          let history = constructorData.team_history || [];
          history.push(newHistoryEntry);
          if(history.length > 10) {
            history = history.slice(1);
          }
          multiPathUpdates[`${TEAMS_PATH}/${constructorId}/team_history`] = history;

          // Reset season stats
          multiPathUpdates[`${TEAMS_PATH}/${constructorId}/season_wins`] = "0";
          multiPathUpdates[`${TEAMS_PATH}/${constructorId}/season_podiums`] = "0";

          // Update Constructor Champion
          if (constructor.position === "1") {
            const currentChamps = parseInt(constructorSnapshot.val().world_championships) || 0;
            multiPathUpdates[`${TEAMS_PATH}/${constructorId}/world_championships`] = (currentChamps + 1).toString();
          }
        }
      }

      // Update the tracker lock for this season
      multiPathUpdates[`${TRACKER_NODE_PATH}/last_champ_season`] = newSeason;

      await db.ref().update(multiPathUpdates);
      console.log(`Championship update for season ${newSeason} complete.`);
      return null; // Acknowledge the schedule trigger

    } catch (error) {
      console.error("Error during 'updateChampionships':", error);
      throw error; // Rethrow to trigger retry logic
    }
  }
);
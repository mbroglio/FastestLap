// Import necessary modules
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.database();

// --- CONSTANTS ---
const PATHS = {
  DRIVERS: "drivers",
  TEAMS: "teams",
  CIRCUITS: "circuits",
  TRACKER: "app_config/stats_tracker",
  CALENDAR: "app_config/calendar",
  TEAM_MAP: "app_config/team_id_name_map",
  DRIVER_MAP: "app_config/driver_id_name_map"
};

const APIS = {
  RACE_RESULTS: "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json",
  DRIVER_STANDINGS: "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json",
  CONSTRUCTOR_STANDINGS: "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json"
};

// Global maps (populated at runtime)
let TEAM_ID_NAME_MAP = {};
let DRIVER_ID_NAME_MAP = {};

/**
 * ===================================================================
 * HELPER FUNCTIONS: DATA FETCHING & UTILS
 * ===================================================================
 */

// Helper to load ID mappings from DB
async function loadMappings() {
  const [teamSnap, driverSnap] = await Promise.all([
    db.ref(PATHS.TEAM_MAP).once("value"),
    db.ref(PATHS.DRIVER_MAP).once("value")
  ]);
  
  TEAM_ID_NAME_MAP = teamSnap.val() || {};
  DRIVER_ID_NAME_MAP = driverSnap.val() || {};
}

// Helper to validate if a race needs processing
async function shouldProcessRace(newSeason, newRound) {
  const trackerSnap = await db.ref(PATHS.TRACKER).once("value");
  const trackerData = trackerSnap.val() || {};
  const lastSeason = trackerData.last_season_updated || 0;
  const lastRound = trackerData.last_race_updated || 0;

  if (newSeason < lastSeason || (newSeason === lastSeason && newRound <= lastRound)) {
    console.log(`Race ${newSeason}-${newRound} already processed. Exiting.`);
    return false;
  }
  return true;
}

// Helper to calculate "Best Result" string (e.g., "1 (x2)")
function calculateBestResult(currentBestStr, newPosition) {
  const bestStr = (typeof currentBestStr === "string") ? currentBestStr : "99 (x0)";
  const currentBestPos = parseInt(bestStr.match(/(\d+)/)[0]);
  
  let currentBestCount = 0;
  const countMatch = bestStr.match(/x(\d+)/);
  if (countMatch && countMatch[1]) {
    currentBestCount = parseInt(countMatch[1]);
  }

  if (newPosition < currentBestPos) {
    return `${newPosition} (x1)`; // New record
  } else if (newPosition === currentBestPos && newPosition <= 99) {
    return `${newPosition} (x${currentBestCount + 1})`; // Tie record
  }
  return null; // No change
}

// Helper to create history entry
function createHistoryEntry(year, position, points, wins, podiums, teamOrTeams) {
  return { year: year.toString(), position, points, wins, podiums, team: teamOrTeams };
}

// Helper to trim history array to last 10 entries
function manageHistoryArray(existingHistory, newEntry) {
  let history = existingHistory || [];
  history.push(newEntry);
  if (history.length > 10) history = history.slice(1);
  return history;
}

/**
 * ===================================================================
 * LOGIC PROCESSORS: RACE UPDATES
 * ===================================================================
 */

async function processCircuitRaceUpdate(newSeason, newRound, results, updates) {
  const calendarSnap = await db.ref(PATHS.CALENDAR).once("value");
  const calendarData = calendarSnap.val() || {};
  
  let circuitKey = Object.keys(calendarData).find(key => parseInt(calendarData[key]?.round) === newRound);

  if (!circuitKey) {
    console.warn(`Could not find calendar entry for round ${newRound}.`);
    return;
  }

  const podiumDrivers = [];
  const podiumTeams = [];

  // Get Top 3
  for (let i = 0; i < 3 && i < results.length; i++) {
    const result = results[i];
    podiumDrivers.push(DRIVER_ID_NAME_MAP[result.Driver.driverId] || result.Driver.familyName);
    podiumTeams.push(TEAM_ID_NAME_MAP[result.Constructor.constructorId] || result.Constructor.name);
  }

  console.log(`Top 3: ${podiumDrivers.join(", ")} | Teams: ${podiumTeams.join(", ")}`);

  updates[`${PATHS.CALENDAR}/${circuitKey}/season_result/year`] = newSeason.toString();
  updates[`${PATHS.CALENDAR}/${circuitKey}/season_result/podium`] = podiumDrivers;
  updates[`${PATHS.CALENDAR}/${circuitKey}/season_result/team`] = podiumTeams;
}

async function processDriverRaceStats(results, updates, constructorAggregator) {
  for (const result of results) {
    const driverId = result.Driver.driverId;
    const constructorId = result.Constructor.constructorId;
    const position = parseInt(result.position);

    const driverSnap = await db.ref(`${PATHS.DRIVERS}/${driverId}`).once("value");
    
    if (driverSnap.exists()) {
      const data = driverSnap.val();

      // Podiums
      if (position <= 3) {
        updates[`${PATHS.DRIVERS}/${driverId}/season_podiums`] = ((parseInt(data.season_podiums) || 0) + 1).toString();
        updates[`${PATHS.DRIVERS}/${driverId}/podiums`] = ((parseInt(data.podiums) || 0) + 1).toString();
        
        // Wins
        if (position === 1) {
          updates[`${PATHS.DRIVERS}/${driverId}/season_wins`] = ((parseInt(data.season_wins) || 0) + 1).toString();
        }
      }

      // Best Result
      const newBestResult = calculateBestResult(data.best_result, position);
      if (newBestResult) {
        updates[`${PATHS.DRIVERS}/${driverId}/best_result`] = newBestResult;
      }
    }

    // Aggregate for Constructors
    if (!constructorAggregator[constructorId]) constructorAggregator[constructorId] = { wins: 0, podiums: 0 };
    if (position === 1) constructorAggregator[constructorId].wins += 1;
    if (position <= 3) constructorAggregator[constructorId].podiums += 1;
  }
}

async function processConstructorRaceStats(constructorAggregator, updates) {
  for (const [teamId, stats] of Object.entries(constructorAggregator)) {
    if (stats.wins === 0 && stats.podiums === 0) continue;

    const teamSnap = await db.ref(`${PATHS.TEAMS}/${teamId}`).once("value");
    if (teamSnap.exists()) {
      const data = teamSnap.val();
      
      if (stats.wins > 0) {
        updates[`${PATHS.TEAMS}/${teamId}/wins`] = ((parseInt(data.wins) || 0) + stats.wins).toString();
        updates[`${PATHS.TEAMS}/${teamId}/season_wins`] = ((parseInt(data.season_wins) || 0) + stats.wins).toString();
      }
      if (stats.podiums > 0) {
        updates[`${PATHS.TEAMS}/${teamId}/podiums`] = ((parseInt(data.podiums) || 0) + stats.podiums).toString();
        updates[`${PATHS.TEAMS}/${teamId}/season_podiums`] = ((parseInt(data.season_podiums) || 0) + stats.podiums).toString();
      }
    } else {
        console.warn(`Team ${teamId} not found in DB.`);
    }
  }
}

/**
 * ===================================================================
 * LOGIC PROCESSORS: CHAMPIONSHIP/END SEASON
 * ===================================================================
 */

async function processDriverSeasonArchive(newSeason, updates) {
  const response = await axios.get(APIS.DRIVER_STANDINGS);
  const standings = response.data.MRData.StandingsTable.StandingsLists[0].DriverStandings;

  for (const driver of standings) {
    const driverId = driver.Driver.driverId;
    const snap = await db.ref(`${PATHS.DRIVERS}/${driverId}`).once("value");

    if (snap.exists()) {
      const data = snap.val();
      
      const teamNames = driver.Constructors.map(c => TEAM_ID_NAME_MAP[c.constructorId] || c.name).join(' / ');
      const entry = createHistoryEntry(
        newSeason, 
        driver.position, 
        driver.points, 
        parseInt(data.season_wins) || 0, 
        parseInt(data.season_podiums) || 0, 
        teamNames
      );

      updates[`${PATHS.DRIVERS}/${driverId}/driver_history`] = manageHistoryArray(data.driver_history, entry);
      
      // Reset Season Stats
      updates[`${PATHS.DRIVERS}/${driverId}/season_wins`] = "0";
      updates[`${PATHS.DRIVERS}/${driverId}/season_podiums`] = "0";

      // Champion check
      if (driver.position === "1") {
        updates[`${PATHS.DRIVERS}/${driverId}/championships`] = ((parseInt(data.championships) || 0) + 1).toString();
      }
    }
  }
}

async function processConstructorSeasonArchive(newSeason, updates) {
  const response = await axios.get(APIS.CONSTRUCTOR_STANDINGS);
  const standings = response.data.MRData.StandingsTable.StandingsLists[0].ConstructorStandings;

  for (const team of standings) {
    const teamId = team.Constructor.constructorId;
    const snap = await db.ref(`${PATHS.TEAMS}/${teamId}`).once("value");

    if (snap.exists()) {
      const data = snap.val();
      
      const entry = createHistoryEntry(
        newSeason, 
        team.position, 
        team.points, 
        parseInt(data.season_wins) || 0, 
        parseInt(data.season_podiums) || 0, 
        null // Teams don't have a "team" field in history usually
      );

      updates[`${PATHS.TEAMS}/${teamId}/team_history`] = manageHistoryArray(data.team_history, entry);

      // Reset Season Stats
      updates[`${PATHS.TEAMS}/${teamId}/season_wins`] = "0";
      updates[`${PATHS.TEAMS}/${teamId}/season_podiums`] = "0";

      // Champion check
      if (team.position === "1") {
        updates[`${PATHS.TEAMS}/${teamId}/world_championships`] = ((parseInt(data.world_championships) || 0) + 1).toString();
      }
    }
  }
}

async function processCircuitSeasonArchive(newSeason, updates) {
  const calendarSnap = await db.ref(PATHS.CALENDAR).once("value");
  const calendarData = calendarSnap.val() || {};

  for (const [circuitKey, circuitData] of Object.entries(calendarData)) {
    const result = circuitData.season_result;
    
    if (result) {
      const entry = {
        year: result.year || newSeason.toString(),
        podium: result.podium || [],
        team: result.team || []
      };

      const circuitSnap = await db.ref(`${PATHS.CIRCUITS}/${circuitKey}`).once("value");
      if (circuitSnap.exists()) {
        const history = manageHistoryArray(circuitSnap.val().track_history, entry);
        updates[`${PATHS.CIRCUITS}/${circuitKey}/track_history`] = history;
      }
    }
    
    // Reset calendar result
    updates[`${PATHS.CALENDAR}/${circuitKey}/season_result`] = { year: null, podium: [], team: [] };
  }
}

/**
 * ===================================================================
 * MAIN EXPORTS
 * ===================================================================
 */

exports.updateRaceStats = onSchedule(
  {
    schedule: "every monday 20:00",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300, // Increased slightly
    retryConfig: { retryCount: 5 } // Adjusted for standard usage
  },
  async (event) => {
    console.log("Starting post-race stats check...");
    try {
      // 1. Fetch API
      const response = await axios.get(APIS.RACE_RESULTS);
      const resultsData = response.data.MRData;

      if (parseInt(resultsData.total) === 0 || !resultsData.RaceTable.Races[0]) {
        console.log("No recent race found.");
        return;
      }

      const raceInfo = resultsData.RaceTable.Races[0];
      const newSeason = parseInt(raceInfo.season);
      const newRound = parseInt(raceInfo.round);

      // 2. Validate Lock
      if (!(await shouldProcessRace(newSeason, newRound))) return;

      console.log(`Processing race: ${newSeason}-${newRound}`);
      
      // 3. Prepare Data & Updates
      await loadMappings();
      const updates = {};
      const constructorAggregator = {};

      // 4. Execute Logic Blocks
      await processCircuitRaceUpdate(newSeason, newRound, raceInfo.Results, updates);
      await processDriverRaceStats(raceInfo.Results, updates, constructorAggregator);
      await processConstructorRaceStats(constructorAggregator, updates);

      // 5. Update Lock
      updates[`${PATHS.TRACKER}/last_season_updated`] = newSeason;
      updates[`${PATHS.TRACKER}/last_race_updated`] = newRound;

      // 6. Atomic Write
      await db.ref().update(updates);
      console.log(`Update complete for ${newSeason}-${newRound}`);

    } catch (error) {
      console.error("Error in updateRaceStats:", error);
      throw error;
    }
  }
);


exports.updateChampionships = onSchedule(
  {
    schedule: "0 12 20 12 *", // December 20th at 12:00
    timeZone: "Europe/Rome",
    timeoutSeconds: 300,
  },
  async (event) => {
    console.log("Starting end-of-season championship check...");
    try {
      // 1. Fetch Basic Info to check season
      const driverRes = await axios.get(APIS.DRIVER_STANDINGS);
      const newSeason = parseInt(driverRes.data.MRData.StandingsTable.season);

      // 2. Check Lock
      const trackerSnap = await db.ref(PATHS.TRACKER).once("value");
      const lastChampSeason = trackerSnap.val()?.last_champ_season || 0;

      if (newSeason <= lastChampSeason) {
        console.log(`Season ${newSeason} already processed.`);
        return;
      }

      console.log(`Archiving season: ${newSeason}`);
      await loadMappings();
      const updates = {};

      // 3. Execute Logic Blocks
      await processDriverSeasonArchive(newSeason, updates);
      await processConstructorSeasonArchive(newSeason, updates);
      await processCircuitSeasonArchive(newSeason, updates);

      // 4. Update Lock
      updates[`${PATHS.TRACKER}/last_champ_season`] = newSeason;

      // 5. Atomic Write
      await db.ref().update(updates);
      console.log(`Season ${newSeason} archived successfully.`);

    } catch (error) {
      console.error("Error in updateChampionships:", error);
      throw error;
    }
  }
);
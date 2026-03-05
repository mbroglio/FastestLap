const axios = require("axios");

const PATHS = {
    drivers: "drivers",
    teams: "teams",
    circuits: "circuits",
    calendar: "app_config/calendar",
    tracker: "app_config/stats_tracker",
    team_map: "app_config/team_id_name_map",
    driver_map: "app_config/driver_id_name_map"
};

const APIS = {
    raceResults: "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json",
    driverStandings: "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json",
    constructorStandings: "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json"
};

let DRIVER_ID_NAME_MAP = {};
let TEAM_ID_NAME_MAP = {};



/* -----------------------------------------------------------------
* LOCAL MAPS LOADING
* -----------------------------------------------------------------
*/

async function loadMappings(db) {
    const [teamSnap, driverSnap] = await Promise.all([
        db.ref(PATHS.team_map).once("value"),
        db.ref(PATHS.driver_map).once("value")
    ]);

    TEAM_ID_NAME_MAP = teamSnap.val() || {};
    DRIVER_ID_NAME_MAP = driverSnap.val() || {};
}



/* -----------------------------------------------------------------
* EXPORTS
* -----------------------------------------------------------------
*/

async function executeRaceStatsUpdate(db) { // post race stats update
    console.log("Starting F1 post-race stats check...");

    const response = await axios.get(APIS.raceResults);
    const resultsData = response.data.MRData;

    if (parseInt(resultsData.total) === 0 || !resultsData.RaceTable.Races[0]) {
        console.log("No recent race found. Exiting.");
        return;
    }

    const raceInfo = resultsData.RaceTable.Races[0];
    const newSeason = parseInt(raceInfo.season);
    const newRound = parseInt(raceInfo.round);
    const trackId = raceInfo.Circuit.circuitId;
    console.log(`Fetched race data for Season ${newSeason}, Round ${newRound} at Circuit ${trackId}.`);

    const trackerRef = db.ref(PATHS.tracker);
    const trackerSnapshot = await trackerRef.once("value");
    const trackerData = trackerSnapshot.val() || {};
    const lastSeason = trackerData.last_season_updated || 0;
    const lastRound = trackerData.last_race_updated || 0;

    if (newSeason < lastSeason || (newSeason === lastSeason && newRound <= lastRound)) {
        console.log(`F1 Race ${newSeason}-${newRound} already processed. Exiting.`);
        return;
    }

    console.log(`New F1 race detected: ${newSeason}-${newRound}. Processing...`);

    await loadMappings(db);

    const multiPathUpdates = {};
    const results = raceInfo.Results;
    const constructorUpdates = {};

    for (const result of results) {
        const driverId = result.Driver.driverId;
        const constructorId = result.Constructor.constructorId;
        const position = parseInt(result.position);

        const positionString = result.positionText;
        const lapsCompleted = parseInt(result.laps);

        const driverRef = db.ref(`${PATHS.drivers}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");

        if (driverSnapshot.exists()) {
            const driverData = driverSnapshot.val();
            if (position <= 3) {
                const currentPodiums = parseInt(driverData.podiums) || 0;
                const seasonPodiums = parseInt(driverData.season_podiums) || 0;
                multiPathUpdates[`${PATHS.drivers}/${driverId}/season_podiums`] = (seasonPodiums + 1).toString();
                multiPathUpdates[`${PATHS.drivers}/${driverId}/podiums`] = (currentPodiums + 1).toString();
                if (position === 1) {
                    const seasonWins = parseInt(driverData.season_wins) || 0;
                    multiPathUpdates[`${PATHS.drivers}/${driverId}/season_wins`] = (seasonWins + 1).toString();
                }
            }

            if(positionString !== "R" && lapsCompleted !== 0) {
                const gpsEntered = parseInt(driverData.gps_entered) || 0;
                multiPathUpdates[`${PATHS.drivers}/${driverId}/gps_entered`] = (gpsEntered + 1).toString();
            }

            // Best Result logic
            let bestResultString = String(driverData.best_result || "99 (x0)");
            if (bestResultString === "null" || bestResultString === "undefined") bestResultString = "99 (x0)";

            const currentBestPos = parseInt(bestResultString.match(/(\d+)/)[0]);
            let currentBestCount = 0;
            const countMatch = bestResultString.match(/x(\d+)/);
            if (countMatch && countMatch[1]) currentBestCount = parseInt(countMatch[1]);

            if (position < currentBestPos) {
                multiPathUpdates[`${PATHS.drivers}/${driverId}/best_result`] = `${position} (x1)`;
            } else if (position === currentBestPos && position <= 99) {
                multiPathUpdates[`${PATHS.drivers}/${driverId}/best_result`] = `${position} (x${currentBestCount + 1})`;
            }
        }

        if (!constructorUpdates[constructorId]) constructorUpdates[constructorId] = { podiums: 0, wins: 0 };
        if (position === 1) constructorUpdates[constructorId].wins += 1;
        if (position <= 3) constructorUpdates[constructorId].podiums += 1;
    }

    for (const constructorId in constructorUpdates) {
        const updates = constructorUpdates[constructorId];
        if (updates.podiums > 0 || updates.wins > 0) {
            const constructorRef = db.ref(`${PATHS.teams}/${constructorId}`);
            const constructorSnapshot = await constructorRef.once("value");
            if (constructorSnapshot.exists()) {
                const constructorData = constructorSnapshot.val();
                if (updates.wins > 0) {
                    const careerWins = parseInt(constructorData.wins) || 0;
                    multiPathUpdates[`${PATHS.teams}/${constructorId}/wins`] = (careerWins + updates.wins).toString();
                    const seasonWins = parseInt(constructorData.season_wins) || 0;
                    multiPathUpdates[`${PATHS.teams}/${constructorId}/season_wins`] = (seasonWins + updates.wins).toString();
                }
                if (updates.podiums > 0) {
                    const currentPodiums = parseInt(constructorData.podiums) || 0;
                    multiPathUpdates[`${PATHS.teams}/${constructorId}/podiums`] = (currentPodiums + updates.podiums).toString();
                    const seasonPodiums = parseInt(constructorData.season_podiums) || 0;
                    multiPathUpdates[`${PATHS.teams}/${constructorId}/season_podiums`] = (seasonPodiums + updates.podiums).toString();
                }
            }
        }
    }

    await processCircuitRaceUpdate(newSeason, newRound, trackId, results, multiPathUpdates, db);

    multiPathUpdates[`${PATHS.tracker}/last_season_updated`] = newSeason;
    multiPathUpdates[`${PATHS.tracker}/last_race_updated`] = newRound;

    await db.ref().update(multiPathUpdates);
    console.log(`F1 Post-race update complete.`);
}

async function executeChampionshipsUpdate(db) { // end of season championship update
    console.log("Starting F1 end-of-season championship check...");

    const driverStandingsRes = await axios.get(APIS.driverStandings);
    const standingsData = driverStandingsRes.data.MRData.StandingsTable;
    const newSeason = parseInt(standingsData.season);

    const trackerRef = db.ref(PATHS.tracker);
    const trackerSnapshot = await trackerRef.once("value");
    const trackerData = trackerSnapshot.val() || {};
    const lastChampSeason = trackerData.last_champ_season || 0;

    if (newSeason <= lastChampSeason) {
        console.log(`F1 Championships for season ${newSeason} already processed.`);
        return;
    }

    console.log(`New F1 season detected: ${newSeason}. Updating...`);
    await loadMappings(db);

    const multiPathUpdates = {};

    await processDriverSeasonArchive(newSeason, multiPathUpdates, db);
    await processConstructorSeasonArchive(newSeason, multiPathUpdates, db);
    await processCircuitSeasonArchive(newSeason, multiPathUpdates, db);


    multiPathUpdates[`${PATHS.tracker}/last_champ_season`] = newSeason;
    await db.ref().update(multiPathUpdates);

    console.log(`F1 Championship update complete.`);
}



/*
* -----------------------------------------------------------------
* HELPER MAIN FUNCTIONS
* -----------------------------------------------------------------
*/

async function processCircuitRaceUpdate(newSeason, newRound, trackId, results, updates, db) {
    const calendarSnap = await db.ref(PATHS.calendar).once("value");
    if (!calendarSnap.exists()) {
        console.log("Calendar is empty");
        createCalendarEntry(newRound, newSeason, trackId, results, updates);
    } else {
        const calendarData = calendarSnap.val() || {};
        for (key in calendarData) {
            if (key === trackId) {
                console.log("Circuit already updated in calendar");
                return;
            }
        }
        createCalendarEntry(newRound, newSeason, trackId, results, updates);
    }
}

async function processDriverSeasonArchive(newSeason, updates, db) {
    const response = await axios.get(APIS.driverStandings);
    const standings = response.data.MRData.StandingsTable.StandingsLists[0].DriverStandings;

    for (const driver of standings) {
        const driverId = driver.Driver.driverId;
        const snap = await db.ref(`${PATHS.drivers}/${driverId}`).once("value");

        if (snap.exists()) {
            const data = snap.val();

            const teamNames = driver.Constructors.map(c => TEAM_ID_NAME_MAP[c.constructorId] || c.name).join(' / ');
            const entry = createHistoryEntryDriver(
                newSeason,
                driver.position,
                driver.points,
                data.season_wins || "0",
                data.season_podiums || "0",
                teamNames
            );

            updates[`${PATHS.drivers}/${driverId}/driver_history`] = manageHistoryArray(data.driver_history, entry);

            // Reset Season Stats
            updates[`${PATHS.drivers}/${driverId}/season_wins`] = "0";
            updates[`${PATHS.drivers}/${driverId}/season_podiums`] = "0";
            // Champion check
            if (driver.position === "1") {
                updates[`${PATHS.drivers}/${driverId}/championships`] = ((parseInt(data.championships) || 0) + 1).toString();
            }
        }
    }
}

async function processConstructorSeasonArchive(newSeason, updates, db) {
    const response = await axios.get(APIS.constructorStandings);
    const standings = response.data.MRData.StandingsTable.StandingsLists[0].ConstructorStandings;

    for (const team of standings) {
        const teamId = team.Constructor.constructorId;
        const snap = await db.ref(`${PATHS.teams}/${teamId}`).once("value");

        if (snap.exists()) {
            const data = snap.val();

            const entry = createHistoryEntryConstructor(
                newSeason,
                team.position,
                team.points,
                data.season_wins || "0",
                data.season_podiums || "0",
                // Teams don't have a "team" field in history
            );

            updates[`${PATHS.teams}/${teamId}/team_history`] = manageHistoryArray(data.team_history, entry);

            // Reset Season Stats
            updates[`${PATHS.teams}/${teamId}/season_wins`] = "0";
            updates[`${PATHS.teams}/${teamId}/season_podiums`] = "0";
            // Champion check
            if (team.position === "1") {
                updates[`${PATHS.teams}/${teamId}/world_championships`] = ((parseInt(data.world_championships) || 0) + 1).toString();
            }
        }
    }
}

async function processCircuitSeasonArchive(newSeason, updates, db) {
    const calendarSnap = await db.ref(PATHS.calendar).once("value");
    const calendarData = calendarSnap.val() || {};

    for (const [circuitKey, circuitData] of Object.entries(calendarData)) {
        const result = circuitData.season_result;

        if (result) {
            const entry = createHistoryEntryCircuit(
                result.year || newSeason.toString(),
                result.podium || [],
                result.team || []
            );

            const circuitSnap = await db.ref(`${PATHS.circuits}/${circuitKey}`).once("value");
            if (circuitSnap.exists()) {
                const history = manageHistoryArray(circuitSnap.val().track_history, entry);
                updates[`${PATHS.circuits}/${circuitKey}/track_history`] = history;
            }
        }

        // Reset calendar result
        updates[`${PATHS.calendar}/${circuitKey}`] = null;
    }
}




/*
* -----------------------------------------------------------------
* HELPER SUPPORT FUNCTIONS
* -----------------------------------------------------------------
*/

// Helper to create history entry
function createHistoryEntryDriver(year, position, points, wins, podiums, teamOrTeams) {
    return { year: year.toString(), position, points, wins, podiums, team: teamOrTeams };
}

function createHistoryEntryConstructor(year, position, points, wins, podiums) {
    return { year: year.toString(), position, points, wins, podiums };
}

function createHistoryEntryCircuit(year, podium, team) {
    return { year: year.toString(), podium, team };
}

// Helper to trim history array to last 10 entries
function manageHistoryArray(existingHistory, newEntry) {
    let history = existingHistory || [];
    history.push(newEntry);
    if (history.length > 10) history = history.slice(1);
    return history;
}

function createCalendarEntry(newRound, newSeason, trackId, results, updates) {
    const podiumDrivers = [];
    const podiumTeams = [];

    // Get Top 3
    for (let i = 0; i < 3 && i < results.length; i++) {
        const result = results[i];
        podiumDrivers.push(DRIVER_ID_NAME_MAP[result.Driver.driverId] || result.Driver.familyName);
        podiumTeams.push(TEAM_ID_NAME_MAP[result.Constructor.constructorId] || result.Constructor.name);
    }

    console.log(`Top 3: ${podiumDrivers.join(", ")} | Teams: ${podiumTeams.join(", ")}`);
    updates[`${PATHS.calendar}/${trackId}/season_result/round`] = newRound;
    updates[`${PATHS.calendar}/${trackId}/season_result/year`] = newSeason.toString();
    updates[`${PATHS.calendar}/${trackId}/season_result/podium`] = podiumDrivers;
    updates[`${PATHS.calendar}/${trackId}/season_result/team`] = podiumTeams;
}






/*
* -----------------------------------------------------------------
* FUNCTION EXPORTS
* -----------------------------------------------------------------
*/

// Export main functions and helper functions for testing
module.exports = {
    executeRaceStatsUpdate,
    executeChampionshipsUpdate,

    // Helper functions for testing
    loadMappings,
    processCircuitRaceUpdate,
    processDriverSeasonArchive,
    processConstructorSeasonArchive,
    processCircuitSeasonArchive
};
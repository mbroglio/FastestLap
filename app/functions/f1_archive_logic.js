const axios = require("axios");
const { PATHS, APIS, loadMappings, getTeamMap, getDriverMap } = require("./f1_constants");

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

    // Clear current season race updates and reset race counter for new season
    multiPathUpdates[PATHS.race_updates] = null;
    multiPathUpdates[`${PATHS.tracker}/last_race_updated`] = 0;
    multiPathUpdates[`${PATHS.tracker}/last_season_updated`] = newSeason;
    multiPathUpdates[`${PATHS.tracker}/last_champ_season`] = newSeason;
    await db.ref().update(multiPathUpdates);

    console.log(`F1 Championship update complete. Reset race_updates and stats tracker for new season ${newSeason}.`);
}

async function processCircuitRaceUpdate(newSeason, newRound, trackId, results, updates, db, forceUpdate = false) {
    const calendarSnap = await db.ref(PATHS.calendar).once("value");
    if (!calendarSnap.exists()) {
        console.log("Calendar is empty");
        createCalendarEntry(newRound, newSeason, trackId, results, updates);
    } else {
        const calendarData = calendarSnap.val() || {};
        const existingCircuit = calendarData[trackId];
        const existingResult = existingCircuit?.season_result;

        if (!forceUpdate && existingResult && String(existingResult.year) === newSeason.toString() && existingResult.round === newRound) {
            console.log("Circuit already updated in calendar for this round");
            return;
        }
        createCalendarEntry(newRound, newSeason, trackId, results, updates);
    }
}

async function processDriverSeasonArchive(newSeason, updates, db) {
    const response = await axios.get(APIS.driverStandings);
    const standings = response.data.MRData.StandingsTable.StandingsLists[0].DriverStandings;
    const teamIdNameMap = getTeamMap();

    for (const driver of standings) {
        const driverId = driver.Driver.driverId;
        const snap = await db.ref(`${PATHS.drivers}/${driverId}`).once("value");

        if (snap.exists()) {
            const data = snap.val();

            const teamNames = driver.Constructors.map(c => teamIdNameMap[c.constructorId] || c.name).join(' / ');
            const seasonStats = (data && data.season_stats) || {};
            const wins = seasonStats.wins || data.season_wins || "0";
            const podiums = seasonStats.podiums || data.season_podiums || "0";

            const entry = createHistoryEntryDriver(
                newSeason,
                driver.position,
                driver.points,
                wins,
                podiums,
                teamNames
            );

            updates[`${PATHS.drivers}/${driverId}/driver_history`] = manageHistoryArray(data.driver_history, entry);

            // Reset Season Stats
            updates[`${PATHS.drivers}/${driverId}/season_stats`] = {
                wins: "0",
                podiums: "0",
                dnfs: "0",
                poles: "0",
                season_position: "-",
                season_points: "0"
            };
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

            const seasonStats = (data && data.season_stats) || {};
            const wins = seasonStats.wins || data.season_wins || "0";
            const podiums = seasonStats.podiums || data.season_podiums || "0";

            const entry = createHistoryEntryConstructor(
                newSeason,
                team.position,
                team.points,
                wins,
                podiums
            );

            updates[`${PATHS.teams}/${teamId}/team_history`] = manageHistoryArray(data.team_history, entry);

            // Reset Season Stats
            updates[`${PATHS.teams}/${teamId}/season_stats`] = {
                wins: "0",
                podiums: "0",
                dnfs: "0",
                poles: "0",
                season_position: "-",
                season_points: "0"
            };
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

function createHistoryEntryDriver(year, position, points, wins, podiums, teamOrTeams) {
    return { year: year.toString(), position, points, wins, podiums, team: teamOrTeams };
}

function createHistoryEntryConstructor(year, position, points, wins, podiums) {
    return { year: year.toString(), position, points, wins, podiums };
}

function createHistoryEntryCircuit(year, podium, team) {
    return { year: year.toString(), podium, team };
}

function manageHistoryArray(existingHistory, newEntry) {
    let history = existingHistory || [];
    history.push(newEntry);
    if (history.length > 10) history = history.slice(1);
    return history;
}

function createCalendarEntry(newRound, newSeason, trackId, results, updates) {
    const podiumDrivers = [];
    const podiumTeams = [];
    const driverIdNameMap = getDriverMap();
    const teamIdNameMap = getTeamMap();

    // Get Top 3
    for (let i = 0; i < 3 && i < results.length; i++) {
        const result = results[i];
        podiumDrivers.push(driverIdNameMap[result.Driver.driverId] || result.Driver.familyName);
        podiumTeams.push(teamIdNameMap[result.Constructor.constructorId] || result.Constructor.name);
    }

    console.log(`Top 3: ${podiumDrivers.join(", ")} | Teams: ${podiumTeams.join(", ")}`);
    updates[`${PATHS.calendar}/${trackId}/season_result/round`] = newRound;
    updates[`${PATHS.calendar}/${trackId}/season_result/year`] = newSeason.toString();
    updates[`${PATHS.calendar}/${trackId}/season_result/podium`] = podiumDrivers;
    updates[`${PATHS.calendar}/${trackId}/season_result/team`] = podiumTeams;
}

module.exports = {
    executeChampionshipsUpdate,
    processCircuitRaceUpdate,
    processDriverSeasonArchive,
    processConstructorSeasonArchive,
    processCircuitSeasonArchive,
    createHistoryEntryDriver,
    createHistoryEntryConstructor,
    createHistoryEntryCircuit,
    manageHistoryArray,
    createCalendarEntry
};

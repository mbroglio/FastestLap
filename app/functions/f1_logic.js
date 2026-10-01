const axios = require("axios");
const careerStatsLogic = require("./career_stats_logic");

const PATHS = {
    drivers: "drivers",
    teams: "teams",
    circuits: "circuits",
    calendar: "app_config/calendar",
    tracker: "app_config/stats_tracker",
    team_map: "app_config/team_id_name_map",
    driver_map: "app_config/driver_id_name_map",
    db_calendar_f1: "app_config/db_calendar/f1",
    race_updates: "app_config/stats_tracker/race_updates"
};

const APIS = {
    raceResults: "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json",
    driverStandings: "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json",
    constructorStandings: "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json",
    qualifyingResults: "https://api.jolpi.ca/ergast/f1/current/qualifying/?format=json",
    qualifyingResultsPoles: "https://api.jolpi.ca/ergast/f1/current/qualifying/1/?format=json",
    results: "https://api.jolpi.ca/ergast/f1/current/results/?format=json"
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
* EXPORTS & CORE RACE STATS UPDATE LOGIC
* -----------------------------------------------------------------
*/

async function executeRaceStatsUpdateWithResults(db, newSeason, newRound, trackId, results) {
    await loadMappings(db);

    const multiPathUpdates = {};
    const constructorUpdates = {};

    // Idempotence check: verify if career stats were already applied for this season and round
    const raceUpdateSnap = await db.ref(`${PATHS.race_updates}/${newSeason}/${newRound}`).once("value");
    const raceUpdateData = raceUpdateSnap.val() || {};
    const careerStatsAlreadyApplied = raceUpdateData.career_stats_applied === true;

    if (careerStatsAlreadyApplied) {
        console.log(`[F1 Race Stats] Career stats already applied for Season ${newSeason}, Round ${newRound}. Skipping increments to prevent duplicate accumulation.`);
    }

    for (const result of results) {
        const driverId = result.Driver?.driverId;
        const constructorId = result.Constructor?.constructorId;
        const position = parseInt(result.position);

        const positionString = result.positionText;
        const statusStr = (result.status || "").toLowerCase();
        // Any driver listed in race results entered the GP (excluding only DNS / Did Not Start)
        const isDns = positionString === "DNS" || positionString === "W" || statusStr.includes("did not start");

        if (driverId && !careerStatsAlreadyApplied) {
            const driverRef = db.ref(`${PATHS.drivers}/${driverId}`);
            const driverSnapshot = await driverRef.once("value");

            if (driverSnapshot.exists()) {
                const driverData = driverSnapshot.val();
                if (position <= 3) {
                    const currentPodiums = parseInt(driverData.podiums) || 0;
                    multiPathUpdates[`${PATHS.drivers}/${driverId}/podiums`] = (currentPodiums + 1).toString();
                }

                if (!isDns) {
                    const gpsEntered = parseInt(driverData.gps_entered) || 0;
                    multiPathUpdates[`${PATHS.drivers}/${driverId}/gps_entered`] = (gpsEntered + 1).toString();
                }

                // Best Result logic
                let bestResultString = String(driverData.best_result || "99 (x0)");
                if (bestResultString === "null" || bestResultString === "undefined") bestResultString = "99 (x0)";

                const currentBestPos = parseInt(bestResultString.match(/(\d+)/)?.[0] || "99");
                let currentBestCount = 0;
                const countMatch = bestResultString.match(/x(\d+)/);
                if (countMatch && countMatch[1]) currentBestCount = parseInt(countMatch[1]);

                if (position < currentBestPos) {
                    multiPathUpdates[`${PATHS.drivers}/${driverId}/best_result`] = `${position} (x1)`;
                } else if (position === currentBestPos && position <= 99) {
                    multiPathUpdates[`${PATHS.drivers}/${driverId}/best_result`] = `${position} (x${currentBestCount + 1})`;
                }
            }
        }

        if (constructorId && !careerStatsAlreadyApplied) {
            if (!constructorUpdates[constructorId]) constructorUpdates[constructorId] = { podiums: 0, wins: 0, gps_entered: 0 };
            if (position === 1) constructorUpdates[constructorId].wins += 1;
            if (position <= 3) constructorUpdates[constructorId].podiums += 1;
            if (!isDns) constructorUpdates[constructorId].gps_entered = 1;
        }
    }

    if (!careerStatsAlreadyApplied) {
        for (const constructorId in constructorUpdates) {
            const updates = constructorUpdates[constructorId];
            if (updates.podiums > 0 || updates.wins > 0 || updates.gps_entered > 0) {
                const constructorRef = db.ref(`${PATHS.teams}/${constructorId}`);
                const constructorSnapshot = await constructorRef.once("value");
                if (constructorSnapshot.exists()) {
                    const constructorData = constructorSnapshot.val();
                    if (updates.wins > 0) {
                        const careerWins = parseInt(constructorData.wins) || 0;
                        multiPathUpdates[`${PATHS.teams}/${constructorId}/wins`] = (careerWins + updates.wins).toString();
                    }
                    if (updates.podiums > 0) {
                        const currentPodiums = parseInt(constructorData.podiums) || 0;
                        multiPathUpdates[`${PATHS.teams}/${constructorId}/podiums`] = (currentPodiums + updates.podiums).toString();
                    }
                    if (updates.gps_entered > 0) {
                        const currentGpsEntered = parseInt(constructorData.gps_entered) || 0;
                        multiPathUpdates[`${PATHS.teams}/${constructorId}/gps_entered`] = (currentGpsEntered + updates.gps_entered).toString();
                    }
                }
            }
        }
        multiPathUpdates[`${PATHS.race_updates}/${newSeason}/${newRound}/career_stats_applied`] = true;
    }

    if (trackId) {
        await processCircuitRaceUpdate(newSeason, newRound, trackId, results, multiPathUpdates, db);
    }

    multiPathUpdates[`${PATHS.tracker}/last_season_updated`] = newSeason;
    multiPathUpdates[`${PATHS.tracker}/last_race_updated`] = newRound;
    await db.ref().update(multiPathUpdates);
    console.log(`F1 Post-race career and calendar updates complete for Season ${newSeason}, Round ${newRound}.`);
}

async function executeRaceStatsUpdate(db, targetSeason = null, targetRound = null) { // post race stats update
    console.log(`Starting F1 post-race stats check${targetRound ? ` for Round ${targetRound}` : ""}...`);

    const apiUrl = (targetSeason && targetRound)
        ? `https://api.jolpi.ca/ergast/f1/${targetSeason}/${targetRound}/results/?format=json`
        : APIS.raceResults;

    const response = await axios.get(apiUrl, { timeout: 15000 });
    const resultsData = response.data?.MRData;

    if (!resultsData || parseInt(resultsData.total) === 0 || !resultsData.RaceTable?.Races?.[0]) {
        console.log("No recent race found. Exiting.");
        return { success: false, reason: "no_results" };
    }

    const raceInfo = resultsData.RaceTable.Races[0];
    const newSeason = parseInt(raceInfo.season);
    const newRound = parseInt(raceInfo.round);
    const trackId = raceInfo.Circuit?.circuitId;
    console.log(`Fetched race data for Season ${newSeason}, Round ${newRound} at Circuit ${trackId}.`);

    const trackerRef = db.ref(PATHS.tracker);
    const trackerSnapshot = await trackerRef.once("value");
    const trackerData = trackerSnapshot.val() || {};
    const lastSeason = trackerData.last_season_updated || 0;
    const lastRound = trackerData.last_race_updated || 0;

    if (!targetRound && (newSeason < lastSeason || (newSeason === lastSeason && newRound <= lastRound))) {
        console.log(`F1 Race ${newSeason}-${newRound} already processed. Exiting.`);
        return { success: false, reason: "already_processed" };
    }

    console.log(`Processing F1 race: ${newSeason}-${newRound}...`);
    await executeRaceStatsUpdateWithResults(db, newSeason, newRound, trackId, raceInfo.Results);

    // Synchronize comprehensive driver season_stats (wins, podiums, dnfs, poles, seasonPosition, seasonPoints)
    await syncDriverSeasonStats(db, newSeason);
    // Synchronize comprehensive constructor season_stats
    await syncConstructorSeasonStats(db, newSeason);

    return { success: true, season: newSeason, round: newRound, trackId, results: raceInfo.Results };
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

    for (const driver of standings) {
        const driverId = driver.Driver.driverId;
        const snap = await db.ref(`${PATHS.drivers}/${driverId}`).once("value");

        if (snap.exists()) {
            const data = snap.val();

            const teamNames = driver.Constructors.map(c => TEAM_ID_NAME_MAP[c.constructorId] || c.name).join(' / ');
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
                // Teams don't have a "team" field in history
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
* DRIVER SEASON STATS SYNC (wins, podiums, dnfs, poles, seasonPosition, seasonPoints)
* -----------------------------------------------------------------
*/

async function fetchAndCalculateDriverSeasonStats() {
    
    const standingsRes = await axios.get(APIS.driverStandings, { timeout: 15000 });
    const standingsTable = standingsRes.data?.MRData?.StandingsTable;
    
    const standingsLists = standingsTable?.StandingsLists || [];
    const driverStandings = (standingsLists.length > 0 && standingsLists[0].DriverStandings) ? standingsLists[0].DriverStandings : [];

    // 2. Fetch Pole Positions (Qualifying 1)
    const polesMap = {};
    try {
        console.log(`Fetching F1 pole positions`);
        const polesRes = await axios.get(APIS.qualifyingResultsPoles+"&limit=100", { timeout: 15000 });
        const races = polesRes.data?.MRData?.RaceTable?.Races || [];
        for (const r of races) {
            if (r.QualifyingResults && r.QualifyingResults[0] && r.QualifyingResults[0].Driver) {
                const poleDriverId = r.QualifyingResults[0].Driver.driverId;
                polesMap[poleDriverId] = (polesMap[poleDriverId] || 0) + 1;
            }
        }
    } catch (e) {
        console.warn(`Could not fetch pole positions for current season: ${e.message}`);
    }

    // 3. Fetch Race Results (Podiums, Wins, DNFs)
    const podiumsMap = {};
    const winsMap = {};
    const dnfsMap = {};

    try {
        let offset = 0;
        const limit = 100;
        let total = 1;

        while (offset < total && offset <= 1500) {
            const resultsUrl = APIS.results + `&limit=${limit}&offset=${offset}`;
            console.log(`Fetching race results chunk: offset ${offset}...`);
            const resultsRes = await axios.get(resultsUrl, { timeout: 15000 });
            total = parseInt(resultsRes.data?.MRData?.total) || 0;
            const races = resultsRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of races) {
                const results = r.Results || [];
                for (const res of results) {
                    if (!res.Driver || !res.Driver.driverId) continue;
                    const dId = res.Driver.driverId;
                    const pos = parseInt(res.position);

                    if (pos === 1) {
                        winsMap[dId] = (winsMap[dId] || 0) + 1;
                    }
                    if (pos <= 3) {
                        podiumsMap[dId] = (podiumsMap[dId] || 0) + 1;
                    }

                    // DNF detection:
                    // In Jolpica/Ergast, retired drivers have positionText "R", "D", "W"
                    // or status not containing "Finished" / "Lap" / "+..."
                    const posText = res.positionText || "";
                    const status = res.status || "";
                    const isFinished = status.includes("Finished") || status.includes("Lap") || status.startsWith("+");
                    const isDnf = posText === "R" || posText === "D" || posText === "W" || !isFinished;

                    if (isDnf) {
                        dnfsMap[dId] = (dnfsMap[dId] || 0) + 1;
                    }
                }
            }

            offset += limit;
            if (races.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch full race results: ${e.message}`);
    }

    // 4. Build consolidated stats dictionary by driverId
    const statsByDriverId = {};

    for (const standing of driverStandings) {
        const dId = standing.Driver.driverId;
        const winsFromStandings = parseInt(standing.wins) || 0;
        const calculatedWins = winsMap[dId] || 0;
        const wins = Math.max(winsFromStandings, calculatedWins).toString();

        statsByDriverId[dId] = {
            wins: wins,
            podiums: (podiumsMap[dId] || 0).toString(),
            dnfs: (dnfsMap[dId] || 0).toString(),
            poles: (polesMap[dId] || 0).toString(),
            season_position: (standing.position || "-").toString(),
            season_points: (standing.points || "0").toString()
        };
    }

    // Also include any driver who participated in a race or qualifying but has 0 points
    const allEncounteredDriverIds = new Set([
        ...Object.keys(statsByDriverId),
        ...Object.keys(podiumsMap),
        ...Object.keys(winsMap),
        ...Object.keys(dnfsMap),
        ...Object.keys(polesMap)
    ]);

    for (const dId of allEncounteredDriverIds) {
        if (!statsByDriverId[dId]) {
            statsByDriverId[dId] = {
                wins: (winsMap[dId] || 0).toString(),
                podiums: (podiumsMap[dId] || 0).toString(),
                dnfs: (dnfsMap[dId] || 0).toString(),
                poles: (polesMap[dId] || 0).toString(),
                season_position: "-",
                season_points: "0"
            };
        }
    }

    return {
        season,
        statsByDriverId
    };
}

async function syncDriverSeasonStats(db, targetSeason) {
    console.log(`Starting driver season_stats sync...`);
    const { season, statsByDriverId } = await fetchAndCalculateDriverSeasonStats();

    const driversSnap = await db.ref(PATHS.drivers).once("value");
    if (!driversSnap.exists()) {
        console.log("No drivers node found in database.");
        return { season, updatedCount: 0 };
    }

    const driversData = driversSnap.val() || {};
    const multiPathUpdates = {};
    let updatedCount = 0;

    for (const driverId of Object.keys(driversData)) {
        const stats = statsByDriverId[driverId] || {
            wins: "0",
            podiums: "0",
            dnfs: "0",
            poles: "0",
            season_position: "-",
            season_points: "0"
        };

        multiPathUpdates[`${PATHS.drivers}/${driverId}/season_stats`] = stats;

        updatedCount++;
    }

    if (Object.keys(multiPathUpdates).length > 0) {
        await db.ref().update(multiPathUpdates);
        console.log(`Successfully updated season_stats for ${updatedCount} drivers (Season ${season}).`);
    }

    return {
        season,
        updatedCount,
        stats: statsByDriverId
    };
}

/*
* -----------------------------------------------------------------
* CONSTRUCTOR SEASON STATS SYNC (wins, podiums, dnfs, poles, seasonPosition, seasonPoints)
* -----------------------------------------------------------------
*/

async function fetchAndCalculateConstructorSeasonStats() {
   


    console.log(`Fetching F1 constructor standings`);
    const standingsRes = await axios.get(APIS.constructorStandings, { timeout: 15000 });
    const standingsTable = standingsRes.data?.MRData?.StandingsTable;
    if (!season && standingsTable?.season) {
        season = standingsTable.season;
    }
    if (!season) {
        season = new Date().getFullYear().toString();
    }

    const standingsLists = standingsTable?.StandingsLists || [];
    const constructorStandings = (standingsLists.length > 0 && standingsLists[0].ConstructorStandings) ? standingsLists[0].ConstructorStandings : [];

    // 2. Fetch Pole Positions (Qualifying 1)
    const polesMap = {};
    try {
        console.log(`Fetching F1 pole positions for constructors}`);
        const polesRes = await axios.get(APIS.qualifyingResultsPoles+"&limit=100", { timeout: 15000 });
        const races = polesRes.data?.MRData?.RaceTable?.Races || [];
        for (const r of races) {
            const cId = r.QualifyingResults && r.QualifyingResults[0] && r.QualifyingResults[0].Constructor && r.QualifyingResults[0].Constructor.constructorId;
            if (cId) {
                polesMap[cId] = (polesMap[cId] || 0) + 1;
            }
        }
    } catch (e) {
        console.warn(`Could not fetch pole positions for constructor: ${e.message}`);
    }

    // 3. Fetch Race Results (Podiums, Wins, DNFs)
    const podiumsMap = {};
    const winsMap = {};
    const dnfsMap = {};

    try {
        let offset = 0;
        const limit = 100;
        let total = 1;

        while (offset < total && offset <= 1500) {
            const resultsUrl = APIS.raceResults+`&limit=${limit}&offset=${offset}`;
            console.log(`Fetching race results chunk for constructors: offset ${offset}...`);
            const resultsRes = await axios.get(resultsUrl, { timeout: 15000 });
            total = parseInt(resultsRes.data?.MRData?.total) || 0;
            const races = resultsRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of races) {
                const results = r.Results || [];
                for (const res of results) {
                    const cId = res.Constructor?.constructorId;
                    if (!cId) continue;
                    const pos = parseInt(res.position);

                    if (pos === 1) {
                        winsMap[cId] = (winsMap[cId] || 0) + 1;
                    }
                    if (pos <= 3) {
                        podiumsMap[cId] = (podiumsMap[cId] || 0) + 1;
                    }

                    const posText = res.positionText || "";
                    const status = res.status || "";
                    const isFinished = status.includes("Finished") || status.includes("Lap") || status.startsWith("+");
                    const isDnf = posText === "R" || posText === "D" || posText === "W" || !isFinished;

                    if (isDnf) {
                        dnfsMap[cId] = (dnfsMap[cId] || 0) + 1;
                    }
                }
            }

            offset += limit;
            if (races.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch full race results: ${e.message}`);
    }

    // 4. Build consolidated stats dictionary by constructorId
    const statsByConstructorId = {};

    for (const standing of constructorStandings) {
        const cId = standing.Constructor.constructorId;
        const winsFromStandings = parseInt(standing.wins) || 0;
        const calculatedWins = winsMap[cId] || 0;
        const wins = Math.max(winsFromStandings, calculatedWins).toString();

        statsByConstructorId[cId] = {
            wins: wins,
            podiums: (podiumsMap[cId] || 0).toString(),
            dnfs: (dnfsMap[cId] || 0).toString(),
            poles: (polesMap[cId] || 0).toString(),
            season_position: (standing.position || "-").toString(),
            season_points: (standing.points || "0").toString()
        };
    }

    // Also include any constructor that participated in a race or qualifying but has 0 points
    const allEncounteredConstructorIds = new Set([
        ...Object.keys(statsByConstructorId),
        ...Object.keys(podiumsMap),
        ...Object.keys(winsMap),
        ...Object.keys(dnfsMap),
        ...Object.keys(polesMap)
    ]);

    for (const cId of allEncounteredConstructorIds) {
        if (!statsByConstructorId[cId]) {
            statsByConstructorId[cId] = {
                wins: (winsMap[cId] || 0).toString(),
                podiums: (podiumsMap[cId] || 0).toString(),
                dnfs: (dnfsMap[cId] || 0).toString(),
                poles: (polesMap[cId] || 0).toString(),
                season_position: "-",
                season_points: "0"
            };
        }
    }

    return {
        season,
        statsByConstructorId
    };
}

async function syncConstructorSeasonStats(db, targetSeason) {
    console.log(`Starting constructor season_stats sync...`);
    const { season, statsByConstructorId } = await fetchAndCalculateConstructorSeasonStats();

    const teamsSnap = await db.ref(PATHS.teams).once("value");
    if (!teamsSnap.exists()) {
        console.log("No teams node found in database.");
        return { season, updatedCount: 0 };
    }

    const teamsData = teamsSnap.val() || {};
    const multiPathUpdates = {};
    let updatedCount = 0;

    for (const constructorId of Object.keys(teamsData)) {
        const stats = statsByConstructorId[constructorId] || {
            wins: "0",
            podiums: "0",
            dnfs: "0",
            poles: "0",
            season_position: "-",
            season_points: "0"
        };

        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_stats`] = stats;

        // Clean up legacy fields as season_stats is now the single source of truth
        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_wins`] = null;
        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_podiums`] = null;
        updatedCount++;
    }

    if (Object.keys(multiPathUpdates).length > 0) {
        await db.ref().update(multiPathUpdates);
        console.log(`Successfully updated season_stats for ${updatedCount} constructors (Season ${season}).`);
    }

    return {
        season,
        updatedCount,
        stats: statsByConstructorId
    };
}

/*
* -----------------------------------------------------------------
* CALENDAR-BASED RACE STATS SCHEDULING & POST-RACE PENALTY VERIFICATION
* -----------------------------------------------------------------
*/

/**
 * Detects variations between previous results snapshot and latest results snapshot (e.g. post-race penalties, DSQ).
 */
function detectResultVariations(oldSnapshot, newSnapshot) {
    if (!oldSnapshot || !Array.isArray(oldSnapshot) || oldSnapshot.length === 0) {
        return { hasChanges: false, isInitial: true, changes: [] };
    }
    if (!newSnapshot || !Array.isArray(newSnapshot) || newSnapshot.length === 0) {
        return { hasChanges: false, isInitial: false, changes: [] };
    }

    const oldMap = new Map();
    for (const item of oldSnapshot) {
        if (item && item.driverId) {
            oldMap.set(item.driverId, item);
        }
    }

    const changes = [];
    for (const newItem of newSnapshot) {
        if (!newItem || !newItem.driverId) continue;
        const oldItem = oldMap.get(newItem.driverId);

        if (!oldItem) {
            changes.push({
                driverId: newItem.driverId,
                type: "added",
                newPos: newItem.position,
                newPoints: newItem.points
            });
            continue;
        }

        const posChanged = String(oldItem.position) !== String(newItem.position);
        const pointsChanged = String(oldItem.points) !== String(newItem.points);
        const statusChanged = String(oldItem.status || "") !== String(newItem.status || "");
        const posTextChanged = String(oldItem.positionText || "") !== String(newItem.positionText || "");

        if (posChanged || pointsChanged || statusChanged || posTextChanged) {
            changes.push({
                driverId: newItem.driverId,
                oldPos: oldItem.position,
                newPos: newItem.position,
                oldPoints: oldItem.points,
                newPoints: newItem.points,
                oldStatus: oldItem.status,
                newStatus: newItem.status,
                oldPosText: oldItem.positionText,
                newPosText: newItem.positionText
            });
        }
    }

    return {
        hasChanges: changes.length > 0,
        changes: changes
    };
}

/**
 * Adjusts career podiums and wins if post-race penalties changed the top 3 or winner.
 */
async function adjustCareerStatsForVariations(db, oldSnapshot, newSnapshot) {
    if (!oldSnapshot || !newSnapshot) return;

    const oldTop3Drivers = new Set();
    const newTop3Drivers = new Set();
    let oldWinnerDriver = null;
    let newWinnerDriver = null;

    const oldTop3Teams = new Set();
    const newTop3Teams = new Set();
    let oldWinnerTeam = null;
    let newWinnerTeam = null;

    for (const item of oldSnapshot) {
        const pos = parseInt(item.position);
        if (pos <= 3) {
            oldTop3Drivers.add(item.driverId);
            oldTop3Teams.add(item.constructorId);
        }
        if (pos === 1) {
            oldWinnerDriver = item.driverId;
            oldWinnerTeam = item.constructorId;
        }
    }

    for (const item of newSnapshot) {
        const pos = parseInt(item.position);
        if (pos <= 3) {
            newTop3Drivers.add(item.driverId);
            newTop3Teams.add(item.constructorId);
        }
        if (pos === 1) {
            newWinnerDriver = item.driverId;
            newWinnerTeam = item.constructorId;
        }
    }

    const updates = {};

    // Driver podium adjustments
    for (const dId of oldTop3Drivers) {
        if (!newTop3Drivers.has(dId)) {
            // Lost podium
            const snap = await db.ref(`${PATHS.drivers}/${dId}/podiums`).once("value");
            const cur = parseInt(snap.val()) || 0;
            updates[`${PATHS.drivers}/${dId}/podiums`] = Math.max(0, cur - 1).toString();
        }
    }
    for (const dId of newTop3Drivers) {
        if (!oldTop3Drivers.has(dId)) {
            // Gained podium
            const snap = await db.ref(`${PATHS.drivers}/${dId}/podiums`).once("value");
            const cur = parseInt(snap.val()) || 0;
            updates[`${PATHS.drivers}/${dId}/podiums`] = (cur + 1).toString();
        }
    }

    // Team podium adjustments
    for (const cId of oldTop3Teams) {
        if (!newTop3Teams.has(cId)) {
            const snap = await db.ref(`${PATHS.teams}/${cId}/podiums`).once("value");
            const cur = parseInt(snap.val()) || 0;
            updates[`${PATHS.teams}/${cId}/podiums`] = Math.max(0, cur - 1).toString();
        }
    }
    for (const cId of newTop3Teams) {
        if (!oldTop3Teams.has(cId)) {
            const snap = await db.ref(`${PATHS.teams}/${cId}/podiums`).once("value");
            const cur = parseInt(snap.val()) || 0;
            updates[`${PATHS.teams}/${cId}/podiums`] = (cur + 1).toString();
        }
    }

    // Team win adjustments
    if (oldWinnerTeam && newWinnerTeam && oldWinnerTeam !== newWinnerTeam) {
        const oldWinSnap = await db.ref(`${PATHS.teams}/${oldWinnerTeam}/wins`).once("value");
        const oldWins = parseInt(oldWinSnap.val()) || 0;
        updates[`${PATHS.teams}/${oldWinnerTeam}/wins`] = Math.max(0, oldWins - 1).toString();

        const newWinSnap = await db.ref(`${PATHS.teams}/${newWinnerTeam}/wins`).once("value");
        const newWins = parseInt(newWinSnap.val()) || 0;
        updates[`${PATHS.teams}/${newWinnerTeam}/wins`] = (newWins + 1).toString();
    }

    if (Object.keys(updates).length > 0) {
        await db.ref().update(updates);
        console.log(`Applied career stats adjustments for post-race penalty variations.`);
    }
}

/**
 * Sends FCM push notification for race stats updates or post-race penalty variations.
 */
async function sendRaceStatsNotification(messaging, { title, body, raceName, round, isVariation = false }) {
    if (!messaging) {
        console.log("Messaging instance not provided, skipping push notification.");
        return null;
    }
    // Only send notifications when post-race penalties or sanctions cause variations
    if (!isVariation) {
        return null;
    }
    try {
        const payload = {
            topic: "session_f1_race",
            notification: {
                title: title,
                body: body
            },
            data: {
                type: "session",
                EXTRA_TARGET_TAB: "standings",
                title: title,
                body: body,
                raceName: raceName || "",
                round: (round || "").toString(),
                isPenaltyVariation: isVariation ? "true" : "false"
            },
            android: {
                priority: "high",
                notification: {
                    channelId: "fastestlap_sessions_v4",
                    sound: "team_radio",
                    defaultSound: false,
                    priority: "high",
                    visibility: "public"
                }
            }
        };

        const response = await messaging.send(payload);
        console.log(`🔥 [FCM PUSH RACE STATS SENT]: ${title} - ${body} (Message ID: ${response})`);
        return response;
    } catch (err) {
        console.warn(`Could not send race stats notification: ${err.message}`);
        return null;
    }
}

/**
 * Finds all races in the DB calendar that are active in the update/retry window or pending.
 */
async function findRacesForUpdate(db, now = Date.now(), options = {}) {
    const currentYear = options.year || options.season || new Date().getFullYear();
    const f1CalRef = db.ref(`${PATHS.db_calendar_f1}/${currentYear}`);
    let f1Snap = await f1CalRef.once("value");

    if (!f1Snap.exists() || !f1Snap.val()) {
        console.log(`F1 calendar for ${currentYear} not found in DB. Fetching via calendarLogic...`);
        const calendarLogic = require("./calendar_logic");
        await calendarLogic.fetchAndStoreF1Calendar(db, currentYear);
        f1Snap = await f1CalRef.once("value");
    }

    const calendarData = f1Snap.val() || {};
    const trackerSnap = await db.ref(PATHS.tracker).once("value");
    const trackerData = trackerSnap.val() || {};
    const raceUpdates = trackerData.race_updates?.[currentYear] || {};
    const lastRaceUpdated = trackerData.last_race_updated || 0;

    const candidateRaces = [];
    const racesList = Array.isArray(calendarData) ? calendarData : Object.values(calendarData);

    for (const race of racesList) {
        if (!race || !race.round) continue;
        const round = parseInt(race.round);
        if (isNaN(round)) continue;

        // If a specific round is requested, skip all other rounds immediately
        if (options.round && round !== options.round) {
            continue;
        }

        const sessions = race.sessions || [];
        const raceSession = sessions.find(s => s.id === "race" || s.type === "race");
        if (!raceSession || !raceSession.startTimeMillis) continue;

        const raceStartTimeMillis = raceSession.startTimeMillis;
        // Window begins ~2 hours after race start
        const windowStartMillis = raceStartTimeMillis + (2 * 60 * 60 * 1000);
        // Window lasts for 12 hours after the initial 2-hour mark (i.e. T+14h from race start)
        const windowEndMillis = windowStartMillis + (12 * 60 * 60 * 1000);

        const currentTracker = raceUpdates[round] || null;
        const status = currentTracker?.status || (round <= lastRaceUpdated ? "completed" : "pending");

        // If force specified for a specific round, bypass timing
        if (options.force && options.round && round === options.round) {
            candidateRaces.push({
                race,
                season: currentYear,
                round,
                forced: true,
                status: currentTracker?.status || "pending",
                raceStartTimeMillis,
                windowStartMillis,
                windowEndMillis,
                tracker: currentTracker
            });
            continue;
        }

        // If options.force is set without specific round, only force uncompleted races that already started
        if (options.force && now >= raceStartTimeMillis && status !== "completed") {
            candidateRaces.push({
                race,
                season: currentYear,
                round,
                forced: true,
                status,
                raceStartTimeMillis,
                windowStartMillis,
                windowEndMillis,
                tracker: currentTracker
            });
            continue;
        }

        // Check if now is within the active post-race window (T+2h to T+14h)
        if (now >= windowStartMillis && now <= windowEndMillis) {
            if (status !== "completed") {
                candidateRaces.push({
                    race,
                    season: currentYear,
                    round,
                    forced: false,
                    status,
                    raceStartTimeMillis,
                    windowStartMillis,
                    windowEndMillis,
                    tracker: currentTracker
                });
            }
        } else if (now > windowEndMillis) {
            // Window has expired. If it was in initial_success, mark completed
            if (status === "initial_success") {
                candidateRaces.push({
                    race,
                    season: currentYear,
                    round,
                    forced: false,
                    status: "expire_window",
                    raceStartTimeMillis,
                    windowStartMillis,
                    windowEndMillis,
                    tracker: currentTracker
                });
            }
        }
    }

    return candidateRaces;
}

/**
 * Orchestrates calendar-based race stats checks, 12h retries, and post-race penalty verifications.
 */
async function executeScheduledRaceStatsCheck(db, messaging, options = {}) {
    const now = Date.now();
    console.log(`[F1 Race Stats Scheduler] Running check at ${new Date(now).toISOString()}...`);

    const candidates = await findRacesForUpdate(db, now, options);

    if (candidates.length === 0) {
        console.log("[F1 Race Stats Scheduler] No races currently in the 2h-14h post-race update window.");
        return { updated: false, reason: "no_active_race_window" };
    }

    const results = [];

    for (const cand of candidates) {
        const { race, season, round, status, tracker, forced, windowEndMillis } = cand;
        const raceName = race.raceName || `Round ${round}`;
        console.log(`[F1 Race Stats Scheduler] Processing Round ${round} (${raceName}) - Status: ${status}`);

        // Handle window expiration
        if (status === "expire_window") {
            console.log(`[F1 Race Stats Scheduler] 12-hour monitoring window closed for Round ${round}. Finalizing.`);
            await db.ref(`${PATHS.race_updates}/${season}/${round}/status`).set("completed");
            await db.ref(`${PATHS.race_updates}/${season}/${round}/completed_at`).set(now);
            results.push({ round, status: "completed", message: "12-hour window concluded, results locked." });
            continue;
        }

        // Throttle check: at least 50 minutes between hourly attempts (unless forced)
        if (!forced && tracker && tracker.last_attempt_millis) {
            const elapsed = now - tracker.last_attempt_millis;
            if (elapsed < 50 * 60 * 1000) {
                const waitMin = Math.round((50 * 60 * 1000 - elapsed) / 60000);
                console.log(`[F1 Race Stats Scheduler] Round ${round}: attempted ${Math.round(elapsed / 60000)}m ago. Next attempt in ~${waitMin}m.`);
                results.push({ round, status: "throttled", waitMinutes: waitMin });
                continue;
            }
        }

        const attemptsCount = (tracker?.attempts_count || 0) + 1;
        const url = `https://api.jolpi.ca/ergast/f1/current/${round}/results/?format=json`;

        let jolpicaRes;
        try {
            console.log(`Fetching results from Jolpica: ${url}`);
            jolpicaRes = await axios.get(url, { timeout: 15000 });
        } catch (fetchErr) {
            console.warn(`[F1 Race Stats Scheduler] Failed to fetch results for Round ${round}: ${fetchErr.message}`);
            await db.ref(`${PATHS.race_updates}/${season}/${round}`).update({
                attempts_count: attemptsCount,
                last_attempt_millis: now,
                last_attempt_iso: new Date(now).toISOString(),
                last_error: fetchErr.message
            });
            results.push({ round, updated: false, error: fetchErr.message, attempt: attemptsCount });
            continue;
        }

        const mrData = jolpicaRes.data?.MRData;
        const total = parseInt(mrData?.total || "0");
        const raceTableRaces = mrData?.RaceTable?.Races;

        if (total === 0 || !raceTableRaces || raceTableRaces.length === 0) {
            console.log(`[F1 Race Stats Scheduler] Results for Round ${round} not yet available on Jolpica (attempt ${attemptsCount}). Will retry next hour.`);
            await db.ref(`${PATHS.race_updates}/${season}/${round}`).update({
                season,
                round,
                race_name: raceName,
                status: "pending",
                attempts_count: attemptsCount,
                last_attempt_millis: now,
                last_attempt_iso: new Date(now).toISOString()
            });
            results.push({ round, updated: false, reason: "results_pending", attempt: attemptsCount });
            continue;
        }

        const raceData = raceTableRaces[0];
        const rawResults = raceData.Results || [];
        const trackId = raceData.Circuit?.circuitId || race.circuit?.id || "";

        const currentSnapshot = rawResults.map(r => ({
            position: r.position,
            positionText: r.positionText,
            driverId: r.Driver?.driverId,
            constructorId: r.Constructor?.constructorId,
            points: r.points,
            status: r.status,
            laps: r.laps
        }));

        // CASE 1: Initial Success (first time results are processed)
        if (status === "pending" || !tracker || !tracker.results_snapshot) {
            console.log(`[F1 Race Stats Scheduler] 🎉 First successful results fetch for Round ${round}! Processing stats...`);

            // Apply career stats and calendar entry
            await executeRaceStatsUpdateWithResults(db, season, round, trackId, rawResults);

            // Sync driver and constructor season stats (comprehensive)
            await syncDriverSeasonStats(db, season);
            await syncConstructorSeasonStats(db, season);

            // Update tracker
            await db.ref(`${PATHS.race_updates}/${season}/${round}`).update({
                season,
                round,
                race_name: raceName,
                status: "initial_success",
                attempts_count: attemptsCount,
                first_success_millis: now,
                first_success_iso: new Date(now).toISOString(),
                last_attempt_millis: now,
                last_attempt_iso: new Date(now).toISOString(),
                results_snapshot: currentSnapshot,
                variations_detected: 0
            });

            await db.ref(PATHS.tracker).update({
                last_race_updated: round,
                last_season_updated: season
            });

            results.push({ round, updated: true, type: "initial_success", attempt: attemptsCount });
            continue;
        }

        // CASE 2: Subsequent Checks -> Verify Post-Race Penalties / Variations
        if (status === "initial_success") {
            const previousSnapshot = tracker.results_snapshot;
            const diff = detectResultVariations(previousSnapshot, currentSnapshot);

            if (!diff.hasChanges) {
                console.log(`[F1 Race Stats Scheduler] Round ${round} penalty check (attempt ${attemptsCount}): No variations detected. Results are stable.`);
                await db.ref(`${PATHS.race_updates}/${season}/${round}`).update({
                    attempts_count: attemptsCount,
                    last_attempt_millis: now,
                    last_attempt_iso: new Date(now).toISOString()
                });
                results.push({ round, updated: false, reason: "no_variations", attempt: attemptsCount });
            } else {
                console.log(`[F1 Race Stats Scheduler] ⚠️ Post-race penalty variation detected for Round ${round}!`);
                console.log(`Changes:`, JSON.stringify(diff.changes, null, 2));

                // 1. Adjust career stats based on old vs new podium/wins
                await adjustCareerStatsForVariations(db, previousSnapshot, currentSnapshot);

                // 2. Update circuit calendar podium (forcing overwrite of previous podium)
                const updates = {};
                await processCircuitRaceUpdate(season, round, trackId, rawResults, updates, db, true);
                if (Object.keys(updates).length > 0) {
                    await db.ref().update(updates);
                }

                // 3. Re-sync driver and constructor season stats (pulls revised season data from Jolpica)
                await syncDriverSeasonStats(db, season);
                await syncConstructorSeasonStats(db, season);

                // 4. Update tracker with new snapshot and record variation history
                const variationEntry = {
                    detected_at: now,
                    detected_iso: new Date(now).toISOString(),
                    attempt: attemptsCount,
                    changes: diff.changes
                };

                const currentVariationsCount = (tracker.variations_detected || 0) + 1;
                await db.ref(`${PATHS.race_updates}/${season}/${round}`).update({
                    results_snapshot: currentSnapshot,
                    attempts_count: attemptsCount,
                    last_attempt_millis: now,
                    last_attempt_iso: new Date(now).toISOString(),
                    last_variation_millis: now,
                    last_variation_iso: new Date(now).toISOString(),
                    variations_detected: currentVariationsCount
                });
                await db.ref(`${PATHS.race_updates}/${season}/${round}/variation_history`).push(variationEntry);

                // 5. Send notification about the post-race penalty variation
                await sendRaceStatsNotification(messaging, {
                    title: `⚠️ Penalità Post-Gara F1: ${raceName}`,
                    body: `Applicate sanzioni post-gara! Classifiche e statistiche aggiornate per il ${raceName}.`,
                    raceName,
                    round,
                    isVariation: true
                });

                results.push({ round, updated: true, type: "penalty_variation_applied", changes: diff.changes });
            }
        }
    }

    return { timestamp: new Date(now).toISOString(), results };
}

/*
* -----------------------------------------------------------------
* FUNCTION EXPORTS
* -----------------------------------------------------------------
*/

// Export main functions and helper functions for testing
module.exports = {
    executeRaceStatsUpdate,
    executeScheduledRaceStatsCheck,
    findRacesForUpdate,
    detectResultVariations,
    adjustCareerStatsForVariations,
    sendRaceStatsNotification,
    executeChampionshipsUpdate,
    syncDriverSeasonStats,
    fetchAndCalculateDriverSeasonStats,
    syncConstructorSeasonStats,
    fetchAndCalculateConstructorSeasonStats,
    syncAllCareerStats: careerStatsLogic.syncAllCareerStats,
    fetchDriverCareerStats: careerStatsLogic.fetchDriverCareerStats,
    fetchConstructorCareerStats: careerStatsLogic.fetchConstructorCareerStats,

    // Helper functions for testing
    loadMappings,
    processCircuitRaceUpdate,
    processDriverSeasonArchive,
    processConstructorSeasonArchive,
    processCircuitSeasonArchive
};
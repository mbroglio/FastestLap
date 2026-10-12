const axios = require("axios");
const { PATHS, APIS } = require("./f1_constants");

function parsePositionString(res) {
    const posText = (res.positionText || "").trim();
    const status = (res.status || "").toLowerCase();
    const isFinished = status.includes("finished") || status.includes("lap") || status.startsWith("+");
    const isDns = posText === "DNS" || posText === "W" || status.includes("did not start");
    const isDnf = posText === "R" || posText === "D" || posText === "W" || posText === "NC" || !isFinished;

    if (isDns) return "DNS";
    if (isDnf) return "DNF";
    return String(res.position || posText || "-");
}

function getConstructorDriverSlot(cId, dId, constructorSeasonResults, round, keyPrefix, teamsDriversMap) {
    const teamDrivers = teamsDriversMap[cId] || [];
    if (teamDrivers.length > 0) {
        if (dId === teamDrivers[0]) return "1";
        if (dId === teamDrivers[1]) return "2";
    }
    const currentRound = (constructorSeasonResults[cId] && constructorSeasonResults[cId][round]) || {};
    if (!currentRound[`${keyPrefix}_driver_1`]) return "1";
    return "2";
}

/**
 * ROUND-SPECIFIC STATS UPDATE:
 * Fetches only the given round results (race, qualifying, sprint) from Jolpica API:
 *   https://api.jolpi.ca/ergast/f1/{season}/{round}/results/?format=json
 *   https://api.jolpi.ca/ergast/f1/{season}/{round}/qualifying/?format=json
 *   https://api.jolpi.ca/ergast/f1/{season}/{round}/sprint/?format=json
 * Updates season_results[round] and refreshes standings on Firebase.
 */
async function syncSeasonStatsForRound(db, round, targetSeason = "current", options = {}) {
    const season = targetSeason ? String(targetSeason) : "current";
    const roundStr = String(round);
    console.log(`[Season Stats] Syncing targeted stats for Season ${season}, Round ${roundStr}...`);

    const roundResultsUrl = `https://api.jolpi.ca/ergast/f1/${season}/${roundStr}/results/?format=json`;
    const roundQualUrl = `https://api.jolpi.ca/ergast/f1/${season}/${roundStr}/qualifying/?format=json`;
    const roundSprintUrl = `https://api.jolpi.ca/ergast/f1/${season}/${roundStr}/sprint/?format=json`;

    const resultsPromise = options.resultsRes 
        ? Promise.resolve(options.resultsRes) 
        : axios.get(roundResultsUrl, { timeout: 15000 }).catch(e => {
            console.warn(`Could not fetch race results for round ${roundStr}: ${e.message}`);
            return { data: null };
        });

    const qualPromise = options.qualRes
        ? Promise.resolve(options.qualRes)
        : axios.get(roundQualUrl, { timeout: 15000 }).catch(e => {
            console.warn(`Could not fetch qualifying results for round ${roundStr}: ${e.message}`);
            return { data: null };
        });

    const sprintPromise = options.sprintRes
        ? Promise.resolve(options.sprintRes)
        : axios.get(roundSprintUrl, { timeout: 15000 }).catch(e => {
            console.warn(`Could not fetch sprint results for round ${roundStr}: ${e.message}`);
            return { data: null };
        });

    const driverStandingsPromise = options.driverStandingsRes
        ? Promise.resolve(options.driverStandingsRes)
        : axios.get(APIS.driverStandings, { timeout: 15000 }).catch(e => {
            console.warn(`Could not fetch driver standings: ${e.message}`);
            return { data: null };
        });

    const constructorStandingsPromise = options.constructorStandingsRes
        ? Promise.resolve(options.constructorStandingsRes)
        : axios.get(APIS.constructorStandings, { timeout: 15000 }).catch(e => {
            console.warn(`Could not fetch constructor standings: ${e.message}`);
            return { data: null };
        });

    const [resultsRes, qualRes, sprintRes, driverStandingsRes, constructorStandingsRes] = await Promise.all([
        resultsPromise,
        qualPromise,
        sprintPromise,
        driverStandingsPromise,
        constructorStandingsPromise
    ]);

    const [driversSnap, teamsSnap] = await Promise.all([
        db.ref(PATHS.drivers).once("value"),
        db.ref(PATHS.teams).once("value")
    ]);

    const driversData = driversSnap.val() || {};
    const teamsData = teamsSnap.val() || {};

    const teamsDriversMap = {};
    for (const [cId, teamObj] of Object.entries(teamsData)) {
        teamsDriversMap[cId] = (teamObj && teamObj.drivers) || [];
    }

    const multiPathUpdates = {};

    const raceList = resultsRes.data?.MRData?.RaceTable?.Races?.[0]?.Results || [];
    const qualList = qualRes.data?.MRData?.RaceTable?.Races?.[0]?.QualifyingResults || [];
    const sprintList = sprintRes.data?.MRData?.RaceTable?.Races?.[0]?.SprintResults || [];

    const driverRoundResults = {};
    const constructorRoundResults = {};

    for (const res of raceList) {
        const dId = res.Driver?.driverId;
        const cId = res.Constructor?.constructorId;
        const posStr = parsePositionString(res);

        if (dId) {
            if (!driverRoundResults[dId]) driverRoundResults[dId] = {};
            driverRoundResults[dId].race = posStr;
        }

        if (cId) {
            if (!constructorRoundResults[cId]) constructorRoundResults[cId] = {};
            const slot = getConstructorDriverSlot(cId, dId, { [cId]: { [roundStr]: constructorRoundResults[cId] } }, roundStr, "race", teamsDriversMap);
            constructorRoundResults[cId][`race_driver_${slot}`] = posStr;
        }
    }

    for (const res of qualList) {
        const dId = res.Driver?.driverId;
        const cId = res.Constructor?.constructorId;
        const posStr = String(res.position || "");

        if (dId) {
            if (!driverRoundResults[dId]) driverRoundResults[dId] = {};
            driverRoundResults[dId].qualifying = posStr;
        }

        if (cId) {
            if (!constructorRoundResults[cId]) constructorRoundResults[cId] = {};
            const slot = getConstructorDriverSlot(cId, dId, { [cId]: { [roundStr]: constructorRoundResults[cId] } }, roundStr, "qualifying", teamsDriversMap);
            constructorRoundResults[cId][`qualifying_driver_${slot}`] = posStr;
        }
    }

    for (const res of sprintList) {
        const dId = res.Driver?.driverId;
        const cId = res.Constructor?.constructorId;
        const posStr = parsePositionString(res);

        if (dId) {
            if (!driverRoundResults[dId]) driverRoundResults[dId] = {};
            driverRoundResults[dId].sprint_race = posStr;
        }

        if (cId) {
            if (!constructorRoundResults[cId]) constructorRoundResults[cId] = {};
            const slot = getConstructorDriverSlot(cId, dId, { [cId]: { [roundStr]: constructorRoundResults[cId] } }, roundStr, "sprint_race", teamsDriversMap);
            constructorRoundResults[cId][`sprint_race_driver_${slot}`] = posStr;
        }
    }

    const driverStandingsLists = driverStandingsRes.data?.MRData?.StandingsTable?.StandingsLists || [];
    const driverStandings = (driverStandingsLists.length > 0 && driverStandingsLists[0].DriverStandings) ? driverStandingsLists[0].DriverStandings : [];
    const driverStandingsMap = {};
    for (const s of driverStandings) {
        const dId = s.Driver?.driverId;
        if (dId) {
            driverStandingsMap[dId] = {
                season_position: (s.position || "-").toString(),
                season_points: (s.points || "0").toString(),
                wins: (s.wins || "0").toString()
            };
        }
    }

    const constrStandingsLists = constructorStandingsRes.data?.MRData?.StandingsTable?.StandingsLists || [];
    const constrStandings = (constrStandingsLists.length > 0 && constrStandingsLists[0].ConstructorStandings) ? constrStandingsLists[0].ConstructorStandings : [];
    const constrStandingsMap = {};
    for (const s of constrStandings) {
        const cId = s.Constructor?.constructorId;
        if (cId) {
            constrStandingsMap[cId] = {
                season_position: (s.position || "-").toString(),
                season_points: (s.points || "0").toString(),
                wins: (s.wins || "0").toString()
            };
        }
    }

    // Update drivers
    for (const driverId of Object.keys(driversData)) {
        const currentStats = driversData[driverId]?.season_stats || {};
        const existingResults = currentStats.season_results || {};

        if (driverRoundResults[driverId]) {
            existingResults[roundStr] = {
                ...(existingResults[roundStr] || {}),
                ...driverRoundResults[driverId]
            };
        }

        let podiums = 0;
        let dnfs = 0;
        let poles = 0;
        for (const [rKey, rVal] of Object.entries(existingResults)) {
            const p = parseInt(rVal.race);
            if (!isNaN(p) && p <= 3) podiums++;
            if (rVal.race === "DNF" || rVal.race === "DNS") dnfs++;
            if (rVal.qualifying === "1") poles++;
        }

        const standing = driverStandingsMap[driverId] || {
            season_position: currentStats.season_position || "-",
            season_points: currentStats.season_points || "0",
            wins: currentStats.wins || "0"
        };

        multiPathUpdates[`${PATHS.drivers}/${driverId}/season_stats`] = {
            wins: standing.wins,
            podiums: podiums.toString(),
            dnfs: dnfs.toString(),
            poles: poles.toString(),
            season_position: standing.season_position,
            season_points: standing.season_points,
            season_results: existingResults
        };
    }

    // Update teams
    for (const constructorId of Object.keys(teamsData)) {
        const currentStats = teamsData[constructorId]?.season_stats || {};
        const existingResults = currentStats.season_results || {};

        if (constructorRoundResults[constructorId]) {
            existingResults[roundStr] = {
                ...(existingResults[roundStr] || {}),
                ...constructorRoundResults[constructorId]
            };
        }

        let podiums = 0;
        let dnfs = 0;
        let poles = 0;
        for (const [rKey, rVal] of Object.entries(existingResults)) {
            const p1 = parseInt(rVal.race_driver_1);
            const p2 = parseInt(rVal.race_driver_2);
            if (!isNaN(p1) && p1 <= 3) podiums++;
            if (!isNaN(p2) && p2 <= 3) podiums++;
            if (rVal.race_driver_1 === "DNF" || rVal.race_driver_1 === "DNS") dnfs++;
            if (rVal.race_driver_2 === "DNF" || rVal.race_driver_2 === "DNS") dnfs++;
            if (rVal.qualifying_driver_1 === "1" || rVal.qualifying_driver_2 === "1") poles++;
        }

        const standing = constrStandingsMap[constructorId] || {
            season_position: currentStats.season_position || "-",
            season_points: currentStats.season_points || "0",
            wins: currentStats.wins || "0"
        };

        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_stats`] = {
            wins: standing.wins,
            podiums: podiums.toString(),
            dnfs: dnfs.toString(),
            poles: poles.toString(),
            season_position: standing.season_position,
            season_points: standing.season_points,
            season_results: existingResults
        };
        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_wins`] = null;
        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_podiums`] = null;
    }

    if (Object.keys(multiPathUpdates).length > 0) {
        await db.ref().update(multiPathUpdates);
        console.log(`[Season Stats] Successfully updated season_stats for Round ${roundStr} (Season ${season}).`);
    }

    return { success: true, season, round: roundStr };
}

/**
 * INITIAL / FULL SEASON STATS CALCULATION:
 * Used for cold-start or initial recovery of all uncontrolled rounds.
 */
async function fetchAndCalculateDriverSeasonStats(targetSeason) {
    const standingsRes = await axios.get(APIS.driverStandings, { timeout: 15000 });
    const standingsTable = standingsRes.data?.MRData?.StandingsTable;
    const season = (targetSeason ? String(targetSeason) : (standingsTable?.season ? String(standingsTable.season) : new Date().getFullYear().toString()));

    const standingsLists = standingsTable?.StandingsLists || [];
    const driverStandings = (standingsLists.length > 0 && standingsLists[0].DriverStandings) ? standingsLists[0].DriverStandings : [];

    // 1. Fetch Pole Positions
    const polesMap = {};
    try {
        console.log(`Fetching F1 pole positions`);
        const polesRes = await axios.get(APIS.qualifyingResultsPoles + "&limit=100", { timeout: 15000 });
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

    const podiumsMap = {};
    const winsMap = {};
    const dnfsMap = {};
    const driverSeasonResults = {};

    // 2. Fetch Race Results (Single call with limit 1000)
    try {
        let offset = 0;
        const limit = 1000;
        let total = 1;

        while (offset < total && offset <= 2000) {
            const resultsUrl = `https://api.jolpi.ca/ergast/f1/${season}/results/?format=json&limit=${limit}&offset=${offset}`;
            console.log(`Fetching race results chunk: offset ${offset}...`);
            const resultsRes = await axios.get(resultsUrl, { timeout: 15000 });
            total = parseInt(resultsRes.data?.MRData?.total) || 0;
            const actualLimit = parseInt(resultsRes.data?.MRData?.limit) || limit;
            const races = resultsRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of races) {
                const round = String(r.round);
                const results = r.Results || [];
                for (const res of results) {
                    if (!res.Driver || !res.Driver.driverId) continue;
                    const dId = res.Driver.driverId;
                    const pos = parseInt(res.position);

                    if (pos === 1) winsMap[dId] = (winsMap[dId] || 0) + 1;
                    if (pos <= 3) podiumsMap[dId] = (podiumsMap[dId] || 0) + 1;

                    const posStr = parsePositionString(res);
                    if (posStr === "DNF" || posStr === "DNS") {
                        dnfsMap[dId] = (dnfsMap[dId] || 0) + 1;
                    }

                    if (!driverSeasonResults[dId]) driverSeasonResults[dId] = {};
                    if (!driverSeasonResults[dId][round]) driverSeasonResults[dId][round] = {};
                    driverSeasonResults[dId][round].race = posStr;
                }
            }

            offset += actualLimit;
            if (races.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch full race results: ${e.message}`);
    }

    // 3. Fetch Qualifying Results
    try {
        let offset = 0;
        const limit = 100;
        let total = 1;

        while (offset < total && offset <= 2000) {
            const qUrl = `https://api.jolpi.ca/ergast/f1/${season}/qualifying/?format=json&limit=${limit}&offset=${offset}`;
            console.log(`Fetching qualifying results chunk: offset ${offset}...`);
            const qRes = await axios.get(qUrl, { timeout: 15000 });
            total = parseInt(qRes.data?.MRData?.total) || 0;
            const actualLimit = parseInt(qRes.data?.MRData?.limit) || limit;
            const qRaces = qRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of qRaces) {
                const round = String(r.round);
                for (const res of r.QualifyingResults || []) {
                    const dId = res.Driver?.driverId;
                    if (!dId) continue;
                    if (!driverSeasonResults[dId]) driverSeasonResults[dId] = {};
                    if (!driverSeasonResults[dId][round]) driverSeasonResults[dId][round] = {};
                    driverSeasonResults[dId][round].qualifying = String(res.position || "");
                }
            }

            offset += actualLimit;
            if (qRaces.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch qualifying results: ${e.message}`);
    }

    // 4. Fetch Sprint Results
    try {
        let offset = 0;
        const limit = 100;
        let total = 1;

        while (offset < total && offset <= 2000) {
            const sUrl = `https://api.jolpi.ca/ergast/f1/${season}/sprint/?format=json&limit=${limit}&offset=${offset}`;
            console.log(`Fetching sprint results chunk: offset ${offset}...`);
            const sRes = await axios.get(sUrl, { timeout: 15000 });
            total = parseInt(sRes.data?.MRData?.total) || 0;
            const actualLimit = parseInt(sRes.data?.MRData?.limit) || limit;
            const sRaces = sRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of sRaces) {
                const round = String(r.round);
                for (const res of r.SprintResults || []) {
                    const dId = res.Driver?.driverId;
                    if (!dId) continue;
                    const posStr = parsePositionString(res);
                    if (!driverSeasonResults[dId]) driverSeasonResults[dId] = {};
                    if (!driverSeasonResults[dId][round]) driverSeasonResults[dId][round] = {};
                    driverSeasonResults[dId][round].sprint_race = posStr;
                }
            }

            offset += actualLimit;
            if (sRaces.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch sprint results: ${e.message}`);
    }

    // 5. Build consolidated stats dictionary
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
            season_points: (standing.points || "0").toString(),
            season_results: driverSeasonResults[dId] || {}
        };
    }

    const allEncounteredDriverIds = new Set([
        ...Object.keys(statsByDriverId),
        ...Object.keys(podiumsMap),
        ...Object.keys(winsMap),
        ...Object.keys(dnfsMap),
        ...Object.keys(polesMap),
        ...Object.keys(driverSeasonResults)
    ]);

    for (const dId of allEncounteredDriverIds) {
        if (!statsByDriverId[dId]) {
            statsByDriverId[dId] = {
                wins: (winsMap[dId] || 0).toString(),
                podiums: (podiumsMap[dId] || 0).toString(),
                dnfs: (dnfsMap[dId] || 0).toString(),
                poles: (polesMap[dId] || 0).toString(),
                season_position: "-",
                season_points: "0",
                season_results: driverSeasonResults[dId] || {}
            };
        }
    }

    return {
        season,
        statsByDriverId
    };
}

async function syncDriverSeasonStats(db, targetSeason) {
    console.log(`Starting full driver season_stats sync...`);
    const { season, statsByDriverId } = await fetchAndCalculateDriverSeasonStats(targetSeason);

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
            season_points: "0",
            season_results: {}
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

async function fetchAndCalculateConstructorSeasonStats(targetSeason, teamsDriversMap = {}) {
    console.log(`Fetching F1 constructor standings`);
    const standingsRes = await axios.get(APIS.constructorStandings, { timeout: 15000 });
    const standingsTable = standingsRes.data?.MRData?.StandingsTable;
    const season = (targetSeason ? String(targetSeason) : (standingsTable?.season ? String(standingsTable.season) : new Date().getFullYear().toString()));

    const standingsLists = standingsTable?.StandingsLists || [];
    const constructorStandings = (standingsLists.length > 0 && standingsLists[0].ConstructorStandings) ? standingsLists[0].ConstructorStandings : [];

    // 1. Fetch Pole Positions
    const polesMap = {};
    try {
        console.log(`Fetching F1 pole positions for constructors`);
        const polesRes = await axios.get(APIS.qualifyingResultsPoles + "&limit=100", { timeout: 15000 });
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

    const podiumsMap = {};
    const winsMap = {};
    const dnfsMap = {};
    const constructorSeasonResults = {};

    // 2. Fetch Race Results
    try {
        let offset = 0;
        const limit = 1000;
        let total = 1;

        while (offset < total && offset <= 2000) {
            const resultsUrl = `https://api.jolpi.ca/ergast/f1/${season}/results/?format=json&limit=${limit}&offset=${offset}`;
            console.log(`Fetching race results chunk for constructors: offset ${offset}...`);
            const resultsRes = await axios.get(resultsUrl, { timeout: 15000 });
            total = parseInt(resultsRes.data?.MRData?.total) || 0;
            const actualLimit = parseInt(resultsRes.data?.MRData?.limit) || limit;
            const races = resultsRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of races) {
                const round = String(r.round);
                const results = r.Results || [];
                for (const res of results) {
                    const cId = res.Constructor?.constructorId;
                    const dId = res.Driver?.driverId;
                    if (!cId) continue;
                    const pos = parseInt(res.position);

                    if (pos === 1) winsMap[cId] = (winsMap[cId] || 0) + 1;
                    if (pos <= 3) podiumsMap[cId] = (podiumsMap[cId] || 0) + 1;

                    const posStr = parsePositionString(res);
                    if (posStr === "DNF" || posStr === "DNS") {
                        dnfsMap[cId] = (dnfsMap[cId] || 0) + 1;
                    }

                    if (!constructorSeasonResults[cId]) constructorSeasonResults[cId] = {};
                    if (!constructorSeasonResults[cId][round]) constructorSeasonResults[cId][round] = {};

                    const slot = getConstructorDriverSlot(cId, dId, constructorSeasonResults, round, "race", teamsDriversMap);
                    constructorSeasonResults[cId][round][`race_driver_${slot}`] = posStr;
                }
            }

            offset += actualLimit;
            if (races.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch full race results for constructors: ${e.message}`);
    }

    // 3. Fetch Qualifying Results
    try {
        let offset = 0;
        const limit = 100;
        let total = 1;

        while (offset < total && offset <= 2000) {
            const qUrl = `https://api.jolpi.ca/ergast/f1/${season}/qualifying/?format=json&limit=${limit}&offset=${offset}`;
            console.log(`Fetching qualifying results chunk for constructors: offset ${offset}...`);
            const qRes = await axios.get(qUrl, { timeout: 15000 });
            total = parseInt(qRes.data?.MRData?.total) || 0;
            const actualLimit = parseInt(qRes.data?.MRData?.limit) || limit;
            const qRaces = qRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of qRaces) {
                const round = String(r.round);
                for (const res of r.QualifyingResults || []) {
                    const cId = res.Constructor?.constructorId;
                    const dId = res.Driver?.driverId;
                    if (!cId) continue;

                    if (!constructorSeasonResults[cId]) constructorSeasonResults[cId] = {};
                    if (!constructorSeasonResults[cId][round]) constructorSeasonResults[cId][round] = {};

                    const slot = getConstructorDriverSlot(cId, dId, constructorSeasonResults, round, "qualifying", teamsDriversMap);
                    constructorSeasonResults[cId][round][`qualifying_driver_${slot}`] = String(res.position || "");
                }
            }

            offset += actualLimit;
            if (qRaces.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch qualifying results for constructors: ${e.message}`);
    }

    // 4. Fetch Sprint Results
    try {
        let offset = 0;
        const limit = 100;
        let total = 1;

        while (offset < total && offset <= 2000) {
            const sUrl = `https://api.jolpi.ca/ergast/f1/${season}/sprint/?format=json&limit=${limit}&offset=${offset}`;
            console.log(`Fetching sprint results chunk for constructors: offset ${offset}...`);
            const sRes = await axios.get(sUrl, { timeout: 15000 });
            total = parseInt(sRes.data?.MRData?.total) || 0;
            const actualLimit = parseInt(sRes.data?.MRData?.limit) || limit;
            const sRaces = sRes.data?.MRData?.RaceTable?.Races || [];

            for (const r of sRaces) {
                const round = String(r.round);
                for (const res of r.SprintResults || []) {
                    const cId = res.Constructor?.constructorId;
                    const dId = res.Driver?.driverId;
                    if (!cId) continue;

                    const posStr = parsePositionString(res);
                    if (!constructorSeasonResults[cId]) constructorSeasonResults[cId] = {};
                    if (!constructorSeasonResults[cId][round]) constructorSeasonResults[cId][round] = {};

                    const slot = getConstructorDriverSlot(cId, dId, constructorSeasonResults, round, "sprint_race", teamsDriversMap);
                    constructorSeasonResults[cId][round][`sprint_race_driver_${slot}`] = posStr;
                }
            }

            offset += actualLimit;
            if (sRaces.length === 0) break;
        }
    } catch (e) {
        console.warn(`Could not fetch sprint results for constructors: ${e.message}`);
    }

    // 5. Build consolidated stats dictionary
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
            season_points: (standing.points || "0").toString(),
            season_results: constructorSeasonResults[cId] || {}
        };
    }

    const allEncounteredConstructorIds = new Set([
        ...Object.keys(statsByConstructorId),
        ...Object.keys(podiumsMap),
        ...Object.keys(winsMap),
        ...Object.keys(dnfsMap),
        ...Object.keys(polesMap),
        ...Object.keys(constructorSeasonResults)
    ]);

    for (const cId of allEncounteredConstructorIds) {
        if (!statsByConstructorId[cId]) {
            statsByConstructorId[cId] = {
                wins: (winsMap[cId] || 0).toString(),
                podiums: (podiumsMap[cId] || 0).toString(),
                dnfs: (dnfsMap[cId] || 0).toString(),
                poles: (polesMap[cId] || 0).toString(),
                season_position: "-",
                season_points: "0",
                season_results: constructorSeasonResults[cId] || {}
            };
        }
    }

    return {
        season,
        statsByConstructorId
    };
}

async function syncConstructorSeasonStats(db, targetSeason) {
    console.log(`Starting full constructor season_stats sync...`);
    const teamsSnap = await db.ref(PATHS.teams).once("value");
    if (!teamsSnap.exists()) {
        console.log("No teams node found in database.");
        return { season: targetSeason || new Date().getFullYear().toString(), updatedCount: 0 };
    }

    const teamsData = teamsSnap.val() || {};
    const teamsDriversMap = {};
    for (const [cId, teamObj] of Object.entries(teamsData)) {
        teamsDriversMap[cId] = (teamObj && teamObj.drivers) || [];
    }

    const { season, statsByConstructorId } = await fetchAndCalculateConstructorSeasonStats(targetSeason, teamsDriversMap);

    const multiPathUpdates = {};
    let updatedCount = 0;

    for (const constructorId of Object.keys(teamsData)) {
        const stats = statsByConstructorId[constructorId] || {
            wins: "0",
            podiums: "0",
            dnfs: "0",
            poles: "0",
            season_position: "-",
            season_points: "0",
            season_results: {}
        };

        multiPathUpdates[`${PATHS.teams}/${constructorId}/season_stats`] = stats;
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

module.exports = {
    syncSeasonStatsForRound,
    fetchAndCalculateDriverSeasonStats,
    syncDriverSeasonStats,
    fetchAndCalculateConstructorSeasonStats,
    syncConstructorSeasonStats
};

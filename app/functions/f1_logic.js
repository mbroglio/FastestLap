const axios = require("axios");

const PATHS = {
    drivers: "drivers",
    teams: "teams",
    tracker: "app_config/stats_tracker"
};

const APIS = {
    raceResults: "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json",
    driverStandings: "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json",
    constructorStandings: "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json"
};

const TEAM_ID_NAME_MAP = {
  "mercedes": "Mercedes", "red_bull": "Red Bull", "ferrari": "Ferrari", "mclaren": "McLaren",
  "alpine": "Alpine", "rb": "Racing Bulls", "aston_martin": "Aston Martin", "williams": "Williams",
  "haas": "Haas", "sauber": "KICK Sauber"
};

// Race Update Logic
async function executeRaceStatsUpdate(db) {
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
    const multiPathUpdates = {};
    const results = raceInfo.Results;
    const constructorUpdates = {};

    for (const result of results) {
        const driverId = result.Driver.driverId;
        const constructorId = result.Constructor.constructorId;
        const position = parseInt(result.position);
        
        const driverRef = db.ref(`${PATHS.drivers}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");

        if (driverSnapshot.exists()) {
            const driverData = driverSnapshot.val();
            if (position <= 3) {
                const currentPodiums = parseInt(driverData.podiums) || 0;
                const seasonPodiums = parseInt(driverData.season_podiums) || 0;
                multiPathUpdates[`${PATHS.drivers}/${driverId}/season_podiums`] = (seasonPodiums + 1).toString();
                multiPathUpdates[`${PATHS.drivers}/${driverId}/podiums`] = (currentPodiums + 1).toString();
                if(position === 1) {
                    const seasonWins = parseInt(driverData.season_wins) || 0;
                    multiPathUpdates[`${PATHS.drivers}/${driverId}/season_wins`] = (seasonWins + 1).toString();
                }
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

    multiPathUpdates[`${PATHS.tracker}/last_season_updated`] = newSeason;
    multiPathUpdates[`${PATHS.tracker}/last_race_updated`] = newRound;
    
    await db.ref().update(multiPathUpdates);
    console.log(`F1 Post-race update complete.`);
}

// Championship Update Logic
async function executeChampionshipsUpdate(db) {
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
    const multiPathUpdates = {};
    const driverStandings = standingsData.StandingsLists[0].DriverStandings;

    for (const driver of driverStandings) {
        const driverId = driver.Driver.driverId;
        const driverRef = db.ref(`${PATHS.drivers}/${driverId}`);
        const driverSnapshot = await driverRef.once("value");

        if (driverSnapshot.exists()) {
            const driverData = driverSnapshot.val();
            const driverTeams = driver.Constructors.map(c => TEAM_ID_NAME_MAP[c.constructorId] || c.name).join(' / ');
            const season_wins = (driverData.season_wins || "0").toString();
            const season_podiums = (driverData.season_podiums || "0").toString();

            const newHistoryEntry = {
                year: newSeason.toString(), position: driver.position, points: driver.points,
                team: driverTeams, wins: season_wins, podiums: season_podiums
            };

            let history = driverData.driver_history || [];
            history.push(newHistoryEntry);
            if (history.length > 10) history = history.slice(1);
            
            multiPathUpdates[`${PATHS.drivers}/${driverId}/driver_history`] = history;
            multiPathUpdates[`${PATHS.drivers}/${driverId}/season_wins`] = "0";
            multiPathUpdates[`${PATHS.drivers}/${driverId}/season_podiums`] = "0";

            if (driver.position === "1") {
                const currentChamps = parseInt(driverData.championships) || 0;
                multiPathUpdates[`${PATHS.drivers}/${driverId}/championships`] = (currentChamps + 1).toString();
            }
        }
    }

    const constructorStandingsRes = await axios.get(APIS.constructorStandings);
    const constructorStandings = constructorStandingsRes.data.MRData.StandingsTable.StandingsLists[0].ConstructorStandings;

    for (const constructor of constructorStandings) {
        const constructorId = constructor.Constructor.constructorId;
        const constructorRef = db.ref(`${PATHS.teams}/${constructorId}`);
        const constructorSnapshot = await constructorRef.once("value");

        if (constructorSnapshot.exists()) {
            const constructorData = constructorSnapshot.val();
            const season_wins = (constructorData.season_wins || "0").toString();
            const season_podiums = (constructorData.season_podiums || "0").toString();

            const newHistoryEntry = {
                year: newSeason.toString(), position: constructor.position, points: constructor.points,
                wins: season_wins, podiums: season_podiums
            };

            let history = constructorData.team_history || [];
            history.push(newHistoryEntry);
            if (history.length > 10) history = history.slice(1);
            
            multiPathUpdates[`${PATHS.teams}/${constructorId}/team_history`] = history;
            multiPathUpdates[`${PATHS.teams}/${constructorId}/season_wins`] = "0";
            multiPathUpdates[`${PATHS.teams}/${constructorId}/season_podiums`] = "0";

            if (constructor.position === "1") {
                const currentChamps = parseInt(constructorData.world_championships) || 0;
                multiPathUpdates[`${PATHS.teams}/${constructorId}/world_championships`] = (currentChamps + 1).toString();
            }
        }
    }

    multiPathUpdates[`${PATHS.tracker}/last_champ_season`] = newSeason;
    await db.ref().update(multiPathUpdates);
    console.log(`F1 Championship update complete.`);
}

// Esxport main functions
module.exports = { executeRaceStatsUpdate, executeChampionshipsUpdate };
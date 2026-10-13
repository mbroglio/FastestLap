const PATHS = {
    drivers: "drivers",
    teams: "teams",
    circuits: "circuits",
    calendar: "app_config/calendar",
    tracker: "app_config/stats_tracker",
    team_map: "app_config/team_id_name_map",
    driver_map: "app_config/driver_id_name_map",
    db_calendar_f1: "app_config/db_calendar/f1",
    nations: "nations",
    race_updates: "app_config/stats_tracker/race_updates"
};

const APIS = {
    raceResults: "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json",
    driverStandings: "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json",
    constructorStandings: "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json",
    qualifyingResults: "https://api.jolpi.ca/ergast/f1/current/qualifying/?format=json",
    qualifyingResultsPoles: "https://api.jolpi.ca/ergast/f1/current/qualifying/1/?format=json",
    results: "https://api.jolpi.ca/ergast/f1/current/results/?format=json",
    sprint: "https://api.jolpi.ca/ergast/f1/current/sprint/?format=json"
};

let DRIVER_ID_NAME_MAP = {};
let TEAM_ID_NAME_MAP = {};

async function loadMappings(db) {
    const [teamSnap, driverSnap] = await Promise.all([
        db.ref(PATHS.team_map).once("value"),
        db.ref(PATHS.driver_map).once("value")
    ]);

    TEAM_ID_NAME_MAP = teamSnap.val() || {};
    DRIVER_ID_NAME_MAP = driverSnap.val() || {};
}

function getTeamMap() {
    return TEAM_ID_NAME_MAP;
}

function getDriverMap() {
    return DRIVER_ID_NAME_MAP;
}

module.exports = {
    PATHS,
    APIS,
    loadMappings,
    getTeamMap,
    getDriverMap
};

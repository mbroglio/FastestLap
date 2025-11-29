// Import necessary modules
const { onSchedule } = require("firebase-functions/v2/scheduler");
const admin = require("firebase-admin");
const axios = require("axios");
const cheerio = require("cheerio");

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.database();
const currentYear = new Date().getFullYear();

// --- CONSTANTS ---
const PATHS = {
  DRIVERS: "drivers",
  TEAMS: "teams",
  CIRCUITS: "circuits",
  TRACKER: "app_config/stats_tracker",
  CALENDAR: "app_config/calendar",
  TEAM_MAP: "app_config/team_id_name_map",
  DRIVER_MAP: "app_config/driver_id_name_map",
  JUNIOR_ROOT_PATH: "junior_categories"
};

const APIS = {
  RACE_RESULTS: "https://api.jolpi.ca/ergast/f1/current/last/results/?format=json",
  DRIVER_STANDINGS: "https://api.jolpi.ca/ergast/f1/current/driverstandings/?format=json",
  CONSTRUCTOR_STANDINGS: "https://api.jolpi.ca/ergast/f1/current/constructorstandings/?format=json",
  F2_WIKI_URL: `https://en.wikipedia.org/wiki/${currentYear}_Formula_2_Championship`,
  F3_WIKI_URL: `https://en.wikipedia.org/wiki/${currentYear}_FIA_Formula_3_Championship`
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
 * JUNIOR SERIES HELPERS 
 * ===================================================================
 */

async function checkRaceYesterday(seriesId) {
    const calendarPath = `${PATHS.JUNIOR_ROOT_PATH}/${seriesId}/calendar`;
    const calendarRef = db.ref(calendarPath);
    const snapshot = await calendarRef.once("value");

    // If the calendar is empty (e.g., first absolute start), force the update to populate it
    if (!snapshot.exists()) {
        console.log(`[${seriesId}] Calendar is empty. Forcing first update.`);
        return true;
    }

    const calendarData = snapshot.val();
    
    // 1. Calculate YESTERDAY's date 
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);

    // 2. Format "Yesterday" as "D MMMM" string
    // Using 'en-GB' because Wikipedia uses English dates (Day Month)
    const formatter = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long' });
    const yesterdayString = formatter.format(yesterday);

    console.log(`[${seriesId}] Checking date: Looking for race held on '${yesterdayString}'...`);

    // 3. Search in calendar
    let raceFound = false;
    for (const key in calendarData) {
        const race = calendarData[key];
        // Compare clean string (e.g., "16 March")
        if (race.feature_date && race.feature_date.toLowerCase() === yesterdayString.toLowerCase()) {
            raceFound = true;
            console.log(`[${seriesId}] Match found: Round ${race.round} at ${race.circuit}`);
            break;
        }
    }

    return raceFound;
}

async function processSeries(seriesId, url) {
  console.log(`Scraping ${seriesId.toUpperCase()} from ${url}...`);
  
  const { data } = await axios.get(url, {
    headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
  });
  const $ = cheerio.load(data);
  
  const updates = {};
  const basePath = `${PATHS.JUNIOR_ROOT_PATH}/${seriesId}`;

  // 1. Entry List
  const entryList = scrapeEntryList($, url);
  if (entryList) updates[`${basePath}/entrylist`] = entryList;

  // 2. Calendar
  const calendar = scrapeCalendar($, url);
  if (calendar) {
    // Convert array to indexed map by round ("1": {...})
    calendar.forEach(race => {
        updates[`${basePath}/calendar/${race.round}`] = {
            round: race.round,
            circuit: race.circuit,
            sprint_date: race.sprint_date,
            feature_date: race.feature_date
        };
    });
  }

  // 3. Race Results (Sprint & Feature Orders)
  const raceResults = scrapeRaceResults($, url);
  if (raceResults) {
    raceResults.forEach(race => {
        // Map scraper JSON structure to DB structure
        updates[`${basePath}/results/${race.round}/sprint`] = {
            fastest_lap: race.sprint_race.fastest_lap,
            pole_position: race.sprint_race.pole_position,
            order: race.sprint_race.order
        };
        updates[`${basePath}/results/${race.round}/feature`] = {
            fastest_lap: race.feature_race.fastest_lap,
            pole_position: race.feature_race.pole_position,
            order: race.feature_race.order
        };
    });
  }

  // 4. Driver Standings
  const driverStandings = scrapeDriverStandings($, url);
  if (driverStandings) {
    driverStandings.forEach(d => {
        updates[`${basePath}/standings/drivers/${d.position}`] = d;
    });
  }

  // 5. Constructor Standings
  const teamStandings = scrapeTeamStandings($, url);
  if (teamStandings) {
    teamStandings.forEach(t => {
        updates[`${basePath}/standings/constructors/${t.position}`] = t;
    });
  }

  // Perform atomic update for this series
  if (Object.keys(updates).length > 0) {
    await db.ref().update(updates);
    console.log(`Database updated for ${seriesId}.`);
  }
}

// SCRAPERS FUNCTIONS

function scrapeEntryList($, url) {
    console.log("Scraping Entry List...");
    const rawList = []; 
    let currentTeam = null, currentCarNumber = null;
    let teamRowSpan = 0, numberRowSpan = 0;

    $('table.wikitable').each((i, table) => {
        const headers = $(table).find('th').text().toLowerCase();
        if (!headers.includes('team') || !headers.includes('driver') || !headers.includes('no.')) return;

        $(table).find('tr').each((rowIndex, row) => {
            const $row = $(row);
            if ($row.find('th').length > 0 && rowIndex === 0) return;
            const cells = $row.find('td');
            if (cells.length === 0) return;

            let cellIndex = 0;
            // Team 
            if (teamRowSpan === 0) {
                const teamCell = $(cells[cellIndex]);
                currentTeam = teamCell.text().trim();
                const spanAttr = teamCell.attr('rowspan');
                teamRowSpan = spanAttr ? parseInt(spanAttr) : 1;
                cellIndex++;
            }
            // Car Number
            if (numberRowSpan === 0) {
                const numberCell = $(cells[cellIndex]);
                currentCarNumber = numberCell.text().trim();
                const spanAttr = numberCell.attr('rowspan');
                numberRowSpan = spanAttr ? parseInt(spanAttr) : 1;
                cellIndex++;
            }
            // Driver
            const driverCell = $(cells[cellIndex]);
            const driverName = driverCell.text().trim();
            let rounds = "All";
            if (cells.length > cellIndex + 1) rounds = $(cells[cellIndex + 1]).text().trim();

            if (currentTeam && driverName) {
                rawList.push({
                    team: cleanText(currentTeam),
                    number: cleanText(currentCarNumber),
                    driver: cleanText(driverName),
                    rounds: cleanText(rounds)
                });
            }
            if (teamRowSpan > 0) teamRowSpan--;
            if (numberRowSpan > 0) numberRowSpan--;
        });
    });

    // Grouping and Sorting
    const teamsMap = {};
    rawList.forEach(entry => {
        if (!teamsMap[entry.team]) teamsMap[entry.team] = [];
        teamsMap[entry.team].push({ number: entry.number, driver: entry.driver, rounds: entry.rounds });
    });

    for (const teamName in teamsMap) {
        teamsMap[teamName].sort((a, b) => {
            const endA = getEndRound(a.rounds);
            const endB = getEndRound(b.rounds);
            if (endA !== endB) return endB - endA; // Descending (who finishes later wins)
            
            const durA = getRoundDuration(a.rounds);
            const durB = getRoundDuration(b.rounds);
            if (durA !== durB) return durB - durA;

            const numA = parseInt(a.number) || 999;
            const numB = parseInt(b.number) || 999;
            return numA - numB;
        });
    }
    return teamsMap;
}

function scrapeCalendar($, url) {
    console.log("Scraping Calendar...");
    const calendar = [];
    $('table.wikitable').each((i, table) => {
        const headers = $(table).find('th').text().toLowerCase();
        if (!headers.includes('round') || !headers.includes('circuit') || !headers.includes('feature race')) return;

        $(table).find('tr').each((rowIndex, row) => {
            const cells = $(row).find('td, th');
            if (cells.length < 4) return;
            
            let roundText = $(cells[0]).text().trim();
            if (isNaN(parseInt(roundText))) return;

            const circuitCell = $(cells[1]);
            let circuitName = "";
            if (circuitCell.find('a').length > 1) circuitName = circuitCell.find('a').eq(1).text().trim();
            else circuitName = circuitCell.text().replace(circuitCell.find('a').first().text(), '').replace(/,/g, '').trim();
            
            // Fix to get clean circuit name
            if(!circuitName) circuitName = circuitCell.find('a').first().text().trim();

            calendar.push({
                round: roundText,
                circuit: cleanText(circuitName),
                sprint_date: cleanText($(cells[2]).text()),
                feature_date: cleanText($(cells[3]).text())
            });
        });
    });
    return calendar;
}

function scrapeRaceResults($, url) {
    console.log("Scraping Results Matrix...");
    const races = [];
    $('table.wikitable').each((i, table) => {
        const headers = $(table).find('th').text().toLowerCase();
        if (!headers.includes('driver') || !headers.includes('points')) return;

        $(table).find('tr').each((rowIndex, row) => {
            const cells = $(row).find('td, th');
            if (cells.length === 0) return;

            let driverName = null;
            let resultsStartIndex = -1;
            for (let k = 0; k < cells.length; k++) {
                const cell = $(cells[k]);
                if (cell.find('.flagicon').length > 0) {
                    driverName = cell.text().trim();
                    resultsStartIndex = k + 1;
                    break;
                }
            }
            if (!driverName || /^[A-Z]{3}$/.test(driverName)) return;

            let raceCounter = 0;
            for (let j = resultsStartIndex; j < cells.length - 1; j++) {
                let rawText = $(cells[j]).text().trim();
                const roundNum = Math.floor(raceCounter / 2) + 1;
                const typeKey = (raceCounter % 2 !== 0) ? 'feature' : 'sprint';
                raceCounter++;

                if (!rawText) continue;
                if (!races[roundNum]) races[roundNum] = { round: roundNum, sprint: { fl: null, pl: null, results: [] }, feature: { fl: null, pl: null, results: [] } };

                // Cleaning and Logic
                rawText = rawText.replace(/\[.*?\]/g, '');
                let isFL = false, isPole = false;
                
                if (rawText.includes('F')) { isFL = true; rawText = rawText.replace('F', ''); }
                if (rawText.includes('P')) { isPole = true; rawText = rawText.replace('P', ''); }
                if (rawText.includes('†')) rawText = "Ret";
                rawText = rawText.trim();

                if (["SR", "FR", "C"].includes(rawText) && rawText.length < 3) continue;

                if (rawText) {
                    if (isFL) races[roundNum][typeKey].fl = driverName;
                    if (isPole) races[roundNum][typeKey].pl = driverName;

                    let sortVal = parseInt(rawText);
                    if (isNaN(sortVal)) {
                        const s = rawText.toUpperCase();
                        sortVal = (s === "RET") ? 1000 : (s === "NC") ? 1001 : (s === "DSQ") ? 1002 : 1003;
                    }

                    races[roundNum][typeKey].results.push({ driver: driverName, position: rawText, sortVal: sortVal });
                }
            }
        });
    });

    const finalResults = [];
    Object.keys(races).sort((a, b) => a - b).forEach(roundKey => {
        const d = races[roundKey];
        const sorter = (a, b) => a.sortVal - b.sortVal;
        d.sprint.results.sort(sorter);
        d.feature.results.sort(sorter);
        
        const clean = (list) => list.map(({ driver, position }) => ({ driver, position }));
        finalResults.push({
            round: parseInt(roundKey),
            sprint_race: { fastest_lap: d.sprint.fl || "N/A", pole_position: d.sprint.pl || "N/A", order: clean(d.sprint.results) },
            feature_race: { fastest_lap: d.feature.fl || "N/A", pole_position: d.feature.pl || "N/A", order: clean(d.feature.results) }
        });
    });
    return finalResults;
}

function scrapeDriverStandings($, url) {
    console.log("Scraping Driver Standings...");
    const standings = [];
    $('table.wikitable').each((i, table) => {
        const headers = $(table).find('th').text().toLowerCase();
        if (!headers.includes('driver') || !headers.includes('points') || !headers.includes('pos')) return;

        $(table).find('tr').each((rowIndex, row) => {
            const $row = $(row);
            let posText = $row.find('th').first().text().trim();
            if (isNaN(parseInt(posText))) return;

            let driverName = null;
            $row.find('td').each((idx, cell) => {
                if ($(cell).find('.flagicon').length > 0) driverName = $(cell).text().trim();
            });
            if (!driverName) return;

            let pointsText = $row.children().last().text().trim();
            standings.push({ position: posText, driver: cleanText(driverName), points: cleanText(pointsText) });
        });
    });
    return standings;
}

function scrapeTeamStandings($, url) {
    console.log("Scraping Team Standings...");
    const standings = [];
    $('table.wikitable').each((i, table) => {
        const headers = $(table).find('th').text().toLowerCase();
        if ((!headers.includes('team') && !headers.includes('entrant')) || !headers.includes('points')) return;
        if (headers.includes('driver')) return;

        $(table).find('tr').each((rowIndex, row) => {
            const $row = $(row);
            let posText = $row.find('th').first().text().trim();
            if (!posText || isNaN(parseInt(posText))) return;

            const firstTd = $row.find('td').first();
            let teamName = firstTd.text().trim();
            if (!teamName) return;

            let pointsText = $row.children().last().text().trim();
            standings.push({ position: posText, team: cleanText(teamName), points: cleanText(pointsText) });
        });
    });
    return standings;
}

// --- UTILS ---
function cleanText(text) {
    if (!text) return "";
    return text.replace(/\[.*?\]/g, '').trim();
}

function getRoundDuration(roundsText) {
    if (!roundsText) return 0;
    const clean = roundsText.trim();
    if (clean.toLowerCase().includes("all")) return 99;
    const rangeMatch = clean.match(/^(\d+)\s*[\–\-]\s*(\d+)$/);
    if (rangeMatch) return (parseInt(rangeMatch[2]) - parseInt(rangeMatch[1])) + 1;
    if (!isNaN(parseInt(clean))) return 1;
    return 0;
}

function getEndRound(roundsText) {
    if (!roundsText) return 0;
    const clean = roundsText.trim();
    if (clean.toLowerCase().includes("all")) return 999;
    const rangeMatch = clean.match(/^(\d+)\s*[\–\-]\s*(\d+)$/);
    if (rangeMatch) return parseInt(rangeMatch[2]);
    if (!isNaN(parseInt(clean))) return parseInt(clean);
    return 0;
}

/**
 * ===================================================================
 *  F1 EXPORTS
 * ===================================================================
 */

exports.updateRaceStats = onSchedule(
  {
    schedule: "every monday 20:00",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300, // Increased slightly
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
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
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
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

/**
 * ===================================================================
 * JUNIOR SERIES EXPORTS
 * ===================================================================
 */
exports.updateJuniorSeries = onSchedule(
  {
    schedule: "every monday 15:00",
    timeZone: "Europe/Rome",
    timeoutSeconds: 300, 
    retryConfig: {
      retryCount: 7,
      minBackoffDuration: "300s",
      maxBackoffDuration: "3600s"
    }
  },
  async (event) => {
    console.log("Junior Series pre-update check (F2 & F3)...");

    try {
      // --- F2 ---
      const shouldUpdateF2 = await checkRaceYesterday("f2");
      if (shouldUpdateF2) {
        console.log("F2: Race detected yesterday. Starting update...");
        await processSeries("f2", APIS.F2_WIKI_URL);
      } else {
        console.log("F2: No Feature Race detected yesterday. Skipping.");
      }
      
      // --- F3 ---
      const shouldUpdateF3 = await checkRaceYesterday("f3");
      if (shouldUpdateF3) {
        console.log("F3: Race detected yesterday. Starting update...");
        await processSeries("f3", APIS.F3_WIKI_URL);
      } else {
        console.log("F3: No Feature Race detected yesterday. Skipping.");
      }

      console.log("Junior Series procedure completed.");
    } catch (error) {
      console.error("Critical error during updateJuniorSeries:", error);
      throw error;
    }
  }
);
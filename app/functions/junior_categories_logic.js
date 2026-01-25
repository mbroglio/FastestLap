const axios = require("axios");
const cheerio = require("cheerio");

const currentYear = new Date().getFullYear();

const JUNIOR_ROOT_PATH = "junior_categories";

const F2_WIKI_URL = `https://en.wikipedia.org/wiki/${currentYear}_Formula_2_Championship`;
const F3_WIKI_URL = `https://en.wikipedia.org/wiki/${currentYear}_FIA_Formula_3_Championship`;




/*------------------------------------------------------------------------------
* EXPORTS 
* -------------------------------------------------------------------------------
*/

// Main Update Function
async function executeJuniorSeriesUpdate(db) {
    console.log("Starting Junior Series Update...");

    // F2
    if (await checkRaceYesterday(db, "f2")) await processSeries(db, "f2", F2_WIKI_URL);
    else{
        console.log("F2: No race yesterday.");
        const f2EntryListRef = db.ref(`${JUNIOR_ROOT_PATH}/f2/entrylist`);
        const snapshot = await f2EntryListRef.once("value");
        if (!snapshot.exists()) {
            console.log("F2: Entry list missing");
            await forceEntryListScrape(db, "f2", F2_WIKI_URL);
        }
    } 

    // F3
    if (await checkRaceYesterday(db, "f3")) await processSeries(db, "f3", F3_WIKI_URL);
    else{
        console.log("F3: No race yesterday.");
        const f3EntryListRef = db.ref(`${JUNIOR_ROOT_PATH}/f3/entrylist`);
        const snapshot = await f3EntryListRef.once("value");
        if (!snapshot.exists()) {
            console.log("F3: Entry list missing");
            await forceEntryListScrape(db, "f3", F3_WIKI_URL);
        }
    } 
}

// Annual Reset Function
async function executeJuniorReset(db) {
    console.log("Resetting Junior Series Database...");
    const updates = {};
    updates[`${JUNIOR_ROOT_PATH}/f2`] = null;
    updates[`${JUNIOR_ROOT_PATH}/f3`] = null;
    await db.ref().update(updates);
    console.log("Reset complete.");
}

// Force Entry List Scrape
async function forceEntryListScrape(db, seriesId, url) {  
    const { data } = await axios.get(url, {
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
    });
    const $ = cheerio.load(data);
    const entryList = await scrapeEntryList($, db, seriesId);

    const updates = {};
    if (entryList) updates[`${JUNIOR_ROOT_PATH}/${seriesId}/entrylist`] = entryList;

    if (Object.keys(updates).length > 0) {
        await db.ref().update(updates);
        console.log(`DB updated for ${seriesId} entry list.`);
    }
}




/*------------------------------------------------------------------------------
* PRE-CHECK LOGIC
* ------------------------------------------------------------------------------
*/
async function checkRaceYesterday(db, seriesId) {
    const calendarPath = `${JUNIOR_ROOT_PATH}/${seriesId}/calendar`;
    const calendarRef = db.ref(calendarPath);
    const snapshot = await calendarRef.once("value");

    // If calendar is empty, force update
    if (!snapshot.exists()) {
        console.log(`[${seriesId}] Calendar empty or not found. Forcing update.`);
        return true;
    }

    const calendarData = snapshot.val();

    // 1. Calculate YESTERDAY's date
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);

    // 2. Format "Yesterday" as "D MMMM" string (e.g., "16 March")
    // en-GB' to have day before month
    const formatter = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'long' });
    const yesterdayString = formatter.format(yesterday);

    console.log(`[${seriesId}] Date check: Looking for race held on '${yesterdayString}'...`);

    // 3. Search in calendar
    let raceFound = false;
    for (const key in calendarData) {
        const race = calendarData[key];

        // Compare string (e.g., "16 March")
        if (race.feature_date && race.feature_date.toLowerCase() === yesterdayString.toLowerCase()) {
            console.log(`[${seriesId}] Match found: Round ${race.round} at ${race.circuit} on Feature Date`);
            raceFound = true;
            break;
        }

        if (race.sprint_date && race.sprint_date.toLowerCase() === yesterdayString.toLowerCase()) {
            console.log(`[${seriesId}] Match found: Round ${race.round} at ${race.circuit} on Sprint Date`);
            raceFound = true;
            break;
        }
    }

    return raceFound;
}




/* * ------------------------------------------------------------------------------
*   SERIES PROCESSING LOGIC
* ------------------------------------------------------------------------------
*/

async function processSeries(db, seriesId, url) {
    console.log(`Processing ${seriesId} from ${url}...`);
    const { data } = await axios.get(url, {
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
    });
    const $ = cheerio.load(data);
    const updates = {};
    const basePath = `${JUNIOR_ROOT_PATH}/${seriesId}`;

    const entryList = await scrapeEntryList($, db, seriesId);
    if (entryList) updates[`${basePath}/entrylist`] = entryList;

    const calendar = await scrapeCalendar($, db);
    if (calendar) {
        calendar.forEach(race => {
            const calendarEntry = {
                round: race.round,
                circuit: race.circuit,
                sprint_date: race.sprint_date,
                feature_date: race.feature_date
            };
            // Add nationFlagUrl if available
            if (race.nation_flag_url) {
                calendarEntry.nation_flag_url = race.nation_flag_url;
            }
            updates[`${basePath}/calendar/${race.round}`] = calendarEntry;
        });
    }

    const raceResults = scrapeRaceResults($, calendar);
    if (raceResults) {
        raceResults.forEach(race => {
            const raceResultsEntry = {
                round: race.round,
                circuit: race.circuit,
                sprint: race.sprint_race,
                feature: race.feature_race,
                nationFlagUrl: race.nationFlagUrl
            }
            updates[`${basePath}/results/${race.round}`] = raceResultsEntry;

            console.log(`Race results for Round ${race.round} processed.`);
        });
    }

    const driverStandings = scrapeDriverStandings($);
    if (driverStandings) {
        driverStandings.forEach(d => updates[`${basePath}/standings/drivers/${d.position}`] = d);
    }

    const teamStandings = scrapeTeamStandings($);
    if (teamStandings) {
        teamStandings.forEach(t => updates[`${basePath}/standings/constructors/${t.position}`] = t);
    }

    if (Object.keys(updates).length > 0) {
        await db.ref().update(updates);
        console.log(`DB updated for ${seriesId}.`);
    }
}





/*
* ------------------------------------------------------------------------------
*   SCRAPING FUNCTIONS
* ------------------------------------------------------------------------------
*/

// Scrapes the entry list
async function scrapeEntryList($, db, seriesId) {
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
    const dbEntryListSnap = await db.ref(`${JUNIOR_ROOT_PATH}/${seriesId}/calendar`).once("value");
    const dbEntryListData = dbEntryListSnap.val() || {};

    rawList.forEach(entry => {
        const teamName = entry.team;

        for (key in dbEntryListData) {
            if (key === teamName) {
                console.log("team already present");
                teamsMap[teamName] = {
                    team_logo: dbEntryListData.getValue(key).team_logo_url
                };
                return;
            } else {
                teamsMap[teamName] = {
                    // Generate the logo URL based on the team name
                    team_logo: {},
                    drivers: []
                };
            }
        }

        teamsMap[teamName].drivers.push({
            number: entry.number,
            driver: entry.driver,
            rounds: entry.rounds
        });

    });

    for (const teamName in teamsMap) {
        teamsMap[teamName].drivers.sort((a, b) => {
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

// Scrapes the calendar and enriches with circuit and nation data
async function scrapeCalendar($, db) {
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
            if (!circuitName) circuitName = circuitCell.find('a').first().text().trim();

            calendar.push({
                round: roundText,
                circuit: cleanText(circuitName),
                sprint_date: cleanText($(cells[2]).text()),
                feature_date: cleanText($(cells[3]).text())
            });
        });
    });

    // Enrich calendar data with circuit details and nation flags
    if (db && calendar.length > 0) {
        console.log("Enriching calendar data with circuit and nation information...");

        // Get circuit_name_id_map
        const circuitMapSnapshot = await db.ref('app_config/circuit_name_id_map').once('value');
        const circuitMap = circuitMapSnapshot.val() || {};

        for (const event of calendar) {
            try {
                // 1. Get circuit ID from circuit_name_id_map
                const circuitId = circuitMap[event.circuit];

                if (circuitId) {
                    console.log(`Found circuit ID '${circuitId}' for '${event.circuit}'`);

                    // 2. Fetch track data from circuits node
                    const trackSnapshot = await db.ref(`circuits/${circuitId}`).once('value');
                    const track = trackSnapshot.val();

                    if (track) {
                        // 3. Overwrite circuit with track.trackName
                        event.circuit = track.trackName;
                        console.log(`Updated circuit name to '${track.trackName}'`);

                        // 4. Fetch nation using track.country
                        if (track.country) {
                            const nationSnapshot = await db.ref(`nations/${track.country}`).once('value');
                            const nation = nationSnapshot.val();

                            if (nation && nation.nation_flag_url) {
                                // 5. Set nationFlagUrl
                                event.nation_flag_url = nation.nation_flag_url;
                                console.log(`Added nation flag URL for country '${track.country}'`);
                            } else {
                                console.warn(`Nation not found or missing flag URL for country '${track.country}'`);
                            }
                        } else {
                            console.warn(`Track '${circuitId}' has no country field`);
                        }
                    } else {
                        console.warn(`No track found for circuit ID '${circuitId}'`);
                    }
                } else {
                    console.warn(`No circuit ID mapping found for '${event.circuit}'`);
                }
            } catch (error) {
                console.error(`Error enriching calendar event for round ${event.round}:`, error.message);
            }
        }
    }

    return calendar;
}

// scrapes the race results
function scrapeRaceResults($, calendar = null) {
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
        const result = {
            round: parseInt(roundKey),
            sprint_race: { fastest_lap: d.sprint.fl || "N/A", pole_position: d.sprint.pl || "N/A", order: clean(d.sprint.results) },
            feature_race: { fastest_lap: d.feature.fl || "N/A", pole_position: d.feature.pl || "N/A", order: clean(d.feature.results) }
        };

        // Enrich with calendar data (circuit name and nation flag URL)
        if (calendar && calendar.length > 0) {
            try {
                const calendarItem = calendar.find(event => parseInt(event.round) === result.round);
                if (calendarItem) {
                    result.circuit = calendarItem.circuit || null;
                    result.nationFlagUrl = calendarItem.nation_flag_url || null;

                    console.log(`Enriched round ${result.round} with circuit '${result.circuit}'`);
                } else {
                    console.warn(`No calendar event found for round ${result.round}`);
                }
            } catch (error) {
                console.error(`Error enriching race results for round ${result.round}:`, error.message);
            }
        }

        finalResults.push(result);
    });
    return finalResults;
}

// scrapes the driver standings
function scrapeDriverStandings($) {
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

// scrapes the team standings
function scrapeTeamStandings($) {
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




/*
* ---------------------------------------------------------------------------
*   HELPER FUNCTIONS
* ---------------------------------------------------------------------------
*/
function cleanText(text) {
    if (!text) return "";
    return text.replace(/\[.*?\]/g, '').trim();
}

// Parses rounds text to determine the duration in rounds
function getRoundDuration(roundsText) {
    if (!roundsText) return 0;
    const clean = roundsText.trim();
    if (clean.toLowerCase().includes("all")) return 99;

    // Handle composed intervals (e.g., "7, 9–10" or "1–3, 5, 7–8")
    if (clean.includes(',')) {
        const parts = clean.split(',').map(p => p.trim());
        let totalRounds = 0;
        for (const part of parts) {
            const rangeMatch = part.match(/^(\d+)\s*[\–\-]\s*(\d+)$/);
            if (rangeMatch) {
                totalRounds += (parseInt(rangeMatch[2]) - parseInt(rangeMatch[1])) + 1;
            } else if (!isNaN(parseInt(part))) {
                totalRounds += 1;
            }
        }
        return totalRounds;
    }

    const rangeMatch = clean.match(/^(\d+)\s*[\–\-]\s*(\d+)$/);
    if (rangeMatch) return (parseInt(rangeMatch[2]) - parseInt(rangeMatch[1])) + 1;
    if (!isNaN(parseInt(clean))) return 1;
    return 0;
}

// Parses rounds text to determine the ending round number
function getEndRound(roundsText) {
    if (!roundsText) return 0;
    const clean = roundsText.trim();
    if (clean.toLowerCase().includes("all")) return 999;

    // Handle composed intervals (e.g., "7, 9–10" or "1–3, 5, 7–8")
    if (clean.includes(',')) {
        const parts = clean.split(',').map(p => p.trim());
        let maxRound = 0;
        for (const part of parts) {
            const rangeMatch = part.match(/^(\d+)\s*[\–\-]\s*(\d+)$/);
            if (rangeMatch) {
                maxRound = Math.max(maxRound, parseInt(rangeMatch[2]));
            } else if (!isNaN(parseInt(part))) {
                maxRound = Math.max(maxRound, parseInt(part));
            }
        }
        return maxRound;
    }

    const rangeMatch = clean.match(/^(\d+)\s*[\–\-]\s*(\d+)$/);
    if (rangeMatch) return parseInt(rangeMatch[2]);
    if (!isNaN(parseInt(clean))) return parseInt(clean);
    return 0;
}




/*
* --------------------------------------------------------------------
*   FUNCTION EXPORTS
* --------------------------------------------------------------------
*/
module.exports = {
    executeJuniorSeriesUpdate,
    executeJuniorReset,

    // Individual functions for testing
    checkRaceYesterday,
    processSeries,
    scrapeEntryList,
    scrapeCalendar,
    scrapeRaceResults,
    scrapeDriverStandings,
    scrapeTeamStandings
};
const axios = require("axios");
const cheerio = require("cheerio");

const currentYear = new Date().getFullYear();

const JUNIOR_ROOT_PATH = "junior_categories";

const F2_WIKI_URL = `https://en.wikipedia.org/wiki/${currentYear}_Formula_2_Championship`;
const F3_WIKI_URL = `https://en.wikipedia.org/wiki/${currentYear}_FIA_Formula_3_Championship`;

// Main Update Function
async function executeJuniorSeriesUpdate(db) {
    console.log("Starting Junior Series Update...");
    
    // F2
    if (await checkRaceYesterday(db, "f2")) await processSeries(db, "f2", F2_WIKI_URL);
    else console.log("F2: No race yesterday.");

    // F3
    if (await checkRaceYesterday(db, "f3")) await processSeries(db, "f3", F3_WIKI_URL);
    else console.log("F3: No race yesterday.");
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

// --- PRE-CHECK LOGIC ---
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
            console.log(`[${seriesId}] Match found: Round ${race.round} at ${race.circuit}`);
            raceFound = true;
            break;
        }
    }

    return raceFound;
}

async function processSeries(db, seriesId, url) {
    console.log(`Processing ${seriesId} from ${url}...`);
    const { data } = await axios.get(url, {
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
    });
    const $ = cheerio.load(data);
    const updates = {};
    const basePath = `${JUNIOR_ROOT_PATH}/${seriesId}`;

    const entryList = scrapeEntryList($);
    if (entryList) updates[`${basePath}/entrylist`] = entryList;

    const calendar = scrapeCalendar($);
    if (calendar) {
        calendar.forEach(race => {
            updates[`${basePath}/calendar/${race.round}`] = {
                round: race.round, circuit: race.circuit, sprint_date: race.sprint_date, feature_date: race.feature_date
            };
        });
    }

    const raceResults = scrapeRaceResults($);
    if (raceResults) {
        raceResults.forEach(race => {
            updates[`${basePath}/results/${race.round}/sprint`] = race.sprint_race;
            updates[`${basePath}/results/${race.round}/feature`] = race.feature_race;
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

// --- CORE SCRAPING LOGIC ---
function scrapeEntryList($) {
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
        const teamName = entry.team;

        // If the team does not exist yet, initialize the new structure
        if (!teamsMap[teamName]) {
            teamsMap[teamName] = {
                // Generate the logo URL based on the team name
                team_logo: {}, 
                drivers: []
            };
        }

        // Add the driver to the 'drivers' array
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

function scrapeCalendar($) {
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

function scrapeRaceResults($) {
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

// Export main functions
module.exports = { executeJuniorSeriesUpdate, executeJuniorReset };
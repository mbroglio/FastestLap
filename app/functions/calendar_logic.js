const axios = require("axios");
const cheerio = require("cheerio");

const JOLPICA_BASE_URL = "https://api.jolpi.ca/ergast/f1";
const F2_BASE_URL = "https://www.fiaformula2.com";
const F3_BASE_URL = "https://www.fiaformula3.com";

const DB_PATHS = {
    calendar: "app_config/db_calendar",
    legacy_circuits_calendar: "app_config/calendar",
    sent_sessions: "app_config/notifications/sent_sessions"
};

/**
 * Historical circuit season results required by bio history tables and f1_logic.js
 */
const LEGACY_CALENDAR_BACKUP = {
  // 2026 Completed Races (This Year: Rounds 1 to 14)
  "albert_park": { "round": 1, "season_result": { "podium": ["G. Russell", "A. K. Antonelli", "C. Leclerc"], "team": ["Mercedes", "Mercedes", "Ferrari"], "year": "2026" } },
  "shanghai": { "round": 2, "season_result": { "podium": ["A. K. Antonelli", "G. Russell", "L. Hamilton"], "team": ["Mercedes", "Mercedes", "Ferrari"], "year": "2026" } },
  "suzuka": { "round": 3, "season_result": { "podium": ["A. K. Antonelli", "O. Piastri", "C. Leclerc"], "team": ["Mercedes", "McLaren", "Ferrari"], "year": "2026" } },
  "miami": { "round": 4, "season_result": { "podium": ["A. K. Antonelli", "L. Norris", "O. Piastri"], "team": ["Mercedes", "McLaren", "McLaren"], "year": "2026" } },
  "villeneuve": { "round": 5, "season_result": { "podium": ["A. K. Antonelli", "L. Hamilton", "M. Verstappen"], "team": ["Mercedes", "Ferrari", "Red Bull"], "year": "2026" } },
  "monaco": { "round": 6, "season_result": { "podium": ["A. K. Antonelli", "L. Hamilton", "I. Hadjar"], "team": ["Mercedes", "Ferrari", "Red Bull"], "year": "2026" } },
  "catalunya": { "round": 7, "season_result": { "podium": ["L. Hamilton", "G. Russell", "L. Norris"], "team": ["Ferrari", "Mercedes", "McLaren"], "year": "2026" } },
  "red_bull_ring": { "round": 8, "season_result": { "podium": ["G. Russell", "M. Verstappen", "A. K. Antonelli"], "team": ["Mercedes", "Red Bull", "Mercedes"], "year": "2026" } },
  "silverstone": { "round": 9, "season_result": { "podium": ["C. Leclerc", "G. Russell", "L. Hamilton"], "team": ["Ferrari", "Mercedes", "Ferrari"], "year": "2026" } },
  "spa": { "round": 10, "season_result": { "podium": ["A. K. Antonelli", "C. Leclerc", "M. Verstappen"], "team": ["Mercedes", "Ferrari", "Red Bull"], "year": "2026" } },
  "hungaroring": { "round": 11, "season_result": { "podium": ["L. Norris", "M. Verstappen", "A. K. Antonelli"], "team": ["McLaren", "Red Bull", "Mercedes"], "year": "2026" } },
  "zandvoort": { "round": 12, "season_result": { "podium": ["L. Norris", "A. K. Antonelli", "G. Russell"], "team": ["McLaren", "Mercedes", "Mercedes"], "year": "2026" } },
  "monza": { "round": 13, "season_result": { "podium": ["A. K. Antonelli", "G. Russell", "M. Verstappen"], "team": ["Mercedes", "Mercedes", "Red Bull"], "year": "2026" } },
  "madring": { "round": 14, "season_result": { "podium": ["A. K. Antonelli", "M. Verstappen", "L. Norris"], "team": ["Mercedes", "Red Bull", "McLaren"], "year": "2026" } },

  // Upcoming 2026 Races (Preserving baseline history entry)
  "baku": { "round": 15, "season_result": { "podium": ["M. Verstappen", "G. Russell", "C. Sainz"], "team": ["Red Bull", "Mercedes", "Williams"], "year": "2025" } },
  "americas": { "round": 18, "season_result": { "podium": ["M. Verstappen", "L. Norris", "C. Leclerc"], "team": ["Red Bull", "McLaren", "Ferrari"], "year": "2025" } },
  "rodriguez": { "round": 19, "season_result": { "podium": ["L. Norris", "C. Leclerc", "M. Verstappen"], "team": ["McLaren", "Ferrari", "Red Bull"], "year": "2025" } },
  "interlagos": { "round": 20, "season_result": { "podium": ["L. Norris", "A. K. Antonelli", "M. Verstappen"], "team": ["McLaren", "Mercedes", "Red Bull"], "year": "2025" } },
  "vegas": { "round": 21, "season_result": { "podium": ["M. Verstappen", "G. Russell", "A. K. Antonelli"], "team": ["Red Bull", "Mercedes", "Mercedes"], "year": "2025" } },
  "losail": { "round": 22, "season_result": { "podium": ["M. Verstappen", "O. Piastri", "C. Sainz"], "team": ["Red Bull", "McLaren", "Williams"], "year": "2025" } },
  "yas_marina": { "round": 23, "season_result": { "podium": ["M. Verstappen", "O. Piastri", "L. Norris"], "team": ["Red Bull", "McLaren", "McLaren"], "year": "2025" } },
  "bahrain": { "round": 4, "season_result": { "podium": ["O. Piastri", "G. Russell", "L. Norris"], "team": ["McLaren", "Mercedes", "McLaren"], "year": "2025" } },
  "jeddah": { "round": 5, "season_result": { "podium": ["O. Piastri", "M. Verstappen", "C. Leclerc"], "team": ["McLaren", "Red Bull", "Ferrari"], "year": "2025" } },
  "imola": { "round": 7, "season_result": { "podium": ["M. Verstappen", "L. Norris", "O. Piastri"], "team": ["Red Bull", "McLaren", "McLaren"], "year": "2025" } },
  "singapore": { "round": 17, "season_result": { "podium": ["G. Russell", "M. Verstappen", "L. Norris"], "team": ["Mercedes", "Red Bull", "McLaren"], "year": "2025" } }
};

/**
 * Restores app_config/calendar with the 2026 race results for all races that took place this year.
 */
async function restoreLegacyCalendar(db) {
    console.log("Restoring app_config/calendar with 2026 race results for this year's completed races...");
    await db.ref(DB_PATHS.legacy_circuits_calendar).set(LEGACY_CALENDAR_BACKUP);
    console.log(`✅ Successfully restored app_config/calendar with 2026 race results (${Object.keys(LEGACY_CALENDAR_BACKUP).length} circuits).`);
    return { success: true, count: Object.keys(LEGACY_CALENDAR_BACKUP).length };
}

/**
 * Calculates standard and local timezone representations for a given session.
 */
function computeSessionTimes(dateStr, timeStr, gmtOffset, trackTimeZone) {
    if (!dateStr && !timeStr) return null;
    let iso = "";
    if (dateStr && timeStr) {
        const timeClean = timeStr.endsWith("Z") ? timeStr : (gmtOffset ? `${timeStr}${gmtOffset}` : `${timeStr}Z`);
        iso = `${dateStr}T${timeClean}`;
    } else if (timeStr && timeStr.includes("T")) {
        iso = gmtOffset ? `${timeStr}${gmtOffset}` : `${timeStr}Z`;
    }
    const d = new Date(iso);
    const startTimeMillis = d.getTime();
    if (isNaN(startTimeMillis)) return null;

    const timeRome = d.toLocaleTimeString("it-IT", {
        hour: "2-digit",
        minute: "2-digit",
        timeZone: "Europe/Rome"
    });
    const dateRome = d.toLocaleDateString("sv-SE", { timeZone: "Europe/Rome" });

    const timeUtc = d.toLocaleTimeString("it-IT", {
        hour: "2-digit",
        minute: "2-digit",
        timeZone: "UTC"
    });
    const dateUtc = d.toLocaleDateString("sv-SE", { timeZone: "UTC" });

    let timeTrack = "";
    if (timeStr && timeStr.includes("T")) {
        const parts = timeStr.split("T");
        if (parts[1]) timeTrack = parts[1].slice(0, 5);
    } else if (trackTimeZone) {
        try {
            timeTrack = d.toLocaleTimeString("it-IT", {
                hour: "2-digit",
                minute: "2-digit",
                timeZone: trackTimeZone
            });
        } catch (ignored) {}
    }

    return {
        startTimeMillis,
        iso: d.toISOString(),
        time: timeRome,
        date: dateRome,
        timeZone: "Europe/Rome",
        timeRome,
        dateRome,
        timeUtc,
        dateUtc,
        timeTrack: timeTrack || timeRome,
        trackTimeZone: trackTimeZone || ""
    };
}

/**
 * Normalizes an F1 race object from Jolpica into structured sessions with UTC timestamps.
 */
function normalizeF1Race(race) {
    const round = parseInt(race.round, 10);
    const raceName = race.raceName || `Round ${round}`;
    const sessions = [];

    function addSession(id, name, shortName, type, topic, date, time) {
        if (!date) return;
        const timing = computeSessionTimes(date, time, null, null);
        if (timing) {
            sessions.push({
                id,
                name,
                shortName,
                category: "f1",
                type,
                topic,
                ...timing
            });
        }
    }

    if (race.FirstPractice) {
        addSession("fp1", "Prove Libere 1 (FP1)", "FP1", "practice", "session_f1_practice", race.FirstPractice.date, race.FirstPractice.time);
    }
    if (race.SecondPractice) {
        addSession("fp2", "Prove Libere 2 (FP2)", "FP2", "practice", "session_f1_practice", race.SecondPractice.date, race.SecondPractice.time);
    }
    if (race.ThirdPractice) {
        addSession("fp3", "Prove Libere 3 (FP3)", "FP3", "practice", "session_f1_practice", race.ThirdPractice.date, race.ThirdPractice.time);
    }
    if (race.SprintQualifying) {
        addSession("sprint_qualifying", "Sprint Qualifying", "Sprint Qualifiche", "qualifying", "session_f1_qualifying", race.SprintQualifying.date, race.SprintQualifying.time);
    }
    if (race.Sprint) {
        addSession("sprint", "Gara Sprint", "Sprint", "sprint", "session_f1_sprint", race.Sprint.date, race.Sprint.time);
    }
    if (race.Qualifying) {
        addSession("qualifying", "Qualifiche", "Qualifiche", "qualifying", "session_f1_qualifying", race.Qualifying.date, race.Qualifying.time);
    }
    if (race.date) {
        addSession("race", "Gara", "Gara", "race", "session_f1_race", race.date, race.time);
    }

    // Sort sessions chronologically
    sessions.sort((a, b) => a.startTimeMillis - b.startTimeMillis);

    const raceTiming = computeSessionTimes(race.date, race.time, null, null);

    return {
        season: race.season,
        round: round,
        raceName: raceName,
        circuit: {
            id: race.Circuit?.circuitId || "",
            name: race.Circuit?.circuitName || "",
            locality: race.Circuit?.Location?.locality || "",
            country: race.Circuit?.Location?.country || "",
            lat: race.Circuit?.Location?.lat || "",
            long: race.Circuit?.Location?.long || ""
        },
        date: raceTiming?.date || race.date,
        time: raceTiming?.time || race.time || "",
        timeZone: "Europe/Rome",
        timeUtc: raceTiming?.timeUtc || race.time || "",
        sessions: sessions
    };
}

/**
 * Downloads and stores current season F1 calendar from Jolpica API to Firebase RTDB under app_config/db_calendar/f1/{year}.
 */
async function fetchAndStoreF1Calendar(db, year = new Date().getFullYear()) {
    console.log(`[F1 Calendar] Fetching season ${year} from Jolpica API into ${DB_PATHS.calendar}/f1/${year}...`);
    const url = `${JOLPICA_BASE_URL}/${year}/races/`;
    const response = await axios.get(url, { timeout: 15000 });
    const rawRaces = response.data?.MRData?.RaceTable?.Races;

    if (!rawRaces || !Array.isArray(rawRaces)) {
        throw new Error(`Invalid response from Jolpica API: ${JSON.stringify(response.data)}`);
    }

    const normalizedRaces = {};
    for (const r of rawRaces) {
        const norm = normalizeF1Race(r);
        normalizedRaces[norm.round] = norm;
    }

    const calendarRef = db.ref(`${DB_PATHS.calendar}/f1/${year}`);
    await calendarRef.set(normalizedRaces);
    await db.ref(`${DB_PATHS.calendar}/f1/last_sync`).set({
        timestamp: Date.now(),
        iso: new Date().toISOString(),
        totalRaces: rawRaces.length,
        year: year
    });

    console.log(`✅ [F1 Calendar] Saved ${rawRaces.length} races for season ${year} to RTDB under ${DB_PATHS.calendar}/f1/${year}.`);
    return { success: true, totalRaces: rawRaces.length };
}

/**
 * Scrapes and stores F2 calendar from fiaformula2.com to Firebase RTDB under app_config/db_calendar/f2/{year}.
 */
async function scrapeAndStoreF2Calendar(db, year = new Date().getFullYear()) {
    console.log(`[F2 Calendar] Scraping season ${year} from ${F2_BASE_URL} into ${DB_PATHS.calendar}/f2/${year}...`);
    const listUrl = `${F2_BASE_URL}/en/racing/${year}`;
    const res = await axios.get(listUrl, {
        headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" },
        timeout: 15000
    });
    const $ = cheerio.load(res.data);
    const roundLinks = new Set();

    $('a[href*="/racing/' + year + '/"]').each((i, el) => {
        const href = $(el).attr("href");
        if (href && !href.endsWith("/" + year) && !href.endsWith("/" + year + "/")) {
            roundLinks.add(href.startsWith("http") ? href : `${F2_BASE_URL}${href}`);
        }
    });

    const races = {};
    let roundNum = 1;

    for (const roundUrl of Array.from(roundLinks)) {
        try {
            const pageRes = await axios.get(roundUrl, {
                headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" },
                timeout: 10000
            });
            const matches = pageRes.data.match(/\{[^{}]*(?:Practice|Qualifying|Sprint Race|Feature Race)[^{}]*\}/g);
            const slug = roundUrl.split("/").filter(Boolean).pop();
            const raceName = slug.charAt(0).toUpperCase() + slug.slice(1);

            const sessions = [];
            if (matches) {
                for (const m of matches) {
                    try {
                        const clean = m.replace(/\\"/g, '"');
                        const s = JSON.parse(clean);
                        if (s.startTime && (s.shortName || s.session)) {
                            const rawName = s.shortName || s.session;
                            const timing = computeSessionTimes(null, s.startTime, s.gmtOffset, s.timezone);
                            if (timing) {
                                let type = "practice";
                                let topic = "session_f2_practice";
                                const lowerName = rawName.toLowerCase();

                                if (lowerName.includes("feature")) {
                                    type = "feature";
                                    topic = "session_f2_feature";
                                } else if (lowerName.includes("sprint")) {
                                    type = "sprint";
                                    topic = "session_f2_sprint";
                                } else if (lowerName.includes("qualifying")) {
                                    type = "qualifying";
                                    topic = "session_f2_qualifying";
                                }

                                sessions.push({
                                    id: `f2_${lowerName.replace(/[^a-z0-9]/g, "_")}`,
                                    name: `F2 - ${rawName}`,
                                    shortName: rawName,
                                    category: "f2",
                                    type,
                                    topic,
                                    ...timing
                                });
                            }
                        }
                    } catch (ignored) {}
                }
            }

            sessions.sort((a, b) => a.startTimeMillis - b.startTimeMillis);

            races[roundNum] = {
                round: roundNum,
                raceName: raceName,
                slug: slug,
                url: roundUrl,
                sessions: sessions
            };
            roundNum++;
        } catch (err) {
            console.warn(`[F2 Calendar] Failed to scrape round ${roundUrl}: ${err.message}`);
        }
    }

    const calendarRef = db.ref(`${DB_PATHS.calendar}/f2/${year}`);
    await calendarRef.set(races);
    await db.ref(`${DB_PATHS.calendar}/f2/last_sync`).set({
        timestamp: Date.now(),
        iso: new Date().toISOString(),
        totalRaces: Object.keys(races).length,
        year: year
    });

    console.log(`✅ [F2 Calendar] Saved ${Object.keys(races).length} rounds for season ${year} to RTDB under ${DB_PATHS.calendar}/f2/${year}.`);
    return { success: true, totalRaces: Object.keys(races).length };
}

/**
 * Scrapes and stores F3 calendar from fiaformula3.com to Firebase RTDB under app_config/db_calendar/f3/{year}.
 */
async function scrapeAndStoreF3Calendar(db, year = new Date().getFullYear()) {
    console.log(`[F3 Calendar] Scraping season ${year} from ${F3_BASE_URL} into ${DB_PATHS.calendar}/f3/${year}...`);
    const listUrl = `${F3_BASE_URL}/en/racing/${year}`;
    const res = await axios.get(listUrl, {
        headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" },
        timeout: 15000
    });
    const $ = cheerio.load(res.data);
    const roundLinks = new Set();

    $('a[href*="/racing/' + year + '/"]').each((i, el) => {
        const href = $(el).attr("href");
        if (href && !href.endsWith("/" + year) && !href.endsWith("/" + year + "/")) {
            roundLinks.add(href.startsWith("http") ? href : `${F3_BASE_URL}${href}`);
        }
    });

    const races = {};
    let roundNum = 1;

    for (const roundUrl of Array.from(roundLinks)) {
        try {
            const pageRes = await axios.get(roundUrl, {
                headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" },
                timeout: 10000
            });
            const matches = pageRes.data.match(/\{[^{}]*(?:Practice|Qualifying|Sprint Race|Feature Race)[^{}]*\}/g);
            const slug = roundUrl.split("/").filter(Boolean).pop();
            const raceName = slug.charAt(0).toUpperCase() + slug.slice(1);

            const sessions = [];
            if (matches) {
                for (const m of matches) {
                    try {
                        const clean = m.replace(/\\"/g, '"');
                        const s = JSON.parse(clean);
                        if (s.startTime && (s.shortName || s.session)) {
                            const rawName = s.shortName || s.session;
                            const timing = computeSessionTimes(null, s.startTime, s.gmtOffset, s.timezone);
                            if (timing) {
                                let type = "practice";
                                let topic = "session_f3_practice";
                                const lowerName = rawName.toLowerCase();

                                if (lowerName.includes("feature")) {
                                    type = "feature";
                                    topic = "session_f3_feature";
                                } else if (lowerName.includes("sprint")) {
                                    type = "sprint";
                                    topic = "session_f3_sprint";
                                } else if (lowerName.includes("qualifying")) {
                                    type = "qualifying";
                                    topic = "session_f3_qualifying";
                                }

                                sessions.push({
                                    id: `f3_${lowerName.replace(/[^a-z0-9]/g, "_")}`,
                                    name: `F3 - ${rawName}`,
                                    shortName: rawName,
                                    category: "f3",
                                    type,
                                    topic,
                                    ...timing
                                });
                            }
                        }
                    } catch (ignored) {}
                }
            }

            sessions.sort((a, b) => a.startTimeMillis - b.startTimeMillis);

            races[roundNum] = {
                round: roundNum,
                raceName: raceName,
                slug: slug,
                url: roundUrl,
                sessions: sessions
            };
            roundNum++;
        } catch (err) {
            console.warn(`[F3 Calendar] Failed to scrape round ${roundUrl}: ${err.message}`);
        }
    }

    const calendarRef = db.ref(`${DB_PATHS.calendar}/f3/${year}`);
    await calendarRef.set(races);
    await db.ref(`${DB_PATHS.calendar}/f3/last_sync`).set({
        timestamp: Date.now(),
        iso: new Date().toISOString(),
        totalRaces: Object.keys(races).length,
        year: year
    });

    console.log(`✅ [F3 Calendar] Saved ${Object.keys(races).length} rounds for season ${year} to RTDB under ${DB_PATHS.calendar}/f3/${year}.`);
    return { success: true, totalRaces: Object.keys(races).length };
}

/**
 * Synchronizes all calendars (F1, F2, F3) for the current year into app_config/db_calendar.
 * Restores app_config/calendar with the 24 circuits history.
 * Removes accidental root db_calendar.
 */
async function syncAllCalendars(db) {
    const currentYear = new Date().getFullYear();
    console.log(`Starting full calendar sync for season ${currentYear} into ${DB_PATHS.calendar}...`);

    // 1. Restore legacy circuit history to app_config/calendar
    try {
        await restoreLegacyCalendar(db);
    } catch (restErr) {
        console.error("Error restoring app_config/calendar:", restErr.message);
    }

    // 2. Remove accidental root db_calendar
    try {
        await db.ref("db_calendar").remove();
        console.log("Removed accidental root /db_calendar path.");
    } catch (cleanErr) {
        console.warn("Could not remove root db_calendar:", cleanErr.message);
    }

    // 3. Populate F1, F2, F3 into app_config/db_calendar
    const results = {};
    try {
        results.f1 = await fetchAndStoreF1Calendar(db, currentYear);
    } catch (e) {
        console.error("F1 Calendar sync failed:", e.message);
        results.f1 = { success: false, error: e.message };
    }

    try {
        results.f2 = await scrapeAndStoreF2Calendar(db, currentYear);
    } catch (e) {
        console.error("F2 Calendar sync failed:", e.message);
        results.f2 = { success: false, error: e.message };
    }

    try {
        results.f3 = await scrapeAndStoreF3Calendar(db, currentYear);
    } catch (e) {
        console.error("F3 Calendar sync failed:", e.message);
        results.f3 = { success: false, error: e.message };
    }

    return {
        timestamp: new Date().toISOString(),
        year: currentYear,
        restoredLegacyCalendar: true,
        results
    };
}

module.exports = {
    fetchAndStoreF1Calendar,
    scrapeAndStoreF2Calendar,
    scrapeAndStoreF3Calendar,
    syncAllCalendars,
    restoreLegacyCalendar,
    normalizeF1Race,
    computeSessionTimes,
    DB_PATHS
};

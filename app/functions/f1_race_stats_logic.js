const axios = require("axios");
const { PATHS, APIS, loadMappings } = require("./f1_constants");
const { processCircuitRaceUpdate } = require("./f1_archive_logic");
const { syncSeasonStatsForRound, syncDriverSeasonStats, syncConstructorSeasonStats } = require("./f1_season_stats_logic");
const { detectResultVariations, adjustCareerStatsForVariations, sendRaceStatsNotification } = require("./f1_penalty_logic");

async function executeRaceStatsUpdateWithResults(db, newSeason, newRound, trackId, results) {
    await loadMappings(db);

    const multiPathUpdates = {};
    const constructorUpdates = {};
    const constructorAssignedSlots = {};

    const teamsSnap = await db.ref(PATHS.teams).once("value");
    const teamsData = teamsSnap.val() || {};

    // Idempotence check: verify if career stats were already applied for this round
    let raceUpdateSnap = await db.ref(`${PATHS.race_updates}/${newRound}`).once("value");
    if (!raceUpdateSnap.exists()) {
        raceUpdateSnap = await db.ref(`${PATHS.race_updates}/${newSeason}/${newRound}`).once("value");
    }
    const raceUpdateData = raceUpdateSnap.val() || {};
    const careerStatsAlreadyApplied = raceUpdateData.career_stats_applied === true;

    if (careerStatsAlreadyApplied) {
        console.log(`[F1 Race Stats] Career stats already applied for Season ${newSeason}, Round ${newRound}. Skipping increments to prevent duplicate accumulation.`);
    }

    for (const result of results) {
        const driverId = result.Driver?.driverId;
        const constructorId = result.Constructor?.constructorId;
        const position = parseInt(result.position);

        const positionString = (result.positionText || "").trim();
        const statusStr = (result.status || "").toLowerCase();
        // Any driver listed in race results entered the GP (excluding only DNS / Did Not Start)
        const isDns = positionString === "DNS" || positionString === "W" || statusStr.includes("did not start");
        const isFinished = statusStr.includes("finished") || statusStr.includes("lap") || statusStr.startsWith("+");
        const isDnf = positionString === "R" || positionString === "D" || positionString === "W" || positionString === "NC" || !isFinished;
        const posStr = isDns ? "DNS" : (isDnf ? "DNF" : String(result.position || positionString || "-"));

        if (driverId) {
            multiPathUpdates[`${PATHS.drivers}/${driverId}/season_stats/season_results/${newRound}/race`] = posStr;
        }

        if (constructorId) {
            const teamDrivers = (teamsData[constructorId] && teamsData[constructorId].drivers) || [];
            let slot = "1";
            if (teamDrivers.length > 0) {
                if (driverId === teamDrivers[0]) slot = "1";
                else if (driverId === teamDrivers[1]) slot = "2";
                else {
                    slot = constructorAssignedSlots[constructorId] ? "2" : "1";
                }
            } else {
                slot = constructorAssignedSlots[constructorId] ? "2" : "1";
            }
            constructorAssignedSlots[constructorId] = true;
            multiPathUpdates[`${PATHS.teams}/${constructorId}/season_stats/season_results/${newRound}/race_driver_${slot}`] = posStr;
        }

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
        multiPathUpdates[`${PATHS.race_updates}/${newRound}/career_stats_applied`] = true;
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

    // Synchronize round season_stats (results, qualifying, sprint, standings)
    await syncSeasonStatsForRound(db, newRound, newSeason);

    return { success: true, season: newSeason, round: newRound, trackId, results: raceInfo.Results };
}

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
    const raceUpdates = (trackerData.race_updates && trackerData.race_updates[currentYear])
        ? trackerData.race_updates[currentYear]
        : (trackerData.race_updates || {});
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

async function executeScheduledRaceStatsCheck(db, messaging, options = {}) {
    const now = options.now || Date.now();
    console.log(`[F1 Race Stats Scheduler] Running check at ${new Date(now).toISOString()}...`);

    const candidates = await findRacesForUpdate(db, now, options);

    if (candidates.length === 0) {
        console.log("[F1 Race Stats Scheduler] No races active in 12-hour monitoring window. Waiting for next window.");
        return { timestamp: new Date(now).toISOString(), results: [] };
    }

    console.log(`[F1 Race Stats Scheduler] Found ${candidates.length} race(s) to process:`, candidates.map(c => `R${c.round} (${c.status})`).join(", "));

    const results = [];

    for (const cand of candidates) {
        const { race, season, round, status, tracker, forced, windowEndMillis } = cand;
        const raceName = race.raceName || `Round ${round}`;
        console.log(`[F1 Race Stats Scheduler] Processing Round ${round} (${raceName}) - Status: ${status}`);

        // Handle window expiration
        if (status === "expire_window") {
            console.log(`[F1 Race Stats Scheduler] 12-hour monitoring window closed for Round ${round}. Finalizing.`);
            await db.ref(`${PATHS.race_updates}/${round}/status`).set("completed");
            await db.ref(`${PATHS.race_updates}/${round}/completed_at`).set(now);
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
            await db.ref(`${PATHS.race_updates}/${round}`).update({
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
            await db.ref(`${PATHS.race_updates}/${round}`).update({
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
        const trackId = raceData.Circuit?.circuitId || race.circuit_id || race.circuit?.id || "";

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

            // Sync driver and constructor season stats for this specific round
            await syncSeasonStatsForRound(db, round, season);

            // Update tracker
            await db.ref(`${PATHS.race_updates}/${round}`).update({
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
                await db.ref(`${PATHS.race_updates}/${round}`).update({
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

                // 3. Re-sync driver and constructor season stats for this round with updated results
                await syncSeasonStatsForRound(db, round, season);

                // 4. Update tracker with new snapshot and record variation history
                const variationEntry = {
                    detected_at: now,
                    detected_iso: new Date(now).toISOString(),
                    attempt: attemptsCount,
                    changes: diff.changes
                };

                const currentVariationsCount = (tracker.variations_detected || 0) + 1;
                await db.ref(`${PATHS.race_updates}/${round}`).update({
                    results_snapshot: currentSnapshot,
                    attempts_count: attemptsCount,
                    last_attempt_millis: now,
                    last_attempt_iso: new Date(now).toISOString(),
                    last_variation_millis: now,
                    last_variation_iso: new Date(now).toISOString(),
                    variations_detected: currentVariationsCount
                });
                await db.ref(`${PATHS.race_updates}/${round}/variation_history`).push(variationEntry);

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

async function fetchWithRetry(url, retries = 3, delayMs = 600) {
    for (let attempt = 1; attempt <= retries; attempt++) {
        try {
            return await axios.get(url, { timeout: 15000 });
        } catch (e) {
            console.warn(`[F1 Race Stats] Attempt ${attempt}/${retries} failed for ${url}: ${e.message}`);
            if (attempt === retries) throw e;
            await new Promise(r => setTimeout(r, delayMs * attempt));
        }
    }
}

async function repopulateAllRaceUpdates(db, targetSeason = "current") {
    console.log(`[F1 Race Stats] Starting repopulation of all race updates for season ${targetSeason}...`);

    const currentYear = new Date().getFullYear().toString();
    await db.ref(`${PATHS.race_updates}/2026`).remove();
    await db.ref(`${PATHS.race_updates}/${currentYear}`).remove();

    // 1. Fetch calendar for season to discover all rounds
    const calendarUrl = `https://api.jolpi.ca/ergast/f1/${targetSeason}.json`;
    const calRes = await fetchWithRetry(calendarUrl, 3, 500);
    const calRaces = calRes.data?.MRData?.RaceTable?.Races || [];
    const detectedSeason = calRes.data?.MRData?.RaceTable?.season || currentYear;

    if (calRaces.length === 0) {
        console.log(`[F1 Race Stats] No calendar found for season ${targetSeason}.`);
        return { success: false, reason: "no_calendar" };
    }

    calRaces.sort((a, b) => parseInt(a.round) - parseInt(b.round));
    console.log(`[F1 Race Stats] Found ${calRaces.length} total calendar rounds for ${detectedSeason}.`);

    // Fetch season standings once to share across all round syncs
    const [driverStandingsRes, constructorStandingsRes] = await Promise.all([
        fetchWithRetry(APIS.driverStandings).catch(e => {
            console.warn(`Could not fetch driver standings: ${e.message}`);
            return { data: null };
        }),
        fetchWithRetry(APIS.constructorStandings).catch(e => {
            console.warn(`Could not fetch constructor standings: ${e.message}`);
            return { data: null };
        })
    ]);

    const now = Date.now();
    let latestRound = 0;
    let completedCount = 0;

    for (const calRace of calRaces) {
        const round = parseInt(calRace.round);
        const raceName = calRace.raceName || `Round ${round}`;
        const trackId = calRace.Circuit?.circuitId || "";

        // Check if results exist for this round
        const resultsUrl = `https://api.jolpi.ca/ergast/f1/${detectedSeason}/${round}/results/?format=json`;
        let roundRes;
        try {
            roundRes = await fetchWithRetry(resultsUrl, 3, 800);
        } catch (e) {
            console.warn(`[F1 Race Stats] Could not fetch results for round ${round}: ${e.message}`);
            continue;
        }

        const rawResults = roundRes.data?.MRData?.RaceTable?.Races?.[0]?.Results || [];
        if (!rawResults || rawResults.length === 0) {
            console.log(`[F1 Race Stats] Round ${round} has no completed results yet. Ending scan.`);
            break;
        }

        latestRound = Math.max(latestRound, round);
        completedCount++;
        console.log(`[F1 Race Stats] Repopulating Round ${round}: ${raceName}...`);

        const currentSnapshot = rawResults.map(res => ({
            position: res.position,
            positionText: res.positionText,
            driverId: res.Driver?.driverId,
            constructorId: res.Constructor?.constructorId,
            points: res.points,
            status: res.status,
            laps: res.laps
        }));

        const existingSnap = await db.ref(`${PATHS.race_updates}/${round}`).once("value");
        const alreadyApplied = (round <= 15) || (existingSnap.exists() && existingSnap.val()?.career_stats_applied === true);

        await db.ref(`${PATHS.race_updates}/${round}`).set({
            season: detectedSeason,
            round,
            race_name: raceName,
            status: "completed",
            career_stats_applied: alreadyApplied,
            attempts_count: 1,
            first_success_millis: existingSnap.val()?.first_success_millis || now,
            first_success_iso: existingSnap.val()?.first_success_iso || new Date(now).toISOString(),
            completed_at: existingSnap.val()?.completed_at || now,
            last_attempt_millis: now,
            last_attempt_iso: new Date(now).toISOString(),
            results_snapshot: currentSnapshot,
            variations_detected: existingSnap.val()?.variations_detected || 0
        });

        if (!alreadyApplied) {
            // Apply career stats for rounds 16 & 17 whose updates failed previously
            await executeRaceStatsUpdateWithResults(db, detectedSeason, round, trackId, rawResults);
        }
        await syncSeasonStatsForRound(db, round, detectedSeason, {
            resultsRes: roundRes,
            driverStandingsRes,
            constructorStandingsRes
        });

        // Throttle slightly between rounds to avoid Jolpica rate limits
        await new Promise(r => setTimeout(r, 400));
    }

    await db.ref(PATHS.tracker).update({
        last_race_updated: latestRound,
        last_season_updated: detectedSeason
    });

    console.log(`[F1 Race Stats] Repopulation complete! Processed ${completedCount} rounds. Latest round: ${latestRound}.`);
    return {
        success: true,
        season: detectedSeason,
        roundsProcessed: completedCount,
        latestRound
    };
}

module.exports = {
    executeRaceStatsUpdateWithResults,
    executeRaceStatsUpdate,
    findRacesForUpdate,
    executeScheduledRaceStatsCheck,
    repopulateAllRaceUpdates
};

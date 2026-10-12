const { PATHS } = require("./f1_constants");

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

module.exports = {
    detectResultVariations,
    adjustCareerStatsForVariations,
    sendRaceStatsNotification
};

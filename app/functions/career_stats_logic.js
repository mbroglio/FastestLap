/**
 * Career Stats Verification and Synchronization Logic
 * Fetches accurate career podiums, wins, and gps_entered from Jolpica API
 * and synchronizes Firebase RTDB for both drivers and constructors.
 */

const axios = require('axios');

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

async function getCount(url) {
    await sleep(250);
    try {
        const response = await axios.get(url, {
            headers: { 'User-Agent': 'FastestLap/1.0' },
            timeout: 10000
        });
        return parseInt(response.data?.MRData?.total) || 0;
    } catch (err) {
        if (err.response && err.response.status === 429) {
            console.log('   [Rate limit 429] Backing off 2s...');
            await sleep(2000);
            return getCount(url);
        }
        console.warn(`   [Warning] Error fetching ${url}: ${err.message}`);
        return 0;
    }
}

async function fetchDriverCareerStats(driverId) {
    const entries = await getCount(`https://api.jolpi.ca/ergast/f1/drivers/${driverId}/results.json?limit=1`);
    const wins = await getCount(`https://api.jolpi.ca/ergast/f1/drivers/${driverId}/results/1.json?limit=1`);
    const p2 = await getCount(`https://api.jolpi.ca/ergast/f1/drivers/${driverId}/results/2.json?limit=1`);
    const p3 = await getCount(`https://api.jolpi.ca/ergast/f1/drivers/${driverId}/results/3.json?limit=1`);
    const podiums = wins + p2 + p3;

    return {
        driverId,
        gps_entered: entries.toString(),
        podiums: podiums.toString(),
        wins: wins.toString()
    };
}

async function fetchConstructorCareerStats(constructorId) {
    const races = await getCount(`https://api.jolpi.ca/ergast/f1/constructors/${constructorId}/races.json?limit=1`);
    const wins = await getCount(`https://api.jolpi.ca/ergast/f1/constructors/${constructorId}/results/1.json?limit=1`);
    const p2 = await getCount(`https://api.jolpi.ca/ergast/f1/constructors/${constructorId}/results/2.json?limit=1`);
    const p3 = await getCount(`https://api.jolpi.ca/ergast/f1/constructors/${constructorId}/results/3.json?limit=1`);
    const podiums = wins + p2 + p3;

    return {
        constructorId,
        gps_entered: races.toString(),
        wins: wins.toString(),
        podiums: podiums.toString()
    };
}

/**
 * Synchronizes career stats (podiums, wins, gps_entered) for all drivers and teams in the DB.
 */
async function syncAllCareerStats(db) {
    console.log('[Career Stats Sync] Starting career stats synchronization from Jolpica...');
    const multiPathUpdates = {};
    const report = {
        drivers: {},
        constructors: {}
    };

    // 1. Synchronize Drivers
    const driversSnap = await db.ref('drivers').once('value');
    const driversData = driversSnap.val() || {};
    const driverIds = Object.keys(driversData);
    console.log(`[Career Stats Sync] Found ${driverIds.length} drivers.`);

    for (const dId of driverIds) {
        const cur = driversData[dId] || {};
        console.log(`[Career Stats Sync] Fetching career data for driver: ${dId}...`);
        const stats = await fetchDriverCareerStats(dId);
        
        multiPathUpdates[`drivers/${dId}/podiums`] = stats.podiums;
        multiPathUpdates[`drivers/${dId}/gps_entered`] = stats.gps_entered;

        report.drivers[dId] = {
            previous: { podiums: cur.podiums, gps_entered: cur.gps_entered },
            updated: { podiums: stats.podiums, gps_entered: stats.gps_entered }
        };
        console.log(`  Driver ${dId}: podiums ${cur.podiums} -> ${stats.podiums}, gps_entered ${cur.gps_entered} -> ${stats.gps_entered}`);
    }

    // 2. Synchronize Constructors
    const teamsSnap = await db.ref('teams').once('value');
    const teamsData = teamsSnap.val() || {};
    const teamIds = Object.keys(teamsData);
    console.log(`[Career Stats Sync] Found ${teamIds.length} constructors.`);

    for (const cId of teamIds) {
        const cur = teamsData[cId] || {};
        console.log(`[Career Stats Sync] Fetching career data for constructor: ${cId}...`);
        const stats = await fetchConstructorCareerStats(cId);

        multiPathUpdates[`teams/${cId}/podiums`] = stats.podiums;
        multiPathUpdates[`teams/${cId}/wins`] = stats.wins;
        multiPathUpdates[`teams/${cId}/gps_entered`] = stats.gps_entered;

        report.constructors[cId] = {
            previous: { podiums: cur.podiums, wins: cur.wins, gps_entered: cur.gps_entered },
            updated: { podiums: stats.podiums, wins: stats.wins, gps_entered: stats.gps_entered }
        };
        console.log(`  Team ${cId}: podiums ${cur.podiums} -> ${stats.podiums}, wins ${cur.wins} -> ${stats.wins}, gps_entered ${cur.gps_entered} -> ${stats.gps_entered}`);
    }

    if (Object.keys(multiPathUpdates).length > 0) {
        await db.ref().update(multiPathUpdates);
        console.log('[Career Stats Sync] Successfully committed all career stats updates to database.');
    }

    return report;
}

module.exports = {
    fetchDriverCareerStats,
    fetchConstructorCareerStats,
    syncAllCareerStats
};

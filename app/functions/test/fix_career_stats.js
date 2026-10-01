/**
 * Career Stats Verification and Repair Script for local testing
 */

const fs = require('fs');
const path = require('path');
const { fetchDriverCareerStats, fetchConstructorCareerStats, syncAllCareerStats } = require('../career_stats_logic');

if (require.main === module) {
    const dbPath = path.join(__dirname, 'db_test.json');
    if (!fs.existsSync(dbPath)) {
        console.error('db_test.json not found');
        process.exit(1);
    }
    const db = JSON.parse(fs.readFileSync(dbPath, 'utf8'));
    console.log('Loaded db_test.json. Starting career stats repair check...');

    (async () => {
        const drivers = Object.keys(db.drivers || {});
        console.log(`Found ${drivers.length} drivers.`);

        for (const d of drivers) {
            const current = db.drivers[d];
            console.log(`Checking driver: ${d} (current podiums: ${current.podiums}, gps_entered: ${current.gps_entered})...`);
            const stats = await fetchDriverCareerStats(d);
            console.log(`  -> Official Jolpica: podiums=${stats.podiums}, gps_entered=${stats.gps_entered}`);
            db.drivers[d].podiums = stats.podiums;
            db.drivers[d].gps_entered = stats.gps_entered;
        }

        const teams = Object.keys(db.teams || {});
        console.log(`Found ${teams.length} constructors.`);
        for (const t of teams) {
            const current = db.teams[t];
            console.log(`Checking team: ${t}...`);
            const stats = await fetchConstructorCareerStats(t);
            console.log(`  -> Official Jolpica: podiums=${stats.podiums}, wins=${stats.wins}, gps_entered=${stats.gps_entered}`);
            db.teams[t].podiums = stats.podiums;
            db.teams[t].wins = stats.wins;
            db.teams[t].gps_entered = stats.gps_entered;
        }

        fs.writeFileSync(dbPath, JSON.stringify(db, null, 2), 'utf8');
        console.log('✓ Successfully repaired db_test.json!');
    })();
}

module.exports = {
    fetchDriverCareerStats,
    fetchConstructorCareerStats,
    syncAllCareerStats
};

/**
 * Individual Function Test Runner
 * Allows testing individual scraping and helper functions
 * 
 * Usage:
 *   node test_individual_functions.js <module> <function_name> [args...]
 * 
 * Modules:
 *   junior - Junior categories functions (F2/F3 scraping)
 *   f1     - F1 logic functions
 * 
 * Examples:
 *   node test_individual_functions.js junior scrapeCalendar f2
 *   node test_individual_functions.js junior scrapeCalendar f3
 *   node test_individual_functions.js junior scrapeRaceResults f2
 *   node test_individual_functions.js junior scrapeRaceResults f3
 *   node test_individual_functions.js junior scrapeEntryList f2
 *   node test_individual_functions.js f1 loadMappings
 */

const fs = require('fs');
const path = require('path');
const axios = require('axios');
const cheerio = require('cheerio');

// Import logic modules
const f1Logic = require('../f1_logic');
const juniorLogic = require('../junior_categories_logic');

// JSON Database path
const DB_FILE = path.join(__dirname, 'db_test.json');

//const currentYear = new Date().getFullYear();

const currentYear = 2026;

const URLS = {
    f2: `https://en.wikipedia.org/wiki/${currentYear}_Formula_2_Championship`,
    f3: `https://en.wikipedia.org/wiki/${currentYear}_FIA_Formula_3_Championship`
}

// Mock Database classes (same as test_runner.js)
class MockDatabase {
    constructor(data) {
        this.data = data;
        this.pendingUpdates = {};
    }

    ref(refPath = '') {
        return new MockDatabaseReference(this, refPath);
    }

    getData() {
        return this.data;
    }

    applyUpdate(updates) {
        for (const [path, value] of Object.entries(updates)) {
            this.setValueAtPath(path, value);
        }
    }

    setValueAtPath(path, value) {
        if (!path) {
            this.data = value;
            return;
        }

        const parts = path.split('/');
        let current = this.data;

        for (let i = 0; i < parts.length - 1; i++) {
            const part = parts[i];
            if (!current[part]) {
                current[part] = {};
            }
            current = current[part];
        }

        const lastPart = parts[parts.length - 1];
        if (value === null) {
            delete current[lastPart];
        } else {
            current[lastPart] = value;
        }
    }

    getValueAtPath(path) {
        if (!path) return this.data;

        const parts = path.split('/');
        let current = this.data;

        for (const part of parts) {
            if (current === null || current === undefined) {
                return null;
            }
            current = current[part];
        }

        return current;
    }
}

class MockDatabaseReference {
    constructor(db, refPath) {
        this.db = db;
        this.refPath = refPath;
    }

    async once(eventType) {
        const value = this.db.getValueAtPath(this.refPath);
        return new MockDataSnapshot(value);
    }

    async update(updates) {
        const fullUpdates = {};
        for (const [key, value] of Object.entries(updates)) {
            const fullPath = this.refPath ? `${this.refPath}/${key}` : key;
            fullUpdates[fullPath] = value;
        }
        this.db.applyUpdate(fullUpdates);
    }

    async set(value) {
        this.db.setValueAtPath(this.refPath, value);
    }
}

class MockDataSnapshot {
    constructor(value) {
        this.value = value;
    }

    exists() {
        return this.value !== null && this.value !== undefined;
    }

    val() {
        return this.value;
    }
}

// Load database
function loadDatabase() {
    try {
        const jsonData = fs.readFileSync(DB_FILE, 'utf8');
        return JSON.parse(jsonData);
    } catch (error) {
        console.error(`Error loading database from ${DB_FILE}:`, error.message);
        process.exit(1);
    }
}

// Save database
function saveDatabase(data) {
    try {
        fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), 'utf8');
        console.log(`\n✓ Database saved to ${DB_FILE}`);
    } catch (error) {
        console.error(`Error saving database:`, error.message);
    }
}

// Function definitions for junior categories
const juniorFunctions = {
    checkRaceYesterday: {
        description: 'Check if there was a race yesterday for a series',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior checkRaceYesterday f2',
        execute: async (db, seriesId = 'f2') => {
            console.log(`Testing checkRaceYesterday for series: ${seriesId}`);
            const result = await juniorLogic.checkRaceYesterday(db, seriesId);
            console.log(`\nResult: ${result ? 'Race found yesterday' : 'No race yesterday'}`);
            return result;
        }
    },

    scrapeCalendar: {
        description: 'Scrape calendar from Wikipedia page',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior scrapeCalendar f2',
        execute: async (db, seriesId = 'f2') => {
            const url = seriesId === 'f2' 
                ? URLS.f2
                : URLS.f3;
            console.log(`Scraping calendar from: ${url}`);
            const { data } = await axios.get(url, {
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
            });
            const $ = cheerio.load(data);
            const result = await juniorLogic.scrapeCalendar($, db);
            console.log(`\nCalendar entries found: ${result?.length || 0}`);
            console.log(JSON.stringify(result, null, 2));
            return result;
        }
    },

    scrapeEntryList: {
        description: 'Scrape entry list from Wikipedia page with team logos',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior scrapeEntryList f2',
        execute: async (db, seriesId = 'f2') => {
            const url = seriesId === 'f2' 
                ? URLS.f2
                : URLS.f3;
            console.log(`Scraping entry list from: ${url}`);
            const { data } = await axios.get(url, {
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
            });
            const $ = cheerio.load(data);
            const result = await juniorLogic.scrapeEntryList($, db);
            console.log(`\nTeams found: ${Object.keys(result || {}).length}`);
            console.log(JSON.stringify(result, null, 2));
            return result;
        }
    },

    scrapeRaceResults: {
        description: 'Scrape race results from Wikipedia page and update database',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior scrapeRaceResults f2',
        execute: async (db, seriesId = 'f2') => {
            const url = seriesId === 'f2' 
                ? URLS.f2
                : URLS.f3;
            console.log(`Scraping race results from: ${url}`);
            const { data } = await axios.get(url, {
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
            });
            const $ = cheerio.load(data);
                    
            // First scrape calendar to enrich results
            const calendar = await juniorLogic.scrapeCalendar($, db);
            const result = juniorLogic.scrapeRaceResults($, calendar);
            console.log(`\nRace results found: ${result?.length || 0}`);
            console.log(JSON.stringify(result, null, 2));
            
            // Update database with scraped results
            if (result && result.length > 0) {
                const updates = {};
                const basePath = `junior_categories/${seriesId}`;
                
                result.forEach(race => {
                    const raceResultsEntry = {
                        round: race.round,
                        circuit: race.circuit,
                        sprint: race.sprint_race,
                        feature: race.feature_race,
                        nationFlagUrl: race.nationFlagUrl
                    };
                    updates[`${basePath}/results/${race.round}`] = raceResultsEntry;
                    console.log(`Updating DB for Round ${race.round}...`);
                });
                
                await db.ref().update(updates);
                console.log(`\n✓ Database updated with ${result.length} race results for ${seriesId}`);
            }
            
            return result;
        }
    },

    scrapeDriverStandings: {
        description: 'Scrape driver standings from Wikipedia page and update database',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior scrapeDriverStandings f2',
        execute: async (db, seriesId = 'f2') => {
            const url = seriesId === 'f2' 
                ? URLS.f2
                : URLS.f3;
            console.log(`Scraping driver standings from: ${url}`);
            const { data } = await axios.get(url, {
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
            });
            const $ = cheerio.load(data);
            
            // First scrape entry list to get team information
            const entryList = await juniorLogic.scrapeEntryList($, db, seriesId);
            console.log(`\nEntry list scraped: ${Object.keys(entryList || {}).length} teams found`);
            
            const result = juniorLogic.scrapeDriverStandings($, entryList);
            console.log(`\nDriver standings found: ${result?.length || 0}`);
            console.log(JSON.stringify(result, null, 2));
            
            // Update database with scraped standings
            if (result && result.length > 0) {
                const updates = {};
                const basePath = `junior_categories/${seriesId}`;
                
                result.forEach(d => {
                    updates[`${basePath}/standings/drivers/${d.position}`] = d;
                    console.log(`Updating DB for driver position ${d.position}...`);
                });
                
                await db.ref().update(updates);
                console.log(`\n✓ Database updated with ${result.length} driver standings for ${seriesId}`);
            }
            
            return result;
        }
    },

    scrapeTeamStandings: {
        description: 'Scrape team standings from Wikipedia page',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior scrapeTeamStandings f2',
        needsDb: false,
        execute: async (seriesId = 'f2') => {
            const url = seriesId === 'f2' 
                ? URLS.f2
                : URLS.f3;
            console.log(`Scraping team standings from: ${url}`);
            const { data } = await axios.get(url, {
                headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' }
            });
            const $ = cheerio.load(data);
            const result = juniorLogic.scrapeTeamStandings($);
            console.log(`\nTeam standings found: ${result?.length || 0}`);
            console.log(JSON.stringify(result, null, 2));
            return result;
        }
    },

    processSeries: {
        description: 'Process entire series (scrape all data and update DB)',
        args: ['seriesId'],
        example: 'node test_individual_functions.js junior processSeries f2',
        execute: async (db, seriesId = 'f2') => {
            const url = seriesId === 'f2' 
                ? URLS.f2
                : URLS.f3; 
            console.log(`Processing ${seriesId} series from: ${url}`);
            await juniorLogic.processSeries(db, seriesId, url);
            console.log(`\n✓ Series ${seriesId} processed successfully`);
            return true;
        }
    }
};

// Function definitions for F1 logic
const f1Functions = {
    loadMappings: {
        description: 'Load driver and team ID mappings from database',
        args: [],
        example: 'node test_individual_functions.js f1 loadMappings',
        execute: async (db) => {
            console.log('Loading ID mappings...');
            await f1Logic.loadMappings(db);
            console.log('✓ Mappings loaded successfully');
            return true;
        }
    },

    processCircuitRaceUpdate: {
        description: 'Process circuit race update with podium data',
        args: ['season', 'round'],
        example: 'node test_individual_functions.js f1 processCircuitRaceUpdate 2025 23',
        execute: async (db, season = '2025', round = '23') => {
            const newSeason = parseInt(season);
            const newRound = parseInt(round);
            
            console.log(`Fetching race results for season ${newSeason}, round ${newRound}...`);
            const response = await axios.get(`https://api.jolpi.ca/ergast/f1/${newSeason}/${newRound}/results/?format=json`);
            const raceInfo = response.data.MRData.RaceTable.Races[0];
            const results = raceInfo?.Results || [];
            const trackId = raceInfo?.Circuit?.circuitId;
            console.log("trackId:", trackId);
            
            const updates = {};
            await f1Logic.loadMappings(db);
            await f1Logic.processCircuitRaceUpdate(newSeason, newRound, trackId, results, updates, db);
            
            console.log('\nUpdates that would be applied:');
            console.log(JSON.stringify(updates, null, 2));
            
            // Apply updates to database
            db.ref().update(updates);
            
            return updates;
        }
    },

    processDriverSeasonArchive: {
        description: 'Archive driver season data to history',
        args: ['season'],
        example: 'node test_individual_functions.js f1 processDriverSeasonArchive 2024',
        execute: async (db, season = '2024') => {
            const newSeason = parseInt(season);
            console.log(`Processing driver season archive for ${newSeason}...`);
            
            const updates = {};
            await f1Logic.loadMappings(db);
            await f1Logic.processDriverSeasonArchive(newSeason, updates, db);
            
            console.log('\nDriver archive updates:');
            console.log(JSON.stringify(updates, null, 2));
            
            return updates;
        }
    },

    processConstructorSeasonArchive: {
        description: 'Archive constructor season data to history',
        args: ['season'],
        example: 'node test_individual_functions.js f1 processConstructorSeasonArchive 2024',
        execute: async (db, season = '2024') => {
            const newSeason = parseInt(season);
            console.log(`Processing constructor season archive for ${newSeason}...`);
            
            const updates = {};
            await f1Logic.loadMappings(db);
            await f1Logic.processConstructorSeasonArchive(newSeason, updates, db);
            
            console.log('\nConstructor archive updates:');
            console.log(JSON.stringify(updates, null, 2));
            
            return updates;
        }
    },

    processCircuitSeasonArchive: {
        description: 'Archive circuit season data to history',
        args: ['season'],
        example: 'node test_individual_functions.js f1 processCircuitSeasonArchive 2024',
        execute: async (db, season = '2024') => {
            const newSeason = parseInt(season);
            console.log(`Processing circuit season archive for ${newSeason}...`);
            
            const updates = {};
            await f1Logic.processCircuitSeasonArchive(newSeason, updates, db);
            
            console.log('\nCircuit archive updates:');
            console.log(JSON.stringify(updates, null, 2));
            
            return updates;
        }
    }
};

// Main execution
async function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0 || args[0] === '--help' || args[0] === '-h') {
        console.log(`
Individual Function Test Runner
================================

Usage:
  node test_individual_functions.js <module> <function_name> [args...]

Modules:
  junior - Junior categories functions (F2/F3 scraping)
  f1     - F1 logic functions (stats processing)

Options:
  --help, -h        - Show this help message
  --list, -l        - List all available functions
  --list-junior     - List junior category functions
  --list-f1         - List F1 functions

Examples (Junior Functions):
  node test_individual_functions.js junior scrapeCalendar f2
  node test_individual_functions.js junior scrapeCalendar f3
  node test_individual_functions.js junior scrapeRaceResults f2
  node test_individual_functions.js junior scrapeRaceResults f3
  node test_individual_functions.js junior scrapeEntryList f2
  node test_individual_functions.js junior scrapeDriversStanding f2
  node test_individual_functions.js junior scrapeDriversStanding f3
  node test_individual_functions.js junior checkRaceYesterday f2
  node test_individual_functions.js junior processSeries f2

Examples (F1 Functions):
  node test_individual_functions.js f1 loadMappings
  node test_individual_functions.js f1 processCircuitRaceUpdate 2025 23
        `);
        process.exit(0);
    }

    if (args[0] === '--list' || args[0] === '-l') {
        console.log('\n=== JUNIOR CATEGORY FUNCTIONS ===');
        Object.entries(juniorFunctions).forEach(([name, info]) => {
            console.log(`\n${name}`);
            console.log(`  ${info.description}`);
            console.log(`  Example: ${info.example}`);
        });

        console.log('\n\n=== F1 LOGIC FUNCTIONS ===');
        Object.entries(f1Functions).forEach(([name, info]) => {
            console.log(`\n${name}`);
            console.log(`  ${info.description}`);
            console.log(`  Example: ${info.example}`);
        });
        console.log('');
        process.exit(0);
    }

    if (args[0] === '--list-junior') {
        console.log('\n=== JUNIOR CATEGORY FUNCTIONS ===');
        Object.entries(juniorFunctions).forEach(([name, info]) => {
            console.log(`\n${name}`);
            console.log(`  ${info.description}`);
            console.log(`  Example: ${info.example}`);
        });
        console.log('');
        process.exit(0);
    }

    if (args[0] === '--list-f1') {
        console.log('\n=== F1 LOGIC FUNCTIONS ===');
        Object.entries(f1Functions).forEach(([name, info]) => {
            console.log(`\n${name}`);
            console.log(`  ${info.description}`);
            console.log(`  Example: ${info.example}`);
        });
        console.log('');
        process.exit(0);
    }

    const module = args[0];
    const functionName = args[1];
    const functionArgs = args.slice(2);

    if (!module || !functionName) {
        console.error('Error: Please specify both module and function name');
        console.error('Use --help for usage information');
        process.exit(1);
    }

    const functions = module === 'junior' ? juniorFunctions : module === 'f1' ? f1Functions : null;

    if (!functions) {
        console.error(`Error: Unknown module '${module}'`);
        console.error('Available modules: junior, f1');
        process.exit(1);
    }

    const func = functions[functionName];

    if (!func) {
        console.error(`Error: Unknown function '${functionName}' in module '${module}'`);
        console.error(`\nAvailable ${module} functions:`);
        Object.keys(functions).forEach(name => console.error(`  - ${name}`));
        process.exit(1);
    }

    console.log(`\n=== Testing: ${module} -> ${functionName} ===`);
    console.log(`Description: ${func.description}\n`);

    console.log(`Loading database from ${DB_FILE}...`);
    const dbData = loadDatabase();
    const mockDb = new MockDatabase(dbData);

    try {
        const result = func.needsDb === false 
            ? await func.execute(...functionArgs)
            : await func.execute(mockDb, ...functionArgs);
        
        console.log('\n=== Function execution completed ===');
        
        // Save changes to database if the function needed it
        if (func.needsDb !== false) {
            const updatedData = mockDb.getData();
            saveDatabase(updatedData);
        }
        
        console.log('\n✓ Test completed successfully');
    } catch (error) {
        console.error('\n✗ Error during execution:');
        console.error(error.message);
        if (error.response) {
            console.error('API Response:', error.response.status, error.response.statusText);
        }
        process.exit(1);
    }
}

// Run
main().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});

/**
 * Test Runner for Firebase Functions
 * Allows testing functions locally using exported JSON database
 * 
 * Usage:
 *   node test_runner.js <function_name> [args]
 * 
 * Available functions:
 *   - f1RaceStats       : Update F1 post-race statistics
 *   - f1Championships   : Update F1 end-of-season championships
 *   - juniorUpdate      : Update junior series (F2/F3)
 *   - juniorReset       : Reset junior series database
 * 
 * Examples:
 *   node test_runner.js f1RaceStats
 *   node test_runner.js juniorUpdate
 */

const fs = require('fs');
const path = require('path');

// Import logic modules
const f1Logic = require('../f1_logic');
const juniorLogic = require('../junior_categories_logic');

// JSON Database path
const DB_FILE = path.join(__dirname, 'db_test.json');

// Mock Firebase Database class
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

// Load JSON database
function loadDatabase() {
    try {
        const jsonData = fs.readFileSync(DB_FILE, 'utf8');
        return JSON.parse(jsonData);
    } catch (error) {
        console.error(`Error loading database from ${DB_FILE}:`, error.message);
        process.exit(1);
    }
}

// Save JSON database
function saveDatabase(data) {
    try {
        fs.writeFileSync(DB_FILE, JSON.stringify(data, null, 2), 'utf8');
        console.log(`\n✓ Database saved to ${DB_FILE}`);
    } catch (error) {
        console.error(`Error saving database to ${DB_FILE}:`, error.message);
        process.exit(1);
    }
}

// Available test functions
const testFunctions = {
    f1RaceStats: async (db) => {
        console.log('=== Running F1 Race Stats Update ===\n');
        await f1Logic.executeRaceStatsUpdate(db);
    },

    f1Championships: async (db) => {
        console.log('=== Running F1 Championships Update ===\n');
        await f1Logic.executeChampionshipsUpdate(db);
    },

    juniorUpdate: async (db) => {
        console.log('=== Running Junior Series Update ===\n');
        await juniorLogic.executeJuniorSeriesUpdate(db);
    },

    juniorReset: async (db) => {
        console.log('=== Running Junior Series Reset ===\n');
        await juniorLogic.executeJuniorReset(db);
    }
};

// Main execution
async function main() {
    const args = process.argv.slice(2);
    
    if (args.length === 0 || args[0] === '--help' || args[0] === '-h') {
        console.log(`
Test Runner for Firebase Functions
====================================

Usage:
  node test_runner.js <function_name>

Available functions:
  f1RaceStats       - Update F1 post-race statistics
  f1Championships   - Update F1 end-of-season championships
  juniorUpdate      - Update junior series (F2/F3)
  juniorReset       - Reset junior series database

Examples:
  node test_runner.js f1RaceStats
  node test_runner.js juniorUpdate

Options:
  --help, -h        - Show this help message
  --list, -l        - List all available functions
        `);
        process.exit(0);
    }

    if (args[0] === '--list' || args[0] === '-l') {
        console.log('\nAvailable functions:');
        Object.keys(testFunctions).forEach(name => {
            console.log(`  - ${name}`);
        });
        console.log('');
        process.exit(0);
    }

    const functionName = args[0];
    const testFunction = testFunctions[functionName];

    if (!testFunction) {
        console.error(`Error: Unknown function '${functionName}'`);
        console.error(`Available functions: ${Object.keys(testFunctions).join(', ')}`);
        console.error(`Use 'node test_runner.js --help' for more information`);
        process.exit(1);
    }

    console.log(`Loading database from ${DB_FILE}...`);
    const dbData = loadDatabase();
    const mockDb = new MockDatabase(dbData);

    try {
        await testFunction(mockDb);
        
        console.log('\n=== Function execution completed ===\n');
        
        const updatedData = mockDb.getData();
        saveDatabase(updatedData);
        
        console.log('✓ Test completed successfully');
    } catch (error) {
        console.error('\n✗ Error during execution:', error);
        process.exit(1);
    }
}

// Run
main().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});

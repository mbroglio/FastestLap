# Quick Test Commands for Firebase Functions
# ============================================
# This file contains ready-to-use PowerShell commands for testing

# Navigate to the functions directory
cd "c:\GitHub_Repositories\AndroidStudioProjects\Progetto Dispositivi Mobili\FastestLap\app\functions"

# --------------------------------------------
# HELP & INFO COMMANDS
# --------------------------------------------

# Show help for complete functions
node test_runner.js --help

# Show help for individual functions
node test_individual_functions.js --help

# List all complete functions
node test_runner.js --list

# List all individual functions
node test_individual_functions.js --list

# List only junior category functions
node test_individual_functions.js --list-junior

# List only F1 logic functions
node test_individual_functions.js --list-f1

# --------------------------------------------
# DATABASE BACKUP & RESTORE
# --------------------------------------------

# Create a backup of the test database
Copy-Item db_test.json db_test.backup.json

# Restore from backup
Copy-Item db_test.backup.json db_test.json

# Create a backup with timestamp
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
Copy-Item db_test.json "db_test.backup.$timestamp.json"

# --------------------------------------------
# RUN TEST FUNCTIONS
# --------------------------------------------

# === COMPLETE WORKFLOW FUNCTIONS ===

# Test F1 Race Statistics Update
node test_runner.js f1RaceStats

# Test F1 Championships Update
node test_runner.js f1Championships

# Test Junior Series (F2/F3) Update
node test_runner.js juniorUpdate

# Test Junior Series Reset
node test_runner.js juniorReset

# === INDIVIDUAL JUNIOR CATEGORY FUNCTIONS ===

# Check if race was yesterday
node test_individual_functions.js junior checkRaceYesterday f2
node test_individual_functions.js junior checkRaceYesterday f3

# Scrape calendar from Wikipedia
node test_individual_functions.js junior scrapeCalendar

# Scrape entry list (teams and drivers)
node test_individual_functions.js junior scrapeEntryList

# Scrape race results matrix
node test_individual_functions.js junior scrapeRaceResults

# Scrape driver standings
node test_individual_functions.js junior scrapeDriverStandings

# Scrape team standings
node test_individual_functions.js junior scrapeTeamStandings

# Process entire series (all scraping + DB update)
node test_individual_functions.js junior processSeries f2
node test_individual_functions.js junior processSeries f3

# === INDIVIDUAL F1 LOGIC FUNCTIONS ===

# Load driver and team ID mappings
node test_individual_functions.js f1 loadMappings

# Process circuit race update (podium data)
node test_individual_functions.js f1 processCircuitRaceUpdate 2025 23

# Archive driver season data
node test_individual_functions.js f1 processDriverSeasonArchive 2024

# Archive constructor season data
node test_individual_functions.js f1 processConstructorSeasonArchive 2024

# Archive circuit season data
node test_individual_functions.js f1 processCircuitSeasonArchive 2024

# --------------------------------------------
# INSPECT DATABASE RESULTS
# --------------------------------------------

# View entire database (formatted)
Get-Content db_test.json | ConvertFrom-Json | ConvertTo-Json -Depth 10

# View specific sections
$db = Get-Content db_test.json | ConvertFrom-Json

# View F1 drivers
$db.drivers | ConvertTo-Json -Depth 5

# View F1 teams
$db.teams | ConvertTo-Json -Depth 5

# View app config
$db.app_config | ConvertTo-Json -Depth 5

# View junior categories
$db.junior_categories | ConvertTo-Json -Depth 5

# View specific driver (example: verstappen)
$db.drivers.verstappen | ConvertTo-Json -Depth 5

# View F2 data
$db.junior_categories.f2 | ConvertTo-Json -Depth 5

# View F3 data
$db.junior_categories.f3 | ConvertTo-Json -Depth 5

# View stats tracker
$db.app_config.stats_tracker | ConvertTo-Json

# --------------------------------------------
# COMPLETE TESTING WORKFLOW
# --------------------------------------------

# 1. Backup current database
Copy-Item db_test.json db_test.backup.json

# 2. Run your test
node test_runner.js f1RaceStats

# 3. Inspect the results
$db = Get-Content db_test.json | ConvertFrom-Json
$db.app_config.stats_tracker

# 4. If needed, restore the backup
# Copy-Item db_test.backup.json db_test.json

# --------------------------------------------
# ADVANCED: MODIFY TEST DATA
# --------------------------------------------

# Load database into PowerShell object
$db = Get-Content db_test.json | ConvertFrom-Json

# Modify data (example: reset last race updated)
$db.app_config.stats_tracker.last_race_updated = 22

# Save modified database
$db | ConvertTo-Json -Depth 100 | Set-Content db_test.json

# Now run test with modified data
node test_runner.js f1RaceStats

# --------------------------------------------
# TESTING SPECIFIC SCENARIOS
# --------------------------------------------

# Scenario 1: Test with a new F1 race
# - Manually lower the last_race_updated value
# - Run f1RaceStats to see it process the "new" race

# Scenario 2: Test championship update
# - Set last_champ_season to previous year
# - Run f1Championships

# Scenario 3: Test junior race detection
# - Edit a race date in junior_categories/f2/calendar or f3/calendar
# - Set feature_date or sprint_date to yesterday's date (format: "10 December")
# - Run juniorUpdate

# --------------------------------------------
# USEFUL FILTERS AND SEARCHES
# --------------------------------------------

# Find all drivers with more than 5 wins
$db = Get-Content db_test.json | ConvertFrom-Json
$db.drivers.PSObject.Properties | Where-Object { [int]$_.Value.wins -gt 5 } | Select-Object Name, @{N='Wins';E={$_.Value.wins}}

# Find all teams sorted by championships
$db.teams.PSObject.Properties | Sort-Object { [int]$_.Value.world_championships } -Descending | Select-Object Name, @{N='Championships';E={$_.Value.world_championships}}

# Get calendar sorted by round
$db.app_config.calendar.PSObject.Properties | Sort-Object { [int]$_.Value.round } | Select-Object Name, @{N='Round';E={$_.Value.round}}

# --------------------------------------------
# TROUBLESHOOTING
# --------------------------------------------

# If you get "Cannot find module" errors:
# & "C:\Program Files\nodejs\npm.cmd" install

# If JSON is corrupted, restore from backup:
# Copy-Item db_test.backup.json db_test.json

# Check Node.js version (should be compatible with Node 22):
# node --version

# View detailed npm info:
# & "C:\Program Files\nodejs\npm.cmd" list

# --------------------------------------------
# ONE-LINER TEST COMMANDS
# --------------------------------------------

# Quick test with automatic backup
Copy-Item db_test.json db_test.backup.json ; node test_runner.js f1RaceStats

# Test and immediately view stats tracker
node test_runner.js f1RaceStats ; (Get-Content db_test.json | ConvertFrom-Json).app_config.stats_tracker

# Test junior and show F2 standings
node test_runner.js juniorUpdate ; (Get-Content db_test.json | ConvertFrom-Json).junior_categories.f2.standings

# --------------------------------------------
# END OF QUICK REFERENCE
# --------------------------------------------

# Firebase Functions Testing Guide - Quick Start

## ✅ Setup Complete!

Your Firebase Functions are now ready for local testing with the exported JSON database.

## 📁 Files Added

1. **test_runner.js** - Test complete functions (main workflows)
2. **test_individual_functions.js** - Test individual functions (scraping, helpers)
3. **TESTING_README.md** - Comprehensive testing guide
4. **TEST_COMMANDS.ps1** - Quick reference PowerShell commands
5. **db_test.backup.json** - Backup of your test database

## 🚀 Quick Start

### 1. Run a Complete Function

Open PowerShell in the functions directory and run:

```powershell
node test_runner.js f1RaceStats
```

### 2. Run Individual Functions

Test specific scraping or helper functions:

```powershell
# Junior calendar and race results (supports F2 or F3)
node test\test_individual_functions.js junior scrapeCalendar f2
node test\test_individual_functions.js junior scrapeCalendar f3
node test\test_individual_functions.js junior scrapeRaceResults f2
node test\test_individual_functions.js junior scrapeRaceResults f3
node test\test_individual_functions.js junior scrapeEntryList f2
node test\test_individual_functions.js junior checkRaceYesterday f2
node test\test_individual_functions.js junior processSeries f2

# F1 logic functions
node test\test_individual_functions.js f1 loadMappings
node test\test_individual_functions.js f1 processCircuitRaceUpdate 2025 23
```

### 3. Available Complete Functions

- `f1RaceStats` - Update F1 post-race statistics
- `f1Championships` - Update F1 end-of-season championships
- `juniorUpdate` - Update junior series (F2/F3)
- `juniorReset` - Reset junior series database

### 4. View Help

```powershell
# Complete functions
node test_runner.js --help

# Individual functions
node test_individual_functions.js --help
node test_individual_functions.js --list
```

## 📋 Common Commands

### Backup Database Before Testing

```powershell
Copy-Item db_test.json db_test.backup.json
```

### Run a Test

```powershell
node test_runner.js f1RaceStats
```

### Restore Database After Testing

```powershell
Copy-Item db_test.backup.json db_test.json
```

### View Results in PowerShell

```powershell
$db = Get-Content db_test.json | ConvertFrom-Json
$db.app_config.stats_tracker
```

## 🔍 How It Works

1. **test_runner.js** loads `db_test.json` into memory
2. Creates a mock Firebase database that operates on the JSON data
3. Runs your selected function
4. Saves the modified data back to `db_test.json`

## ⚠️ Important Notes

- **Database is Modified**: Each test run will modify `db_test.json`
- **Always Backup**: Create backups before testing
- **API Calls**: Functions that call external APIs (F1 stats, Wikipedia) still make real HTTP
  requests
- **Date-Sensitive**: Some functions check dates (e.g., junior series checks for yesterday's races)

## 📖 Full Documentation

See **TESTING_README.md** for:

- Detailed function descriptions
- Advanced testing scenarios
- Debugging tips
- Integration workflows

See **TEST_COMMANDS.ps1** for:

- Ready-to-use PowerShell commands
- Database inspection techniques
- Common testing workflows

## 🎯 Example Workflow

```powershell
# Navigate to functions directory
cd "c:\GitHub_Repositories\AndroidStudioProjects\Progetto Dispositivi Mobili\FastestLap\app\functions"

# Backup database
Copy-Item db_test.json db_test.backup.json

# Run F2 calendar scraping with enriched data (circuit names and flags)
node test\test_individual_functions.js junior scrapeCalendar f2

# Run F3 race results with enriched data (circuits and flags from calendar)
node test\test_individual_functions.js junior scrapeRaceResults f3

# Run complete junior series update
node test_runner.js juniorUpdate

# View what changed in the database
$db = Get-Content db_test.json | ConvertFrom-Json
$db.junior_categories.f2.calendar

# If you want to restore
Copy-Item db_test.backup.json db_test.json
```

## 🐛 Troubleshooting

### "Cannot find module" Error

Install dependencies:

```powershell
npm install
```

### PowerShell Execution Policy Error

Use npm.cmd directly:

```powershell
& "C:\Program Files\nodejs\npm.cmd" install
```

### Function Doesn't Update Data

- Check console output for specific error messages
- Some functions only run when conditions are met (e.g., new race detected)
- Review function logic to understand trigger conditions

## ✨ Next Steps

1. Read **TESTING_README.md** for comprehensive documentation
2. Try running each test function to see what they do
3. Check **TEST_COMMANDS.ps1** for more advanced PowerShell commands
4. Experiment with modifying `db_test.json` to test different scenarios

---

**Happy Testing! 🎉**

For questions, check the console output - all functions include detailed logging.

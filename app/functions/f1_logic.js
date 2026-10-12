/**
 * F1 Logic Entry Point & Facade
 * 
 * Modular architecture:
 * - f1_constants.js: Shared paths, APIs, mapping cache and loader.
 * - f1_archive_logic.js: End-of-season championships and history archiving.
 * - f1_season_stats_logic.js: Driver and constructor season stats calculation from Jolpica and sync.
 * - f1_penalty_logic.js: Post-race penalty variation detection, career stats adjustments, and notifications.
 * - f1_race_stats_logic.js: Post-race stats processing and dynamic calendar monitoring scheduler.
 * - career_stats_logic.js: Career stats calculations and Wikipedia/Jolpica scraping.
 */

const constants = require("./f1_constants");
const archiveLogic = require("./f1_archive_logic");
const seasonStatsLogic = require("./f1_season_stats_logic");
const penaltyLogic = require("./f1_penalty_logic");
const raceStatsLogic = require("./f1_race_stats_logic");
const careerStatsLogic = require("./career_stats_logic");

module.exports = {
    // Constants & Mappings
    PATHS: constants.PATHS,
    APIS: constants.APIS,
    loadMappings: constants.loadMappings,
    getTeamMap: constants.getTeamMap,
    getDriverMap: constants.getDriverMap,

    // Race Stats & Scheduling
    executeRaceStatsUpdate: raceStatsLogic.executeRaceStatsUpdate,
    executeRaceStatsUpdateWithResults: raceStatsLogic.executeRaceStatsUpdateWithResults,
    findRacesForUpdate: raceStatsLogic.findRacesForUpdate,
    executeScheduledRaceStatsCheck: raceStatsLogic.executeScheduledRaceStatsCheck,
    repopulateAllRaceUpdates: raceStatsLogic.repopulateAllRaceUpdates,

    // End-of-Season Championships & History
    executeChampionshipsUpdate: archiveLogic.executeChampionshipsUpdate,
    processCircuitRaceUpdate: archiveLogic.processCircuitRaceUpdate,
    processDriverSeasonArchive: archiveLogic.processDriverSeasonArchive,
    processConstructorSeasonArchive: archiveLogic.processConstructorSeasonArchive,
    processCircuitSeasonArchive: archiveLogic.processCircuitSeasonArchive,
    createHistoryEntryDriver: archiveLogic.createHistoryEntryDriver,
    createHistoryEntryConstructor: archiveLogic.createHistoryEntryConstructor,
    createHistoryEntryCircuit: archiveLogic.createHistoryEntryCircuit,
    manageHistoryArray: archiveLogic.manageHistoryArray,
    createCalendarEntry: archiveLogic.createCalendarEntry,

    // Driver & Constructor Season Stats
    syncSeasonStatsForRound: seasonStatsLogic.syncSeasonStatsForRound,
    syncDriverSeasonStats: seasonStatsLogic.syncDriverSeasonStats,
    fetchAndCalculateDriverSeasonStats: seasonStatsLogic.fetchAndCalculateDriverSeasonStats,
    syncConstructorSeasonStats: seasonStatsLogic.syncConstructorSeasonStats,
    fetchAndCalculateConstructorSeasonStats: seasonStatsLogic.fetchAndCalculateConstructorSeasonStats,

    // Post-Race Penalty & Variations
    detectResultVariations: penaltyLogic.detectResultVariations,
    adjustCareerStatsForVariations: penaltyLogic.adjustCareerStatsForVariations,
    sendRaceStatsNotification: penaltyLogic.sendRaceStatsNotification,

    // Career Stats
    syncAllCareerStats: careerStatsLogic.syncAllCareerStats,
    fetchDriverCareerStats: careerStatsLogic.fetchDriverCareerStats,
    fetchConstructorCareerStats: careerStatsLogic.fetchConstructorCareerStats
};
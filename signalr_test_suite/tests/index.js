/**
 * Index / Manifest for FastestLap Test Suite
 */

const testTrackMapAnalyzer = require('./test_track_map_analyzer');
const testLiveTimingEngine = require('./test_live_timing_engine');
const testBakuRace = require('./circuits/test_baku_race');
const testSepangRace = require('./circuits/test_sepang_race');
const testTelemetryDecoder = require('./test_telemetry_decoder');
const testCornerSync = require('./test_corner_sync');
const testSepangSessions = require('./test_sepang_sessions');
const testNegotiate = require('./test_negotiate');
const testLiveConnection = require('./test_live_connection');
const testRateLimits = require('./test_rate_limits');

module.exports = {
  testTrackMapAnalyzer,
  testLiveTimingEngine,
  testBakuRace,
  testSepangRace,
  testTelemetryDecoder,
  testCornerSync,
  testSepangSessions,
  testNegotiate,
  testLiveConnection,
  testRateLimits
};

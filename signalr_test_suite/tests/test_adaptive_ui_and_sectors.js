/**
 * test_adaptive_ui_and_sectors.js
 * Unit Test Suite for Sector Colors, Cockpit Toggle, and Adaptive Session UI
 */

const assert = require('assert');
const fs = require('fs');
const path = require('path');
const vm = require('vm');

async function testAdaptiveUiAndSectors() {
  console.log('\n===============================================================================');
  console.log('TEST SUITE: Adaptive Session UI, Sector Timing Colors & Cockpit Toggle');
  console.log('===============================================================================');

  // Load files in VM context
  const jsDir = path.join(__dirname, '..', 'js');
  const timingStateSrc = fs.readFileSync(path.join(jsDir, 'timing_state.js'), 'utf8');
  const raceControlSrc = fs.readFileSync(path.join(jsDir, 'race_control.js'), 'utf8');
  const trackCanvasSrc = fs.readFileSync(path.join(jsDir, 'track_canvas.js'), 'utf8');
  const telemetryCockpitSrc = fs.readFileSync(path.join(jsDir, 'telemetry_cockpit.js'), 'utf8');
  const timingTablesSrc = fs.readFileSync(path.join(jsDir, 'timing_tables.js'), 'utf8');
  const liveTimingAppSrc = fs.readFileSync(path.join(jsDir, 'live_timing_app.js'), 'utf8');

  // Create mock DOM environment
  const elements = {};
  function createMockElement(id) {
    const el = {
      id: id,
      style: {},
      classList: {
        classes: new Set(),
        add(c) { this.classes.add(c); },
        remove(c) { this.classes.delete(c); },
        contains(c) { return this.classes.has(c); }
      },
      innerHTML: '',
      textContent: '',
      className: '',
      appendChild: () => {},
      addEventListener: () => {},
      getContext: () => ({
        clearRect: () => {},
        beginPath: () => {},
        arc: () => {},
        fill: () => {},
        stroke: () => {},
        drawImage: () => {}
      })
    };
    elements[id] = el;
    return el;
  }

  // Pre-populate mock DOM elements
  const mockIds = [
    'c1BatteryBox', 'c1BoostBox', 'c1SectorsBox', 'c1S1Cell', 'c1S2Cell', 'c1S3Cell',
    'c1S1Val', 'c1S2Val', 'c1S3Val', 'c1SocVal', 'c1SocBar', 'c1BoostBadge',
    'c1Speed', 'c1Gear', 'c1Rpm', 'c1ThrBar', 'c1ThrVal', 'c1BrkBar', 'c1BrkVal',
    'c1AeroBadge', 'c1ColorDot', 'c1Title', 'c1Team', 'c1LocText',
    'c2BatteryBox', 'c2BoostBox', 'c2SectorsBox', 'c2S1Cell', 'c2S2Cell', 'c2S3Cell',
    'c2S1Val', 'c2S2Val', 'c2S3Val', 'c2SocVal', 'c2SocBar', 'c2BoostBadge',
    'c2Speed', 'c2Gear', 'c2Rpm', 'c2ThrBar', 'c2ThrVal', 'c2BrkBar', 'c2BrkVal',
    'c2AeroBadge', 'c2ColorDot', 'c2Title', 'c2Team', 'c2LocText',
    'statusBarFlag', 'statusBarFlagIcon', 'statusBarFlagText',
    'statusBarSessionVal', 'statusBarLapVal', 'statusBarClockVal',
    'fullStandingsBody', 'compactStandingsBody'
  ];
  mockIds.forEach(id => createMockElement(id));

  const fullThead = { innerHTML: '' };
  const compactThead = { innerHTML: '' };

  const sandbox = {
    console: console,
    document: {
      createElement(tag) {
        return createMockElement('el_' + Math.random());
      },
      getElementById(id) {
        return elements[id] || createMockElement(id);
      },
      querySelectorAll(selector) {
        return [];
      },
      querySelector(selector) {
        if (selector.includes('.full-standings-table thead')) return fullThead;
        if (selector.includes('.compact-timing-table thead')) return compactThead;
        return null;
      },
      addEventListener: () => {}
    },
    window: {
      location: { search: '' },
      addEventListener: () => {}
    },
    setTimeout: (fn) => fn(),
    requestAnimationFrame: () => {}
  };

  vm.createContext(sandbox);

  // Execute scripts in sandbox
  vm.runInContext(timingStateSrc, sandbox);
  vm.runInContext(raceControlSrc, sandbox);
  vm.runInContext(trackCanvasSrc, sandbox);
  vm.runInContext(timingTablesSrc, sandbox);
  vm.runInContext(telemetryCockpitSrc, sandbox);

  // [1] Sector Colors Evaluation Test
  console.log('[1] Testing Sector Timing Colors (Purple / Green / Yellow / Gray)...');
  vm.runInContext(`
    DRIVER_LAPS = {
      '1': [
        { lap: 1, startSec: 10, dur: 90.0, s1: 30.0, s2: 30.0, s3: 30.0 },
        { lap: 2, startSec: 110, dur: 91.0, s1: 29.5, s2: 30.5, s3: 31.0 }
      ],
      '16': [
        { lap: 1, startSec: 10, dur: 89.5, s1: 29.8, s2: 29.8, s3: 29.9 }
      ]
    };
  `, sandbox);

  const sessionBests = {
    bestLap: 89.5,
    bestS1: 29.5, // #1 Lap 2
    bestS2: 29.8, // #16 Lap 1
    bestS3: 29.9  // #16 Lap 1
  };

  // Evaluate #1 at t=210 (after Lap 2 completed)
  const secTimes1 = sandbox.getDriverSectorTimes('1', 210, sessionBests);
  assert.strictEqual(secTimes1.s1Class, 'sector-purple', 'S1 must be purple (record of session)');
  assert.strictEqual(secTimes1.s2Class, 'sector-yellow', 'S2 must be yellow (slower than driver PB)');
  assert.strictEqual(secTimes1.s3Class, 'sector-yellow', 'S3 must be yellow (slower than driver PB)');
  console.log('  ✅ Driver #1 Lap 2 sectors evaluated: S1 Purple (29.5s), S2 Yellow (30.5s), S3 Yellow (31.0s)');

  // Evaluate #16 at t=110 (after Lap 1 completed)
  const secTimes16 = sandbox.getDriverSectorTimes('16', 110, sessionBests);
  assert.strictEqual(secTimes16.s1Class, 'sector-green', 'S1 must be green (personal best, but not session best)');
  assert.strictEqual(secTimes16.s2Class, 'sector-purple', 'S2 must be purple (session record)');
  assert.strictEqual(secTimes16.s3Class, 'sector-purple', 'S3 must be purple (session record)');
  console.log('  ✅ Driver #16 Lap 1 sectors evaluated: S1 Green (29.8s), S2 Purple (29.8s), S3 Purple (29.9s)');

  // Test empty/uncompleted driver -> Gray
  const secTimesEmpty = sandbox.getDriverSectorTimes('99', 10, sessionBests);
  assert.strictEqual(secTimesEmpty.s1Class, 'sector-gray', 'Empty driver sector must be gray');
  assert.strictEqual(secTimesEmpty.s1, '-', 'Empty driver sector value must be -');
  console.log('  ✅ Empty driver sectors evaluated: Gray fallback verified');

  // [2] Testing Cockpit Switch (Qualifying & Free Practice vs Race)
  console.log('\n[2] Testing Cockpit Layout Toggle...');
  vm.runInContext("currentSecond = 210;", sandbox);

  // In Practice: Battery and Boost hidden, Sectors shown
  vm.runInContext("sessionType = 'practice';", sandbox);
  sandbox.updateCockpit(1, '1');
  assert.strictEqual(elements['c1BatteryBox'].style.display, 'none', 'Practice: Battery box must be hidden');
  assert.strictEqual(elements['c1BoostBox'].style.display, 'none', 'Practice: Boost box must be hidden');
  assert.strictEqual(elements['c1SectorsBox'].style.display, 'flex', 'Practice: Sectors box must be displayed');
  assert.strictEqual(elements['c1S1Val'].textContent, '29.500', 'Practice: S1 value must be 29.500');
  assert(elements['c1S1Cell'].className.includes('sector-purple'), 'Practice: S1 cell class must contain sector-purple');
  console.log('  ✅ Practice cockpit verified: Battery/Boost hidden, Sectors active with purple S1');

  // In Qualifying: Battery and Boost hidden, Sectors shown
  vm.runInContext("sessionType = 'qualifying';", sandbox);
  sandbox.updateCockpit(1, '16');
  assert.strictEqual(elements['c1BatteryBox'].style.display, 'none', 'Qualifying: Battery box must be hidden');
  assert.strictEqual(elements['c1BoostBox'].style.display, 'none', 'Qualifying: Boost box must be hidden');
  assert.strictEqual(elements['c1SectorsBox'].style.display, 'flex', 'Qualifying: Sectors box must be displayed');
  assert.strictEqual(elements['c1S2Val'].textContent, '29.800', 'Qualifying: S2 value must be 29.800');
  assert(elements['c1S2Cell'].className.includes('sector-purple'), 'Qualifying: S2 cell class must contain sector-purple');
  console.log('  ✅ Qualifying cockpit verified: Battery/Boost hidden, Sectors active with purple S2');

  // In Race: Sectors hidden, Battery and Boost shown
  vm.runInContext("sessionType = 'race';", sandbox);
  sandbox.updateCockpit(1, '1');
  assert.strictEqual(elements['c1SectorsBox'].style.display, 'none', 'Race: Sectors box must be hidden');
  assert.strictEqual(elements['c1BatteryBox'].style.display, 'flex', 'Race: Battery box must be displayed');
  assert.strictEqual(elements['c1BoostBox'].style.display, 'flex', 'Race: Boost box must be displayed');
  assert(elements['c1SocVal'].textContent.includes('%'), 'Race: Battery SoC% populated');
  console.log('  ✅ Race cockpit verified: Sectors hidden, Battery & Boost active');

  // [3] Testing Adaptive Table Headers & Structure
  console.log('\n[3] Testing Adaptive Table Headers...');
  
  // Free Practice headers
  sandbox.renderTableHeaders('practice');
  assert(fullThead.innerHTML.includes('MIGLIOR GIRO'), 'FP full table must include MIGLIOR GIRO');
  assert(fullThead.innerHTML.includes('GIRI'), 'FP full table must include GIRI');
  assert(!fullThead.innerHTML.includes('PIT'), 'FP full table must not include PIT column');
  assert(!fullThead.innerHTML.includes('+/-'), 'FP full table must not include +/- column');
  assert(compactThead.innerHTML.includes('STATO'), 'FP compact table must include STATO');
  console.log('  ✅ Practice table headers verified (Best lap, gap, interval, last lap, tyre, laps, status)');

  // Qualifying headers
  vm.runInContext("sessionPart = 'Q';", sandbox);
  sandbox.renderTableHeaders('qualifying');
  assert(fullThead.innerHTML.includes('Q1'), 'Qualifying full table must include Q1');
  assert(fullThead.innerHTML.includes('Q2'), 'Qualifying full table must include Q2');
  assert(fullThead.innerHTML.includes('Q3'), 'Qualifying full table must include Q3');
  assert(!fullThead.innerHTML.includes('PIT'), 'Qualifying full table must not include PIT');
  assert(!fullThead.innerHTML.includes('+/-'), 'Qualifying full table must not include +/-');
  console.log('  ✅ Qualifying table headers verified (Q1, Q2, Q3, gap, tyre, laps, status)');

  // Sprint Qualifying headers
  vm.runInContext("sessionPart = 'SQ';", sandbox);
  sandbox.renderTableHeaders('qualifying');
  assert(fullThead.innerHTML.includes('SQ1'), 'Sprint Qualifying full table must include SQ1');
  assert(fullThead.innerHTML.includes('SQ2'), 'Sprint Qualifying full table must include SQ2');
  assert(fullThead.innerHTML.includes('SQ3'), 'Sprint Qualifying full table must include SQ3');
  console.log('  ✅ Sprint Qualifying table headers verified (SQ1, SQ2, SQ3)');

  // Race headers
  sandbox.renderTableHeaders('race');
  assert(fullThead.innerHTML.includes('+/-'), 'Race full table must include +/-');
  assert(fullThead.innerHTML.includes('PIT'), 'Race full table must include PIT');
  assert(compactThead.innerHTML.includes('+/-'), 'Race compact table must include +/-');
  assert(compactThead.innerHTML.includes('INTERVALLO'), 'Race compact table must include INTERVALLO');
  console.log('  ✅ Race table headers verified (+/-, pos, gap, interval, last, best, tyre, pit)');

  // [4] Testing Status Bar Adaptability
  console.log('\n[4] Testing Status Bar Adaptability Across Sessions...');
  vm.runInContext(liveTimingAppSrc, sandbox);

  // Status bar in Practice
  vm.runInContext("sessionType = 'practice'; sessionPart = 'FP2';", sandbox);
  sandbox.updateSessionStatusBar();
  assert.strictEqual(elements['statusBarSessionVal'].textContent, 'FP2', 'Practice session name');
  assert.strictEqual(elements['statusBarLapVal'].textContent, 'SESSIONE ATTIVA', 'Practice progress readout');
  console.log('  ✅ Practice status bar verified: "FP2", "SESSIONE ATTIVA"');

  // Status bar in Qualifying
  vm.runInContext("sessionType = 'qualifying'; sessionPart = 'Q2';", sandbox);
  sandbox.updateSessionStatusBar();
  assert.strictEqual(elements['statusBarSessionVal'].textContent, 'Q2', 'Qualifying session name');
  assert.strictEqual(elements['statusBarLapVal'].textContent, 'Q2 ATTIVA', 'Qualifying progress readout');
  console.log('  ✅ Qualifying status bar verified: "Q2", "Q2 ATTIVA"');

  // Status bar in Race
  vm.runInContext("sessionType = 'race'; TOTAL_LAPS = 56; focusedDriver = '1';", sandbox);
  sandbox.updateSessionStatusBar();
  assert.strictEqual(elements['statusBarSessionVal'].textContent, 'GARA', 'Race session name');
  assert(elements['statusBarLapVal'].textContent.includes('GIRO'), 'Race progress readout shows lap count');
  console.log('  ✅ Race status bar verified: "GARA", "GIRO X/56"');

  console.log('\n🎉 ALL ADAPTIVE UI & SECTOR TIMING TESTS PASSED PERFECTLY!\n');
  return true;
}

if (require.main === module) {
  testAdaptiveUiAndSectors().catch(err => {
    console.error('Test failure:', err);
    process.exit(1);
  });
}

module.exports = testAdaptiveUiAndSectors;

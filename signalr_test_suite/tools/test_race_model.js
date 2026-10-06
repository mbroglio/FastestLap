const config = require('./src/config');
const raceModel = require('./src/race_model');
const jolpicaOfficial = require('./data/jolpica_sepang_official.json');

// Simulate race every 5 seconds and record all overtakes
let previousRanks = null;
const overtakes = [];

for (let t = 0; t <= raceModel.RACE_FINISH_SEC; t += 5) {
  const drivers = Object.keys(config.DEFAULT_DRIVERS);
  const currentRanks = drivers.map(k => ({
    key: k,
    progress: raceModel.getDriverProgress(k, t)
  })).sort((a, b) => b.progress - a.progress);

  if (previousRanks) {
    for (let pos = 0; pos < currentRanks.length; pos++) {
      const drv = currentRanks[pos].key;
      const prevPos = previousRanks.findIndex(r => r.key === drv);
      if (prevPos > pos) {
        const passedDrv = previousRanks[pos].key;
        const lap = raceModel.getDriverLap(drv, t);
        overtakes.push({
          timeSec: t,
          lap: lap,
          pos: pos + 1,
          attacker: drv,
          defender: passedDrv
        });
      }
    }
  }
  previousRanks = currentRanks;
}

console.log('Total overtakes detected across the race:', overtakes.length);
console.log('\nSample overtakes:');
overtakes.slice(0, 15).forEach(o => {
  const d1 = config.DEFAULT_DRIVERS[o.attacker];
  const d2 = config.DEFAULT_DRIVERS[o.defender];
  console.log(`[t=${o.timeSec}s, Giro ${o.lap}] P${o.pos}: #${d1.number} ${d1.code} (${d1.lastName}) supera #${d2.number} ${d2.code} (${d2.lastName})!`);
});

// Verify Final Classification matches Jolpica
console.log(`\n--- VERIFYING JOLPICA OFFICIAL STANDINGS (Lap 55, t=${raceModel.RACE_FINISH_SEC}s) ---`);
const rankedAtFinish = Object.keys(config.DEFAULT_DRIVERS).map(k => ({
  key: k,
  driver: config.DEFAULT_DRIVERS[k],
  prog: raceModel.getDriverProgress(k, raceModel.RACE_FINISH_SEC),
  lap: raceModel.getDriverLap(k, raceModel.RACE_FINISH_SEC),
  isRet: raceModel.isDriverRetired(k, raceModel.RACE_FINISH_SEC)
})).sort((a, b) => b.prog - a.prog);

const leaderProg = rankedAtFinish[0].prog;
let allMatch = true;

rankedAtFinish.forEach((r, idx) => {
  const jolpicaRow = jolpicaOfficial.results[idx];
  const matched = (r.key === jolpicaRow.number);
  if (!matched) allMatch = false;
  const gapStr = r.isRet ? `DNF (Lap ${r.lap})` : (idx === 0 ? 'LEADER' : `+${(leaderProg - r.prog).toFixed(3)}s`);
  console.log(`P${idx + 1}: #${r.key} ${r.driver.code} (${gapStr}) [Jolpica: #${jolpicaRow.number} ${jolpicaRow.Driver.code}] ${matched ? '✅' : '❌'}`);
});

if (allMatch) {
  console.log('\n🎉 ALL 22 DRIVERS MATCH 100% WITH JOLPICA OFFICIAL API!');
} else {
  console.error('\n❌ Classification mismatch with Jolpica official API!');
  process.exitCode = 1;
}

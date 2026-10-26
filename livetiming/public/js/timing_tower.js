/**
 * FastestLap LiveTiming - Timing Tower (Leaderboard) Controller
 */

function parseTowerLapToSec(str) {
  if (!str || typeof str !== 'string') return 0;
  const clean = str.trim();
  if (!clean || clean === '--' || clean === '-') return 0;
  const parts = clean.split(':');
  if (parts.length === 2) {
    const mins = parseFloat(parts[0]);
    const secs = parseFloat(parts[1]);
    if (!isNaN(mins) && !isNaN(secs)) return mins * 60 + secs;
  }
  const val = parseFloat(clean);
  return !isNaN(val) && val > 0 ? val : 0;
}

function parseTowerDeltaToSec(str) {
  if (!str || typeof str !== 'string') return null;
  const clean = str.trim().toUpperCase();
  if (!clean || clean === '--' || clean === '-') return null;
  if (clean === 'LEADER' || clean === '0' || clean === '0.000' || clean === '+0.000') return 0;
  if (clean.includes('LAP') || clean.includes('L')) return null;
  const num = parseFloat(clean.replace('+', '').replace('S', ''));
  return !isNaN(num) && num >= 0 ? num : null;
}

class TimingTower {
  constructor(tableBodyElement, onSelectDriver) {
    this.tbody = tableBodyElement;
    this.onSelectDriver = onSelectDriver || (() => {});
    this.selectedDriver = null;
    this.tyresCache = {};
    this.lastLeaderboard = [];
    this.isQualifying = false;
    this.isChequered = false;
    this.sessionStatusStr = '';
  }

  setSessionContext(ctx) {
    if (!ctx || typeof ctx !== 'object') return;
    if (ctx.isQualifying !== undefined) this.isQualifying = Boolean(ctx.isQualifying);
    if (ctx.isChequered !== undefined) this.isChequered = Boolean(ctx.isChequered);
    if (ctx.sessionStatus !== undefined) this.sessionStatusStr = String(ctx.sessionStatus || '').toUpperCase();
    if (this.lastLeaderboard.length > 0) {
      this.updateLeaderboard(this.lastLeaderboard);
    }
  }

  _isQualifyingEnded(leaderboard) {
    if (!this.isQualifying) return false;
    const st = this.sessionStatusStr;
    if (st === 'INACTIVE' || st === 'FINALISED' || st === 'ENDS' || st === 'ABORTED') {
      return true;
    }
    if (st === 'FINISHED' || this.isChequered) {
      // Se tutte le vetture hanno concluso il giro lanciato (sono ai box, ritirate o con settori fermi)
      const anyActiveFlying = leaderboard.some(r => {
        if (!r || r.retired || r.inPit || r.knockedOut || r.status === 'PIT' || r.status === 'RET') return false;
        const s = r.sectors || {};
        const s0 = s['0'] || s[0] || {};
        const s1 = s['1'] || s[1] || {};
        const s2 = s['2'] || s[2] || {};
        const hasLiveSector = Boolean(
          (s0.Value && !s1.Value && !s2.Value) ||
          (s0.Value && s1.Value && !s2.Value)
        );
        return hasLiveSector;
      });
      return !anyActiveFlying;
    }
    return false;
  }

  updateTyres(tyresPayload) {
    if (!tyresPayload || typeof tyresPayload !== 'object') return;
    const lines = tyresPayload.Lines || tyresPayload;
    for (const [key, item] of Object.entries(lines)) {
      if (!item) continue;
      const num = String(item.racingNumber || item.RacingNumber || key);
      const stintsRaw = item.stints || item.Stints;
      if (!stintsRaw) continue;

      let currentStint = null;
      if (Array.isArray(stintsRaw)) {
        for (let i = stintsRaw.length - 1; i >= 0; i--) {
          if (stintsRaw[i] && (stintsRaw[i].Compound || stintsRaw[i].compound)) {
            currentStint = stintsRaw[i];
            break;
          }
        }
        if (!currentStint && stintsRaw.length > 0) currentStint = stintsRaw[stintsRaw.length - 1];
      } else if (typeof stintsRaw === 'object') {
        const keys = Object.keys(stintsRaw).sort((a, b) => Number(a) - Number(b));
        for (let i = keys.length - 1; i >= 0; i--) {
          const s = stintsRaw[keys[i]];
          if (s && (s.Compound || s.compound)) {
            currentStint = s;
            break;
          }
        }
        if (!currentStint && keys.length > 0) currentStint = stintsRaw[keys[keys.length - 1]];
      }

      if (currentStint) {
        const comp = currentStint.Compound || currentStint.compound;
        if (comp) {
          this.tyresCache[num] = {
            compound: String(comp).toUpperCase(),
            age: Number(currentStint.TotalLaps) || 0,
            new: currentStint.New === true || currentStint.New === 'true'
          };
        }
      }
    }
    if (this.lastLeaderboard.length > 0) {
      this.updateLeaderboard(this.lastLeaderboard);
    }
  }

  updateLeaderboard(rawLeaderboard) {
    if (!rawLeaderboard || !Array.isArray(rawLeaderboard)) return;
    this.lastLeaderboard = rawLeaderboard;

    // Clona le righe per non mutare in modo distruttivo lo snapshot originale se cambiano gli stati
    const leaderboard = rawLeaderboard.map(r => Object.assign({}, r));
    const isQualiEnded = this._isQualifyingEnded(leaderboard);

    if (leaderboard.length > 0) {
      if (isQualiEnded) {
        // Al termine di ogni sessione di qualifica (Q1, Q2 o Q3), azzera BEST, GAP e INT
        for (const row of leaderboard) {
          row.bestLapTime = '--';
          row.gapToLeader = '--';
          row.intervalToAhead = '--';
        }
      } else {
        // In Qualifica, rimuove eventuali tempi residui della sessione precedente (Q1/Q2)
        // per i piloti eliminati o che non hanno ancora registrato un giro nella sessione corrente
        if (this.isQualifying) {
          let maxValidLapSec = 0;
          let invalidationStarted = false;
          for (let i = 0; i < leaderboard.length; i++) {
            const row = leaderboard[i];
            const lapSec = parseTowerLapToSec(row.bestLapTime);
            if (row.knockedOut || invalidationStarted || (i > 0 && lapSec > 0 && maxValidLapSec > 0 && lapSec < maxValidLapSec - 0.001)) {
              invalidationStarted = true;
              row.bestLapTime = '--';
              row.gapToLeader = '--';
              row.intervalToAhead = '--';
            } else if (lapSec > 0) {
              maxValidLapSec = lapSec;
            } else {
              invalidationStarted = true;
              row.bestLapTime = '--';
              row.gapToLeader = '--';
              row.intervalToAhead = '--';
            }
          }
        }

        const leader = leaderboard[0];
        const leaderLapSec = parseTowerLapToSec(leader.bestLapTime);

        if (this.isQualifying && !leaderLapSec) {
          leader.gapToLeader = '--';
          leader.intervalToAhead = '--';
        } else {
          leader.gapToLeader = 'LEADER';
          leader.intervalToAhead = '--';
        }

        for (let i = 1; i < leaderboard.length; i++) {
          const curr = leaderboard[i];
          const ahead = leaderboard[i - 1];

          const currLapSec = parseTowerLapToSec(curr.bestLapTime);
          const aheadLapSec = parseTowerLapToSec(ahead.bestLapTime);

          const isGapMissing = !curr.gapToLeader || curr.gapToLeader === '--' || curr.gapToLeader === '-' || curr.gapToLeader === 'LEADER';
          const isIntMissing = !curr.intervalToAhead || curr.intervalToAhead === '--' || curr.intervalToAhead === '-' || curr.intervalToAhead === 'LEADER';

          if (!currLapSec && isGapMissing && isIntMissing) {
            curr.gapToLeader = '--';
            curr.intervalToAhead = '--';
            continue;
          }

          if (isGapMissing) {
            if (currLapSec > 0 && leaderLapSec > 0 && currLapSec >= leaderLapSec) {
              curr.gapToLeader = '+' + (currLapSec - leaderLapSec).toFixed(3);
            } else {
              const aheadGapSec = i === 1 ? 0 : parseTowerDeltaToSec(ahead.gapToLeader);
              const currIntSec = parseTowerDeltaToSec(curr.intervalToAhead);
              if (aheadGapSec !== null && currIntSec !== null) {
                curr.gapToLeader = '+' + (aheadGapSec + currIntSec).toFixed(3);
              }
            }
          }

          if (isIntMissing) {
            const currGapSec = parseTowerDeltaToSec(curr.gapToLeader);
            const aheadGapSec = i === 1 ? 0 : parseTowerDeltaToSec(ahead.gapToLeader);

            if (currGapSec !== null && aheadGapSec !== null && currGapSec >= aheadGapSec) {
              curr.intervalToAhead = '+' + (currGapSec - aheadGapSec).toFixed(3);
            } else if (currLapSec > 0 && aheadLapSec > 0 && currLapSec >= aheadLapSec) {
              curr.intervalToAhead = '+' + (currLapSec - aheadLapSec).toFixed(3);
            }
          }

          if (!curr.gapToLeader || curr.gapToLeader === '--' || curr.gapToLeader === '-') {
            const aheadGapSec = i === 1 ? 0 : parseTowerDeltaToSec(ahead.gapToLeader);
            const currIntSec = parseTowerDeltaToSec(curr.intervalToAhead);
            if (aheadGapSec !== null && currIntSec !== null) {
              curr.gapToLeader = '+' + (aheadGapSec + currIntSec).toFixed(3);
            }
          }
        }
      }
    }

    // Calcola il miglior giro assoluto della sessione (sia tra bestLapTime che tra lastLapTime)
    let overallFastestLapSec = Infinity;
    leaderboard.forEach(r => {
      const bSec = parseTowerLapToSec(r.bestLapTime);
      if (bSec > 50 && bSec < overallFastestLapSec) {
        overallFastestLapSec = bSec;
      }
      const lSec = parseTowerLapToSec(r.lastLapTime);
      if (lSec > 50 && lSec < overallFastestLapSec) {
        overallFastestLapSec = lSec;
      }
    });

    this.tbody.innerHTML = '';
    leaderboard.forEach(row => {
      const tr = document.createElement('tr');
      tr.className = `timing-row ${this.selectedDriver === row.racingNumber ? 'selected' : ''}`;
      tr.dataset.driver = row.racingNumber;

      tr.onclick = () => {
        this.selectedDriver = row.racingNumber;
        document.querySelectorAll('.timing-row').forEach(r => r.classList.remove('selected'));
        tr.classList.add('selected');
        this.onSelectDriver(row.racingNumber);
      };

      // Usa prioritariamente i dati gomme streaming live aggiornati
      const cachedTyre = this.tyresCache[row.racingNumber];
      const tyreCompound = (row.tyreCompound && row.tyreCompound !== 'UNKNOWN')
        ? row.tyreCompound
        : (cachedTyre ? cachedTyre.compound : 'MEDIUM');
      const tyreAge = (row.tyreAge !== undefined && row.tyreAge !== null && row.tyreAge > 0)
        ? row.tyreAge
        : (cachedTyre && cachedTyre.age ? cachedTyre.age : (row.tyreAge || 0));

      this.tyresCache[row.racingNumber] = {
        compound: tyreCompound,
        age: tyreAge,
        new: row.tyreNew
      };

      // Format sectors
      const s1 = row.sectors['0'] || {};
      const s2 = row.sectors['1'] || {};
      const s3 = row.sectors['2'] || {};

      const s1Class = s1.OverallFastest ? 'purple' : (s1.PersonalFastest ? 'green' : 'yellow');
      const s2Class = s2.OverallFastest ? 'purple' : (s2.PersonalFastest ? 'green' : 'yellow');
      const s3Class = s3.OverallFastest ? 'purple' : (s3.PersonalFastest ? 'green' : 'yellow');

      // Calcolo colorazione viola per BEST e LAST
      const bSec = parseTowerLapToSec(row.bestLapTime);
      const lSec = parseTowerLapToSec(row.lastLapTime);

      const isBestOverall = row.bestLapOverallFastest ||
        (overallFastestLapSec < Infinity && bSec > 0 && Math.abs(bSec - overallFastestLapSec) <= 0.005);

      const isLastOverall = row.lastLapOverallFastest ||
        (overallFastestLapSec < Infinity && lSec > 0 && Math.abs(lSec - overallFastestLapSec) <= 0.005);

      const bestClass = isBestOverall ? 'purple' : (bSec > 0 ? 'green' : '');
      const lastClass = isLastOverall ? 'purple' : (bSec > 0 && lSec > 0 && Math.abs(lSec - bSec) <= 0.005 ? 'green' : '');

      tr.innerHTML = `
        <td class="col-pos">${row.displayPosition}</td>
        <td>
          <div class="col-driver">
            <span class="team-stripe" style="background: ${row.teamColour}"></span>
            <span class="driver-code">${row.tla}</span>
            <span class="driver-num">#${row.racingNumber}</span>
          </div>
        </td>
        <td class="col-gap">${row.gapToLeader}</td>
        <td class="col-int">${row.intervalToAhead}</td>
        <td><span class="sector-val ${s1Class}">${s1.Value || '--'}</span></td>
        <td><span class="sector-val ${s2Class}">${s2.Value || '--'}</span></td>
        <td><span class="sector-val ${s3Class}">${s3.Value || '--'}</span></td>
        <td class="col-last ${isLastOverall ? 'purple-cell' : ''}"><span class="lap-val ${lastClass}">${row.lastLapTime || '--'}</span></td>
        <td class="col-best ${isBestOverall ? 'purple-cell' : ''}"><span class="lap-val ${bestClass}">${row.bestLapTime || '--'}</span></td>
        <td>
          <span class="tyre-badge ${tyreCompound}">${tyreCompound ? tyreCompound[0] : 'M'}</span>
          <span class="tyre-age">${tyreAge}L</span>
        </td>
        <td>${row.status === 'TRACK' ? row.numberOfPitStops : row.status}</td>
      `;

      this.tbody.appendChild(tr);
    });
  }

  setSelected(driverNum) {
    this.selectedDriver = String(driverNum);
    document.querySelectorAll('.timing-row').forEach(r => {
      if (r.dataset.driver === this.selectedDriver) r.classList.add('selected');
      else r.classList.remove('selected');
    });
  }
}

window.TimingTower = TimingTower;

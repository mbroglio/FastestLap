/**
 * FastestLap LiveTiming - Timing Tower (Leaderboard) Controller
 */

class TimingTower {
  constructor(tableBodyElement, onSelectDriver) {
    this.tbody = tableBodyElement;
    this.onSelectDriver = onSelectDriver || (() => {});
    this.selectedDriver = null;
  }

  updateLeaderboard(leaderboard) {
    if (!leaderboard || !Array.isArray(leaderboard)) return;

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

      // Format sectors
      const s1 = row.sectors['0'] || {};
      const s2 = row.sectors['1'] || {};
      const s3 = row.sectors['2'] || {};

      const s1Class = s1.OverallFastest ? 'purple' : (s1.PersonalFastest ? 'green' : 'yellow');
      const s2Class = s2.OverallFastest ? 'purple' : (s2.PersonalFastest ? 'green' : 'yellow');
      const s3Class = s3.OverallFastest ? 'purple' : (s3.PersonalFastest ? 'green' : 'yellow');

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
        <td>${row.bestLapTime}</td>
        <td>
          <span class="tyre-badge ${row.tyreCompound}">${row.tyreCompound ? row.tyreCompound[0] : 'M'}</span>
          <span class="tyre-age">${row.tyreAge}L</span>
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

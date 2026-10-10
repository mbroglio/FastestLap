/**
 * FastestLap — timing_tables.js
 * Modulo Rendering Tabelle Classifica per Prove Libere, Qualifiche e Gara
 */

function renderBadge(text, badgeClass) {
  if (!text || text === '-') return '<span class="cell-time-plain">-</span>';
  return '<span class="timing-badge ' + (badgeClass || '') + '">' + text + '</span>';
}

function renderTableHeaders(type) {
  const fullThead = document.querySelector('#view-standings .full-standings-table thead');
  const compactThead = document.querySelector('#side-standings .compact-timing-table thead');
  if (!fullThead && !compactThead) return;

  const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(type, sessionPart) : type;

  if (sType === 'practice') {
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-best">MIGLIOR GIRO</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-int">INTERVALLO</th>'
        + '<th class="col-last">ULTIMO GIRO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-laps">GIRI</th>'
        + '<th class="col-status">STATO</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-best">MIGLIORE</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-last">ULTIMO</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '<th class="col-side-status">STATO</th>'
        + '</tr>';
    }
  } else if (sType === 'qualifying') {
    const isSq = typeof isSprintQualifying === 'function' ? isSprintQualifying(type, sessionPart) : (sessionPart && sessionPart.toUpperCase().startsWith('SQ'));
    const q1H = isSq ? 'SQ1' : 'Q1';
    const q2H = isSq ? 'SQ2' : 'Q2';
    const q3H = isSq ? 'SQ3' : 'Q3';
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-q1">' + q1H + '</th>'
        + '<th class="col-q2">' + q2H + '</th>'
        + '<th class="col-q3">' + q3H + '</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-laps">GIRI</th>'
        + '<th class="col-status">STATO</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-best">TEMPO</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '<th class="col-side-status">STATO</th>'
        + '</tr>';
    }
  } else {
    // Gara o Sprint
    if (fullThead) {
      fullThead.innerHTML = '<tr>'
        + '<th class="col-pos">POS</th>'
        + '<th class="col-gain">+/-</th>'
        + '<th class="col-driver">PILOTA</th>'
        + '<th class="col-gap">DISTACCO</th>'
        + '<th class="col-int">INTERVALLO</th>'
        + '<th class="col-last">ULTIMO GIRO</th>'
        + '<th class="col-best">MIGLIOR GIRO</th>'
        + '<th class="col-tyre">GOMMA</th>'
        + '<th class="col-pit">PIT</th>'
        + '</tr>';
    }
    if (compactThead) {
      compactThead.innerHTML = '<tr>'
        + '<th class="col-side-eye"></th>'
        + '<th class="col-side-pos">POS</th>'
        + '<th class="col-side-gain">+/-</th>'
        + '<th class="col-side-driver">PILOTA</th>'
        + '<th class="col-side-gap">DISTACCO</th>'
        + '<th class="col-side-int">INTERVALLO</th>'
        + '<th class="col-side-last">ULTIMO</th>'
        + '<th class="col-side-best">MIGLIORE</th>'
        + '<th class="col-side-tyre">GOMMA</th>'
        + '</tr>';
    }
  }
}

function updateTimingTablesFP() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const ranked = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    return {
      key: k,
      driver: d,
      stats: stats,
      state: state,
      tireInfo: tireInfo,
      bestLap: stats.personalBest
    };
  }).sort((a, b) => a.bestLap - b.bestLap);

  const leaderTime = (ranked[0] && ranked[0].bestLap < Infinity) ? ranked[0].bestLap : null;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;

    let gapStr = '-';
    let intStr = '-';
    let gapColorClass = '';
    if (r.bestLap < Infinity && leaderTime < Infinity) {
      if (idx === 0) {
        gapStr = 'LEADER';
        intStr = '-';
      } else {
        const gapVal = r.bestLap - leaderTime;
        gapStr = '+' + gapVal.toFixed(3) + 's';
        const prevLap = ranked[idx - 1].bestLap;
        if (prevLap < Infinity) {
          intStr = '+' + (r.bestLap - prevLap).toFixed(3) + 's';
        }
      }
    }

    let statusBadgeHtml = '';
    if (r.state.isRetired) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">STOP</span>';
    } else if (r.state.inPit) {
      statusBadgeHtml = '<span class="status-badge status-inpit">' + (r.stats.lapsCount === 0 ? 'IN GARAGE' : 'IN PIT') + '</span>';
    } else if (r.state.isOutLapMerge) {
      statusBadgeHtml = '<span class="status-badge status-outlap">OUT-LAP</span>';
    } else if (r.stats.lapsCount === 0 && !window.IS_SYNTHETIC_SIMULATION) {
      statusBadgeHtml = '<span class="status-badge status-inpit">IN GARAGE</span>';
    } else {
      statusBadgeHtml = '<span class="status-badge status-on-track">ON TRACK</span>';
    }

    const tyreBadgeHtml = '<span class="tire-badge" style="border-color: ' + r.tireInfo.color + '; color: ' + r.tireInfo.color + ';">'
      + r.tireInfo.compound + ' <span class="tire-laps">(' + r.tireInfo.age + 'L)</span>'
      + '</span>';

    fullHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-num-bold">#' + d.number + '</span> ' + d.firstName + ' <span class="drv-last-bold">' + d.lastName.toUpperCase() + '</span></span></td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td class="col-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td class="col-int int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="laps-cell">' + r.stats.lapsCount + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    compactHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \'' + r.key + '\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td class="col-side-gap gap-cell ' + gapColorClass + '">' + gapStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="col-side-status">' + statusBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTablesQ() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = Object.keys(DRIVERS);
  const sessionBests = getSessionBests(currentSecond);

  const q1Cut = SESSION_DURATION * 0.35;
  const q2Cut = SESSION_DURATION * 0.70;
  let activeQ = 'Q1';
  if (sessionPart === 'Q3' || sessionPart === 'SQ3' || ((sessionPart === '' || sessionPart === 'Q' || sessionPart === 'SQ') && currentSecond >= q2Cut)) {
    activeQ = 'Q3';
  } else if (sessionPart === 'Q2' || sessionPart === 'SQ2' || ((sessionPart === '' || sessionPart === 'Q' || sessionPart === 'SQ') && currentSecond >= q1Cut)) {
    activeQ = 'Q2';
  }

  const qDrivers = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    const laps = DRIVER_LAPS[k] || [];
    const completedLaps = laps.filter(l => l.startSec + (l.dur || 0) <= currentSecond && l.dur && l.dur > 50);

    const q1Laps = completedLaps.filter(l => l.startSec < q1Cut);
    const q2Laps = completedLaps.filter(l => l.startSec >= q1Cut && l.startSec < q2Cut);
    const q3Laps = completedLaps.filter(l => l.startSec >= q2Cut);

    const tel = (typeof DRIVER_TELEMETRY !== 'undefined' && DRIVER_TELEMETRY[k]) ? DRIVER_TELEMETRY[k] : {};
    const q1Best = tel.q1Best || q1Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const q2Best = tel.q2Best || q2Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const q3Best = tel.q3Best || q3Laps.reduce((min, l) => l.dur < min ? l.dur : min, Infinity);
    const overallBest = tel.overallBest || stats.personalBest || (q3Best < Infinity ? q3Best : (q2Best < Infinity ? q2Best : q1Best));
    const officialPos = tel.position !== undefined ? tel.position : null;

    return {
      key: k,
      driver: d,
      q1Best: q1Best < Infinity ? q1Best : (completedLaps.length > 0 ? completedLaps[0].dur : Infinity),
      q2Best: q2Best,
      q3Best: q3Best,
      overallBest: overallBest,
      officialPos: officialPos,
      stats: stats,
      state: state,
      tireInfo: tireInfo,
      lapsCount: tel.pitCount !== undefined ? tel.pitCount : completedLaps.length
    };
  });

  const q1Sorted = [...qDrivers].sort((a, b) => {
    if (a.officialPos !== null && b.officialPos !== null) return a.officialPos - b.officialPos;
    if (a.overallBest !== b.overallBest) return a.overallBest - b.overallBest;
    return GRID_ORDER.indexOf(a.key) - GRID_ORDER.indexOf(b.key);
  });

  let ranked = [];
  const hasOfficialPositions = qDrivers.some(qd => qd.officialPos !== null);
  if (hasOfficialPositions) {
    ranked = q1Sorted;
  } else if (activeQ === 'Q1') {
    ranked = q1Sorted;
  } else if (activeQ === 'Q2') {
    const top15 = q1Sorted.slice(0, 15).sort((a, b) => {
      const aVal = a.q2Best < Infinity ? a.q2Best : a.q1Best;
      const bVal = b.q2Best < Infinity ? b.q2Best : b.q1Best;
      return aVal - bVal;
    });
    const elimQ1 = q1Sorted.slice(15);
    ranked = [...top15, ...elimQ1];
  } else {
    const q2Sorted = q1Sorted.slice(0, 15).sort((a, b) => {
      const aVal = a.q2Best < Infinity ? a.q2Best : a.q1Best;
      const bVal = b.q2Best < Infinity ? b.q2Best : b.q1Best;
      return aVal - bVal;
    });
    const top10 = q2Sorted.slice(0, 10).sort((a, b) => {
      const aVal = a.q3Best < Infinity ? a.q3Best : (a.q2Best < Infinity ? a.q2Best : a.q1Best);
      const bVal = b.q3Best < Infinity ? b.q3Best : (b.q2Best < Infinity ? b.q2Best : b.q1Best);
      return aVal - bVal;
    });
    const elimQ2 = q2Sorted.slice(10);
    const elimQ1 = q1Sorted.slice(15);
    ranked = [...top10, ...elimQ2, ...elimQ1];
  }

  let bestQ1Val = Infinity, bestQ2Val = Infinity, bestQ3Val = Infinity;
  ranked.forEach(r => {
    if (r.q1Best < bestQ1Val) bestQ1Val = r.q1Best;
    if (r.q2Best < bestQ2Val) bestQ2Val = r.q2Best;
    if (r.q3Best < bestQ3Val) bestQ3Val = r.q3Best;
  });

  const leaderTime = (ranked[0] && ranked[0].overallBest < Infinity) ? ranked[0].overallBest : null;
  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;

    let isEliminated = false;
    const hasAnyLaps = (bestQ1Val < Infinity);
    if (hasAnyLaps) {
      if (activeQ === 'Q1' && currentPos > 15) {
        isEliminated = true;
      } else if (activeQ === 'Q2' && currentPos > 10) {
        isEliminated = true;
      } else if (activeQ === 'Q3' && currentPos > 10) {
        isEliminated = true;
      }
    }

    let gapStr = '-';
    if (r.overallBest < Infinity && leaderTime < Infinity) {
      if (idx === 0) {
        gapStr = 'LEADER';
      } else {
        gapStr = '+' + (r.overallBest - leaderTime).toFixed(3) + 's';
      }
    }

    let statusBadgeHtml = '';
    if (isEliminated) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">OUT</span>';
    } else if (r.state.isRetired) {
      statusBadgeHtml = '<span class="status-badge status-eliminated">STOP</span>';
    } else if (r.state.inPit) {
      statusBadgeHtml = '<span class="status-badge status-inpit">' + (r.lapsCount === 0 ? 'IN GARAGE' : 'IN PIT') + '</span>';
    } else if (r.state.isOutLapMerge) {
      statusBadgeHtml = '<span class="status-badge status-outlap">OUT-LAP</span>';
    } else if (r.lapsCount === 0 && !window.IS_SYNTHETIC_SIMULATION) {
      statusBadgeHtml = '<span class="status-badge status-inpit">IN GARAGE</span>';
    } else {
      statusBadgeHtml = '<span class="status-badge status-on-track">ON TRACK</span>';
    }

    const tyreBadgeHtml = '<span class="tire-badge" style="border-color: ' + r.tireInfo.color + '; color: ' + r.tireInfo.color + ';">'
      + r.tireInfo.compound + ' <span class="tire-laps">(' + r.tireInfo.age + 'L)</span>'
      + '</span>';

    const q1Class = (r.q1Best < Infinity && r.q1Best <= bestQ1Val + 0.005) ? 'badge-purple' : (r.q1Best < Infinity ? 'badge-green' : '');
    const q2Class = (r.q2Best < Infinity && r.q2Best <= bestQ2Val + 0.005) ? 'badge-purple' : (r.q2Best < Infinity ? 'badge-green' : '');
    const q3Class = (r.q3Best < Infinity && r.q3Best <= bestQ3Val + 0.005) ? 'badge-purple' : (r.q3Best < Infinity ? 'badge-green' : '');

    const q1Str = r.q1Best < Infinity ? formatLapTime(r.q1Best) : '-';
    const q2Str = r.q2Best < Infinity ? formatLapTime(r.q2Best) : '-';
    const q3Str = r.q3Best < Infinity ? formatLapTime(r.q3Best) : '-';

    fullHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + (isEliminated ? ' row-eliminated' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-num-bold">#' + d.number + '</span> ' + d.firstName + ' <span class="drv-last-bold">' + d.lastName.toUpperCase() + '</span></span></td>'
      + '<td>' + renderBadge(q1Str, q1Class) + '</td>'
      + '<td>' + renderBadge(q2Str, q2Class) + '</td>'
      + '<td>' + renderBadge(q3Str, q3Class) + '</td>'
      + '<td class="col-gap gap-cell">' + gapStr + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="laps-cell">' + r.lapsCount + '</td>'
      + '<td>' + statusBadgeHtml + '</td>'
      + '</tr>';

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    const compactTimeStr = r.overallBest < Infinity ? formatLapTime(r.overallBest) : '-';
    compactHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + (isEliminated ? ' row-eliminated' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \'' + r.key + '\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td>' + renderBadge(compactTimeStr, 'badge-green') + '</td>'
      + '<td class="col-side-gap gap-cell">' + gapStr + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="col-side-status">' + statusBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTablesRace() {
  const fullTbody = document.getElementById('fullStandingsBody');
  const compactTbody = document.getElementById('compactStandingsBody');
  if (!fullTbody && !compactTbody) return;

  const driverKeys = GRID_ORDER;
  const sessionBests = getSessionBests(currentSecond);

  const ranked = driverKeys.map(k => {
    const d = DRIVERS[k];
    const stats = getDriverTimingStats(k, currentSecond, sessionBests);
    const state = getDriverVisualState(k, currentSecond);
    const tireInfo = getDriverTireInfo(k, currentSecond);
    return {
      key: k,
      driver: d,
      stats: stats,
      state: state,
      tireInfo: tireInfo
    };
  });

  const activeKey = (currentSideTab === 'side-telemetry') ? selectedDriver1 : focusedDriver;

  let fullHtml = '';
  let compactHtml = '';

  ranked.forEach((r, idx) => {
    const d = r.driver;
    const isCurrentActive = r.key === activeKey;
    const currentPos = idx + 1;
    const gridPos = GRID_ORDER.indexOf(r.key) + 1;
    const posGain = gridPos - currentPos;

    let gainHtml = '<span class="gain-zero">-</span>';
    if (posGain > 0) gainHtml = '<span class="gain-pos">▲' + posGain + '</span>';
    else if (posGain < 0) gainHtml = '<span class="gain-neg">▼' + Math.abs(posGain) + '</span>';

    let gapStr = '-';
    let intStr = '-';
    if (idx === 0) {
      gapStr = 'LEADER';
      intStr = '-';
    } else {
      const tel = (typeof DRIVER_TELEMETRY !== 'undefined') ? DRIVER_TELEMETRY[r.key] : null;
      if (tel && tel.gapLeader && tel.gapLeader !== '-') {
        gapStr = tel.gapLeader;
        intStr = tel.interval || '-';
      } else if (r.stats && r.stats.gapLeader && r.stats.gapLeader !== '-') {
        gapStr = r.stats.gapLeader;
        intStr = r.stats.interval || '-';
      } else {
        gapStr = '-';
        intStr = '-';
      }
    }

    const tyreBadgeHtml = '<span class="tire-badge" style="border-color: ' + r.tireInfo.color + '; color: ' + r.tireInfo.color + ';">'
      + r.tireInfo.compound + ' <span class="tire-laps">(' + r.tireInfo.age + 'L)</span>'
      + '</span>';

    const pitCount = (typeof getDriverPitCount === 'function') ? getDriverPitCount(r.key, currentSecond) : 0;

    fullHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-gain">' + gainHtml + '</td>'
      + '<td class="col-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-num-bold">#' + d.number + '</span> ' + d.firstName + ' <span class="drv-last-bold">' + d.lastName.toUpperCase() + '</span></span></td>'
      + '<td class="col-gap gap-cell">' + gapStr + '</td>'
      + '<td class="col-int int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '<td class="pit-cell">' + pitCount + '</td>'
      + '</tr>';

    const isFollowActive = (isMapFollowingDriver && focusedDriver === r.key);
    const eyeBtnClass = isFollowActive ? 'btn-side-eye active' : 'btn-side-eye';
    const eyeSvg = '<svg viewBox="0 0 16 16" width="13" height="13" fill="currentColor">'
      + '<path d="M16 8s-3-5.5-8-5.5S0 8 0 8s3 5.5 8 5.5S16 8 16 8zM1.173 8a13.133 13.133 0 0 1 1.66-2.043C4.12 4.668 5.88 3.5 8 3.5c2.12 0 3.879 1.168 5.168 2.457A13.133 13.133 0 0 1 14.828 8c-.058.087-.122.183-.195.288-.335.48-.83 1.12-1.465 1.755C11.879 11.332 10.119 12.5 8 12.5c-2.12 0-3.879-1.168-5.168-2.457A13.134 13.134 0 0 1 1.172 8z"/>'
      + '<path d="M8 5.5a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5zM7 8a1 1 0 1 1 2 0 1 1 0 0 1-2 0z"/>'
      + '</svg>';

    compactHtml += '<tr class="' + (isCurrentActive ? 'active' : '') + '" onclick="onDriverRowClick(\'' + r.key + '\')">'
      + '<td class="col-side-eye"><button class="' + eyeBtnClass + '" onclick="onEyeButtonClick(event, \'' + r.key + '\')" title="Focus su pilota">' + eyeSvg + '</button></td>'
      + '<td class="pos-cell">' + currentPos + '</td>'
      + '<td class="col-side-gain">' + gainHtml + '</td>'
      + '<td class="col-side-driver"><span class="driver-pill"><span class="color-dot" style="background: ' + d.color + ';"></span><span class="drv-code-bold">#' + d.number + ' ' + d.code + '</span></span></td>'
      + '<td class="col-side-gap gap-cell">' + gapStr + '</td>'
      + '<td class="col-side-int int-cell">' + intStr + '</td>'
      + '<td>' + renderBadge(r.stats.lastLapStr, r.stats.lastLapBadge) + '</td>'
      + '<td>' + renderBadge(r.stats.bestLapStr, r.stats.bestLapBadge) + '</td>'
      + '<td>' + tyreBadgeHtml + '</td>'
      + '</tr>';
  });

  if (fullTbody) fullTbody.innerHTML = fullHtml;
  if (compactTbody) compactTbody.innerHTML = compactHtml;
}

function updateTimingTables() {
  const sType = typeof normalizeSessionType === 'function' ? normalizeSessionType(sessionType, sessionPart) : sessionType;
  if (sType === 'practice') {
    updateTimingTablesFP();
  } else if (sType === 'qualifying') {
    updateTimingTablesQ();
  } else {
    // Gara o Sprint
    updateTimingTablesRace();
  }
}

function onDriverRowClick(drvKey) {
  expandedDriverKey = (expandedDriverKey === drvKey) ? null : drvKey;
  focusedDriver = drvKey;
  selectedDriver1 = drvKey;
  const s1 = document.getElementById('drv1Select');
  if (s1) s1.value = drvKey;
  if (typeof updateCockpit === 'function') updateCockpit(1, selectedDriver1);
  updateTimingTables();
  if (typeof drawTrack === 'function') drawTrack();
}

function onEyeButtonClick(event, drvKey) {
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }
  if (isMapFollowingDriver && focusedDriver === drvKey) {
    if (typeof resetMapFocus === 'function') resetMapFocus();
    return;
  }
  focusedDriver = drvKey;
  selectedDriver1 = drvKey;
  isMapFollowingDriver = true;
  if (typeof userZoom !== 'undefined') userZoom = 2.4;

  const resetBtn = document.getElementById('btnResetMapFocus');
  if (resetBtn) resetBtn.style.display = 'inline-flex';

  if (typeof updateZoomButtonsState === 'function') updateZoomButtonsState();
  if (typeof centerMapOnDriver === 'function') centerMapOnDriver(drvKey);

  const s1 = document.getElementById('drv1Select');
  if (s1) s1.value = drvKey;
  if (typeof updateCockpit === 'function') updateCockpit(1, drvKey);

  updateTimingTables();
  if (typeof drawTrack === 'function') drawTrack();
}

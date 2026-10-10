/**
 * FastestLap — race_control.js
 * Modulo Gestione Messaggi e Notifiche FIA Race Control & Direzione Gara
 */

let RACE_CONTROL_MESSAGES = [];
let activeRcFilter = 'ALL';

function formatRaceControlTime(m) {
  if (m && m.date) {
    try {
      const d = new Date(m.date);
      if (!isNaN(d.getTime())) {
        const hh = String(d.getHours()).padStart(2, '0');
        const mm = String(d.getMinutes()).padStart(2, '0');
        const ss = String(d.getSeconds()).padStart(2, '0');
        return hh + ':' + mm + ':' + ss;
      }
    } catch (e) {}
  }
  return formatSimClock((m && m.timeSec !== undefined) ? m.timeSec : currentSecond);
}

function getInitialRaceControlMessages(sType, sPart) {
  // Non hardcodare messaggi fittizi: se la sessione reale è in attesa o non ci sono messaggi FIA, la lista resta vuota
  return [];
}

function updateRaceControl() {
  const activeMsgs = RACE_CONTROL_MESSAGES.filter(m => (isGenericRealMode ? true : (m.timeSec || 0) <= currentSecond));
  const totalCount = document.getElementById('rcTotalCount');
  if (totalCount) totalCount.textContent = activeMsgs.length + ' Messaggi';

  const latest = activeMsgs.length > 0 ? activeMsgs[activeMsgs.length - 1] : null;
  let tickerText = '🟢 Direzione Gara FIA — In attesa di comunicazioni';
  if (latest) {
    tickerText = '🟡 ' + formatRaceControlTime(latest) + ' — ' + (latest.message || 'Direzione Gara');
  }

  const sTicker = document.getElementById('sideRcTickerCard');
  const fTicker = document.getElementById('fullRcTickerCard');
  if (sTicker) sTicker.textContent = tickerText;
  if (fTicker) fTicker.textContent = tickerText;

  const fList = document.getElementById('fullRcFeedList');
  const sList = document.getElementById('sideRcFeedList');
  if (!fList && !sList) return;

  const filtered = activeMsgs.filter(m => {
    if (activeRcFilter === 'ALL') return true;
    if (activeRcFilter === 'Flag') return m.flag || m.category === 'Flag';
    if (activeRcFilter === 'SafetyCar') return (m.category === 'SafetyCar') || (m.message && m.message.includes('SAFETY CAR'));
    if (activeRcFilter === 'Investigation') return (m.category === 'Investigation') || (m.message && m.message.includes('INVESTIGAT'));
    return true;
  }).reverse();

  let itemsHtml = '';
  if (filtered.length === 0) {
    itemsHtml = '<div class="rc-empty-notice" style="padding: 24px 12px; text-align: center; color: #8b949e; font-size: 11px; font-weight: 600;">🟢 Nessuna comunicazione dalla Direzione Gara al momento</div>';
  } else {
    itemsHtml = filtered.slice(0, 50).map(m => {
      let flagClass = '';
      const flagVal = (m.flag || '').toUpperCase();
      const msgVal = (m.message || '').toUpperCase();
      if (flagVal === 'YELLOW' || msgVal.includes('YELLOW')) flagClass = 'yellow';
      else if (flagVal === 'GREEN' || msgVal.includes('GREEN')) flagClass = 'green';
      else if (flagVal === 'RED' || msgVal.includes('RED')) flagClass = 'red';
      else if (m.category === 'SafetyCar' || msgVal.includes('SAFETY CAR')) flagClass = 'sc';

      const lapNum = m.lapNumber !== undefined ? m.lapNumber : m.lap;
      const lapLabel = lapNum ? (' • GIRO ' + lapNum) : '';

      return '<div class="rc-msg-card ' + flagClass + '">'
        + '<div class="rc-msg-meta">'
        + '<span>' + (m.category || 'DIREZIONE GARA') + lapLabel + '</span>'
        + '<span>' + formatRaceControlTime(m) + '</span>'
        + '</div>'
        + '<div class="rc-msg-text">' + m.message + '</div>'
        + '</div>';
    }).join('');
  }

  if (fList) fList.innerHTML = itemsHtml;
  if (sList) sList.innerHTML = itemsHtml;
}

function setRcFilter(cat) {
  activeRcFilter = cat;
  document.querySelectorAll('.rc-filter-btn').forEach(b => b.classList.remove('active'));
  const activeBtn = document.getElementById({
    ALL: 'rcFilterAll',
    Flag: 'rcFilterFlags',
    SafetyCar: 'rcFilterSC',
    Investigation: 'rcFilterInv'
  }[cat]);
  if (activeBtn) activeBtn.classList.add('active');
  updateRaceControl();
}

// Iniezione dinamica da Android o stream SignalR
window.updateRaceControlMessages = function(msgs) {
  if (!msgs) return;
  let parsed = msgs;
  if (typeof msgs === 'string') {
    try {
      parsed = JSON.parse(msgs);
    } catch (e) {
      console.warn('Failed to parse race control messages json:', e);
      return;
    }
  }
  if (!Array.isArray(parsed) || parsed.length === 0) return;

  const normalized = parsed.map(m => ({
    category: m.category || (m.flag ? 'Flag' : 'DIREZIONE GARA'),
    message: m.message || '',
    flag: m.flag || null,
    lap: m.lapNumber !== undefined ? m.lapNumber : m.lap,
    date: m.date || null,
    timeSec: m.timeSec !== undefined ? m.timeSec : currentSecond
  }));

  RACE_CONTROL_MESSAGES = normalized;
  updateRaceControl();
};

/**
 * FastestLap LiveTiming - Track Canvas Renderer
 * Supporta:
 * 1. Immagine circuito passata dall'app Android (?circuit_image=...)
 * 2. Geometria ufficiale del circuito + miniSectorsIndexes + modello fisico F1 QSS (Multiviewer API)
 * 3. Coordinate GPS dirette da Position.z oppure cinematica 60fps ancorata ai mini-settori live di TimingData
 */

class TrackCanvas {
  constructor(canvasElement) {
    this.canvas = canvasElement;
    this.ctx = this.canvas.getContext('2d');
    this.positions = {};
    this.drivers = {};
    this.selectedDriver = null;
    this.animationId = null;
    this.trackImage = null;

    // Tracciato vettoriale ufficiale e profilo fisico F1 per punto
    this.loadedCircuitKey = null;
    this.circuitRotationDeg = 0;
    this.circuitPoints = [];
    this.circuitCorners = [];
    this.miniSectorsIndexes = [];
    this.trackLengthMeters = 5400;
    this.referenceLapSec = 94;
    this.hasLiveGps = false;

    // Stato cinematico sul giro per ciascun pilota (sincronizzato coi mini-settori F1)
    this.driverLapState = {};
    this.lastFrameTime = performance.now();

    // Bounding Box dinamico per scalare le coordinate cartesiane reali di F1
    this.bounds = {
      minX: Infinity,
      maxX: -Infinity,
      minY: Infinity,
      maxY: -Infinity,
      isCalibrated: false
    };

    // Scie di punti separate per ciascun pilota (evita linee incrociate tra auto)
    this.driverTrails = {};
    this.maxTrailPointsPerDriver = 600;

    this._resize();
    window.addEventListener('resize', () => this._resize());
    window.setCircuitImage = (url) => this.setCircuitImage(url);

    this.onSelectDriver = null;
    this.canvas.addEventListener('click', (e) => {
      const rect = this.canvas.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const clickY = e.clientY - rect.top;
      let closestDriver = null;
      let closestDist = 20; // 20px hit test radius

      for (const [num, pos] of Object.entries(this.positions)) {
        if (!pos.screenX || !pos.screenY) continue;
        const d = Math.hypot(clickX - pos.screenX, clickY - pos.screenY);
        if (d < closestDist) {
          closestDist = d;
          closestDriver = num;
        }
      }
      if (closestDriver) {
        this.setSelectedDriver(closestDriver);
        if (typeof this.onSelectDriver === 'function') {
          this.onSelectDriver(closestDriver);
        }
      }
    });

    this.startRendering();
  }

  resizeCanvas() {
    this._resize();
  }

  _resolveDriverColor(num) {
    if (!this._driverColorsCache) this._driverColorsCache = {};
    const key = String(num);

    const driver = this.drivers[key] || this.drivers[String(parseInt(key, 10))] || {};
    const raw = driver.TeamColour || driver.teamColour || driver.Color || driver.color || driver.teamColor;
    if (raw && typeof raw === 'string') {
      const clean = raw.trim().replace(/^#/, '');
      if (/^[0-9a-fA-F]{6}$/.test(clean)) {
        const hex = '#' + clean;
        this._driverColorsCache[key] = hex;
        return hex;
      }
    }

    if (this._driverColorsCache[key]) {
      return this._driverColorsCache[key];
    }

    const team = String(driver.TeamName || driver.teamName || '').toLowerCase();
    const tla = String(driver.Tla || driver.tla || '').toUpperCase();
    const n = parseInt(key, 10);

    let hex = null;
    if (team.includes('ferrari') || n === 16 || n === 55 || tla === 'LEC' || tla === 'SAI') hex = '#e8002d';
    else if (team.includes('mclaren') || n === 4 || n === 81 || tla === 'NOR' || tla === 'PIA') hex = '#ff8000';
    else if (team.includes('red bull') || n === 1 || n === 11 || n === 3 || n === 6 || tla === 'VER' || tla === 'PER' || tla === 'HAD') hex = '#3671c6';
    else if (team.includes('mercedes') || n === 63 || n === 44 || n === 12 || tla === 'RUS' || tla === 'HAM' || tla === 'ANT') hex = '#27f4d2';
    else if (team.includes('aston') || n === 14 || n === 18 || tla === 'ALO' || tla === 'STR') hex = '#229971';
    else if (team.includes('alpine') || n === 10 || n === 31 || tla === 'GAS' || tla === 'OCO') hex = '#0093cc';
    else if (team.includes('williams') || n === 23 || n === 43 || tla === 'ALB' || tla === 'COL') hex = '#64c4ff';
    else if (team.includes('racing bulls') || team.includes('rb') || n === 30 || n === 41 || tla === 'LAW' || tla === 'TSU') hex = '#6692ff';
    else if (team.includes('audi') || team.includes('sauber') || team.includes('kick') || n === 5 || n === 77 || tla === 'BOR' || tla === 'BOT') hex = '#f50537';
    else if (team.includes('haas') || n === 27 || n === 87 || tla === 'HUL' || tla === 'BEA') hex = '#b6babd';

    if (!hex) hex = '#e10600';
    this._driverColorsCache[key] = hex;
    return hex;
  }

  setCircuitImage(url) {
    if (!url || typeof url !== 'string' || !url.trim()) return;
    const cleanUrl = url.trim();
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      this.trackImage = img;
    };
    img.src = cleanUrl;
  }

  async loadCircuitByKey(circuitKey) {
    if (!circuitKey || String(circuitKey) === String(this.loadedCircuitKey)) return;
    this.loadedCircuitKey = String(circuitKey);

    const years = [2024, 2023];
    for (const year of years) {
      try {
        const resp = await fetch(`https://api.multiviewer.app/api/v1/circuits/${circuitKey}/${year}`);
        if (!resp.ok) continue;
        const data = await resp.json();
        if (data && Array.isArray(data.x) && Array.isArray(data.y) && data.x.length > 20) {
          this._applyCircuitGeometry(data);
          return;
        }
      } catch (e) {
        // Fallback se offline
      }
    }

    if (this.circuitPoints.length === 0) {
      this._buildFallbackCircuit();
    }
  }

  _applyCircuitGeometry(data) {
    const rotDeg = Number(data.rotation) || 0;
    this.circuitRotationDeg = rotDeg;
    const rad = (rotDeg * Math.PI) / 180;
    const cos = Math.cos(rad);
    const sin = Math.sin(rad);

    // Determina se le coordinate Multiviewer sono in decimetri (scala F1 Position.z = 1/10m)
    let maxCoordAbs = 0;
    const len = Math.min(data.x.length, data.y.length);
    for (let i = 0; i < len; i++) {
      const ax = Math.abs(Number(data.x[i]) || 0);
      const ay = Math.abs(Number(data.y[i]) || 0);
      if (ax > maxCoordAbs) maxCoordAbs = ax;
      if (ay > maxCoordAbs) maxCoordAbs = ay;
    }
    const coordScale = maxCoordAbs > 2500 ? 0.1 : 1.0;

    const rotatePt = (x, y) => ({
      x: (x * cos - y * sin) * coordScale,
      y: (x * sin + y * cos) * coordScale
    });

    const pts = [];
    for (let i = 0; i < len; i++) {
      const rx = Number(data.x[i]);
      const ry = Number(data.y[i]);
      if (Number.isFinite(rx) && Number.isFinite(ry)) {
        pts.push(rotatePt(rx, ry));
      }
    }

    if (pts.length < 10) return;

    this.miniSectorsIndexes = Array.isArray(data.miniSectorsIndexes)
      ? data.miniSectorsIndexes.map(v => Number(v)).filter(v => Number.isFinite(v))
      : [];

    if (data.candidateLap && Number(data.candidateLap.lapTime) > 50) {
      this.referenceLapSec = Number(data.candidateLap.lapTime);
    }

    this.circuitCorners = [];
    if (Array.isArray(data.corners)) {
      for (const c of data.corners) {
        if (c && c.trackPosition && Number.isFinite(c.trackPosition.x) && Number.isFinite(c.trackPosition.y)) {
          const rp = rotatePt(Number(c.trackPosition.x), Number(c.trackPosition.y));
          this.circuitCorners.push({
            number: c.number,
            x: rp.x,
            y: rp.y
          });
        }
      }
    }

    this._buildPhysicsProfileOnPoints(pts);
    this.circuitPoints = pts;
    this._calibrateBoundsFromPoints(this.circuitPoints);
  }

  _buildFallbackCircuit() {
    const pts = [];
    const total = 480;
    for (let i = 0; i < total; i++) {
      const t = (i / total) * 2 * Math.PI;
      const x = 4000 * Math.cos(t) + 1200 * Math.cos(2 * t) - 600 * Math.sin(3 * t);
      const y = 2600 * Math.sin(t) + 900 * Math.sin(2 * t) + 500 * Math.cos(3 * t);
      pts.push({ x, y });
    }
    this._buildPhysicsProfileOnPoints(pts);
    this.circuitPoints = pts;
    this._calibrateBoundsFromPoints(this.circuitPoints);
  }

  /**
   * Calcola il profilo cinematico e dinamico reale F1 (Quasi-Steady-State Forward/Backward Pass)
   * su ogni punto del tracciato: distanza metrica, raggio di curvatura, velocità di percorrenza,
   * zone di staccata (longG < 0) e zone di trazione/rettilineo (longG > 0).
   */
  _buildPhysicsProfileOnPoints(pts) {
    const n = pts.length;
    if (n < 10) return;

    let totalRaw = 0;
    for (let i = 0; i < n; i++) {
      const next = pts[(i + 1) % n];
      pts[i].rawDs = Math.hypot(next.x - pts[i].x, next.y - pts[i].y);
      totalRaw += pts[i].rawDs;
    }

    // Le coordinate ufficiali F1/Multiviewer sono in decimetri (es. 54277 = 5427.7m)
    const scaleToMeters = totalRaw > 15000 ? 0.1 : 1.0;
    let cumMeters = 0;
    for (let i = 0; i < n; i++) {
      pts[i].ds = Math.max(0.4, pts[i].rawDs * scaleToMeters);
      pts[i].distMeters = cumMeters;
      cumMeters += pts[i].ds;
    }
    this.trackLengthMeters = Math.max(1000, cumMeters);

    const findIdxByOffsetMeters = (startIdx, offsetMeters) => {
      let acc = 0;
      let curr = startIdx;
      const step = offsetMeters >= 0 ? 1 : -1;
      const target = Math.abs(offsetMeters);
      let guard = 0;
      while (acc < target && guard < n) {
        curr = (curr + step + n) % n;
        acc += pts[curr].ds;
        guard++;
      }
      return curr;
    };

    const maxVms = 328 / 3.6;

    // 1. Limite di velocità in curva dalla curvatura geometrica multi-scala (14m per tornanti, 30m per esse veloci)
    for (let i = 0; i < n; i++) {
      const calcKappa = (halfWindowM) => {
        const iP = findIdxByOffsetMeters(i, -halfWindowM);
        const iN = findIdxByOffsetMeters(i, halfWindowM);
        const a1 = Math.atan2(pts[i].y - pts[iP].y, pts[i].x - pts[iP].x);
        const a2 = Math.atan2(pts[iN].y - pts[i].y, pts[iN].x - pts[i].x);
        let dT = Math.abs(a2 - a1);
        if (dT > Math.PI) dT = 2 * Math.PI - dT;
        return dT / (halfWindowM * 2);
      };

      const kappa = Math.max(calcKappa(14), calcKappa(30));
      pts[i].kappa = kappa;
      pts[i].curvature = Math.min(1, kappa / 0.035);

      if (kappa < 0.0012) {
        pts[i].vLimit = maxVms;
      } else {
        const aLat = 16 + Math.min(28, (0.006 / kappa) * 18);
        const vCorner = Math.sqrt(aLat / kappa);
        pts[i].vLimit = Math.max(84 / 3.6, Math.min(maxVms, vCorner));
      }
      pts[i].v = pts[i].vLimit;
    }

    // 2. Backward Pass: propagazione zone di frenata prima di ogni curva
    for (let pass = 0; pass < 2; pass++) {
      for (let i = n - 1; i >= 0; i--) {
        const next = pts[(i + 1) % n];
        const aBrake = 24 + 16 * Math.pow(next.v / maxVms, 2);
        const vMaxEntry = Math.sqrt(next.v * next.v + 2 * aBrake * pts[i].ds);
        if (vMaxEntry < pts[i].v) {
          pts[i].v = vMaxEntry;
        }
      }
    }

    // 3. Forward Pass: propagazione accelerazione longitudinale in uscita di curva e sui rettilinei
    for (let pass = 0; pass < 2; pass++) {
      for (let i = 0; i < n; i++) {
        const next = pts[(i + 1) % n];
        const vRatio = pts[i].v / (330 / 3.6);
        const aAccel = Math.max(1.0, 14.0 * (1 - Math.pow(vRatio, 1.6)));
        const vMaxExit = Math.sqrt(pts[i].v * pts[i].v + 2 * aAccel * pts[i].ds);
        if (vMaxExit < next.v) {
          next.v = vMaxExit;
        }
      }
    }

    // 4. Calcolo accelerazione longitudinale in G, velocità in km/h e tempo cumulato sul giro (tauSec)
    let simTimeSec = 0;
    for (let i = 0; i < n; i++) {
      const next = pts[(i + 1) % n];
      pts[i].speedKmh = Math.round(pts[i].v * 3.6);
      pts[i].longG = ((next.v * next.v - pts[i].v * pts[i].v) / (2 * pts[i].ds)) / 9.81;
      pts[i].rawTauSec = simTimeSec;
      simTimeSec += pts[i].ds / Math.max(15, pts[i].v);
    }
    this.simBaseLapSec = Math.max(60, simTimeSec);
    const refLap = this.referenceLapSec || this.simBaseLapSec;
    const tauScale = refLap / Math.max(1, simTimeSec);
    for (let i = 0; i < n; i++) {
      pts[i].tauSec = pts[i].rawTauSec * tauScale;
    }
  }

  _calibrateBoundsFromPoints(points) {
    if (!points || points.length === 0) return;
    let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
    for (const p of points) {
      if (p.x < minX) minX = p.x;
      if (p.x > maxX) maxX = p.x;
      if (p.y < minY) minY = p.y;
      if (p.y > maxY) maxY = p.y;
    }
    if (Number.isFinite(minX) && Number.isFinite(maxX)) {
      this.bounds.minX = minX;
      this.bounds.maxX = maxX;
      this.bounds.minY = minY;
      this.bounds.maxY = maxY;
      this.bounds.isCalibrated = true;
    }
  }

  reset() {
    this.positions = {};
    this.driverTrails = {};
    this.driverLapState = {};
    this.hasLiveGps = false;
    if (this.circuitPoints.length > 0) {
      this._calibrateBoundsFromPoints(this.circuitPoints);
    } else {
      this.bounds = {
        minX: Infinity,
        maxX: -Infinity,
        minY: Infinity,
        maxY: -Infinity,
        isCalibrated: false
      };
    }
  }

  _resize() {
    const rect = this.canvas.parentElement.getBoundingClientRect();
    this.width = rect.width;
    this.height = rect.height;
    const dpr = window.devicePixelRatio || 1;
    this.canvas.width = this.width * dpr;
    this.canvas.height = this.height * dpr;
    this.ctx.setTransform(1, 0, 0, 1, 0, 0);
    this.ctx.scale(dpr, dpr);
  }

  updateDrivers(driverMap) {
    this.drivers = driverMap || {};
  }

  updatePositions(positionsMap) {
    if (!positionsMap) return;

    const rotDeg = Number(this.circuitRotationDeg) || 0;
    const rad = (rotDeg * Math.PI) / 180;
    const cos = Math.cos(rad);
    const sin = Math.sin(rad);

    for (const [num, pos] of Object.entries(positionsMap)) {
      if (pos == null || typeof pos.x !== 'number' || typeof pos.y !== 'number') continue;
      if (!Number.isFinite(pos.x) || !Number.isFinite(pos.y)) continue;
      if (pos.x === 0 && pos.y === 0) continue;

      this.hasLiveGps = true;

      const rx = rotDeg !== 0 ? (pos.x * cos - pos.y * sin) : pos.x;
      const ry = rotDeg !== 0 ? (pos.x * sin + pos.y * cos) : pos.y;

      if (rx < this.bounds.minX) this.bounds.minX = rx;
      if (rx > this.bounds.maxX) this.bounds.maxX = rx;
      if (ry < this.bounds.minY) this.bounds.minY = ry;
      if (ry > this.bounds.maxY) this.bounds.maxY = ry;
      this.bounds.isCalibrated = true;

      if (!this.driverTrails[num]) {
        this.driverTrails[num] = [];
      }
      const trail = this.driverTrails[num];
      if (trail.length === 0 || Math.hypot(rx - trail[trail.length - 1].x, ry - trail[trail.length - 1].y) > 2) {
        if (trail.length >= this.maxTrailPointsPerDriver) {
          trail.shift();
        }
        trail.push({ x: rx, y: ry });
      }

      const now = performance.now();
      const existing = this.positions[num];
      let vx = 0, vy = 0;
      if (existing && existing.lastGpsTime && now > existing.lastGpsTime) {
        const dt = (now - existing.lastGpsTime) / 1000;
        if (dt > 0.05 && dt < 1.0) {
          vx = (rx - (existing.targetX !== undefined ? existing.targetX : existing.x)) / dt;
          vy = (ry - (existing.targetY !== undefined ? existing.targetY : existing.y)) / dt;
        }
      }
      if (!this.positions[num]) {
        this.positions[num] = { x: rx, y: ry, curX: rx, curY: ry, targetX: rx, targetY: ry, vx: 0, vy: 0, lastGpsTime: now };
      } else {
        const p = this.positions[num];
        p.targetX = rx;
        p.targetY = ry;
        p.x = rx;
        p.y = ry;
        if (Math.hypot(vx, vy) < 250) {
          p.vx = vx;
          p.vy = vy;
        }
        p.lastGpsTime = now;
      }
    }
  }

  _setDriverPos(num, x, y, snapImmediate) {
    if (!this.positions[num]) {
      this.positions[num] = { x, y, curX: x, curY: y };
    } else {
      this.positions[num].x = x;
      this.positions[num].y = y;
      if (!this.hasLiveGps) {
        // In modalità cinematica, (x, y) è già calcolata a 60fps con continuità perfetta lungo l'asfalto:
        // sincronizzazione diretta per azzerare lag 2D, tagli di curva e false accelerazioni
        this.positions[num].curX = x;
        this.positions[num].curY = y;
      } else if (snapImmediate || !Number.isFinite(this.positions[num].curX) || !Number.isFinite(this.positions[num].curY)) {
        this.positions[num].curX = x;
        this.positions[num].curY = y;
      }
    }
  }

  /**
   * Sincronizza lo stato cinematico di ciascun pilota con i mini-settori (Segments),
   * i distacchi live (GAP/INT) e lo stato InPit/PitOut provenienti da TimingData.
   */
  updatePositionsFromLeaderboard(leaderboard) {
    if (this.hasLiveGps || !Array.isArray(leaderboard) || leaderboard.length === 0) return;
    if (this.circuitPoints.length === 0) {
      this._buildFallbackCircuit();
    }

    const totalLen = this.trackLengthMeters;
    const refLap = this.referenceLapSec || this.simBaseLapSec || 94;

    const parseDeltaSec = (str) => {
      if (!str || typeof str !== 'string') return null;
      const c = str.trim().toUpperCase();
      if (!c || c === '-' || c === '--') return null;
      if (c === 'LEADER' || c === '0' || c === '0.000' || c === '+0.000') return 0;
      if (c.includes('LAP') || c.includes('L')) return null;
      const v = parseFloat(c.replace('+', '').replace('S', ''));
      return Number.isFinite(v) && v >= 0 ? v : null;
    };

    const circularTauDiff = (a, b) => {
      let d = (a - b) % refLap;
      if (d > refLap * 0.5) d -= refLap;
      if (d < -refLap * 0.5) d += refLap;
      return d;
    };

    let runningGapSec = 0;

    leaderboard.forEach((row, idx) => {
      const num = String(row.racingNumber);
      if (!num) return;

      const segInfo = this._computeExactMiniSectorAnchor(row.sectors);
      const lapSec = this._parseLapSeconds(row.bestLapTime) || this._parseLapSeconds(row.lastLapTime) || refLap;
      const isInPit = Boolean(row.inPit || row.status === 'PIT' || row.retired || row.status === 'RET');
      const isPitOut = Boolean(row.pitOut || row.status === 'OUT');

      const directGap = idx === 0 ? 0 : parseDeltaSec(row.gapToLeader);
      const intGap = parseDeltaSec(row.intervalToAhead);
      if (idx === 0) {
        runningGapSec = 0;
      } else if (directGap !== null) {
        runningGapSec = directGap;
      } else if (intGap !== null && runningGapSec !== null) {
        runningGapSec += intGap;
      } else {
        runningGapSec = null;
      }

      if (!this.driverLapState[num]) {
        const initTau = segInfo.segStartTau !== null
          ? segInfo.segStartTau + Math.max(0, (segInfo.segEndTau - segInfo.segStartTau) * 0.5)
          : ((1 - idx * 0.025) % 1 + 1) % 1 * refLap;
        const initSample = this._sampleTrackPointAtTau(initTau);
        this.driverLapState[num] = {
          tauSec: initTau,
          distMeters: initSample.distMeters,
          lastSegKey: segInfo.segKey,
          currentMiniSector: segInfo.currentMiniSector,
          segStartTau: segInfo.segStartTau,
          segEndTau: segInfo.segEndTau,
          onTrackGapSec: runningGapSec,
          intervalSec: intGap,
          lapSec,
          inPit: isInPit,
          pitOut: isPitOut,
          retired: Boolean(row.retired || row.status === 'RET'),
          gridIndex: idx
        };
      }

      const st = this.driverLapState[num];
      st.lapSec = lapSec;
      st.inPit = isInPit;
      st.pitOut = isPitOut;
      st.retired = Boolean(row.retired || row.status === 'RET');
      st.gridIndex = idx;
      st.onTrackGapSec = runningGapSec;
      st.intervalSec = intGap;
      st.currentMiniSector = segInfo.currentMiniSector;
      st.segStartTau = segInfo.segStartTau;
      st.segEndTau = segInfo.segEndTau;

      if (isInPit) {
        st.targetMeters = (idx * 14) % Math.min(260, totalLen * 0.05);
        if (!Number.isFinite(st.distMeters)) {
          st.distMeters = st.targetMeters;
          st.tauSec = (st.distMeters / Math.max(1, totalLen)) * refLap;
        }
        return;
      }

      if (segInfo.segStartTau !== null) {
        const segSpan = Math.max(0.5, (segInfo.segEndTau - segInfo.segStartTau + refLap) % refLap || 4.0);
        if (segInfo.segKey !== st.lastSegKey) {
          st.lastSegKey = segInfo.segKey;
          st.segMaxRel = segSpan * 0.95;
          if (!Number.isFinite(st.tauSec)) {
            st.tauSec = (segInfo.segStartTau + segSpan * 0.1) % refLap;
          }
        }
      }
    });

    // Distribuisce i piloti che condividono lo stesso mini-settore in base ai distacchi reali (INT / GAP)
    // garantendo ordine rigoroso in pista e zero sovrapposizioni
    let i = 0;
    while (i < leaderboard.length) {
      const firstNum = String(leaderboard[i].racingNumber);
      const firstSt = this.driverLapState[firstNum];
      if (!firstSt || firstSt.inPit || firstSt.currentMiniSector === null || firstSt.segStartTau === null) {
        i++;
        continue;
      }

      const mSec = firstSt.currentMiniSector;
      const group = [firstSt];
      let j = i + 1;
      while (j < leaderboard.length) {
        const nextSt = this.driverLapState[String(leaderboard[j].racingNumber)];
        if (!nextSt || nextSt.inPit) {
          j++;
          continue;
        }
        if (nextSt.currentMiniSector === mSec) {
          group.push(nextSt);
          j++;
        } else {
          break;
        }
      }

      const segStart = firstSt.segStartTau;
      const segSpan = Math.max(0.6, (firstSt.segEndTau - segStart + refLap) % refLap || 4.0);

      if (group.length === 1) {
        firstSt.segMaxRel = segSpan * 0.95;
        firstSt.targetTau = null; // Pilota solitario: corre al proprio ritmo naturale di gara
      } else {
        // Calcola i distacchi cumulati dal primo pilota del gruppo nel mini-settore
        const relGaps = [0];
        let accGap = 0;
        for (let k = 1; k < group.length; k++) {
          const stepSec = group[k].intervalSec != null && group[k].intervalSec > 0
            ? Math.min(2.5, Math.max(0.18, group[k].intervalSec))
            : 0.45;
          accGap += stepSec;
          relGaps.push(accGap);
        }

        const totalGroupSpan = Math.max(0.3, accGap);
        const usableSpan = segSpan * 0.84;
        const scale = Math.min(1.0, usableSpan / totalGroupSpan);
        const neededSpan = totalGroupSpan * scale;

        // Il primo pilota del gruppo guida la fila nel mini-settore
        let leadRel = circularTauDiff(firstSt.tauSec, segStart);
        const minLeadRel = Math.min(segSpan * 0.92, neededSpan + segSpan * 0.08);
        leadRel = Math.max(minLeadRel, Math.min(segSpan * 0.95, leadRel));

        for (let k = 0; k < group.length; k++) {
          const st = group[k];
          const targetRel = Math.max(segSpan * 0.04, leadRel - relGaps[k] * scale);
          st.segMaxRel = targetRel;
          st.targetTau = (segStart + targetRel) % refLap;
          if (!Number.isFinite(st.tauSec)) {
            st.tauSec = st.targetTau;
          }
        }
      }

      i = j;
    }
  }

  /**
   * Avanza a 60fps le vetture lungo la polilinea del circuito seguendo il profilo fisico v(s)
   * (rallenta nelle staccate e nei tornanti, accelera sui rettilinei) con decelerazione asintotica dolce.
   */
  _stepKinematics(now) {
    const dtSec = Math.max(0.001, Math.min(0.05, (now - this.lastFrameTime) / 1000));
    this.lastFrameTime = now;

    if (this.hasLiveGps || this.circuitPoints.length === 0) return;

    const refLap = this.referenceLapSec || this.simBaseLapSec || 94;

    const circularTauDiff = (a, b) => {
      let d = (a - b) % refLap;
      if (d > refLap * 0.5) d -= refLap;
      if (d < -refLap * 0.5) d += refLap;
      return d;
    };

    for (const [num, st] of Object.entries(this.driverLapState)) {
      if (st.inPit) {
        if (st.targetMeters !== undefined) {
          const dDiff = st.targetMeters - (st.distMeters || 0);
          st.distMeters = (st.distMeters || 0) + dDiff * Math.min(1.0, dtSec * 3.0);
        }
        const pt = this._sampleTrackPointAtMeters(st.distMeters || 0);
        this._setDriverPos(num, pt.x, pt.y, false);
        continue;
      }

      if (!Number.isFinite(st.tauSec)) continue;

      const basePace = Math.max(0.85, Math.min(1.15, refLap / Math.max(65, Math.min(130, st.lapSec || refLap))));
      let paceFactor = basePace;

      // Correzione asintotica dolce per agganciare targetTau (mantenimento distacchi INT/GAP)
      if (st.targetTau !== null && st.targetTau !== undefined) {
        const tauErr = circularTauDiff(st.targetTau, st.tauSec);
        if (Math.abs(tauErr) > refLap * 0.35) {
          // Disallineamento macroscopico: riallinea
          st.tauSec = st.targetTau;
        } else {
          // Modulazione fluida della velocità limitata a [-20%, +20%]: ZERO scatti e ZERO accelerazioni brusche
          const paceCorrection = Math.max(-0.20, Math.min(0.20, tauErr * 0.35));
          paceFactor *= (1.0 + paceCorrection);
        }
      }

      // Decelerazione morbida e continua all'avvicinarsi del limite del mini-settore (ease-out coseno C1)
      if (st.segStartTau !== null && st.segEndTau !== null) {
        const segSpan = Math.max(0.5, (st.segEndTau - st.segStartTau + refLap) % refLap || 4.0);
        const maxRel = st.segMaxRel !== undefined ? st.segMaxRel : (segSpan * 0.95);
        let rel = circularTauDiff(st.tauSec, st.segStartTau);
        if (rel >= maxRel * 0.80) {
          const ratio = Math.min(1.0, (rel - maxRel * 0.80) / Math.max(0.08, maxRel * 0.20));
          const easeOut = 1.0 - 0.60 * (0.5 - 0.5 * Math.cos(ratio * Math.PI));
          paceFactor *= easeOut;
          if (rel > maxRel) {
            const over = rel - maxRel;
            paceFactor *= Math.max(0.20, Math.exp(-over / 0.5));
          }
        }
      }

      if (st.pitOut && st.distMeters < 320) {
        paceFactor = Math.min(paceFactor, 0.35); // Limitatore corsia box
      }

      st.tauSec = (st.tauSec + dtSec * paceFactor + refLap) % refLap;
      const newPt = this._sampleTrackPointAtTau(st.tauSec);
      st.distMeters = newPt.distMeters;
      this._setDriverPos(num, newPt.x, newPt.y, false);
    }
  }

  _sampleTrackPointAtTau(tauSec) {
    const pts = this.circuitPoints;
    const n = pts.length;
    if (n === 0) return { x: 0, y: 0, v: 60, speedKmh: 216, longG: 0, kappa: 0, curvature: 0, idx: 0, distMeters: 0, tauSec: 0 };

    const refLap = this.referenceLapSec || this.simBaseLapSec || 94;
    const t = ((tauSec % refLap) + refLap) % refLap;

    let lo = 0;
    let hi = n - 1;
    while (lo < hi) {
      const mid = (lo + hi + 1) >> 1;
      if ((pts[mid].tauSec || 0) <= t) lo = mid;
      else hi = mid - 1;
    }

    const i0 = lo;
    const i1 = (i0 + 1) % n;
    const p0 = pts[i0];
    const p1 = pts[i1];
    const t0 = p0.tauSec || 0;
    const t1 = i1 === 0 ? refLap : (p1.tauSec || refLap);
    const spanT = Math.max(0.005, t1 - t0);
    const frac = Math.max(0, Math.min(1, (t - t0) / spanT));

    const d0 = p0.distMeters || 0;
    const d1 = i1 === 0 ? this.trackLengthMeters : (p1.distMeters || d0);

    return {
      x: p0.x + (p1.x - p0.x) * frac,
      y: p0.y + (p1.y - p0.y) * frac,
      v: p0.v + (p1.v - p0.v) * frac,
      speedKmh: p0.speedKmh + (p1.speedKmh - p0.speedKmh) * frac,
      longG: p0.longG + (p1.longG - p0.longG) * frac,
      kappa: p0.kappa + (p1.kappa - p0.kappa) * frac,
      curvature: p0.curvature + (p1.curvature - p0.curvature) * frac,
      idx: i0,
      distMeters: d0 + (d1 - d0) * frac,
      tauSec: t
    };
  }

  _sampleTrackPointAtMeters(distMeters) {
    const pts = this.circuitPoints;
    const n = pts.length;
    if (n === 0) return { x: 0, y: 0, v: 60, speedKmh: 216, longG: 0, kappa: 0, curvature: 0, idx: 0 };

    const totalLen = this.trackLengthMeters;
    const d = ((distMeters % totalLen) + totalLen) % totalLen;

    // Ricerca binaria sul profilo metrico cumulato
    let lo = 0;
    let hi = n - 1;
    while (lo < hi) {
      const mid = (lo + hi + 1) >> 1;
      if (pts[mid].distMeters <= d) lo = mid;
      else hi = mid - 1;
    }

    const i0 = lo;
    const i1 = (i0 + 1) % n;
    const p0 = pts[i0];
    const p1 = pts[i1];
    const segLen = Math.max(0.1, p0.ds || 1);
    const frac = Math.max(0, Math.min(1, (d - p0.distMeters) / segLen));

    return {
      x: p0.x + (p1.x - p0.x) * frac,
      y: p0.y + (p1.y - p0.y) * frac,
      v: p0.v + (p1.v - p0.v) * frac,
      speedKmh: p0.speedKmh + (p1.speedKmh - p0.speedKmh) * frac,
      longG: p0.longG + (p1.longG - p0.longG) * frac,
      kappa: p0.kappa + (p1.kappa - p0.kappa) * frac,
      curvature: p0.curvature + (p1.curvature - p0.curvature) * frac,
      idx: i0,
      distMeters: d
    };
  }

  /**
   * Restituisce le metriche fisiche esatte nel punto del circuito in cui si trova la vettura
   * per garantire corrispondenza 1:1 tra mappa pista e cruscotto telemetrico.
   */
  getDriverTrackMetrics(driverNum) {
    const num = String(driverNum);
    const st = this.driverLapState[num];
    if (!st || this.circuitPoints.length === 0) {
      return {
        progress: 0,
        distMeters: 0,
        curvature: 0,
        kappa: 0,
        physicsSpeedKmh: 0,
        longG: 0,
        isBrakingZone: false,
        isOnStraight: true
      };
    }

    const sample = this._sampleTrackPointAtMeters(st.distMeters);
    const progress = sample.distMeters / Math.max(1, this.trackLengthMeters);

    return {
      progress,
      distMeters: sample.distMeters,
      curvature: sample.curvature,
      kappa: sample.kappa,
      physicsSpeedKmh: sample.speedKmh,
      longG: sample.longG,
      isBrakingZone: sample.longG < -0.35,
      isOnStraight: sample.kappa < 0.0022 && sample.longG >= -0.25
    };
  }

  /**
   * Determina il mini-settore attivo reale del pilota tenendo conto del settore corrente (S1, S2 o S3)
   * ed evitando salti al passaggio sul traguardo quando i segmenti del giro precedente non sono ancora azzerati.
   */
  _computeExactMiniSectorAnchor(sectors) {
    if (!sectors || typeof sectors !== 'object' || this.circuitPoints.length === 0) {
      return { currentMiniSector: null, segStartTau: null, segEndTau: null, anchorDistMeters: null, segKey: null };
    }

    const s0 = sectors['0'] || sectors[0] || {};
    const s1 = sectors['1'] || sectors[1] || {};
    const s2 = sectors['2'] || sectors[2] || {};

    const segs0 = s0.Segments ? (Array.isArray(s0.Segments) ? s0.Segments : Object.values(s0.Segments)) : [];
    const segs1 = s1.Segments ? (Array.isArray(s1.Segments) ? s1.Segments : Object.values(s1.Segments)) : [];
    const segs2 = s2.Segments ? (Array.isArray(s2.Segments) ? s2.Segments : Object.values(s2.Segments)) : [];

    const n0 = segs0.length;
    const n1 = segs1.length;
    const n2 = segs2.length;
    const totalSegs = n0 + n1 + n2;
    if (totalSegs === 0) {
      return { currentMiniSector: null, segStartTau: null, segEndTau: null, anchorDistMeters: null, segKey: null };
    }

    const isSegDone = (seg) => Boolean(seg && Number(seg.Status) > 0 && Number(seg.Status) !== 2064);
    const countContigDone = (arr) => {
      let c = 0;
      for (let i = 0; i < arr.length; i++) {
        if (isSegDone(arr[i])) c++;
        else break;
      }
      return c;
    };

    const done0 = countContigDone(segs0);
    const done1 = countContigDone(segs1);
    const done2 = countContigDone(segs2);

    const hasVal0 = Boolean(s0.Value && String(s0.Value).trim());
    const hasVal1 = Boolean(s1.Value && String(s1.Value).trim());
    const hasVal2 = Boolean(s2.Value && String(s2.Value).trim());

    if (done0 === 0 && done1 === 0 && done2 === 0 && !hasVal0 && !hasVal1 && !hasVal2) {
      return { currentMiniSector: null, segStartTau: null, segEndTau: null, anchorDistMeters: null, segKey: null };
    }

    let currentMiniSector = 0;

    if (done0 < n0) {
      // Pilota attualmente nel Settore 1 (in F1 SignalR s0/s1/s2.Value restano valorizzati col giro precedente durante S1)
      currentMiniSector = done0;
    } else if (done1 < n1) {
      // Settore 1 completato (done0 == n0), pilota attualmente nel Settore 2
      currentMiniSector = n0 + done1;
    } else if (done2 < n2) {
      // Settori 1 e 2 completati (done0 == n0, done1 == n1), pilota attualmente nel Settore 3
      currentMiniSector = n0 + n1 + done2;
    } else {
      // Tutti i segmenti completati (done0 == n0, done1 == n1, done2 == n2):
      // la vettura ha appena tagliato il traguardo ed è nel primo mini-settore (0) del nuovo giro
      currentMiniSector = 0;
    }

    currentMiniSector = Math.max(0, Math.min(totalSegs - 1, currentMiniSector));

    const nPts = this.circuitPoints.length;
    const refLap = this.referenceLapSec || this.simBaseLapSec || 94;

    let startPtIdx = 0;
    if (currentMiniSector === 0) {
      startPtIdx = 0;
    } else if (this.miniSectorsIndexes.length > 0) {
      const mIdx = Math.min(currentMiniSector - 1, this.miniSectorsIndexes.length - 1);
      startPtIdx = Math.max(0, Math.min(nPts - 1, this.miniSectorsIndexes[mIdx]));
    } else {
      startPtIdx = Math.min(nPts - 1, Math.floor((currentMiniSector / totalSegs) * nPts));
    }

    let endPtIdx = nPts - 1;
    if (currentMiniSector >= totalSegs - 1) {
      endPtIdx = nPts - 1;
    } else if (this.miniSectorsIndexes.length > currentMiniSector) {
      endPtIdx = Math.max(startPtIdx + 1, Math.min(nPts - 1, this.miniSectorsIndexes[currentMiniSector]));
    } else {
      endPtIdx = Math.min(nPts - 1, Math.floor(((currentMiniSector + 1) / totalSegs) * nPts));
    }

    const segStartTau = this.circuitPoints[startPtIdx].tauSec || 0;
    const segEndTau = currentMiniSector >= totalSegs - 1
      ? refLap
      : Math.max(segStartTau + 0.5, this.circuitPoints[endPtIdx].tauSec || refLap);
    const anchorDistMeters = this.circuitPoints[startPtIdx].distMeters || 0;

    return {
      currentMiniSector,
      segStartTau,
      segEndTau,
      anchorDistMeters,
      segKey: `${currentMiniSector}/${totalSegs}`
    };
  }

  _parseLapSeconds(str) {
    if (!str || typeof str !== 'string') return 0;
    const parts = str.trim().split(':');
    if (parts.length === 2) {
      const m = parseFloat(parts[0]);
      const s = parseFloat(parts[1]);
      if (!isNaN(m) && !isNaN(s)) return m * 60 + s;
    }
    const v = parseFloat(str);
    return !isNaN(v) && v > 0 ? v : 0;
  }

  setSelectedDriver(driverNum) {
    this.selectedDriver = String(driverNum);
  }

  stop() {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
      this.animationId = null;
    }
  }

  startRendering() {
    const loop = (now) => {
      this._stepKinematics(now || performance.now());
      this.render();
      this.animationId = requestAnimationFrame(loop);
    };
    this.animationId = requestAnimationFrame(loop);
  }

  _drawTrackImage(ctx, w, h) {
    if (!this.trackImage || !this.trackImage.complete || !this.trackImage.naturalWidth) return false;
    const imgW = this.trackImage.naturalWidth;
    const imgH = this.trackImage.naturalHeight;
    const pad = 24;
    const scale = Math.min((w - pad * 2) / imgW, (h - pad * 2) / imgH);
    const drawW = imgW * scale;
    const drawH = imgH * scale;
    const drawX = (w - drawW) / 2;
    const drawY = (h - drawH) / 2;
    ctx.drawImage(this.trackImage, drawX, drawY, drawW, drawH);
    return true;
  }

  render() {
    const ctx = this.ctx;
    const w = this.width;
    const h = this.height;
    if (!w || !h) return;

    ctx.clearRect(0, 0, w, h);

    if (!this.bounds.isCalibrated) {
      if (this.trackImage) {
        this._drawTrackImage(ctx, w, h);
      } else {
        ctx.fillStyle = '#64748b';
        ctx.font = '12px sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('CARICAMENTO MAPPA CIRCUITO IN CORSO...', w / 2, h / 2);
      }
      return;
    }

    const padding = 38;
    const spanX = Math.max(1, this.bounds.maxX - this.bounds.minX);
    const spanY = Math.max(1, this.bounds.maxY - this.bounds.minY);

    const scaleX = (w - padding * 2) / spanX;
    const scaleY = (h - padding * 2) / spanY;
    const scale = Math.min(scaleX, scaleY);

    const midX = (this.bounds.minX + this.bounds.maxX) / 2;
    const midY = (this.bounds.minY + this.bounds.maxY) / 2;

    const toScreenX = (x) => (w / 2) + (x - midX) * scale;
    // Inverti asse Y cartesiano in coordinate schermo
    const toScreenY = (y) => (h / 2) - (y - midY) * scale;

    // 1. Disegna layout tracciato
    if (this.trackImage && !this.hasLiveGps && this.circuitPoints.length === 0) {
      this._drawTrackImage(ctx, w, h);
    } else {
      const pathPoints = this.circuitPoints.length > 10 ? this.circuitPoints : this._getLongestTrail();

      if (pathPoints.length > 10) {
        ctx.beginPath();
        for (let i = 0; i < pathPoints.length; i++) {
          const pt = pathPoints[i];
          const sx = toScreenX(pt.x);
          const sy = toScreenY(pt.y);
          if (i === 0) ctx.moveTo(sx, sy);
          else ctx.lineTo(sx, sy);
        }
        if (this.circuitPoints.length > 10) ctx.closePath();

        // Bordo esterno pista
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 14;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        ctx.stroke();

        // Asfalto interno
        ctx.strokeStyle = '#0f172a';
        ctx.lineWidth = 8;
        ctx.stroke();

        // Linea del traguardo (Start / Finish)
        const sf = pathPoints[0];
        const sfX = toScreenX(sf.x);
        const sfY = toScreenY(sf.y);
        ctx.beginPath();
        ctx.arc(sfX, sfY, 5, 0, 2 * Math.PI);
        ctx.fillStyle = '#e10600';
        ctx.fill();

        // Numeri delle curve ufficiali
        if (this.circuitCorners.length > 0) {
          ctx.fillStyle = '#64748b';
          ctx.font = 'bold 9px monospace';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          for (const corner of this.circuitCorners) {
            const cx = toScreenX(corner.x);
            const cy = toScreenY(corner.y);
            ctx.fillText(String(corner.number), cx, cy);
          }
        }
      }
    }

    // 2. Rendering delle Monoposto (il pilota selezionato viene disegnato per ultimo così è sempre in primo piano)
    const entries = Object.entries(this.positions).sort((a, b) => {
      if (a[0] === this.selectedDriver) return 1;
      if (b[0] === this.selectedDriver) return -1;
      return 0;
    });

    const now = performance.now();
    const renderDt = Math.max(0.001, Math.min(0.05, (now - (this.lastRenderTime || now)) / 1000));
    this.lastRenderTime = now;
    const smoothAlpha = 1.0 - Math.exp(-18.0 * renderDt);
    const wrapThreshold = Math.max(spanX, spanY) * 0.25;

    for (const [num, pos] of entries) {
      if (!Number.isFinite(pos.x) || !Number.isFinite(pos.y)) continue;

      if (!this.hasLiveGps) {
        // In modalità cinematica spline di pista, (pos.x, pos.y) è già campionata a 60fps lungo l'asfalto
        pos.curX = pos.x;
        pos.curY = pos.y;
      } else {
        if (!Number.isFinite(pos.curX) || !Number.isFinite(pos.curY)) {
          pos.curX = pos.targetX !== undefined ? pos.targetX : pos.x;
          pos.curY = pos.targetY !== undefined ? pos.targetY : pos.y;
        } else {
          const targetX = pos.targetX !== undefined ? pos.targetX : pos.x;
          const targetY = pos.targetY !== undefined ? pos.targetY : pos.y;
          if (pos.vx && pos.vy) {
            pos.curX += pos.vx * renderDt;
            pos.curY += pos.vy * renderDt;
          }
          const dx = targetX - pos.curX;
          const dy = targetY - pos.curY;
          const d = Math.hypot(dx, dy);
          if (d > wrapThreshold) {
            pos.curX = targetX;
            pos.curY = targetY;
          } else {
            const blendAlpha = 1.0 - Math.exp(-14.0 * renderDt);
            pos.curX += dx * blendAlpha;
            pos.curY += dy * blendAlpha;
          }
        }
      }

      const sx = toScreenX(pos.curX);
      const sy = toScreenY(pos.curY);

      if (!Number.isFinite(sx) || !Number.isFinite(sy)) continue;

      pos.screenX = sx;
      pos.screenY = sy;

      const driver = this.drivers[num] || {};
      const teamColor = this._resolveDriverColor(num);
      const isSelected = this.selectedDriver === num;

      // Alone pilota selezionato
      if (isSelected) {
        ctx.beginPath();
        ctx.arc(sx, sy, 14, 0, 2 * Math.PI);
        ctx.fillStyle = 'rgba(225, 6, 0, 0.35)';
        ctx.fill();
        ctx.strokeStyle = '#e10600';
        ctx.lineWidth = 2;
        ctx.stroke();
      }

      // Vettura
      ctx.beginPath();
      ctx.arc(sx, sy, 6.5, 0, 2 * Math.PI);
      ctx.fillStyle = teamColor;
      ctx.fill();
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 1.5;
      ctx.stroke();

      // Etichetta TLA
      const tla = driver.Tla || driver.tla || num;
      ctx.fillStyle = 'rgba(15, 23, 42, 0.85)';
      ctx.fillRect(sx + 8, sy - 7, 24, 13);
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 8px monospace';
      ctx.textAlign = 'left';
      ctx.textBaseline = 'middle';
      ctx.fillText(tla, sx + 10, sy);
    }
  }

  _getLongestTrail() {
    let longestTrail = [];
    for (const trail of Object.values(this.driverTrails)) {
      if (trail.length > longestTrail.length) {
        longestTrail = trail;
      }
    }
    return longestTrail;
  }
}

window.TrackCanvas = TrackCanvas;
if (typeof module !== 'undefined' && module.exports) {
  module.exports = TrackCanvas;
}

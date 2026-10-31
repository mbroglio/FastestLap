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
    this.scaleToMeters = scaleToMeters;
    let cumMeters = 0;
    for (let i = 0; i < n; i++) {
      // Usa la distanza euclidea reale (senza gonfiare i segmenti <0.4m) per evitare rallentamenti spurii sui vertici ravvicinati
      pts[i].ds = Math.max(1e-5, pts[i].rawDs * scaleToMeters);
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
        if (step > 0) {
          acc += pts[curr].ds;
          curr = (curr + 1) % n;
        } else {
          curr = (curr - 1 + n) % n;
          acc += pts[curr].ds;
        }
        guard++;
      }
      return curr;
    };

    const maxVms = 328 / 3.6;

    // 1. Curvatura geometrica multi-scala (16m per tornanti, 32m per esse veloci) con filtro Gaussiano metrico
    const rawKappa = new Array(n);
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
      rawKappa[i] = Math.max(calcKappa(16), calcKappa(32));
    }

    const kernelOffsetsM = [-16, -8, 0, 8, 16];
    const kernelWeights = [0.12, 0.22, 0.32, 0.22, 0.12];
    for (let i = 0; i < n; i++) {
      let kappa = 0;
      for (let k = 0; k < kernelOffsetsM.length; k++) {
        const idxK = findIdxByOffsetMeters(i, kernelOffsetsM[k]);
        kappa += rawKappa[idxK] * kernelWeights[k];
      }
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
    for (let pass = 0; pass < 3; pass++) {
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
    for (let pass = 0; pass < 3; pass++) {
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

    // 4. Calcolo accelerazione longitudinale in G su finestra metrica (10m), velocità in km/h e tempo cumulato (tauSec)
    let simTimeSec = 0;
    for (let i = 0; i < n; i++) {
      const next = pts[(i + 1) % n];
      const iAhead = findIdxByOffsetMeters(i, 10);
      let distAhead = (pts[iAhead].distMeters - pts[i].distMeters + this.trackLengthMeters) % this.trackLengthMeters;
      if (distAhead < 1.0) distAhead = Math.max(1.0, pts[i].ds);
      pts[i].speedKmh = pts[i].v * 3.6;
      pts[i].longG = ((pts[iAhead].v * pts[iAhead].v - pts[i].v * pts[i].v) / (2 * distAhead)) / 9.81;
      pts[i].rawTauSec = simTimeSec;
      const vSegAvg = 0.5 * (pts[i].v + next.v);
      simTimeSec += pts[i].ds / Math.max(15, vSegAvg);
    }
    this.simBaseLapSec = Math.max(60, simTimeSec);
    // Evita che un candidateLap lento/bagnato di Multiviewer dilati il riferimento oltre il +12% del modello fisico
    if (this.referenceLapSec && this.referenceLapSec > this.simBaseLapSec * 1.12) {
      this.referenceLapSec = Number(this.simBaseLapSec.toFixed(3));
    }
    const refLap = this.referenceLapSec || Number(this.simBaseLapSec.toFixed(3));
    this.referenceLapSec = refLap;
    const tauScale = refLap / Math.max(1, simTimeSec);
    for (let i = 0; i < n; i++) {
      pts[i].tauSec = pts[i].rawTauSec * tauScale;
      pts[i].v = pts[i].v / tauScale;
      pts[i].speedKmh = pts[i].v * 3.6;
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

    // Trova il miglior riferimento cronometrico reale nella sessione corrente
    let sessionBestLapSec = Infinity;
    for (const r of leaderboard) {
      const b = this._parseLapSeconds(r.bestLapTime) || this._parseLapSeconds(r.lastLapTime);
      if (b > 55 && b < 135 && b < sessionBestLapSec) {
        sessionBestLapSec = b;
      }
    }
    const defaultLapSec = Number.isFinite(sessionBestLapSec) ? sessionBestLapSec : refLap;

    let runningGapSec = 0;
    const initSegCounts = {};

    leaderboard.forEach((row, idx) => {
      const num = String(row.racingNumber);
      if (!num) return;

      const segInfo = this._computeExactMiniSectorAnchor(row.sectors);
      const lapSec = this._parseLapSeconds(row.bestLapTime) || this._parseLapSeconds(row.lastLapTime) || defaultLapSec;
      const isInPit = Boolean(row.inPit || row.status === 'PIT' || row.retired || row.status === 'RET');
      const isPitOut = Boolean(row.pitOut || row.status === 'OUT');
      const basePace = Math.max(0.85, Math.min(1.25, refLap / Math.max(60, Math.min(130, lapSec || refLap))));

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
        const hasSeg = segInfo.segStartTau !== null;
        const drvNumInt = parseInt(num, 10) || (idx + 1);
        const pitSlotMeters = (drvNumInt * 11) % Math.min(220, totalLen * 0.04);
        const segSpan = hasSeg
          ? Math.max(0.5, (segInfo.segEndTau - segInfo.segStartTau + refLap) % refLap || 4.0)
          : 4.0;
        const mKey = String(segInfo.currentMiniSector ?? 'none');
        const sameSegIdx = initSegCounts[mKey] || 0;
        initSegCounts[mKey] = sameSegIdx + 1;
        const initFrac = Math.max(0.06, 0.28 - sameSegIdx * 0.07);

        const initTau = isInPit
          ? (pitSlotMeters / Math.max(1, totalLen)) * refLap
          : (hasSeg
            ? (segInfo.segStartTau + segSpan * initFrac) % refLap
            : ((1 - idx * 0.025) % 1 + 1) % 1 * refLap);
        const initSample = this._sampleTrackPointAtTau(initTau);
        this.driverLapState[num] = {
          tauSec: initTau,
          distMeters: isInPit ? pitSlotMeters : initSample.distMeters,
          lastSegKey: segInfo.segKey,
          prevMiniSector: segInfo.currentMiniSector,
          prevSegSpan: segSpan,
          elapsedInSegSec: hasSeg ? (segSpan * initFrac) / basePace : 0,
          hasCompletedFirstSegTransition: false,
          livePace: basePace,
          smoothPace: basePace,
          currentMiniSector: segInfo.currentMiniSector,
          totalMiniSectors: segInfo.totalSegs || 22,
          segStartTau: segInfo.segStartTau,
          segEndTau: segInfo.segEndTau,
          hasValidAnchor: hasSeg && !isInPit,
          wasInPit: isInPit,
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
      st.totalMiniSectors = segInfo.totalSegs || st.totalMiniSectors || 22;
      st.segStartTau = segInfo.segStartTau;
      st.segEndTau = segInfo.segEndTau;

      if (isInPit) {
        const drvNumInt = parseInt(num, 10) || (idx + 1);
        st.targetMeters = (drvNumInt * 11) % Math.min(220, totalLen * 0.04);
        st.wasInPit = true;
        st.targetTau = null;
        if (!Number.isFinite(st.distMeters)) {
          st.distMeters = st.targetMeters;
        }
        st.tauSec = (st.distMeters / Math.max(1, totalLen)) * refLap;
        st.elapsedInSegSec = 0;
        return;
      }

      if (st.wasInPit) {
        st.wasInPit = false;
        st.tauSec = ((st.distMeters || 0) / Math.max(1, totalLen)) * refLap;
        st.hasValidAnchor = true;
        st.hasCompletedFirstSegTransition = false;
        st.elapsedInSegSec = 0;
        st.livePace = basePace;
        st.smoothPace = isPitOut && (st.distMeters || 0) < 280 ? 0.42 : basePace;
      }

      if (segInfo.segStartTau !== null) {
        const segSpan = Math.max(0.5, (segInfo.segEndTau - segInfo.segStartTau + refLap) % refLap || 4.0);
        if (!st.hasValidAnchor || !Number.isFinite(st.tauSec)) {
          st.tauSec = (segInfo.segStartTau + segSpan * 0.25) % refLap;
          st.elapsedInSegSec = (segSpan * 0.25) / basePace;
          st.hasCompletedFirstSegTransition = false;
          st.livePace = basePace;
          st.smoothPace = basePace;
          st.hasValidAnchor = true;
        }
        if (segInfo.segKey !== st.lastSegKey) {
          const totS = st.totalMiniSectors || 22;
          const isNextSeg = st.prevMiniSector !== null &&
            st.prevMiniSector !== undefined &&
            ((segInfo.currentMiniSector - st.prevMiniSector + totS) % totS === 1);
          if (st.hasCompletedFirstSegTransition && isNextSeg && st.elapsedInSegSec > 0.6 && st.elapsedInSegSec < 25.0 && st.prevSegSpan > 0.4) {
            const measuredPace = Math.max(0.42, Math.min(1.30, st.prevSegSpan / st.elapsedInSegSec));
            if (measuredPace < basePace * 0.78) {
              // Giro lento / raffreddamento (cooldown): adatta il passo effettivo
              st.livePace = (st.livePace || basePace) * 0.50 + measuredPace * 0.50;
            } else {
              // Giro veloce normale: assorbi il jitter di rete mantenendo il passo nominale costante
              st.livePace = (st.livePace || basePace) * 0.40 + basePace * 0.60;
            }
          } else if (segInfo.currentMiniSector === 0) {
            st.livePace = basePace;
          }
          st.hasCompletedFirstSegTransition = true;
          st.lastSegKey = segInfo.segKey;
          st.prevMiniSector = segInfo.currentMiniSector;
          st.prevSegSpan = segSpan;
          st.elapsedInSegSec = 0;
        }
      }
    });
  }

  /**
   * Avanza a 60fps le vetture lungo la polilinea del circuito seguendo il profilo fisico v(s)
   * con anello ad aggancio di fase (PLL) continuo C2: zero frenate artificiali a metà rettilineo
   * e adattamento automatico ai giri di lancio/raffreddamento (cooldown).
   */
  _stepKinematics(now) {
    const dtSec = Math.max(0.001, Math.min(0.05, (now - this.lastFrameTime) / 1000));
    this.lastFrameTime = now;

    if (this.hasLiveGps || this.circuitPoints.length === 0) return;

    const totalLen = this.trackLengthMeters;
    const refLap = this.referenceLapSec || this.simBaseLapSec || 94;

    const circularTauDiff = (a, b) => {
      let d = (a - b) % refLap;
      if (d > refLap * 0.5) d -= refLap;
      if (d < -refLap * 0.5) d += refLap;
      return d;
    };

    const activeByMiniSector = {};
    for (const st of Object.values(this.driverLapState)) {
      if (!st || st.inPit || st.currentMiniSector === null || !Number.isFinite(st.tauSec)) continue;
      const mKey = String(st.currentMiniSector);
      if (!activeByMiniSector[mKey]) activeByMiniSector[mKey] = [];
      activeByMiniSector[mKey].push(st);
    }
    for (const group of Object.values(activeByMiniSector)) {
      if (group.length > 1) {
        const segStart = group[0].segStartTau || 0;
        group.sort((a, b) => circularTauDiff(b.tauSec, segStart) - circularTauDiff(a.tauSec, segStart));
      }
    }

    for (const [num, st] of Object.entries(this.driverLapState)) {
      if (st.inPit) {
        if (st.targetMeters !== undefined) {
          let dDiff = ((st.targetMeters - (st.distMeters || 0)) % totalLen + totalLen) % totalLen;
          if (dDiff > totalLen * 0.5) dDiff -= totalLen;
          if (Math.abs(dDiff) > 380) {
            st.distMeters = st.targetMeters;
          } else {
            st.distMeters = ((st.distMeters || 0) + dDiff * Math.min(1.0, dtSec * 3.0) + totalLen) % totalLen;
          }
        }
        st.tauSec = ((st.distMeters || 0) / Math.max(1, totalLen)) * refLap;
        st.smoothPace = 0;
        const pt = this._sampleTrackPointAtMeters(st.distMeters || 0);
        this._setDriverPos(num, pt.x, pt.y, false);
        continue;
      }

      if (!Number.isFinite(st.tauSec)) continue;

      const basePace = Math.max(0.85, Math.min(1.25, refLap / Math.max(60, Math.min(130, st.lapSec || refLap))));
      const activePace = st.livePace || basePace;
      let targetPace = activePace;

      if (st.segStartTau !== null && st.segEndTau !== null) {
        const segSpan = Math.max(0.5, (st.segEndTau - st.segStartTau + refLap) % refLap || 4.0);
        st.elapsedInSegSec = (st.elapsedInSegSec || 0) + dtSec;

        let expectedRel = st.elapsedInSegSec * activePace;
        let instantBasePace = activePace;

        // Se il pilota impiega molto più tempo del previsto nel mini-settore (>130%, es. giro di raffreddamento in FP),
        // riduce progressivamente la velocità attesa senza mai fermare la vettura né frenare nei giri veloci
        if (expectedRel > segSpan * 1.30) {
          const overTime = st.elapsedInSegSec - (segSpan * 1.30) / activePace;
          const decay = Math.exp(-overTime / Math.max(3.5, segSpan));
          instantBasePace = Math.max(0.35, activePace * decay);
          expectedRel = Math.min(segSpan * 1.85, segSpan * 1.30 + (1.0 - decay) * segSpan * 0.55);
          st.livePace = Math.max(0.45, activePace * (0.996 + 0.004 * decay));
        }

        const expectedTau = (st.segStartTau + expectedRel) % refLap;
        const phaseErr = circularTauDiff(expectedTau, st.tauSec);
        const rel = circularTauDiff(st.tauSec, st.segStartTau);

        if (Math.abs(rel) > refLap * 0.42 && Math.abs(phaseErr) > refLap * 0.42) {
          // Riallineamento solo in caso di disallineamento iniziale estremo (>42% del giro)
          st.tauSec = expectedTau;
          st.smoothPace = instantBasePace;
        } else {
          // Controllore Phase-Locked Loop (PLL) proporzionale a guadagno morbido:
          // assorbe il jitter di rete (±0.8s) distribuendo la correzione di fase su più mini-settori
          // senza mai causare accelerazioni o decelerazioni percepibili sui rettilinei
          const maxPull = Math.abs(phaseErr) > 2.5 ? 0.22 : 0.10;
          const maxBrake = Math.abs(phaseErr) > 2.5 ? -0.18 : -0.09;
          const gain = Math.abs(phaseErr) < 0.6 ? 0.06 : 0.10;
          const pllCorrection = Math.max(maxBrake, Math.min(maxPull, phaseErr * gain));
          targetPace = instantBasePace * (1.0 + pllCorrection);
        }

        // Prevenzione sovrapposizione visiva tra vetture nello stesso mini-settore (solo se < 0.26s dietro la vettura davanti)
        const group = activeByMiniSector[String(st.currentMiniSector)];
        if (group && group.length > 1) {
          const myIdx = group.indexOf(st);
          if (myIdx > 0) {
            const aheadSt = group[myIdx - 1];
            const gapAheadTau = circularTauDiff(aheadSt.tauSec, st.tauSec);
            if (gapAheadTau >= 0 && gapAheadTau < 0.26) {
              const bufferRatio = (0.26 - gapAheadTau) / 0.26;
              targetPace *= (1.0 - 0.14 * bufferRatio);
            }
          }
        }
      }

      if (st.pitOut && st.distMeters < 280) {
        targetPace = Math.min(targetPace, 0.42); // Limitatore corsia box in uscita pit
      }

      targetPace = Math.max(0.32, Math.min(1.65, targetPace));

      // Filtro passa-basso esponenziale sulla velocità cinematica per garantire continuità C2 (zero scatti d'accelerazione)
      const paceAlpha = 1.0 - Math.exp(-4.0 * dtSec);
      const prevSmooth = Number.isFinite(st.smoothPace) && st.smoothPace > 0 ? st.smoothPace : targetPace;
      st.smoothPace = prevSmooth + (targetPace - prevSmooth) * paceAlpha;
      st.smoothPace = Math.max(0.32, Math.min(1.65, st.smoothPace));

      st.tauSec = (st.tauSec + dtSec * st.smoothPace + refLap) % refLap;
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
    const spanT = Math.max(1e-6, t1 - t0);
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
    const segLen = Math.max(1e-5, p0.ds || 1e-5);
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
    if (this.circuitPoints.length === 0) {
      return {
        progress: 0,
        distMeters: 0,
        curvature: 0,
        kappa: 0,
        physicsSpeedKmh: 0,
        effectivePace: 1.0,
        longG: 0,
        isBrakingZone: false,
        isOnStraight: true
      };
    }

    let distMeters = st ? st.distMeters : undefined;
    if (!Number.isFinite(distMeters)) {
      const livePos = this.positions[num];
      if (livePos && Number.isFinite(livePos.curX) && Number.isFinite(livePos.curY)) {
        let bestIdx = 0;
        let bestD2 = Infinity;
        for (let i = 0; i < this.circuitPoints.length; i++) {
          const dx = this.circuitPoints[i].x - livePos.curX;
          const dy = this.circuitPoints[i].y - livePos.curY;
          const d2 = dx * dx + dy * dy;
          if (d2 < bestD2) {
            bestD2 = d2;
            bestIdx = i;
          }
        }
        distMeters = this.circuitPoints[bestIdx].distMeters || 0;
      } else {
        return {
          progress: 0,
          distMeters: 0,
          curvature: 0,
          kappa: 0,
          physicsSpeedKmh: 0,
          effectivePace: 1.0,
          longG: 0,
          isBrakingZone: false,
          isOnStraight: true
        };
      }
    }

    const sample = this._sampleTrackPointAtMeters(distMeters);
    const progress = sample.distMeters / Math.max(1, this.trackLengthMeters);
    const effectivePace = st ? (st.inPit ? 0 : (st.smoothPace || 1.0)) : 1.0;

    return {
      progress,
      distMeters: sample.distMeters,
      curvature: sample.curvature,
      kappa: sample.kappa,
      physicsSpeedKmh: sample.speedKmh,
      effectivePace,
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
      return { currentMiniSector: null, totalSegs: 0, segStartTau: null, segEndTau: null, anchorDistMeters: null, segKey: null };
    }

    const s0 = sectors['0'] || sectors[0] || {};
    const s1 = sectors['1'] || sectors[1] || {};
    const s2 = sectors['2'] || sectors[2] || {};

    const toSegArray = (segs) => {
      if (!segs) return [];
      if (Array.isArray(segs)) return segs;
      if (typeof segs === 'object') {
        const keys = Object.keys(segs)
          .filter(k => !Number.isNaN(Number(k)))
          .sort((a, b) => Number(a) - Number(b));
        return keys.map(k => segs[k]);
      }
      return [];
    };

    const segs0 = toSegArray(s0.Segments);
    const segs1 = toSegArray(s1.Segments);
    const segs2 = toSegArray(s2.Segments);

    const n0 = segs0.length;
    const n1 = segs1.length;
    const n2 = segs2.length;
    const totalSegs = n0 + n1 + n2;
    if (totalSegs === 0) {
      return { currentMiniSector: null, totalSegs: 0, segStartTau: null, segEndTau: null, anchorDistMeters: null, segKey: null };
    }

    // Include Status 2064 (pitlane exit/entry) and find highest active segment index per sector
    // so cars on Out-Laps / In-Laps or with a single missed segment packet never freeze at mini-sector 0
    const isSegDone = (seg) => Boolean(seg && Number(seg.Status) > 0);
    const countActiveDone = (arr) => {
      let lastDone = -1;
      for (let i = 0; i < arr.length; i++) {
        if (isSegDone(arr[i])) lastDone = i;
      }
      return lastDone + 1;
    };

    const done0 = countActiveDone(segs0);
    const done1 = countActiveDone(segs1);
    const done2 = countActiveDone(segs2);

    const hasVal0 = Boolean(s0.Value && String(s0.Value).trim());
    const hasVal1 = Boolean(s1.Value && String(s1.Value).trim());
    const hasVal2 = Boolean(s2.Value && String(s2.Value).trim());

    if (done0 === 0 && done1 === 0 && done2 === 0 && !hasVal0 && !hasVal1 && !hasVal2) {
      return { currentMiniSector: null, totalSegs, segStartTau: null, segEndTau: null, anchorDistMeters: null, segKey: null };
    }

    let currentMiniSector = 0;

    if (done2 > 0 && done2 < n2 && done1 === n1) {
      currentMiniSector = n0 + n1 + done2;
    } else if (done1 > 0 && done1 < n1 && (done0 === n0 || done2 === 0)) {
      currentMiniSector = n0 + done1;
    } else if (done0 < n0) {
      // Pilota attualmente nel Settore 1
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

    // Mappatura proporzionale esatta anche quando miniSectorsIndexes.length (es. 21 in Messico) != totalSegs (es. 22)
    const getBoundaryPtIdx = (segIdx) => {
      if (segIdx <= 0) return 0;
      if (segIdx >= totalSegs) return nPts - 1;
      if (this.miniSectorsIndexes.length > 0) {
        const bounds = this.miniSectorsIndexes[0] > 0
          ? [0, ...this.miniSectorsIndexes]
          : [...this.miniSectorsIndexes];
        const mIntervals = Math.max(1, bounds.length - 1);
        const pos = (segIdx / totalSegs) * mIntervals;
        const i0 = Math.min(mIntervals, Math.floor(pos));
        const i1 = Math.min(mIntervals, i0 + 1);
        const frac = pos - i0;
        const idx = Math.round(bounds[i0] + (bounds[i1] - bounds[i0]) * frac);
        return Math.max(0, Math.min(nPts - 1, idx));
      }
      return Math.min(nPts - 1, Math.floor((segIdx / totalSegs) * nPts));
    };

    const startPtIdx = getBoundaryPtIdx(currentMiniSector);
    const endPtIdx = Math.max(startPtIdx + 1, getBoundaryPtIdx(currentMiniSector + 1));

    const segStartTau = this.circuitPoints[startPtIdx].tauSec || 0;
    const segEndTau = currentMiniSector >= totalSegs - 1
      ? refLap
      : Math.max(segStartTau + 0.8, this.circuitPoints[Math.min(nPts - 1, endPtIdx)].tauSec || refLap);
    const anchorDistMeters = this.circuitPoints[startPtIdx].distMeters || 0;

    return {
      currentMiniSector,
      totalSegs,
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

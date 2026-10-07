/**
 * TrackMapAnalyzer.js
 * Generic Circuit Map & Telemetry Analysis Engine
 *
 * Provides:
 *  - 2D Affine Calibration: Fits GPS (x, y) coordinates to SVG/bitmap pixels (px, py)
 *    using closed-form least-squares regression (scale, rotation, translation).
 *  - Centerline Sampling & Curvature Profiling: Resamples raw track coordinates into
 *    N uniform nodes with tangent angles, curvature, cornering speeds, and time marks.
 *  - Sector Timing Beam Detection: Configures S/F line, Sector 1, and Sector 2 beams.
 *  - Pit Lane Geometry & Blending: Connects pit lane with main track using smooth
 *    C1-continuous transition curves to guarantee 0-jump re-entries.
 */

class TrackMapAnalyzer {
  /**
   * Fits an optimal 2D affine similarity transform mapping source (x, y) points
   * to target (px, py) image pixel coordinates using least-squares:
   *   px = a * x - b * y + tx
   *   py = b * x + a * y + ty
   * where scale = sqrt(a^2 + b^2), rotation = atan2(b, a)
   *
   * @param {Array<{x: number, y: number}>} srcPoints Source points (e.g. GPS decimeters)
   * @param {Array<{px: number, py: number}>} dstPoints Target points (e.g. image pixels)
   * @returns {Object} Transform parameters { scale, rotationDeg, tx, ty, a, b, rmse }
   */
  static fitAffineTransform(srcPoints, dstPoints) {
    if (!srcPoints || !dstPoints || srcPoints.length !== dstPoints.length || srcPoints.length < 2) {
      throw new Error('fitAffineTransform requires at least 2 matching point pairs');
    }

    const n = srcPoints.length;
    let sumX = 0, sumY = 0, sumU = 0, sumV = 0;
    for (let i = 0; i < n; i++) {
      sumX += srcPoints[i].x;
      sumY += srcPoints[i].y;
      sumU += dstPoints[i].px;
      sumV += dstPoints[i].py;
    }
    const meanX = sumX / n;
    const meanY = sumY / n;
    const meanU = sumU / n;
    const meanV = sumV / n;

    let numA = 0, numB = 0, den = 0;
    for (let i = 0; i < n; i++) {
      const dx = srcPoints[i].x - meanX;
      const dy = srcPoints[i].y - meanY;
      const du = dstPoints[i].px - meanU;
      const dv = dstPoints[i].py - meanV;

      numA += dx * du + dy * dv;
      numB += dx * dv - dy * du;
      den += dx * dx + dy * dy;
    }

    if (Math.abs(den) < 1e-12) {
      throw new Error('Degenerate point configuration for affine fit');
    }

    const a = numA / den;
    const b = numB / den;
    const tx = meanU - a * meanX + b * meanY;
    const ty = meanV - b * meanX - a * meanY;

    const scale = Math.sqrt(a * a + b * b);
    const rotationDeg = (Math.atan2(b, a) * 180) / Math.PI;

    // Calculate RMSE
    let sumSqErr = 0;
    for (let i = 0; i < n; i++) {
      const predPx = a * srcPoints[i].x - b * srcPoints[i].y + tx;
      const predPy = b * srcPoints[i].x + a * srcPoints[i].y + ty;
      const errX = predPx - dstPoints[i].px;
      const errY = predPy - dstPoints[i].py;
      sumSqErr += errX * errX + errY * errY;
    }
    const rmse = Math.sqrt(sumSqErr / n);

    return {
      scale,
      rotationDeg,
      tx,
      ty,
      a,
      b,
      rmse
    };
  }

  /**
   * Applies the affine transformation to a point (x, y)
   */
  static transformPoint(pt, transform) {
    const { a, b, tx, ty } = transform;
    return {
      px: a * pt.x - b * pt.y + tx,
      py: b * pt.x + a * pt.y + ty
    };
  }

  /**
   * Resamples raw centerline points into N smooth, uniform nodes with curvature,
   * heading angles, and realistic speed estimates.
   *
   * @param {Array<{px: number, py: number}>} rawPoints
   * @param {Object} options { targetCount: 450, totalLapDuration: 95.0, maxSpeed: 330, minSpeed: 75 }
   * @returns {Array<Object>} Processed track nodes
   */
  static sampleCenterline(rawPoints, options = {}) {
    if (!rawPoints || rawPoints.length < 3) {
      throw new Error('Centerline requires at least 3 points');
    }

    const targetCount = options.targetCount || 450;
    const totalLapDuration = options.totalLapDuration || 95.0;
    const maxSpeed = options.maxSpeed || 330;
    const minSpeed = options.minSpeed || 75;
    const trackLengthMeters = options.trackLengthMeters || (targetCount > 200 ? 5500 : 3500);

    // 1. Compute cumulative segment distances
    const cumDist = [0];
    for (let i = 0; i < rawPoints.length; i++) {
      const nextIdx = (i + 1) % rawPoints.length;
      const dx = rawPoints[nextIdx].px - rawPoints[i].px;
      const dy = rawPoints[nextIdx].py - rawPoints[i].py;
      const segLen = Math.hypot(dx, dy);
      cumDist.push(cumDist[cumDist.length - 1] + segLen);
    }
    const totalPixelLength = cumDist[cumDist.length - 1];

    // 2. Uniform distance resampling along total length
    const nodes = [];
    for (let i = 0; i < targetCount; i++) {
      const targetDist = (i / targetCount) * totalPixelLength;

      // Find segment
      let segIdx = 0;
      while (segIdx < cumDist.length - 1 && cumDist[segIdx + 1] < targetDist) {
        segIdx++;
      }
      const segStartDist = cumDist[segIdx];
      const segEndDist = cumDist[segIdx + 1];
      const segLen = segEndDist - segStartDist;
      const u = segLen > 0 ? (targetDist - segStartDist) / segLen : 0;

      const p1 = rawPoints[segIdx % rawPoints.length];
      const p2 = rawPoints[(segIdx + 1) % rawPoints.length];

      const px = p1.px + (p2.px - p1.px) * u;
      const py = p1.py + (p2.py - p1.py) * u;

      nodes.push({
        idx: i,
        px: Number(px.toFixed(2)),
        py: Number(py.toFixed(2)),
        distPx: Number(targetDist.toFixed(2)),
        progress: Number((i / targetCount).toFixed(4))
      });
    }

    const N = targetCount;

    // 3. Compute smoothed curvature and heading angle for each node
    for (let i = 0; i < N; i++) {
      const pPrev = nodes[(i - 2 + N) % N];
      const pCurr = nodes[i];
      const pNext = nodes[(i + 2) % N];

      const heading = Math.atan2(pNext.py - pPrev.py, pNext.px - pPrev.px);
      const a1 = Math.atan2(pCurr.py - pPrev.py, pCurr.px - pPrev.px);
      const a2 = Math.atan2(pNext.py - pCurr.py, pNext.px - pCurr.px);
      let dAngle = Math.abs(a2 - a1);
      if (dAngle > Math.PI) dAngle = 2 * Math.PI - dAngle;

      pCurr.heading = Number(heading.toFixed(4));
      pCurr.curvature = Number(dAngle.toFixed(4));
    }

    // 4. Physical segment distances in meters
    const dsMeters = new Float64Array(N);
    for (let i = 0; i < N; i++) {
      const next = nodes[(i + 1) % N];
      const dp = Math.hypot(next.px - nodes[i].px, next.py - nodes[i].py);
      dsMeters[i] = (dp / totalPixelLength) * trackLengthMeters;
    }

    // 5. Target corner speeds based on curvature (grip limit at apex)
    const targetSpeeds = new Float64Array(N); // m/s
    for (let i = 0; i < N; i++) {
      const c = nodes[i].curvature;
      let spdKmh;
      if (c > 0.8) spdKmh = minSpeed;
      else if (c > 0.5) spdKmh = minSpeed + (maxSpeed - minSpeed) * 0.12;
      else if (c > 0.25) spdKmh = minSpeed + (maxSpeed - minSpeed) * 0.32;
      else if (c > 0.12) spdKmh = minSpeed + (maxSpeed - minSpeed) * 0.60;
      else spdKmh = maxSpeed;
      targetSpeeds[i] = spdKmh / 3.6;
    }

    // 6. Backward braking pass (propagates braking zones backwards from apexes)
    const speeds = new Float64Array(targetSpeeds);
    const maxBrakeDecel = 40.0; // m/s^2 (~4.0G)
    for (let pass = 0; pass < 2; pass++) {
      for (let i = N - 1; i >= 0; i--) {
        const nextIdx = (i + 1) % N;
        const maxAllowed = Math.sqrt(speeds[nextIdx] * speeds[nextIdx] + 2 * maxBrakeDecel * dsMeters[i]);
        if (speeds[i] > maxAllowed) speeds[i] = maxAllowed;
      }
    }

    // 7. Forward acceleration pass (propagates traction acceleration out of corners)
    const maxAccel = 11.5; // m/s^2 (~1.15G)
    for (let pass = 0; pass < 2; pass++) {
      for (let i = 0; i < N; i++) {
        const nextIdx = (i + 1) % N;
        const maxAllowed = Math.sqrt(speeds[i] * speeds[i] + 2 * maxAccel * dsMeters[i]);
        if (speeds[nextIdx] > maxAllowed) speeds[nextIdx] = maxAllowed;
      }
    }

    // 8. Physical time integration (dt = ds / v_avg)
    const timeDeltas = new Float64Array(N);
    let totalPhysicsTime = 0;
    for (let i = 0; i < N; i++) {
      const nextIdx = (i + 1) % N;
      const vAvg = (speeds[i] + speeds[nextIdx]) / 2;
      const dt = dsMeters[i] / Math.max(minSpeed / 3.6, vAvg);
      timeDeltas[i] = dt;
      totalPhysicsTime += dt;
    }

    // Sector calibration or global normalization
    let s1Scale = 1.0, s2Scale = 1.0, s3Scale = 1.0;
    const s1BeamIdx = options.s1NodeIndex;
    const s2BeamIdx = options.s2NodeIndex;
    const s1Target = options.s1TargetSec;
    const s2Target = options.s2TargetSec;

    if (s1BeamIdx && s2BeamIdx && s1Target && s2Target) {
      const s1RawTime = timeDeltas.slice(0, s1BeamIdx).reduce((a, b) => a + b, 0);
      const s2RawTime = timeDeltas.slice(s1BeamIdx, s2BeamIdx).reduce((a, b) => a + b, 0);
      const s3RawTime = timeDeltas.slice(s2BeamIdx, N).reduce((a, b) => a + b, 0);
      s1Scale = s1Target / Math.max(0.001, s1RawTime);
      s2Scale = (s2Target - s1Target) / Math.max(0.001, s2RawTime);
      s3Scale = (totalLapDuration - s2Target) / Math.max(0.001, s3RawTime);
    } else {
      const globalScale = totalLapDuration / Math.max(0.001, totalPhysicsTime);
      s1Scale = globalScale;
      s2Scale = globalScale;
      s3Scale = globalScale;
    }

    let runningTime = 0;
    for (let i = 0; i < N; i++) {
      nodes[i].t = Number(runningTime.toFixed(3));
      const scale = (s1BeamIdx && i < s1BeamIdx) ? s1Scale : ((s2BeamIdx && i < s2BeamIdx) ? s2Scale : s3Scale);
      runningTime += timeDeltas[i] * scale;

      const spdKmh = Math.round(speeds[i] * 3.6);
      nodes[i].speed = Math.max(minSpeed, Math.min(maxSpeed, spdKmh));

      const nextIdx = (i + 1) % N;
      const nextSpdKmh = Math.round(speeds[nextIdx] * 3.6);
      const isBraking = nextSpdKmh < spdKmh - 1;
      const isAccel = nextSpdKmh > spdKmh + 0.5;

      if (nodes[i].speed < 110) nodes[i].gear = 2;
      else if (nodes[i].speed < 160) nodes[i].gear = 3;
      else if (nodes[i].speed < 210) nodes[i].gear = 4;
      else if (nodes[i].speed < 255) nodes[i].gear = 5;
      else if (nodes[i].speed < 290) nodes[i].gear = 6;
      else if (nodes[i].speed < 320) nodes[i].gear = 7;
      else nodes[i].gear = 8;

      if (isBraking) {
        nodes[i].brake = Math.min(100, Math.max(40, Math.round((spdKmh - nextSpdKmh) * 10)));
        nodes[i].throttle = 0;
      } else if (isAccel) {
        nodes[i].brake = 0;
        nodes[i].throttle = nodes[i].speed > 250 ? 100 : Math.round(60 + 40 * (nodes[i].speed / maxSpeed));
      } else {
        nodes[i].brake = 0;
        nodes[i].throttle = nodes[i].curvature > 0.25 ? 40 : 100;
      }

      nodes[i].rpm = Math.round(10200 + (nodes[i].speed % 45) / 45 * 1900 + (nodes[i].speed > 300 ? 400 : 0));
      nodes[i].drs = (nodes[i].speed > 280 && nodes[i].brake === 0 && nodes[i].throttle === 100) ? 1 : 0;
    }

    return nodes;
  }

  /**
   * Divides track nodes into 3 sectors according to S1 and S2 time boundaries.
   *
   * @param {Array<Object>} nodes Track nodes with `t` property
   * @param {number} s1Time End of Sector 1 in seconds
   * @param {number} s2Time End of Sector 2 in seconds
   * @returns {Object} { nodes, sfBeam, s1Beam, s2Beam }
   */
  static configureSectors(nodes, s1Time, s2Time) {
    if (!nodes || nodes.length === 0) throw new Error('Nodes cannot be empty');

    let s1Beam = null;
    let s2Beam = null;
    const sfBeam = nodes[0];

    for (let i = 0; i < nodes.length; i++) {
      const node = nodes[i];
      if (node.t < s1Time) {
        node.sector = 1;
      } else if (node.t < s2Time) {
        node.sector = 2;
        if (!s1Beam) s1Beam = node;
      } else {
        node.sector = 3;
        if (!s2Beam) s2Beam = node;
      }
    }

    if (!s1Beam) s1Beam = nodes[Math.floor(nodes.length / 3)];
    if (!s2Beam) s2Beam = nodes[Math.floor((2 * nodes.length) / 3)];

    return {
      nodes,
      sfBeam,
      s1Beam,
      s2Beam
    };
  }

  /**
   * Connects pit lane nodes to main track, establishing entry branch,
   * exit merge, and a smooth Hermite blending transition curve.
   *
   * @param {Array<{px: number, py: number}>} pitNodes Raw pit lane points
   * @param {Array<Object>} trackNodes Track nodes
   * @param {Object} options { pitSpeedLimit: 80, mergeDurationSec: 2.0 }
   * @returns {Object} Pit lane model
   */
  static configurePitLane(pitNodes, trackNodes, options = {}) {
    if (!pitNodes || pitNodes.length < 2) {
      throw new Error('Pit lane requires at least 2 nodes');
    }

    const pitSpeedLimit = options.pitSpeedLimit || 80;
    const mergeDurationSec = options.mergeDurationSec || 2.0;

    // Find closest track node to pit entry
    const entryPt = pitNodes[0];
    let minEntryDist = Infinity;
    let entryTrackIdx = 0;

    for (let i = 0; i < trackNodes.length; i++) {
      const d = Math.hypot(trackNodes[i].px - entryPt.px, trackNodes[i].py - entryPt.py);
      if (d < minEntryDist) {
        minEntryDist = d;
        entryTrackIdx = i;
      }
    }

    // Find closest track node to pit exit
    const exitPt = pitNodes[pitNodes.length - 1];
    let minExitDist = Infinity;
    let exitTrackIdx = 0;

    for (let i = 0; i < trackNodes.length; i++) {
      const d = Math.hypot(trackNodes[i].px - exitPt.px, trackNodes[i].py - exitPt.py);
      if (d < minExitDist) {
        minExitDist = d;
        exitTrackIdx = i;
      }
    }

    // Interpolate pit lane nodes
    const processedPitNodes = pitNodes.map((p, idx) => ({
      idx,
      px: Number(p.px.toFixed(2)),
      py: Number(p.py.toFixed(2)),
      progress: Number((idx / (pitNodes.length - 1)).toFixed(4)),
      speed: pitSpeedLimit
    }));

    return {
      pitNodes: processedPitNodes,
      entry: {
        trackIdx: entryTrackIdx,
        px: entryPt.px,
        py: entryPt.py,
        distanceToTrack: minEntryDist
      },
      exit: {
        trackIdx: exitTrackIdx,
        px: exitPt.px,
        py: exitPt.py,
        distanceToTrack: minExitDist
      },
      pitSpeedLimit,
      mergeDurationSec,
      /**
       * Smoothly blends coordinates during pit lane exit transition using
       * smoothstep Hermite polynomial (3*u^2 - 2*u^3) to avoid positional jerks.
       */
      blendExitPosition(exitProgress, trackPoint) {
        const u = Math.max(0, Math.min(1, exitProgress));
        const smoothU = u * u * (3 - 2 * u);
        return {
          px: (1 - smoothU) * exitPt.px + smoothU * trackPoint.px,
          py: (1 - smoothU) * exitPt.py + smoothU * trackPoint.py
        };
      }
    };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = TrackMapAnalyzer;
}

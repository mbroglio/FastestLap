/**
 * TrackModel.js
 * Generic Data Model and Spatial Coordinate Provider for any F1 Circuit
 */

class TrackModel {
  /**
   * @param {Object} spec Track specification
   */
  constructor(spec = {}) {
    this.circuitId = spec.circuitId || 'unknown';
    this.circuitName = spec.circuitName || 'Generic Circuit';
    this.imageFile = spec.imageFile || '';
    this.dimensions = spec.dimensions || { width: 1000, height: 750 };
    this.lapDuration = spec.lapDuration || 90.0;
    this.trackNodes = spec.trackNodes || [];
    this.pitLane = spec.pitLane || null;
    this.sectors = spec.sectors || {};
    this.turns = spec.turns || [];
    this.gridSlots = spec.gridSlots || [];
  }

  /**
   * Total number of track nodes
   */
  getNodeCount() {
    return this.trackNodes.length;
  }

  /**
   * Retrieves continuous interpolated track state for a given second of the lap
   * @param {number} sec Second within lap (modulo lapDuration)
   * @returns {Object} { px, py, sector, speed, gear, throttle, brake, rpm, drs, location }
   */
  getPointAtTime(sec) {
    const N = this.trackNodes.length;
    if (N === 0) {
      return { px: 0, py: 0, sector: 1, speed: 0, gear: 1, throttle: 0, brake: 0, rpm: 4000, drs: 0 };
    }

    const t = ((sec % this.lapDuration) + this.lapDuration) % this.lapDuration;

    // Binary search across sorted time marks
    let low = 0, high = N - 1;
    while (low <= high) {
      const mid = (low + high) >> 1;
      if (this.trackNodes[mid].t <= t) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }

    const i1 = Math.max(0, high);
    const i2 = (i1 + 1) % N;
    const p1 = this.trackNodes[i1];
    const p2 = this.trackNodes[i2];

    let dt = (p2.t >= p1.t) ? (p2.t - p1.t) : (this.lapDuration - p1.t + p2.t);
    if (dt <= 0) dt = 0.001;

    const u = Math.max(0, Math.min(1, (t >= p1.t ? t - p1.t : this.lapDuration - p1.t + t) / dt));

    const px = Number((p1.px + (p2.px - p1.px) * u).toFixed(2));
    const py = Number((p1.py + (p2.py - p1.py) * u).toFixed(2));
    const speed = Math.round(p1.speed + (p2.speed - p1.speed) * u);
    const sector = u < 0.5 ? p1.sector : p2.sector;
    const gear = u < 0.5 ? p1.gear : p2.gear;
    const throttle = Math.round((p1.throttle || 100) * (1 - u) + (p2.throttle || 100) * u);
    const brake = Math.round((p1.brake || 0) * (1 - u) + (p2.brake || 0) * u);
    const rpm = Math.round((p1.rpm || 11000) * (1 - u) + (p2.rpm || 11000) * u);
    const drs = p1.drs || 0;

    return {
      px,
      py,
      sector: sector || 1,
      speed,
      gear,
      throttle,
      brake,
      rpm,
      drs,
      location: p1.location || (p1.sector ? `Sector ${p1.sector}` : 'On Track')
    };
  }

  /**
   * Retrieves continuous interpolated track state by normalized lap progress [0.0, 1.0]
   */
  getPointAtProgress(progress) {
    const sec = ((progress % 1.0) + 1.0) % 1.0 * this.lapDuration;
    return this.getPointAtTime(sec);
  }

  /**
   * Retrieves interpolated pit lane position for a normalized progress [0.0, 1.0]
   */
  getPitPointAtProgress(progress) {
    if (!this.pitLane || !this.pitLane.pitNodes || this.pitLane.pitNodes.length === 0) {
      return this.getPointAtTime(0);
    }

    const nodes = this.pitLane.pitNodes;
    const clampedU = Math.max(0, Math.min(1, progress));
    const exactIdx = clampedU * (nodes.length - 1);
    const i1 = Math.floor(exactIdx);
    const i2 = Math.min(nodes.length - 1, i1 + 1);
    const subU = exactIdx - i1;

    const p1 = nodes[i1];
    const p2 = nodes[i2];

    return {
      px: Number((p1.px + (p2.px - p1.px) * subU).toFixed(2)),
      py: Number((p1.py + (p2.py - p1.py) * subU).toFixed(2)),
      speed: this.pitLane.pitSpeedLimit || 80,
      sector: 1
    };
  }

  /**
   * Calculates the exact visual position of a car taking into account whether it is
   * on the main track, inside the pit lane, exiting the pit lane, or undergoing an
   * off-track incident / excursion / barrier crash.
   *
   * @param {number} trackSec Current lap time coordinate
   * @param {boolean} inPit Is driver currently in pit lane
   * @param {number} pitProgress Normalized progress inside pit lane [0, 1]
   * @param {number} mergeProgress Normalized progress of exit rejoin blend [0, 1] (or null if not merging)
   * @param {Object|null} incident Active incident descriptor
   * @param {number|null} sessionTimeSec Current global session time in seconds
   * @returns {Object} { px, py, speed, gear, throttle, brake, rpm, drs, inPit, isOffTrack, hazard, status, location }
   */
  getVisualCarPosition(trackSec, inPit = false, pitProgress = 0, mergeProgress = null, incident = null, sessionTimeSec = null) {
    const mainTrackPt = this.getPointAtTime(trackSec);

    // 1. Off-track Incident Handling (Runoff excursions, barrier crashes, stopped cars)
    if (incident && sessionTimeSec !== null && sessionTimeSec >= incident.startSec) {
      const dt = sessionTimeSec - incident.startSec;
      const targetPt = { px: incident.targetPx, py: incident.targetPy };

      // Case A: Temporary off-track excursion with rejoin (e.g. Hamilton T1, Sainz T3)
      if (incident.type === 'OFF_TRACK_REJOIN') {
        const dur = incident.durationSec || 14.0;
        if (dt <= dur) {
          const tEntry = incident.entryDuration || 3.0;
          const tMan = incident.maneuverDuration || (dur - 6.0);
          const tRejoin = incident.rejoinDuration || 3.0;
          const originPt = this.getPointAtTime(incident.trackSec || trackSec);

          if (dt < tEntry) {
            // Sliding off track into runoff / escape road
            const u = Math.min(1, Math.max(0, dt / tEntry));
            const s = u * u * (3 - 2 * u); // Hermite smoothstep
            return {
              ...mainTrackPt,
              px: Number((originPt.px * (1 - s) + targetPt.px * s).toFixed(2)),
              py: Number((originPt.py * (1 - s) + targetPt.py * s).toFixed(2)),
              speed: Math.round(originPt.speed * (1 - s) + 20 * s),
              gear: 1,
              throttle: 0,
              brake: 100,
              rpm: Math.round(11000 * (1 - s) + 4000 * s),
              drs: 0,
              inPit: false,
              isOffTrack: true,
              hazard: true,
              status: 'OFF_TRACK_SLIDE',
              location: incident.locationName || 'Via di Fuga'
            };
          } else if (dt < tEntry + tMan) {
            // Maneuvering in escape road
            const u = (dt - tEntry) / tMan;
            const wiggleX = Math.sin(u * Math.PI) * 1.5;
            const wiggleY = Math.cos(u * Math.PI) * 1.0;
            return {
              ...mainTrackPt,
              px: Number((targetPt.px + wiggleX).toFixed(2)),
              py: Number((targetPt.py + wiggleY).toFixed(2)),
              speed: 18,
              gear: 1,
              throttle: 25,
              brake: 10,
              rpm: 4500,
              drs: 0,
              inPit: false,
              isOffTrack: true,
              hazard: true,
              status: 'ESCAPE_ROAD_MANEUVER',
              location: incident.locationName || 'Via di Fuga'
            };
          } else {
            // Rejoining track onto racing line
            const u = Math.min(1, Math.max(0, (dt - tEntry - tMan) / tRejoin));
            const s = u * u * (3 - 2 * u);
            const rejoinPt = this.getPointAtTime((incident.trackSec || trackSec) + 2.5);
            return {
              ...mainTrackPt,
              px: Number((targetPt.px * (1 - s) + rejoinPt.px * s).toFixed(2)),
              py: Number((targetPt.py * (1 - s) + rejoinPt.py * s).toFixed(2)),
              speed: Math.round(20 * (1 - s) + 140 * s),
              gear: u < 0.5 ? 2 : 3,
              throttle: 80,
              brake: 0,
              rpm: Math.round(5000 * (1 - s) + 10500 * s),
              drs: 0,
              inPit: false,
              isOffTrack: false,
              hazard: true,
              status: 'REJOINING_TRACK',
              location: incident.locationName || 'Rientro in Pista'
            };
          }
        }
      }

      // Case B: Crash / Stopped Retirement
      if (incident.type === 'CRASH_RETIRED' || incident.type === 'STOPPED_RETIRED') {
        const tTrans = incident.transitionDuration || 2.5;
        const originPt = this.getPointAtTime(incident.trackSec || trackSec);
        if (dt < tTrans) {
          const u = Math.min(1, Math.max(0, dt / tTrans));
          const s = u * u * (3 - 2 * u);
          return {
            ...mainTrackPt,
            px: Number((originPt.px * (1 - s) + targetPt.px * s).toFixed(2)),
            py: Number((originPt.py * (1 - s) + targetPt.py * s).toFixed(2)),
            speed: Math.round(originPt.speed * (1 - s)),
            gear: 0,
            throttle: 0,
            brake: 100,
            rpm: Math.round(9000 * (1 - s)),
            drs: 0,
            inPit: false,
            isOffTrack: true,
            hazard: true,
            isRetired: true,
            status: 'CRASH_TRANSITION',
            location: incident.locationName || 'Impatto Fuori Pista'
          };
        } else {
          return {
            ...mainTrackPt,
            px: targetPt.px,
            py: targetPt.py,
            speed: 0,
            gear: 0,
            throttle: 0,
            brake: 0,
            rpm: 0,
            drs: 0,
            inPit: false,
            isOffTrack: true,
            hazard: false,
            isRetired: true,
            status: 'RETIRED_STOPPED',
            location: incident.locationName || 'Fermo Fuori Pista'
          };
        }
      }

      // Case C: Pit Retirement
      if (incident.type === 'PIT_RETIRED') {
        return {
          ...mainTrackPt,
          px: targetPt.px,
          py: targetPt.py,
          speed: 0,
          gear: 0,
          throttle: 0,
          brake: 0,
          rpm: 0,
          drs: 0,
          inPit: true,
          isOffTrack: false,
          hazard: false,
          isRetired: true,
          status: 'RETIRED_PIT',
          location: incident.locationName || 'Garage Box'
        };
      }
    }

    if (inPit) {
      const pitPt = this.getPitPointAtProgress(pitProgress);
      return {
        ...mainTrackPt,
        px: pitPt.px,
        py: pitPt.py,
        speed: this.pitLane ? this.pitLane.pitSpeedLimit : 80,
        gear: 2,
        throttle: 40,
        brake: 0,
        drs: 0,
        inPit: true,
        isOffTrack: false,
        hazard: false,
        location: 'Pit Lane'
      };
    }

    if (mergeProgress !== null && mergeProgress >= 0 && mergeProgress <= 1 && this.pitLane) {
      const blended = this.pitLane.blendExitPosition(mergeProgress, mainTrackPt);
      return {
        ...mainTrackPt,
        px: blended.px,
        py: blended.py,
        inPit: false,
        isOffTrack: false,
        hazard: false,
        location: 'Pit Rejoin'
      };
    }

    return {
      ...mainTrackPt,
      inPit: false,
      isOffTrack: false,
      hazard: false
    };
  }

  /**
   * Finds the closest track node to given coordinates
   */
  findNearestNode(px, py) {
    let minDist = Infinity;
    let closestNode = null;
    let closestIdx = -1;

    for (let i = 0; i < this.trackNodes.length; i++) {
      const node = this.trackNodes[i];
      const d = Math.hypot(node.px - px, node.py - py);
      if (d < minDist) {
        minDist = d;
        closestNode = node;
        closestIdx = i;
      }
    }

    return { node: closestNode, index: closestIdx, distance: minDist };
  }
}

if (typeof module !== 'undefined' && module.exports) {
  module.exports = TrackModel;
}

/**
 * FastestLap LiveTiming - Cockpit Dashboard Controller
 * Real-time driver telemetry instruments: Gear, Speed, Shift Lights, Throttle, Brake, Battery/ERS, Aero/DRS.
 */

class CockpitDashboard {
  constructor(containerElement) {
    this.container = containerElement;
    this.driverNum = null;
    this.driverInfo = null;

    // DOM References
    this.driverNameEl = document.getElementById('cockpitDriverName');
    this.driverTeamEl = document.getElementById('cockpitTeamName');
    this.gearEl = document.getElementById('cockpitGear');
    this.speedEl = document.getElementById('cockpitSpeed');
    this.rpmEl = document.getElementById('cockpitRpm');
    this.shiftLeds = Array.from(document.querySelectorAll('.shift-led'));
    this.throttleBarEl = document.getElementById('throttleBar');
    this.throttleValEl = document.getElementById('throttleValue');
    this.brakeBarEl = document.getElementById('brakeBar');
    this.brakeValEl = document.getElementById('brakeValue');
    this.batteryBarEl = document.getElementById('batteryBar');
    this.batteryValEl = document.getElementById('batteryValue');
    this.drsEl = document.getElementById('drsStatus');
    this.ersEl = document.getElementById('ersStatus');
  }

  setDriver(driverNum, driverInfo) {
    this.driverNum = String(driverNum);
    this.driverInfo = driverInfo || {};
    if (this.driverNameEl) {
      this.driverNameEl.textContent = `${this.driverInfo.FullName || this.driverInfo.BroadcastName || 'Driver ' + driverNum} #${driverNum}`;
    }
    if (this.driverTeamEl) {
      this.driverTeamEl.textContent = this.driverInfo.TeamName || 'Formula 1 Team';
    }
  }

  updateTelemetry(telemetry) {
    if (!telemetry) return;

    // Gear
    if (this.gearEl) {
      const g = telemetry.gear;
      this.gearEl.textContent = g === 0 ? 'N' : (g !== null && g !== undefined ? g : '-');
    }

    // Speed
    if (this.speedEl) {
      this.speedEl.textContent = telemetry.speed !== null && telemetry.speed !== undefined ? Math.round(telemetry.speed) : '--';
    }

    // RPM & Shift Lights
    const rpm = telemetry.rpm || 0;
    if (this.rpmEl) {
      this.rpmEl.textContent = rpm ? `${rpm} RPM` : '-- RPM';
    }

    // Update 10 LED Shift Lights (9000 to 12500 RPM range)
    const minRpm = 9500;
    const maxRpm = 12500;
    const ratio = Math.max(0, Math.min(1, (rpm - minRpm) / (maxRpm - minRpm)));
    const activeLeds = Math.round(ratio * this.shiftLeds.length);

    this.shiftLeds.forEach((led, idx) => {
      if (idx < activeLeds) {
        led.classList.add('on');
      } else {
        led.classList.remove('on');
      }
    });

    // Throttle Bar (0 - 100%)
    const thr = telemetry.throttle !== null ? Math.round(telemetry.throttle) : 0;
    if (this.throttleBarEl) this.throttleBarEl.style.width = `${thr}%`;
    if (this.throttleValEl) this.throttleValEl.textContent = `${thr}%`;

    // Brake Bar
    const brk = telemetry.brakeRaw !== null && telemetry.brakeRaw !== undefined 
      ? Math.round(telemetry.brakeRaw) 
      : (telemetry.brake === 1 ? 100 : 0);
    if (this.brakeBarEl) this.brakeBarEl.style.width = `${brk}%`;
    if (this.brakeValEl) this.brakeValEl.textContent = `${brk}%`;

    // Battery / ERS Energy Store
    const bat = telemetry.battery !== undefined ? Math.round(telemetry.battery) : 80;
    if (this.batteryBarEl) this.batteryBarEl.style.width = `${bat}%`;
    if (this.batteryValEl) this.batteryValEl.textContent = `${bat}%`;

    // DRS / Aero Mode
    if (this.drsEl) {
      const drsState = telemetry.drsState || (telemetry.drsActive ? 'OPEN' : 'OFF');
      this.drsEl.textContent = drsState;
      this.drsEl.className = 'indicator-value ' + (
        drsState === 'OPEN' ? 'drs-open' : (drsState === 'AVAILABLE' ? 'drs-available' : 'drs-off')
      );
    }

    // ERS Mode
    if (this.ersEl) {
      const mode = telemetry.ersMode || 'BALANCED';
      this.ersEl.textContent = mode;
      this.ersEl.className = 'indicator-value ' + (
        mode === 'HOTLAP' ? 'ers-hotlap' : (mode === 'OVERTAKE' ? 'ers-overtake' : 'ers-balanced')
      );
    }
  }
}

window.CockpitDashboard = CockpitDashboard;

import json
import os

with open('signalr_test_suite/data/openf1_stints_11731.json', 'r', encoding='utf-8') as f:
    stints_raw = json.load(f)

# Group by driver
drivers = {}
for s in stints_raw:
    d = str(s['driver_number'])
    if d not in drivers:
        drivers[d] = []
    drivers[d].append(s)

cleaned_stints = {}
for d, s_list in sorted(drivers.items(), key=lambda x: int(x[0])):
    # Deduplicate and sort
    s_list.sort(key=lambda x: (x['stint_number'], x['lap_start']))
    clean_list = []
    seen = set()
    for s in s_list:
        key = (s['stint_number'], s['lap_start'], s['lap_end'], s['compound'])
        if key not in seen:
            seen.add(key)
            clean_list.append({
                'stint': s['stint_number'],
                'lapStart': s['lap_start'],
                'lapEnd': s['lap_end'],
                'compound': s['compound'],
                'code': 'I' if s['compound'] == 'INTERMEDIATE' else s['compound'][0],
                'ageAtStart': s['tyre_age_at_start']
            })
    cleaned_stints[d] = clean_list

# Generate JS file
js_content = """/**
 * Official Tire Stints & Compounds for Sepang Grand Prix 2026
 * Sourced directly from FastF1 / Live Timing & OpenF1 Session 11731
 * Track Condition: Damp/Wet start (Intermediates used by multiple drivers), drying track.
 */

const DRIVER_STINTS = """ + json.dumps(cleaned_stints, indent=2) + """;

function getDriverStintInfo(drvKey, lapNum) {
  const stints = DRIVER_STINTS[drvKey];
  if (!stints || stints.length === 0) {
    return { stint: 1, compound: 'MEDIUM', code: 'M', tyreAge: lapNum || 1 };
  }
  
  // Find matching stint for lapNum
  for (let i = 0; i < stints.length; i++) {
    const s = stints[i];
    if (lapNum >= s.lapStart && lapNum <= s.lapEnd) {
      const age = s.ageAtStart + (lapNum - s.lapStart);
      return {
        stint: s.stint,
        compound: s.compound,
        code: s.code,
        tyreAge: age
      };
    }
  }

  // Fallback to last stint if lap exceeds
  const last = stints[stints.length - 1];
  const age = last.ageAtStart + Math.max(0, lapNum - last.lapStart);
  return {
    stint: last.stint,
    compound: last.compound,
    code: last.code,
    tyreAge: age
  };
}

module.exports = {
  DRIVER_STINTS,
  getDriverStintInfo
};
"""

with open('signalr_test_suite/src/driver_stints.js', 'w', encoding='utf-8') as f_out:
    f_out.write(js_content)

print(f"Generated signalr_test_suite/src/driver_stints.js for {len(cleaned_stints)} drivers!")

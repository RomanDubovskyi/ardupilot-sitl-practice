# Strike Mission System - Task Breakdown

## Project Overview
Build a system that commands an ArduPilot plane to execute precision strike missions against ground targets. The plane will autonomously fly to a target, perform a 45° dive attack, activate a servo to simulate weapon release, pull out safely, and return home.

---

## MVP Scope
- **Single target** strike capability
- Pre-flight safety checks
- Autonomous dive-drop-climb sequence
- State tracking throughout mission
- Emergency abort functionality

---

## Phase 1: Connection & Basic Commands

### Objective
Establish reliable communication with ArduPilot SITL and execute basic flight commands.

### Requirements
- Connect to plane via MAVLink protocol
- Send ARM command
- Send mode change commands (MANUAL, FBWA, GUIDED)
- Read current flight mode
- Read basic telemetry (position, altitude)

### Testing in SITL
1. Start ArduPilot SITL (plane)
2. Start MAVLink bridge server
3. Run your application
4. Verify connection established
5. Send ARM command → See "ARMED" in SITL console
6. Send mode change to GUIDED → See "Mode GUIDED" in SITL
7. Read telemetry → Verify correct lat/lon/alt displayed

### Success Criteria
- ✓ Application connects without errors
- ✓ ARM command executes successfully
- ✓ Mode changes confirmed in SITL
- ✓ Telemetry data accurately reflects SITL state
- ✓ Graceful disconnection when app closes

### Limitations
- No flight movement yet, only ground operations
- Manual throttle control still needed for takeoff in SITL

### Expected Output
- Console logs showing connection status
- Current mode displayed
- Position updates (lat/lon/alt) printed every 2-5 seconds

---

## Phase 2: Takeoff & Landing

### Objective
Command the plane to takeoff autonomously and land safely.

### Requirements
- Execute takeoff sequence (mode change + throttle)
- Monitor altitude during climb
- Verify plane reaches target altitude
- Execute landing sequence (mode RTL or LAND)
- Monitor descent and touchdown

### Testing in SITL
1. Start SITL with armed plane on ground
2. Send takeoff command to cruise altitude (300m)
3. Watch SITL HUD - plane should climb
4. Monitor altitude telemetry - verify reaching 300m
5. Send land/RTL command
6. Verify plane descends and lands at home position

### Success Criteria
- ✓ Plane takes off without manual intervention
- ✓ Climbs to specified altitude (±10m tolerance)
- ✓ Maintains altitude once reached
- ✓ RTL navigates back to launch point
- ✓ Lands within 50m of home position
- ✓ Disarms automatically after landing

### Limitations
- For ArduPilot planes, may need to use FBWA mode + throttle control for takeoff
- RTL behavior depends on plane configuration

### Expected Output
- Altitude updates showing climb: 0m → 50m → 150m → 300m
- Mode transitions: MANUAL → FBWA → GUIDED → RTL
- Final message: "Landed successfully at home"

---

## Phase 3: Waypoint Navigation

### Objective
Command plane to fly to specific GPS coordinates autonomously.

### Requirements
- Send "goto location" command with lat/lon/alt
- Plane navigates to waypoint
- Monitor distance to target
- Detect arrival at waypoint (within threshold)
- Transition to loiter or next action

### Testing in SITL
1. Takeoff to cruise altitude (300m)
2. Send goto command to point 1km away
3. Watch SITL map - plane should turn toward target
4. Monitor distance decreasing
5. Verify arrival detection when within 50m
6. Plane should loiter at waypoint

### Success Criteria
- ✓ Plane turns toward waypoint immediately
- ✓ Follows direct path (not wandering)
- ✓ Arrives within 50m of target coordinates
- ✓ Application detects arrival accurately
- ✓ Can chain multiple waypoints sequentially

### Limitations
- Wind simulation in SITL may cause drift
- Waypoint radius tolerance impacts precision

### Expected Output
- "Flying to target: -35.123456, 149.123456"
- "Distance to target: 850m... 600m... 300m... 50m"
- "Arrived at waypoint"

---

## Phase 4: Pre-Flight Safety Checks

### Objective
Implement automated safety verification before mission execution.

### Requirements
- Check GPS status (fix type, satellite count, HDOP)
- Verify minimum GPS accuracy (3D fix, 6+ satellites)
- Check battery level (>30% for mission)
- Verify plane is armed or armable
- Check altitude sensor health
- Validate home position is set
- Confirm no existing failsafes active

### Testing in SITL
1. Start SITL in various states:
   - Good: All sensors healthy
   - Bad GPS: Reduce sat count in SITL params
   - Low battery: Set battery to 20%
2. Run pre-flight checks
3. Verify detection of each failure condition
4. Confirm mission blocked when unsafe

### Success Criteria
- ✓ All checks pass with healthy SITL
- ✓ GPS check fails when sats < 6
- ✓ Battery check fails when < 30%
- ✓ Clear error messages for each failure
- ✓ Mission won't start if any check fails

### Limitations
- SITL simulation may not perfectly replicate all sensor failures
- Some checks may need parameter tuning

### Expected Output
```
Pre-flight checks:
✓ GPS: 3D Fix, 10 satellites, HDOP 0.8
✓ Battery: 95% (12.4V)
✓ Home position: Set
✓ Sensors: All healthy
✓ Airspace: Clear
Mission approved for execution
```

Or on failure:
```
✗ GPS: Only 4 satellites (need 6+)
✗ Battery: 25% (need 30%+)
Mission BLOCKED - resolve issues before flight
```

---

## Phase 5: State Machine Implementation

### Objective
Build a mission state tracker that manages transitions through the mission lifecycle.

### Requirements
States to implement:
- **IDLE** - System ready, no mission
- **PREFLIGHT** - Running safety checks
- **TAKEOFF** - Climbing to cruise altitude
- **TRANSIT** - Flying to target approach point
- **ATTACK_APPROACH** - Setting up for dive
- **DIVING** - Executing 45° dive
- **PULLING_OUT** - Recovering from dive
- **CLIMBING** - Returning to cruise altitude
- **RTL** - Returning to launch
- **LANDING** - Final descent
- **ABORTED** - Emergency abort active

### Transition Rules
- IDLE → PREFLIGHT (when mission submitted)
- PREFLIGHT → TAKEOFF (if checks pass)
- PREFLIGHT → IDLE (if checks fail)
- TAKEOFF → TRANSIT (when altitude reached)
- TRANSIT → ATTACK_APPROACH (when near target)
- ATTACK_APPROACH → DIVING (when positioned)
- DIVING → PULLING_OUT (when min altitude reached)
- PULLING_OUT → CLIMBING (when pitch positive)
- CLIMBING → RTL (when cruise altitude reached)
- RTL → LANDING (when near home)
- LANDING → IDLE (when on ground)
- ANY → ABORTED (on emergency abort)

### Testing in SITL
1. Start mission, log each state transition
2. Verify state only changes when conditions met
3. Test abort from each state - should go to ABORTED then RTL
4. Verify no invalid transitions occur
5. Check state persists correctly across telemetry updates

### Success Criteria
- ✓ All states reachable in normal mission flow
- ✓ Transitions happen only when conditions met
- ✓ Abort works from any state
- ✓ State visible in API status endpoint
- ✓ No race conditions or stuck states

### Limitations
- State timing depends on telemetry update rate
- Rapid state changes may need debouncing

### Expected Output
```
[12:34:01] State: IDLE → PREFLIGHT
[12:34:02] State: PREFLIGHT → TAKEOFF (checks passed)
[12:34:45] State: TAKEOFF → TRANSIT (altitude 300m reached)
[12:35:30] State: TRANSIT → ATTACK_APPROACH (1km from target)
[12:36:00] State: ATTACK_APPROACH → DIVING
[12:36:15] State: DIVING → PULLING_OUT (100m AGL)
[12:36:25] State: PULLING_OUT → CLIMBING
[12:36:50] State: CLIMBING → RTL (cruise altitude)
[12:37:30] State: RTL → LANDING (home proximity)
[12:38:00] State: LANDING → IDLE (touchdown)
```

---

## Phase 6: Drop Point Calculation

### Objective
Calculate the precise moment to activate the servo during dive to hit the target.

### Requirements
- Input: Target coordinates, drop altitude, plane velocity
- Calculate ballistic trajectory of dropped object
- Account for:
  - Horizontal velocity at drop
  - Altitude above target
  - Wind (optional for MVP)
  - Gravity (9.81 m/s²)
- Output: GPS coordinates where drop should occur
- Convert to distance/time before target

### Physics Model (Simplified)
```
Given:
- Plane velocity: V (m/s)
- Drop altitude: H (meters above target)
- Gravity: g = 9.81 m/s²

Time to fall: t = sqrt(2*H/g)
Horizontal distance: d = V * t

Drop point = target - (d in direction of travel)
```

### Testing in SITL
1. Set target coordinates
2. Plan approach at known velocity (e.g., 25 m/s)
3. Calculate drop point for 200m altitude
4. Verify calculation: d = 25 * sqrt(2*200/9.81) ≈ 159m
5. Test with different velocities and altitudes
6. Compare calculated vs actual impact point (visual in SITL map)

### Success Criteria
- ✓ Drop point calculated correctly for static conditions
- ✓ Accounts for velocity variations (20-30 m/s range)
- ✓ Accounts for altitude variations (100-300m range)
- ✓ Calculation completes in <100ms
- ✓ Results are GPS coordinates (lat/lon)

### Limitations
- Simplified ballistics (no air resistance on dropped object)
- Assumes level terrain at target
- Wind effects ignored in MVP
- Object assumed to drop straight down relative to ground

### Expected Output
```
Target: -35.363262, 149.165238
Plane velocity: 25 m/s
Drop altitude: 200m AGL

Calculated:
- Fall time: 6.4 seconds
- Horizontal drift: 159 meters
- Drop point: -35.361828, 149.165238 (159m before target)
- Drop activation: 6.4 seconds before target
```

---

## Phase 7: Dive Attack Profile

### Objective
Generate precise waypoint sequence for dive-drop-climb maneuver.

### Requirements
Generate waypoints:
1. **Approach Point** - 1km before target, cruise altitude, aligned with attack heading
2. **Dive Start** - 500m before target, begin descent at -45° pitch
3. **Drop Point** - Calculated position, servo activation altitude (150-200m)
4. **Pull Out** - Minimum safe altitude (100m), start climb
5. **Climb Out** - Return to cruise altitude (300m)

Constraints:
- Maximum dive angle: -45°
- Minimum altitude: 100m AGL
- Maximum G-load during pull-out: 3G (for safety)
- Climb rate: 5 m/s typical

### Waypoint Calculations
```
Approach Point:
- 1000m before target on attack heading
- Altitude: cruise (300m)

Dive Entry:
- 500m before target
- Altitude: cruise (300m)
- Pitch: start transition to -45°

Drop Point:
- Distance: calculated from physics (Phase 6)
- Altitude: 150-200m (based on velocity)
- Action: activate servo

Pull Out Point:
- Altitude: 100m AGL (minimum safe)
- Pitch: transition from -45° to +10°

Climb Out:
- Return to 300m
- Heading: toward home or next target
```

### Testing in SITL
1. Define target at known location
2. Generate dive profile waypoints
3. Upload to SITL as mission
4. Execute and monitor in HUD:
   - Approach at 300m
   - Dive starts at 500m before target
   - Pitch reaches -45°
   - Pull out at 100m
   - Climb back to 300m
5. Verify waypoint spacing is safe (no abrupt maneuvers)

### Success Criteria
- ✓ Approach point 1km from target
- ✓ Dive angle reaches -45° (±5°)
- ✓ Pull out occurs at 100m (±10m)
- ✓ Climb completes back to cruise altitude
- ✓ No excessive G-forces (monitor SITL warnings)
- ✓ Waypoints execute in sequence without skipping

### Limitations
- ArduPilot may smooth pitch transitions (won't be instant -45°)
- Wind affects actual trajectory
- SITL physics simplified vs real aircraft

### Expected Output
```
Dive Attack Profile Generated:
Waypoint 1: Approach (-35.354123, 149.165238, 300m)
Waypoint 2: Dive Entry (-35.358692, 149.165238, 300m, pitch -20°)
Waypoint 3: Dive Active (-35.360500, 149.165238, 200m, pitch -45°)
Waypoint 4: Drop Point (-35.361828, 149.165238, 175m, SERVO ON)
Waypoint 5: Pull Out (-35.362500, 149.165238, 100m, pitch +10°)
Waypoint 6: Climb Out (-35.363000, 149.165238, 300m)

Mission duration: ~90 seconds
Total distance: 2.5 km
```

---

## Phase 8: Servo Activation Command

### Objective
Trigger servo activation at precise moment during dive to simulate weapon release.

### Requirements
- Send MAVLink command to activate servo channel
- Typical servo: Channel 9, PWM 1000→2000
- Activation duration: 0.5 seconds (pulse)
- Verify command received by autopilot
- Log activation timestamp

### MAVLink Details
Command: `DO_SET_SERVO`
Parameters:
- Servo channel (e.g., 9)
- PWM value (1000 = closed, 2000 = open)

Sequence:
1. Send: Channel 9, PWM 2000 (activate)
2. Wait: 500ms
3. Send: Channel 9, PWM 1000 (reset)

### Testing in SITL
1. Configure SITL servo output (SERVO9_FUNCTION = 1)
2. Execute dive mission
3. At drop point, send servo command
4. Monitor SITL console for: "Servo 9 output: 2000"
5. After 500ms verify: "Servo 9 output: 1000"
6. Log shows activation at correct altitude/position

### Success Criteria
- ✓ Servo command sent at calculated drop point
- ✓ SITL confirms servo activation
- ✓ Timing accurate (±0.5 seconds)
- ✓ Servo resets after activation
- ✓ Drop occurs at predicted coordinates (±50m)

### Limitations
- SITL doesn't simulate actual object drop physics
- Can only verify command sent, not physical result
- No feedback if servo mechanically fails (not simulated)

### Expected Output
```
[12:36:12.450] Approaching drop point (175m AGL)
[12:36:12.500] DROP ACTIVATED - Servo 9 PWM 2000
[12:36:12.500] Position: -35.361828, 149.165238, 175m
[12:36:13.000] Servo reset - PWM 1000
[12:36:13.100] Pulling out at 100m AGL
```

SITL Console:
```
Servo 9 output: 2000
Servo 9 output: 1000
```

---

## Phase 9: Emergency Abort

### Objective
Implement immediate mission termination with safe return to home.

### Requirements
- Abort API endpoint/command
- Works from any mission state
- Actions:
  - Stop current maneuver immediately
  - Cancel all pending waypoints
  - Switch to RTL mode
  - Log abort reason and timestamp
  - Transition state to ABORTED → RTL
- Resume not allowed after abort (mission failed)

### Abort Triggers (manual for MVP)
- User abort command
- (Future: Low battery, GPS loss, geofence breach)

### Testing in SITL
Test abort during each phase:
1. **During takeoff** - Should level off and RTL
2. **During transit** - Should turn toward home
3. **During dive** - Should pull out and RTL (critical!)
4. **During climb** - Should continue climb then RTL

Special test: Abort during dive at 150m
- Verify immediate pull-out (no continuing descent)
- Verify climb to safe altitude before RTL
- Check no aggressive maneuvers

### Success Criteria
- ✓ Abort interrupts mission within 1 second
- ✓ Plane safely returns from any state
- ✓ Abort during dive doesn't crash (pulls out first)
- ✓ Mission state set to ABORTED
- ✓ Home position reached
- ✓ Lands automatically
- ✓ Abort reason logged

### Limitations
- ArduPilot may delay response (telemetry lag)
- Abort during dive requires altitude margin (don't test too low)

### Expected Output
```
[12:36:05] State: DIVING (altitude 140m)
[12:36:06] ABORT COMMANDED - User initiated
[12:36:06] State: DIVING → ABORTED
[12:36:07] Canceling remaining waypoints
[12:36:07] Switching to RTL mode
[12:36:08] State: ABORTED → RTL
[12:36:08] Climbing to safe altitude (200m)
[12:36:45] Returning to home
[12:37:20] Landing at home position
[12:37:55] Mission terminated - ABORTED
```

---

## Phase 10: Full Mission Integration

### Objective
Combine all phases into a complete end-to-end strike mission.

### Requirements
Single API call triggers:
1. Pre-flight checks
2. Takeoff to cruise altitude
3. Transit to target approach point
4. Execute dive attack with servo drop
5. Climb back to cruise
6. RTL and land
7. Report mission success

Input:
```json
{
  "target": {
    "latitude": -35.363262,
    "longitude": 149.165238,
    "altitude": 584
  },
  "cruiseAltitude": 300,
  "dropAltitude": 175,
  "minSafeAltitude": 100
}
```

### Mission Flow
```
1. Validate target coordinates
2. Run pre-flight checks
3. Calculate drop point
4. Generate dive profile waypoints
5. Arm plane
6. Takeoff to 300m
7. Transit to approach point
8. Execute dive sequence:
   - Descend at -45°
   - Activate servo at drop point
   - Pull out at 100m
9. Climb to 300m
10. RTL and land
11. Disarm
```

### Testing in SITL
End-to-end test:
1. Start SITL with plane on ground
2. Send mission request with target 2km away
3. **Do not intervene** - full autonomous execution
4. Monitor state transitions through all phases
5. Verify dive occurs at correct location
6. Verify servo activation logged
7. Verify safe landing at home

Measure:
- Total mission time
- Accuracy: distance from drop point to target
- Safety: minimum altitude during pull-out

### Success Criteria
- ✓ Mission completes without manual intervention
- ✓ All state transitions occur correctly
- ✓ Drop point within 100m of target
- ✓ Pull-out altitude > 100m
- ✓ Lands at home position (±50m)
- ✓ Total mission time < 5 minutes (for 2km target)
- ✓ No errors or warnings in logs

### Limitations
- SITL wind may affect accuracy
- No actual payload drop physics
- Simplified terrain (flat ground assumed)

### Expected Output
```
Mission Request Received:
Target: -35.363262, 149.165238
Cruise: 300m, Drop: 175m, Min Safe: 100m

Pre-flight: PASSED
Drop point calculated: -35.361828, 149.165238
Mission waypoints: 8 generated

[12:34:00] IDLE → PREFLIGHT
[12:34:01] PREFLIGHT → TAKEOFF
[12:34:45] TAKEOFF → TRANSIT (300m reached)
[12:35:30] TRANSIT → ATTACK_APPROACH
[12:36:00] ATTACK_APPROACH → DIVING
[12:36:12] SERVO ACTIVATED (175m AGL)
[12:36:15] DIVING → PULLING_OUT (100m)
[12:36:25] PULLING_OUT → CLIMBING
[12:36:50] CLIMBING → RTL
[12:37:30] RTL → LANDING
[12:38:00] LANDING → IDLE

Mission Complete:
Duration: 4m 0s
Drop accuracy: 47m from target
Min altitude: 103m AGL
Status: SUCCESS
```

---

## Post-MVP Enhancements (Not Required for Initial Completion)

### Multiple Targets
- Accept array of targets
- Route optimization (shortest path)
- Fuel/battery calculation for multiple strikes

### Advanced Attack Parameters
- Attack heading selection (compass direction)
- Multiple attack patterns (orbit, pop-up)
- Delayed drop (timed release)

### Enhanced Safety
- Mission validation (distance, fuel checks)
- Geofence enforcement
- Battery failsafe
- GPS health monitoring with auto-abort

### Telemetry & Reporting
- Real-time WebSocket telemetry stream
- Post-mission reports
- Video feed integration (if camera available)
- Hit assessment (BDA - Battle Damage Assessment)

---

## Testing Checklist

Before considering MVP complete, verify:

- [ ] Connection established with SITL
- [ ] ARM/DISARM commands work
- [ ] Mode changes execute (MANUAL, FBWA, GUIDED, RTL)
- [ ] Telemetry reads accurate position/altitude
- [ ] Takeoff reaches target altitude
- [ ] Waypoint navigation arrives within 50m
- [ ] Pre-flight checks detect bad GPS
- [ ] Pre-flight checks detect low battery
- [ ] State machine transitions correctly through all states
- [ ] Drop point calculation produces reasonable values
- [ ] Dive profile generates 6+ waypoints
- [ ] Servo activation command sent
- [ ] SITL confirms servo output change
- [ ] Abort works from TRANSIT state
- [ ] Abort works from DIVING state (critical!)
- [ ] Full mission completes without intervention
- [ ] Plane lands within 50m of home
- [ ] No crashes or unsafe maneuvers
- [ ] Logs show complete mission trace

---

## Resources & Hints

### ArduPilot SITL Commands
```bash
# Start plane simulator
cd ~/uas/ardupilot/ArduPlane
source ../.venv/bin/activate
../Tools/autotest/sim_vehicle.py --console --map --out=udp:127.0.0.1:14540

# MAVProxy console commands
mode GUIDED          # Switch to guided mode
arm throttle         # Arm motors
rc 3 1700           # Set throttle (for manual takeoff if needed)
```

### MAVLink Message Types to Research
- `COMMAND_LONG` - Send commands (ARM, mode change)
- `SET_POSITION_TARGET_GLOBAL_INT` - Goto waypoint
- `MISSION_ITEM_INT` - Upload waypoint missions
- `DO_SET_SERVO` - Activate servo
- `HEARTBEAT` - Connection health
- `GLOBAL_POSITION_INT` - Current GPS position
- `VFR_HUD` - Airspeed, altitude, heading

### State Machine Pattern
Consider using a state pattern or enum-based state machine with:
- Current state tracking
- Transition validation (prevent invalid transitions)
- Entry/exit actions for each state
- State persistence across telemetry updates

### Drop Point Math Validation
Test calculation with known values:
```
Velocity: 25 m/s
Altitude: 200m AGL
Expected fall time: sqrt(2*200/9.81) = 6.39s
Expected distance: 25 * 6.39 = 159.8m
```

### Safety Margins
Always maintain margins:
- Minimum altitude +10m buffer
- Drop point calculation +20% margin
- Geofence boundary -50m buffer
- Battery reserve +5% buffer

---

## Troubleshooting Common Issues

**Issue**: Plane won't arm
- Check: GPS has 3D fix
- Check: Home position is set
- Check: No active failsafes
- Solution: Wait for GPS lock, check SITL console

**Issue**: Goto waypoint fails
- Check: Plane is in GUIDED mode
- Check: Plane is airborne (>50m altitude)
- Solution: Switch to GUIDED after takeoff

**Issue**: Dive too aggressive / crashes
- Check: Pull-out altitude not too low
- Check: Waypoint spacing adequate
- Solution: Increase minimum safe altitude, reduce dive angle

**Issue**: Servo doesn't activate
- Check: Servo channel configured in SITL
- Check: Command acknowledgment received
- Solution: Verify MAVLink DO_SET_SERVO parameters

**Issue**: State machine stuck
- Check: Telemetry update rate
- Check: Condition thresholds (altitude tolerance, distance threshold)
- Solution: Add timeout fallbacks, log state transition conditions

---

## Final Notes

- **Safety First**: Always test in SITL before real hardware
- **Incremental Development**: Complete each phase before moving to next
- **Logging**: Log everything - it's your debugging lifeline
- **Testing**: Test each component in isolation before integration
- **Validation**: Verify calculations manually before trusting them
- **Error Handling**: Plan for failures at every step
- **Documentation**: Comment your code - explain the "why"

Good luck with your strike mission system! 🎯

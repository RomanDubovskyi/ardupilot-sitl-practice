# State Machine Architecture Analysis
## Architectural Question: Plane State Management

### Current Situation
- Phase 2: Implementing takeoff and landing in `PlaneServiceImpl`
- Need to maintain state (can't land if already disarmed on ground, etc.)
- Considering two approaches:
  1. **Enum + Switch**: Define states as enum, use switch/case for logic
  2. **State Pattern**: Separate handlers per state (PlaneDisarmedHandler, PlaneArmedHandler, etc.)

---

## How This Is Canonically Solved in Production

### UAV/Drone Industry Standards

From research on ArduPilot and MAVLink-based systems:

1. **Mode-Based State Machines**: [ArduPilot uses a mode-based architecture](https://nrotella.github.io/journal/ardupilot-drone-control-stack.html) where each flight mode is a separate class inheriting from a base `Mode` class
   - Each mode encapsulates its own control logic, navigation, and throttle behavior
   - The `ModeAuto` class implements autonomous mission execution as a state machine with multiple submodes
   - Transitions are driven by MAVLink messages and internal conditions

2. **Hierarchical State Machines**: [ArduPilot implements cascaded control architecture](https://deepwiki.com/ArduPilot/ardupilot/3.1-arducopter) with:
   - Flight mode logic (top layer)
   - Position control (middle layer)
   - Attitude control (middle layer)
   - Motor output (bottom layer)

3. **Event-Driven FSMs**: [Autonomous drone systems use event-driven state machines](https://medium.com/@abhiojha8/flying-an-autonomous-drone-using-event-driven-state-machine-e0e1fb756d54) for managing complex autonomous behavior

### Enterprise Java Patterns

From design pattern research:

1. **State Pattern vs Strategy Pattern**:
   - [State Pattern](https://www.baeldung.com/java-state-design-pattern): For dynamic behavior that changes over time, states can trigger their own transitions
   - [Strategy Pattern](https://www.baeldung.com/cs/design-state-pattern-vs-strategy-pattern): For selecting algorithms that remain stable, set externally
   - **Recommendation for your case**: State Pattern (behavior changes dynamically, states need to manage transitions)

2. **Key Characteristics of State Pattern**:
   - States are aware of each other and can trigger transitions
   - Context (your PlaneServiceImpl) delegates behavior to current state
   - States are changed dynamically during runtime
   - Closely related to Finite State Machine concept

---

## Analysis of Your Proposed Approaches

### Approach 1: Enum + Switch/Case

```java
enum PlaneState {
    IDLE, PREFLIGHT, ARMED, TAKEOFF, AIRBORNE, LANDING, ABORTED
}

public void takeOff(System drone) {
    switch(currentState) {
        case IDLE -> throw new IllegalStateException("Cannot takeoff: not armed");
        case ARMED -> performTakeoff(drone);
        case AIRBORNE -> throw new IllegalStateException("Already airborne");
        // ... more cases
    }
}
```

**Pros**:
- Simple to understand
- Easy to see all states in one place
- Low overhead for simple scenarios

**Cons**:
- Violates Open/Closed Principle (must modify switch for new states)
- All state logic clutters service class
- Hard to test individual state behaviors
- Becomes unwieldy with complex state-specific logic
- Doesn't scale well to Phase 5's full state machine (11 states)

**Verdict**: Good for Phase 2 only, but you'll need to refactor for Phase 5

---

### Approach 2: State Pattern with Handlers

```java
interface PlaneStateHandler {
    void takeOff(PlaneContext context, System drone);
    void land(PlaneContext context, System drone);
    PlaneState getState();
}

class ArmedStateHandler implements PlaneStateHandler {
    public void takeOff(PlaneContext context, System drone) {
        // Perform takeoff logic
        // Transition to next state
        context.setState(new TakeoffStateHandler());
    }

    public void land(PlaneContext context, System drone) {
        throw new IllegalStateException("Cannot land: not airborne");
    }
}
```

**Pros**:
- Follows Open/Closed Principle (add new states without modifying existing)
- Each state handler is independently testable
- State-specific logic is encapsulated
- Natural fit for Phase 5's complex state machine
- Mirrors ArduPilot's mode-based architecture
- States can manage their own transitions (like real FSM)

**Cons**:
- More classes to manage
- Slightly more complex initial setup
- May feel like over-engineering for just 2-3 states

**Verdict**: Best for long-term maintainability and scales naturally to Phase 5

---

## Hybrid Recommendation: Pragmatic Approach

Given that you're at Phase 2 but need to evolve to Phase 5, here's a pragmatic middle ground:

### 1. Define States as Enum (for type safety)
```java
public enum PlaneState {
    IDLE,           // System ready, no mission
    PREFLIGHT,      // Running safety checks
    ARMED,          // Armed but on ground
    TAKEOFF,        // Climbing to cruise altitude
    TRANSIT,        // Flying to target approach point
    ATTACK_APPROACH,// Setting up for dive
    DIVING,         // Executing 45° dive
    PULLING_OUT,    // Recovering from dive
    CLIMBING,       // Returning to cruise altitude
    RTL,            // Returning to launch
    LANDING,        // Final descent
    ABORTED         // Emergency abort active
}
```

### 2. Create State Handler Interface
```java
public interface PlaneStateHandler {
    void onEnter(PlaneContext context, System drone);
    void onExit(PlaneContext context, System drone);
    void takeOff(PlaneContext context, System drone);
    void land(PlaneContext context, System drone);
    void abort(PlaneContext context, System drone);
    PlaneState getState();
}
```

### 3. Abstract Base Handler (DRY principle)
```java
public abstract class AbstractPlaneStateHandler implements PlaneStateHandler {
    protected final PlaneState state;

    protected AbstractPlaneStateHandler(PlaneState state) {
        this.state = state;
    }

    @Override
    public PlaneState getState() {
        return state;
    }

    @Override
    public void onEnter(PlaneContext context, System drone) {
        // Default: do nothing
    }

    @Override
    public void onExit(PlaneContext context, System drone) {
        // Default: do nothing
    }

    @Override
    public void takeOff(PlaneContext context, System drone) {
        throw new IllegalStateException("Cannot takeoff from state: " + state);
    }

    @Override
    public void land(PlaneContext context, System drone) {
        throw new IllegalStateException("Cannot land from state: " + state);
    }

    @Override
    public void abort(PlaneContext context, System drone) {
        // All states should support abort
        context.setState(PlaneState.ABORTED);
        // RTL logic here
    }
}
```

### 4. Context Class (PlaneServiceImpl becomes simpler)
```java
@Service
public class PlaneServiceImpl implements PlaneService {

    private PlaneStateHandler currentStateHandler;
    private final Map<PlaneState, PlaneStateHandler> stateHandlers;

    public PlaneServiceImpl() {
        // Initialize all state handlers
        stateHandlers = Map.ofEntries(
            entry(PlaneState.IDLE, new IdleStateHandler()),
            entry(PlaneState.ARMED, new ArmedStateHandler()),
            entry(PlaneState.TAKEOFF, new TakeoffStateHandler()),
            // ... other handlers
        );

        currentStateHandler = stateHandlers.get(PlaneState.IDLE);
    }

    @Override
    public void takeOff(System drone) {
        currentStateHandler.takeOff(this, drone);
    }

    @Override
    public void land(System drone) {
        currentStateHandler.land(this, drone);
    }

    public void setState(PlaneState newState) {
        PlaneStateHandler newHandler = stateHandlers.get(newState);

        currentStateHandler.onExit(this, drone);
        currentStateHandler = newHandler;
        newHandler.onEnter(this, drone);

        log.info("State transition: {} -> {}",
                 currentStateHandler.getState(), newState);
    }

    public PlaneState getCurrentState() {
        return currentStateHandler.getState();
    }
}
```

### 5. Concrete State Handlers (Start with what you need for Phase 2)

```java
@Component
public class ArmedStateHandler extends AbstractPlaneStateHandler {

    public ArmedStateHandler() {
        super(PlaneState.ARMED);
    }

    @Override
    public void takeOff(PlaneContext context, System drone) {
        // Phase 2 takeoff logic
        // 1. Switch to FBWA mode
        // 2. Apply throttle
        // 3. Monitor altitude
        // 4. Transition to TAKEOFF state

        context.setState(PlaneState.TAKEOFF);
    }

    // land() throws exception (inherited from abstract class)
}

@Component
public class TakeoffStateHandler extends AbstractPlaneStateHandler {

    public TakeoffStateHandler() {
        super(PlaneState.TAKEOFF);
    }

    @Override
    public void onEnter(PlaneContext context, System drone) {
        // Monitor altitude climb
        // When cruise altitude reached, transition to next state
    }

    @Override
    public void land(PlaneContext context, System drone) {
        // Can abort takeoff and land
        context.setState(PlaneState.LANDING);
    }
}
```

---

## Recommended Package Structure

```
org.example.plane/
├── PlaneService.java (interface)
├── PlaneServiceImpl.java (context/orchestrator)
├── state/
│   ├── PlaneState.java (enum)
│   ├── PlaneStateHandler.java (interface)
│   ├── AbstractPlaneStateHandler.java (base class)
│   ├── IdleStateHandler.java
│   ├── ArmedStateHandler.java
│   ├── TakeoffStateHandler.java
│   ├── AirborneStateHandler.java
│   ├── LandingStateHandler.java
│   └── ... (add more as you progress through phases)
└── Plane.java
```

---

## Why This Approach Works

1. **Mirrors ArduPilot's Architecture**: Similar to how ArduPilot has Mode classes
2. **Incremental Development**: Start with 3-4 states for Phase 2, add more for Phase 5
3. **Testability**: Each state handler can be unit tested in isolation
4. **Type Safety**: Enum prevents invalid states
5. **Clear Transitions**: Easy to see which states can transition to which
6. **SRP Compliance**: Each handler has one responsibility
7. **Open/Closed**: Add new states without modifying existing ones

---

## Phase 2 Minimal Implementation

For Phase 2, you only need:
- `IdleStateHandler` - Initial state, waiting for commands
- `ArmedStateHandler` - Armed, ready for takeoff
- `TakeoffStateHandler` - Climbing to cruise altitude
- `AirborneStateHandler` - Cruising/loitering
- `LandingStateHandler` - Descending to land

Then expand to full 12 states in Phase 5.

---

## Alternative: Spring State Machine

For a production-grade solution, consider [Spring State Machine](https://spring.io/projects/spring-statemachine):

**Pros**:
- Enterprise-tested framework
- Built-in transition validation
- Event-driven architecture
- Persistence support
- Guards and actions

**Cons**:
- Learning curve
- May be overkill for MVP
- Another dependency

**Recommendation**: Start with custom State Pattern, migrate to Spring State Machine if complexity increases post-MVP.

---

## Summary

**For Phase 2 → Phase 5 Evolution**:
1. Use **State Pattern with handlers** (not simple switch/case)
2. Define all states upfront in enum (based on Phase 5 requirements)
3. Implement only the handlers you need for current phase
4. `PlaneServiceImpl` becomes a thin context/orchestrator
5. Each state handler encapsulates its behavior and transitions
6. This mirrors production UAV systems like ArduPilot

**Start Simple, Scale Gracefully**: Don't implement all 12 states now, but architect for them.

---

## Sources
- [ArduPilot Drone Control Stack](https://nrotella.github.io/journal/ardupilot-drone-control-stack.html)
- [ArduCopter Flight Controllers](https://deepwiki.com/ArduPilot/ardupilot/3.1-arducopter)
- [Event-Driven State Machine for Autonomous Drones](https://medium.com/@abhiojha8/flying-an-autonomous-drone-using-event-driven-state-machine-e0e1fb756d54)
- [State Pattern in Java - Baeldung](https://www.baeldung.com/java-state-design-pattern)
- [State Pattern vs Strategy Pattern](https://www.baeldung.com/cs/design-state-pattern-vs-strategy-pattern)
- [State Design Pattern - Refactoring Guru](https://refactoring.guru/design-patterns/state)
- [How to Use State Machines - State Pattern](https://blogs.itemis.com/en/how-to-use-state-machines-for-your-modeling-part-5-the-state-pattern)

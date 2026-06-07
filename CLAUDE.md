# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

JavaFX desktop simulation of an automated gardening system. The UI is only an operator
dashboard — all simulation rules live in backend modules so the project can be exercised
headlessly through `GardenSimulationAPI` without launching JavaFX. Java 17, Maven, JavaFX 21.

## Commands

Maven uses JavaFX 21, which requires JDK 17+. Set `JAVA_HOME` if the default JDK is older:

```bash
# Run the game-style garden board (configured mainClass in pom.xml)
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn javafx:run

# Compile everything
mvn compile
```

There is no JUnit test suite. The two classes under `src/test/java/garden/` are
**standalone `main()` drivers**, not unit tests, and `mvn test` does not run them. Run the
headless API path (no JavaFX needed — exclude the `app` package from compilation):

```bash
javac -d /tmp/garden-classes $(find src/main/java -path 'src/main/java/garden/app' -prune -o -name '*.java' -print)
javac -cp /tmp/garden-classes -d /tmp/garden-classes src/test/java/garden/ManualApiCheck.java
java -cp /tmp/garden-classes garden.ManualApiCheck      # single scripted day-by-day check
java -cp /tmp/garden-classes garden.StabilityCheck      # 24 random days, seeded
```

Both drivers write results to `log.txt` (overwritten on each `initializeGarden()`).

## Architecture

The flow is **API → SimulationEngine → GardenModules → Garden/Plant model**, with every
step appended to `log.txt`.

### Two `GardenSimulationAPI` classes (intentional duplication)
- `GardenSimulationAPI` in the **default package** (`src/main/java/GardenSimulationAPI.java`)
  is the entry point a grading script instantiates via `new GardenSimulationAPI()`. It is a
  thin wrapper that catches **every `Throwable`** on every method — the grader awards 0 on a
  crash, so no exception may escape. `getPlants()` returns an empty-but-shaped map on failure.
- `garden.api.GardenSimulationAPI` holds the real logic and delegates to `SimulationEngine`.

When changing the API surface, keep both classes in sync. Required methods:
`initializeGarden()`, `getPlants()`, `rain(int)`, `temperature(int)`, `parasite(String)`, `getState()`.

### SimulationEngine (`garden.core`)
Orchestrator and single source of truth for a run. Each `submitEvent(...)`:
1. `advanceDay()` on the shared `SimulationContext` (the day counter + log facade),
2. plants consume daily water,
3. every `GardenModule.handleEvent(...)` runs (wrapped in try/catch — a module throwing
   logs `MODULE_ERROR` but does not abort the day),
4. `runDailyUpdate(...)` calls each module's `dailyUpdate(...)`, re-evaluates water stress,
   and writes the `DAY_END` summary.

`initialize()` loads from `garden_config.json`; `initializeWith(Map<PlantType,Integer>)` is a
GUI-only override that builds a custom garden. `ensureInitialized()` lazily initializes so API
calls never NPE if `initializeGarden()` was skipped.

### Modules (`garden.module`)
`WateringSystem`, `TemperatureControlSystem`, `PestControlSystem`, `FertilizerSystem` each
implement `GardenModule` (`handleEvent` + `dailyUpdate`). They are **independent and
order-sensitive** — the engine runs them in the fixed list order defined in the
`SimulationEngine` constructor. Add a new automation system by implementing `GardenModule`
and adding it to that list.

### Events (`garden.event`)
`GardenEvent` (name + value) with implementations `RainEvent`, `TemperatureEvent`,
`ParasiteEvent`, `ManualEvent`. Modules switch on the event's `name()`.

### Model (`garden.model`)
`PlantType` is an enum holding all per-variety tuning (water requirement, min/max temperature,
vulnerable parasites, recovery rate) — this is where plant balancing lives. `Plant` holds
mutable per-instance state (health, water, status, active parasites, death reason). `Garden`
is the plant collection. `GardenSnapshot` is an immutable view the JavaFX UI renders from
(the UI never touches model objects directly).

### Two JavaFX apps (`garden.app`)
- `GardenGame` — animated game-style board; this is the `mainClass` Maven runs.
- `GardenApp` — table/control dashboard variant.

Both drive the same `SimulationEngine` and only read `GardenSnapshot`. Styling is in
`src/main/resources/garden/app/garden.css`.

### Logging (`garden.logging`)
`GardenLogger` writes a CSV `log.txt` with header
`TIMESTAMP,DAY,EVENT,EVENT_VALUE,MODULE,ACTION,PLANTS_ALIVE,PLANTS_DEAD,DETAILS`. `reset()`
truncates and rewrites the header (called on every initialize). All fields are quoted/sanitized;
blank values become `-`. This file is the primary artifact graders inspect.

## Configuration

`garden_config.json` (project root, loaded by relative path) seeds the garden via a `plants`
array of `{ "name", "amount" }`. If missing/unreadable, `GardenConfigLoader` falls back to
defaults and guarantees ≥10 living plants with every variety represented. `parasite()` accepts
specific names (`aphid`, `mite`) and generic terms (`insects`), which map to each plant's own
vulnerabilities.

# Java Simulated Garden

Computerized Garden is a JavaFX desktop simulation for an automated gardening system.
The UI is only an operator dashboard; the simulation rules live in backend modules so the
project can also be tested through `GardenSimulationAPI` without launching JavaFX.

## Main Features

- JavaFX dashboard with plant table, event controls, and live log viewer.
- Game-style JavaFX garden board with animated plant rendering.
- Standalone `GardenSimulationAPI` for instructor automation scripts.
- Modular backend with watering, temperature control, pest control, and fertilizer systems.
- Config-driven garden setup from `garden_config.json`.
- Detailed CSV-style `log.txt` with day, event, module action, alive plants, dead plants, sensor readings, and details.
- Multiple plant varieties with different water, temperature, parasite, and recovery needs.

## API

The project provides both:

- `garden.api.GardenSimulationAPI`
- default-package `GardenSimulationAPI`

The default-package class is included for compatibility with simple grading scripts.

Required methods:

```java
void initializeGarden()
Map<String, Object> getPlants()
void rain(int amount)
void temperature(int temperature)
void parasite(String parasite)
void getState()
```

## Configuration

The garden is seeded from `garden_config.json` in the project root using a relative path.
Edit the `plants` array to change varieties and counts:

```json
{ "plants": [ { "name": "Rose", "amount": 5 }, { "name": "Tomato", "amount": 5 } ] }
```

If the file is missing or unreadable, the loader falls back to sensible defaults
and still guarantees at least 10 living plants with every variety represented.

`parasite()` accepts both specific parasite names, such as `aphid` and `mite`,
and generic terms, such as `insects`. Generic terms are mapped to each plant's own
vulnerabilities so the event has a realistic effect.

## Run

With Maven installed:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 mvn javafx:run
```

Core API compilation check without JavaFX:

```bash
javac -d /tmp/garden-classes $(find src/main/java -path 'src/main/java/garden/app' -prune -o -name '*.java' -print)
javac -cp /tmp/garden-classes -d /tmp/garden-classes src/test/java/garden/ManualApiCheck.java
java -cp /tmp/garden-classes garden.ManualApiCheck
```

## Project Layout

```text
src/main/java/garden/api       API adapter
src/main/java/garden/app       JavaFX dashboard and game UI
src/main/java/garden/core      Simulation engine and context
src/main/java/garden/model     Plant and garden domain objects
src/main/java/garden/event     Simulation events
src/main/java/garden/module    Independent automation modules
src/main/java/garden/logging   Log writer
src/main/java/garden/config    Config loader
```

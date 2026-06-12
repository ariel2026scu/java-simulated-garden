# Requirements & OO Analysis

## 1. Problem Statement

Modern home and commercial gardening faces a fundamental challenge: plants require consistent,
expert care across multiple environmental dimensions — watering, temperature, nutrients, and
pest management — that human gardeners cannot always provide around the clock. Missed watering
cycles, unexpected frosts, or undetected insect infestations can destroy an entire growing
season in days.

This project designs and implements a **Computerized Garden Simulation System** that
autonomously monitors garden conditions, responds to environmental events (rain, temperature
changes, parasite outbreaks), and keeps a detailed audit log of every occurrence. A JavaFX
dashboard allows human gardeners to observe the garden state and trigger manual interventions,
while a standalone API enables automated third-party testing of garden survival over a
simulated 24-day period.

---

## 2. Feature List

### Core Garden Features
- **Multi-variety plant support**: 8 distinct plant species (Rose, Tomato, Lettuce, Cactus,
  Sunflower, Basil, Pepper, Lavender), each with unique water needs, temperature tolerance,
  parasite susceptibility, and recovery rate.
- **Plant lifecycle**: Plants progress through states — HEALTHY → STRESSED / INFESTED →
  RECOVERING → DEAD — based on environmental conditions.
- **Config-file seeding**: Garden initialized from a JSON config file
  (`garden_config.json`), ensuring at least 10 plants and all varieties are alive at start.
  The GUI startup picker can also seed a custom variety→count garden (`initializeWith`).
- **Manual plant addition**: Human gardeners can add individual plants via the GUI.
- **Manual plant removal**: Human gardeners can remove a single selected plant, several at
  once (Ctrl/Shift multi-select on the table or the board), remove a plant straight from a
  board tile via its right-click context menu, or clear **all dead plants** in one click.
- **Garden Defense Board**: Plants hold a stable board slot. Removing a plant frees its slot
  and leaves a visible gap instead of shuffling later plants forward; a later add reuses the
  lowest free slot.

### Autonomous Modules (≥ 3 required)
| Module | Responsibility |
|---|---|
| **WateringSystem** | Captures rainfall; auto-irrigates under-watered plants daily |
| **TemperatureControlSystem** | Mitigates extreme heat/cold; evaluates per-plant stress |
| **PestControlSystem** | Records infestations; deploys treatment after 2 days; does not restore full health |
| **FertilizerSystem** | Monitors soil nutrients; applies fertilizer when low; supports gradual healing |

### API (Grading Script Interface)
- `initializeGarden()` — seeds garden from config; starts simulation clock
- `getPlants()` — returns names, water requirements, and parasites of all alive plants
- `rain(int)` — simulates rainfall; adjusts water levels
- `temperature(int)` — sets daily temperature (40–120°F); resets after each day
- `parasite(String)` — triggers infestation on susceptible plants
- `getState()` — logs full per-plant status snapshot to `log.txt`

### Logging
- Structured CSV log (`log.txt`) with columns:
  `TIMESTAMP, DAY, EVENT, EVENT_VALUE, MODULE, ACTION, PLANTS_ALIVE, PLANTS_DEAD, DETAILS`
- Every event, module response, and daily update is recorded.
- `getState()` emits one row per alive plant and one per dead plant for easy grader review.

### User Interface (JavaFX)
Single `GardenShell` window with two tabs over one shared engine:
- **Living Garden** (`GameView`) — animated autonomous board that advances days on a timer
  (1x/5x/20x speed), draws cartoon plants, sprinklers, drones, shade/heat panels, and a
  startup plant picker.
- **Admin Dashboard** (`DashboardView`) — data view with:
  - Live plant table, colour-coded rows by health status, multi-select enabled
  - Garden Defense Board of slot tiles (click to select, right-click to remove)
  - Summary bar: day, alive/dead counts, soil nutrients, inside/outside temperature, log path
  - Controls: Rain (up to 200mm), Set Temperature, Parasite, Advance Day, Log State,
    Reset Garden, Add Plant, **Remove Selected**, **Remove All Dead** (disabled when nothing
    is dead), and **Open Log File**
  - Live `log.txt` viewer

---

## 3. Use Cases (User Stories & Scenarios)

### UC-1: Initialize Garden
**As a** grading script or gardener,  
**I want to** call `initializeGarden()`,  
**so that** the garden is populated with at least 10 plants of all varieties and the simulation clock begins.

**Main Scenario:**
1. System reads `garden_config.json`.
2. Creates plants per the config; adds any missing varieties to meet minimum variety requirement.
3. Guarantees ≥ 10 alive plants.
4. Logs GARDEN_CREATED to `log.txt`.

**Alternative:** Config file missing → falls back to default plant counts; logs warning.

---

### UC-2: Simulate Rainfall
**As a** grading script,  
**I want to** call `rain(25)`,  
**so that** all alive plants receive extra water and the WateringSystem records the event.

**Main Scenario:**
1. WateringSystem receives RainEvent(25).
2. Each alive plant's water level increases by 25 (capped at 3× daily requirement).
3. RAIN_CAPTURED logged.
4. End-of-day auto-irrigation skips already-saturated plants.

---

### UC-3: Temperature Stress
**As a** grading script,  
**I want to** call `temperature(111)`,  
**so that** heat-sensitive plants take stress damage and the climate module mitigates extreme values.

**Main Scenario:**
1. TemperatureControlSystem receives TemperatureEvent(111) (clamped to the 40–120°F range).
2. System applies a partial mitigation — reduces by 10°F when > 95°F, or warms by 12°F when
   < 50°F — and records both the raw "outside" and conditioned "inside" reading for the UI.
3. End of day: each plant's `evaluateTemperature` checks against its min/max range.
4. Heat-tolerant plants (Cactus, Lavender) survive; heat-sensitive (Lettuce, Rose) take damage.
5. Temperature resets to default (72°F) after the day ends; the last observed reading persists
   so the UI does not snap back to 72°F a moment after each event.

---

### UC-4: Parasite Infestation
**As a** grading script,  
**I want to** call `parasite("aphid")`,  
**so that** susceptible plants are infested, take ongoing damage for 2 days, then are treated — but do not fully heal immediately.

**Main Scenario:**
1. PestControlSystem records infestation; deals 6 HP initial contact damage to each susceptible plant.
2. Day 1 update: parasites age; apply 10–15 HP ongoing damage.
3. Day 2 update: parasites reach TREATMENT_DAYS; PestControlSystem removes them.
4. Plants enter RECOVERING state; FertilizerSystem gradually restores health over subsequent days.

**Key constraint:** Plants never jump back to 100 HP when treated.

---

### UC-5: Monitor Garden State
**As a** grading script (called after 24 simulated days),  
**I want to** call `getState()`,  
**so that** the log shows which plants survived and why others died.

**Main Scenario:**
1. Engine logs one GARDEN_SNAPSHOT summary row.
2. Logs one PLANT_STATUS row per alive plant (type, health, water, status, parasites).
3. Logs one PLANT_DEAD row per dead plant (type, death reason).

---

### UC-6: Human Gardener Adds a Plant
**As a** human gardener using the GUI,  
**I want to** select "Basil" from the dropdown and click "Add Plant",  
**so that** a new Basil plant is added to the garden mid-simulation.

**Main Scenario:**
1. Gardener selects plant type from ComboBox.
2. Clicks "Add Plant".
3. Engine creates plant, adds it to garden, logs PLANT_ADDED.
4. Table refreshes and shows the new plant highlighted in green (HEALTHY).

---

### UC-8: Human Gardener Removes Plants
**As a** human gardener using the GUI,  
**I want to** select one or more plants and click "Remove Selected" (or right-click a board
tile and choose "Remove"),  
**so that** unwanted or crowded plants leave the garden.

**Main Scenario:**
1. Gardener Ctrl/Shift-clicks rows in the plant table or tiles on the Garden Defense Board.
2. Clicks "Remove Selected" (or uses a tile's right-click "Remove" item).
3. Engine removes each plant, frees its board slot (leaving a gap), and logs PLANT_REMOVED.
4. Table and board refresh.

**Alternative:** Nothing selected → "Remove Selected" is disabled.

---

### UC-9: Clear All Dead Plants
**As a** human gardener using the GUI,  
**I want to** click "Remove All Dead",  
**so that** every dead plant is cleared from the board in one action.

**Main Scenario:**
1. Gardener clicks "Remove All Dead".
2. Engine removes every dead plant and logs DEAD_PLANTS_CLEARED with the count.
3. Board and table refresh.

**Alternative:** No dead plants → the button is disabled.

---

### UC-10: Garden Survives 24-Hour Automated Test
**As a** grader running the monitoring script,  
**I want** the garden to run for 24 simulated days without crashing and with plants alive at the end,  
**so that** the autonomous systems can be validated.

**Main Scenario:**
1. `initializeGarden()` called once.
2. Each simulated hour: `rain()`, `temperature()`, or `parasite()` is called randomly.
3. All exceptions are caught and logged; the simulation never terminates.
4. After 24 calls, `getState()` reveals a surviving, active garden.

---

## 4. Non-Functional Requirements

- **Robustness**: All RuntimeExceptions in module execution are caught and logged; no event
  can crash the simulation.
- **Persistence**: Log file uses relative paths only; portable across machines.
- **Extensibility**: New plant types require only a new `PlantType` enum entry + config update.
  New modules require only implementing `GardenModule` and adding to the engine's list.
- **Readability**: Log is CSV-formatted with a descriptive header row; graders can open it
  in any spreadsheet tool and filter by DAY, MODULE, or ACTION.

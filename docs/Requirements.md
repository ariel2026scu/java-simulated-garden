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
- **Manual plant addition**: Human gardeners can add individual plants via the GUI.

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
- Live plant table with colour-coded rows by health status
- Summary bar: day, alive/dead counts, soil nutrients, temperature, log path
- Controls: Rain, Temperature, Trigger Parasite, Advance Day, Log State, Reset, Add Plant

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
1. TemperatureControlSystem receives TemperatureEvent(111).
2. System applies a partial mitigation (reduces by 10°F for values > 95°F).
3. End of day: each plant's `evaluateTemperature` checks against its min/max range.
4. Heat-tolerant plants (Cactus, Lavender) survive; heat-sensitive (Lettuce, Rose) take damage.
5. Temperature resets to default (72°F) after the day ends.

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

### UC-7: Garden Survives 24-Hour Automated Test
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

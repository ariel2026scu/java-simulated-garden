# User Manual

The project ships with **two interchangeable JavaFX front-ends** over the same
simulation backend. Both write to the same `log.txt`.

| Front-end | Class | Best for |
|-----------|-------|----------|
| Living Garden (animated) | `garden.app.GardenGame` | Demos / presentation — autonomous animated garden |
| Operator Dashboard (data) | `garden.app.GardenApp` | Inspecting plant tables, status counts, and the live log |

The Maven launcher (`mvn javafx:run`) starts whichever class is set as
`<mainClass>` in `pom.xml`. To switch, edit that one line and re-run.

> Always launch with `mvn javafx:run` (not a plain "Run" of the class), otherwise
> the JDK reports "JavaFX runtime components are missing".

---

## A. Living Garden (`GardenGame`)

### Startup: choose your plants
On launch a setup screen appears. Each of the eight varieties has a count
spinner (0–40), pre-filled from `garden_config.json` and annotated with that
plant's water need and comfortable temperature band.

- **🌱 Plant garden** — start the simulation with your chosen counts.
- **Use config defaults** — start straight from `garden_config.json`.
- If every count is 0 the system falls back to the config defaults, so the
  garden never starts empty.

### The running garden
- The garden advances **simulated days automatically on a timer** — no clicking
  required. The automated modules (not the player) decide whether plants live,
  which demonstrates the "survives on its own" requirement.
- Plants are drawn as cartoon vectors that sway while healthy, **wilt and fall
  over when they die**, and pop back up if revived. A health bar floats above
  each living plant; tiny bug markers appear when a plant is infested.

### Module devices (animate automatically from live state)
- **Watering** — a thirsty plant gets a sprinkler nozzle + blue spray arcs and
  falling droplets until its moisture is restored.
- **Temperature control** — above 95 °F shade panels slide across the top
  (cooling); below 55 °F orange heat lamps glow (heating).
- **Pest control** — a drone flies to each infested plant and sprays green mist,
  then leaves once the infestation clears.
- **Fertilizer** — when soil nutrients drop below 45 %, brown granules rain down
  onto the beds.

### Guest-gardener controls (bottom bar)
These inject **disturbances** (challenges, not life support):
- **🌧 Rain** — adds water to every plant (a very large pour can overwater).
- **🔥 Heat Wave** / **❄ Cold Snap** — push the temperature to an extreme.
- **🐛 Pest Outbreak** — infests vulnerable plants with a random parasite.
- **📋 Log State** — writes a full garden snapshot to `log.txt`.

### Time speed & milestones
- **1x / 5x / 20x** — change how fast simulated days pass. 20x lets a grader
  watch dozens of autonomous days in a couple of minutes.
- The HUD shows the current day, alive/dead counts, soil %, and temperature.
  Every 5 surviving days a milestone toast appears.

---

## B. Operator Dashboard (`GardenApp`)

- The top summary shows the simulated day, alive/dead plants, soil nutrients,
  and ambient temperature.
- The center board is a lane-based lawn; each tile shows a plant marker, health
  bar, name, and condition.
- The right panel shows status totals, a detailed plant table, and the log path.
- The lower panel mirrors `log.txt`.

Controls: `Advance Day`, `Rain`, `Set Temperature`, `Parasite`, `Log State`,
`Reset Garden`, and `Add Plant` (with a variety picker).

---

## Demonstrating that plants genuinely live AND die

The garden is **not** hardcoded to survive. Survival is produced by the modules;
remove or overwhelm them and plants die. To show both behaviors:

1. **Self-survival** — start the garden and let it run untouched at 5x/20x.
   Water drops each day and the watering module refills it *before* the stress
   check, so healthy plants stay at full health. Watch the day counter climb.
2. **Death by pests** — click **🐛 Pest Outbreak** several times. Infested
   plants take damage for 2 days before pest control clears them, and treatment
   does **not** restore lost health, so repeated waves wear plants down to DEAD
   (they wilt and fall over).
3. **Death by overwatering** — pour a very large **🌧 Rain**; plants above twice
   their water need take "overwatering" damage.
4. **Death by climate** — trigger a **🔥 Heat Wave** / **❄ Cold Snap** beyond a
   plant's tolerance to see "heat/cold stress" damage before the climate module
   pulls the temperature back.

Death reasons (`dehydration`, `low water`, `overwatering`, `cold/heat stress`,
`parasite infestation`) are all recorded in `log.txt`.

---

## Log Format

`log.txt` uses this header:

```text
TIMESTAMP,DAY,EVENT,EVENT_VALUE,MODULE,ACTION,PLANTS_ALIVE,PLANTS_DEAD,DETAILS
```

- Use `DAY_END` rows to check whether the garden survived each simulated day.
- Use the `MODULE` and `ACTION` columns to see how each automated subsystem
  responded (e.g. `WateringSystem / AUTO_IRRIGATION`,
  `PestControlSystem / PEST_MANAGEMENT`).

---

## 24-hour survival run

The graded long-run uses the monitoring **API** (`GardenSimulationAPI`), which
initializes from `garden_config.json` and keeps the minimum-variety / minimum-10
survival guarantees. For that test, run via the API or the `GardenApp`
dashboard. Use `GardenGame` for live demos — at high speed it advances many
simulated days quickly, which also grows `log.txt` quickly.

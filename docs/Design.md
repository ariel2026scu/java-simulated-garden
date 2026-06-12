# Design Notes

## Architecture

```text
GardenShell (JavaFX)
  ├─ GameView (Living Garden) ─┐
  └─ DashboardView (Admin) ────┤ both observe one shared engine via GardenSnapshot
                               ▼
                       SimulationEngine
                           -> Garden -> Plant / PlantType
                           -> GardenModule implementations
                           -> SimulationContext
                           -> GardenLogger (log.txt)
```

The JavaFX UI does not modify plant health, water, parasites, or logs directly. It reads
immutable `GardenSnapshot`s; all state changes go through `SimulationEngine`. The two tabs
cross-refresh each other on any change.

## Core Classes

- `Garden`: owns plants and environmental state, including the last observed (conditioned)
  and last raw event temperatures, and assigns each plant a stable board slot.
- `Plant`: owns health, water, status, parasite state, death reason, and its board slot.
- `PlantType`: defines scientific-style needs for each variety.
- `SimulationEngine`: coordinates days, events, modules, snapshots, logging, plant add/remove,
  and bulk dead-plant clearing.
- `GardenModule`: interface used by all automation modules.
- `GardenSnapshot`: immutable view (with `PlantView.slotIndex` and inside/outside temperature)
  that the JavaFX layer renders from — the UI never touches model objects directly.
- `GardenSimulationAPI`: standalone API required by the assignment.

## Board slots and removal

The Garden Defense Board lays plants out by a stable `boardSlot` index rather than list order.
`Garden.addPlant` assigns the lowest free slot; `removePlant` frees it. Removing a plant
therefore leaves a visible gap instead of shuffling later plants forward, while the underlying
`plants` list stays gapless so modules never see holes. The engine exposes `removePlant(name)`
(single / multi-select) and `removeDeadPlants()` (bulk), both logged.

## Modules

- `WateringSystem`: captures rain and performs automatic irrigation.
- `TemperatureControlSystem`: mitigates heat/cold and resets daily temperature.
- `PestControlSystem`: applies parasite vulnerability and targeted treatment.
- `FertilizerSystem`: monitors soil nutrients and supports gradual recovery.

## Extensibility

To add a module:

1. Implement `GardenModule`.
2. Add the module to the `modules` list in `SimulationEngine`.
3. Keep UI changes optional by exposing new state through `GardenSnapshot`.

To add a plant:

1. Add a new `PlantType`.
2. Add it to `garden_config.json`.
3. No UI or API changes are required.

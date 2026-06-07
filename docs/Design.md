# Design Notes

## Architecture

```text
JavaFX UI
  -> SimulationEngine
      -> Garden
      -> GardenModule implementations
      -> GardenLogger
```

The JavaFX UI does not modify plant health, water, parasites, or logs directly.
All state changes go through `SimulationEngine`.

## Core Classes

- `Garden`: owns plants and environmental state.
- `Plant`: owns health, water, status, parasite state, and death reason.
- `PlantType`: defines scientific-style needs for each variety.
- `SimulationEngine`: coordinates days, events, modules, snapshots, and logging.
- `GardenModule`: interface used by all automation modules.
- `GardenSimulationAPI`: standalone API required by the assignment.

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

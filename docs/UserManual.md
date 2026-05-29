# User Manual

## Starting the Garden

Run the JavaFX app with `mvn javafx:run`. The system initializes the garden from
`garden_config.json` and creates a fresh `log.txt`.

## Dashboard

- The top summary shows the current simulated day, alive plants, dead plants, soil nutrients,
  and ambient temperature.
- The center garden board uses a lane-based lawn layout. Each tile represents one plant,
  with a plant-color marker, health bar, name, and current condition.
- The right panel shows plant status totals, plant details, and the active log path.
- The lower log panel shows the same event records written to `log.txt`.

## Controls

- `Advance Day`: runs one simulated day with normal automated module behavior.
- `Rain`: sends rainfall to the backend. Large rain can overwater sensitive plants.
- `Set Temperature`: sends a daily temperature event. The climate system mitigates extremes,
  then plants evaluate stress.
- `Parasite`: triggers a named parasite. Vulnerable plants take realistic damage and pest
  control responds without restoring full health.
- `Log State`: writes a current garden snapshot to `log.txt`.
- `Reset Garden`: reloads the config and starts a new log.

## Log Format

`log.txt` uses this header:

```text
TIMESTAMP,DAY,EVENT,EVENT_VALUE,MODULE,ACTION,PLANTS_ALIVE,PLANTS_DEAD,DETAILS
```

Use `DAY_END` rows to quickly evaluate whether the garden survived each simulated day.
Use `MODULE` and `ACTION` columns to see how each automated subsystem responded.

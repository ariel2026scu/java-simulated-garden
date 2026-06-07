package garden.module;

import garden.core.SimulationContext;
import garden.event.GardenEvent;
import garden.event.TemperatureEvent;
import garden.model.Garden;
import garden.model.Plant;

public class TemperatureControlSystem implements GardenModule {
    private static final int DEFAULT_TEMPERATURE = 72;

    @Override
    public String getName() {
        return "TemperatureControlSystem";
    }

    @Override
    public void handleEvent(Garden garden, GardenEvent event, SimulationContext context) {
        if (event instanceof TemperatureEvent temperatureEvent) {
            int rawTemperature = Math.max(40, Math.min(120, temperatureEvent.temperature()));
            int adjustedTemperature = rawTemperature;
            if (rawTemperature < 50) {
                adjustedTemperature += 12;
            } else if (rawTemperature > 95) {
                adjustedTemperature -= 10;
            }
            garden.setAmbientTemperature(adjustedTemperature);
            // Persist the conditioned reading for the UI: ambientTemperature
            // gets reset to the 72°F baseline at end of day (per API spec), so
            // without this the dashboard / game HUD would always read 72°F a
            // moment after the user fires a temperature event.
            garden.setLastObservedTemperature(adjustedTemperature);
            context.log(event.name(), event.value(), getName(), "CLIMATE_RESPONSE", garden,
                    "Outside temperature " + rawTemperature + "F adjusted to " + adjustedTemperature + "F.");
        }
    }

    @Override
    public void dailyUpdate(Garden garden, SimulationContext context) {
        // SENSE: read the ambient temperature sensor before evaluating stress.
        context.log("SENSOR", "day " + context.getDay(), "TemperatureSensor", "READING", garden,
                "Ambient temperature reads " + garden.getAmbientTemperature() + "F.");
        for (Plant plant : garden.getAlivePlants()) {
            plant.evaluateTemperature(garden.getAmbientTemperature());
        }
        context.log("DAILY_UPDATE", "day " + context.getDay(), getName(), "TEMPERATURE_CHECK", garden,
                "Evaluated plant temperature stress at " + garden.getAmbientTemperature() + "F.");
        garden.setAmbientTemperature(DEFAULT_TEMPERATURE);
    }
}

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
            context.log(event.name(), event.value(), getName(), "CLIMATE_RESPONSE", garden,
                    "Outside temperature " + rawTemperature + "F adjusted to " + adjustedTemperature + "F.");
        }
    }

    @Override
    public void dailyUpdate(Garden garden, SimulationContext context) {
        for (Plant plant : garden.getAlivePlants()) {
            plant.evaluateTemperature(garden.getAmbientTemperature());
        }
        context.log("DAILY_UPDATE", "day " + context.getDay(), getName(), "TEMPERATURE_CHECK", garden,
                "Evaluated plant temperature stress at " + garden.getAmbientTemperature() + "F.");
        garden.setAmbientTemperature(DEFAULT_TEMPERATURE);
    }
}

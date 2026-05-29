package garden.module;

import garden.core.SimulationContext;
import garden.event.GardenEvent;
import garden.event.RainEvent;
import garden.model.Garden;
import garden.model.Plant;

public class WateringSystem implements GardenModule {
    @Override
    public String getName() {
        return "WateringSystem";
    }

    @Override
    public void handleEvent(Garden garden, GardenEvent event, SimulationContext context) {
        if (event instanceof RainEvent rainEvent) {
            int amount = Math.max(0, rainEvent.amount());
            for (Plant plant : garden.getAlivePlants()) {
                plant.addWater(amount);
            }
            context.log(event.name(), event.value(), getName(), "RAIN_CAPTURED", garden,
                    "Rain increased water levels by " + amount + " for all living plants.");
        }
    }

    @Override
    public void dailyUpdate(Garden garden, SimulationContext context) {
        // SENSE: read each plant's moisture sensor before the actuator responds.
        int thirsty = 0;
        for (Plant plant : garden.getAlivePlants()) {
            if (plant.getWaterLevel() < plant.getType().getWaterRequirement()) {
                thirsty++;
            }
        }
        context.log("SENSOR", "day " + context.getDay(), "MoistureSensor", "READING", garden,
                thirsty + " of " + garden.getAlivePlants().size()
                        + " living plants read below their target moisture level.");

        // ACT: irrigate the plants the sensor flagged.
        int watered = 0;
        for (Plant plant : garden.getAlivePlants()) {
            int need = plant.getType().getWaterRequirement();
            if (plant.getWaterLevel() < need) {
                int amount = need - plant.getWaterLevel();
                plant.addWater(amount);
                watered++;
            }
        }
        context.log("DAILY_UPDATE", "day " + context.getDay(), getName(), "AUTO_IRRIGATION", garden,
                "Watered " + watered + " plants that were below their target moisture level.");
    }
}

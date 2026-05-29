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

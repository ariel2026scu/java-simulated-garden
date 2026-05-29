package garden.module;

import garden.core.SimulationContext;
import garden.event.GardenEvent;
import garden.model.Garden;
import garden.model.Plant;

public class FertilizerSystem implements GardenModule {
    @Override
    public String getName() {
        return "FertilizerSystem";
    }

    @Override
    public void handleEvent(Garden garden, GardenEvent event, SimulationContext context) {
        // This module reacts during daily updates so UI/API events stay loosely coupled.
    }

    @Override
    public void dailyUpdate(Garden garden, SimulationContext context) {
        garden.changeSoilNutrients(-2);
        if (garden.getSoilNutrients() < 45) {
            garden.changeSoilNutrients(35);
            context.log("DAILY_UPDATE", "day " + context.getDay(), getName(), "FERTILIZER_APPLIED", garden,
                    "Soil nutrients were low; applied controlled fertilizer. New nutrient score: "
                            + garden.getSoilNutrients() + ".");
        } else {
            context.log("DAILY_UPDATE", "day " + context.getDay(), getName(), "SOIL_CHECK", garden,
                    "Soil nutrient score is " + garden.getSoilNutrients() + ".");
        }

        if (garden.getSoilNutrients() >= 50) {
            for (Plant plant : garden.getAlivePlants()) {
                plant.heal(plant.getType().getRecoveryRate());
            }
        }
    }
}

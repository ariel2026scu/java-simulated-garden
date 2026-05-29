package garden.module;

import garden.core.SimulationContext;
import garden.event.GardenEvent;
import garden.event.ParasiteEvent;
import garden.model.Garden;
import garden.model.Plant;
import garden.model.PlantType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PestControlSystem implements GardenModule {
    @Override
    public String getName() {
        return "PestControlSystem";
    }

    /**
     * Records the infestation and deals initial contact damage. Treatment is
     * deliberately delayed — parasites continue to damage the plant for
     * {@link Plant#TREATMENT_DAYS} daily-update cycles before being removed.
     * This ensures pest control "does not heal the plant back to full health
     * upon deployment" as required.
     */
    @Override
    public void handleEvent(Garden garden, GardenEvent event, SimulationContext context) {
        if (event instanceof ParasiteEvent parasiteEvent) {
            String parasite = parasiteEvent.parasite().toLowerCase().trim();
            boolean generic = !isKnownParasite(parasite);
            int infected = 0;
            for (Plant plant : garden.getAlivePlants()) {
                // For a generic term (e.g. "insects", "pests") that isn't one of
                // our defined parasite names, map it to a parasite each plant is
                // actually vulnerable to so the event still has a realistic effect.
                String applied = generic ? firstVulnerability(plant) : parasite;
                if (applied != null && plant.infest(applied)) {
                    infected++;
                    plant.damage(6, "initial contact damage: " + applied);
                }
            }
            String label = generic ? parasite + " (mapped to per-plant vulnerabilities)" : parasite;
            context.log(event.name(), event.value(), getName(), "INFESTATION_DETECTED", garden,
                    "Parasite '" + label + "' infested " + infected + " susceptible plants. "
                    + "Pest control treatment will begin after " + Plant.TREATMENT_DAYS
                    + " day(s) — plants will sustain damage until then.");
        }
    }

    /** True if the term matches a parasite defined on any plant type. */
    private boolean isKnownParasite(String parasite) {
        if (parasite == null || parasite.isBlank()) {
            return false;
        }
        Set<String> known = new HashSet<>();
        for (PlantType type : PlantType.values()) {
            known.addAll(type.getParasites());
        }
        return known.contains(parasite);
    }

    /** The first parasite the plant is vulnerable to, or null if it has none. */
    private String firstVulnerability(Plant plant) {
        List<String> parasites = plant.getType().getParasites();
        return parasites.isEmpty() ? null : parasites.get(0);
    }

    /**
     * Each day: apply ongoing damage to infested plants, age all parasites,
     * then remove those that have been active long enough for treatment to work.
     * Treatment stops further damage but does NOT restore lost health.
     */
    @Override
    public void dailyUpdate(Garden garden, SimulationContext context) {
        int activeBefore = 0;
        int treated = 0;

        for (Plant plant : garden.getAlivePlants()) {
            // 1. Count and apply ongoing damage while still infested
            if (!plant.getActiveParasites().isEmpty()) {
                activeBefore++;
                plant.applyParasiteDamage();
            }
            // 2. Age parasites; collect those ready for treatment
            List<String> ready = plant.ageParasitesAndGetTreatmentReady();
            // 3. Remove mature infestations (plant health is NOT restored)
            for (String p : ready) {
                plant.treatParasite(p);
                treated++;
            }
        }

        context.log("DAILY_UPDATE", "day " + context.getDay(), getName(), "PEST_MANAGEMENT", garden,
                "Active infestations before treatment: " + activeBefore
                + "; parasites removed today by pest control: " + treated
                + ". Affected plants must recover health gradually over time.");
    }
}

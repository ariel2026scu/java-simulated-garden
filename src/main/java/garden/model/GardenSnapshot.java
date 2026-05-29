package garden.model;

import java.util.List;

public record GardenSnapshot(
        int day,
        int alivePlants,
        int deadPlants,
        int soilNutrients,
        int ambientTemperature,
        List<PlantView> plants
) {
    public record PlantView(
            String name,
            String type,
            int health,
            int waterLevel,
            String status,
            String activeParasites,
            String deathReason
    ) {
    }
}

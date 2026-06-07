package garden.model;

import java.util.List;

/**
 * Immutable view the JavaFX layer renders from. {@code ambientTemperature}
 * here is the <i>last observed</i> climate-conditioned reading and persists
 * between events — not the moment-to-moment baseline the simulation snaps
 * back to after each day. See {@link Garden#getLastObservedTemperature()}.
 */
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

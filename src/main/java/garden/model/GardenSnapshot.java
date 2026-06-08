package garden.model;

import java.util.List;

/**
 * Immutable view the JavaFX layer renders from.
 *
 * <p>{@code ambientTemperature} is the <i>climate-conditioned</i> reading the
 * plants actually experienced — what the greenhouse's control system achieved
 * after softening the threat (e.g. 110°F outside → 100°F inside). It persists
 * between events as the "last observed" reading rather than snapping back to
 * the 72°F baseline a moment after each event.
 *
 * <p>{@code outsideTemperature} is the <i>raw</i> un-conditioned value the
 * gardener requested in the most recent temperature event, paired with the
 * conditioned reading above so the UI can show "outside vs inside" side by
 * side and the climate control's effect is visible.
 */
public record GardenSnapshot(
        int day,
        int alivePlants,
        int deadPlants,
        int soilNutrients,
        int ambientTemperature,
        int outsideTemperature,
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

package garden.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Garden {
    private final List<Plant> plants = new ArrayList<>();
    private int soilNutrients = 75;
    private int ambientTemperature = 72;
    private int lastObservedTemperature = 72;
    private int lastEventRawTemperature = 72;

    public void clear() {
        plants.clear();
        soilNutrients = 75;
        ambientTemperature = 72;
        lastObservedTemperature = 72;
        lastEventRawTemperature = 72;
    }

    public void addPlant(Plant plant) {
        plants.add(plant);
    }

    /** Removes the given plant instance. Returns true if it was present. */
    public boolean removePlant(Plant plant) {
        return plants.remove(plant);
    }

    /** Removes the (unique-named) plant matching the given name, if any. */
    public boolean removePlantByName(String name) {
        return plants.removeIf(plant -> plant.getName().equals(name));
    }

    public List<Plant> getPlants() {
        return Collections.unmodifiableList(plants);
    }

    public List<Plant> getAlivePlants() {
        return plants.stream().filter(Plant::isAlive).collect(Collectors.toList());
    }

    public List<Plant> getDeadPlants() {
        return plants.stream().filter(plant -> !plant.isAlive()).collect(Collectors.toList());
    }

    public Map<PlantType, Long> countAliveByType() {
        return getAlivePlants().stream()
                .collect(Collectors.groupingBy(Plant::getType, () -> new EnumMap<>(PlantType.class), Collectors.counting()));
    }

    public int getSoilNutrients() {
        return soilNutrients;
    }

    public void changeSoilNutrients(int delta) {
        soilNutrients = Math.max(0, Math.min(100, soilNutrients + delta));
    }

    public int getAmbientTemperature() {
        return ambientTemperature;
    }

    public void setAmbientTemperature(int ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    /**
     * Most recent climate-conditioned temperature the control system actually
     * responded to. Unlike {@link #ambientTemperature}, this value is NOT reset
     * by the end-of-day update — it persists between events so the UI can show
     * the last reading the gardener saw, instead of always reverting to the
     * 72°F baseline a moment after every event.
     */
    public int getLastObservedTemperature() {
        return lastObservedTemperature;
    }

    public void setLastObservedTemperature(int lastObservedTemperature) {
        this.lastObservedTemperature = lastObservedTemperature;
    }

    /**
     * The most recent outside (un-conditioned) temperature requested via a
     * temperature event, before the climate control system softened it. Paired
     * with {@link #getLastObservedTemperature()} this lets the UI distinguish
     * "this is what the weather threw at us" from "this is what the greenhouse
     * actually exposed the plants to today".
     */
    public int getLastEventRawTemperature() {
        return lastEventRawTemperature;
    }

    public void setLastEventRawTemperature(int lastEventRawTemperature) {
        this.lastEventRawTemperature = lastEventRawTemperature;
    }
}

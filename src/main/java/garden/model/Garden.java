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

    public void clear() {
        plants.clear();
        soilNutrients = 75;
        ambientTemperature = 72;
    }

    public void addPlant(Plant plant) {
        plants.add(plant);
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
}

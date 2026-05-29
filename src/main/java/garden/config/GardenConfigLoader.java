package garden.config;

import garden.model.Garden;
import garden.model.Plant;
import garden.model.PlantType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GardenConfigLoader {
    private static final Pattern PLANT_ENTRY = Pattern.compile("\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"amount\"\\s*:\\s*(\\d+)\\s*}");

    public Garden load(Path configPath) {
        Garden garden = new Garden();
        Map<PlantType, Integer> requestedPlants = readConfig(configPath);
        if (requestedPlants.isEmpty()) {
            requestedPlants = defaultPlants();
        }
        for (Map.Entry<PlantType, Integer> entry : requestedPlants.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                garden.addPlant(new Plant(entry.getKey()));
            }
        }
        ensureMinimumVariety(garden);
        return garden;
    }

    private Map<PlantType, Integer> readConfig(Path configPath) {
        Map<PlantType, Integer> result = new LinkedHashMap<>();
        if (!Files.exists(configPath)) {
            return result;
        }
        try {
            String json = Files.readString(configPath);
            Matcher matcher = PLANT_ENTRY.matcher(json);
            while (matcher.find()) {
                PlantType type = PlantType.fromName(matcher.group(1));
                int amount = Integer.parseInt(matcher.group(2));
                result.put(type, Math.max(0, amount));
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not read garden config, using defaults: " + e.getMessage());
            return new LinkedHashMap<>();
        }
        return result;
    }

    private Map<PlantType, Integer> defaultPlants() {
        Map<PlantType, Integer> defaults = new LinkedHashMap<>();
        defaults.put(PlantType.ROSE, 5);
        defaults.put(PlantType.TOMATO, 5);
        defaults.put(PlantType.LETTUCE, 4);
        defaults.put(PlantType.CACTUS, 3);
        defaults.put(PlantType.SUNFLOWER, 4);
        return defaults;
    }

    private void ensureMinimumVariety(Garden garden) {
        Map<PlantType, Long> aliveByType = garden.countAliveByType();
        for (PlantType type : PlantType.values()) {
            if (!aliveByType.containsKey(type)) {
                garden.addPlant(new Plant(type));
            }
        }
        while (garden.getAlivePlants().size() < 10) {
            garden.addPlant(new Plant(PlantType.ROSE));
        }
    }
}

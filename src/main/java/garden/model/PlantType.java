package garden.model;

import java.util.List;

public enum PlantType {
    ROSE("Rose",       10, 55,  88, List.of("aphid", "mite", "beetle"),           6),
    TOMATO("Tomato",   15, 60,  90, List.of("aphid", "hornworm", "whitefly"),      8),
    LETTUCE("Lettuce", 12, 45,  75, List.of("slug", "aphid", "cutworm"),           5),
    CACTUS("Cactus",    4, 65, 115, List.of("mealybug", "scale"),                  3),
    SUNFLOWER("Sunflower", 14, 55, 95, List.of("beetle", "moth", "aphid"),         7),
    BASIL("Basil",     13, 55,  85, List.of("aphid", "spider_mite", "thrip"),      7),
    PEPPER("Pepper",   11, 60,  95, List.of("aphid", "mite", "thrip"),             6),
    LAVENDER("Lavender", 5, 50, 100, List.of("aphid", "whitefly"),                 4);

    private final String displayName;
    private final int waterRequirement;
    private final int minTemperature;
    private final int maxTemperature;
    private final List<String> parasites;
    private final int recoveryRate;

    PlantType(String displayName, int waterRequirement, int minTemperature, int maxTemperature,
              List<String> parasites, int recoveryRate) {
        this.displayName = displayName;
        this.waterRequirement = waterRequirement;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.parasites = parasites;
        this.recoveryRate = recoveryRate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWaterRequirement() {
        return waterRequirement;
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public List<String> getParasites() {
        return parasites;
    }

    public int getRecoveryRate() {
        return recoveryRate;
    }

    public static PlantType fromName(String name) {
        for (PlantType type : values()) {
            if (type.displayName.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown plant type: " + name);
    }
}

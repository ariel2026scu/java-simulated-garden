package garden.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Plant {
    /**
     * Number of daily-update cycles a parasite stays active before pest control
     * removes it. Plants take damage for this many days; treatment does NOT restore
     * lost health, it only stops further infestation damage.
     */
    public static final int TREATMENT_DAYS = 2;

    private static final AtomicInteger IDS = new AtomicInteger(1);

    private final int id;
    private final PlantType type;
    private int health;
    private int waterLevel;
    private PlantStatus status;
    /** Maps parasite name → number of daily-update cycles it has been active. */
    private final Map<String, Integer> parasiteAge = new LinkedHashMap<>();
    private String deathReason;
    /**
     * Stable position on the Garden Defense Board, assigned by {@link Garden}
     * when the plant is added. Removing a plant frees its slot; a later add
     * reuses the lowest free slot, so deletions leave a visible gap rather than
     * shuffling every later plant forward. -1 until assigned.
     */
    private int boardSlot = -1;

    public Plant(PlantType type) {
        this.id = IDS.getAndIncrement();
        this.type = type;
        this.health = 100;
        this.waterLevel = type.getWaterRequirement();
        this.status = PlantStatus.HEALTHY;
        this.deathReason = "";
    }

    public int getId() { return id; }
    public PlantType getType() { return type; }
    public int getBoardSlot() { return boardSlot; }
    public void setBoardSlot(int boardSlot) { this.boardSlot = boardSlot; }
    public String getName() { return type.getDisplayName() + "-" + id; }
    public int getHealth() { return health; }
    public int getWaterLevel() { return waterLevel; }
    public PlantStatus getStatus() { return status; }
    public String getDeathReason() { return deathReason; }
    public boolean isAlive() { return status != PlantStatus.DEAD; }

    public List<String> getActiveParasites() {
        return Collections.unmodifiableList(List.copyOf(parasiteAge.keySet()));
    }

    public void addWater(int amount) {
        if (!isAlive()) return;
        waterLevel = clamp(waterLevel + Math.max(0, amount), 0, type.getWaterRequirement() * 3);
    }

    public void consumeDailyWater() {
        if (!isAlive()) return;
        waterLevel = Math.max(0, waterLevel - type.getWaterRequirement());
    }

    public void damage(int amount, String reason) {
        if (!isAlive()) return;
        health = clamp(health - Math.max(0, amount), 0, 100);
        if (health == 0) {
            status = PlantStatus.DEAD;
            deathReason = reason;
        } else if (!parasiteAge.isEmpty()) {
            status = PlantStatus.INFESTED;
        } else if (health < 70) {
            status = PlantStatus.STRESSED;
        }
    }

    public void heal(int amount) {
        if (!isAlive() || !parasiteAge.isEmpty()) return;
        health = clamp(health + Math.max(0, amount), 0, 100);
        status = health >= 85 ? PlantStatus.HEALTHY : PlantStatus.RECOVERING;
    }

    /**
     * Infects this plant with the given parasite, if the plant is alive and
     * susceptible. Returns true if a new infestation was recorded.
     */
    public boolean infest(String parasite) {
        if (!isAlive() || !type.getParasites().contains(parasite.toLowerCase())) return false;
        parasiteAge.putIfAbsent(parasite.toLowerCase(), 0);
        status = PlantStatus.INFESTED;
        return true;
    }

    /**
     * Ages all active parasites by one day and returns those that have reached
     * {@link #TREATMENT_DAYS} — i.e. are ready to be removed by pest control.
     * Called once per daily-update cycle by {@code PestControlSystem}.
     */
    public List<String> ageParasitesAndGetTreatmentReady() {
        parasiteAge.replaceAll((k, v) -> v + 1);
        return parasiteAge.entrySet().stream()
                .filter(e -> e.getValue() >= TREATMENT_DAYS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Removes a parasite that has been treated. Does NOT restore health — the
     * plant must recover naturally over time via the fertilizer/healing system.
     */
    public boolean treatParasite(String parasite) {
        boolean removed = parasiteAge.remove(parasite.toLowerCase()) != null;
        if (removed && isAlive() && parasiteAge.isEmpty()) {
            status = health >= 70 ? PlantStatus.RECOVERING : PlantStatus.STRESSED;
        }
        return removed;
    }

    public void evaluateWaterStress() {
        if (!isAlive()) return;
        int need = type.getWaterRequirement();
        if (waterLevel == 0) {
            damage(18, "dehydration");
        } else if (waterLevel < need / 2) {
            damage(8, "low water");
        } else if (waterLevel > need * 2) {
            damage(7, "overwatering");
        }
    }

    public void evaluateTemperature(int temperature) {
        if (!isAlive()) return;
        if (temperature < type.getMinTemperature()) {
            damage(Math.min(20, (type.getMinTemperature() - temperature) / 2 + 5), "cold stress");
        } else if (temperature > type.getMaxTemperature()) {
            damage(Math.min(22, (temperature - type.getMaxTemperature()) / 2 + 5), "heat stress");
        }
    }

    /** Applies ongoing damage from all currently active parasites. */
    public void applyParasiteDamage() {
        if (!isAlive() || parasiteAge.isEmpty()) return;
        damage(10 + parasiteAge.size() * 5, "parasite infestation: " + parasiteAge.keySet());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

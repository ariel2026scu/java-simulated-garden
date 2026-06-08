package garden.event;

/**
 * Single API-level temperature event. The {@link #name()} the log records is
 * classified by value so the EVENT column is grep-able: {@code HEAT_WAVE}
 * above 95°F, {@code COLD_SNAP} below 55°F, otherwise {@code TEMPERATURE}.
 * Modules still dispatch on {@code instanceof TemperatureEvent}, not on the
 * name string, so this classification is purely for log readability.
 */
public record TemperatureEvent(int temperature) implements GardenEvent {
    @Override
    public String name() {
        if (temperature > 95) return "HEAT_WAVE";
        if (temperature < 55) return "COLD_SNAP";
        return "TEMPERATURE";
    }

    @Override
    public String value() {
        return Integer.toString(temperature);
    }
}

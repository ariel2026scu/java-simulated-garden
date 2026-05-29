package garden.event;

public record TemperatureEvent(int temperature) implements GardenEvent {
    @Override
    public String name() {
        return "TEMPERATURE";
    }

    @Override
    public String value() {
        return Integer.toString(temperature);
    }
}

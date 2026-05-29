package garden.event;

public record RainEvent(int amount) implements GardenEvent {
    @Override
    public String name() {
        return "RAIN";
    }

    @Override
    public String value() {
        return Integer.toString(amount);
    }
}

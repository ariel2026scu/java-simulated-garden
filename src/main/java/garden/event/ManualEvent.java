package garden.event;

public record ManualEvent(String action, String value) implements GardenEvent {
    @Override
    public String name() {
        return action;
    }
}

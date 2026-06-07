package garden.event;

public record ParasiteEvent(String parasite) implements GardenEvent {
    @Override
    public String name() {
        return "PARASITE";
    }

    @Override
    public String value() {
        return parasite;
    }
}

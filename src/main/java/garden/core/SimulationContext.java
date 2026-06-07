package garden.core;

import garden.logging.GardenLogger;
import garden.model.Garden;

public class SimulationContext {
    private final GardenLogger logger;
    private int day;

    public SimulationContext(GardenLogger logger) {
        this.logger = logger;
        this.day = 0;
    }

    public int getDay() {
        return day;
    }

    public void advanceDay() {
        day++;
    }

    public void resetDay() {
        day = 0;
    }

    public void log(String event, String value, String module, String action, Garden garden, String details) {
        logger.log(day, event, value, module, action, garden, details);
    }
}

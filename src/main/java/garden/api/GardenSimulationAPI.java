package garden.api;

import garden.core.SimulationEngine;
import garden.event.ParasiteEvent;
import garden.event.RainEvent;
import garden.event.TemperatureEvent;

import java.util.Map;

public class GardenSimulationAPI {
    private final SimulationEngine engine;

    public GardenSimulationAPI() {
        this.engine = new SimulationEngine();
    }

    public void initializeGarden() {
        engine.initialize();
    }

    public Map<String, Object> getPlants() {
        return engine.getPlantDefinitions();
    }

    public void rain(int amount) {
        engine.submitEvent(new RainEvent(amount));
    }

    public void temperature(int temperature) {
        engine.submitEvent(new TemperatureEvent(temperature));
    }

    public void parasite(String parasite) {
        engine.submitEvent(new ParasiteEvent(parasite == null ? "" : parasite));
    }

    public void getState() {
        engine.logCurrentState();
    }

    public SimulationEngine getEngine() {
        return engine;
    }
}

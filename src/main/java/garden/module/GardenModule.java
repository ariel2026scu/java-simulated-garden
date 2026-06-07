package garden.module;

import garden.core.SimulationContext;
import garden.event.GardenEvent;
import garden.model.Garden;

public interface GardenModule {
    String getName();

    void handleEvent(Garden garden, GardenEvent event, SimulationContext context);

    void dailyUpdate(Garden garden, SimulationContext context);
}

package garden;

import garden.api.GardenSimulationAPI;

public class ManualApiCheck {
    public static void main(String[] args) {
        GardenSimulationAPI api = new GardenSimulationAPI();
        api.initializeGarden();
        System.out.println(api.getPlants());
        api.rain(12);
        api.temperature(98);
        api.parasite("aphid");
        api.getState();
        System.out.println("Manual API check completed. See log.txt.");
    }
}

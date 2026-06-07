package garden;

import garden.api.GardenSimulationAPI;

import java.util.List;
import java.util.Random;

public class StabilityCheck {
    public static void main(String[] args) {
        GardenSimulationAPI api = new GardenSimulationAPI();
        api.initializeGarden();
        Random random = new Random(275);
        List<String> parasites = List.of("aphid", "mite", "hornworm", "slug", "mealybug", "beetle");
        for (int day = 0; day < 24; day++) {
            int event = random.nextInt(3);
            if (event == 0) {
                api.rain(random.nextInt(31));
            } else if (event == 1) {
                api.temperature(40 + random.nextInt(81));
            } else {
                api.parasite(parasites.get(random.nextInt(parasites.size())));
            }
        }
        api.getState();
        System.out.println("24-day stability check completed. See log.txt.");
    }
}

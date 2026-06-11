package garden.app;

import garden.core.SimulationEngine;
import garden.model.GardenSnapshot;
import garden.model.PlantType;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * Main JavaFX entry point. Hosts a single {@link SimulationEngine} that is
 * shared between two tabs:
 * <ul>
 *     <li><b>Living Garden</b> — {@link GameView}, an animated autonomous garden
 *         that advances simulated days on a timer and renders cartoon plants,
 *         sprinklers, drones, etc.</li>
 *     <li><b>Admin Dashboard</b> — {@link DashboardView}, a control-and-tables
 *         view for setting precise event values, adding plants, and reading
 *         {@code log.txt} live.</li>
 *     <li><b>Help</b> — {@link HelpView}, a static usage guide explaining that
 *         the watering/temperature/pest/fertilizer systems run automatically,
 *         with buttons to open the bundled Markdown docs.</li>
 * </ul>
 * Both views observe the same engine, and any change made in one tab is
 * reflected in the other through cross-refresh callbacks.
 */
public class GardenShell extends Application {

    @Override
    public void start(Stage stage) {
        SimulationEngine engine = new SimulationEngine();
        // Initialize from garden_config.json so the setup picker can show the
        // configured per-variety counts as its default spinner values. The
        // picker may then call initializeWith(...) to overwrite that with the
        // gardener's own selection — whichever runs last is what both tabs see.
        engine.initialize();

        applyWindowIcons(stage);
        applyDockIcon();
        stage.setMinWidth(1080);
        stage.setMinHeight(720);

        showSetupScreen(stage, engine);
        stage.show();
    }

    /**
     * Pre-tab setup screen, ported back from the old standalone {@code
     * GardenGame}. Both tabs share the same {@link SimulationEngine}, so the
     * counts picked here apply to the whole app — there is intentionally
     * only one starting selection, not one per tab.
     */
    private void showSetupScreen(Stage stage, SimulationEngine engine) {
        GardenSnapshot defaults = engine.snapshot();
        Map<PlantType, Integer> defaultCounts = defaultCountsFromSnapshot(defaults);
        Map<PlantType, Spinner<Integer>> spinners = new EnumMap<>(PlantType.class);

        Label title = new Label("Choose your starting plants");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #eafff0;");
        Label sub = new Label("Defaults come from garden_config.json. Set any count you like, "
                + "then plant the garden. The selection is shared between both tabs.");
        sub.setStyle("-fx-text-fill: #bfe3c9; -fx-font-size: 13px;");
        sub.setWrapText(true);
        sub.setMaxWidth(640);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(18, 4, 18, 4));
        int row = 0;
        for (PlantType type : PlantType.values()) {
            Label name = new Label(type.getDisplayName());
            name.setStyle("-fx-text-fill: #eafff0; -fx-font-size: 14px;");
            name.setMinWidth(110);
            Spinner<Integer> spinner = new Spinner<>(0, 40, defaultCounts.getOrDefault(type, 0));
            spinner.setEditable(true);
            spinner.setPrefWidth(90);
            Label hint = new Label("water " + type.getWaterRequirement()
                    + " · ok " + type.getMinTemperature() + "–" + type.getMaxTemperature() + "°F");
            hint.setStyle("-fx-text-fill: #8fb89a; -fx-font-size: 11px;");
            spinners.put(type, spinner);
            grid.add(name, 0, row);
            grid.add(spinner, 1, row);
            grid.add(hint, 2, row);
            row++;
        }

        Label totalLabel = new Label();
        totalLabel.setStyle("-fx-text-fill: #ffe9a8; -fx-font-size: 14px; -fx-font-weight: bold;");
        Runnable updateTotal = () -> {
            int total = spinners.values().stream()
                    .mapToInt(s -> s.getValue() == null ? 0 : s.getValue()).sum();
            totalLabel.setText("Total plants: " + total
                    + (total == 0 ? "  (will use config defaults)" : ""));
        };
        spinners.values().forEach(s -> s.valueProperty().addListener((o, a, b) -> updateTotal.run()));
        updateTotal.run();

        Button useDefaults = new Button("Use config defaults");
        useDefaults.setOnAction(e -> {
            engine.initialize();
            launchMainUI(stage, engine);
        });
        Button startBtn = new Button("🌱 Plant garden");
        startBtn.setStyle("-fx-background-color: #2e7d49; -fx-text-fill: white; -fx-font-weight: bold;");
        startBtn.setOnAction(e -> {
            Map<PlantType, Integer> chosen = new EnumMap<>(PlantType.class);
            spinners.forEach((t, s) -> chosen.put(t, s.getValue() == null ? 0 : s.getValue()));
            engine.initializeWith(chosen);
            launchMainUI(stage, engine);
        });
        HBox buttons = new HBox(12, useDefaults, startBtn);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(8, title, sub, grid, totalLabel, buttons);
        box.setPadding(new Insets(28, 36, 28, 36));
        box.setStyle("-fx-background-color: #0c241a;");

        Scene setupScene = new Scene(box);
        stage.setTitle("Computerized Garden — Setup");
        stage.setScene(setupScene);
    }

    /**
     * Builds the two-tab main UI and swaps the stage from the setup scene to
     * the running app. Must run AFTER the engine has been initialized with
     * the chosen counts, because both views snapshot the engine in their
     * constructors.
     */
    private void launchMainUI(Stage stage, SimulationEngine engine) {
        GameView gameView = new GameView(engine);
        DashboardView dashboardView = new DashboardView(engine);

        // When either view mutates the engine, sync the sibling so the user
        // sees the same world state on whichever tab they switch to.
        gameView.setOnStateChanged(dashboardView::refresh);
        dashboardView.setOnStateChanged(gameView::refreshFromEngine);

        Tab gameTab = new Tab("🌱 Living Garden", gameView.getRoot());
        gameTab.setClosable(false);
        Tab dashboardTab = new Tab("📋 Admin Dashboard", dashboardView.getRoot());
        dashboardTab.setClosable(false);

        Tab helpTab = new Tab("❓ Help", new HelpView().getRoot());
        helpTab.setClosable(false);

        TabPane tabs = new TabPane(gameTab, dashboardTab, helpTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Scene scene = new Scene(tabs, 1280, 820);
        scene.getStylesheets().add(getClass().getResource("/garden/app/garden.css").toExternalForm());

        stage.setTitle("Computerized Garden — Living View & Dashboard");
        stage.setOnCloseRequest(e -> gameView.stopAnimation());
        stage.setScene(scene);
    }

    /**
     * Counts alive plants per {@link PlantType} in the given snapshot — used
     * to seed the setup-screen spinners with the config-driven defaults.
     */
    private Map<PlantType, Integer> defaultCountsFromSnapshot(GardenSnapshot snapshot) {
        Map<PlantType, Integer> counts = new EnumMap<>(PlantType.class);
        for (GardenSnapshot.PlantView v : snapshot.plants()) {
            if (!"DEAD".equals(v.status())) {
                PlantType t;
                try {
                    t = PlantType.fromName(v.type());
                } catch (RuntimeException ex) {
                    continue;
                }
                counts.merge(t, 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * Candidate icon sizes we probe under {@code /icons/icon_<size>.png}.
     * Missing entries are silently skipped, so the resource set can grow or
     * shrink without code changes. Covers the common JavaFX / macOS / iOS /
     * Windows tiers so any reasonable icon bundle drops in cleanly.
     */
    private static final int[] CANDIDATE_SIZES = {
            16, 20, 24, 29, 32, 40, 48, 64, 72, 96,
            128, 152, 167, 180, 192, 256, 512, 1024
    };

    /** Dock / taskbar floor — anything smaller looks fuzzy on a Retina Dock. */
    private static final int DOCK_MIN_ACCEPTABLE = 128;

    /**
     * JavaFX-side: probes every {@link #CANDIDATE_SIZES} entry and adds
     * whichever PNGs actually exist on the classpath to the stage's icon
     * list. JavaFX / the OS pick the closest size at render time, so it's
     * fine — and desirable — to include both tiny (title bar) and large
     * (Cmd-Tab thumbnail) variants in the same list.
     */
    private void applyWindowIcons(Stage stage) {
        int loaded = 0;
        for (int size : CANDIDATE_SIZES) {
            String path = "/icons/icon_" + size + ".png";
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    stage.getIcons().add(new Image(in));
                    loaded++;
                }
            } catch (Exception ex) {
                System.err.println("[GardenShell] could not load " + path + ": " + ex);
            }
        }
        System.err.println("[GardenShell] window icons loaded: " + loaded);
    }

    /**
     * macOS Dock (and Linux/Windows taskbar) icon: {@link Stage#getIcons()}
     * doesn't reach the macOS Dock — that surface is owned by AWT's
     * {@link java.awt.Taskbar} API. Without this call the Dock keeps showing
     * the default Java coffee-cup even after the stage icons are set.
     *
     * <p>Picks the largest available PNG from {@link #CANDIDATE_SIZES} that
     * is at least {@link #DOCK_MIN_ACCEPTABLE} pixels, so we never set the
     * Dock to a fuzzy 16/32-pixel image just because that's all we found.
     * If nothing sufficiently large exists we leave the Dock alone (system
     * default) rather than degrading the look.
     *
     * <p>Tolerant of platforms that don't expose the feature: missing
     * Taskbar support, missing ICON_IMAGE feature, or any I/O error during
     * load all degrade silently rather than crashing.
     */
    private void applyDockIcon() {
        try {
            if (!java.awt.Taskbar.isTaskbarSupported()) {
                return;
            }
            java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
            if (!taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                return;
            }
            for (int i = CANDIDATE_SIZES.length - 1; i >= 0; i--) {
                int size = CANDIDATE_SIZES[i];
                if (size < DOCK_MIN_ACCEPTABLE) {
                    break;
                }
                String path = "/icons/icon_" + size + ".png";
                try (InputStream in = getClass().getResourceAsStream(path)) {
                    if (in == null) {
                        continue;
                    }
                    java.awt.Image awtImage = javax.imageio.ImageIO.read(in);
                    if (awtImage != null) {
                        taskbar.setIconImage(awtImage);
                        System.err.println("[GardenShell] Dock icon set from " + path);
                        return;
                    }
                }
            }
            System.err.println("[GardenShell] no Dock-quality icon (>="
                    + DOCK_MIN_ACCEPTABLE + "px) found under /icons/; "
                    + "Dock will use system default.");
        } catch (Exception ex) {
            System.err.println("[GardenShell] could not set Dock icon: " + ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package garden.app;

import garden.core.SimulationEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

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
 * </ul>
 * Both views observe the same engine, and any change made in one tab is
 * reflected in the other through cross-refresh callbacks.
 */
public class GardenShell extends Application {

    @Override
    public void start(Stage stage) {
        SimulationEngine engine = new SimulationEngine();
        engine.initialize();

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

        TabPane tabs = new TabPane(gameTab, dashboardTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Scene scene = new Scene(tabs, 1280, 820);
        scene.getStylesheets().add(getClass().getResource("/garden/app/garden.css").toExternalForm());

        stage.setTitle("Computerized Garden — Living View & Dashboard");
        stage.setMinWidth(1080);
        stage.setMinHeight(720);
        stage.setOnCloseRequest(e -> gameView.stopAnimation());
        applyWindowIcons(stage);
        applyDockIcon();
        stage.setScene(scene);
        stage.show();
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

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
     * JavaFX-side: loads every size from {@code /icons/icon_*.png} into the
     * stage's icon list, used for the window title bar and Cmd-Tab / Alt-Tab
     * thumbnails.
     */
    private void applyWindowIcons(Stage stage) {
        int[] sizes = {16, 32, 48, 64, 128, 256, 512};
        for (int size : sizes) {
            String path = "/icons/icon_" + size + ".png";
            try (InputStream in = getClass().getResourceAsStream(path)) {
                if (in != null) {
                    stage.getIcons().add(new Image(in));
                }
            } catch (Exception ex) {
                System.err.println("[GardenShell] could not load " + path + ": " + ex);
            }
        }
    }

    /**
     * macOS Dock (and Linux/Windows taskbar) icon: {@link Stage#getIcons()}
     * doesn't reach the macOS Dock — that surface is owned by AWT's
     * {@link java.awt.Taskbar} API. Without this call the Dock keeps showing
     * the default Java coffee-cup even after the stage icons are set.
     *
     * <p>Tolerant of platforms that don't expose the feature: missing
     * Taskbar support, missing ICON_IMAGE feature, or any I/O error during
     * load all degrade silently to "no Dock icon" rather than crashing.
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
            try (InputStream in = getClass().getResourceAsStream("/icons/icon_512.png")) {
                if (in == null) {
                    return;
                }
                java.awt.Image awtImage = javax.imageio.ImageIO.read(in);
                if (awtImage != null) {
                    taskbar.setIconImage(awtImage);
                }
            }
        } catch (Exception ex) {
            System.err.println("[GardenShell] could not set Dock icon: " + ex);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

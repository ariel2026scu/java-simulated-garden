package garden.app;

import garden.core.SimulationEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

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
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

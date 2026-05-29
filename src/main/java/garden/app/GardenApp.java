package garden.app;

import garden.core.SimulationEngine;
import garden.event.ParasiteEvent;
import garden.event.RainEvent;
import garden.event.TemperatureEvent;
import garden.model.GardenSnapshot;
import garden.model.PlantType;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GardenApp extends Application {
    private final SimulationEngine engine = new SimulationEngine();
    private final GridPane gardenBoard = new GridPane();
    private final TableView<GardenSnapshot.PlantView> plantTable = new TableView<>();
    private final TextArea logArea = new TextArea();
    private final Label dayValue = new Label();
    private final Label aliveValue = new Label();
    private final Label deadValue = new Label();
    private final Label soilValue = new Label();
    private final Label temperatureValue = new Label();
    private final Label healthyValue = new Label();
    private final Label recoveringValue = new Label();
    private final Label stressedValue = new Label();
    private final Label infestedValue = new Label();
    private final Label deadStatusValue = new Label();
    private final Label logPathValue = new Label();

    @Override
    public void start(Stage stage) {
        engine.initialize();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildHeader());
        root.setLeft(buildControls());
        root.setCenter(buildGardenBoard());
        root.setRight(buildStatusPanel());
        root.setBottom(buildLogPanel());
        refresh();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/garden/app/garden.css").toExternalForm());
        stage.setTitle("Computerized Garden Simulation");
        stage.setMinWidth(1040);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader() {
        Label title = new Label("Computerized Garden Simulation");
        title.getStyleClass().add("app-title");

        HBox metrics = new HBox(10,
                metric("Day", dayValue),
                metric("Alive", aliveValue),
                metric("Dead", deadValue),
                metric("Soil", soilValue),
                metric("Temperature", temperatureValue)
        );
        metrics.setAlignment(Pos.CENTER_LEFT);

        VBox header = new VBox(8, title, metrics);
        header.getStyleClass().add("header");
        return header;
    }

    private VBox buildControls() {
        Spinner<Integer> rainAmount = new Spinner<>(0, 45, 10);
        Spinner<Integer> temperature = new Spinner<>(40, 120, 72);
        TextField parasiteField = new TextField("aphid");
        parasiteField.setPrefColumnCount(12);

        Button rainButton = primaryButton("Rain");
        rainButton.setOnAction(event -> {
            engine.submitEvent(new RainEvent(rainAmount.getValue()));
            refresh();
        });

        Button tempButton = primaryButton("Set Temperature");
        tempButton.setOnAction(event -> {
            engine.submitEvent(new TemperatureEvent(temperature.getValue()));
            refresh();
        });

        Button pestButton = primaryButton("Trigger Parasite");
        pestButton.setOnAction(event -> {
            engine.submitEvent(new ParasiteEvent(parasiteField.getText()));
            refresh();
        });

        GridPane events = new GridPane();
        events.getStyleClass().add("form-grid");
        events.addRow(0, new Label("Rain"), rainAmount);
        events.add(rainButton, 0, 1, 2, 1);
        events.addRow(2, new Label("Temperature"), temperature);
        events.add(tempButton, 0, 3, 2, 1);
        events.addRow(4, new Label("Parasite"), parasiteField);
        events.add(pestButton, 0, 5, 2, 1);

        Button nextDay = secondaryButton("Advance Day");
        nextDay.setOnAction(event -> {
            engine.advanceOneDay();
            refresh();
        });

        Button stateButton = secondaryButton("Log State");
        stateButton.setOnAction(event -> {
            engine.logCurrentState();
            refresh();
        });

        Button resetButton = secondaryButton("Reset Garden");
        resetButton.setOnAction(event -> {
            engine.initialize();
            refresh();
        });

        ComboBox<String> plantTypeCombo = new ComboBox<>(
                FXCollections.observableArrayList(Arrays.stream(PlantType.values())
                        .map(PlantType::getDisplayName)
                        .collect(Collectors.toList())));
        plantTypeCombo.setValue(PlantType.ROSE.getDisplayName());
        plantTypeCombo.setMaxWidth(Double.MAX_VALUE);

        Button addPlantButton = secondaryButton("Add Plant");
        addPlantButton.setOnAction(event -> {
            String selected = plantTypeCombo.getValue();
            if (selected != null) {
                engine.addPlant(PlantType.fromName(selected));
                refresh();
            }
        });

        VBox controls = new VBox(12,
                sectionTitle("Events"),
                events,
                new Separator(),
                sectionTitle("Garden"),
                nextDay,
                stateButton,
                resetButton,
                new Separator(),
                sectionTitle("Planting"),
                plantTypeCombo,
                addPlantButton
        );
        controls.getStyleClass().add("side-panel");
        controls.setPrefWidth(230);
        controls.setMinWidth(210);
        return controls;
    }

    private VBox buildGardenBoard() {
        gardenBoard.getStyleClass().add("garden-board");
        gardenBoard.setHgap(8);
        gardenBoard.setVgap(8);
        gardenBoard.setPadding(new Insets(12));
        for (int column = 0; column < 8; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(12.5);
            constraints.setHgrow(Priority.ALWAYS);
            gardenBoard.getColumnConstraints().add(constraints);
        }
        for (int row = 0; row < 5; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setPercentHeight(20);
            constraints.setVgrow(Priority.ALWAYS);
            gardenBoard.getRowConstraints().add(constraints);
        }

        VBox board = new VBox(8, sectionTitle("Garden Defense Board"), gardenBoard);
        board.getStyleClass().add("board-panel");
        VBox.setVgrow(gardenBoard, Priority.ALWAYS);
        BorderPane.setMargin(board, new Insets(12));
        return board;
    }

    private TableView<GardenSnapshot.PlantView> buildPlantTable() {
        TableColumn<GardenSnapshot.PlantView, String> name = textColumn("Plant", GardenSnapshot.PlantView::name, 140);
        TableColumn<GardenSnapshot.PlantView, String> type = textColumn("Type", GardenSnapshot.PlantView::type, 110);
        TableColumn<GardenSnapshot.PlantView, Number> health = intColumn("Health", GardenSnapshot.PlantView::health, 145);
        health.setCellFactory(column -> progressCell(100));
        TableColumn<GardenSnapshot.PlantView, Number> water = intColumn("Water", GardenSnapshot.PlantView::waterLevel, 130);
        water.setCellFactory(column -> progressCell(45));
        TableColumn<GardenSnapshot.PlantView, String> status = textColumn("Status", GardenSnapshot.PlantView::status, 115);
        TableColumn<GardenSnapshot.PlantView, String> parasites = textColumn("Parasites", GardenSnapshot.PlantView::activeParasites, 150);
        TableColumn<GardenSnapshot.PlantView, String> deathReason = textColumn("Death Reason", GardenSnapshot.PlantView::deathReason, 220);

        plantTable.getColumns().setAll(List.of(name, type, health, water, status, parasites, deathReason));
        plantTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        plantTable.setRowFactory(table -> new TableRow<>() {
            @Override
            protected void updateItem(GardenSnapshot.PlantView item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("healthy-row", "recovering-row", "stressed-row", "infested-row", "dead-row");
                if (empty || item == null) {
                    return;
                }
                switch (item.status()) {
                    case "HEALTHY" -> getStyleClass().add("healthy-row");
                    case "RECOVERING" -> getStyleClass().add("recovering-row");
                    case "STRESSED" -> getStyleClass().add("stressed-row");
                    case "INFESTED" -> getStyleClass().add("infested-row");
                    case "DEAD" -> getStyleClass().add("dead-row");
                    default -> {
                    }
                }
            }
        });
        BorderPane.setMargin(plantTable, new Insets(12, 12, 8, 12));
        return plantTable;
    }

    private VBox buildStatusPanel() {
        buildPlantTable();
        plantTable.setPrefHeight(330);
        VBox panel = new VBox(10,
                sectionTitle("Plant Status"),
                statusRow("Healthy", healthyValue),
                statusRow("Recovering", recoveringValue),
                statusRow("Stressed", stressedValue),
                statusRow("Infested", infestedValue),
                statusRow("Dead", deadStatusValue),
                new Separator(),
                sectionTitle("Plant Details"),
                plantTable,
                new Separator(),
                sectionTitle("Log File"),
                logPathValue
        );
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(360);
        panel.setMinWidth(320);
        logPathValue.getStyleClass().add("path-label");
        logPathValue.setWrapText(true);
        return panel;
    }

    private VBox buildLogPanel() {
        logArea.setEditable(false);
        logArea.setPrefRowCount(9);
        logArea.getStyleClass().add("log-area");
        Label header = sectionTitle("Event Log");
        VBox box = new VBox(6, header, logArea);
        box.getStyleClass().add("log-panel");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        return box;
    }

    private void refresh() {
        GardenSnapshot snapshot = engine.snapshot();
        dayValue.setText(Integer.toString(snapshot.day()));
        aliveValue.setText(Integer.toString(snapshot.alivePlants()));
        deadValue.setText(Integer.toString(snapshot.deadPlants()));
        soilValue.setText(snapshot.soilNutrients() + "%");
        temperatureValue.setText(snapshot.ambientTemperature() + "F");
        logPathValue.setText(engine.getLogPath().toAbsolutePath().toString());

        Map<String, Long> statusCounts = snapshot.plants().stream()
                .collect(Collectors.groupingBy(GardenSnapshot.PlantView::status, Collectors.counting()));
        healthyValue.setText(count(statusCounts, "HEALTHY"));
        recoveringValue.setText(count(statusCounts, "RECOVERING"));
        stressedValue.setText(count(statusCounts, "STRESSED"));
        infestedValue.setText(count(statusCounts, "INFESTED"));
        deadStatusValue.setText(count(statusCounts, "DEAD"));

        plantTable.setItems(FXCollections.observableArrayList(snapshot.plants()));
        renderGardenBoard(snapshot.plants());
        try {
            logArea.setText(Files.readString(engine.getLogPath()));
            logArea.positionCaret(logArea.getLength());
        } catch (IOException e) {
            logArea.setText("Unable to read log: " + e.getMessage());
        }
    }

    private void renderGardenBoard(List<GardenSnapshot.PlantView> plants) {
        gardenBoard.getChildren().clear();
        int totalCells = 5 * 8;
        for (int index = 0; index < totalCells; index++) {
            StackPane tile = new StackPane();
            tile.getStyleClass().add("lawn-tile");
            tile.setMinSize(88, 86);
            tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            if (index < plants.size()) {
                tile.getChildren().add(plantTile(plants.get(index)));
            }
            gardenBoard.add(tile, index % 8, index / 8);
        }
    }

    private VBox plantTile(GardenSnapshot.PlantView plant) {
        Circle icon = new Circle(18);
        icon.getStyleClass().addAll("plant-icon", plant.type().toLowerCase());

        Label name = new Label(shortName(plant.name()));
        name.getStyleClass().add("tile-name");

        ProgressBar health = new ProgressBar(Math.max(0, plant.health()) / 100.0);
        health.getStyleClass().add("tile-health");
        health.setMaxWidth(Double.MAX_VALUE);

        Label status = new Label(statusLabel(plant));
        status.getStyleClass().add("tile-status");

        VBox tileContent = new VBox(4, icon, name, health, status);
        tileContent.getStyleClass().addAll("plant-tile-content", plant.status().toLowerCase());
        tileContent.setAlignment(Pos.CENTER);
        return tileContent;
    }

    private String shortName(String name) {
        int dash = name.indexOf('-');
        return dash > 0 ? name.substring(0, dash) : name;
    }

    private String statusLabel(GardenSnapshot.PlantView plant) {
        if (!"-".equals(plant.activeParasites())) {
            return plant.activeParasites();
        }
        return switch (plant.status()) {
            case "HEALTHY" -> "ready";
            case "RECOVERING" -> "recovering";
            case "STRESSED" -> "stressed";
            case "INFESTED" -> "infested";
            case "DEAD" -> "dead";
            default -> plant.status().toLowerCase();
        };
    }

    private HBox metric(String label, Label value) {
        Label name = new Label(label);
        name.getStyleClass().add("metric-name");
        value.getStyleClass().add("metric-value");
        HBox box = new HBox(6, name, new Separator(Orientation.VERTICAL), value);
        box.getStyleClass().add("metric");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox statusRow(String label, Label value) {
        Label name = new Label(label);
        HBox row = new HBox(8, name, value);
        row.getStyleClass().add("status-row");
        HBox.setHgrow(name, Priority.ALWAYS);
        value.getStyleClass().add("status-value");
        return row;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("secondary-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    private String count(Map<String, Long> statusCounts, String key) {
        return Long.toString(statusCounts.getOrDefault(key, 0L));
    }

    private TableCell<GardenSnapshot.PlantView, Number> progressCell(int maxValue) {
        return new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            private final Label label = new Label();
            private final HBox box = new HBox(6, bar, label);

            {
                bar.setPrefWidth(76);
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                    return;
                }
                int number = value.intValue();
                bar.setProgress(Math.max(0, Math.min(1, number / (double) maxValue)));
                label.setText(Integer.toString(number));
                setGraphic(box);
            }
        };
    }

    private TableColumn<GardenSnapshot.PlantView, String> textColumn(
            String title,
            java.util.function.Function<GardenSnapshot.PlantView, String> value,
            int width) {
        TableColumn<GardenSnapshot.PlantView, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private TableColumn<GardenSnapshot.PlantView, Number> intColumn(
            String title,
            java.util.function.ToIntFunction<GardenSnapshot.PlantView> value,
            int width) {
        TableColumn<GardenSnapshot.PlantView, Number> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new SimpleIntegerProperty(value.applyAsInt(cell.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

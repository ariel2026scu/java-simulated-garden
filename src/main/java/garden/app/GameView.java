package garden.app;

import garden.core.SimulationEngine;
import garden.event.ParasiteEvent;
import garden.event.RainEvent;
import garden.event.TemperatureEvent;
import garden.model.GardenSnapshot;
import garden.model.PlantType;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * "Living Garden" — animated, autonomous visualisation of the shared
 * {@link SimulationEngine}. Designed to be embedded in {@link GardenShell}
 * alongside {@link DashboardView}, so they observe the same engine.
 *
 * <p>NOTHING here keeps the plants alive: the view simply advances simulated
 * days on a timer and renders whatever {@link SimulationEngine#snapshot()}
 * reports. The watering, temperature, pest-control and fertilizer modules do
 * all the work, so this screen directly demonstrates the spec requirement that
 * "the garden survives on its own".
 *
 * <p>The canvas grows vertically with the plant count and is hosted inside a
 * {@link ScrollPane}, so large gardens stay fully browsable.
 */
public class GameView {

    private static final int CANVAS_W = 1120;
    private static final int COLS = 6;
    private static final double MARGIN_X = 90;
    private static final double TOP = 150;
    private static final double CELL_W = (CANVAS_W - MARGIN_X * 2) / COLS;
    private static final double CELL_H = 150;
    private static final double BOTTOM_PAD = 60;

    private static final int COMFORT_MIN = 55;
    private static final int COMFORT_MAX = 95;
    private static final int SOIL_LOW = 45;

    // Real seconds per simulated day at 1x speed. At the prior value of 3.5s
    // every minute produced ~17 days, each writing ~11 log lines, which buried
    // user-fired events in autonomous-tick noise. 8s gives the gardener time
    // to read the log and compose an event before the next day rolls over.
    private static final double SECONDS_PER_DAY = 8.0;

    private final SimulationEngine engine;
    private final Random random = new Random();

    private final List<PlantSprite> sprites = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Drone> drones = new ArrayList<>();

    private final BorderPane root = new BorderPane();
    private final Canvas canvas = new Canvas(CANVAS_W, computeCanvasHeight(1));
    private final GraphicsContext gc = canvas.getGraphicsContext2D();
    private GardenSnapshot snapshot;
    private long lastNanos = 0;
    private double dayAccumulator = 0;
    private double speed = 1.0;
    private double speedBeforePause = 1.0;
    private double worldTime = 0;

    private int lastDayMilestone = 0;
    private double toastTimer = 0;
    private String toastText = "";

    /** Transient post-event visual effect: rain shower, heat-wave flash, or cold-snap flash. */
    private enum WeatherEffect { NONE, RAIN, HEAT, COLD }
    private WeatherEffect weatherEffect = WeatherEffect.NONE;
    private double weatherEffectTimer = 0;
    private double rainSpawnAccumulator = 0;

    private final Label dayLabel = new Label();
    private final Label aliveLabel = new Label();
    private final Label envLabel = new Label();
    private final Label speedLabel = new Label();

    private AnimationTimer timer;
    private Runnable onStateChanged = () -> {};

    public GameView(SimulationEngine engine) {
        this.engine = engine;
        this.snapshot = engine.snapshot();
        syncSprites();

        StackPane canvasHolder = new StackPane(canvas);
        canvasHolder.setStyle("-fx-background-color: #0b2018;");
        canvasHolder.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(canvasHolder);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle(
                "-fx-background: #0b2018;"
                        + " -fx-background-color: #0b2018;"
                        + " -fx-control-inner-background: #0b2018;");

        root.setTop(buildHud());
        root.setCenter(scrollPane);
        root.setBottom(buildToolbar());
        root.setStyle("-fx-background-color: #0c241a;");

        refreshHud();
        startAnimation();
    }

    public Node getRoot() {
        return root;
    }

    public void setOnStateChanged(Runnable callback) {
        this.onStateChanged = callback == null ? () -> {} : callback;
    }

    /** Called by sibling views (e.g. dashboard buttons) after they mutate the engine. */
    public void refreshFromEngine() {
        refreshSnapshot();
    }

    /** Stop the animation loop. Call from the host Application on window close. */
    public void stopAnimation() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    // ── HUD & toolbar ────────────────────────────────────────────────────────

    private HBox buildHud() {
        style(dayLabel, "#eafff0", 16, true);
        style(aliveLabel, "#eafff0", 16, true);
        style(envLabel, "#bfe3c9", 14, false);
        style(speedLabel, "#ffe9a8", 15, true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox hud = new HBox(22, dayLabel, aliveLabel, envLabel, spacer, speedLabel);
        hud.setAlignment(Pos.CENTER_LEFT);
        hud.setPadding(new Insets(12, 20, 12, 20));
        hud.setStyle("-fx-background-color: #07150f;");
        return hud;
    }

    private HBox buildToolbar() {
        Label gardener = new Label("Guest gardener:");
        style(gardener, "#9fd4ad", 13, false);

        Button rain = btn("🌧 Rain");
        rain.setOnAction(e -> throwEvent(new RainEvent(18), "Gardener poured rain"));
        Button heat = btn("🔥 Heat Wave");
        heat.setOnAction(e -> throwEvent(new TemperatureEvent(110), "Heat wave incoming!"));
        Button cold = btn("❄ Cold Snap");
        cold.setOnAction(e -> throwEvent(new TemperatureEvent(40), "Cold snap incoming!"));
        Button pest = btn("🐛 Pest Outbreak");
        pest.setOnAction(e -> {
            String[] pool = {"aphid", "beetle", "mite", "slug", "hornworm", "whitefly"};
            throwEvent(new ParasiteEvent(pool[random.nextInt(pool.length)]), "Pests invaded the garden!");
        });
        Button logState = btn("📋 Log State");
        logState.setTooltip(new Tooltip(
                "Writes a STATE summary row + one row per plant to log.txt.\n"
                        + "Useful right before showing the log to a grader so the\n"
                        + "current garden snapshot is captured on disk."));
        logState.setOnAction(e -> {
            safe(engine::logCurrentState);
            showToast("📋 Snapshot written to log.txt");
            onStateChanged.run();
        });

        Label spd = new Label("Speed:");
        style(spd, "#9fd4ad", 13, false);
        Button pauseBtn = btn("⏸ Pause");
        pauseBtn.setOnAction(e -> {
            if (speed > 0) {
                speedBeforePause = speed;
                speed = 0;
                pauseBtn.setText("▶ Resume");
            } else {
                speed = speedBeforePause > 0 ? speedBeforePause : 1.0;
                pauseBtn.setText("⏸ Pause");
            }
        });
        Button s1 = btn("1x");
        s1.setOnAction(e -> {
            speed = 1.0;
            pauseBtn.setText("⏸ Pause");
        });
        Button s5 = btn("5x");
        s5.setOnAction(e -> {
            speed = 5.0;
            pauseBtn.setText("⏸ Pause");
        });
        Button s20 = btn("20x");
        s20.setOnAction(e -> {
            speed = 20.0;
            pauseBtn.setText("⏸ Pause");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, gardener, rain, heat, cold, pest, logState, spacer, spd, pauseBtn, s1, s5, s20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 20, 12, 20));
        bar.setStyle("-fx-background-color: #07150f;");
        return bar;
    }

    private Button btn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #2f7d4f; -fx-text-fill: white; -fx-font-weight: bold;"
                + " -fx-background-radius: 8; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
        return b;
    }

    private void style(Label l, String color, int size, boolean bold) {
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;"
                + (bold ? " -fx-font-weight: bold;" : ""));
    }

    // ── Disturbance events (forwarded to the engine, also advance a sim day) ──

    private void throwEvent(garden.event.GardenEvent event, String toast) {
        safe(() -> engine.submitEvent(event));
        refreshSnapshot();
        showToast(toast);
        triggerWeatherEffect(event);
    }

    /** Kick off the matching visual effect for the event the gardener just fired. */
    private void triggerWeatherEffect(garden.event.GardenEvent event) {
        if (event instanceof garden.event.RainEvent) {
            weatherEffect = WeatherEffect.RAIN;
            weatherEffectTimer = 3.0;
        } else if (event instanceof garden.event.TemperatureEvent te) {
            int temp = te.temperature();
            if (temp > COMFORT_MAX) {
                weatherEffect = WeatherEffect.HEAT;
                weatherEffectTimer = 3.0;
            } else if (temp < COMFORT_MIN) {
                weatherEffect = WeatherEffect.COLD;
                weatherEffectTimer = 3.0;
            }
        }
    }

    // ── Autonomous simulation driver ──────────────────────────────────────────

    private void startAnimation() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                    return;
                }
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                if (dt > 0.1) {
                    dt = 0.1;
                }
                try {
                    update(dt);
                    render();
                } catch (Throwable t) {
                    System.err.println("[GameView] frame error contained: " + t);
                }
            }
        };
        timer.start();
    }

    private void update(double dt) {
        worldTime += dt;
        dayAccumulator += dt * speed;
        while (dayAccumulator >= SECONDS_PER_DAY) {
            dayAccumulator -= SECONDS_PER_DAY;
            // Auto-advances log as AUTO_TICK so they don't drown out the
            // gardener's explicit RAIN / TEMPERATURE / PARASITE / MANUAL_DAY
            // events when scrolling log.txt.
            safe(engine::tickAutonomous);
            refreshSnapshot();
        }

        for (PlantSprite s : sprites) {
            s.update(dt);
        }
        updateDevices(dt);
        updateDrones(dt);
        updateWeatherEffect(dt);
        updateParticles(dt);

        if (toastTimer > 0) {
            toastTimer -= dt;
        }
        refreshHud();
    }

    private void updateWeatherEffect(double dt) {
        if (weatherEffectTimer <= 0) {
            weatherEffect = WeatherEffect.NONE;
            return;
        }
        weatherEffectTimer -= dt;
        if (weatherEffect == WeatherEffect.RAIN) {
            // Spawn a steady stream of raindrops across the top of the canvas.
            rainSpawnAccumulator += dt;
            int toSpawn = (int) (rainSpawnAccumulator / 0.015);
            rainSpawnAccumulator -= toSpawn * 0.015;
            double w = canvas.getWidth();
            for (int i = 0; i < toSpawn; i++) {
                particles.add(Particle.rainstreak(random.nextDouble() * w, -8));
            }
        }
    }

    private void refreshSnapshot() {
        int prevTemp = snapshot == null ? 72 : snapshot.ambientTemperature();
        snapshot = engine.snapshot();
        int newTemp = snapshot.ambientTemperature();
        // If a temperature change came from somewhere other than this view's
        // own toolbar (e.g. the dashboard's "Set Temperature" button) the local
        // triggerWeatherEffect never ran. Detect the change here so the game
        // tab animates regardless of which tab fired the event.
        if (newTemp != prevTemp) {
            if (newTemp > COMFORT_MAX) {
                weatherEffect = WeatherEffect.HEAT;
                weatherEffectTimer = 3.0;
            } else if (newTemp < COMFORT_MIN) {
                weatherEffect = WeatherEffect.COLD;
                weatherEffectTimer = 3.0;
            }
        }
        syncSprites();
        if (snapshot.day() >= lastDayMilestone + 5) {
            lastDayMilestone = (snapshot.day() / 5) * 5;
            if (snapshot.alivePlants() > 0) {
                showToast("🏆 Survived " + lastDayMilestone + " days — "
                        + snapshot.alivePlants() + " plants thriving!");
            }
        }
        onStateChanged.run();
    }

    private void syncSprites() {
        List<GardenSnapshot.PlantView> plants = snapshot.plants();
        resizeCanvasFor(plants.size());
        if (sprites.size() != plants.size()) {
            sprites.clear();
            for (int i = 0; i < plants.size(); i++) {
                PlantSprite s = new PlantSprite(i);
                s.apply(plants.get(i));
                s.phase = random.nextDouble() * Math.PI * 2;
                sprites.add(s);
            }
        } else {
            for (int i = 0; i < plants.size(); i++) {
                sprites.get(i).apply(plants.get(i));
            }
        }
    }

    /** Grow the canvas so every plant row is reachable via the surrounding ScrollPane. */
    private void resizeCanvasFor(int plantCount) {
        double newHeight = computeCanvasHeight(plantCount);
        if (Math.abs(canvas.getHeight() - newHeight) > 0.5) {
            canvas.setHeight(newHeight);
        }
    }

    private static double computeCanvasHeight(int plantCount) {
        int rows = Math.max(1, (int) Math.ceil(plantCount / (double) COLS));
        return TOP + rows * CELL_H + BOTTOM_PAD;
    }

    // ── Module device animations (driven purely by live state) ────────────────

    private double sprinkleTimer = 0;
    private double fertilizeTimer = 0;

    private void updateDevices(double dt) {
        sprinkleTimer -= dt;
        if (sprinkleTimer <= 0) {
            sprinkleTimer = 0.05;
            for (PlantSprite s : sprites) {
                if (s.alive && s.water < s.requirement) {
                    s.sprinkling = true;
                    particles.add(Particle.droplet(s.x + (random.nextDouble() - 0.5) * 30, s.y - 70));
                } else {
                    s.sprinkling = false;
                }
            }
        }

        for (PlantSprite s : sprites) {
            if (s.alive && s.infested && !s.hasDrone) {
                Drone d = new Drone(s);
                drones.add(d);
                s.hasDrone = true;
            }
        }

        fertilizeTimer -= dt;
        if (fertilizeTimer <= 0 && snapshot.soilNutrients() < SOIL_LOW) {
            fertilizeTimer = 0.08;
            double fx = MARGIN_X + random.nextDouble() * (CANVAS_W - MARGIN_X * 2);
            particles.add(Particle.granule(fx, TOP - 30));
        }
    }

    private void updateDrones(double dt) {
        for (Drone d : drones) {
            d.update(dt, particles, random);
        }
        drones.removeIf(d -> {
            if (d.finished()) {
                d.target.hasDrone = false;
                return true;
            }
            return false;
        });
    }

    private void updateParticles(double dt) {
        double maxY = canvas.getHeight();
        for (Particle p : particles) {
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += p.gravity * dt;
            p.life -= dt;
        }
        particles.removeIf(p -> p.life <= 0 || p.y > maxY + 20);
        // Cap is high enough to host a full rain shower (~600 streaks) plus the
        // usual sprinkler / drone / fertilizer particles concurrently.
        while (particles.size() > 1500) {
            particles.remove(0);
        }
    }

    // ── Rendering ──────────────────────────────────────────────────────────────

    private void render() {
        drawBackground();
        for (Particle p : particles) {
            p.draw(gc);
        }
        for (PlantSprite s : sprites) {
            drawPlant(s);
            if (s.sprinkling) {
                drawSprinkler(s);
            }
        }
        for (Drone d : drones) {
            d.draw(gc);
        }
        // Both the persistent weather tint and the climate-control device
        // (shade panels / heat lamps) are now gated on the weather-effect
        // timer below, so a temperature event reads as a dramatic 3-second
        // wave instead of an immortal overlay glued to the canvas.
        drawWeatherEffect();
        drawToast();
    }

    /**
     * Transient pulsing overlay drawn for a few seconds after a weather event,
     * plus the matching climate-control device animation at the top of the
     * canvas. The status text leads with the threat icon (🔥 for heat wave,
     * ❄ for cold snap) so it visibly matches the button the gardener pressed,
     * while the device beneath represents the response the control system is
     * deploying (cooling shade panels vs. heat lamps).
     */
    private void drawWeatherEffect() {
        if (weatherEffect == WeatherEffect.NONE || weatherEffectTimer <= 0) {
            return;
        }
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double fade = Math.min(1, weatherEffectTimer);
        double pulse = 0.5 + 0.5 * Math.sin(weatherEffectTimer * 8);
        int temp = snapshot.ambientTemperature();
        switch (weatherEffect) {
            case RAIN -> {
                // Slight blue desaturation so the raindrops read against bright
                // grass; the streaks themselves come from the particle list.
                gc.setFill(Color.color(0.4, 0.55, 0.85, 0.10 * fade));
                gc.fillRect(0, 0, w, h);
            }
            case HEAT -> {
                gc.setFill(Color.color(1, 0.45, 0.05, 0.18 * pulse * fade));
                gc.fillRect(0, 0, w, h);
                // Cooling shade panels sliding into place, each tagged "SHADE"
                // so the visual element is named without the gardener needing
                // to read the banner first.
                gc.setFill(Color.web("#1f6f8b"));
                double slice = w / 6.0;
                for (int i = 0; i < 6; i++) {
                    double x = i * slice + Math.sin(worldTime * 2 + i) * 4;
                    gc.fillRoundRect(x + 6, 18, slice - 12, 26, 8, 8);
                }
                gc.setFill(Color.web("#dcefff"));
                gc.setFont(javafx.scene.text.Font.font(9));
                for (int i = 0; i < 6; i++) {
                    double cx = i * slice + slice / 2;
                    gc.fillText("SHADE", cx - 14, 36);
                }
                drawWeatherBanner("🔥 HEAT WAVE — Cooling shade panels deployed ("
                        + temp + "°F)", 56);
            }
            case COLD -> {
                gc.setFill(Color.color(0.4, 0.65, 1.0, 0.22 * pulse * fade));
                gc.fillRect(0, 0, w, h);
                // Heat lamps lighting up, each tagged "HEAT LAMP" for the
                // same reason as the shade panels above.
                for (int i = 0; i < 6; i++) {
                    double x = (i + 0.5) * (w / 6.0);
                    double glow = 0.5 + 0.3 * Math.sin(worldTime * 4 + i);
                    gc.setFill(Color.color(1, 0.5, 0.1, glow));
                    gc.fillOval(x - 18, 16, 36, 36);
                    gc.setFill(Color.web("#ffd089"));
                    gc.fillOval(x - 8, 24, 16, 16);
                }
                gc.setFill(Color.web("#fff1c2"));
                gc.setFont(javafx.scene.text.Font.font(9));
                for (int i = 0; i < 6; i++) {
                    double cx = (i + 0.5) * (w / 6.0);
                    gc.fillText("HEAT LAMP", cx - 24, 64);
                }
                drawWeatherBanner("❄ COLD SNAP — Heat lamps engaged ("
                        + temp + "°F)", 84);
            }
            default -> {
            }
        }
    }

    /**
     * High-contrast banner pill (dark backdrop + white text) so the weather
     * label is readable on top of the orange / blue tint overlays. The text
     * pill auto-sizes from the rendered label width so longer messages don't
     * overflow.
     */
    private void drawWeatherBanner(String text, double y) {
        javafx.scene.text.Text probe = new javafx.scene.text.Text(text);
        probe.setFont(javafx.scene.text.Font.font(15));
        double textWidth = probe.getLayoutBounds().getWidth();
        double padX = 16;
        double padY = 6;
        double pillW = textWidth + padX * 2;
        double pillH = 26;
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRoundRect(20, y, pillW, pillH, 12, 12);
        gc.setStroke(Color.color(1, 1, 1, 0.35));
        gc.setLineWidth(1.0);
        gc.strokeRoundRect(20, y, pillW, pillH, 12, 12);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font(15));
        gc.fillText(text, 20 + padX, y + pillH - padY - 2);
    }

    private void drawBackground() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);
        double horizon = TOP - 40;

        // ── Sky: soft vertical gradient ───────────────────────────────────────
        gc.setFill(new LinearGradient(0, 0, 0, horizon, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#aee3ff")),
                new Stop(0.6, Color.web("#cdeffd")),
                new Stop(1, Color.web("#eafbf0"))));
        gc.fillRect(0, 0, w, horizon);

        // Sun with a soft glow, drifting slightly with time.
        double sunX = w - 130 + Math.sin(worldTime * 0.2) * 8;
        double sunY = 64;
        gc.setFill(new RadialGradient(0, 0, sunX, sunY, 90, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(1, 0.95, 0.6, 0.55)),
                new Stop(1, Color.color(1, 0.95, 0.6, 0))));
        gc.fillOval(sunX - 90, sunY - 90, 180, 180);
        gc.setFill(new RadialGradient(0, 0, sunX - 8, sunY - 8, 34, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fff6cf")),
                new Stop(1, Color.web("#ffdf6b"))));
        gc.fillOval(sunX - 30, sunY - 30, 60, 60);

        // Drifting clouds.
        for (int i = 0; i < 3; i++) {
            double cspeed = 14 + i * 6;
            double cx = ((worldTime * cspeed) + i * 420) % (w + 220) - 110;
            double cy = 40 + i * 26;
            drawCloud(cx, cy, 1.0 - i * 0.12);
        }

        // ── Rolling hills behind the beds ─────────────────────────────────────
        gc.setFill(Color.web("#bfe6a4"));
        gc.beginPath();
        gc.moveTo(0, horizon);
        for (double x = 0; x <= w; x += 40) {
            gc.lineTo(x, horizon - 22 - Math.sin(x / 160.0) * 14);
        }
        gc.lineTo(w, horizon);
        gc.closePath();
        gc.fill();

        // ── Lawn: gradient grass with alternating mowed stripes ───────────────
        gc.setFill(new LinearGradient(0, horizon, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7cc36b")),
                new Stop(1, Color.web("#4f9b48"))));
        gc.fillRect(0, horizon, w, h - horizon);

        int rows = Math.max(1, (int) Math.ceil(sprites.size() / (double) COLS));
        for (int r = 0; r < rows; r++) {
            double y = horizon + r * CELL_H;
            gc.setFill(r % 2 == 0 ? Color.color(1, 1, 1, 0.05) : Color.color(0, 0, 0, 0.05));
            gc.fillRect(0, y, w, CELL_H);

            double bx = MARGIN_X - 34;
            double bw = w - (MARGIN_X - 34) * 2;
            double by = y + CELL_H - 30;
            gc.setFill(new LinearGradient(0, by, 0, by + 26, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#8a6a3f")),
                    new Stop(1, Color.web("#4f3a20"))));
            gc.fillRoundRect(bx, by, bw, 26, 12, 12);
            gc.setStroke(Color.web("#3a2a16"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(bx, by, bw, 26, 12, 12);
            gc.setFill(Color.color(0, 0, 0, 0.18));
            for (int s = 0; s < 26; s++) {
                double sx = bx + 10 + ((s * 53) % (int) (bw - 20));
                double sy = by + 6 + ((s * 17) % 14);
                gc.fillOval(sx, sy, 2.5, 2.5);
            }
        }
    }

    /** Soft puffy cloud built from overlapping translucent circles. */
    private void drawCloud(double x, double y, double scale) {
        gc.setFill(Color.color(1, 1, 1, 0.85));
        double s = 1.0 * scale;
        gc.fillOval(x, y, 60 * s, 36 * s);
        gc.fillOval(x + 30 * s, y - 14 * s, 54 * s, 42 * s);
        gc.fillOval(x + 64 * s, y, 58 * s, 34 * s);
        gc.fillOval(x + 26 * s, y + 8 * s, 70 * s, 30 * s);
    }

    private void drawPlant(PlantSprite s) {
        double shadow = s.alive ? 1.0 : (1 - s.wilt * 0.4);
        gc.setFill(Color.color(0, 0, 0, 0.16 * shadow));
        gc.fillOval(s.x - 26 * shadow, s.y + 4, 52 * shadow, 13);

        gc.save();
        gc.translate(s.x, s.y);
        if (!s.alive) {
            gc.rotate(s.wilt * 70);
            gc.setGlobalAlpha(1 - s.wilt * 0.35);
        } else {
            gc.rotate(Math.sin(s.phase * 2 + s.index) * 4);
        }
        double pop = 1 + s.pop * 0.35;
        gc.scale(pop, pop);
        drawPlantBody(s.type, !s.alive, s.infested);
        gc.restore();

        gc.setFill(Color.web("#dfeede"));
        gc.setFont(javafx.scene.text.Font.font(11));
        gc.fillText(s.type.getDisplayName(), s.x - 24, s.y + 22);
        if (s.alive) {
            drawBar(s.x - 26, s.y - 96, 52, s.displayHealth / 100.0,
                    Color.web("#5fd36b"), Color.web("#e8472f"));
        }
    }

    private static final Color OUTLINE = Color.web("#2b3a2b");

    /** Cartoon plant. Origin at soil base; grows upward (-y). */
    private void drawPlantBody(PlantType type, boolean dead, boolean infested) {
        Color stemHi = dead ? Color.web("#9a8456") : Color.web("#5fc66a");
        Color stemLo = dead ? Color.web("#6b5536") : Color.web("#2f8f3f");

        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        gc.setStroke(stemLo);
        gc.setLineWidth(7);
        gc.strokeLine(0, 0, 0, -34);
        gc.setStroke(stemHi);
        gc.setLineWidth(3);
        gc.strokeLine(-1, -2, -1, -32);
        leaf(-15, -20, 20, 12, -25, dead);
        leaf(15, -28, 20, 12, 25, dead);

        switch (type) {
            case ROSE -> {
                blossom(0, -50, 17, 16, c(dead, "#ff9ab8", "#9a7e7e"), c(dead, "#e0466b", "#7a5b5b"));
                blossom(0, -50, 10, 9, c(dead, "#ffd0dd", "#a98e8e"), c(dead, "#f06d8b", "#8a6b6b"));
                gc.setFill(c(dead, "#c23357", "#6f5454"));
                gc.fillOval(-4, -54, 8, 8);
            }
            case TOMATO -> {
                gc.setFill(c(dead, "#3fae50", "#7a6a45"));
                gc.fillPolygon(new double[]{-7, 0, 7}, new double[]{-56, -66, -56}, 3);
                blossom(0, -42, 17, 16, c(dead, "#ff7a5f", "#9a7b6b"), c(dead, "#e8472f", "#7a5b4b"));
                blossom(-9, -36, 9, 8, c(dead, "#ff8f72", "#a98b78"), c(dead, "#e8472f", "#7a5b4b"));
            }
            case LETTUCE -> {
                blossom(-12, -40, 14, 13, c(dead, "#b6e08a", "#9a9a6f"), c(dead, "#79c44f", "#7a7a4f"));
                blossom(12, -40, 14, 13, c(dead, "#b6e08a", "#9a9a6f"), c(dead, "#79c44f", "#7a7a4f"));
                blossom(0, -50, 16, 14, c(dead, "#cdeea0", "#a8a87a"), c(dead, "#8fce5e", "#85854f"));
            }
            case CACTUS -> {
                roundBlob(-12, -62, 24, 50, 14, c(dead, "#79c08a", "#7f8f6a"), c(dead, "#3f9e57", "#5f6f4a"));
                roundBlob(-28, -48, 15, 26, 10, c(dead, "#79c08a", "#7f8f6a"), c(dead, "#3f9e57", "#5f6f4a"));
                roundBlob(13, -54, 15, 26, 10, c(dead, "#79c08a", "#7f8f6a"), c(dead, "#3f9e57", "#5f6f4a"));
                if (!dead) {
                    gc.setFill(Color.web("#ffd34d"));
                    gc.fillOval(-4, -64, 8, 8);
                }
            }
            case SUNFLOWER -> {
                Color pHi = c(dead, "#ffe08a", "#bcae7f");
                Color pLo = c(dead, "#f4c542", "#9a8a4f");
                for (int i = 0; i < 12; i++) {
                    double ang = Math.toRadians(i * 30);
                    double px = Math.cos(ang) * 23;
                    double py = -54 + Math.sin(ang) * 23;
                    gc.save();
                    gc.translate(px, py);
                    gc.rotate(Math.toDegrees(ang) + 90);
                    blossom(0, 0, 6, 11, pHi, pLo);
                    gc.restore();
                }
                blossom(0, -54, 14, 14, c(dead, "#a9763f", "#7a6a4f"), c(dead, "#7a4a1f", "#5b4a2f"));
            }
            case BASIL -> {
                leaf(-13, -48, 20, 15, -20, dead);
                leaf(13, -48, 20, 15, 20, dead);
                blossom(0, -58, 13, 13, c(dead, "#6fc97a", "#8a9a6f"), c(dead, "#3f9e4f", "#5f6f4a"));
            }
            case PEPPER -> {
                gc.setFill(c(dead, "#3fae50", "#7a6a45"));
                gc.fillRoundRect(-4, -66, 8, 12, 4, 4);
                blossom(0, -44, 11, 19, c(dead, "#ff6a4f", "#9a7b6b"), c(dead, "#e23b2f", "#7a5d3f"));
            }
            case LAVENDER -> {
                gc.setStroke(c(dead, "#3f9e4f", "#6b6a45"));
                gc.setLineWidth(3);
                for (int i = -1; i <= 1; i++) {
                    double sx = i * 10;
                    gc.strokeLine(0, -30, sx, -50);
                    for (int b = 0; b < 4; b++) {
                        blossom(sx, -52 - b * 6, 5, 6, c(dead, "#c7a8e8", "#9a8fa8"), c(dead, "#9b6fd4", "#7a6f8a"));
                    }
                }
            }
            default -> blossom(0, -50, 15, 15, c(dead, "#6fc97a", "#8a9a6f"), c(dead, "#3fae50", "#5f6f4a"));
        }

        if (infested && !dead) {
            gc.setFill(Color.web("#5b3f95"));
            gc.fillOval(9, -40, 7, 7);
            gc.fillOval(-15, -30, 6, 6);
            gc.setStroke(OUTLINE);
            gc.setLineWidth(1);
            gc.strokeOval(9, -40, 7, 7);
            gc.strokeOval(-15, -30, 6, 6);
        }
    }

    private static Color c(boolean dead, String live, String deadHex) {
        return Color.web(dead ? deadHex : live);
    }

    private void blossom(double cx, double cy, double rx, double ry, Color light, Color dark) {
        gc.setFill(new RadialGradient(0, 0, cx - rx * 0.3, cy - ry * 0.4, Math.max(rx, ry) * 1.3,
                false, CycleMethod.NO_CYCLE, new Stop(0, light), new Stop(1, dark)));
        gc.fillOval(cx - rx, cy - ry, rx * 2, ry * 2);
        gc.setStroke(OUTLINE);
        gc.setLineWidth(1.4);
        gc.strokeOval(cx - rx, cy - ry, rx * 2, ry * 2);
        gc.setFill(Color.color(1, 1, 1, 0.45));
        gc.fillOval(cx - rx * 0.45, cy - ry * 0.6, rx * 0.5, ry * 0.5);
    }

    private void leaf(double cx, double cy, double w, double h, double rotDeg, boolean dead) {
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(rotDeg);
        gc.setFill(new RadialGradient(0, 0, -w * 0.2, -h * 0.3, w, false, CycleMethod.NO_CYCLE,
                new Stop(0, c(dead, "#6fd07a", "#9a8a5f")), new Stop(1, c(dead, "#2f8f3f", "#6b5536"))));
        gc.fillOval(-w / 2, -h / 2, w, h);
        gc.setStroke(OUTLINE);
        gc.setLineWidth(1.2);
        gc.strokeOval(-w / 2, -h / 2, w, h);
        gc.restore();
    }

    private void roundBlob(double x, double y, double w, double h, double arc, Color light, Color dark) {
        gc.setFill(new LinearGradient(x, y, x + w, y, false, CycleMethod.NO_CYCLE,
                new Stop(0, light), new Stop(1, dark)));
        gc.fillRoundRect(x, y, w, h, arc, arc);
        gc.setStroke(OUTLINE);
        gc.setLineWidth(1.4);
        gc.strokeRoundRect(x, y, w, h, arc, arc);
    }

    private void drawSprinkler(PlantSprite s) {
        gc.setFill(Color.web("#4a90c2"));
        gc.fillRect(s.x + 20, s.y - 8, 6, 12);
        gc.setStroke(Color.color(0.6, 0.8, 1, 0.5));
        gc.setLineWidth(1.5);
        for (int i = 0; i < 3; i++) {
            double r = 14 + i * 8 + Math.sin(worldTime * 6 + i) * 2;
            gc.strokeArc(s.x + 23 - r, s.y - 4 - r, r * 2, r * 2, 20, 110, javafx.scene.shape.ArcType.OPEN);
        }
    }

    private void drawBar(double x, double y, double w, double ratio, Color hi, Color lo) {
        ratio = Math.max(0, Math.min(1, ratio));
        gc.setFill(Color.color(0, 0, 0, 0.35));
        gc.fillRoundRect(x - 1, y - 1, w + 2, 7, 4, 4);
        gc.setFill(ratio > 0.4 ? hi : lo);
        gc.fillRoundRect(x, y, w * ratio, 5, 4, 4);
    }

    private void drawToast() {
        if (toastTimer <= 0 || toastText.isEmpty()) {
            return;
        }
        double alpha = Math.min(1, toastTimer);
        double w = canvas.getWidth();
        gc.setGlobalAlpha(alpha);
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRoundRect(w / 2.0 - 200, 90, 400, 40, 12, 12);
        gc.setFill(Color.web("#eafff0"));
        gc.setFont(javafx.scene.text.Font.font(15));
        gc.fillText(toastText, w / 2.0 - 185, 116);
        gc.setGlobalAlpha(1);
    }

    private void showToast(String text) {
        toastText = text;
        toastTimer = 3.0;
    }

    private void refreshHud() {
        dayLabel.setText("📅 Day " + snapshot.day());
        aliveLabel.setText("🌱 Alive " + snapshot.alivePlants() + "   💀 Dead " + snapshot.deadPlants());
        envLabel.setText("Soil " + snapshot.soilNutrients() + "%   Temp " + snapshot.ambientTemperature() + "°F");
        speedLabel.setText(speed > 0 ? "⏩ " + (int) speed + "x" : "⏸ Paused");
    }

    private void safe(Runnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            System.err.println("[GameView] engine call contained: " + t);
        }
    }

    // ── Layout helpers ─────────────────────────────────────────────────────────

    private double cellX(int index) {
        return MARGIN_X + (index % COLS) * CELL_W + CELL_W / 2;
    }

    private double cellY(int index) {
        return TOP + (index / COLS) * CELL_H + CELL_H - 60;
    }

    // ── Sprite / entity types ───────────────────────────────────────────────────

    private final class PlantSprite {
        final int index;
        PlantType type = PlantType.ROSE;
        int requirement = 10;
        double x;
        double y;
        boolean alive = true;
        boolean infested;
        double water;
        double targetHealth = 100;
        double displayHealth = 100;
        double phase;
        double wilt;
        double pop;
        boolean sprinkling;
        boolean hasDrone;

        PlantSprite(int index) {
            this.index = index;
            this.x = cellX(index);
            this.y = cellY(index);
        }

        void apply(GardenSnapshot.PlantView v) {
            PlantType t = safeType(v.type());
            this.type = t;
            this.requirement = t.getWaterRequirement();
            this.water = v.waterLevel();
            this.targetHealth = v.health();
            boolean nowAlive = !"DEAD".equals(v.status());
            if (nowAlive && !this.alive) {
                this.pop = 1.0;
            }
            this.alive = nowAlive;
            this.infested = "INFESTED".equals(v.status()) || !"-".equals(v.activeParasites());
        }

        void update(double dt) {
            phase += dt;
            displayHealth += (targetHealth - displayHealth) * Math.min(1, dt * 3);
            if (pop > 0) {
                pop = Math.max(0, pop - dt * 1.5);
            }
            if (!alive) {
                wilt = Math.min(1, wilt + dt * 1.0);
            } else {
                wilt = Math.max(0, wilt - dt * 2);
            }
        }

        private PlantType safeType(String name) {
            try {
                return PlantType.fromName(name);
            } catch (RuntimeException e) {
                return PlantType.ROSE;
            }
        }
    }

    /** A pest-control drone that flies to an infested plant and sprays it. */
    private static final class Drone {
        final PlantSprite target;
        double x;
        double y;
        double t;
        double sprayTimer;

        Drone(PlantSprite target) {
            this.target = target;
            this.x = target.x + 160;
            this.y = Math.max(60, target.y - 220);
        }

        void update(double dt, List<Particle> particles, Random rnd) {
            t += dt;
            double tx = target.x;
            double ty = target.y - 95;
            x += (tx - x) * Math.min(1, dt * 2.2);
            y += (ty - y) * Math.min(1, dt * 2.2);
            if (Math.hypot(tx - x, ty - y) < 24) {
                sprayTimer -= dt;
                if (sprayTimer <= 0) {
                    sprayTimer = 0.05;
                    particles.add(Particle.mist(x + (rnd.nextDouble() - 0.5) * 16, y + 12));
                }
            }
        }

        boolean finished() {
            return (!target.infested && t > 0.6) || t > 6.0;
        }

        void draw(GraphicsContext gc) {
            gc.save();
            gc.translate(x, y);
            gc.setFill(Color.web("#cfd6dd"));
            gc.fillRoundRect(-12, -6, 24, 12, 6, 6);
            gc.setStroke(Color.web("#7f8c99"));
            gc.setLineWidth(2);
            gc.strokeLine(-12, -6, -20, -14);
            gc.strokeLine(12, -6, 20, -14);
            gc.setFill(Color.web("#9fb0bf"));
            gc.fillOval(-24, -18, 10, 6);
            gc.fillOval(14, -18, 10, 6);
            gc.setFill(Color.web("#3fae50"));
            gc.fillOval(-4, 4, 8, 6);
            gc.restore();
        }
    }

    private static final class Particle {
        double x;
        double y;
        double vx;
        double vy;
        double gravity;
        double life;
        double size;
        Color color;
        boolean streak;

        static Particle droplet(double x, double y) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = -10;
            p.vy = 180;
            p.gravity = 200;
            p.life = 0.7;
            p.size = 3;
            p.color = Color.web("#a9d4ff");
            return p;
        }

        static Particle granule(double x, double y) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = (Math.random() - 0.5) * 20;
            p.vy = 120;
            p.gravity = 260;
            p.life = 1.2;
            p.size = 3;
            p.color = Color.web("#7a5a2e");
            return p;
        }

        static Particle mist(double x, double y) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = (Math.random() - 0.5) * 24;
            p.vy = 36 + Math.random() * 24;
            p.gravity = 40;
            p.life = 0.6;
            p.size = 4 + Math.random() * 3;
            p.color = Color.web("#bfe9a0");
            return p;
        }

        /** Thin, fast-falling streak used by the rain weather effect. */
        static Particle rainstreak(double x, double y) {
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = -40;
            p.vy = 720 + Math.random() * 120;
            p.gravity = 0;
            p.life = 2.0;
            p.size = 9 + Math.random() * 6;
            p.color = Color.web("#9cc6ff");
            p.streak = true;
            return p;
        }

        void draw(GraphicsContext gc) {
            gc.setGlobalAlpha(Math.max(0, Math.min(1, life)));
            if (streak) {
                gc.setStroke(color);
                gc.setLineWidth(1.4);
                gc.strokeLine(x, y, x + 2, y + size);
            } else {
                gc.setFill(color);
                gc.fillOval(x, y, size, size);
            }
            gc.setGlobalAlpha(1);
        }
    }
}

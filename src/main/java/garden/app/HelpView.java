package garden.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;

/**
 * "Help" tab — a self-contained, scrollable usage guide shown alongside
 * {@link GameView} and {@link DashboardView} in {@link GardenShell}.
 *
 * <p>The single most important thing it communicates: the watering,
 * fertilizer, pest-control and temperature subsystems all run
 * <b>automatically</b> every simulated day. The user does not keep plants
 * alive by hand — the automation does — and the bottom-bar buttons are
 * <i>disturbances</i> the gardener throws at an already-self-running garden.
 *
 * <p>It also offers buttons to open the bundled Markdown docs
 * ({@code README.md}, {@code docs/UserManual.md}) in the OS default app, for
 * readers who want the full manual.
 */
public class HelpView {

    private final VBox root;

    public HelpView() {
        VBox content = new VBox(18);
        content.setPadding(new Insets(28, 40, 36, 40));
        content.setMaxWidth(860);
        content.setStyle("-fx-background-color: #0c241a;");

        content.getChildren().add(title("Computerized Garden — Help"));
        content.getChildren().add(lead(
                "This is an automated garden simulation. The garden looks after "
                        + "itself: every simulated day the automation systems check each "
                        + "plant and act on their own. Your job is to watch, and "
                        + "optionally throw challenges at it to see how it copes."));

        content.getChildren().add(section("The two tabs"));
        content.getChildren().add(bullet("🌱 Living Garden",
                "An animated, autonomous board. Days advance on a timer (no clicking "
                        + "needed) and the plants, sprinklers, drones and shade panels "
                        + "animate straight from the live simulation state. Use the speed "
                        + "buttons (1x / 5x / 20x) to fast-forward many days."));
        content.getChildren().add(bullet("📋 Admin Dashboard",
                "A control-and-tables view: set exact event values, add plants, read the "
                        + "per-plant table, and watch log.txt update live. Both tabs share "
                        + "one engine, so a change in either is reflected in the other."));

        content.getChildren().add(section("These systems run automatically"));
        content.getChildren().add(lead(
                "You never have to water, fertilize or treat plants yourself. Each "
                        + "simulated day, in this fixed order, the automation runs:"));
        content.getChildren().add(bullet("💧 Watering",
                "Captures rain and auto-irrigates any plant below its water need — and "
                        + "it refills moisture BEFORE the daily health check, so a healthy "
                        + "plant simply stays healthy."));
        content.getChildren().add(bullet("🌡 Temperature control",
                "Above 95°F it slides shade panels across to cool the beds; below 55°F "
                        + "heat lamps warm them, pulling the temperature back toward a safe band."));
        content.getChildren().add(bullet("🐛 Pest control",
                "Detects infested plants and sends a drone to spray and clear the parasite. "
                        + "(Treatment removes the pest but does not restore health already lost.)"));
        content.getChildren().add(bullet("🌿 Fertilizer",
                "Monitors soil nutrients and feeds the beds when they drop too low, "
                        + "supporting gradual recovery."));

        content.getChildren().add(section("Your controls are disturbances, not life support"));
        content.getChildren().add(lead(
                "The bottom-bar buttons inject challenges at an already self-running "
                        + "garden — they are how you test it, not how you keep it alive:"));
        content.getChildren().add(bullet("🌧 Rain",
                "Adds water to every plant. A very large pour can over-water and damage plants."));
        content.getChildren().add(bullet("🔥 Heat Wave / ❄ Cold Snap",
                "Push the temperature to an extreme; go beyond a plant's tolerance and it "
                        + "takes heat/cold stress before the climate system recovers."));
        content.getChildren().add(bullet("🐛 Pest Outbreak",
                "Infests vulnerable plants with a random parasite. Repeated waves wear "
                        + "plants down because lost health is not restored."));
        content.getChildren().add(bullet("📋 Log State",
                "Writes a full garden snapshot to log.txt for inspection."));

        content.getChildren().add(section("Where results are recorded"));
        content.getChildren().add(lead(
                "Everything is logged to log.txt in the project folder (a CSV row per "
                        + "module action, plus a DAY_END summary each day). Open it to see "
                        + "exactly which subsystem did what, how many plants are alive/dead, "
                        + "and why any plant died (dehydration, overwatering, heat/cold "
                        + "stress, parasite infestation)."));

        content.getChildren().add(section("Full documentation"));
        content.getChildren().add(lead(
                "For the complete manual and design notes, open the bundled docs:"));
        content.getChildren().add(docButtons());

        ScrollPane scroll = new ScrollPane(centered(content));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #0c241a; -fx-background-color: #0c241a;");

        root = new VBox(scroll);
        VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        root.setStyle("-fx-background-color: #0c241a;");
    }

    public Node getRoot() {
        return root;
    }

    /** Wraps the content column so it stays centred and capped at maxWidth. */
    private Node centered(VBox content) {
        VBox holder = new VBox(content);
        holder.setAlignment(Pos.TOP_CENTER);
        holder.setStyle("-fx-background-color: #0c241a;");
        return holder;
    }

    private Label title(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 26));
        l.setStyle("-fx-text-fill: #eafff0;");
        return l;
    }

    private Label section(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 18));
        l.setStyle("-fx-text-fill: #ffe9a8; -fx-padding: 10 0 0 0;");
        return l;
    }

    /** A wide paragraph of body text. */
    private Label lead(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(780);
        l.setStyle("-fx-text-fill: #cfe9d6; -fx-font-size: 14px;");
        return l;
    }

    /** A "heading — body" row used for each tab / system / control. */
    private Node bullet(String heading, String body) {
        Label h = new Label(heading);
        h.setStyle("-fx-text-fill: #eafff0; -fx-font-size: 14px; -fx-font-weight: bold;");
        h.setMinWidth(190);
        h.setMaxWidth(190);
        h.setWrapText(true);
        Label b = new Label(body);
        b.setWrapText(true);
        b.setMaxWidth(560);
        b.setStyle("-fx-text-fill: #bfe3c9; -fx-font-size: 13px;");
        HBox row = new HBox(14, h, b);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(0, 0, 0, 8));
        return row;
    }

    /** Buttons that open the bundled Markdown docs in the OS default app. */
    private Node docButtons() {
        Button readme = docButton("Open README.md", "README.md");
        Button manual = docButton("Open User Manual", "docs/UserManual.md");
        Button design = docButton("Open Design Notes", "docs/Design.md");
        HBox box = new HBox(12, readme, manual, design);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(4, 0, 0, 8));
        return box;
    }

    private Button docButton(String label, String relativePath) {
        Button btn = new Button(label);
        btn.setStyle("-fx-background-color: #2e7d49; -fx-text-fill: white; -fx-font-weight: bold;");
        btn.setOnAction(e -> openDoc(relativePath));
        File f = new File(relativePath);
        if (!f.exists()) {
            btn.setDisable(true);
            btn.setText(label + " (not found)");
        }
        return btn;
    }

    /**
     * Opens the given project-relative doc with the OS default handler. Degrades
     * silently if AWT Desktop is unavailable or the file is gone — the tab's
     * own text is the primary content, the docs are a bonus.
     */
    private void openDoc(String relativePath) {
        try {
            File f = new File(relativePath).getAbsoluteFile();
            if (f.exists() && java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(f);
            } else {
                System.err.println("[HelpView] cannot open doc: " + relativePath);
            }
        } catch (Exception ex) {
            System.err.println("[HelpView] could not open " + relativePath + ": " + ex);
        }
    }
}

package garden.app;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Tiny, dependency-free Markdown renderer good enough for this project's docs.
 * It is <i>not</i> a full CommonMark implementation — it covers the block and
 * inline constructs the bundled {@code README.md} / {@code docs/*.md} actually
 * use, and renders them into JavaFX nodes (so the Help tab's doc viewer shows
 * formatted text rather than raw Markdown).
 *
 * <p>Supported: ATX headings (#, ##, ###…), fenced code blocks (```), inline
 * code (`code`), bold (**…**), bullet lists (-, *), ordered lists (1.),
 * block-quotes (&gt;), horizontal rules (---), and pipe tables (rendered
 * monospaced). Anything else renders as a plain paragraph.
 */
final class SimpleMarkdownView {

    private static final double BODY_SIZE = 13.5;
    private static final String TEXT_COLOR = "#222222";

    private SimpleMarkdownView() {
    }

    /** Renders the markdown source into a scrollable, formatted node. */
    static Node render(String markdown) {
        VBox column = new VBox(8);
        column.setPadding(new Insets(20, 26, 24, 26));
        column.setStyle("-fx-background-color: #fdfdfb;");

        String[] lines = markdown.replace("\r\n", "\n").split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];

            // Fenced code block: gather until the closing fence.
            if (line.trim().startsWith("```")) {
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().startsWith("```")) {
                    code.append(lines[i]).append('\n');
                    i++;
                }
                i++; // skip closing fence (or run off the end)
                column.getChildren().add(codeBlock(code.toString()));
                continue;
            }

            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                i++;
                continue;
            }

            // Horizontal rule.
            if (trimmed.matches("-{3,}|\\*{3,}|_{3,}")) {
                column.getChildren().add(new Separator());
                i++;
                continue;
            }

            // ATX heading.
            int hashes = 0;
            while (hashes < trimmed.length() && trimmed.charAt(hashes) == '#') {
                hashes++;
            }
            if (hashes > 0 && hashes <= 6 && hashes < trimmed.length()
                    && trimmed.charAt(hashes) == ' ') {
                column.getChildren().add(heading(trimmed.substring(hashes + 1).trim(), hashes));
                i++;
                continue;
            }

            // Pipe table: gather consecutive lines containing '|'.
            if (trimmed.contains("|")) {
                StringBuilder table = new StringBuilder();
                while (i < lines.length && lines[i].contains("|") && !lines[i].trim().isEmpty()) {
                    String row = lines[i].trim();
                    if (!row.matches("[\\s|:\\-]+")) { // skip the |---|---| separator row
                        table.append(row).append('\n');
                    }
                    i++;
                }
                column.getChildren().add(codeBlock(table.toString()));
                continue;
            }

            // Block-quote.
            if (trimmed.startsWith(">")) {
                column.getChildren().add(blockQuote(trimmed.replaceFirst(">\\s?", "")));
                i++;
                continue;
            }

            // Bullet list item.
            if (trimmed.matches("[-*]\\s+.*")) {
                column.getChildren().add(listItem("•  ", trimmed.replaceFirst("[-*]\\s+", "")));
                i++;
                continue;
            }

            // Ordered list item.
            java.util.regex.Matcher ol = java.util.regex.Pattern
                    .compile("(\\d+)\\.\\s+(.*)").matcher(trimmed);
            if (ol.matches()) {
                column.getChildren().add(listItem(ol.group(1) + ".  ", ol.group(2)));
                i++;
                continue;
            }

            // Plain paragraph.
            column.getChildren().add(paragraph(trimmed, BODY_SIZE, 0));
            i++;
        }

        ScrollPane scroll = new ScrollPane(column);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #fdfdfb; -fx-background-color: #fdfdfb;");
        return scroll;
    }

    private static Label heading(String text, int level) {
        double size = switch (level) {
            case 1 -> 23;
            case 2 -> 18;
            case 3 -> 15.5;
            default -> 14;
        };
        Label l = new Label(stripInlineMarks(text));
        l.setFont(Font.font("System", FontWeight.BOLD, size));
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: #16341f; -fx-padding: " + (level <= 2 ? "12 0 2 0" : "8 0 0 0") + ";");
        return l;
    }

    private static TextFlow paragraph(String text, double size, double leftPad) {
        TextFlow flow = parseInline(text, size);
        flow.setPadding(new Insets(0, 0, 0, leftPad));
        return flow;
    }

    private static Node listItem(String marker, String text) {
        TextFlow flow = new TextFlow();
        Text bullet = new Text(marker);
        bullet.setFont(Font.font("System", BODY_SIZE));
        bullet.setStyle("-fx-fill: " + TEXT_COLOR + ";");
        flow.getChildren().add(bullet);
        flow.getChildren().addAll(parseInline(text, BODY_SIZE).getChildren());
        flow.setPadding(new Insets(0, 0, 0, 14));
        return flow;
    }

    private static Node blockQuote(String text) {
        TextFlow flow = parseInline(text, BODY_SIZE);
        for (Node n : flow.getChildren()) {
            if (n instanceof Text t) {
                t.setFont(Font.font("System", FontPosture.ITALIC, BODY_SIZE));
            }
        }
        flow.setPadding(new Insets(2, 0, 2, 16));
        flow.setStyle("-fx-border-color: transparent transparent transparent #b9d6c2;"
                + " -fx-border-width: 0 0 0 3; -fx-background-color: #f1f6f2;");
        return flow;
    }

    private static Region codeBlock(String code) {
        Label l = new Label(code.stripTrailing());
        l.setWrapText(false);
        l.setFont(Font.font("monospace", 12.5));
        l.setStyle("-fx-text-fill: #1b1b1b;");
        VBox holder = new VBox(l);
        holder.setPadding(new Insets(10, 12, 10, 12));
        holder.setStyle("-fx-background-color: #eef1ee; -fx-background-radius: 5;");
        return holder;
    }

    /**
     * Parses inline {@code **bold**} and {@code `code`} runs into a
     * {@link TextFlow}. Unmatched markers are treated as literal text.
     */
    private static TextFlow parseInline(String s, double size) {
        TextFlow flow = new TextFlow();
        StringBuilder buf = new StringBuilder();
        boolean bold = false;
        int n = s.length();
        for (int k = 0; k < n; k++) {
            char c = s.charAt(k);
            if (c == '`') {
                flushText(flow, buf.toString(), bold, size);
                buf.setLength(0);
                int end = s.indexOf('`', k + 1);
                if (end < 0) {
                    buf.append(c);
                } else {
                    Label code = new Label(s.substring(k + 1, end));
                    code.setFont(Font.font("monospace", size));
                    code.setStyle("-fx-background-color: #e7ece7; -fx-padding: 0 3 0 3;"
                            + " -fx-text-fill: #224; -fx-background-radius: 3;");
                    flow.getChildren().add(code);
                    k = end;
                }
            } else if (c == '*' && k + 1 < n && s.charAt(k + 1) == '*') {
                flushText(flow, buf.toString(), bold, size);
                buf.setLength(0);
                bold = !bold;
                k++; // consume second '*'
            } else {
                buf.append(c);
            }
        }
        flushText(flow, buf.toString(), bold, size);
        return flow;
    }

    private static void flushText(TextFlow flow, String s, boolean bold, double size) {
        if (s.isEmpty()) {
            return;
        }
        Text t = new Text(s);
        t.setFont(bold ? Font.font("System", FontWeight.BOLD, size) : Font.font("System", size));
        t.setStyle("-fx-fill: " + TEXT_COLOR + ";");
        flow.getChildren().add(t);
    }

    /** Strips inline markers from heading text (headings are styled, not parsed). */
    private static String stripInlineMarks(String s) {
        return s.replace("**", "").replace("`", "");
    }
}

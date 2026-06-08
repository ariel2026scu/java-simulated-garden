package garden.logging;

import garden.model.Garden;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class GardenLogger {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int RECENT_CAPACITY = 50;
    private final Path logPath;
    private final Deque<LogEntry> recent = new ArrayDeque<>();

    /** Compact projection of a log row for in-memory consumers (e.g. the game-tab event panel). */
    public record LogEntry(int day, String event, String value, String module, String action) { }

    public GardenLogger(Path logPath) {
        this.logPath = logPath;
    }

    public synchronized void reset() {
        try {
            Files.writeString(logPath, "TIMESTAMP,DAY,EVENT,EVENT_VALUE,MODULE,ACTION,PLANTS_ALIVE,PLANTS_DEAD,DETAILS%n".formatted(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not reset garden log: " + e.getMessage());
        }
        recent.clear();
    }

    public synchronized void log(int day, String event, String value, String module, String action, Garden garden, String details) {
        String row = String.join(",",
                sanitize(LocalDateTime.now().format(FORMATTER)),
                Integer.toString(day),
                sanitize(event),
                sanitize(value),
                sanitize(module),
                sanitize(action),
                Integer.toString(garden.getAlivePlants().size()),
                Integer.toString(garden.getDeadPlants().size()),
                sanitize(details)) + System.lineSeparator();
        try {
            Files.writeString(logPath, row, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Could not write garden log: " + e.getMessage());
        }
        recent.addLast(new LogEntry(day, event, value, module, action));
        while (recent.size() > RECENT_CAPACITY) {
            recent.removeFirst();
        }
    }

    /**
     * Snapshot of the most recent rows written to log.txt, oldest first.
     * Lets the UI render a "recent events" strip without re-reading the file.
     */
    public synchronized List<LogEntry> recentEntries() {
        return new ArrayList<>(recent);
    }

    public Path getLogPath() {
        return logPath;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return '"' + value.replace("\"", "'").replace("\n", " ") + '"';
    }
}

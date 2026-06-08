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
    private static final int USER_EVENTS_CAPACITY = 20;
    private final Path logPath;
    private final Deque<LogEntry> recent = new ArrayDeque<>();
    /**
     * Separate ring buffer for explicit user-fired events so the autonomous-tick
     * rows (~11 per AUTO_TICK day) can't evict them from the main 50-row buffer
     * within a few seconds. Capped at 20 so even a long unattended run still
     * remembers the last 20 things the gardener actually did.
     */
    private final Deque<LogEntry> recentUserEvents = new ArrayDeque<>();

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
        recentUserEvents.clear();
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
        LogEntry entry = new LogEntry(day, event, value, module, action);
        recent.addLast(entry);
        while (recent.size() > RECENT_CAPACITY) {
            recent.removeFirst();
        }
        // User-fired engine events: one EVENT_RECEIVED row per submitEvent
        // call, excluding the autonomous AUTO_TICK source so the panel won't
        // be drowned in background simulation ticks.
        if ("EVENT_RECEIVED".equals(action) && !"AUTO_TICK".equals(event)) {
            recentUserEvents.addLast(entry);
            while (recentUserEvents.size() > USER_EVENTS_CAPACITY) {
                recentUserEvents.removeFirst();
            }
        }
    }

    /**
     * Snapshot of the most recent rows written to log.txt, oldest first.
     * Lets the UI render a "recent events" strip without re-reading the file.
     */
    public synchronized List<LogEntry> recentEntries() {
        return new ArrayList<>(recent);
    }

    /**
     * Recent gardener-fired events only (excludes AUTO_TICK background rows).
     * Survives long unattended runs because its eviction policy isn't tied to
     * the total log volume.
     */
    public synchronized List<LogEntry> recentUserEntries() {
        return new ArrayList<>(recentUserEvents);
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

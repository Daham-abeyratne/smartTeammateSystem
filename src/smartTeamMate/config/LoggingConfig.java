package smartTeamMate.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingConfig {

    private static boolean initialized = false;

    public static void setup() {
        if (initialized) return;
        initialized = true;

        try {
            // Ensure logs folder exists
            Path logDir = Path.of("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectory(logDir);
            }

            // Create log file handler
            FileHandler fileHandler = new FileHandler("logs/system.log", true);
            fileHandler.setFormatter(new SimpleFormatter());

            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.ALL);

            // Remove console logging handlers
            for (var handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            // Add file logging only
            rootLogger.addHandler(fileHandler);

        } catch (IOException e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }
}

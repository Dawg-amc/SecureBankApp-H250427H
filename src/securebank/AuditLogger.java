package securebank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;

/** Append-only security audit log. Never records passwords or hashes. */
public class AuditLogger {
    private final Path file;

    public AuditLogger(Path file) { this.file = file; }

    public void log(String event, String username, String detail) {
        String line = Instant.now() + " | " + event + " | user=" + clean(username) + " | " + clean(detail) + System.lineSeparator();
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Warning: could not write audit log.");
        }
    }

    /** Strips CR/LF so user input cannot forge extra log lines (log injection). */
    private static String clean(String s) {
        return s == null ? "-" : s.replaceAll("[\\r\\n|]", "_");
    }
}

package com.yichen.recalltotem.audit;

import com.yichen.recalltotem.RecallTotemMod;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public final class AuditManager {
    private static final Path LOG_PATH = com.yichen.recalltotem.config.ModConfig.getConfigDir().resolve("logs").resolve("recalltotem-region-audit.log");

    private AuditManager() {}

    public static synchronized void log(String actor, String action, String details) {
        try {
            Path parent = LOG_PATH.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            String line = Instant.now().toString() + " | " + actor + " | " + action + " | " + details + System.lineSeparator();
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(LOG_PATH, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
                pw.print(line);
            }
        } catch (IOException e) {
            RecallTotemMod.LOGGER.warn("Failed to write audit log", e);
        }
    }

    public static synchronized java.util.List<String> tail(int maxLines) {
        try {
            if (Files.notExists(LOG_PATH)) return java.util.Collections.emptyList();
            java.util.List<String> all = Files.readAllLines(LOG_PATH);
            if (all.size() <= maxLines) return all;
            return all.subList(Math.max(0, all.size() - maxLines), all.size());
        } catch (IOException e) {
            RecallTotemMod.LOGGER.warn("Failed to read audit log", e);
            return java.util.Collections.emptyList();
        }
    }
}

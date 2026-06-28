package com.yichen.recalltotem.web;

import com.google.gson.Gson;
import com.yichen.recalltotem.RecallTotemMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;

public final class WebhookDeliveryStore {
    private static final int DEFAULT_MAX = 500;
    private static final Deque<Map<String, Object>> buffer = new ArrayDeque<>();
    private static final int maxEntries = DEFAULT_MAX;
    private static final Path STORE_PATH = com.yichen.recalltotem.config.ModConfig.getConfigDir().resolve("logs").resolve("recalltotem-webhook-deliveries.jsonl");
    private static final Gson GSON = new Gson();

    private WebhookDeliveryStore() {}

    public static synchronized void record(String timestamp, String event, String actor, int httpCode, String responseSnippet) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", UUID.randomUUID().toString());
            entry.put("timestamp", timestamp == null ? Instant.now().toString() : timestamp);
            entry.put("event", event);
            entry.put("actor", actor);
            entry.put("httpCode", httpCode);
            entry.put("response", responseSnippet == null ? "" : responseSnippet);
            entry.put("attempts", 1);
            entry.put("lastStatus", httpCode);
            entry.put("doNotRetry", false);
            entry.put("nextRetryAt", computeNextRetryIso(1));
            buffer.addLast(entry);
            while (buffer.size() > maxEntries) buffer.removeFirst();
            try {
                Files.createDirectories(STORE_PATH.getParent());
                String line = GSON.toJson(entry) + System.lineSeparator();
                Files.writeString(STORE_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                RecallTotemMod.LOGGER.debug("Failed to persist webhook delivery", e);
            }
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("WebhookDeliveryStore.record failed", e);
        }
    }

    public static synchronized List<Map<String, Object>> recent() {
        return new ArrayList<>(buffer);
    }

    public static synchronized Optional<Map<String, Object>> getByIndex(int index) {
        if (index < 0 || index >= buffer.size()) return Optional.empty();
        return Optional.of(new ArrayList<>(buffer).get(index));
    }

    public static synchronized void updateAttempt(Map<String, Object> entry, int httpCode, String responseSnippet) {
        try {
            int attempts = ((Number) entry.getOrDefault("attempts", 0)).intValue();
            attempts++;
            entry.put("attempts", attempts);
            entry.put("lastStatus", httpCode);
            entry.put("response", responseSnippet == null ? "" : responseSnippet);
            entry.put("nextRetryAt", computeNextRetryIso(attempts));
            try {
                Files.createDirectories(STORE_PATH.getParent());
                String line = GSON.toJson(Map.of(
                    "resend_of", entry.get("id"),
                    "timestamp", Instant.now().toString(),
                    "httpCode", httpCode,
                    "response", responseSnippet
                )) + System.lineSeparator();
                Files.writeString(STORE_PATH, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (Exception e) {
                RecallTotemMod.LOGGER.debug("Failed to persist webhook delivery update", e);
            }
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("WebhookDeliveryStore.updateAttempt failed", e);
        }
    }

    public static synchronized void markDoNotRetry(Map<String, Object> entry, boolean value) {
        entry.put("doNotRetry", value);
    }

    private static String computeNextRetryIso(int attempts) {
        try {
            var cfg = com.yichen.recalltotem.config.ModConfig.get();
            double multiplier = cfg.webhookRetryBackoffMultiplier > 0 ? cfg.webhookRetryBackoffMultiplier : 2.0;
            long baseSeconds = Math.max(1, cfg.webhookRetryIntervalSeconds);
            double backoff = baseSeconds * Math.pow(multiplier, Math.max(0, attempts - 1));
            Instant next = Instant.now().plusSeconds((long) backoff);
            return next.toString();
        } catch (Exception e) {
            return Instant.now().plusSeconds(60).toString();
        }
    }
}

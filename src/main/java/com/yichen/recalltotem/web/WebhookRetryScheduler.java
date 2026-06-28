package com.yichen.recalltotem.web;

import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.audit.AuditManager;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public final class WebhookRetryScheduler {
    private static final ScheduledExecutorService SCHED = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RecallTotem-WebhookRetry");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> task;

    private WebhookRetryScheduler() {}

    public static void start() {
        try {
            var cfg = ModConfig.get();
            if (!cfg.enableWebhooks || !cfg.webhookRetryEnabled) return;
            long interval = Math.max(10, cfg.webhookRetryIntervalSeconds);
            task = SCHED.scheduleAtFixedRate(WebhookRetryScheduler::runOnce, 5, interval, TimeUnit.SECONDS);
            RecallTotemMod.LOGGER.info("WebhookRetryScheduler started with interval " + interval + "s");
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to start WebhookRetryScheduler", e);
        }
    }

    public static void stop() {
        if (task != null) task.cancel(false);
        SCHED.shutdownNow();
    }

    private static void runOnce() {
        try {
            var cfg = ModConfig.get();
            List<Map<String, Object>> recent = WebhookDeliveryStore.recent();
            for (int i = 0; i < recent.size(); i++) {
                Map<String, Object> entry = recent.get(i);
                boolean doNotRetry = Boolean.TRUE.equals(entry.getOrDefault("doNotRetry", false));
                if (doNotRetry) continue;
                int attempts = ((Number) entry.getOrDefault("attempts", 0)).intValue();
                int lastStatus = ((Number) entry.getOrDefault("lastStatus", -1)).intValue();
                String nextRetryAt = (String) entry.getOrDefault("nextRetryAt", Instant.now().toString());
                Instant nextRetry = Instant.parse(nextRetryAt);
                if (Instant.now().isBefore(nextRetry)) continue;
                if (attempts >= Math.max(1, cfg.webhookRetryMaxAttempts)) {
                    entry.put("doNotRetry", true);
                    AuditManager.log("system", "webhook.retry.exhausted", "id=" + entry.get("id") + " attempts=" + attempts);
                    continue;
                }
                if (lastStatus >= 200 && lastStatus < 300) continue;
                String event = String.valueOf(entry.getOrDefault("event", "unknown"));
                String actor = String.valueOf(entry.getOrDefault("actor", "system"));
                Map<String, String> details = Map.of("retry_of", String.valueOf(entry.getOrDefault("timestamp", Instant.now().toString())));
                WebhookNotifier.notify(event, actor, details, true);
                WebhookDeliveryStore.updateAttempt(entry, -1, "retry-sent");
                AuditManager.log("system", "webhook.retry.attempt", "id=" + entry.get("id") + " attempts=" + (attempts + 1));
            }
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("WebhookRetryScheduler runOnce failed", e);
        }
    }
}

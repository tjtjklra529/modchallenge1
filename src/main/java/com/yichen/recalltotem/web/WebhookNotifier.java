package com.yichen.recalltotem.web;

import com.google.gson.Gson;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.RecallTotemMod;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class WebhookNotifier {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 500L;

    private WebhookNotifier() {}

    public static void notify(String eventType, String actor, Map<String, String> details) {
        notify(eventType, actor, details, false);
    }

    public static void notify(String eventType, String actor, Map<String, String> details, boolean isRetry) {
        try {
            var cfg = ModConfig.get();
            if (!cfg.enableWebhooks || cfg.webhookUrl == null || cfg.webhookUrl.isEmpty()) return;

            var payload = Map.of(
                "event", eventType,
                "actor", actor,
                "timestamp", Instant.now().toString(),
                "details", details == null ? Map.of() : details
            );

            byte[] body = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
            int maxRetries = cfg.webhookRetries > 0 ? cfg.webhookRetries : DEFAULT_RETRIES;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                HttpURLConnection conn = null;
                int code = -1;
                String respSnippet = "";
                try {
                    URL url = new URL(cfg.webhookUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(5000);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    if (cfg.webhookSecretHeader != null && !cfg.webhookSecretHeader.isEmpty()) {
                        String signature = computeHmacSha256Base64(body, cfg.webhookSecretHeader);
                        conn.setRequestProperty("X-RecallTotem-Signature", signature);
                    }
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body);
                    }
                    code = conn.getResponseCode();
                    InputStream in = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
                    if (in != null) {
                        byte[] buf = new byte[512];
                        int read = in.read(buf);
                        if (read > 0) respSnippet = new String(buf, 0, Math.min(read, 256), StandardCharsets.UTF_8);
                    }
                    WebhookDeliveryStore.record(Instant.now().toString(), eventType, actor, code, respSnippet);
                    if (code >= 200 && code < 300) {
                        return;
                    } else {
                        RecallTotemMod.LOGGER.warn("Webhook attempt " + attempt + " returned HTTP " + code);
                    }
                } catch (Exception e) {
                    WebhookDeliveryStore.record(Instant.now().toString(), eventType, actor, -1, e.getMessage());
                    RecallTotemMod.LOGGER.warn("Webhook attempt " + attempt + " failed", e);
                } finally {
                    if (conn != null) conn.disconnect();
                }
                long backoff = (long) (BASE_BACKOFF_MS * Math.pow(cfg.webhookRetryBackoffMultiplier > 0 ? cfg.webhookRetryBackoffMultiplier : 2.0, attempt - 1));
                try { TimeUnit.MILLISECONDS.sleep(backoff); } catch (InterruptedException ignored) {}
            }
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to send webhook notification", e);
        }
    }

    private static String computeHmacSha256Base64(byte[] data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data);
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to compute HMAC signature", e);
            return "";
        }
    }
}

package com.yichen.recalltotem.web;

import com.google.gson.Gson;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.audit.AuditManager;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class WebhookResendHandler {
    private static final Gson GSON = new Gson();
    private static HttpServer server;

    public static void start() {
        try {
            ModConfig cfg = ModConfig.get();
            if (!cfg.enableWebExport || cfg.webExportToken == null || cfg.webExportToken.isEmpty()) return;
            server = HttpServer.create(new InetSocketAddress(cfg.webExportPort + 1), 0);
            server.createContext("/recalltotem/webhook-resend", (HttpExchange exchange) -> {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("token=" + cfg.webExportToken)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                var params = parseQuery(query);
                String idStr = params.get("id");
                if (idStr == null) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                int id;
                try { id = Integer.parseInt(idStr); } catch (NumberFormatException ex) {
                    exchange.sendResponseHeaders(400, -1);
                    return;
                }
                List<Map<String, Object>> recent = WebhookDeliveryStore.recent();
                if (id < 0 || id >= recent.size()) {
                    exchange.sendResponseHeaders(404, -1);
                    return;
                }
                Map<String, Object> entry = recent.get(id);
                String event = String.valueOf(entry.getOrDefault("event", "unknown"));
                String actor = String.valueOf(entry.getOrDefault("actor", "system"));
                Map<String, String> details = Map.of("resend_of", String.valueOf(entry.getOrDefault("timestamp", "")));
                WebhookNotifier.notify(event, actor, details);
                AuditManager.log("system", "webhook.resend", "id=" + id + " event=" + event);
                String resp = GSON.toJson(Map.of("status", "ok", "id", id));
                byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            RecallTotemMod.LOGGER.info("WebhookResendHandler started on port " + (cfg.webExportPort + 1));
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to start WebhookResendHandler", e);
        }
    }

    private static java.util.Map<String, String> parseQuery(String q) {
        var map = new java.util.HashMap<String, String>();
        for (String part : q.split("&")) {
            int i = part.indexOf('=');
            if (i > 0) map.put(part.substring(0, i), part.substring(i + 1));
        }
        return map;
    }

    public static void stop() {
        if (server != null) server.stop(0);
    }
}

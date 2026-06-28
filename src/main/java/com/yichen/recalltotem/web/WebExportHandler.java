package com.yichen.recalltotem.web;

import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.RecallTotemMod;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class WebExportHandler {
    private static HttpServer server;
    private static final Gson GSON = new Gson();

    public static void start() {
        try {
            ModConfig cfg = ModConfig.get();
            if (!cfg.enableWebExport || cfg.webExportToken == null || cfg.webExportToken.isEmpty()) return;
            server = HttpServer.create(new InetSocketAddress(cfg.webExportPort), 0);

            server.createContext("/recalltotem/regions", (HttpExchange exchange) -> {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("token=" + cfg.webExportToken)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                String json = GSON.toJson(cfg.protectedRegions);
                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            server.createContext("/recalltotem/audit", (HttpExchange exchange) -> {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("token=" + cfg.webExportToken)) {
                    exchange.sendResponseHeaders(403, -1);
                    return;
                }
                var lines = com.yichen.recalltotem.audit.AuditManager.tail(500);
                StringBuilder sb = new StringBuilder();
                for (String l : lines) {
                    sb.append(l).append("\n");
                }
                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });

            if (cfg.enableWebDashboard) {
                server.createContext("/recalltotem/dashboard/index.html", (HttpExchange exchange) -> {
                    String html = WebExportHandler.loadResource("/web/dashboard/index.html");
                    byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
                server.createContext("/recalltotem/dashboard/regions.js", (HttpExchange exchange) -> {
                    String js = WebExportHandler.loadResource("/web/dashboard/regions.js");
                    byte[] bytes = js.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/javascript; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
                server.createContext("/recalltotem/dashboard/deliveries.js", (HttpExchange exchange) -> {
                    String js = WebExportHandler.loadResource("/web/dashboard/deliveries.js");
                    byte[] bytes = js.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/javascript; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
            }

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            RecallTotemMod.LOGGER.info("WebExportHandler started on port " + cfg.webExportPort);
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to start WebExportHandler", e);
        }
    }

    public static void stop() {
        if (server != null) server.stop(0);
    }

    private static String loadResource(String path) {
        try (var in = WebExportHandler.class.getResourceAsStream(path)) {
            if (in == null) return "";
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to load resource " + path, e);
            return "";
        }
    }
}

package com.yichen.recalltotem.config;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    @SerializedName("protected_regions")
    public List<ProtectedRegion> protectedRegions = new ArrayList<>();

    @SerializedName("protect_spawn_from_anchors")
    public boolean protectSpawnFromAnchors = true;

    @SerializedName("protected_spawn_radius_blocks")
    public int protectedSpawnRadiusBlocks = 64;

    @SerializedName("enable_web_export")
    public boolean enableWebExport = false;

    @SerializedName("web_export_port")
    public int webExportPort = 8123;

    @SerializedName("web_export_token")
    public String webExportToken = "";

    @SerializedName("allow_region_claiming")
    public boolean allowRegionClaiming = true;

    @SerializedName("enable_webhooks")
    public boolean enableWebhooks = false;

    @SerializedName("webhook_url")
    public String webhookUrl = "";

    @SerializedName("webhook_secret_header")
    public String webhookSecretHeader = "";

    @SerializedName("webhook_retries")
    public int webhookRetries = 3;

    @SerializedName("enable_web_dashboard")
    public boolean enableWebDashboard = false;

    @SerializedName("webhook_retry_enabled")
    public boolean webhookRetryEnabled = true;

    @SerializedName("webhook_retry_interval_seconds")
    public int webhookRetryIntervalSeconds = 60;

    @SerializedName("webhook_retry_max_attempts")
    public int webhookRetryMaxAttempts = 5;

    @SerializedName("webhook_retry_backoff_multiplier")
    public double webhookRetryBackoffMultiplier = 2.0;

    private static ModConfig INSTANCE = null;

    public static ModConfig get() {
        if (INSTANCE == null) INSTANCE = new ModConfig();
        return INSTANCE;
    }

    public static Path getConfigDir() {
        return Paths.get("config").resolve("recalltotem");
    }

    public static void loadOrCreateDefault() {
        INSTANCE = new ModConfig();
    }

    public static void save() {
        // placeholder: real code writes config file
    }

    @Override
    public String toString() {
        return "ModConfig{regions=" + protectedRegions.size() + "}";
    }
}

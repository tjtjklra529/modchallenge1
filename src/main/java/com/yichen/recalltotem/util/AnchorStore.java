package com.yichen.recalltotem.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class AnchorStore {
    private AnchorStore() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class AnchorEntry {
        public String name;
        public int blockX, blockY, blockZ;
        public String dimension;
    }

    private static Path worldDir = null;

    public static void setWorldDir(Path dir) {
        worldDir = dir;
    }

    public static void clearWorldDir() {
        worldDir = null;
    }

    private static Path dataFile() {
        Path base = worldDir != null ? worldDir : ModConfig.getConfigDir();
        return base.resolve("recalltotem_anchors.json");
    }

    private static Map<String, List<AnchorEntry>> loadAll() {
        Path file = dataFile();
        if (!Files.exists(file)) return new HashMap<>();
        try {
            Map<String, List<AnchorEntry>> map = GSON.fromJson(Files.readString(file),
                new TypeToken<Map<String, List<AnchorEntry>>>(){}.getType());
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to read anchor data", e);
            return new HashMap<>();
        }
    }

    private static void saveAll(Map<String, List<AnchorEntry>> map) {
        try {
            Files.createDirectories(dataFile().getParent());
            Files.writeString(dataFile(), GSON.toJson(map));
        } catch (IOException e) {
            RecallTotemMod.LOGGER.warn("Failed to save anchor data", e);
        }
    }

    public static List<AnchorEntry> getAnchors(UUID playerUuid) {
        return new ArrayList<>(loadAll().getOrDefault(playerUuid.toString(), new ArrayList<>()));
    }

    public static boolean isLoaded(int blockX, int blockY, int blockZ, String dimension) {
        for (List<AnchorEntry> entries : loadAll().values()) {
            for (AnchorEntry e : entries) {
                if (e.blockX == blockX && e.blockY == blockY && e.blockZ == blockZ
                        && dimension.equals(e.dimension)) return true;
            }
        }
        return false;
    }

    public static void addAnchor(UUID playerUuid, String name, int blockX, int blockY, int blockZ, String dimension) {
        Map<String, List<AnchorEntry>> all = loadAll();
        List<AnchorEntry> entries = all.computeIfAbsent(playerUuid.toString(), k -> new ArrayList<>());
        entries.removeIf(e -> e.blockX == blockX && e.blockY == blockY && e.blockZ == blockZ
                && dimension.equals(e.dimension));
        AnchorEntry entry = new AnchorEntry();
        entry.name = name;
        entry.blockX = blockX;
        entry.blockY = blockY;
        entry.blockZ = blockZ;
        entry.dimension = dimension;
        entries.add(entry);
        saveAll(all);
    }

    public static void removeAnchorAt(int blockX, int blockY, int blockZ, String dimension) {
        Map<String, List<AnchorEntry>> all = loadAll();
        boolean changed = false;
        for (List<AnchorEntry> entries : all.values()) {
            changed |= entries.removeIf(e -> e.blockX == blockX && e.blockY == blockY
                    && e.blockZ == blockZ && dimension.equals(e.dimension));
        }
        if (changed) saveAll(all);
    }
}

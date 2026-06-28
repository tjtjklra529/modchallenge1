package com.yichen.recalltotem.tools;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import com.yichen.recalltotem.RecallTotemMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class WhitelistBulkTool {
    private static final Gson GSON = new Gson();

    private WhitelistBulkTool() {}

    public static class Entry {
        public int index;
        public List<String> whitelist = new ArrayList<>();
    }

    public static List<String> validate(Path file) throws Exception {
        List<String> errors = new ArrayList<>();
        String json = Files.readString(file);
        Entry[] entries;
        try {
            entries = GSON.fromJson(json, Entry[].class);
        } catch (JsonSyntaxException e) {
            throw new Exception("Invalid JSON: " + e.getMessage());
        }
        var regions = ModConfig.get().protectedRegions;
        for (Entry e : entries) {
            if (e.index < 0 || e.index >= regions.size()) {
                errors.add("Invalid index: " + e.index);
                continue;
            }
            for (String s : e.whitelist) {
                try {
                    UUID.fromString(s);
                } catch (Exception ex) {
                    errors.add("Invalid UUID '" + s + "' for index " + e.index);
                }
            }
        }
        return errors;
    }

    public static int apply(Path file) throws Exception {
        Entry[] entries = GSON.fromJson(Files.readString(file), Entry[].class);
        var regions = ModConfig.get().protectedRegions;
        int applied = 0;
        for (Entry e : entries) {
            if (e.index < 0 || e.index >= regions.size()) continue;
            ProtectedRegion r = regions.get(e.index);
            r.whitelist.clear();
            for (String s : e.whitelist) {
                r.whitelist.add(s);
            }
            applied++;
        }
        ModConfig.save();
        RecallTotemMod.LOGGER.info("Applied whitelist bulk import: " + applied + " region(s)");
        return applied;
    }
}

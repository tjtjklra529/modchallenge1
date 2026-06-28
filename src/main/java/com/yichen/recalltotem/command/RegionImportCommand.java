package com.yichen.recalltotem.command;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionImportCommand {
    private static final Gson GSON = new Gson();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("import")
                            .then(argument("json", StringArgumentType.greedyString())
                                .executes(ctx -> importRegions(ctx.getSource(), StringArgumentType.getString(ctx, "json")))
                            )
                        )
                        .then(literal("import-file")
                            .then(argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> importFile(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )
            );
        });
    }

    private static int importRegions(ServerCommandSource source, String json) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to import regions."));
            return 0;
        }
        try {
            ProtectedRegion[] regions = GSON.fromJson(json, ProtectedRegion[].class);
            if (regions == null || regions.length == 0) {
                source.sendError(Text.literal("No regions found in JSON."));
                return 0;
            }
            for (ProtectedRegion r : regions) {
                ModConfig.get().protectedRegions.add(r);
            }
            ModConfig.save();
            source.sendFeedback(() -> Text.literal("Imported " + regions.length + " protected region(s)."), false);
            String actor = source.getName();
            AuditManager.log(actor, "region.import", "count=" + regions.length);
            RecallTotemMod.LOGGER.info("Imported " + regions.length + " protected regions via command.");
            return regions.length;
        } catch (JsonSyntaxException e) {
            source.sendError(Text.literal("Invalid JSON: " + e.getMessage()));
            return 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to import regions."));
            RecallTotemMod.LOGGER.warn("Region import failed", e);
            return 0;
        }
    }

    private static int importFile(ServerCommandSource source, String pathStr) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to import regions from file."));
            return 0;
        }
        try {
            java.nio.file.Path path = java.nio.file.Path.of(pathStr);
            if (!java.nio.file.Files.exists(path)) {
                source.sendError(Text.literal("File not found: " + pathStr));
                return 0;
            }
            String json = java.nio.file.Files.readString(path);
            ProtectedRegion[] regions = GSON.fromJson(json, ProtectedRegion[].class);
            if (regions == null || regions.length == 0) {
                source.sendError(Text.literal("No regions found in file."));
                return 0;
            }
            for (ProtectedRegion r : regions) {
                ModConfig.get().protectedRegions.add(r);
            }
            ModConfig.save();
            source.sendFeedback(() -> Text.literal("Imported " + regions.length + " protected region(s) from file."), false);
            String actor = source.getName();
            AuditManager.log(actor, "region.import_file", "path=" + pathStr + " count=" + regions.length);
            RecallTotemMod.LOGGER.info("Imported " + regions.length + " protected regions from file: " + pathStr);
            return regions.length;
        } catch (JsonSyntaxException e) {
            source.sendError(Text.literal("Invalid JSON in file: " + e.getMessage()));
            return 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to import regions from file."));
            RecallTotemMod.LOGGER.warn("Region import-file failed", e);
            return 0;
        }
    }
}

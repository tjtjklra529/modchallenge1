package com.yichen.recalltotem.command;

import com.google.gson.Gson;
import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.StringArgumentType;
import static net.minecraft.server.command.CommandManager.argument;

public class RegionExportCommand {
    private static final Gson GSON = new Gson();

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("export")
                            .then(argument("path", StringArgumentType.greedyString())
                                .executes(ctx -> exportRegions(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )
            );
        });
    }

    private static int exportRegions(ServerCommandSource source, String pathStr) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to export regions."));
            return 0;
        }
        try {
            ProtectedRegion[] arr = ModConfig.get().protectedRegions.toArray(new ProtectedRegion[0]);
            String json = GSON.toJson(arr);
            Path path = Path.of(pathStr);
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            source.sendFeedback(() -> Text.literal("Exported " + arr.length + " region(s) to " + path.toString()), false);
            String actor = source.getName().getString();
            AuditManager.log(actor, "region.export", "path=" + path.toString() + " count=" + arr.length);
            RecallTotemMod.LOGGER.info("Exported protected regions to " + path.toString());
            return arr.length;
        } catch (IOException e) {
            source.sendError(Text.literal("Failed to write file: " + e.getMessage()));
            RecallTotemMod.LOGGER.warn("Region export failed", e);
            return 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Unexpected error during export."));
            RecallTotemMod.LOGGER.warn("Region export unexpected error", e);
            return 0;
        }
    }
}

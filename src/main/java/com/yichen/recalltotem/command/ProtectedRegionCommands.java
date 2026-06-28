package com.yichen.recalltotem.command;

import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class ProtectedRegionCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("add")
                            .then(argument("x", IntegerArgumentType.integer())
                                .then(argument("z", IntegerArgumentType.integer())
                                    .then(argument("radius", IntegerArgumentType.integer(1))
                                        .then(argument("dimension", StringArgumentType.word())
                                            .executes(ctx -> addRegion(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                IntegerArgumentType.getInteger(ctx, "z"),
                                                IntegerArgumentType.getInteger(ctx, "radius"),
                                                StringArgumentType.getString(ctx, "dimension")))
                                        )
                                        .executes(ctx -> addRegion(ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "x"),
                                            IntegerArgumentType.getInteger(ctx, "z"),
                                            IntegerArgumentType.getInteger(ctx, "radius"),
                                            "minecraft:overworld"))
                                    )
                                )
                            )
                        )
                        .then(literal("remove")
                            .then(argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> removeRegion(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))
                            )
                        )
                        .then(literal("list")
                            .executes(ctx -> listRegions(ctx.getSource()))
                        )
                    )
            );
        });
    }

    private static int addRegion(ServerCommandSource source, int x, int z, int radius, String dimension) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to add protected regions."));
            return 0;
        }
        ProtectedRegion r = new ProtectedRegion(x, z, radius, dimension);
        ModConfig.get().protectedRegions.add(r);
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Added protected region at (" + x + "," + z + ") radius " + radius + " dim " + dimension), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.add", "center=" + x + "," + z + " radius=" + radius + " dim=" + dimension);
        RecallTotemMod.LOGGER.info("Added protected region: " + x + "," + z + " r=" + radius + " dim=" + dimension);
        return 1;
    }

    private static int removeRegion(ServerCommandSource source, int index) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to remove protected regions."));
            return 0;
        }
        if (index < 0 || index >= ModConfig.get().protectedRegions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion removed = ModConfig.get().protectedRegions.remove(index);
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Removed protected region #" + index), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.remove", "index=" + index + " center=" + removed.centerX + "," + removed.centerZ);
        return 1;
    }

    private static int listRegions(ServerCommandSource source) {
        var regions = ModConfig.get().protectedRegions;
        if (regions == null || regions.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No protected regions configured."), false);
            return 1;
        }
        for (int i = 0; i < regions.size(); i++) {
            ProtectedRegion r = regions.get(i);
            int idx = i;
            source.sendFeedback(() -> Text.literal("#" + idx + ": center=(" + r.centerX + "," + r.centerZ + ") radius=" + r.radiusBlocks + " dim=" + r.dimension + (r.adminNote != null && !r.adminNote.isEmpty() ? " note=" + r.adminNote : "")), false);
        }
        String actor = source.getName();
        AuditManager.log(actor, "region.list", "count=" + regions.size());
        return regions.size();
    }
}

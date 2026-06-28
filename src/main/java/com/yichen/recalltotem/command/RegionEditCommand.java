package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionEditCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("edit")
                            .then(argument("index", IntegerArgumentType.integer(0))
                                .then(argument("field", StringArgumentType.word())
                                    .then(argument("value", StringArgumentType.greedyString())
                                        .executes(ctx -> editRegion(ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "index"),
                                            StringArgumentType.getString(ctx, "field"),
                                            StringArgumentType.getString(ctx, "value")))
                                    )
                                )
                            )
                        )
                    )
            );
        });
    }

    private static int editRegion(ServerCommandSource source, int index, String field, String value) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to edit regions."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (regions == null || index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        try {
            String oldVal = "";
            switch (field.toLowerCase()) {
                case "center_x": oldVal = String.valueOf(r.centerX); r.centerX = Integer.parseInt(value); break;
                case "center_z": oldVal = String.valueOf(r.centerZ); r.centerZ = Integer.parseInt(value); break;
                case "radius_blocks": oldVal = String.valueOf(r.radiusBlocks); r.radiusBlocks = Integer.parseInt(value); break;
                case "dimension": oldVal = r.dimension; r.dimension = value; break;
                case "deny_message": oldVal = r.denyMessage; r.denyMessage = value; break;
                case "admin_note": oldVal = r.adminNote; r.adminNote = value; break;
                default:
                    source.sendError(Text.literal("Unknown field: " + field));
                    return 0;
            }
            ModConfig.save();
            source.sendFeedback(() -> Text.literal("Region #" + index + " updated (" + field + ")."), false);
            String actor = source.getName().getString();
            AuditManager.log(actor, "region.edit", "index=" + index + " field=" + field + " from=\"" + oldVal + "\" to=\"" + value + "\"");
            RecallTotemMod.LOGGER.info("Region #" + index + " edited: " + field + "=" + value);
            return 1;
        } catch (NumberFormatException e) {
            source.sendError(Text.literal("Invalid numeric value for field " + field));
            return 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to edit region."));
            RecallTotemMod.LOGGER.warn("Failed to edit region", e);
            return 0;
        }
    }
}

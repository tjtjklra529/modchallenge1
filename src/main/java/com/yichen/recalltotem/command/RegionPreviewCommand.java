package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import com.yichen.recalltotem.viz.RegionVisualizer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import static net.minecraft.server.command.CommandManager.argument;

public class RegionPreviewCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("preview")
                            .then(argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> previewRegion(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))
                            )
                        )
                    )
            );
        });
    }

    private static int previewRegion(ServerCommandSource source, int index) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to preview regions."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (regions == null || index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendError(Text.literal("Must be run by a player to preview in-world."));
            return 0;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        RegionVisualizer.previewRegion(player, r);
        source.sendFeedback(() -> Text.literal("Previewing region #" + index), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.preview", "index=" + index);
        return 1;
    }
}

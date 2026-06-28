package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class RegionPreviewToggleCommand {
    private static boolean globalPreviewEnabled = false;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("preview-toggle")
                            .executes(ctx -> togglePreview(ctx.getSource()))
                        )
                        .then(literal("preview-status")
                            .executes(ctx -> status(ctx.getSource()))
                        )
                    )
            );
        });
    }

    private static int togglePreview(ServerCommandSource source) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to toggle preview."));
            return 0;
        }
        globalPreviewEnabled = !globalPreviewEnabled;
        source.sendFeedback(() -> Text.literal("Global preview " + (globalPreviewEnabled ? "enabled" : "disabled")), false);
        String actor = source.getName().getString();
        AuditManager.log(actor, "region.preview.toggle", "enabled=" + globalPreviewEnabled);
        RecallTotemMod.LOGGER.info("Global preview toggled: " + globalPreviewEnabled);
        return 1;
    }

    private static int status(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("Global preview is " + (globalPreviewEnabled ? "enabled" : "disabled")), false);
        String actor = source.getName().getString();
        AuditManager.log(actor, "region.preview.status", "queried enabled=" + globalPreviewEnabled);
        return 1;
    }
}

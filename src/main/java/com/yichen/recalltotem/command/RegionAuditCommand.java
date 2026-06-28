package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import static net.minecraft.server.command.CommandManager.argument;

public class RegionAuditCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("audit")
                            .executes(ctx -> showAudit(ctx.getSource(), 25))
                            .then(argument("lines", IntegerArgumentType.integer(1, 200))
                                .executes(ctx -> showAudit(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "lines")))
                            )
                        )
                    )
            );
        });
    }

    private static int showAudit(ServerCommandSource source, int lines) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to view the audit log."));
            return 0;
        }
        var entries = AuditManager.tail(lines);
        if (entries.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Audit log is empty."), false);
            return 1;
        }
        for (String e : entries) {
            source.sendFeedback(() -> Text.literal(e), false);
        }
        RecallTotemMod.LOGGER.info("Displayed last " + entries.size() + " audit entries to " + source.getName());
        return entries.size();
    }
}

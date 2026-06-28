package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionAuditFilterCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("audit-filter")
                            .then(argument("type", StringArgumentType.word())
                                .then(argument("query", StringArgumentType.greedyString())
                                    .executes(ctx -> filter(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type"),
                                        StringArgumentType.getString(ctx, "query")))
                                )
                            )
                        )
                    )
            );
        });
    }

    private static int filter(ServerCommandSource source, String type, String query) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to filter the audit log."));
            return 0;
        }
        var lines = AuditManager.tail(1000);
        int shown = 0;
        String q = query.toLowerCase();
        for (String line : lines) {
            boolean match = switch (type.toLowerCase()) {
                case "actor" -> line.toLowerCase().contains("| " + q + " |");
                case "action" -> line.toLowerCase().contains("| " + q + " ");
                case "text" -> line.toLowerCase().contains(q);
                default -> false;
            };
            if (match) {
                source.sendFeedback(() -> Text.literal(line), false);
                shown++;
                if (shown >= 200) break;
            }
        }
        int finalShown = shown;
        source.sendFeedback(() -> Text.literal("Shown " + finalShown + " matching audit entries."), false);
        return shown;
    }
}

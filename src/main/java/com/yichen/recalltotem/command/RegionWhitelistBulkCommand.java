package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.tools.WhitelistBulkTool;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionWhitelistBulkCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("whitelist")
                            .then(literal("bulk-apply")
                                .then(argument("path", StringArgumentType.greedyString())
                                    .executes(ctx -> dryRun(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                                )
                            )
                            .then(literal("bulk-apply-apply")
                                .then(argument("path", StringArgumentType.greedyString())
                                    .executes(ctx -> apply(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                                )
                            )
                        )
                    )
            );
        });
    }

    private static int dryRun(ServerCommandSource source, String pathStr) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to bulk-apply whitelists."));
            return 0;
        }
        try {
            List<String> errors = WhitelistBulkTool.validate(Path.of(pathStr));
            if (errors.isEmpty()) {
                source.sendFeedback(() -> Text.literal("Dry-run OK: no errors found."), false);
            } else {
                for (String e : errors) {
                    source.sendError(Text.literal("Error: " + e));
                }
            }
            String actor = source.getName().getString();
            AuditManager.log(actor, "region.whitelist.bulk_dryrun", "path=" + pathStr + " errors=" + errors.size());
            return errors.isEmpty() ? 1 : 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to validate file: " + e.getMessage()));
            RecallTotemMod.LOGGER.warn("Whitelist bulk dry-run failed", e);
            return 0;
        }
    }

    private static int apply(ServerCommandSource source, String pathStr) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to bulk-apply whitelists."));
            return 0;
        }
        try {
            int count = WhitelistBulkTool.apply(Path.of(pathStr));
            source.sendFeedback(() -> Text.literal("Applied whitelist bulk import: " + count + " region(s)."), false);
            String actor = source.getName().getString();
            AuditManager.log(actor, "region.whitelist.bulk_apply", "path=" + pathStr + " count=" + count);
            return count;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to apply file: " + e.getMessage()));
            RecallTotemMod.LOGGER.warn("Whitelist bulk apply failed", e);
            return 0;
        }
    }
}

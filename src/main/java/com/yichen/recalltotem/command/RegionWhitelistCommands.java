package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import com.yichen.recalltotem.web.WebhookNotifier;
import com.yichen.recalltotem.RecallTotemMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionWhitelistCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("whitelist")
                            .then(literal("add")
                                .then(argument("index", IntegerArgumentType.integer(0))
                                    .then(argument("player", StringArgumentType.word())
                                        .executes(ctx -> add(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), StringArgumentType.getString(ctx, "player")))
                                    )
                                )
                            )
                            .then(literal("remove")
                                .then(argument("index", IntegerArgumentType.integer(0))
                                    .then(argument("player", StringArgumentType.word())
                                        .executes(ctx -> remove(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), StringArgumentType.getString(ctx, "player")))
                                    )
                                )
                            )
                            .then(literal("list")
                                .then(argument("index", IntegerArgumentType.integer(0))
                                    .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))
                                )
                            )
                        )
                    )
            );
        });
    }

    private static int add(ServerCommandSource source, int index, String playerOrUuid) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to modify whitelists."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        String uuid = resolveToUuid(source.getServer(), playerOrUuid);
        if (uuid == null) {
            source.sendError(Text.literal("Could not resolve player or UUID: " + playerOrUuid));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        if (!r.whitelist.contains(uuid)) r.whitelist.add(uuid);
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Added to whitelist for region #" + index + ": " + uuid), false);
        String actor = source.getName().getString();
        AuditManager.log(actor, "region.whitelist.add", "index=" + index + " uuid=" + uuid);
        WebhookNotifier.notify("region.whitelist.add", actor, java.util.Map.of("index", String.valueOf(index), "uuid", uuid));
        return 1;
    }

    private static int remove(ServerCommandSource source, int index, String playerOrUuid) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to modify whitelists."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        String uuid = resolveToUuid(source.getServer(), playerOrUuid);
        if (uuid == null) {
            source.sendError(Text.literal("Could not resolve player or UUID: " + playerOrUuid));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        if (r.whitelist.remove(uuid)) {
            ModConfig.save();
            source.sendFeedback(() -> Text.literal("Removed from whitelist for region #" + index + ": " + uuid), false);
            String actor = source.getName().getString();
            AuditManager.log(actor, "region.whitelist.remove", "index=" + index + " uuid=" + uuid);
            WebhookNotifier.notify("region.whitelist.remove", actor, java.util.Map.of("index", String.valueOf(index), "uuid", uuid));
            return 1;
        } else {
            source.sendError(Text.literal("UUID not found in whitelist: " + uuid));
            return 0;
        }
    }

    private static int list(ServerCommandSource source, int index) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to list whitelists."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        if (r.whitelist == null || r.whitelist.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Region #" + index + " whitelist is empty."), false);
            return 1;
        }
        for (String u : r.whitelist) {
            String display = u;
            var player = source.getServer().getPlayerManager().getPlayer(u);
            if (player != null) display = player.getEntityName();
            source.sendFeedback(() -> Text.literal(display + " (" + u + ")"), false);
        }
        return r.whitelist.size();
    }

    private static String resolveToUuid(MinecraftServer server, String playerOrUuid) {
        try {
            java.util.UUID.fromString(playerOrUuid);
            return playerOrUuid;
        } catch (Exception ignored) {}
        var pm = server.getPlayerManager();
        var p = pm.getPlayer(playerOrUuid);
        if (p != null) return p.getUuidAsString();
        return null;
    }
}

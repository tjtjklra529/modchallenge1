package com.yichen.recalltotem.command;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import com.yichen.recalltotem.RecallTotemMod;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionOwnershipCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("claim")
                            .then(argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> claim(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))
                            )
                        )
                        .then(literal("transfer")
                            .then(argument("index", IntegerArgumentType.integer(0))
                                .then(argument("player", StringArgumentType.word())
                                    .executes(ctx -> transfer(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), StringArgumentType.getString(ctx, "player")))
                                )
                            )
                        )
                        .then(literal("set-owner")
                            .then(argument("index", IntegerArgumentType.integer(0))
                                .then(argument("uuid", StringArgumentType.word())
                                    .executes(ctx -> setOwner(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), StringArgumentType.getString(ctx, "uuid")))
                                )
                            )
                        )
                    )
            );
        });
    }

    private static int claim(ServerCommandSource source, int index) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to claim regions."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        if (!(source.getEntity() instanceof ServerPlayerEntity)) {
            source.sendError(Text.literal("Must be run by a player to claim."));
            return 0;
        }
        ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
        r.ownerUuid = player.getUuidAsString();
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("You claimed region #" + index), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.claim", "index=" + index + " owner=" + r.ownerUuid);
        return 1;
    }

    private static int transfer(ServerCommandSource source, int index, String playerName) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to transfer regions."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        PlayerManager pm = server.getPlayerManager();
        var target = pm.getPlayer(playerName);
        if (target == null) {
            source.sendError(Text.literal("Player not found or offline: " + playerName));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        r.ownerUuid = target.getUuidAsString();
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Transferred region #" + index + " to " + playerName), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.transfer", "index=" + index + " to=" + target.getUuidAsString());
        return 1;
    }

    private static int setOwner(ServerCommandSource source, int index, String uuid) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to set owners."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        r.ownerUuid = uuid;
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Set owner of region #" + index + " to " + uuid), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.set_owner", "index=" + index + " owner=" + uuid);
        return 1;
    }
}

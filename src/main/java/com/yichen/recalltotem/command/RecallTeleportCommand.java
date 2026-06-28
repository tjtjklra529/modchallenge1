package com.yichen.recalltotem.command;

import com.yichen.recalltotem.util.AnchorUtil;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class RecallTeleportCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("go")
                        .executes(ctx -> recall(ctx.getSource())))
                    .executes(ctx -> recall(ctx.getSource()))
            );
        });
    }

    private static int recall(ServerCommandSource source) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Only players can use /recall."));
            return 0;
        }

        AnchorUtil.AnchorData data = AnchorUtil.getPlayerAnchor(player.getUuid());
        if (data == null) {
            source.sendError(Text.literal("You have no Recall Anchor set. Right-click a Recall Anchor block to set one."));
            return 0;
        }

        ServerWorld world = AnchorUtil.resolveWorld(source.getServer(), data.dimension);
        if (world == null) {
            source.sendError(Text.literal("Your Recall Anchor's dimension no longer exists."));
            return 0;
        }

        player.teleport(world, data.x, data.y, data.z, data.yaw, data.pitch);
        player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        source.sendFeedback(() -> Text.literal("Recalled to your anchor."), false);
        return 1;
    }
}

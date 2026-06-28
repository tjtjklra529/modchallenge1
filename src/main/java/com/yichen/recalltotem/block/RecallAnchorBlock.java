package com.yichen.recalltotem.block;

import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import com.yichen.recalltotem.util.AnchorUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class RecallAnchorBlock extends Block {

    public RecallAnchorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        if (ModConfig.get().protectSpawnFromAnchors) {
            BlockPos worldSpawn = serverPlayer.getServerWorld().getSpawnPos();
            double dx = pos.getX() - worldSpawn.getX();
            double dz = pos.getZ() - worldSpawn.getZ();
            double distSq = dx * dx + dz * dz;
            int protectedRadius = ModConfig.get().protectedSpawnRadiusBlocks;
            if (distSq <= (double) protectedRadius * protectedRadius) {
                serverPlayer.sendMessage(Text.literal("You cannot set a Recall Anchor this close to world spawn."), false);
                world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.8f);
                return ActionResult.FAIL;
            }
        }

        List<ProtectedRegion> regions = ModConfig.get().protectedRegions;
        if (regions != null) {
            String dim = serverPlayer.getServerWorld().getRegistryKey().getValue().toString();
            for (ProtectedRegion r : regions) {
                if (r == null) continue;
                if (!r.contains(pos.getX(), pos.getZ(), dim)) continue;
                if (!r.isActiveNow()) continue;
                if (r.whitelist != null && r.whitelist.contains(serverPlayer.getUuidAsString())) continue;
                String msg = (r.denyMessage != null && !r.denyMessage.isEmpty())
                    ? r.denyMessage
                    : "You cannot set a Recall Anchor inside a protected region.";
                serverPlayer.sendMessage(Text.literal(msg), false);
                world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.8f);
                return ActionResult.FAIL;
            }
        }

        AnchorUtil.setPlayerAnchor(serverPlayer, pos, serverPlayer.getServerWorld());
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        serverPlayer.sendMessage(Text.literal("Recall Anchor set."), false);

        return ActionResult.CONSUME;
    }
}

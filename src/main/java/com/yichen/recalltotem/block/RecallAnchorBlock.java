package com.yichen.recalltotem.block;

import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import com.yichen.recalltotem.item.ModItems;
import com.yichen.recalltotem.util.AnchorStore;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class RecallAnchorBlock extends Block {

    public static final BooleanProperty LOADED = BooleanProperty.of("loaded");

    public RecallAnchorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(LOADED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LOADED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);

        if (held.isOf(Items.GOLD_INGOT)) {
            if (state.get(LOADED)) {
                serverPlayer.sendMessage(Text.literal("This Recall Anchor is already loaded."), true);
                return ActionResult.SUCCESS;
            }

            // Spawn/region protection checks
            if (ModConfig.get().protectSpawnFromAnchors) {
                BlockPos worldSpawn = serverPlayer.getServerWorld().getSpawnPos();
                double dx = pos.getX() - worldSpawn.getX();
                double dz = pos.getZ() - worldSpawn.getZ();
                if (dx * dx + dz * dz <= (double) ModConfig.get().protectedSpawnRadiusBlocks * ModConfig.get().protectedSpawnRadiusBlocks) {
                    serverPlayer.sendMessage(Text.literal("You cannot load a Recall Anchor this close to world spawn."), false);
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.8f);
                    return ActionResult.FAIL;
                }
            }

            String dim = serverPlayer.getServerWorld().getRegistryKey().getValue().toString();
            List<ProtectedRegion> regions = ModConfig.get().protectedRegions;
            if (regions != null) {
                for (ProtectedRegion r : regions) {
                    if (r == null || !r.contains(pos.getX(), pos.getZ(), dim) || !r.isActiveNow()) continue;
                    if (r.whitelist != null && r.whitelist.contains(serverPlayer.getUuidAsString())) continue;
                    String msg = (r.denyMessage != null && !r.denyMessage.isEmpty())
                        ? r.denyMessage : "You cannot load a Recall Anchor inside a protected region.";
                    serverPlayer.sendMessage(Text.literal(msg), false);
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.8f);
                    return ActionResult.FAIL;
                }
            }

            // Name comes from the ingot if renamed via anvil, else default
            String name = held.hasCustomName() ? held.getName().getString() : "Recall Anchor";

            // Consume ingot
            if (!player.getAbilities().creativeMode) {
                held.decrement(1);
            }

            // Save to anchor store
            AnchorStore.addAnchor(serverPlayer.getUuid(), name, pos.getX(), pos.getY(), pos.getZ(), dim);

            // Switch to loaded texture
            world.setBlockState(pos, state.with(LOADED, true));

            // Give Recall Button only if player doesn't already have one
            if (!player.getInventory().containsAny(stack -> stack.isOf(ModItems.RECALL_BUTTON))) {
                ItemStack button = new ItemStack(ModItems.RECALL_BUTTON);
                if (!player.getInventory().insertStack(button)) {
                    player.dropItem(button, false);
                }
            }

            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            serverPlayer.sendMessage(Text.literal("Recall Anchor loaded as \"" + name + "\". You received a Recall Button!"), false);
            return ActionResult.SUCCESS;
        }

        if (state.get(LOADED)) {
            serverPlayer.sendMessage(Text.literal("This anchor is loaded. Use your Recall Button to teleport here."), true);
        } else {
            serverPlayer.sendMessage(Text.literal("Right-click with a gold ingot to load this anchor."), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            String dim = ((ServerWorld) world).getRegistryKey().getValue().toString();
            AnchorStore.removeAnchorAt(pos.getX(), pos.getY(), pos.getZ(), dim);
        }
        super.onBreak(world, pos, state, player);
    }
}

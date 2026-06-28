package com.yichen.recalltotem.item;

import com.yichen.recalltotem.screen.RecallScreenFactory;
import com.yichen.recalltotem.util.AnchorStore;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class RecallButtonItem extends Item {

    public RecallButtonItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) return TypedActionResult.success(user.getStackInHand(hand));

        ServerPlayerEntity player = (ServerPlayerEntity) user;
        List<AnchorStore.AnchorEntry> anchors = AnchorStore.getAnchors(player.getUuid());
        player.openHandledScreen(new RecallScreenFactory(anchors));

        return TypedActionResult.success(user.getStackInHand(hand));
    }
}

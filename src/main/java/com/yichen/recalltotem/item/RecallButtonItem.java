package com.yichen.recalltotem.item;

import com.yichen.recalltotem.ModScreenHandlers;
import com.yichen.recalltotem.screen.RecallSelectScreenHandler;
import com.yichen.recalltotem.util.AnchorStore;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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

        player.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayerEntity p, PacketByteBuf buf) {
                buf.writeInt(anchors.size());
                for (AnchorStore.AnchorEntry e : anchors) {
                    buf.writeString(e.name);
                    buf.writeInt(e.blockX);
                    buf.writeInt(e.blockY);
                    buf.writeInt(e.blockZ);
                    buf.writeString(e.dimension);
                }
            }

            @Override
            public Text getDisplayName() { return Text.literal("Recall Anchors"); }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new RecallSelectScreenHandler(syncId, inv, anchors);
            }
        });

        return TypedActionResult.success(user.getStackInHand(hand));
    }
}

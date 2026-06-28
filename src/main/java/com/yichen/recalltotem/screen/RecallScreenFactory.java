package com.yichen.recalltotem.screen;

import com.yichen.recalltotem.util.AnchorStore;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class RecallScreenFactory implements ExtendedScreenHandlerFactory {

    private final List<AnchorStore.AnchorEntry> anchors;

    public RecallScreenFactory(List<AnchorStore.AnchorEntry> anchors) {
        this.anchors = anchors;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
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
    public Text getDisplayName() {
        return Text.literal("Recall Anchors");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new RecallSelectScreenHandler(syncId, inv, anchors);
    }
}

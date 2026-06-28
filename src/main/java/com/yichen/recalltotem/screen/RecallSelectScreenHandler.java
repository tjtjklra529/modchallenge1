package com.yichen.recalltotem.screen;

import com.yichen.recalltotem.ModScreenHandlers;
import com.yichen.recalltotem.util.AnchorStore;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;

import java.util.ArrayList;
import java.util.List;

public class RecallSelectScreenHandler extends ScreenHandler {

    private final List<AnchorStore.AnchorEntry> anchors;

    // Client-side: reads from packet
    public RecallSelectScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        super(ModScreenHandlers.RECALL_SELECT, syncId);
        int count = buf.readInt();
        anchors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AnchorStore.AnchorEntry e = new AnchorStore.AnchorEntry();
            e.name = buf.readString();
            e.blockX = buf.readInt();
            e.blockY = buf.readInt();
            e.blockZ = buf.readInt();
            e.dimension = buf.readString();
            anchors.add(e);
        }
    }

    // Server-side
    public RecallSelectScreenHandler(int syncId, PlayerInventory inv, List<AnchorStore.AnchorEntry> anchors) {
        super(ModScreenHandlers.RECALL_SELECT, syncId);
        this.anchors = new ArrayList<>(anchors);
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }

    public List<AnchorStore.AnchorEntry> getAnchors() { return anchors; }
}

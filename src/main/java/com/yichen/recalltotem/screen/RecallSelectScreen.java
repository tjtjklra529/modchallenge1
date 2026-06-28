package com.yichen.recalltotem.screen;

import com.yichen.recalltotem.util.AnchorStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

@Environment(EnvType.CLIENT)
public class RecallSelectScreen extends HandledScreen<RecallSelectScreenHandler> {

    private static final int BUTTON_HEIGHT = 22;
    private static final int PADDING = 12;

    public RecallSelectScreen(RecallSelectScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        List<AnchorStore.AnchorEntry> anchors = handler.getAnchors();
        this.backgroundWidth = 200;
        this.backgroundHeight = PADDING * 2 + 14 + Math.max(1, anchors.size()) * (BUTTON_HEIGHT + 4);
    }

    @Override
    protected void init() {
        super.init();
        List<AnchorStore.AnchorEntry> anchors = handler.getAnchors();
        if (anchors.isEmpty()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("No anchors loaded yet"), b -> close())
                .dimensions(x + 22, y + PADDING + 14, 156, BUTTON_HEIGHT).build());
        } else {
            for (int i = 0; i < anchors.size(); i++) {
                final int idx = i;
                AnchorStore.AnchorEntry entry = anchors.get(i);
                String dim = entry.dimension.contains(":") ? entry.dimension.split(":")[1] : entry.dimension;
                String label = entry.name + "  §7[" + dim + "]";
                addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(idx);
                    ClientPlayNetworking.send(new Identifier("recalltotem", "recall_select"), buf);
                    close();
                }).dimensions(x + 22, y + PADDING + 14 + idx * (BUTTON_HEIGHT + 4), 156, BUTTON_HEIGHT).build());
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // dark panel
        context.fill(x - 1, y - 1, x + backgroundWidth + 1, y + backgroundHeight + 1, 0xFF000000);
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF1A1A2E);
        // title bar
        context.fill(x, y, x + backgroundWidth, y + PADDING + 10, 0xFF16213E);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, x + backgroundWidth / 2, y + 4, 0x88CCFF);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // suppress default "Inventory" label
    }
}

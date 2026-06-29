package com.yichen.recalltotem.screen;

import com.yichen.recalltotem.util.AnchorStore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@Environment(EnvType.CLIENT)
public class RecallSelectScreen extends HandledScreen<RecallSelectScreenHandler> {

    private static final int MAX_VISIBLE = 7;
    private static final int ENTRY_HEIGHT = 22;
    private static final int ENTRY_STRIDE = ENTRY_HEIGHT + 3;
    private static final int PADDING = 10;
    private static final int TITLE_HEIGHT = 16;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int LIST_WIDTH = 200;

    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private List<AnchorStore.AnchorEntry> anchors;

    public RecallSelectScreen(RecallSelectScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.anchors = handler.getAnchors();
        int visible = Math.max(1, Math.min(MAX_VISIBLE, anchors.isEmpty() ? 1 : anchors.size()));
        this.backgroundWidth = LIST_WIDTH + SCROLLBAR_WIDTH + 4;
        this.backgroundHeight = PADDING * 2 + TITLE_HEIGHT + visible * ENTRY_STRIDE;
    }

    @Override
    protected void init() {
        super.init();
        // No child buttons — we draw and handle clicks manually
    }

    private int listTop() { return y + PADDING + TITLE_HEIGHT; }
    private int listBottom() { return y + backgroundHeight - PADDING; }
    private int listHeight() { return listBottom() - listTop(); }
    private int maxScroll() { return Math.max(0, anchors.size() - MAX_VISIBLE); }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Panel
        context.fill(x - 1, y - 1, x + backgroundWidth + 1, y + backgroundHeight + 1, 0xFF000000);
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF1A1A2E);
        // Title bar
        context.fill(x, y, x + backgroundWidth, y + PADDING + TITLE_HEIGHT - 2, 0xFF16213E);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, x + backgroundWidth / 2, y + PADDING / 2 + 2, 0x88CCFF);

        if (anchors.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("No anchors loaded yet"), x + backgroundWidth / 2, listTop() + 4, 0xAAAAAA);
            return;
        }

        // Clip to list area
        context.enableScissor(x, listTop(), x + LIST_WIDTH + 2, listBottom());

        int top = listTop();
        for (int i = scrollOffset; i < Math.min(anchors.size(), scrollOffset + MAX_VISIBLE + 1); i++) {
            AnchorStore.AnchorEntry entry = anchors.get(i);
            int ey = top + (i - scrollOffset) * ENTRY_STRIDE;
            if (ey + ENTRY_HEIGHT < listTop() || ey > listBottom()) continue;

            boolean hovered = mouseX >= x + 4 && mouseX < x + LIST_WIDTH - 2
                && mouseY >= ey && mouseY < ey + ENTRY_HEIGHT;

            int bg = hovered ? 0xFF2A3A5E : 0xFF1E2E4A;
            context.fill(x + 4, ey, x + LIST_WIDTH - 2, ey + ENTRY_HEIGHT, bg);

            String dim = entry.dimension.contains(":") ? entry.dimension.split(":")[1] : entry.dimension;
            String label = entry.name + " §7[" + dim + "]";
            context.drawTextWithShadow(textRenderer, Text.literal(label),
                x + 8, ey + (ENTRY_HEIGHT - 8) / 2, 0xFFFFFF);
        }

        context.disableScissor();

        // Scrollbar
        if (maxScroll() > 0) {
            int sbX = x + backgroundWidth - SCROLLBAR_WIDTH - 2;
            int sbTrackTop = listTop();
            int sbTrackH = listHeight();
            context.fill(sbX, sbTrackTop, sbX + SCROLLBAR_WIDTH, sbTrackTop + sbTrackH, 0xFF0D0D1A);

            int thumbH = Math.max(16, sbTrackH * MAX_VISIBLE / anchors.size());
            int thumbY = sbTrackTop + (sbTrackH - thumbH) * scrollOffset / maxScroll();
            context.fill(sbX, thumbY, sbX + SCROLLBAR_WIDTH, thumbY + thumbH, 0xFF4466AA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scrollOffset = MathHelper.clamp((int) (scrollOffset - amount), 0, maxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll() > 0) {
            int sbX = x + backgroundWidth - SCROLLBAR_WIDTH - 2;
            if (mouseX >= sbX && mouseX < sbX + SCROLLBAR_WIDTH
                    && mouseY >= listTop() && mouseY < listBottom()) {
                draggingScrollbar = true;
                return true;
            }
        }

        if (button == 0 && mouseX >= x + 4 && mouseX < x + LIST_WIDTH - 2
                && mouseY >= listTop() && mouseY < listBottom()) {
            int rel = (int) mouseY - listTop();
            int idx = scrollOffset + rel / ENTRY_STRIDE;
            if (idx >= 0 && idx < anchors.size()) {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(idx);
                ClientPlayNetworking.send(new Identifier("recalltotem", "recall_select"), buf);
                close();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScrollbar && maxScroll() > 0) {
            int track = listHeight();
            double fraction = (mouseY - listTop()) / track;
            scrollOffset = MathHelper.clamp((int) Math.round(fraction * maxScroll()), 0, maxScroll());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // suppress default labels
    }
}

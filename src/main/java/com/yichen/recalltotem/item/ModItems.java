package com.yichen.recalltotem.item;

import com.yichen.recalltotem.block.ModBlocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static Item RECALL_ANCHOR_ITEM;
    public static Item RECALL_BUTTON;

    public static void registerModItems() {
        RECALL_ANCHOR_ITEM = new BlockItem(ModBlocks.RECALL_ANCHOR, new Item.Settings());
        Registry.register(Registries.ITEM, new Identifier("recalltotem", "recall_anchor"), RECALL_ANCHOR_ITEM);

        RECALL_BUTTON = new RecallButtonItem(new Item.Settings().maxCount(1));
        Registry.register(Registries.ITEM, new Identifier("recalltotem", "recall_button"), RECALL_BUTTON);
    }
}

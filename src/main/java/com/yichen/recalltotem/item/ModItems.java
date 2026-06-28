package com.yichen.recalltotem.item;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import com.yichen.recalltotem.block.ModBlocks;

public final class ModItems {
    public static Item RECALL_ANCHOR_ITEM;

    public static void registerModItems() {
        RECALL_ANCHOR_ITEM = new BlockItem(ModBlocks.RECALL_ANCHOR, new Item.Settings());
        Registry.register(Registry.ITEM, new Identifier("recalltotem", "recall_anchor"), RECALL_ANCHOR_ITEM);
    }
}

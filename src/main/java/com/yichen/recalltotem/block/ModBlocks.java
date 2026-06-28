package com.yichen.recalltotem.block;

import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class ModBlocks {
    public static Block RECALL_ANCHOR;

    public static void registerModBlocks() {
        RECALL_ANCHOR = new RecallAnchorBlock(Block.Settings.of(Material.METAL).strength(3.5f));
        Registry.register(Registry.BLOCK, new Identifier("recalltotem", "recall_anchor"), RECALL_ANCHOR);
    }
}

package com.yichen.recalltotem.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {
    public static Block RECALL_ANCHOR;

    public static void registerModBlocks() {
        RECALL_ANCHOR = new RecallAnchorBlock(
            AbstractBlock.Settings.create()
                .mapColor(MapColor.IRON_GRAY)
                .strength(1.5f)
                .sounds(BlockSoundGroup.METAL)
                .luminance(state -> state.get(RecallAnchorBlock.LOADED) ? 9 : 6)
        );
        Registry.register(Registries.BLOCK, new Identifier("recalltotem", "recall_anchor"), RECALL_ANCHOR);
    }
}

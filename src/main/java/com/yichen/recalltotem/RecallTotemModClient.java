package com.yichen.recalltotem;

import com.yichen.recalltotem.screen.RecallSelectScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

@Environment(EnvType.CLIENT)
public class RecallTotemModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.RECALL_SELECT, RecallSelectScreen::new);
    }
}

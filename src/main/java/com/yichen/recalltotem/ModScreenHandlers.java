package com.yichen.recalltotem;

import com.yichen.recalltotem.screen.RecallSelectScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static ScreenHandlerType<RecallSelectScreenHandler> RECALL_SELECT;

    public static void register() {
        RECALL_SELECT = Registry.register(
            Registries.SCREEN_HANDLER,
            new Identifier("recalltotem", "recall_select"),
            new ExtendedScreenHandlerType<>((syncId, inv, buf) -> new RecallSelectScreenHandler(syncId, inv, buf))
        );
    }
}

package com.yichen.recalltotem;

import com.yichen.recalltotem.book.BookManager;
import com.yichen.recalltotem.block.ModBlocks;
import com.yichen.recalltotem.command.RecallCommands;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.item.ModItems;
import com.yichen.recalltotem.roles.LuckPermsRoleAdapter;
import com.yichen.recalltotem.roles.RoleProviders;
import com.yichen.recalltotem.web.WebExportHandler;
import com.yichen.recalltotem.web.WebhookResendHandler;
import com.yichen.recalltotem.web.WebhookRetryScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

public class RecallTotemMod implements ModInitializer {
    public static final String MOD_ID = "recalltotem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Recall Totem mod");
        ModConfig.loadOrCreateDefault();
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        RecallCommands.register();

        try {
            Files.createDirectories(ModConfig.getConfigDir().resolve("logs"));
            java.nio.file.Path audit = ModConfig.getConfigDir().resolve("logs").resolve("recalltotem-region-audit.log");
            if (java.nio.file.Files.notExists(audit)) java.nio.file.Files.createFile(audit);
        } catch (Exception ignored) {}

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                if (handler.getPlayer() instanceof ServerPlayerEntity) {
                    BookManager.giveHelpBookIfNeeded((ServerPlayerEntity) handler.getPlayer());
                }
            } catch (Exception e) {
                LOGGER.warn("Error giving help book on join", e);
            }
        });

        try {
            var adapter = LuckPermsRoleAdapter.tryCreate();
            if (adapter != null) {
                RoleProviders.register(adapter);
                LOGGER.info("Registered LuckPermsRoleAdapter for role checks");
            }
        } catch (Exception e) {
            LOGGER.info("LuckPerms adapter not registered: " + e.getMessage());
        }

        WebExportHandler.start();
        WebhookResendHandler.start();
        WebhookRetryScheduler.start();

        LOGGER.info("Recall Totem mod initialized with config: " + ModConfig.get().toString());
    }
}

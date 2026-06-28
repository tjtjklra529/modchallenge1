package com.yichen.recalltotem;

import com.yichen.recalltotem.book.BookManager;
import com.yichen.recalltotem.block.ModBlocks;
import com.yichen.recalltotem.command.RecallCommands;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.item.ModItems;
import com.yichen.recalltotem.roles.LuckPermsRoleAdapter;
import com.yichen.recalltotem.roles.RoleProviders;
import com.yichen.recalltotem.util.AnchorStore;
import com.yichen.recalltotem.web.WebExportHandler;
import com.yichen.recalltotem.web.WebhookResendHandler;
import com.yichen.recalltotem.web.WebhookRetryScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.List;

public class RecallTotemMod implements ModInitializer {
    public static final String MOD_ID = "recalltotem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Recall Totem mod");
        ModConfig.loadOrCreateDefault();
        ModScreenHandlers.register();
        ModBlocks.registerModBlocks();
        ModItems.registerModItems();
        RecallCommands.register();

        ServerPlayNetworking.registerGlobalReceiver(
            new Identifier("recalltotem", "recall_select"),
            (server, player, handler, buf, responseSender) -> {
                int index = buf.readInt();
                server.execute(() -> {
                    List<AnchorStore.AnchorEntry> anchors = AnchorStore.getAnchors(player.getUuid());
                    if (index < 0 || index >= anchors.size()) return;
                    AnchorStore.AnchorEntry entry = anchors.get(index);
                    RegistryKey<net.minecraft.world.World> key = RegistryKey.of(RegistryKeys.WORLD, new Identifier(entry.dimension));
                    ServerWorld world = server.getWorld(key);
                    if (world == null) {
                        player.sendMessage(Text.literal("That anchor's dimension no longer exists."), false);
                        return;
                    }
                    if (net.minecraft.world.World.END.equals(key)) {
                        ServerWorld endWorld = server.getWorld(net.minecraft.world.World.END);
                        if (endWorld != null && endWorld.getEnderDragonFight() != null && !endWorld.getEnderDragonFight().hasPreviouslyKilled()) {
                            player.sendMessage(Text.literal("You must defeat the Ender Dragon before recalling to The End."), false);
                            return;
                        }
                    }
                    player.teleport(world, entry.blockX + 0.5, entry.blockY + 1.0, entry.blockZ + 0.5, player.getYaw(), player.getPitch());
                    player.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("Recalled to \"" + entry.name + "\"."), false);
                });
            }
        );

        try {
            Files.createDirectories(ModConfig.getConfigDir().resolve("logs"));
            java.nio.file.Path audit = ModConfig.getConfigDir().resolve("logs").resolve("recalltotem-region-audit.log");
            if (java.nio.file.Files.notExists(audit)) java.nio.file.Files.createFile(audit);
        } catch (Exception ignored) {}

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                if (handler.getPlayer() instanceof ServerPlayerEntity) {
                    ServerPlayerEntity sp = (ServerPlayerEntity) handler.getPlayer();
                    BookManager.giveHelpBookIfNeeded(sp);
                    server.execute(() -> {
                        var recipes = server.getRecipeManager().values().stream()
                            .filter(r -> r.getId().getNamespace().equals("recalltotem"))
                            .toList();
                        sp.unlockRecipes(recipes);
                    });
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

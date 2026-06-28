package com.yichen.recalltotem.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ModConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AnchorUtil {
    private AnchorUtil() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class AnchorData {
        public double x, y, z;
        public float yaw, pitch;
        public String dimension;
    }

    private static Path dataFile() {
        return ModConfig.getConfigDir().resolve("anchors.json");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, AnchorData> loadAll() {
        Path file = dataFile();
        if (!Files.exists(file)) return new HashMap<>();
        try {
            String json = Files.readString(file);
            Map<String, AnchorData> map = GSON.fromJson(json,
                new com.google.gson.reflect.TypeToken<Map<String, AnchorData>>(){}.getType());
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            RecallTotemMod.LOGGER.warn("Failed to read anchor data", e);
            return new HashMap<>();
        }
    }

    private static void saveAll(Map<String, AnchorData> map) {
        try {
            Files.createDirectories(dataFile().getParent());
            Files.writeString(dataFile(), GSON.toJson(map));
        } catch (IOException e) {
            RecallTotemMod.LOGGER.warn("Failed to save anchor data", e);
        }
    }

    public static void setPlayerAnchor(ServerPlayerEntity player, BlockPos pos, ServerWorld world) {
        Map<String, AnchorData> all = loadAll();
        AnchorData data = new AnchorData();
        data.x = pos.getX() + 0.5;
        data.y = pos.getY() + 1.0;
        data.z = pos.getZ() + 0.5;
        data.yaw = player.getYaw();
        data.pitch = player.getPitch();
        data.dimension = world.getRegistryKey().getValue().toString();
        all.put(player.getUuidAsString(), data);
        saveAll(all);
    }

    public static AnchorData getPlayerAnchor(UUID playerUuid) {
        return loadAll().get(playerUuid.toString());
    }

    public static ServerWorld resolveWorld(net.minecraft.server.MinecraftServer server, String dimensionId) {
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, new Identifier(dimensionId));
        return server.getWorld(key);
    }
}

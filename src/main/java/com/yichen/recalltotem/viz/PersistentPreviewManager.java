package com.yichen.recalltotem.viz;

import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.config.ProtectedRegion;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PersistentPreviewManager {
    private static final Map<Integer, ScheduledFuture<?>> activePreviews = new ConcurrentHashMap<>();
    private static final Map<Integer, ProtectedRegion> previewRegions = new ConcurrentHashMap<>();
    private static int nextId = 1;

    private PersistentPreviewManager() {}

    public static int startPersistentPreview(MinecraftServer server, ServerPlayerEntity requester, ProtectedRegion region, int seconds) {
        int id = nextId++;
        previewRegions.put(id, region);
        RegionVisualizer.previewRegion(requester, region);
        ScheduledFuture<?> future = server.getScheduler().scheduleAtFixedRate(() -> {
            try {
                RegionVisualizer.previewRegion(requester, region);
            } catch (Exception e) {
                RecallTotemMod.LOGGER.warn("Error during persistent preview", e);
            }
        }, 5, 5, TimeUnit.SECONDS);

        ScheduledFuture<?> canceller = server.getScheduler().schedule(() -> {
            try {
                future.cancel(false);
                activePreviews.remove(id);
                previewRegions.remove(id);
                AuditManager.log(requester.getEntityName(), "region.preview.persist.stop", "id=" + id);
                RecallTotemMod.LOGGER.info("Persistent preview " + id + " ended");
            } catch (Exception e) {
                RecallTotemMod.LOGGER.warn("Failed to cancel persistent preview", e);
            }
        }, seconds, TimeUnit.SECONDS);

        activePreviews.put(id, canceller);
        AuditManager.log(requester.getEntityName(), "region.preview.persist.start", "id=" + id + " seconds=" + seconds);
        RecallTotemMod.LOGGER.info("Started persistent preview " + id + " for " + seconds + "s");
        return id;
    }

    public static void stopPreview(int id, String actor) {
        ScheduledFuture<?> f = activePreviews.remove(id);
        if (f != null) {
            f.cancel(false);
            previewRegions.remove(id);
            AuditManager.log(actor, "region.preview.persist.stop", "id=" + id);
            RecallTotemMod.LOGGER.info("Stopped persistent preview " + id);
        }
    }
}

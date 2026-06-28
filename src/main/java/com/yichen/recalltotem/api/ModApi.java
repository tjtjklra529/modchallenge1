package com.yichen.recalltotem.api;

import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;

import java.util.List;

/**
 * Public API surface for other mods to interact with RecallTotemMod.
 */
public final class ModApi {
    private ModApi() {}

    public static List<ProtectedRegion> getProtectedRegions() {
        return ModConfig.get().protectedRegions;
    }

    public static boolean isLocationProtected(int x, int z, String dimension) {
        for (ProtectedRegion r : ModConfig.get().protectedRegions) {
            if (r != null && r.contains(x, z, dimension) && r.isActiveNow()) {
                return true;
            }
        }
        return false;
    }
}

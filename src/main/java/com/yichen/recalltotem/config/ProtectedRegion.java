package com.yichen.recalltotem.config;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ProtectedRegion {
    @SerializedName("center_x")
    public int centerX = 0;

    @SerializedName("center_z")
    public int centerZ = 0;

    @SerializedName("radius_blocks")
    public int radiusBlocks = 0;

    @SerializedName("dimension")
    public String dimension = "minecraft:overworld";

    @SerializedName("deny_message")
    public String denyMessage = "";

    @SerializedName("admin_note")
    public String adminNote = "";

    @SerializedName("owner_uuid")
    public String ownerUuid = "";

    @SerializedName("allowed_roles")
    public List<String> allowedRoles = new ArrayList<>();

    @SerializedName("whitelist")
    public List<String> whitelist = new ArrayList<>();

    @SerializedName("schedules")
    public List<ScheduleWindow> schedules = new ArrayList<>();

    public static class ScheduleWindow {
        @SerializedName("start_iso")
        public String startIso = "";
        @SerializedName("end_iso")
        public String endIso = "";
    }

    public ProtectedRegion() {}

    public ProtectedRegion(int x, int z, int radius, String dim) {
        this.centerX = x;
        this.centerZ = z;
        this.radiusBlocks = radius;
        this.dimension = dim;
    }

    public boolean contains(int x, int z, String dim) {
        if (this.dimension != null && !this.dimension.isEmpty() && !this.dimension.equals(dim)) {
            return false;
        }
        long dx = x - centerX;
        long dz = z - centerZ;
        return dx * dx + dz * dz <= (long) radiusBlocks * radiusBlocks;
    }

    public boolean isActiveNow() {
        if (schedules == null || schedules.isEmpty()) {
            return true;
        }
        Instant now = Instant.now();
        for (ScheduleWindow s : schedules) {
            if (s == null) continue;
            try {
                if (s.startIso == null || s.startIso.isEmpty() || s.endIso == null || s.endIso.isEmpty()) {
                    continue;
                }
                Instant start = Instant.parse(s.startIso);
                Instant end = Instant.parse(s.endIso);
                if (!end.isBefore(start) && (now.equals(start) || now.equals(end) || (now.isAfter(start) && now.isBefore(end)))) {
                    return true;
                }
            } catch (DateTimeParseException e) {
                // ignore malformed schedule entries
            } catch (Exception e) {
                // ignore unexpected parsing errors
            }
        }
        return false;
    }
}

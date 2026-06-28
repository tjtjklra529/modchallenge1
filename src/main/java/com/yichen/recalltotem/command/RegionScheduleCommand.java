package com.yichen.recalltotem.command;

import com.yichen.recalltotem.RecallTotemMod;
import com.yichen.recalltotem.audit.AuditManager;
import com.yichen.recalltotem.config.ModConfig;
import com.yichen.recalltotem.config.ProtectedRegion;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

public class RegionScheduleCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("recall")
                    .then(literal("region")
                        .then(literal("schedule")
                            .then(literal("add")
                                .then(argument("index", IntegerArgumentType.integer(0))
                                    .then(argument("start", StringArgumentType.word())
                                        .then(argument("end", StringArgumentType.word())
                                            .executes(ctx -> add(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "index"),
                                                StringArgumentType.getString(ctx, "start"),
                                                StringArgumentType.getString(ctx, "end")))
                                        )
                                    )
                                )
                            )
                            .then(literal("remove")
                                .then(argument("index", IntegerArgumentType.integer(0))
                                    .then(argument("schedIndex", IntegerArgumentType.integer(0))
                                        .executes(ctx -> remove(ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "index"),
                                            IntegerArgumentType.getInteger(ctx, "schedIndex")))
                                    )
                                )
                            )
                            .then(literal("list")
                                .then(argument("index", IntegerArgumentType.integer(0))
                                    .executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index")))
                                )
                            )
                            .then(literal("validate")
                                .then(argument("start", StringArgumentType.word())
                                    .then(argument("end", StringArgumentType.word())
                                        .executes(ctx -> validate(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "start"),
                                            StringArgumentType.getString(ctx, "end")))
                                    )
                                )
                            )
                        )
                    )
            );
        });
    }

    private static int add(ServerCommandSource source, int index, String startIso, String endIso) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to add schedules."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (regions == null || index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        Instant start;
        Instant end;
        try {
            start = Instant.parse(startIso);
            end = Instant.parse(endIso);
        } catch (DateTimeParseException e) {
            source.sendError(Text.literal("Invalid ISO-8601 datetime. Use format like 2026-07-01T08:00:00Z"));
            return 0;
        }
        if (end.isBefore(start) || end.equals(start)) {
            source.sendError(Text.literal("End time must be after start time."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        ProtectedRegion.ScheduleWindow s = new ProtectedRegion.ScheduleWindow();
        s.startIso = startIso;
        s.endIso = endIso;
        r.schedules.add(s);
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Added schedule to region #" + index + ": " + startIso + " -> " + endIso), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.schedule.add", "index=" + index + " start=" + startIso + " end=" + endIso);
        RecallTotemMod.LOGGER.info("Added schedule to region #" + index + " " + startIso + " -> " + endIso);
        return 1;
    }

    private static int remove(ServerCommandSource source, int index, int schedIndex) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to remove schedules."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (regions == null || index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        if (schedIndex < 0 || schedIndex >= r.schedules.size()) {
            source.sendError(Text.literal("Invalid schedule index."));
            return 0;
        }
        r.schedules.remove(schedIndex);
        ModConfig.save();
        source.sendFeedback(() -> Text.literal("Removed schedule #" + schedIndex + " from region #" + index), false);
        String actor = source.getName();
        AuditManager.log(actor, "region.schedule.remove", "index=" + index + " schedIndex=" + schedIndex);
        return 1;
    }

    private static int list(ServerCommandSource source, int index) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to list schedules."));
            return 0;
        }
        var regions = ModConfig.get().protectedRegions;
        if (regions == null || index < 0 || index >= regions.size()) {
            source.sendError(Text.literal("Invalid region index."));
            return 0;
        }
        ProtectedRegion r = regions.get(index);
        List<ProtectedRegion.ScheduleWindow> schedules = r.schedules;
        if (schedules == null || schedules.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Region #" + index + " has no schedules."), false);
            return 1;
        }
        for (int i = 0; i < schedules.size(); i++) {
            ProtectedRegion.ScheduleWindow sw = schedules.get(i);
            int idx = i;
            source.sendFeedback(() -> Text.literal("#" + idx + ": " + sw.startIso + " -> " + sw.endIso), false);
        }
        String actor = source.getName();
        AuditManager.log(actor, "region.schedule.list", "index=" + index + " count=" + schedules.size());
        return schedules.size();
    }

    private static int validate(ServerCommandSource source, String startIso, String endIso) {
        if (!source.hasPermissionLevel(2)) {
            source.sendError(Text.literal("You do not have permission to validate schedules."));
            return 0;
        }
        try {
            Instant start = Instant.parse(startIso);
            Instant end = Instant.parse(endIso);
            if (end.isBefore(start) || end.equals(start)) {
                source.sendError(Text.literal("Invalid schedule: end must be after start."));
                return 0;
            }
            source.sendFeedback(() -> Text.literal("Schedule is valid: " + startIso + " -> " + endIso), false);
            String actor = source.getName();
            AuditManager.log(actor, "region.schedule.validate", "start=" + startIso + " end=" + endIso);
            return 1;
        } catch (DateTimeParseException e) {
            source.sendError(Text.literal("Invalid ISO-8601 datetime. Use format like 2026-07-01T08:00:00Z"));
            return 0;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to validate schedule."));
            RecallTotemMod.LOGGER.warn("Schedule validation error", e);
            return 0;
        }
    }
}

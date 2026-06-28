package com.yichen.recalltotem.command;

public final class RecallCommands {
    public static void register() {
        ProtectedRegionCommands.register();
        RegionEditCommand.register();
        RegionImportCommand.register();
        RegionExportCommand.register();
        RegionPreviewCommand.register();
        RegionPreviewToggleCommand.register();
        RegionScheduleCommand.register();
        RegionOwnershipCommands.register();
        RegionAuditCommand.register();
        RegionAuditFilterCommand.register();
        RegionWhitelistCommands.register();
        RegionWhitelistBulkCommand.register();
    }
}

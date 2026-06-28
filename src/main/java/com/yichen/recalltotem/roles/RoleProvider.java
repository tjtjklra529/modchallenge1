package com.yichen.recalltotem.roles;

import net.minecraft.server.network.ServerPlayerEntity;

public interface RoleProvider {
    boolean playerHasRole(ServerPlayerEntity player, String role);

    RoleProvider NOOP = (player, role) -> false;
}

package com.yichen.recalltotem.roles;

import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.UUID;

public final class LuckPermsRoleAdapter implements RoleProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsRoleAdapter.class);

    private final Object luckPermsApi;
    private final Method getUserManagerMethod;
    private final Method getUserMethod;
    private final Method getPrimaryGroupMethod;
    private final Method userHasPermissionMethod;

    private LuckPermsRoleAdapter(Object api, Method getUserManagerMethod, Method getUserMethod, Method getPrimaryGroupMethod, Method userHasPermissionMethod) {
        this.luckPermsApi = api;
        this.getUserManagerMethod = getUserManagerMethod;
        this.getUserMethod = getUserMethod;
        this.getPrimaryGroupMethod = getPrimaryGroupMethod;
        this.userHasPermissionMethod = userHasPermissionMethod;
    }

    public static LuckPermsRoleAdapter tryCreate() {
        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Method get = providerClass.getMethod("get");
            Object api = get.invoke(null);

            Method getUserManager = api.getClass().getMethod("getUserManager");
            Object userManager = getUserManager.invoke(api);

            Method getUser = null;
            for (Method m : userManager.getClass().getMethods()) {
                if (m.getName().equals("getUser") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == UUID.class) {
                    getUser = m;
                    break;
                }
            }

            Method getPrimaryGroup = null;
            Method hasPermission = null;
            try {
                Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
                for (Method m : userClass.getMethods()) {
                    if (m.getName().equals("getPrimaryGroup") && m.getParameterCount() == 0) getPrimaryGroup = m;
                    if (m.getName().equals("hasPermission") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) hasPermission = m;
                }
            } catch (ClassNotFoundException ignored) {}

            LOGGER.info("LuckPerms detected; adapter created");
            return new LuckPermsRoleAdapter(api, getUserManager, getUser, getPrimaryGroup, hasPermission);
        } catch (ClassNotFoundException e) {
            LOGGER.info("LuckPerms not present");
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize LuckPerms adapter", e);
            return null;
        }
    }

    @Override
    public boolean playerHasRole(ServerPlayerEntity player, String role) {
        try {
            if (luckPermsApi == null || getUserMethod == null) return false;
            UUID uuid = player.getUuid();

            Object userManager = getUserManagerMethod.invoke(luckPermsApi);
            Object userObj = getUserMethod.invoke(userManager, uuid);

            if (userObj == null) return false;

            if (getPrimaryGroupMethod != null) {
                Object grp = getPrimaryGroupMethod.invoke(userObj);
                if (grp != null && role.equalsIgnoreCase(String.valueOf(grp))) return true;
            }

            if (userHasPermissionMethod != null) {
                String node = "recall.region.bypass." + role;
                Object res = userHasPermissionMethod.invoke(userObj, node);
                if (res instanceof Boolean && (Boolean) res) return true;
            }

            return false;
        } catch (Exception e) {
            LOGGER.debug("LuckPerms role check failed", e);
            return false;
        }
    }
}

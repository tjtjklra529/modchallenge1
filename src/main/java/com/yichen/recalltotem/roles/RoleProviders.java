package com.yichen.recalltotem.roles;

import java.util.concurrent.atomic.AtomicReference;

public final class RoleProviders {
    private static final AtomicReference<RoleProvider> PROVIDER = new AtomicReference<>(RoleProvider.NOOP);

    private RoleProviders() {}

    public static void register(RoleProvider provider) {
        if (provider != null) PROVIDER.set(provider);
    }

    public static RoleProvider get() {
        return PROVIDER.get();
    }
}

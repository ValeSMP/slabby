package com.valesmp.slabby.wrapper.permission;

import java.util.UUID;

public interface PermissionWrapper {

    boolean hasPermission(final UUID uniqueId, final String permission);

    default boolean ifPermission(final UUID uniqueId, final String permission, final Runnable action) {
        final boolean hasPermission = hasPermission(uniqueId, permission);

        if (hasPermission)
            action.run();

        return hasPermission;
    }

}

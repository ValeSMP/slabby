package gg.mew.slabby.wrapper.permission;

import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.UUID;

public final class BukkitPermissionWrapper implements PermissionWrapper {

    @Override
    public boolean hasPermission(final UUID uniqueId, final String permission) {
        return Objects.requireNonNull(Bukkit.getPlayer(uniqueId)).hasPermission(permission);
    }

}

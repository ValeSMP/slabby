package com.valesmp.slabby.wrapper.claim;

import com.valesmp.slabby.SlabbyHelper;
import lombok.RequiredArgsConstructor;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public final class LandsClaimWrapper implements ClaimWrapper {

    private static final String LANDS_BLOCK_PLACE_BYPASS = "lands.bypass.block_place";

    private final LandsIntegration lands;

    @Override
    public boolean canCreateShop(final UUID uniqueId, final int x, final int y, final int z, final String world) {
        final var bukkitWorld = Bukkit.getWorld(world);
        final var landWorld = lands.getWorld(Objects.requireNonNull(bukkitWorld));

        if (landWorld == null)
            return true;


        if (Objects.requireNonNull(Bukkit.getPlayer(uniqueId)).hasPermission(LANDS_BLOCK_PLACE_BYPASS))
            return true;

        return landWorld.hasRoleFlag(uniqueId, new Location(bukkitWorld, x, y, z), Flags.BLOCK_PLACE);
    }

    @Override
    public Area getArea() {
        final var config = SlabbyHelper.api().configuration().lands();
        return new Area(config.minX(), config.minZ(), config.maxX(), config.maxZ(), config.world());
    }

}

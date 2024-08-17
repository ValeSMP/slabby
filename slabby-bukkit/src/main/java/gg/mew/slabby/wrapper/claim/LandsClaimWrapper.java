package gg.mew.slabby.wrapper.claim;

import gg.mew.slabby.SlabbyHelper;
import gg.mew.slabby.shop.Shop;
import lombok.RequiredArgsConstructor;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.land.ChunkCoordinate;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor
public final class LandsClaimWrapper implements ClaimWrapper {

    private static final String LANDS_SHOPPING_DISTRICT_NAME = "spawn";
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
    public boolean isInShoppingDistrict(final Shop shop) {
        final var location = new Location(Bukkit.getWorld(shop.world()), shop.x(), shop.y(), shop.z());
        final var chunk = location.getChunk();

        //NOTE: getLandByChunk is preferred according to the api, but there is no guarantee the chunk is loaded, thus this.
        final var land = this.lands.getLandByUnloadedChunk(location.getWorld(), chunk.getX(), chunk.getZ());

        //TODO: Support shops outside of shopping district, different dimensions, etc...
        return land != null && land.getName().equalsIgnoreCase(LANDS_SHOPPING_DISTRICT_NAME);
    }

    @Override
    public Area getArea() {
        final var config = SlabbyHelper.api().configuration().lands();
        return new Area(config.minX(), config.minZ(), config.maxX(), config.maxZ(), config.world());
    }

}

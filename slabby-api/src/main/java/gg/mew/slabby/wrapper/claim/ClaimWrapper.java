package gg.mew.slabby.wrapper.claim;

import java.util.UUID;

public interface ClaimWrapper {

    boolean canCreateShop(final UUID uniqueId, final int x, final int y, final int z, final String world);

}

package gg.mew.slabby.wrapper.claim;

import gg.mew.slabby.shop.Shop;

import java.util.UUID;

public interface ClaimWrapper {

    boolean canCreateShop(final UUID uniqueId, final int x, final int y, final int z, final String world);

    boolean isInShoppingDistrict(final Shop shop);

    Area getArea();

    record Area(int minX, int minZ, int maxX, int maxZ, String world) {}

}

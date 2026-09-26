package com.valesmp.slabby.cache;

import com.j256.ormlite.dao.Dao;
import com.valesmp.slabby.shop.SQLiteShop;
import com.valesmp.slabby.shop.Shop;

public final class ShopCache extends DaoCache<SQLiteShop, Integer, ShopCache.Position> {

    public record Position(int x, int y, int z, String world) {}

    public ShopCache(final Dao<SQLiteShop, Integer> dao) {
        super(dao, SQLiteShop::id);
    }

    public void store(final int x, final int y, final int z, final String world, final Shop shop) {
        this.store(new Position(x, y, z, world), (SQLiteShop) shop);
    }

    public void store(final Shop shop) {
        if (shop.hasLocation())
            this.store(shop.x(), shop.y(), shop.z(), shop.world(), shop);

        if (shop.hasInventory())
            this.store(shop.inventoryX(), shop.inventoryY(), shop.inventoryZ(), shop.inventoryWorld(), shop);
    }

    public Cached get(final int x, final int y, final int z, final String world) {
        return get(new Position(x, y, z, world));
    }

    public void delete(final int x, final int y, final int z, final String world) {
        this.delete(new Position(x, y, z, world));
    }

}

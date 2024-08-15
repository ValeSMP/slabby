package gg.mew.slabby.cache;

import com.j256.ormlite.dao.Dao;
import gg.mew.slabby.shop.SQLiteShop;
import gg.mew.slabby.shop.Shop;

import java.util.Objects;

public final class ShopCache extends DaoCache<SQLiteShop, Integer, Integer> {

    public ShopCache(final Dao<SQLiteShop, Integer> dao) {
        super(dao, SQLiteShop::id);
    }

    public static int hash(final int x, final int y, final int z, final String world) {
        return Objects.hash(x, y, z, world);
    }

    public void store(final int x, final int y, final int z, final String world, final Shop shop) {
        this.store(hash(x, y, z, world), (SQLiteShop) shop);
    }

    public void store(final Shop shop) {
        if (shop.hasLocation())
            this.store(shop.x(), shop.y(), shop.z(), shop.world(), shop);

        if (shop.hasInventory())
            this.store(shop.inventoryX(), shop.inventoryY(), shop.inventoryZ(), shop.inventoryWorld(), shop);
    }

    public Cached get(final int x, final int y, final int z, final String world) {
        return get(hash(x, y, z, world));
    }

    public void delete(final int x, final int y, final int z, final String world) {
        this.delete(hash(x, y, z, world));
    }

}

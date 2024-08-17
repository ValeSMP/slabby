package gg.mew.slabby.shop;

import gg.mew.slabby.exception.SlabbyException;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

public interface ShopRepository {

    <T> T builder(final Class<?> builderType);

    Cache shopCache();

    void createOrUpdate(final Shop shop) throws SlabbyException;
    void createOrUpdate(final ShopOwner shopOwner) throws SlabbyException;

    void delete(final Shop shop) throws SlabbyException;
    void delete(final ShopOwner shopOwner) throws SlabbyException;

    void update(final Shop shop) throws SlabbyException;
    void update(final ShopOwner shopOwner) throws SlabbyException;

    void refresh(final Shop shop) throws SlabbyException;
    void refresh(final ShopOwner shopOwner) throws SlabbyException;

    void markAsDeleted(final UUID uniqueId, final Shop shop) throws SlabbyException;

    <T> Optional<Shop> shopById(final T id) throws SlabbyException;
    
    Optional<Shop> shopAt(final int x, final int y, final int z, final String world) throws SlabbyException;

    Optional<Shop> shopWithInventoryAt(final int x, final int y, final int z, final String world) throws SlabbyException;

    Collection<Shop> shopsOf(final UUID uniqueId, final Shop.State state) throws SlabbyException;

    Collection<Shop> shopsByItem(final String item) throws SlabbyException;

    Collection<Shop> shopsInArea(final int minX, final int minZ, final int maxX, final int maxZ, final String world);

    boolean isShopOrInventory(final int x, final int y, final int z, final String world) throws SlabbyException;

    @SuppressWarnings("UnusedReturnValue")
    <T> T transaction(final Callable<T> transaction) throws SlabbyException;

}

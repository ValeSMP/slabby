package gg.mew.slabby.shop;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.exception.SlabbyException;
import gg.mew.slabby.exception.UnrecoverableException;
import gg.mew.slabby.shop.log.ValueChanged;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

public final class SQLiteShopRepository implements ShopRepository, Closeable {

    @SuppressWarnings("FieldCanBeLocal")
    private final SlabbyAPI api;

    private final ConnectionSource connectionSource;

    private final Dao<SQLiteShop, Integer> shopDao;
    private final Dao<SQLiteShopOwner, Integer> shopOwnerDao;
    private final Dao<SQLiteShopLog, Integer> shopLogDao;

    public SQLiteShopRepository(final SlabbyAPI api) throws SQLException {
        this.api = api;

        this.connectionSource = new JdbcConnectionSource(api.configuration().database().url());

        this.shopDao = DaoManager.createDao(this.connectionSource, SQLiteShop.class);
        this.shopOwnerDao = DaoManager.createDao(this.connectionSource, SQLiteShopOwner.class);
        this.shopLogDao = DaoManager.createDao(this.connectionSource, SQLiteShopLog.class);

        //NOTE: weakly referenced cache, any operation where a shop object is long-lived should force a refresh beforehand
        //TODO(TEST): may not work with QueryBuilder
        this.shopDao.setObjectCache(true);
        this.shopOwnerDao.setObjectCache(true);
    }

    public void initialize() throws SQLException {
        TableUtils.createTableIfNotExists(this.connectionSource, SQLiteShop.class);
        TableUtils.createTableIfNotExists(this.connectionSource, SQLiteShopOwner.class);
        TableUtils.createTableIfNotExists(this.connectionSource, SQLiteShopLog.class);
    }

    @Override
    public void close() {
        try {
            this.connectionSource.close();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T builder(final Class<?> builderType) {
        if (builderType == Shop.Builder.class)
            return (T) SQLiteShop.builder();

        if (builderType == ShopOwner.Builder.class)
            return (T) SQLiteShopOwner.builder();

        if (builderType == ShopLog.Builder.class)
            return (T) SQLiteShopLog.builder();

        throw new IllegalArgumentException();
    }

    @Override
    public void createOrUpdate(final Shop shop) throws SlabbyException {
        try {
            this.shopDao.createOrUpdate((SQLiteShop) shop);
            this.shopDao.refresh((SQLiteShop) shop); //NOTE: Required because the owners collection is not eagerly loaded
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while inserting or updating shop", e);
        }
    }

    @Override
    public void delete(final Shop shop) throws SlabbyException {
        try {
            this.shopDao.delete((SQLiteShop) shop);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while deleting shop", e);
        }
    }

    @Override
    public void delete(final ShopOwner shopOwner) throws SlabbyException {
        try {
            this.shopOwnerDao.delete((SQLiteShopOwner) shopOwner);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while deleting shop owner", e);
        }
    }

    @Override
    public void createOrUpdate(final ShopOwner shopOwner) throws SlabbyException {
        try {
            this.shopOwnerDao.createOrUpdate((SQLiteShopOwner) shopOwner);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while inserting or updating shop owner", e);
        }
    }

    @Override
    public void update(final Shop shop) throws SlabbyException {
        try {
            this.shopDao.update((SQLiteShop) shop);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while updating shop", e);
        }
    }

    @Override
    public void update(final ShopOwner shopOwner) throws SlabbyException {
        try {
            this.shopOwnerDao.update((SQLiteShopOwner) shopOwner);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while updating shop owner", e);
        }
    }

    @Override
    public void refresh(final Shop shop) throws SlabbyException {
        try {
            this.shopDao.refresh((SQLiteShop) shop);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while refreshing shop", e);
        }
    }

    @Override
    public void refresh(final ShopOwner shopOwner) throws SlabbyException {
        try {
            this.shopOwnerDao.refresh((SQLiteShopOwner) shopOwner);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while refreshing shop owner", e);
        }
    }

    @Override
    public void markAsDeleted(final UUID uniqueId, final Shop shop) throws SlabbyException {
        shop.state(Shop.State.DELETED);
        shop.location(null, null, null, null);

        //NOTE: We remove the inventory link because otherwise a new shop cannot be linked to this location
        //NOTE: We also cannot add the shop state to the index because then a shop cannot be restored if another shop uses that inventory location
        shop.inventory(null, null, null, null);

        this.transaction(() -> {
            this.shopDao.update((SQLiteShop) shop);

            final var log = api.repository()
                    .<ShopLog.Builder>builder(ShopLog.Builder.class)
                    .action(ShopLog.Action.SHOP_DESTROYED)
                    .uniqueId(uniqueId)
                    .build();

            shop.logs().add(log);

            return null;
        });
    }

    @Override
    public Optional<Shop> shopAt(final int x, final int y, final int z, final String world) throws SlabbyException {
        try {
            final var result = this.shopDao.queryBuilder()
                    .where()
                    .eq(Shop.Names.STATE, Shop.State.ACTIVE)
                    .and()
                    .eq(Shop.Names.X, x)
                    .and()
                    .eq(Shop.Names.Y, y)
                    .and()
                    .eq(Shop.Names.Z, z)
                    .and()
                    .eq(Shop.Names.WORLD, world)
                    .queryForFirst();
            return Optional.ofNullable(result);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while retrieving shop by location", e);
        }
    }

    @Override
    public Optional<Shop> shopWithInventoryAt(final int x, final int y, final int z, final String world) throws SlabbyException {
        try {
            final var result = this.shopDao.queryBuilder()
                    .where()
                    .eq(Shop.Names.STATE, Shop.State.ACTIVE)
                    .and()
                    .eq(Shop.Names.INVENTORY_X, x)
                    .and()
                    .eq(Shop.Names.INVENTORY_Y, y)
                    .and()
                    .eq(Shop.Names.INVENTORY_Z, z)
                    .and()
                    .eq(Shop.Names.INVENTORY_WORLD, world)
                    .queryForFirst();
            return Optional.ofNullable(result);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while retrieving shop by inventory location", e);
        }
    }

    @Override
    public <T> Optional<Shop> shopById(final T id) throws SlabbyException {
        if (id == null)
            return Optional.empty();

        try {
            return Optional.ofNullable(this.shopDao.queryForId((int)id));
        } catch (SQLException e) {
            throw new UnrecoverableException("Error while retrieving shop by id");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Shop> shopsOf(final UUID uniqueId, final Shop.State state) throws SlabbyException {
        try {
            final var result = this.shopOwnerDao.queryBuilder()
                    .join(this.shopDao.queryBuilder().where().eq(Shop.Names.STATE, state).queryBuilder())
                    .where().eq(ShopOwner.Names.UNIQUE_ID, uniqueId)
                    .query()
                    .stream()
                    .map(SQLiteShopOwner::shop)
                    .toList();
            return (Collection<Shop>) (Collection<? extends Shop>) result;
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while retrieving shops for owner");
        }
    }

    @Override
    public <T> T transaction(final Callable<T> transaction) throws SlabbyException {
        try {
            return TransactionManager.callInTransaction(this.connectionSource, transaction);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while running transaction", e);
        }
    }
}

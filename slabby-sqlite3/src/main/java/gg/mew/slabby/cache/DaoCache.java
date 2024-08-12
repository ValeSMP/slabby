package gg.mew.slabby.cache;

import com.j256.ormlite.dao.Dao;
import gg.mew.slabby.exception.SlabbyException;
import gg.mew.slabby.exception.UnrecoverableException;
import lombok.experimental.Accessors;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Accessors(fluent = true, chain = false)
public class DaoCache<T, ID, CacheID> {

    private final Dao<T, ID> dao;

    protected final Map<CacheID, Cached<ID>> cache = new HashMap<>();

    private final Function<T, ID> resolver;

    //TODO: cached locations that aren't accessed in N amount of time should be removed

    public DaoCache(final Dao<T, ID> dao, final Function<T, ID> resolver) {
        this.dao = dao;
        this.resolver = resolver;
    }

    public Optional<T> get(final CacheID cacheID) throws SlabbyException {
        final var cached = cache.get(cacheID);

        if (cached != null && cached.exists()) {
            try {
                return Optional.of(this.dao.queryForId(cached.id()));
            } catch (final SQLException e) {
                throw new UnrecoverableException("Error while retrieving shop from cache", e);
            }
        }

        return Optional.empty();
    }

    public void store(final CacheID cacheID, final T item) {
        this.cache.put(cacheID, item == null ? new Cached<>(null) : new Cached<>(resolver.apply(item)));
    }

    protected record Cached<ID>(ID id) {

        public boolean exists() {
            return id != null;
        }

    }

}

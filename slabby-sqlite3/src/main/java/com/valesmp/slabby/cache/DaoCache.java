package com.valesmp.slabby.cache;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ReferenceObjectCache;
import com.valesmp.slabby.exception.SlabbyException;
import com.valesmp.slabby.exception.UnrecoverableException;
import com.valesmp.slabby.shop.Cache;
import com.valesmp.slabby.shop.SQLiteShop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Accessors(fluent = true, chain = false)
public class DaoCache<T, ID, CacheID> implements Cache {

    private final Dao<T, ID> dao;

    protected final Map<CacheID, Cached> cache = new HashMap<>();

    private final Function<T, ID> resolver;

    public DaoCache(final Dao<T, ID> dao, final Function<T, ID> resolver) {
        this.dao = dao;
        this.resolver = resolver;

        try {
            this.dao.setObjectCache(true);
        } catch (final SQLException e) {
            throw new UnrecoverableException("Error while creating object cache", e);
        }
    }

    public Cached get(final CacheID cacheID) throws SlabbyException {
        final var cached = cache.get(cacheID);

        if (cached != null) {
            cached.accessed();

            return cached;
        }

        return null;
    }

    public void store(final CacheID cacheID, final T item) {
        this.cache.put(cacheID, item == null ? new Cached(null) : new Cached(resolver.apply(item)));
    }

    public void delete(final CacheID cacheID) {
        this.cache.remove(cacheID);
    }

    public void expire(final Duration time) {
        final var now = LocalDateTime.now();

        this.cache.entrySet().removeIf(e -> e.getValue().lastAccessed().plus(time).isBefore(now));

        ((ReferenceObjectCache)this.dao.getObjectCache()).cleanNullReferences(SQLiteShop.class);
    }

    @Getter
    @RequiredArgsConstructor
    public final class Cached {

        private final ID identity;

        private LocalDateTime lastAccessed = LocalDateTime.now();

        public void accessed() {
            this.lastAccessed = LocalDateTime.now();
        }

        public boolean hasIdentity() {
            return identity != null;
        }

        public T get() throws SlabbyException {
            try {
                return dao.queryForId(this.identity());
            } catch (final SQLException e) {
                throw new UnrecoverableException("Error while retrieving shop from cache", e);
            }
        }

    }

}

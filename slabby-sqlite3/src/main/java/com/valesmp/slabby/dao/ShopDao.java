package com.valesmp.slabby.dao;

import com.j256.ormlite.support.ConnectionSource;
import com.valesmp.slabby.shop.SQLiteShop;

import java.sql.SQLException;
import java.util.Collection;

public final class ShopDao extends AuditDao<SQLiteShop> {

    public ShopDao(final ConnectionSource connectionSource, final Class<SQLiteShop> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    @Override
    public int delete(final SQLiteShop data) throws SQLException {
        data.owners().clear();
        return super.delete(data);
    }

    @Override
    public int delete(Collection<SQLiteShop> datas) throws SQLException {
        datas.forEach(it -> it.owners().clear());
        return super.delete(datas);
    }

}

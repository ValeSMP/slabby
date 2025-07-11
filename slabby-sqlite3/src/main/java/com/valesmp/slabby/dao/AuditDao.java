package com.valesmp.slabby.dao;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;
import com.valesmp.slabby.SlabbyHelper;
import com.valesmp.slabby.audit.Auditable;

import java.sql.SQLException;
import java.util.Collection;

public class AuditDao<T extends Auditable> extends BaseDaoImpl<T, Integer> {

    public AuditDao(final ConnectionSource connectionSource, final Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    @Override
    public int create(final T data) throws SQLException {
        data.createdOn(SlabbyHelper.api().legacyNow());
        return super.create(data);
    }

    @Override
    public int create(final Collection<T> datas) throws SQLException {
        final var now = SlabbyHelper.api().legacyNow();
        datas.forEach(it -> it.createdOn(now));
        return super.create(datas);
    }

    @Override
    public synchronized CreateOrUpdateStatus createOrUpdate(final T data) throws SQLException {
        final var now = SlabbyHelper.api().legacyNow();

        if (data.createdOn() == null)
            data.createdOn(now);
        else {
            data.lastModifiedOn(now);
        }
        return super.createOrUpdate(data);
    }

    @Override
    public int update(final T data) throws SQLException {
        data.lastModifiedOn(SlabbyHelper.api().legacyNow());
        return super.update(data);
    }

}

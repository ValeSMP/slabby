package com.valesmp.slabby.shop;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.valesmp.slabby.dao.AuditDao;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.UUID;

@DatabaseTable(tableName = "shop_logs", daoClass = AuditDao.class)
@Builder
@Accessors(fluent = true, chain = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class SQLiteShopLog implements ShopLog {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private SQLiteShop shop;

    @DatabaseField(canBeNull = true)
    private UUID uniqueId;

    @DatabaseField(canBeNull = false)
    private Action action;

    @DatabaseField(canBeNull = true)
    private String data;

    @DatabaseField(canBeNull = false)
    private Date createdOn;

    @DatabaseField(canBeNull = true)
    private Date lastModifiedOn;

    public static final class SQLiteShopLogBuilder implements ShopLog.Builder {}

}

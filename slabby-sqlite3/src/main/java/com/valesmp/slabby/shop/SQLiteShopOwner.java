package com.valesmp.slabby.shop;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.valesmp.slabby.dao.AuditDao;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.UUID;

@Builder
@DatabaseTable(tableName = "shop_owners", daoClass = AuditDao.class)
@Accessors(fluent = true, chain = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class SQLiteShopOwner implements ShopOwner {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, uniqueCombo = true)
    private SQLiteShop shop;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private UUID uniqueId;

    @DatabaseField(canBeNull = false)
    private int share;

    @DatabaseField(canBeNull = false)
    private Date createdOn;

    @DatabaseField(canBeNull = true)
    private Date lastModifiedOn;

    public static final class SQLiteShopOwnerBuilder implements ShopOwner.Builder {}

}

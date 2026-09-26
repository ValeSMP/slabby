package com.valesmp.slabby.shop;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.UUID;

@DatabaseTable(tableName = "pending_notifications")
@Builder
@Accessors(fluent = true, chain = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class SQLitePendingNotification implements PendingNotification {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, index = true)
    private UUID recipient;

    @DatabaseField(canBeNull = false)
    private ShopLog.Action action;

    @DatabaseField(canBeNull = false)
    private int quantity;

    @DatabaseField(canBeNull = false)
    private double amount;

    @DatabaseField(canBeNull = false)
    private Date createdOn;

}

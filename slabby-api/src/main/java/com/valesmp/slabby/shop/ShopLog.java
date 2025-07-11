package com.valesmp.slabby.shop;

import com.valesmp.slabby.SlabbyHelper;
import com.valesmp.slabby.audit.Auditable;
import com.valesmp.slabby.shop.log.LocationChanged;
import com.valesmp.slabby.shop.log.Transaction;
import com.valesmp.slabby.shop.log.ValueChanged;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.UUID;

public interface ShopLog extends Auditable {

    UUID uniqueId();
    Action action();
    String data();

    @Getter
    @Accessors(fluent = true, chain = false)
    enum Action {
        LOCATION_CHANGED(LocationChanged.class),
        BUY_PRICE_CHANGED(ValueChanged.Double.class),
        SELL_PRICE_CHANGED(ValueChanged.Double.class),
        QUANTITY_CHANGED(ValueChanged.Int.class),
        NOTE_CHANGED(ValueChanged.String.class),
        NAME_CHANGED(ValueChanged.String.class),

        INVENTORY_LINK_CHANGED(LocationChanged.class),

        OWNER_ADDED(Void.class),
        OWNER_REMOVED(Void.class),

        BUY(Transaction.class),
        SELL(Transaction.class),

        DEPOSIT(Transaction.class),
        WITHDRAW(Transaction.class),

        SHOP_CREATED(Void.class),
        SHOP_DESTROYED(Void.class);

        private final Class<?> dataClass;

        Action(final Class<?> dataClass) {
            this.dataClass = dataClass;
        }
    }

    interface Builder {

        Builder action(final Action action);
        Builder uniqueId(final UUID uniqueId);
        Builder data(final String data);
        ShopLog build();

        default Builder serialized(final Object any) {
            return this.data(SlabbyHelper.api().toJson(any));
        }

    }

}

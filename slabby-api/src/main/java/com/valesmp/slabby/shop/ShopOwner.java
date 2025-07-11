package com.valesmp.slabby.shop;

import com.valesmp.slabby.audit.Auditable;
import lombok.experimental.UtilityClass;

import java.util.UUID;

public interface ShopOwner extends Auditable {

    @UtilityClass
    final class Names {
        public final String UNIQUE_ID = "uniqueId";
        public final String SHARE = "share";
    }

    UUID uniqueId();

    int share();

    interface Builder {

        Builder uniqueId(final UUID uniqueId);
        Builder share(final int share);
        ShopOwner build();

    }

}

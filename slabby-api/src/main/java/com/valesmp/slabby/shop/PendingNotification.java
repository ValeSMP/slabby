package com.valesmp.slabby.shop;

import java.util.Date;
import java.util.UUID;

public interface PendingNotification {

    UUID recipient();

    ShopLog.Action action();

    int quantity();

    double amount();

    Date createdOn();

}

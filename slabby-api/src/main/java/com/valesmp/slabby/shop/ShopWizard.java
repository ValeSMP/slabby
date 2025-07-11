package com.valesmp.slabby.shop;

import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public interface ShopWizard {

    <T> T id();

    Shop shop();
    WizardState wizardState();
    Shop.State state();
    Integer x();
    Integer y();
    Integer z();
    String world();
    String item();
    String note();
    Double buyPrice();
    Double sellPrice();
    int quantity();
    Map<ShopLog.Action, Object> valueChanges();

    ShopWizard wizardState(final WizardState state);

    ShopWizard location(final Integer x, final Integer y, final Integer z, final String world);

    ShopWizard item(final String item);
    ShopWizard note(final String note);

    ShopWizard buyPrice(final Double buyPrice);
    ShopWizard sellPrice(final Double sellPrice);
    ShopWizard quantity(final int quantity);
    ShopWizard state(final Shop.State state);

    enum WizardState {
        AWAITING_ITEM,
        AWAITING_NOTE,
        AWAITING_BUY_PRICE,
        AWAITING_SELL_PRICE,
        AWAITING_QUANTITY,
        AWAITING_INVENTORY_LINK,
        AWAITING_LOCATION,
        AWAITING_CONFIRMATION,
        AWAITING_TEMP_QUANTITY;

        public boolean awaitingTextInput() {
            return this == AWAITING_NOTE
                    || this == AWAITING_BUY_PRICE
                    || this == AWAITING_SELL_PRICE
                    || this == AWAITING_QUANTITY
                    || this == AWAITING_TEMP_QUANTITY;
        }

    }

}

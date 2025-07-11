package com.valesmp.slabby.shop;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.log.LocationChanged;
import com.valesmp.slabby.shop.log.ValueChanged;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Accessors(fluent = true, chain = true)
@Getter
public final class BukkitShopWizard implements ShopWizard {

    private final SlabbyAPI api;

    private final Map<ShopLog.Action, Object> valueChanges = new HashMap<>();

    @Getter
    private final Integer id;

    @Getter
    private final Shop shop;

    @Setter
    private WizardState wizardState;

    private Integer x;
    private Integer y;
    private Integer z;
    private String world;

    private String item;

    private String note;
    private Double buyPrice;
    private Double sellPrice;
    private int quantity;

    @Setter
    private Shop.State state;

    public BukkitShopWizard(final SlabbyAPI api) {
        this.api = api;

        this.shop = null;
        this.id = null;

        final var buyPrice = this.api.configuration().defaults().buyPrice();
        final var sellPrice = this.api.configuration().defaults().sellPrice();

        this.note = this.api.configuration().defaults().note();
        this.buyPrice = buyPrice == -1D ? null : buyPrice;
        this.sellPrice = sellPrice == -1D ? null : sellPrice;
        this.quantity = this.api.configuration().defaults().quantity();
    }

    public BukkitShopWizard(final SlabbyAPI api, final Shop shop) {
        this.api = api;

        this.shop = shop;

        this.id = shop.id();

        this.x = shop.x();
        this.y = shop.y();
        this.z = shop.z();
        this.world = shop.world();
        this.note = shop.note();
        this.buyPrice = shop.buyPrice();
        this.sellPrice = shop.sellPrice();
        this.quantity = shop.quantity();
        this.item = shop.item();
        this.state = shop.state();
    }

    @Override
    public ShopWizard location(final Integer x, final Integer y, final Integer z, final String world) {
        if (!Objects.equals(this.x, x) && !Objects.equals(this.y, y) && !Objects.equals(this.z, z) && Objects.equals(this.world, world))
            this.valueChanges.put(ShopLog.Action.LOCATION_CHANGED, new LocationChanged(x, y, z, world));
        else
            this.valueChanges.remove(ShopLog.Action.LOCATION_CHANGED);

        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;

        return this;
    }

    @Override
    public ShopWizard item(@NonNull final String item) {
        this.item = item;
        return this;
    }

    @Override
    public ShopWizard note(final String note) {
        if (!Objects.equals(this.note, note))
            this.valueChanges.put(ShopLog.Action.NOTE_CHANGED, new ValueChanged.String(this.note, note));
        else
            this.valueChanges.remove(ShopLog.Action.NOTE_CHANGED);

        this.note = note;
        return this;
    }

    @Override
    public ShopWizard buyPrice(final Double buyPrice) {
        if (!Objects.equals(this.buyPrice, buyPrice))
            this.valueChanges.put(ShopLog.Action.BUY_PRICE_CHANGED, new ValueChanged.Double(this.buyPrice, buyPrice));
        else
            this.valueChanges.remove(ShopLog.Action.BUY_PRICE_CHANGED);

        this.buyPrice = buyPrice;
        return this;
    }

    @Override
    public ShopWizard sellPrice(final Double sellPrice) {
        if (!Objects.equals(this.sellPrice, sellPrice))
            this.valueChanges.put(ShopLog.Action.SELL_PRICE_CHANGED, new ValueChanged.Double(this.sellPrice, sellPrice));
        else
            this.valueChanges.remove(ShopLog.Action.SELL_PRICE_CHANGED);

        this.sellPrice = sellPrice;
        return this;
    }

    @Override
    public ShopWizard quantity(final int quantity) {
        if (this.quantity != quantity)
            this.valueChanges.put(ShopLog.Action.QUANTITY_CHANGED, new ValueChanged.Int(this.quantity, quantity));
        else
            this.valueChanges.remove(ShopLog.Action.QUANTITY_CHANGED);

        this.quantity = quantity;
        return this;
    }

}

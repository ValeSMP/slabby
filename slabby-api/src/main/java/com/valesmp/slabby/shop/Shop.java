package com.valesmp.slabby.shop;

import com.valesmp.slabby.audit.Auditable;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public interface Shop extends Auditable {

    @UtilityClass
    final class Names {
        public final String ITEM = "item";
        public final String X = "x";
        public final String Y = "y";
        public final String Z = "z";
        public final String WORLD = "world";
        public final String BUY_PRICE = "buyPrice";
        public final String SELL_PRICE = "sellPrice";
        public final String QUANTITY = "quantity";
        public final String STOCK = "stock";
        public final String NOTE = "note";
        public final String NAME = "name";
        public final String INVENTORY_X = "inventoryX";
        public final String INVENTORY_Y = "inventoryY";
        public final String INVENTORY_Z = "inventoryZ";
        public final String INVENTORY_WORLD = "inventoryWorld";
        public final String STATE = "state";
    }

    <T> T id();

    String item();

    Integer x();

    Integer y();

    Integer z();

    String world();

    void location(final Integer x, final Integer y, final Integer z, final String world);

    default boolean hasLocation() {
        return x() != null && y() != null && z() != null && world() != null;
    }

    Double buyPrice();

    void buyPrice(final Double buyPrice);

    Double sellPrice();

    void sellPrice(final Double sellPrice);

    int quantity();

    void quantity(final int quantity);

    Integer stock();

    void stock(final Integer stock);

    String note();

    void note(final String note);

    String name();

    void name(final String name);

    Integer inventoryX();

    Integer inventoryY();

    Integer inventoryZ();

    String inventoryWorld();

    State state();

    void state(final State state);

    void inventory(final Integer x, final Integer y, final Integer z, final String world);

    default boolean isAt(final int x, final int y, final int z, final String world) {
        return this.state() == State.ACTIVE &&
                Objects.equals(this.x(), x) && Objects.equals(this.y(), y) && Objects.equals(this.z(), z) && Objects.equals(this.world(), world);
    }

    default boolean isInventoryAt(final int inventoryX, final int inventoryY, final int inventoryZ, final String inventoryWorld) {
        return this.state() == State.ACTIVE &&
                Objects.equals(this.inventoryX(), inventoryX) && Objects.equals(this.inventoryY(), inventoryY) && Objects.equals(this.inventoryZ(), inventoryZ) && Objects.equals(this.inventoryWorld(), inventoryWorld);
    }

    default boolean hasInventory() {
        return inventoryX() != null && inventoryY() != null && inventoryZ() != null && inventoryWorld() != null;
    }

    default boolean hasStock(final int quantity) {
        return stock() == null || stock() >= quantity;
    }

    UUID displayEntityId();

    void displayEntityId(final UUID displayEntityId);

    Collection<ShopOwner> owners();

    Collection<ShopLog> logs();

    boolean isOwner(final UUID uniqueId);

    interface Builder {
         Builder item(final String item);
         Builder location(final Integer x, final Integer y, final Integer z, final String world);
         Builder buyPrice(final Double buyPrice);
         Builder sellPrice(final Double sellPrice);
         Builder quantity(final int quantity);
         Builder stock(final Integer stock);
         Builder note(final String note);
         Builder name(final String name);
         Builder displayEntityId(final UUID displayEntityId);
         Builder inventory(final Integer x, final Integer y, final Integer z, final String world);
         Shop build();

     }

     enum State {
         ACTIVE,
         DELETED,
     }
}
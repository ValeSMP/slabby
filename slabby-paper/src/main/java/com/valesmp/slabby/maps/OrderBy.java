package com.valesmp.slabby.maps;

import com.valesmp.slabby.shop.Shop;
import lombok.Getter;

import java.util.Comparator;
import java.util.function.Predicate;

@Getter
public enum OrderBy implements Comparator<Shop>, Predicate<Shop> {

    BuyAscending(Comparator.comparingDouble(it -> it.buyPrice() / it.quantity()), it ->
            it.buyPrice() != null && it.hasStock(it.quantity())),

    BuyDescending(Comparator.<Shop>comparingDouble(it -> it.buyPrice() / it.quantity()).reversed(), it ->
            it.buyPrice() != null && it.hasStock(it.quantity())),

    SellAscending(Comparator.comparingDouble(it -> it.sellPrice() / it.quantity()), it ->
            it.sellPrice() != -1 && it.quantity() > 0),

    SellDescending(Comparator.<Shop>comparingDouble(it -> it.sellPrice() / it.quantity()).reversed(), it ->
            it.sellPrice() != -1 && it.quantity() > 0);

    private final Comparator<Shop> comparator;
    private final Predicate<Shop> filter;

    OrderBy(final Comparator<Shop> comparator, final Predicate<Shop> filter) {
        this.comparator = comparator;
        this.filter = filter;
    }

    @Override
    public int compare(final Shop o1, final Shop o2) {
        return this.comparator.compare(o1, o2);
    }

    @Override
    public boolean test(final Shop shop) {
        return this.filter.test(shop);
    }
}
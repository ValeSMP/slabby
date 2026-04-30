package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.audit.Auditable;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.shop.ShopLog;
import com.valesmp.slabby.shop.log.LocationChanged;
import com.valesmp.slabby.shop.log.Transaction;
import com.valesmp.slabby.shop.log.ValueChanged;

import dev.hxrry.hxgui.builders.ItemBuilder;
import dev.hxrry.hxgui.components.Pagination;
import dev.hxrry.hxgui.core.MenuItem;

import lombok.experimental.UtilityClass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

// 1.3 changes: replaced manual pagination with HxGUI's Pagination component, added category filter row
// 1.1>2 changes inc. no pagedgui or invui, addition of temporary manual pagination, recursive reopening, max items per page, prev and next buttons conditionally showing and createdLogItem extracted helper method because pretty
@UtilityClass
public final class LogShopUI {

    public enum Filter {
        ALL("All", Material.BOOKSHELF, EnumSet.allOf(ShopLog.Action.class)),
        TRADES("Trades", Material.GOLD_INGOT, EnumSet.of(
                ShopLog.Action.BUY,
                ShopLog.Action.SELL)),
        STOCK("Stock", Material.CHEST, EnumSet.of(
                ShopLog.Action.DEPOSIT,
                ShopLog.Action.WITHDRAW)),
        CHANGES("Changes", Material.WRITABLE_BOOK, EnumSet.of(
                ShopLog.Action.BUY_PRICE_CHANGED,
                ShopLog.Action.SELL_PRICE_CHANGED,
                ShopLog.Action.QUANTITY_CHANGED,
                ShopLog.Action.NOTE_CHANGED,
                ShopLog.Action.NAME_CHANGED,
                ShopLog.Action.LOCATION_CHANGED,
                ShopLog.Action.INVENTORY_LINK_CHANGED,
                ShopLog.Action.OWNER_ADDED,
                ShopLog.Action.OWNER_REMOVED,
                ShopLog.Action.SHOP_CREATED,
                ShopLog.Action.SHOP_DESTROYED));

        private final String label;
        private final Material icon;
        private final Set<ShopLog.Action> actions;

        Filter(final String label, final Material icon, final Set<ShopLog.Action> actions) {
            this.label = label;
            this.icon = icon;
            this.actions = actions;
        }

        public boolean accepts(final ShopLog.Action action) {
            return this.actions.contains(action);
        }
    }

    private static final int FILTER_TRADES_SLOT = 47;
    private static final int FILTER_STOCK_SLOT = 48;
    private static final int FILTER_ALL_SLOT = 50;
    private static final int FILTER_CHANGES_SLOT = 51;

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop) {
        open(api, shopOwner, shop, Filter.ALL);
    }

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop, final Filter filter) {
        final var filteredLogs = shop.logs().stream()
                .filter(log -> filter.accepts(log.action()))
                .sorted(Comparator.comparing(Auditable::createdOn, Comparator.reverseOrder()))
                .toList();

        final var menuItems = new ArrayList<MenuItem>(filteredLogs.size());
        for (final var log : filteredLogs)
            menuItems.add(new MenuItem(createLogItem(api, log)));

        final var pagination = new Pagination(api.messages().log().title(), 6);
        pagination.contentArea(0, 44).navigationSlots(45, 53, 49);
        pagination.setItems(menuItems);
        pagination.open(shopOwner);

        final var personalMenu = pagination.getMenu();
        for (final var f : Arrays.asList(Filter.TRADES, Filter.STOCK, Filter.ALL, Filter.CHANGES)) {
            personalMenu.setItemFor(shopOwner, slotFor(f), new MenuItem(
                    createFilterButton(f, filter == f),
                    event -> open(api, shopOwner, shop, f)));
        }
    }

    private int slotFor(final Filter filter) {
        return switch (filter) {
            case ALL -> FILTER_ALL_SLOT;
            case TRADES -> FILTER_TRADES_SLOT;
            case STOCK -> FILTER_STOCK_SLOT;
            case CHANGES -> FILTER_CHANGES_SLOT;
        };
    }

    private ItemStack createFilterButton(final Filter filter, final boolean active) {
        final var builder = ItemBuilder.of(filter.icon)
                .name(Component.text(filter.label, active ? NamedTextColor.GREEN : NamedTextColor.YELLOW));

        if (active) {
            builder.lore(List.of(Component.text("Active filter", NamedTextColor.GRAY)))
                    .enchant(Enchantment.UNBREAKING, 1)
                    .flags(ItemFlag.HIDE_ENCHANTS);
        } else {
            builder.lore(List.of(Component.text("Click to filter", NamedTextColor.GRAY)));
        }

        return builder.build();
    }

    private ItemStack createLogItem(final SlabbyAPI api, final ShopLog log) {
        final var item = new ItemStack(Material.PAPER);
        final var meta = item.getItemMeta();
        final var lore = new ArrayList<Component>();

        //TODO: use display name
        final var player = Bukkit.getOfflinePlayer(log.uniqueId());
        lore.add(api.messages().log().player(Component.text(player.getName())));

        switch (log.action()) {
            case BUY -> {
                meta.displayName(api.messages().log().buy().title());

                final var data = api.fromJson(log.data(), Transaction.class);

                lore.add(api.messages().log().buy().amount(data.amount()));
                lore.add(api.messages().log().buy().quantity(data.quantity()));
            }
            case SELL -> {
                meta.displayName(api.messages().log().sell().title());

                final var data = api.fromJson(log.data(), Transaction.class);

                lore.add(api.messages().log().sell().amount(data.amount()));
                lore.add(api.messages().log().sell().quantity(data.quantity()));
            }
            case DEPOSIT -> {
                meta.displayName(api.messages().log().deposit().title());
                final var data = api.fromJson(log.data(), ValueChanged.Int.class);
                final var deposited = data.to() - data.from();
                lore.add(api.messages().log().deposit().amount(deposited));
            }
            case WITHDRAW -> {
                meta.displayName(api.messages().log().withdraw().title());
                final var data = api.fromJson(log.data(), ValueChanged.Int.class);
                final var withdrawn = data.from() - data.to();
                lore.add(api.messages().log().withdraw().amount(withdrawn));
            }
            case INVENTORY_LINK_CHANGED -> {
                meta.displayName(api.messages().log().inventoryLinkChanged().title());

                final var data = api.fromJson(log.data(), LocationChanged.class);

                if (data.isRemoved()) {
                    lore.add(api.messages().log().inventoryLinkChanged().removed());
                } else {
                    lore.add(api.messages().log().inventoryLinkChanged().x(data.x()));
                    lore.add(api.messages().log().inventoryLinkChanged().y(data.y()));
                    lore.add(api.messages().log().inventoryLinkChanged().z(data.z()));
                    lore.add(api.messages().log().inventoryLinkChanged().world(data.world()));
                }
            }
            case LOCATION_CHANGED -> {
                meta.displayName(api.messages().log().locationChanged().title());

                final var data = api.fromJson(log.data(), LocationChanged.class);

                lore.add(api.messages().log().locationChanged().x(data.x()));
                lore.add(api.messages().log().locationChanged().y(data.y()));
                lore.add(api.messages().log().locationChanged().z(data.z()));
                lore.add(api.messages().log().locationChanged().world(data.world()));
            }
            case NAME_CHANGED -> {
                meta.displayName(api.messages().log().nameChanged().title());

                final var data = (ValueChanged.String) api.fromJson(log.data(), log.action().dataClass());

                lore.add(api.messages().log().nameChanged().from(data.from()));
                lore.add(api.messages().log().nameChanged().to(data.to()));
            }
            case NOTE_CHANGED -> {
                meta.displayName(api.messages().log().noteChanged().title());

                final var data = (ValueChanged.String) api.fromJson(log.data(), log.action().dataClass());

                lore.add(api.messages().log().noteChanged().from(data.from()));
                lore.add(api.messages().log().noteChanged().to(data.to()));
            }
            case QUANTITY_CHANGED -> {
                meta.displayName(api.messages().log().quantityChanged().title());

                final var data = (ValueChanged.Int) api.fromJson(log.data(), log.action().dataClass());

                lore.add(api.messages().log().quantityChanged().from(data.from()));
                lore.add(api.messages().log().quantityChanged().to(data.to()));
            }
            case SELL_PRICE_CHANGED -> {
                meta.displayName(api.messages().log().sellPriceChanged().title());

                final var data = (ValueChanged.Double) api.fromJson(log.data(), log.action().dataClass());

                lore.add(api.messages().log().sellPriceChanged().from(data.from()));
                lore.add(api.messages().log().sellPriceChanged().to(data.to()));
            }
            case BUY_PRICE_CHANGED -> {
                meta.displayName(api.messages().log().buyPriceChanged().title());

                final var data = (ValueChanged.Double) api.fromJson(log.data(), log.action().dataClass());

                lore.add(api.messages().log().buyPriceChanged().from(data.from()));
                lore.add(api.messages().log().buyPriceChanged().to(data.to()));
            }
            case SHOP_DESTROYED -> {
                meta.displayName(api.messages().log().shopDestroyed().title());
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + log.action());
        }

        lore.add(api.messages().log().date(log.createdOn()));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

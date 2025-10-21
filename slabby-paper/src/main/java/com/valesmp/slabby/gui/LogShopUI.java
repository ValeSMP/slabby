package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.audit.Auditable;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.shop.log.LocationChanged;
import com.valesmp.slabby.shop.log.Transaction;
import com.valesmp.slabby.shop.log.ValueChanged;

import lombok.experimental.UtilityClass;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.hxrry.hxgui.builders.GUIBuilder;
import dev.hxrry.hxgui.builders.ItemBuilder;

import java.util.ArrayList;
import java.util.Comparator;

// TODO: implement pagination into hxgui?
// 1.1>2 changes inc. no pagedgui or invui, addition of temporary manual pagination, recursive reopening, max items per page, prev and next buttons conditionally showing and createdLogItem extracted helper method because pretty

@UtilityClass
public final class LogShopUI {

    //TODO: category menu
    private static final int ITEMS_PER_PAGE = 45;

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop) {
        open(api, shopOwner, shop, 0);
    }

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop, final int page) {
        // get all logs, sort by date with newest first as default
        final var allLogs = shop.logs().stream()
                .sorted(Comparator.comparing(Auditable::createdOn, Comparator.reverseOrder()))
                .toList();

        // calculate pagination
        final int totalPages = (int) Math.ceil((double) allLogs.size() / ITEMS_PER_PAGE);
        final int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        final int startIndex = currentPage * ITEMS_PER_PAGE;
        final int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allLogs.size());

        // get logs for current page
        final var pageLogs = allLogs.subList(startIndex, endIndex);

        // build gui
        final var builder = GUIBuilder.chest()
                .title(api.messages().log().title())
                .rows(6);

        // add log items
        int slot = 0;
        for (var log : pageLogs) {
            builder.item(slot++, createLogItem(api, log));
        }

        // row 6 is nav controls in 45-53
        // slot 48 is prev page
        if (currentPage > 0) {
            builder.item(48, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE)
                    .name(api.messages().general().previousPage())
                    .build(),
                    event -> open(api, shopOwner, shop, currentPage - 1));
        }

        if (currentPage < totalPages - 1) {
            builder.item(50, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE)
                    .name(api.messages().general().nextPage())
                    .build(),
                    event -> open(api, shopOwner, shop, currentPage + 1));
        }

        builder.open(shopOwner);
    }

    private ItemStack createLogItem(final SlabbyAPI api, final com.valesmp.slabby.shop.ShopLog log) {
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

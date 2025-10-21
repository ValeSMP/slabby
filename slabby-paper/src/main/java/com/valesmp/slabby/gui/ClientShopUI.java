package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.Shop;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import org.bukkit.Bukkit;

import dev.hxrry.hxgui.builders.GUIBuilder;
import dev.hxrry.hxgui.builders.ItemBuilder;
import dev.hxrry.hxgui.core.Menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// changes 1.1 > 1.2 removed autoupdate, TODO: reintroduce it
// also removed supplieditem and used regular items with click handlers instead, added conditional button creation into this menu and helper methods because OCD
@UtilityClass
public final class ClientShopUI {

    //temp
    private final Map<UUID, MenuContext> openMenus = new HashMap<>();

    //temp
    private static class MenuContext {
        final Menu menu;

        MenuContext(Menu menu) {
            this.menu = menu;
        }
    }

    public void open(final SlabbyAPI api, final Player client, final Shop shop) {
        final var item = api.serialization().<ItemStack>deserialize(shop.item());
        final var uniqueId = client.getUniqueId();

        if (item.getMaxStackSize() != 1)
            item.setAmount(Math.max(1, Math.min(shop.quantity(), item.getMaxStackSize())));

        final var builder = GUIBuilder.chest()
                .title(api.messages().client().title())
                .rows(1);

        // slot zero; the buy button
        if (shop.buyPrice() != null) {
            
            final var buyLore = new ArrayList<Component>();
            
            buyLore.add(api.messages().client().buy().price(shop.buyPrice()));
            if (shop.stock() != null) {
                buyLore.add(api.messages().client().buy().stock(shop.stock()));
                buyLore.add(api.messages().client().buy().stacks(shop.stock() / item.getMaxStackSize()));
            }

            final var buyButton = ItemBuilder.of(Material.GOLD_INGOT)
                    .name(api.messages().client().buy().title(item.displayName(), shop.quantity()))
                    .build();
            final var buyMeta = buyButton.getItemMeta();
            buyMeta.lore(buyLore);
            buyButton.setItemMeta(buyMeta);

            builder.item(0, buyButton, event -> {
                api.exceptionService().tryCatch(client.getUniqueId(), () -> {
                    api.operations().buy(client.getUniqueId(), shop);
                    refreshBalance(api, client, uniqueId);
                });
            });
        }

        // slot 1; tha sell button
        if (shop.sellPrice() != null) {
            
            final var sellLore = new ArrayList<Component>();
            sellLore.add(api.messages().client().sell().price(shop.sellPrice()));
            if (shop.stock() != null) {
                sellLore.add(api.messages().client().sell().stock(shop.stock()));
                sellLore.add(api.messages().client().sell().stacks(shop.stock() / item.getMaxStackSize()));
            }

            final var sellButton = ItemBuilder.of(Material.IRON_INGOT)
                    .name(api.messages().client().sell().title(item.displayName(), shop.quantity()))
                    .build();
            final var sellMeta = sellButton.getItemMeta();
            sellMeta.lore(sellLore);
            sellButton.setItemMeta(sellMeta);

            builder.item(1, sellButton, event -> {
                api.exceptionService().tryCatch(uniqueId, () -> {
                    api.operations().sell(client.getUniqueId(), shop);
                    refreshBalance(api, client, uniqueId);
                });
            });   
        }

        // slot 4; shop item display thingy
        builder.item(4, item);

        // slot 6; sellers notes if left
        final var noteItem = ItemBuilder.of(Material.NAME_TAG)
                .name(api.messages().client().note().title())
                .build();
        final var noteMeta = noteItem.getItemMeta();
        noteMeta.lore(List.of(Component.text(shop.note(), NamedTextColor.DARK_PURPLE)));
        noteItem.setItemMeta(noteMeta);
        builder.item(6, noteItem);

        //slot 7 current funds display
        final var fundsItem = createFundsItem(api, client);
        builder.item(7, fundsItem);

        // slot 8; command block (shop info)
        builder.item(8, createCommandBlockItem(api, shop, item));

        // open gui
        builder.open(client);
    }

    private void refreshBalance(final SlabbyAPI api, final Player player, final UUID uniqueId) {
        final var context = openMenus.get(uniqueId);
        if (context == null) return;

        final Inventory inv = context.menu.getInventory(player);

        inv.setItem(7, createFundsItem(api, player));
    }

    public void onClose(final UUID uniqueId) {
        openMenus.remove(uniqueId);
    }

    // helper: create funds display item
    private ItemStack createFundsItem(final SlabbyAPI api, final Player client) {
        final var fundsItem = ItemBuilder.of(Material.PAPER)
                .name(api.messages().client().funds().title())
                .build();
        final var fundsMeta = fundsItem.getItemMeta();
        fundsMeta.lore(List.of(api.messages().client().funds().balance(
                api.economy().balance(client.getUniqueId()))));
        fundsItem.setItemMeta(fundsMeta);
        return fundsItem;
    }

    // helper: create command block item
    private ItemStack createCommandBlockItem(final SlabbyAPI api, final Shop shop, final ItemStack itemStack) {
        final var owners = shop.owners()
                .stream()
                .map(o -> Bukkit.getOfflinePlayer(o.uniqueId()).getName())
                .toArray(String[]::new);

        final var lore = new ArrayList<Component>();
        lore.add(api.messages().commandBlock().owners(owners));
        lore.add(api.messages().commandBlock().selling(itemStack.displayName()));

        if (shop.buyPrice() != null) {
            final var buyPriceEach = shop.buyPrice() == 0 ? 0 : shop.buyPrice() / shop.quantity();
            lore.add(api.messages().commandBlock().buyPrice(shop.quantity(), shop.buyPrice(), buyPriceEach));
        }

        if (shop.sellPrice() != null) {
            final var sellPriceEach = shop.sellPrice() == 0 ? 0 : shop.sellPrice() / shop.quantity();
            lore.add(api.messages().commandBlock().sellPrice(shop.quantity(), shop.sellPrice(), sellPriceEach));
        }

        if (shop.stock() == null) {
            lore.add(Component.empty());
            lore.add(Component.text("⚡ ADMIN SHOP - INFINITE STOCK ⚡", NamedTextColor.GOLD));
        }

        final var item = ItemBuilder.of(Material.COMMAND_BLOCK)
                .name(api.messages().commandBlock().title())
                .build();
        final var meta = item.getItemMeta();
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}

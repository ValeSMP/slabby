package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.audit.Auditable;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.shop.ShopWizard;

import dev.hxrry.hxgui.components.Pagination;
import dev.hxrry.hxgui.core.MenuItem;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// REMOVE: InvUI imports (Window, PagedGui, PageItem, AdventureComponentWrapper, Markers)
// NEW: HxGUI imports (ItemBuilder, Pagination, MenuItem)

@UtilityClass
public final class RestoreShopUI {

    public void open(final SlabbyAPI api, final Player viewer, final UUID uniqueId) {
        // Get all deleted shops sorted by creation date (newest first)
        final var deletedShops = api.repository()
                .shopsOf(uniqueId, Shop.State.DELETED)
                .stream()
                .sorted(Comparator.comparing(Auditable::createdOn, Comparator.reverseOrder()))
                .toList();

        // NEW: Create pagination component (6 rows: 5 for content + 1 for navigation)
        final Pagination pagination = new Pagination(api.messages().restore().title(), 6);
        
        // NEW: Configure pagination layout
        pagination.contentArea(0, 44) // Slots 0-44 (5 rows of 9 = 45 slots)
                  .navigationSlots(45, 53, 49); // Previous=45, Next=53, Info=49

        // NEW: Convert shops to MenuItems with click handlers
        final List<MenuItem> shopItems = new ArrayList<>();
        
        for (Shop shop : deletedShops) {
            final var item = api.serialization().<ItemStack>deserialize(shop.item());

            // Set stack size appropriately
            if (item.getMaxStackSize() != 1) {
                item.setAmount(Math.max(1, Math.min(shop.quantity(), item.getMaxStackSize())));
            }

            // Build lore for the shop item
            final var owners = shop.owners()
                    .stream()
                    .map(owner -> Bukkit.getOfflinePlayer(owner.uniqueId()).getName())
                    .toArray(String[]::new);

            final List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            
            if (shop.buyPrice() != null) {
                lore.add(api.messages().restore().buyPrice(shop.buyPrice()));
            }
            
            if (shop.sellPrice() != null) {
                lore.add(api.messages().restore().sellPrice(shop.sellPrice()));
            }
            
            lore.add(api.messages().restore().quantity(shop.quantity()));
            
            if (shop.stock() != null) {
                lore.add(api.messages().restore().stock(shop.stock()));
            }
            
            lore.add(api.messages().restore().note(shop.note()));
            lore.add(api.messages().restore().owners(owners));

            // Apply lore to item
            item.lore(lore);

            // NEW: Create MenuItem with click handler for restoring shop
            final MenuItem menuItem = new MenuItem(item, clickEvent -> {
                // Start restoration wizard
                api.operations()
                        .wizardOf(viewer.getUniqueId(), shop)
                        .state(Shop.State.ACTIVE)
                        .wizardState(ShopWizard.WizardState.AWAITING_LOCATION);

                viewer.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
                viewer.sendMessage(api.messages().restore().message());
            });

            shopItems.add(menuItem);
        }

        // NEW: Add all items to pagination
        pagination.setItems(shopItems);

        // NEW: Open paginated menu
        pagination.open(viewer);
    }

}
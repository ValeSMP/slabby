package com.valesmp.slabby.gui;

import com.valesmp.slabby.Slabby;
import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.exception.PlayerOutOfInventorySpaceException;
import com.valesmp.slabby.exception.PlayerOutOfStockException;
import com.valesmp.slabby.exception.ShopOutOfStockException;
import com.valesmp.slabby.helper.ItemHelper;
import com.valesmp.slabby.permission.SlabbyPermissions;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.shop.ShopWizard;
import com.valesmp.slabby.wrapper.sound.Sounds;
import dev.hxrry.hxgui.builders.GUIBuilder;
import dev.hxrry.hxgui.builders.ItemBuilder;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;


// 1.1 > 1.2 changes: removed supplied items as items are now created on demand with helper methods
// added shift click detection and added permission checks. new scheduled open with same as the original method from Furb
// added new helper methods for clean item creation because OCD

@UtilityClass
public final class OwnerShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop) {
        final var item = api.serialization().<ItemStack>deserialize(shop.item());
        final var uniqueId = shopOwner.getUniqueId();

        if (item.getMaxStackSize() != 1)
            item.setAmount(Math.max(1, Math.min(shop.quantity(), item.getMaxStackSize())));

        final var builder = GUIBuilder.chest()
                .title(api.messages().owner().title())
                .rows(2);

        // row 1: stock management (if shop has stock)
        if (shop.stock() != null) {
            // slot 0: deposit shit
            builder.item(0, createDepositItem(api, shop, item), event -> {
                if (event.isShiftClick()) {
                    final var amount = ItemHelper.countSimilar(shopOwner.getInventory(), item);
                    if (amount <= 0) {
                        api.exceptionService().tryCatch(uniqueId, PlayerOutOfStockException::new);
                        return;
                    }
                    api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, amount));
                } else {
                    final var wizard = api.operations().wizards().get(uniqueId);
                    final var amount = wizard != null ? wizard.quantity() : shop.quantity();
                    api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, amount));
                }
            });

            // slot 1: withdraw
            builder.item(1, createWithdrawItem(api, shop, item), event -> {
                if (event.isShiftClick()) {
                    final var spaceLeft = ItemHelper.getSpace(shopOwner.getInventory(), item);
                    final var amount = Math.min(spaceLeft, shop.stock());

                    if (spaceLeft <= 0) {
                        api.exceptionService().tryCatch(uniqueId, PlayerOutOfInventorySpaceException::new);
                        return;
                    }
                    if (shop.stock() == 0) {
                        api.exceptionService().tryCatch(uniqueId, ShopOutOfStockException::new);
                        return;
                    }
                    api.exceptionService().tryCatch(uniqueId, () -> api.operations().withdraw(uniqueId, shop, amount));
                } else {
                    final var wizard = api.operations().wizards().get(uniqueId);
                    final var amount = wizard != null ? wizard.quantity() : shop.quantity();
                    api.exceptionService().tryCatch(uniqueId, () -> api.operations().withdraw(uniqueId, shop, amount));
                }
            });

            // slot 2: change rate
            builder.item(2, createChangeRateItem(api, uniqueId, shop), event -> {
                final var wizard = api.operations().wizardOf(uniqueId, shop)
                        .wizardState(ShopWizard.WizardState.AWAITING_TEMP_QUANTITY);
                shopOwner.closeInventory();
                shopOwner.sendMessage(api.messages().modify().quantity().request());
                api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
            });
        }

        // slot 4: shop item display
        builder.item(4, item);

        // slot 5: inventory link (if has stock and permission)
        if (shop.stock() != null) {
            api.permission().ifPermission(uniqueId, SlabbyPermissions.SHOP_LINK, () -> {
                builder.item(5, createInventoryLinkItem(api, shop), event -> {
                    if (shop.hasInventory()) {
                        api.exceptionService().tryCatch(uniqueId, () -> api.operations().unlinkShop(uniqueId, shop));
                    } else {
                        api.operations()
                                .wizardOf(uniqueId, shop)
                                .wizardState(ShopWizard.WizardState.AWAITING_INVENTORY_LINK);
                        api.sound().play(uniqueId, shop, Sounds.AWAITING_INPUT);
                        shopOwner.sendMessage(api.messages().owner().inventoryLink().message());
                        shopOwner.closeInventory();
                    }
                });
            });
        }

        // slot 6: command block
        builder.item(6, createCommandBlockItem(api, shop, item));

        // slot 7: modify button
        builder.item(7, ItemBuilder.of(Material.COMPARATOR)
                .name(api.messages().owner().modify().title())
                .build(), event -> {
            ModifyShopUI.open(api, shopOwner, api.operations().wizardOf(uniqueId, shop));
            api.sound().play(uniqueId, shop, Sounds.NAVIGATION);
        });

        // slot 8: view as customer
        builder.item(8, ItemBuilder.of(Material.OAK_SIGN)
                .name(api.messages().owner().customer().title())
                .build(), event -> {
            ClientShopUI.open(api, shopOwner, shop);
            api.sound().play(uniqueId, shop, Sounds.NAVIGATION);
        });

        // slot 9 (row 2, slot 0): logs (if permission)
        api.permission().ifPermission(uniqueId, SlabbyPermissions.SHOP_LOGS, () -> {
            builder.item(9, ItemBuilder.of(Material.BOOK)
                    .name(api.messages().owner().logs().title())
                    .build(), event -> LogShopUI.open(api, shopOwner, shop));
        });

        // schedule open on main thread (same as original)
        Bukkit.getScheduler().runTask((Slabby) api, () -> builder.open(shopOwner));
    }

    // helper: create deposit item
    private ItemStack createDepositItem(final SlabbyAPI api, final Shop shop, final ItemStack item) {
        final var depositItem = ItemBuilder.of(Material.CHEST_MINECART)
                .name(api.messages().owner().deposit().title(item.displayName()))
                .build();
        final var meta = depositItem.getItemMeta();
        meta.lore(List.of(
                api.messages().owner().deposit().bulk(),
                api.messages().owner().stock(shop.stock()),
                api.messages().owner().stacks(shop.stock() / item.getMaxStackSize())
        ));
        depositItem.setItemMeta(meta);
        return depositItem;
    }

    // helper: create withdraw item
    private ItemStack createWithdrawItem(final SlabbyAPI api, final Shop shop, final ItemStack item) {
        final var withdrawItem = ItemBuilder.of(Material.HOPPER_MINECART)
                .name(api.messages().owner().withdraw().title(item.displayName()))
                .build();
        final var meta = withdrawItem.getItemMeta();
        meta.lore(List.of(
                api.messages().owner().withdraw().bulk(),
                api.messages().owner().stock(shop.stock()),
                api.messages().owner().stacks(shop.stock() / item.getMaxStackSize())
        ));
        withdrawItem.setItemMeta(meta);
        return withdrawItem;
    }

    // helper: create change rate item
    private ItemStack createChangeRateItem(final SlabbyAPI api, final java.util.UUID uniqueId, final Shop shop) {
        final var wizard = api.operations().wizards().get(uniqueId);
        final var amount = wizard != null ? wizard.quantity() : shop.quantity();

        final var rateItem = ItemBuilder.of(Material.MINECART)
                .name(api.messages().owner().changeRate().title())
                .build();
        final var meta = rateItem.getItemMeta();
        meta.lore(List.of(api.messages().owner().changeRate().amount(amount)));
        rateItem.setItemMeta(meta);
        return rateItem;
    }

    // helper: create inventory link item
    private ItemStack createInventoryLinkItem(final SlabbyAPI api, final Shop shop) {
        if (shop.hasInventory()) {
            return ItemBuilder.of(Material.ENDER_CHEST)
                    .name(api.messages().owner().inventoryLink().cancel().title())
                    .build();
        } else {
            final var linkItem = ItemBuilder.of(Material.CHEST)
                    .name(api.messages().owner().inventoryLink().title())
                    .build();
            final var meta = linkItem.getItemMeta();
            meta.lore(List.of(api.messages().owner().inventoryLink().description()));
            linkItem.setItemMeta(meta);
            return linkItem;
        }
    }

    // helper: create command block item (reused from previous files)
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

        final var commandBlock = ItemBuilder.of(Material.COMMAND_BLOCK)
                .name(api.messages().commandBlock().title())
                .build();
        final var meta = commandBlock.getItemMeta();
        meta.lore(lore);
        commandBlock.setItemMeta(meta);
        return commandBlock;
    }

}
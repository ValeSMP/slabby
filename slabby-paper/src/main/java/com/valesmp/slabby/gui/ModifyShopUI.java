package com.valesmp.slabby.gui;

import com.valesmp.slabby.Slabby;
import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.ShopWizard;
import com.valesmp.slabby.wrapper.sound.Sounds;

import dev.hxrry.hxgui.builders.GUIBuilder;
import dev.hxrry.hxgui.builders.ItemBuilder;

import lombok.experimental.UtilityClass;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

// 1.1 > 1.2 changes: removed InvUI, added new helper methods for pwettification
// new scheduled open with bukkit.getScheduler().runtask, and reused same component lore workaround until hxgui fix implemented

@UtilityClass
public final class ModifyShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final ShopWizard wizard) {
        final var uniqueId = shopOwner.getUniqueId();
        final var item = api.serialization().<ItemStack>deserialize(wizard.item());

        if (item.getMaxStackSize() != 1)
            item.setAmount(Math.max(1, Math.min(wizard.quantity(), item.getMaxStackSize())));

        final var builder = GUIBuilder.chest()
                .title(api.messages().modify().title())
                .rows(1);

        // slot 0: shop item display
        builder.item(0, item);

        // slot 1: edit note
        builder.item(1, createNoteItem(api, wizard), event -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_NOTE);
            shopOwner.closeInventory();
            shopOwner.sendMessage(api.messages().modify().note().request());
            api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        });

        // slot 2: move shop
        builder.item(2, createMoveItem(api, wizard), event -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_LOCATION);
            shopOwner.closeInventory();
            shopOwner.sendMessage(api.messages().modify().move().message());
            api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        });

        // slot 3: edit buy price
        builder.item(3, createBuyPriceItem(api, wizard), event -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_BUY_PRICE);
            shopOwner.closeInventory();
            shopOwner.sendMessage(api.messages().modify().buy().request());
            api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        });

        // slot 4: edit sell price
        builder.item(4, createSellPriceItem(api, wizard), event -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_SELL_PRICE);
            shopOwner.closeInventory();
            shopOwner.sendMessage(api.messages().modify().sell().request());
            api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        });

        // slot 5: edit quantity
        builder.item(5, createQuantityItem(api, wizard), event -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_QUANTITY);
            shopOwner.closeInventory();
            shopOwner.sendMessage(api.messages().modify().quantity().request());
            api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        });

        // slot 7: confirm/save
        builder.item(7, createConfirmItem(api, wizard), event -> 
            api.exceptionService().tryCatch(uniqueId, () -> {
                api.operations().createOrUpdateShop(shopOwner.getUniqueId(), wizard);
                api.operations().wizards().remove(shopOwner.getUniqueId());
                shopOwner.closeInventory();
                api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.MODIFY_SUCCESS);
            })
        );

        // slot 8: cancel
        builder.item(8, ItemBuilder.of(Material.BARRIER)
                .name(api.messages().modify().cancel().title())
                .build(), event -> {
            api.operations().wizards().remove(shopOwner.getUniqueId());
            shopOwner.closeInventory();
            api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.CANCEL);
        });

        // schedule open on main thread
        Bukkit.getScheduler().runTask((Slabby) api, () -> builder.open(shopOwner));
    }

    // helper: create note item
    private ItemStack createNoteItem(final SlabbyAPI api, final ShopWizard wizard) {
        final var noteItem = ItemBuilder.of(Material.NAME_TAG)
                .name(api.messages().modify().note().title())
                .build();
        final var meta = noteItem.getItemMeta();
        meta.lore(List.of(Component.text(wizard.note(), NamedTextColor.DARK_PURPLE)));
        noteItem.setItemMeta(meta);
        return noteItem;
    }

    // celper: create move item
    private ItemStack createMoveItem(final SlabbyAPI api, final ShopWizard wizard) {
        final var moveItem = ItemBuilder.of(Material.ENDER_PEARL)
                .name(api.messages().modify().move().title())
                .build();
        final var meta = moveItem.getItemMeta();
        meta.lore(List.of(api.messages().modify().move().location(
                wizard.x(), wizard.y(), wizard.z(), wizard.world())));
        moveItem.setItemMeta(meta);
        return moveItem;
    }

    // helper: create buy price item
    private ItemStack createBuyPriceItem(final SlabbyAPI api, final ShopWizard wizard) {
        final var buyPrice = wizard.buyPrice() == null ? -1d : wizard.buyPrice();
        final var buyItem = ItemBuilder.of(Material.GREEN_STAINED_GLASS_PANE)
                .name(api.messages().modify().buy().title())
                .build();
        final var meta = buyItem.getItemMeta();
        meta.lore(List.of(
                api.messages().modify().buy().amount(buyPrice),
                api.messages().modify().clickToSet(),
                api.messages().modify().buy().notForSale()
        ));
        buyItem.setItemMeta(meta);
        return buyItem;
    }

    // helper: create sell price item
    private ItemStack createSellPriceItem(final SlabbyAPI api, final ShopWizard wizard) {
        final var sellPrice = wizard.sellPrice() == null ? -1d : wizard.sellPrice();
        final var sellItem = ItemBuilder.of(Material.RED_STAINED_GLASS_PANE)
                .name(api.messages().modify().sell().title())
                .build();
        final var meta = sellItem.getItemMeta();
        meta.lore(List.of(
                api.messages().modify().sell().amount(sellPrice),
                api.messages().modify().clickToSet(),
                api.messages().modify().sell().notBuying()
        ));
        sellItem.setItemMeta(meta);
        return sellItem;
    }

    // helper: create quantity item
    private ItemStack createQuantityItem(final SlabbyAPI api, final ShopWizard wizard) {
        final var quantityItem = ItemBuilder.of(Material.YELLOW_STAINED_GLASS_PANE)
                .name(api.messages().modify().quantity().title())
                .build();
        final var meta = quantityItem.getItemMeta();
        meta.lore(List.of(
                api.messages().modify().quantity().amount(wizard.quantity()),
                api.messages().modify().clickToSet(),
                api.messages().modify().quantity().description()
        ));
        quantityItem.setItemMeta(meta);
        return quantityItem;
    }

    // helper: create confirm item
    private ItemStack createConfirmItem(final SlabbyAPI api, final ShopWizard wizard) {
        final var confirmItem = ItemBuilder.of(Material.NETHER_STAR)
                .name(api.messages().modify().confirm().title())
                .build();
        final var meta = confirmItem.getItemMeta();
        meta.lore(List.of(
                api.messages().modify().confirm().description(),
                api.messages().modify().confirm().location(wizard.world(), wizard.x(), wizard.y(), wizard.z())
        ));
        confirmItem.setItemMeta(meta);
        return confirmItem;
    }

}
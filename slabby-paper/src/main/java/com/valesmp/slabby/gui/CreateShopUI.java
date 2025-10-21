package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.ShopWizard;

import lombok.experimental.UtilityClass;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import dev.hxrry.hxgui.builders.GUIBuilder;
import dev.hxrry.hxgui.builders.ItemBuilder;

// 1.1 > 1.2 changes, complete rework of shopui to fit in line with HxGUI
// removed all invui imports, added GUIBuilder and ItemBuilder from HxGUI, now uses direct .openPlayer instead of window building :slay:
// made it look pwetty

@UtilityClass
public final class CreateShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final Block block) {
        // Build and open GUI using HxGUI
        GUIBuilder.chest()
                .title(api.messages().create().title()) // HxGUI supports Adventure Components directly!
                .rows(1)
                .item(0, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(1, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(2, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(3, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(4, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(5, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(6, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(7, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .item(8, ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).build())
                .open(shopOwner);

        // Set wizard state
        api.operations().wizard(shopOwner.getUniqueId())
                .wizardState(ShopWizard.WizardState.AWAITING_ITEM)
                .location(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

}

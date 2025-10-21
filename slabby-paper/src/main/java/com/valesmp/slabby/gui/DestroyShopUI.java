package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.Shop;
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

// 1.1 > 1.2 changes; removed invui imports, advcompwrapper, and itemStack, added in the chest GUI builder from hxgui and inline command block creation
// also added helper method because OCD

@UtilityClass
public final class DestroyShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop) {
        final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());
        final var uniqueId = shopOwner.getUniqueId();

        // build confirm button
        final var confirmButton = ItemBuilder.of(Material.GREEN_STAINED_GLASS_PANE)
                .name(api.messages().destroy().confirm().title())
                .build();
        // manually set lore with a component
        final var confirmMeta = confirmButton.getItemMeta();
        confirmMeta.lore(List.of(api.messages().destroy().confirm().description()));
        confirmButton.setItemMeta(confirmMeta);

        // build cancel button
        final var cancelButton = ItemBuilder.of(Material.BARRIER)
                .name(api.messages().destroy().cancel().title())
                .build();

        // build GUI with HxGUI
        GUIBuilder.chest()
                .title(api.messages().destroy().title()) // HxGUI supports adventure components because i'm cool
                .rows(1)
                // slot 3: confirm button
                .item(3, confirmButton, event -> api.exceptionService().tryCatch(uniqueId, () -> {
                    api.operations().removeShop(uniqueId, shop);
                    shopOwner.closeInventory();
                }))
                // slot 4: shop info
                .item(4, createCommandBlockItem(api, shop, itemStack))
                // slot 5: cancel button (barrier)
                .item(5, cancelButton, event -> {
                    shopOwner.closeInventory();
                    api.sound().play(shopOwner.getUniqueId(), shop, Sounds.CANCEL);
                })
                .open(shopOwner);
    }

    // helper method to create command block item
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

        final var item = ItemBuilder.of(Material.COMMAND_BLOCK)
                .name(api.messages().commandBlock().title())
                .build();
        
        // manually set lore with components until HxGUI improved
        final var meta = item.getItemMeta();
        meta.lore(lore);
        item.setItemMeta(meta);
        
        return item;
    }

}
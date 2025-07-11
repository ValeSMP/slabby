package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.wrapper.sound.Sounds;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import static com.valesmp.slabby.gui.GuiHelper.*;

import java.util.ArrayList;

@UtilityClass
public final class DestroyShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop) {
        final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());
        final var uniqueId = shopOwner.getUniqueId();

        final var gui = Gui.empty(9, 1);

        gui.setItem(3, 0, new SimpleItem(itemStack(Material.GREEN_STAINED_GLASS_PANE, (it, meta) -> {
            meta.displayName(api.messages().destroy().confirm().title());
            meta.lore(new ArrayList<>() {{
                add(api.messages().destroy().confirm().description());
            }});
        }).get(), c -> api.exceptionService().tryCatch(uniqueId, () -> {
            api.operations().removeShop(uniqueId, shop);
            gui.closeForAllViewers();
        })));

        gui.setItem(4, 0, commandBlock(api, shop, itemStack));

        gui.setItem(5, 0, new SimpleItem(itemStack(Material.BARRIER, (it, meta) -> {
            meta.displayName(api.messages().destroy().cancel().title());
        }).get(), c -> {
            gui.closeForAllViewers();
            api.sound().play(shopOwner.getUniqueId(), shop, Sounds.CANCEL);
        }));

        final var window = Window.single()
                .setViewer(shopOwner)
                .setTitle(new AdventureComponentWrapper(api.messages().destroy().title()))
                .setGui(gui)
                .build();

        window.open();
    }

}

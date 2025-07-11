package com.valesmp.slabby.gui;

import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.shop.ShopWizard;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

@UtilityClass
public final class CreateShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final Block block) {
        final var gui = Gui.normal()
                .setStructure(".........")
                .addIngredient('.', new SimpleItem(new ItemBuilder(Material.RED_STAINED_GLASS_PANE)))
                .build();

        final var window = Window.single()
                .setViewer(shopOwner)
                .setTitle(new AdventureComponentWrapper(api.messages().create().title()))
                .setGui(gui)
                .build();

        window.open();

        api.operations().wizard(shopOwner.getUniqueId())
                .wizardState(ShopWizard.WizardState.AWAITING_ITEM)
                .location(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

}

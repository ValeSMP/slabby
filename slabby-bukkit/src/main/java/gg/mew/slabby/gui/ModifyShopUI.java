package gg.mew.slabby.gui;

import gg.mew.slabby.Slabby;
import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.exception.SlabbyException;
import gg.mew.slabby.helper.ItemHelper;
import gg.mew.slabby.shop.ShopWizard;
import gg.mew.slabby.wrapper.sound.Sounds;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import static gg.mew.slabby.gui.GuiHelper.*;

import java.util.ArrayList;

@UtilityClass
public final class ModifyShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final ShopWizard wizard) {
        final var uniqueId = shopOwner.getUniqueId();
        final var gui = Gui.empty(9, 1);

        final var item = api.serialization().<ItemStack>deserialize(wizard.item());

        if (item.getMaxStackSize() != 1)
            item.setAmount(wizard.quantity());

        gui.setItem(0, 0, new SimpleItem(item));
        gui.setItem(1, 0, new SimpleItem(itemStack(Material.NAME_TAG, (it, meta) -> {
            meta.displayName(api.messages().modify().note().title());
            meta.lore(new ArrayList<>() {{
                add(Component.text(wizard.note(), NamedTextColor.DARK_PURPLE));
            }});
        }).get(), c -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_NOTE);
            gui.closeForAllViewers();
            shopOwner.sendMessage(api.messages().modify().note().request());
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        }));

        gui.setItem(2, 0, new SimpleItem(itemStack(Material.ENDER_PEARL, (it, meta) -> {
            meta.displayName(api.messages().modify().move().title());
            meta.lore(new ArrayList<>() {{
                add(api.messages().modify().move().location(wizard.x(), wizard.y(), wizard.z(), wizard.world()));
            }});
        }).get(), c -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_LOCATION);
            gui.closeForAllViewers();
            shopOwner.sendMessage(api.messages().modify().move().message());
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        }));

        gui.setItem(3, 0, new SimpleItem(itemStack(Material.GREEN_STAINED_GLASS_PANE, (it, meta) -> {
            meta.displayName(api.messages().modify().buy().title());
            meta.lore(new ArrayList<>() {{
                final var buyPrice = wizard.buyPrice() == null ? -1d : wizard.buyPrice();
                add(api.messages().modify().buy().amount(buyPrice));
                add(api.messages().modify().clickToSet());
                add(api.messages().modify().buy().notForSale());
            }});
        }).get(), c -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_BUY_PRICE);
            gui.closeForAllViewers();
            shopOwner.sendMessage(api.messages().modify().buy().request());
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        }));

        gui.setItem(4, 0, new SimpleItem(itemStack(Material.RED_STAINED_GLASS_PANE, (it, meta) -> {
            meta.displayName(api.messages().modify().sell().title());
            meta.lore(new ArrayList<>() {{
                final var sellPrice = wizard.sellPrice() == null ? -1d : wizard.sellPrice();
                add(api.messages().modify().sell().amount(sellPrice));
                add(api.messages().modify().clickToSet());
                add(api.messages().modify().sell().notBuying());
            }});
        }).get(), c -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_SELL_PRICE);
            gui.closeForAllViewers();
            shopOwner.sendMessage(api.messages().modify().sell().request());
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        }));

        gui.setItem(5, 0, new SimpleItem(itemStack(Material.YELLOW_STAINED_GLASS_PANE, (it, meta) -> {
            meta.displayName(api.messages().modify().quantity().title());
            meta.lore(new ArrayList<>() {{
                add(api.messages().modify().quantity().amount(wizard.quantity()));
                add(api.messages().modify().clickToSet());
                add(api.messages().modify().quantity().description());
            }});
        }).get(), c -> {
            wizard.wizardState(ShopWizard.WizardState.AWAITING_QUANTITY);
            gui.closeForAllViewers();
            shopOwner.sendMessage(api.messages().modify().quantity().request());
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);
        }));

        gui.setItem(7, 0, new SimpleItem(itemStack(Material.NETHER_STAR, (it, meta) -> {
            meta.displayName(api.messages().modify().confirm().title());
            meta.lore(new ArrayList<>() {{
                add(api.messages().modify().confirm().description());
                add(api.messages().modify().confirm().location(wizard.world(), wizard.x(), wizard.y(), wizard.z()));
            }});
        }).get(), c -> api.exceptionService().tryCatch(uniqueId, () -> {
            api.operations().createOrUpdateShop(shopOwner.getUniqueId(), wizard);
            api.operations().wizards().remove(shopOwner.getUniqueId());
            gui.closeForAllViewers();
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.MODIFY_SUCCESS);
        })));

        gui.setItem(8, 0, new SimpleItem(itemStack(Material.BARRIER, (it, meta) -> {
            meta.displayName(api.messages().modify().cancel().title());
        }).get(), c -> {
            api.operations().wizards().remove(shopOwner.getUniqueId());
            gui.closeForAllViewers();
            api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.CANCEL);
        }));

        final var window = Window.single()
                .setViewer(shopOwner)
                .setTitle(new AdventureComponentWrapper(api.messages().modify().title()))
                .setGui(gui)
                .build();

        Bukkit.getScheduler().runTask((Slabby)api, window::open);
    }

}

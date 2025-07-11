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
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.SuppliedItem;
import xyz.xenondevs.invui.window.Window;

import static com.valesmp.slabby.gui.GuiHelper.*;

import java.util.ArrayList;

@UtilityClass
public final class OwnerShopUI {

    public void open(final SlabbyAPI api, final Player shopOwner, final Shop shop) {
        final var item = api.serialization().<ItemStack>deserialize(shop.item());
        final var uniqueId = shopOwner.getUniqueId();

        if (item.getMaxStackSize() != 1)
            item.setAmount(Math.max(1, Math.min(shop.quantity(), item.getMaxStackSize())));

        final var gui = Gui.empty(9, 2);

        if (shop.stock() != null) {
            gui.setItem(0, 0, new SuppliedItem(itemStack(Material.CHEST_MINECART, (it, meta) -> {
                meta.displayName(api.messages().owner().deposit().title(item.displayName()));
                meta.lore(new ArrayList<>() {{
                    add(api.messages().owner().deposit().bulk());
                    add(api.messages().owner().stock(shop.stock()));
                    add(api.messages().owner().stacks(shop.stock() / item.getMaxStackSize()));
                }});
            }), c -> {
                if (c.getEvent().isShiftClick()) {
                    final var amount = ItemHelper.countSimilar(shopOwner.getInventory(), item);

                    if (amount <= 0)
                        return api.exceptionService().tryCatch(uniqueId, () -> {
                            throw new PlayerOutOfStockException();
                        });

                    return api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, amount));
                }

                final var wizard = api.operations().wizards().get(uniqueId);
                final var amount = wizard != null ? wizard.quantity() : shop.quantity();

                return api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, amount));
            }));

            gui.setItem(1, 0, new SuppliedItem(itemStack(Material.HOPPER_MINECART, (it, meta) -> {
                meta.displayName(api.messages().owner().withdraw().title(item.displayName()));
                meta.lore(new ArrayList<>() {{
                    add(api.messages().owner().withdraw().bulk());
                    add(api.messages().owner().stock(shop.stock()));
                    add(api.messages().owner().stacks(shop.stock() / item.getMaxStackSize()));
                }});
            }), c -> {
                if (c.getEvent().isShiftClick()) {
                    final var spaceLeft = ItemHelper.getSpace(shopOwner.getInventory(), item);
                    final var amount = Math.min(spaceLeft, shop.stock());

                    if (spaceLeft <= 0)
                        return api.exceptionService().tryCatch(uniqueId, () -> {
                            throw new PlayerOutOfInventorySpaceException();
                        });

                    if (shop.stock() == 0)
                        return api.exceptionService().tryCatch(uniqueId, () -> {
                           throw new ShopOutOfStockException();
                        });

                    return api.exceptionService().tryCatch(uniqueId, () -> api.operations().withdraw(uniqueId, shop, amount));
                }

                final var wizard = api.operations().wizards().get(uniqueId);
                final var amount = wizard != null ? wizard.quantity() : shop.quantity();

                return api.exceptionService().tryCatch(uniqueId, () -> api.operations().withdraw(uniqueId, shop, amount));
            }));

            gui.setItem(2, 0, new SuppliedItem(itemStack(Material.MINECART, (it, meta) -> {
                meta.displayName(api.messages().owner().changeRate().title());
                meta.lore(new ArrayList<>() {{
                    final var wizard = api.operations().wizards().get(uniqueId);
                    final var amount = wizard != null ? wizard.quantity() : shop.quantity();
                    add(api.messages().owner().changeRate().amount(amount));
                }});
            }), c -> {
                final var wizard = api.operations().wizardOf(uniqueId, shop)
                        .wizardState(ShopWizard.WizardState.AWAITING_TEMP_QUANTITY);

                gui.closeForAllViewers();
                shopOwner.sendMessage(api.messages().modify().quantity().request());
                api.sound().play(shopOwner.getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.AWAITING_INPUT);

                return false;
            }));
        }

        api.permission().ifPermission(uniqueId, SlabbyPermissions.SHOP_LOGS, () -> {
            gui.setItem(0, 1, new SimpleItem(GuiHelper.itemStack(Material.BOOK, (it, meta) -> {
                meta.displayName(api.messages().owner().logs().title());
            }).get(), c -> LogShopUI.open(api, shopOwner, shop)));
        });

        gui.setItem(4, 0, new SimpleItem(item));

        api.permission().ifPermission(uniqueId, SlabbyPermissions.SHOP_LINK, () -> {
            if (shop.stock() == null)
                return;

            gui.setItem(5, 0, new SuppliedItem(() -> {
                if (shop.hasInventory()) {
                    return itemStack(Material.ENDER_CHEST, (it, meta) -> {
                        meta.displayName(api.messages().owner().inventoryLink().cancel().title());
                    }).get();
                } else {
                    return itemStack(Material.CHEST, (it, meta) -> {
                        meta.displayName(api.messages().owner().inventoryLink().title());
                        meta.lore(new ArrayList<>() {{
                            add(api.messages().owner().inventoryLink().description());
                        }});
                    }).get();
                }
            }, c -> {
                if (shop.hasInventory()) {
                    return api.exceptionService().tryCatch(uniqueId, () -> api.operations().unlinkShop(uniqueId, shop));
                }

                api.operations()
                        .wizardOf(uniqueId, shop)
                        .wizardState(ShopWizard.WizardState.AWAITING_INVENTORY_LINK);

                api.sound().play(uniqueId, shop, Sounds.AWAITING_INPUT);
                shopOwner.sendMessage(api.messages().owner().inventoryLink().message());
                gui.closeForAllViewers();

                return true;
            }));
        });

        gui.setItem(6, 0, commandBlock(api, shop, item));

        gui.setItem(7, 0, new SimpleItem(itemStack(Material.COMPARATOR, (it, meta) -> {
            meta.displayName(api.messages().owner().modify().title());
        }).get(), c -> {
            ModifyShopUI.open(api, shopOwner, api.operations().wizardOf(uniqueId, shop));
            api.sound().play(uniqueId, shop, Sounds.NAVIGATION);
        }));

        gui.setItem(8, 0, new SimpleItem(itemStack(Material.OAK_SIGN, (it, meta) -> {
            meta.displayName(api.messages().owner().customer().title());
        }).get(), c -> {
            ClientShopUI.open(api, shopOwner, shop);
            api.sound().play(uniqueId, shop, Sounds.NAVIGATION);
        }));

        final var window = Window.single()
                .setViewer(shopOwner)
                .setTitle(new AdventureComponentWrapper(api.messages().owner().title()))
                .setGui(gui)
                .build();

        Bukkit.getScheduler().runTask((Slabby)api, window::open);
    }

}

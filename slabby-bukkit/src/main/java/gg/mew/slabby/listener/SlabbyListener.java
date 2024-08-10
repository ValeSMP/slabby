package gg.mew.slabby.listener;

import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.exception.FaultException;
import gg.mew.slabby.exception.SlabbyException;
import gg.mew.slabby.gui.*;
import gg.mew.slabby.helper.BlockHelper;
import gg.mew.slabby.helper.ItemHelper;
import gg.mew.slabby.permission.SlabbyPermissions;
import gg.mew.slabby.shop.Shop;
import gg.mew.slabby.shop.ShopWizard;
import gg.mew.slabby.wrapper.sound.Sounds;
import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public final class SlabbyListener implements Listener {

    private final SlabbyAPI api;

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerInteract(final PlayerInteractEvent event) {
        api.permission().ifPermission(event.getPlayer().getUniqueId(), SlabbyPermissions.SHOP_INTERACT, () -> handlePlayerInteract(event));
    }

    private void handlePlayerInteract(PlayerInteractEvent event) {
        final var block = event.getClickedBlock();
        final var player = event.getPlayer();

        if (block == null || block.getType() == Material.AIR || event.getHand() != EquipmentSlot.HAND)
            return;

        final var uniqueId = player.getUniqueId();

        final int blockX = block.getX();
        final int blockY = block.getY();
        final int blockZ = block.getZ();
        final String blockWorld = block.getWorld().getName();

        final var shopOpt = new AtomicReference<Optional<Shop>>();

        if (!api.exceptionService().tryCatch(uniqueId, () -> shopOpt.set(api.repository().shopAt(blockX, blockY, blockZ, blockWorld))))
            return;

        final var configurationItem = Bukkit.getItemFactory().createItemStack(api.configuration().item());
        final var hasConfigurationItem = event.getItem() != null && event.getItem().isSimilar(configurationItem);

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK -> {
                shopOpt.get().ifPresentOrElse(shop -> {
                    if (shop.isOwner(uniqueId) || api.isAdminMode(uniqueId)) {
                        if (hasConfigurationItem) {
                            DestroyShopUI.open(api, player, shop);
                        } else {
                            OwnerShopUI.open(api, player, shop);
                        }
                    } else {
                        ClientShopUI.open(api, player, shop);
                    }
                }, () -> {
                    final var canAccessClaim = api.claim() == null ||
                            api.claim().canCreateShop(uniqueId,
                                    blockX,
                                    blockY,
                                    blockZ,
                                    event.getClickedBlock().getWorld().getName());

                    if (canAccessClaim && hasConfigurationItem && BlockHelper.isShopAllowed(event.getClickedBlock())) {
                        api.operations().ifWizardOrElse(uniqueId, w -> {
                            if (w.wizardState() == ShopWizard.WizardState.AWAITING_LOCATION) {
                                w.location(blockX, blockY, blockZ, blockWorld);
                                w.wizardState(ShopWizard.WizardState.AWAITING_CONFIRMATION);
                                api.sound().play(uniqueId, w.x(), w.y(), w.z(), w.world(), Sounds.MODIFY_SUCCESS);
                                ModifyShopUI.open(api, player, w);
                            }
                        }, () -> api.permission().ifPermission(uniqueId, SlabbyPermissions.SHOP_MODIFY, () -> CreateShopUI.open(api, player, block)));
                    }
                });
            }
            case LEFT_CLICK_BLOCK -> {
                shopOpt.get().ifPresent(shop -> {
                    if (!shop.isOwner(uniqueId) && !api.isAdminMode(uniqueId) || shop.stock() == null)
                        return;

                    if (api.configuration().restock().punch().enabled()) {
                        if (api.configuration().restock().punch().shulker() && event.getItem() != null && event.getItem().getType() == Material.SHULKER_BOX) {
                            api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, 1));
                        } else if (api.configuration().restock().punch().bulk())  {
                            final var item = api.serialization().<ItemStack>deserialize(shop.item());

                            final var quantity = player.isSneaking()
                                    ? ItemHelper.countSimilar(player.getInventory(), item)
                                    : shop.quantity();

                            if (quantity > 0)
                                api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, quantity));
                        }
                    }
                });

                api.operations().ifWizard(uniqueId, wizard -> {
                    if (wizard.wizardState() == ShopWizard.WizardState.AWAITING_INVENTORY_LINK) {
                        if (player.isSneaking() && BlockHelper.isInventoryAllowed(event.getClickedBlock())) {
                            api.operations().linkShop(uniqueId, wizard, block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
                        }
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onInventoryClick(final InventoryClickEvent event) {
        api.operations().ifWizard(event.getWhoClicked().getUniqueId(), wizard -> {
            if (wizard.wizardState() == ShopWizard.WizardState.AWAITING_ITEM) {
                final var item = event.getCurrentItem();

                if (item == null)
                    return;

                wizard.wizardState(ShopWizard.WizardState.AWAITING_CONFIRMATION)
                        .item(api.serialization().serialize(item.clone()))
                        .quantity(item.getMaxStackSize());

                ModifyShopUI.open(api, (Player) event.getWhoClicked(), wizard);

                event.setCancelled(true);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onInventoryClose(final InventoryCloseEvent event) {
        if (event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
            api.operations().ifWizard(event.getPlayer().getUniqueId(), wizard -> {
                if (wizard.wizardState() == ShopWizard.WizardState.AWAITING_CONFIRMATION
                        || wizard.wizardState() == ShopWizard.WizardState.AWAITING_ITEM
                        || wizard.wizardState() == null)
                    api.operations().wizards().remove(event.getPlayer().getUniqueId()); //HOTFIX: wizard can get into an invalid state where state is null
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onChatMessage(final AsyncChatEvent event) {
        api.operations().ifWizard(event.getPlayer().getUniqueId(), wizard -> {
            if (wizard.wizardState() == null || !wizard.wizardState().awaitingTextInput())
                return;

            final var serializer = PlainTextComponentSerializer.plainText();
            final var text = serializer.serialize(event.message());

            try {
                switch (wizard.wizardState()) {
                    case AWAITING_NOTE -> wizard.note(text);
                    case AWAITING_BUY_PRICE -> {
                        final var buyPrice = getAndCheckPrice(Double.parseDouble(text));
                        wizard.buyPrice(buyPrice == -1 ? null : buyPrice);
                    }
                    case AWAITING_SELL_PRICE -> {
                        final var sellPrice = getAndCheckPrice(Double.parseDouble(text));
                        wizard.sellPrice(sellPrice == -1 ? null : sellPrice);
                    }
                    case AWAITING_QUANTITY, AWAITING_TEMP_QUANTITY -> {
                        final var quantity = Integer.parseInt(text);
                        final var item = api.serialization().<ItemStack>deserialize(wizard.item());

                        final var maxQuantity = 36 * item.getMaxStackSize();

                        if (quantity < 1 || quantity > maxQuantity)
                            throw new FaultException(api.messages().modify().quantity().minMax(maxQuantity));

                        wizard.quantity(quantity);
                    }
                }

                api.sound().play(event.getPlayer().getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.MODIFY_SUCCESS);
            } catch (final NumberFormatException e) {
                event.getPlayer().sendMessage(api.messages().modify().invalidNumber());
                api.sound().play(event.getPlayer().getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.BLOCKED);
            } catch (final FaultException e) {
                event.getPlayer().sendMessage(e.component());
                api.sound().play(event.getPlayer().getUniqueId(), wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.BLOCKED);
            }

            if (wizard.wizardState() == ShopWizard.WizardState.AWAITING_TEMP_QUANTITY)
                OwnerShopUI.open(api, event.getPlayer(), wizard.shop());
            else
                ModifyShopUI.open(api, event.getPlayer(), wizard);

            wizard.wizardState(ShopWizard.WizardState.AWAITING_CONFIRMATION);

            event.setCancelled(true);
        });
    }

    private double getAndCheckPrice(final double price) {
        final var check = BigDecimal.valueOf(price);

        if (check.scale() > 2)
            throw new FaultException(api.messages().modify().decimalPlaces());

        if (price < 0 && price != -1)
            throw new FaultException(api.messages().modify().minimumPrice());

        return price;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onInventoryMoveItem(final InventoryMoveItemEvent event) {
        final var restock = api.configuration().restock();

        if (!restock.chests().enabled() || !restock.chests().hoppers().enabled())
            return;

        final var location = event.getDestination().getLocation();

        if (location == null || event.getDestination().getType() != InventoryType.CHEST)
            return;

        Optional<Shop> shopOpt = Optional.empty();

        try {
            shopOpt = api.repository().shopWithInventoryAt(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
        } catch (final SlabbyException ignored) {}

        shopOpt.ifPresent(shop -> {
            final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());

            if (!itemStack.isSimilar(event.getItem()))
                return;

            shop.stock(shop.stock() + event.getItem().getAmount());

            try {
                api.repository().update(shop);

                event.setItem(ItemStack.empty());
            } catch (final SlabbyException e) {
                api.exceptionService().logToConsole("Error while attempting to update shop from linked inventory", e);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEntityChangeBlock(final EntityChangeBlockEvent event) {
        if (!BlockHelper.isSlabbyBlock(event.getBlock()))
            return;

        final var x = event.getBlock().getX();
        final var y = event.getBlock().getY();
        final var z = event.getBlock().getZ();
        final var world = event.getBlock().getWorld().getName();

        if (api.repository().isShopOrInventory(x, y, z, world))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockBreak(final BlockBreakEvent event) {
        if (!BlockHelper.isSlabbyBlock(event.getBlock()))
            return;

        final var x = event.getBlock().getX();
        final var y = event.getBlock().getY();
        final var z = event.getBlock().getZ();
        final var world = event.getBlock().getWorld().getName();

        if (api.repository().isShopOrInventory(x, y, z, world))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockBurn(final BlockBurnEvent event) {
        if (!BlockHelper.isSlabbyBlock(event.getBlock()))
            return;

        final var x = event.getBlock().getX();
        final var y = event.getBlock().getY();
        final var z = event.getBlock().getZ();
        final var world = event.getBlock().getWorld().getName();

        if (api.repository().isShopOrInventory(x, y, z, world))
            event.setCancelled(true);
    }

    //TODO: N+1 problem. I should just use a query here

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockExplode(final BlockExplodeEvent event) {
        event.blockList().removeIf(block -> {
            final var x = block.getX();
            final var y = block.getY();
            final var z = block.getZ();
            final var world = block.getWorld().getName();

            return BlockHelper.isSlabbyBlock(block) && api.repository().isShopOrInventory(x, y, z, world);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEntityExplode(final EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            final var x = block.getX();
            final var y = block.getY();
            final var z = block.getZ();
            final var world = block.getWorld().getName();

            return BlockHelper.isSlabbyBlock(block) && api.repository().isShopOrInventory(x, y, z, world);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockPistonExtend(final BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> {
            final var x = block.getX();
            final var y = block.getY();
            final var z = block.getZ();
            final var world = block.getWorld().getName();

            return BlockHelper.isSlabbyBlock(block) && api.repository().isShopOrInventory(x, y, z, world);
        })) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockPistonRetract(final BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> {
            final var x = block.getX();
            final var y = block.getY();
            final var z = block.getZ();
            final var world = block.getWorld().getName();

            return BlockHelper.isSlabbyBlock(block) && api.repository().isShopOrInventory(x, y, z, world);
        })) {
            event.setCancelled(true);
        }
    }

}

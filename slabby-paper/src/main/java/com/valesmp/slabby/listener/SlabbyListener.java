package com.valesmp.slabby.listener;

import com.valesmp.slabby.Slabby;
import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.exception.FaultException;
import com.valesmp.slabby.exception.SlabbyException;
import com.valesmp.slabby.gui.*;
import com.valesmp.slabby.helper.BlockHelper;
import com.valesmp.slabby.helper.ItemHelper;
import com.valesmp.slabby.permission.SlabbyPermissions;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.shop.ShopWizard;
import com.valesmp.slabby.wrapper.sound.Sounds;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.format.NamedTextColor;
import static net.kyori.adventure.text.Component.text;


import java.math.BigDecimal;
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

        if (!BlockHelper.isSlabbyBlock(block))
            return;

        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK -> onRightClick(event, player, block);
            case LEFT_CLICK_BLOCK -> onLeftClick(event, player, block);
        }
    }

    private void onLeftClick(final PlayerInteractEvent event, final Player player, final Block block) {
        final var uniqueId = player.getUniqueId();

        final var blockX = block.getX();
        final var blockY = block.getY();
        final var blockZ = block.getZ();
        final var blockWorld = block.getWorld().getName();

        if (BlockHelper.isShopAllowed(block)) {
            final var shopOpt = new AtomicReference<Optional<Shop>>();

            if (!api.exceptionService().tryCatch(uniqueId, () -> shopOpt.set(api.repository().shopAt(blockX, blockY, blockZ, blockWorld))))
                return;

            shopOpt.get().ifPresent(shop -> {
                if (!shop.isOwner(uniqueId) && !api.isAdminMode(uniqueId) || shop.stock() == null)
                    return;

                if (api.configuration().restock().punch().enabled()) {
                    if (api.configuration().restock().punch().shulker() && event.getItem() != null && event.getItem().getType() == Material.SHULKER_BOX) {
                        api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, 1));
                    } else if (api.configuration().restock().punch().bulk())  {
                        final var item = api.serialization().<ItemStack>deserialize(shop.item());

                        final var quantity = player.isSneaking() ? ItemHelper.countSimilar(player.getInventory(), item) : shop.quantity();

                        if (quantity > 0)
                            api.exceptionService().tryCatch(uniqueId, () -> api.operations().deposit(uniqueId, shop, quantity));
                    }
                }
            });
        } else if (BlockHelper.isInventoryAllowed(block)) {
            api.operations().ifWizard(uniqueId, wizard -> {
                player.sendMessage(text("[DEBUG] Wizard state: " + wizard.wizardState(), NamedTextColor.AQUA));
                
                if (wizard.wizardState() == ShopWizard.WizardState.AWAITING_INVENTORY_LINK) {
                    if (player.isSneaking()) {

                        player.sendMessage(text("[DEBUG] Attempting to link chest at: " + blockX + ", " + blockY + ", " + blockZ, NamedTextColor.YELLOW));

                        api.operations().linkShop(uniqueId, wizard, blockX, blockY, blockZ, blockWorld);
                        
                        player.sendMessage(text("[Slabby] ", NamedTextColor.GREEN)
                            .append(text("✓ Chest linked successfully!", NamedTextColor.WHITE))
                            .appendNewline()
                            .append(text("Location: " + blockX + ", " + blockY + ", " + blockZ, NamedTextColor.GRAY)));
                    } else {
                        player.sendMessage(text("[DEBUG] You ain't sneakin! Hold sneak, THEN left click the chest", NamedTextColor.RED));
                    }
                }
            });
        }
    }

    private void onRightClick(final PlayerInteractEvent event, final Player player, final Block block) {
        if (!BlockHelper.isShopAllowed(block))
            return;

        final var uniqueId = player.getUniqueId();
        final var configurationItem = Bukkit.getItemFactory().createItemStack(api.configuration().item());
        final var hasConfigurationItem = event.getItem() != null && event.getItem().isSimilar(configurationItem);

        final var blockX = block.getX();
        final var blockY = block.getY();
        final var blockZ = block.getZ();
        final var blockWorld = block.getWorld().getName();

        final var shopOpt = new AtomicReference<Optional<Shop>>();

        if (!api.exceptionService().tryCatch(event.getPlayer().getUniqueId(), () -> shopOpt.set(api.repository().shopAt(blockX, blockY, blockZ, blockWorld))))
            return;

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
            final var canAccessClaim = api.claim() == null || api.claim().canCreateShop(uniqueId, blockX, blockY, blockZ, blockWorld);

            if (canAccessClaim && hasConfigurationItem) {
                
                if (!isWithinBoundaries(blockX, blockZ, blockWorld)) {
                    player.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                        .append(text("Shops can only be created in the shopping district!", NamedTextColor.RED))
                        .appendNewline()
                        .append(text("Allowed area: ", NamedTextColor.GRAY))
                        .append(text(getBoundaryDescription(), NamedTextColor.YELLOW)));
                    return;
                }
                
                final var wizard = api.operations().wizards().get(uniqueId);

                if (wizard != null && wizard.wizardState() == ShopWizard.WizardState.AWAITING_LOCATION) {
                    
                    if (!isWithinBoundaries(blockX, blockZ, blockWorld)) {
                    player.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                        .append(text("Shops can only be placed in the shopping district!", NamedTextColor.RED))
                        .appendNewline()
                        .append(text("Allowed area: ", NamedTextColor.GRAY))
                        .append(text(getBoundaryDescription(), NamedTextColor.YELLOW)));
                    return;
                }
                    
                    wizard.location(blockX, blockY, blockZ, blockWorld);
                    wizard.wizardState(ShopWizard.WizardState.AWAITING_CONFIRMATION);
                    api.sound().play(uniqueId, wizard.x(), wizard.y(), wizard.z(), wizard.world(), Sounds.MODIFY_SUCCESS);
                    ModifyShopUI.open(api, player, wizard);
                } else if (api.permission().hasPermission(uniqueId, SlabbyPermissions.SHOP_MODIFY)) {
                    CreateShopUI.open(api, player, block);
                }
            }
        });
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

        if (event.getAction() == InventoryAction.DROP_ONE_SLOT 
                || event.getAction() == InventoryAction.DROP_ALL_CURSOR 
                || event.getAction() == InventoryAction.DROP_ALL_SLOT 
                || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
            return;
        }

        if (ItemHelper.isRestrictedItem(event.getCursor()) && event.getClickedInventory() != event.getWhoClicked().getInventory()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onInventoryClose(final InventoryCloseEvent event) {
        if (event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
            api.operations().ifWizard(event.getPlayer().getUniqueId(), wizard -> {
                //NOTE: It seems that the wizard can get into an invalid state sometimes.
                if (wizard.wizardState() == null
                        || wizard.wizardState() == ShopWizard.WizardState.AWAITING_CONFIRMATION
                        || wizard.wizardState() == ShopWizard.WizardState.AWAITING_ITEM)
                    api.operations().wizards().remove(event.getPlayer().getUniqueId());
            });

            OwnerShopUI.onClose(event.getPlayer().getUniqueId());
            ClientShopUI.onClose(event.getPlayer().getUniqueId());
        }

        handleChestRestocking(event);
    }

    private boolean isWithinBoundaries(final int x, final int z, final String world) {
        // with no claim system, allow anywhere
        if (api.claim() == null) {
            return true;
        }

        final var area = api.claim().getArea();
        
        // check in correct world
        if (!area.world().equalsIgnoreCase(world)) {
            return false;
        }
        
        // check in correct boundaries
        return x >= area.minX() && x <= area.maxX() 
            && z >= area.minZ() && z <= area.maxZ();
    }

    private String getBoundaryDescription() {
        if (api.claim() == null) {
            return "anywhere";
        }
        
        final var area = api.claim().getArea();
        return String.format("X: %d to %d, Z: %d to %d (%s)", 
            area.minX(), area.maxX(), 
            area.minZ(), area.maxZ(), 
            area.world());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onChatMessage(final AsyncPlayerChatEvent event) {
        // https://github.com/Aust1n46/VentureChat/issues/122
        api.operations().ifWizard(event.getPlayer().getUniqueId(), wizard -> {
            if (wizard.wizardState() == null || !wizard.wizardState().awaitingTextInput())
                return;

            final var text = event.getMessage();

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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerQuit(final PlayerQuitEvent event) {
        api.operations().wizards().remove(event.getPlayer().getUniqueId());
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
        Bukkit.getLogger().info("[DEBUG HOPPER] onInventoryMoveItem triggered");
        
        final var restock = api.configuration().restock();

        if (!restock.chests().enabled() || !restock.chests().hoppers().enabled()) {
            Bukkit.getLogger().info("[DEBUG HOPPER] Chest/hopper restocking disabled in config");
            return;
        }
        
        Bukkit.getLogger().info("[DEBUG HOPPER] Chest restocking enabled");

        final var destination = event.getDestination();
        final var location = destination.getLocation();

        if (location == null || destination.getType() != InventoryType.CHEST)
            return;

        Optional<Shop> shopOpt = Optional.empty();

        try {
            shopOpt = api.repository().shopWithInventoryAt(
                location.getBlockX(), 
                location.getBlockY(), 
                location.getBlockZ(), 
                location.getWorld().getName());
        } catch (final SlabbyException e) {
            api.exceptionService().logToConsole("Error while attempting to get shop for linked inventory", e);
            return;
        }

        if (shopOpt.isEmpty())
            return;

        final var shop = shopOpt.get();
        final var shopItem = api.serialization().<ItemStack>deserialize(shop.item());

        // only process if the item being moved matches the shop item - issimilar is modern approach ?
        if (!shopItem.isSimilar(event.getItem()))
            return;

        //TODO: ensure shop is not full, use same security measures as deposit function
        if (restock.chests().hoppers().batches() && shopItem.getMaxStackSize() > 1) {
            final var amount = Math.min(shopItem.getMaxStackSize(), shop.quantity());
            shopItem.setAmount(amount - 1);

            if (destination.containsAtLeast(shopItem, shopItem.getAmount())) {
                shop.stock(shop.stock() + amount);

                try {
                    api.repository().update(shop);
                    event.setItem(ItemStack.empty());
                    destination.remove(shopItem);
                } catch (final SlabbyException e) {
                    api.exceptionService().logToConsole("Error while attempting to update shop from linked inventory", e);
                }
            }
        } else {
            shop.stock(shop.stock() + event.getItem().getAmount());

            try {
                api.repository().update(shop);
                event.setItem(ItemStack.empty());
            } catch (final SlabbyException e) {
                api.exceptionService().logToConsole("Error while attempting to update shop from linked inventory", e);
            }
        }
        
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (!ItemHelper.isRestrictedItem(event.getItemDrop().getItemStack()))
            return;

        event.getItemDrop().remove();
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getInventory() == event.getWhoClicked().getInventory())
            return;

        if (ItemHelper.isRestrictedItem(event.getOldCursor()))
            event.setCancelled(true);
    }

    private void handleChestRestocking(final InventoryCloseEvent event) {
        // Check if chest restocking is enabled
        if (!api.configuration().restock().chests().enabled()) {
            return;
        }
        
        // Only handle player closures
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        // Only handle chest inventories
        if (event.getInventory().getType() != InventoryType.CHEST) {
            return;
        }
        
        // Get chest location
        final var location = event.getInventory().getLocation();
        if (location == null) {
            return;
        }
        
        // Check if this chest is linked to a shop
        Optional<Shop> shopOpt = Optional.empty();
        try {
            shopOpt = api.repository().shopWithInventoryAt(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getWorld().getName());
        } catch (final SlabbyException e) {
            api.exceptionService().logToConsole("Error checking for linked shop on chest close", e);
            return;
        }
        
        if (shopOpt.isEmpty()) {
            return; // Not a linked chest
        }
        
        final var shop = shopOpt.get();
        
        // SECURITY: Only allow shop owner or admins to manually restock
        if (!shop.isOwner(player.getUniqueId()) && !api.isAdminMode(player.getUniqueId())) {
            return;
        }
        
        // Process the restocking
        try {
            updateShopStockFromChest(shop, event.getInventory());
            
            // Only send success message if player is still online (not disconnected)
            if (player.isOnline() && event.getReason() == InventoryCloseEvent.Reason.PLAYER) {
                player.sendMessage(text("[Slabby] ", NamedTextColor.GREEN)
                    .append(text("✓ Chest restocked! New stock: " + shop.stock(), NamedTextColor.WHITE)));
            }
            
            // Log to console if disconnect/kick (for admin awareness)
            if (event.getReason() != InventoryCloseEvent.Reason.PLAYER) {
                Bukkit.getLogger().info("[Slabby] Auto-restocked shop " + shop.id() + 
                    " for " + player.getName() + " (reason: " + event.getReason() + ")");
            }
        } catch (final SlabbyException e) {
            api.exceptionService().logToConsole("Error updating stock from manual chest restock on close", e);
            
            // Only send error message if player is still online
            if (player.isOnline()) {
                player.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("Error updating shop stock!", NamedTextColor.RED)));
            }
        }
    }

    private void updateShopStockFromChest(final Shop shop, final Inventory chestInventory) throws SlabbyException {
        api.repository().refresh(shop);
        
        final var shopItem = api.serialization().<ItemStack>deserialize(shop.item());
        
        Bukkit.getLogger().info("[DEBUG] updateShopStockFromChest called for shop " + shop.id());
        Bukkit.getLogger().info("[DEBUG] Looking for item: " + shopItem.getType());
        
        int totalInChest = 0;
        for (ItemStack item : chestInventory.getContents()) {
            if (item != null && shopItem.isSimilar(item)) {
                totalInChest += item.getAmount();
                Bukkit.getLogger().info("[DEBUG] Found stack: " + item.getAmount());
            }
        }
        
        Bukkit.getLogger().info("[DEBUG] Total items in chest: " + totalInChest);
        
        if (totalInChest == 0) {
            Bukkit.getLogger().info("[DEBUG] No items found - exiting");
            return;
        }
        
        int oldStock = shop.stock();
        int newStock = oldStock + totalInChest;
        
        Bukkit.getLogger().info("[DEBUG] Old stock: " + oldStock + ", New stock: " + newStock);
        
        if (newStock > api.configuration().maxStock()) {
            newStock = api.configuration().maxStock();
            Bukkit.getLogger().info("[DEBUG] Capped at max stock: " + newStock);
        }
        
        shop.stock(newStock);
        Bukkit.getLogger().info("[DEBUG] Set shop stock to: " + newStock);
        
        api.repository().update(shop);
        Bukkit.getLogger().info("[DEBUG] Updated shop in database");
        
        // FIXED: Remove ALL matching items properly by iterating through slots
        for (int i = 0; i < chestInventory.getSize(); i++) {
            ItemStack item = chestInventory.getItem(i);
            if (item != null && shopItem.isSimilar(item)) {
                chestInventory.setItem(i, null); // Clear the slot
                Bukkit.getLogger().info("[DEBUG] Removed stack from slot " + i);
            }
        }
        Bukkit.getLogger().info("[DEBUG] All matching items removed from chest");
    }
}



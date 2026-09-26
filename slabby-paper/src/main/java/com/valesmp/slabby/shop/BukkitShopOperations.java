package com.valesmp.slabby.shop;

import com.valesmp.slabby.Slabby;
import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.exception.*;
import com.valesmp.slabby.exception.UnsupportedOperationException;
import com.valesmp.slabby.helper.ItemHelper;
import com.valesmp.slabby.permission.SlabbyPermissions;
import com.valesmp.slabby.shop.log.LocationChanged;
import com.valesmp.slabby.shop.log.Transaction;
import com.valesmp.slabby.shop.log.ValueChanged;
import com.valesmp.slabby.wrapper.sound.Sounds;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Accessors(fluent = true, chain = false)
public final class BukkitShopOperations implements ShopOperations {

    @Getter
    // concurrent because the async chat listener reads from it off the main thread
    private final Map<UUID, ShopWizard> wizards = new ConcurrentHashMap<>();

    private final SlabbyAPI api;

    @Override
    public ShopWizard wizard(final UUID uniqueId) {
        return this.wizards.compute(uniqueId, (k, v) -> new BukkitShopWizard(api));
    }

    @Override
    public ShopWizard wizardOf(final UUID uniqueId, final Shop shop) {
        return this.wizards.compute(uniqueId, (k, v) -> new BukkitShopWizard(api, shop));
    }

    @Override
    public void ifWizard(final UUID uniqueId, final Consumer<ShopWizard> action) {
        final var wizard = this.wizards.get(uniqueId);

        if (wizard != null)
            action.accept(wizard);
    }

    @Override
    public void ifWizardOrElse(final UUID uniqueId, final Consumer<ShopWizard> action, final Runnable orElse) {
        final var wizard = this.wizards.get(uniqueId);

        if (wizard != null)
            action.accept(wizard);
        else
            orElse.run();
    }

    @Override
    public Map<UUID, Double> splitCost(final double amount, final Shop shop) {
        final var result = new HashMap<UUID, Double>();

        for (final var shopOwner : shop.owners()) {
            result.put(shopOwner.uniqueId(), amount * (shopOwner.share() * 0.01));
        }

        return result;
    }

    @Override
    public void buy(final UUID uniqueId, final Shop shop) throws SlabbyException {
        if (!api.permission().hasPermission(uniqueId, SlabbyPermissions.SHOP_INTERACT))
            throw new NoPermissionException();

        api.repository().refresh(shop);

        if (shop.buyPrice() == null)
            throw new UnsupportedOperationException("Unable to buy from shop: shop is not selling");

        final var client = Objects.requireNonNull(Bukkit.getPlayer(uniqueId));

        if (!shop.hasStock(shop.quantity()))
            throw new ShopOutOfStockException();

        final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());

        if (!ItemHelper.hasSpace(client.getInventory(), itemStack, shop.quantity()))
            throw new PlayerOutOfInventorySpaceException();

        if (!api.economy().hasAmount(uniqueId, shop.buyPrice()))
            throw new InsufficientBalanceToBuyException();

        final var result = api.economy().withdraw(uniqueId, shop.buyPrice());

        if (!result.success())
            throw new InsufficientBalanceToBuyException();

        if (shop.stock() != null)
            shop.stock(shop.stock() - shop.quantity());

        try {
            api.repository().transaction(() -> {
                api.repository().update(shop);

                final var log = api.repository()
                        .<ShopLog.Builder>builder(ShopLog.Builder.class)
                        .action(ShopLog.Action.BUY)
                        .uniqueId(uniqueId)
                        .serialized(new Transaction(shop.buyPrice(), shop.quantity()))
                        .build();

                shop.logs().add(log);
                return null;
            });
        } catch (final SlabbyException e) {
            // save failed so give the buyer their money back and reset the stock
            api.economy().deposit(uniqueId, result.amount());
            api.repository().refresh(shop);
            throw e;
        }

        // owners only get paid once the sale is actually saved
        if (shop.stock() != null)
            splitCost(result.amount(), shop).forEach((key, value) -> api.economy().deposit(key, value));

        addItemToInventory(itemStack, client, shop.quantity());

        notifyBuy(uniqueId, shop, client, itemStack);
    }

    private void notifyBuy(final UUID uniqueId, final Shop shop, final Player client, final ItemStack itemStack) {
        api.sound().play(uniqueId, shop, Sounds.BUY_SELL_SUCCESS);

        client.sendMessage(api.messages().client().buy().message(itemStack.displayName(), shop.quantity(), shop.buyPrice()));

        if (shop.stock() != null) {
            for (final var shopOwner : shop.owners()) {
                final var playerOwner = Bukkit.getPlayer(shopOwner.uniqueId());
                final var ownerCut = shop.buyPrice() * shopOwner.share() * 0.01;

                if (playerOwner != null) {
                    playerOwner.sendMessage(api.messages().client().buy().messageOwner(client.displayName(), shop.quantity(), itemStack.displayName(), shop.buyPrice()));
                } else {
                    enqueueOfflineNotification(shopOwner.uniqueId(), ShopLog.Action.BUY, shop.quantity(), ownerCut);
                }
            }
        }
    }

    @Override
    public void sell(final UUID uniqueId, final Shop shop) throws SlabbyException {
        if (!api.permission().hasPermission(uniqueId, SlabbyPermissions.SHOP_INTERACT))
            throw new NoPermissionException();

        api.repository().refresh(shop);

        if (shop.sellPrice() == null)
            throw new UnsupportedOperationException("Unable to sell to shop: shop is not buying");

        final var client = Objects.requireNonNull(Bukkit.getPlayer(uniqueId));
        final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());

        if (!client.getInventory().containsAtLeast(itemStack, shop.quantity()))
            throw new PlayerOutOfStockException();

        final var cost = splitCost(shop.sellPrice(), shop);

        if (shop.stock() != null) {
            if (cost.entrySet().stream().anyMatch(it -> !api.economy().hasAmount(it.getKey(), it.getValue())))
                throw new InsufficientBalanceToSellException();

            try {
                final var stock = Math.addExact(shop.stock(), shop.quantity());

                if (stock > api.configuration().maxStock())
                    throw new ShopOutOfSpaceException();

                shop.stock(stock);
            } catch (final ArithmeticException e) {
                throw new ShopOutOfSpaceException(e);
            }
        }

        final var charged = new HashMap<UUID, Double>();

        if (shop.stock() != null) {
            for (final var entry : cost.entrySet()) {
                // if any owner cant pay, refund whoever already paid and call it off
                if (!api.economy().withdraw(entry.getKey(), entry.getValue()).success()) {
                    refund(charged);
                    api.repository().refresh(shop);
                    throw new InsufficientBalanceToSellException();
                }

                charged.put(entry.getKey(), entry.getValue());
            }
        }

        try {
            api.repository().transaction(() -> {
                api.repository().update(shop);

                final var log = api.repository()
                        .<ShopLog.Builder>builder(ShopLog.Builder.class)
                        .action(ShopLog.Action.SELL)
                        .uniqueId(uniqueId)
                        .serialized(new Transaction(shop.sellPrice(), shop.quantity()))
                        .build();

                shop.logs().add(log);
                return null;
            });
        } catch (final SlabbyException e) {
            refund(charged);
            api.repository().refresh(shop);
            throw e;
        }

        itemStack.setAmount(shop.quantity());

        client.getInventory().removeItem(itemStack);

        api.economy().deposit(uniqueId, shop.sellPrice());

        notifySell(shop, client, itemStack);
    }

    private void notifySell(final Shop shop, final Player client, final ItemStack itemStack) {
        api.sound().play(client.getUniqueId(), shop, Sounds.BUY_SELL_SUCCESS);

        client.sendMessage(api.messages().client().sell().message(itemStack.displayName(), shop.quantity(), shop.sellPrice()));

        if (shop.stock() != null) {
            for (final var shopOwner : shop.owners()) {
                final var playerOwner = Bukkit.getPlayer(shopOwner.uniqueId());
                final var ownerCut = shop.sellPrice() * shopOwner.share() * 0.01;

                if (playerOwner != null) {
                    playerOwner.sendMessage(api.messages().client().sell().messageOwner(client.displayName(), shop.quantity(), itemStack.displayName(), shop.sellPrice()));
                } else {
                    enqueueOfflineNotification(shopOwner.uniqueId(), ShopLog.Action.SELL, shop.quantity(), ownerCut);
                }
            }
        }
    }

    private void refund(final Map<UUID, Double> charged) {
        charged.forEach((key, value) -> api.economy().deposit(key, value));
    }

    private void enqueueOfflineNotification(final UUID recipient, final ShopLog.Action action, final int quantity, final double amount) {
        try {
            api.repository().enqueueNotification(recipient, action, quantity, amount);
        } catch (final SlabbyException e) {
            api.exceptionService().logToConsole("Error enqueueing offline notification", e);
        }
    }

    @Override
    public void withdraw(final UUID uniqueId, final Shop shop, final int amount) throws SlabbyException {
        if (amount < 1)
            throw new IllegalArgumentException("Amount has to be higher than zero");

        if (shop.stock() == null)
            throw new UnsupportedOperationException("Cannot withdraw from admin shop");

        api.repository().refresh(shop);

        if (!shop.hasStock(amount))
            throw new ShopOutOfStockException();

        final var shopOwner = Objects.requireNonNull(Bukkit.getPlayer(uniqueId));
        final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());

        if (!ItemHelper.hasSpace(shopOwner.getInventory(), itemStack, amount))
            throw new PlayerOutOfInventorySpaceException();

        final var stock = shop.stock();

        shop.stock(stock - amount);

        api.repository().transaction(() -> {
            api.repository().update(shop);

            final var log = api.repository()
                    .<ShopLog.Builder>builder(ShopLog.Builder.class)
                    .action(ShopLog.Action.WITHDRAW)
                    .uniqueId(uniqueId)
                    .serialized(new ValueChanged.Int(stock, shop.stock()))
                    .build();

            shop.logs().add(log);
            return null;
        });

        addItemToInventory(itemStack, shopOwner, amount);

        api.sound().play(uniqueId, shop, Sounds.BUY_SELL_SUCCESS);
    }

    @Override
    public void deposit(final UUID uniqueId, final Shop shop, int amount) throws SlabbyException {
        if (amount < 1)
            throw new IllegalArgumentException("Amount has to be higher than zero");

        if (shop.stock() == null)
            throw new UnsupportedOperationException("Cannot deposit to admin shop");

        api.repository().refresh(shop);

        final var shopOwner = Objects.requireNonNull(Bukkit.getPlayer(uniqueId));
        final var itemStack = api.serialization().<ItemStack>deserialize(shop.item());
        final var itemInHand = shopOwner.getInventory().getItemInMainHand();

        Runnable removeItem;

        if (api.configuration().restock().punch().shulker()
                && !itemStack.isSimilar(itemInHand)
                && itemInHand.getItemMeta() instanceof BlockStateMeta meta
                && meta.getBlockState() instanceof ShulkerBox shulker) {
            amount = ItemHelper.countSimilar(shulker.getInventory(), itemStack);

            if (amount == 0)
                throw new PlayerOutOfStockException();

            removeItem = () -> {
                shulker.getInventory().removeItem(itemStack);
                meta.setBlockState(shulker);
                itemInHand.setItemMeta(meta);
            };
        } else {
            if (!shopOwner.getInventory().containsAtLeast(itemStack, amount))
                throw new PlayerOutOfStockException();

            removeItem = () -> shopOwner.getInventory().removeItem(itemStack);
        }

        final var oldStock = shop.stock();

        if (shop.stock() != null) {
            try {
                final var stock = Math.addExact(oldStock, amount);

                if (stock > api.configuration().maxStock())
                    throw new ShopOutOfSpaceException();

                shop.stock(stock);
            } catch (final ArithmeticException e) {
                throw new ShopOutOfSpaceException(e);
            }
        }

        api.repository().transaction(() -> {
            api.repository().update(shop);

            final var log = api.repository()
                    .<ShopLog.Builder>builder(ShopLog.Builder.class)
                    .action(ShopLog.Action.DEPOSIT)
                    .uniqueId(uniqueId)
                    .serialized(new ValueChanged.Int(oldStock, shop.stock()))
                    .build();

            shop.logs().add(log);
            return null;
        });

        itemStack.setAmount(amount);

        removeItem.run();

        api.sound().play(uniqueId, shop, Sounds.BUY_SELL_SUCCESS);
    }

    @Override
    public void createOrUpdateShop(final UUID uniqueId, final ShopWizard wizard) throws SlabbyException {
        final var success = api.repository().transaction(() -> {
            final var shopOpt = api.repository().shopById(wizard.id());

            if (shopOpt.isPresent()) {
                final var shop = shopOpt.get();

                shop.buyPrice(wizard.buyPrice());
                shop.sellPrice(wizard.sellPrice());
                shop.quantity(wizard.quantity());
                shop.note(wizard.note());
                shop.state(wizard.state());

                shop.location(wizard.x(), wizard.y(), wizard.z(), wizard.world());

                api.repository().transaction(() -> {
                    for (final var entry : wizard.valueChanges().entrySet()) {
                        final var log = api.repository().<ShopLog.Builder>builder(ShopLog.Builder.class)
                                .action(entry.getKey())
                                .uniqueId(uniqueId)
                                .serialized(entry.getValue())
                                .build();

                        shop.logs().add(log);
                    }
                    api.repository().update(shop);
                    return null;
                });
            } else {
                final var shop = api.repository().<Shop.Builder>builder(Shop.Builder.class)
                        .location(wizard.x(), wizard.y(), wizard.z(), wizard.world())
                        .item(wizard.item())
                        .buyPrice(wizard.buyPrice())
                        .sellPrice(wizard.sellPrice())
                        .quantity(wizard.quantity())
                        .note(wizard.note())
                        .stock(api.isAdminMode(uniqueId) ? null : 0)
                        .build();

                api.repository().transaction(() -> {
                    api.repository().createOrUpdate(shop);

                    shop.owners().add(api.repository().<ShopOwner.Builder>builder(ShopOwner.Builder.class)
                            .uniqueId(uniqueId)
                            .share(100)
                            .build());

                    shop.logs().add(api.repository().<ShopLog.Builder>builder(ShopLog.Builder.class)
                            .action(ShopLog.Action.SHOP_CREATED)
                            .uniqueId(uniqueId).build());

                    return null;
                });
            }

            return true;
        });

        if (success) {
            final var shopOpt = api.repository().shopAt(wizard.x(), wizard.y(), wizard.z(), wizard.world());

            if (shopOpt.isPresent()) {
                final var shop = shopOpt.get();

                spawnDisplayItem(shop);

                api.repository().update(shop);
            }
        }
    }

    @Override
    public void removeShop(final UUID uniqueId, final Shop shop) throws SlabbyException {
        // stops a double click on confirm deleting it twice
        if (shop.state() == Shop.State.DELETED)
            return;

        if (shop.displayEntityId() != null && Bukkit.getEntity(shop.displayEntityId()) instanceof Display e)
            e.remove();

        api.repository().markAsDeleted(uniqueId, shop);

        api.sound().play(uniqueId, shop, Sounds.DESTROY);
    }

    @Override
    public void spawnDisplayItem(final Shop shop) {
        // if the old one isnt loaded right now it gets cleaned up when its chunk loads, see SlabbyListener#onEntitiesLoad
        if (shop.displayEntityId() != null && Bukkit.getEntity(shop.displayEntityId()) instanceof Display old)
            old.remove();

        final var item = api.serialization().<ItemStack>deserialize(shop.item());
        final var bukkitWorld = Bukkit.getWorld(shop.world());
        final var block = bukkitWorld.getBlockAt(shop.x(), shop.y(), shop.z());

        final var itemDisplay = (ItemDisplay) bukkitWorld.spawnEntity(new Location(bukkitWorld, block.getBoundingBox().getCenterX(), block.getBoundingBox().getMaxY(), block.getBoundingBox().getCenterZ()), EntityType.ITEM_DISPLAY);

        itemDisplay.setBillboard(Display.Billboard.VERTICAL);

        itemDisplay.setItemStack(item);
        itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);

        // tag it with the shop id so we can tell which shop it belongs to later
        itemDisplay.getPersistentDataContainer().set(((Slabby) api).displayKey(), PersistentDataType.INTEGER, shop.<Integer>id());

        shop.displayEntityId(itemDisplay.getUniqueId());
    }

    @Override
    public void linkShop(final UUID uniqueId, final ShopWizard wizard, final int x, final int y, final int z, final String world) throws SlabbyException {
        final var linkShopOpt = api.repository().shopAt(wizard.x(), wizard.y(), wizard.z(), wizard.world());

        if (linkShopOpt.isPresent()) {
            final var shop = linkShopOpt.get();

            shop.inventory(x, y, z, world);

            api.repository().transaction(() -> {
                api.repository().update(shop);

                final var log = api.repository().<ShopLog.Builder>builder(ShopLog.Builder.class)
                        .action(ShopLog.Action.INVENTORY_LINK_CHANGED)
                        .uniqueId(uniqueId)
                        .serialized(new LocationChanged(shop.inventoryX(), shop.inventoryY(), shop.inventoryZ(), shop.world()))
                        .build();

                shop.logs().add(log);

                return null;
            });

            api.sound().play(uniqueId, shop, Sounds.SUCCESS);
        }

        api.operations().wizards().remove(uniqueId);
    }

    @Override
    public void unlinkShop(final UUID uniqueId, final Shop shop) throws SlabbyException {
        shop.inventory(null, null, null, null);

        api.repository().transaction(() -> {
            api.repository().update(shop);

            final var log = api.repository().<ShopLog.Builder>builder(ShopLog.Builder.class)
                    .action(ShopLog.Action.INVENTORY_LINK_CHANGED)
                    .uniqueId(uniqueId)
                    .serialized(new LocationChanged(null, null, null, null))
                    .build();

            shop.logs().add(log);

            return null;
        });

        api.sound().play(uniqueId, shop, Sounds.MODIFY_SUCCESS);

        Objects.requireNonNull(Bukkit.getPlayer(uniqueId))
                .sendMessage(api.messages().owner().inventoryLink().cancel().message());
    }

    private static void addItemToInventory(final ItemStack itemStack, final Player player, final int amount) {
        final var itemStacks = new ArrayList<ItemStack>();

        var quantity = amount;

        while (quantity > 0) {
            final var cloneStack = itemStack.clone();
            final var maxStackSize = Math.min(quantity, cloneStack.getMaxStackSize());

            cloneStack.setAmount(maxStackSize);

            itemStacks.add(cloneStack);

            quantity -= maxStackSize;
        }

        final var leftover = player.getInventory().addItem(itemStacks.toArray(ItemStack[]::new));

        // anything that didnt fit goes on the floor instead of vanishing
        leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation(), it));
    }

}

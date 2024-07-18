package gg.mew.slabby.importer.slabbo;

import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.importer.Importer;
import gg.mew.slabby.shop.Shop;
import gg.mew.slabby.shop.ShopOwner;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

public final class SlabboImporter implements Importer {

    @Override
    public void onImport(final SlabbyAPI api) {
        final var slabboDir = new File(api.directory().getParentFile(), "Slabbo");
        final var slabboConfig = YamlConfiguration.loadConfiguration(new File(slabboDir, "shops.yml"));

        @SuppressWarnings("unchecked")
        final var slabboShops = (Map<String, SlabboShop>) (Object) slabboConfig.getConfigurationSection("shops").getValues(false);

        for (final var oldShop : slabboShops.values()) {
            try {
                api.repository().transaction(() -> {
                    final var shop = api.repository()
                            .<Shop.Builder>builder(Shop.Builder.class)
                            .item(oldShop.item())
                            .x(oldShop.x())
                            .y(oldShop.y())
                            .z(oldShop.z())
                            .world(oldShop.world())
                            .buyPrice(oldShop.buyPrice())
                            .sellPrice(oldShop.sellPrice())
                            .quantity(oldShop.quantity())
                            .stock(oldShop.stock())
                            .note(oldShop.note())
                            .name(oldShop.name())
                            .build();

                    api.repository().createOrUpdate(shop);

                    final var owner = api.repository()
                            .<ShopOwner.Builder>builder(ShopOwner.Builder.class)
                            .uniqueId(oldShop.uniqueId())
                            .share(100)
                            .build();

                    shop.owners().add(owner);

                    api.operations().removeAndSpawnDisplayItem(shop);

                    api.repository().update(shop);

                    return null;
                });
            } catch (final SQLException e) {
                api.exceptionService().logToConsole("Error while importing slabbo shop", e);
            }

            final var keyShopLocation = new NamespacedKey("slabbo", "shoplocation");

            for (final var world : Bukkit.getWorlds()) {
                for (final var item : world.getEntitiesByClass(Item.class)) {
                    final var persistentData = item.getItemStack().getItemMeta().getPersistentDataContainer();
                    if (persistentData.has(keyShopLocation)) {
                        item.remove();
                    }
                }
            }
        }
    }

}
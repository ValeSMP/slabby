package gg.mew.slabby.importer.slabbo;

import gg.mew.slabby.SlabbyHelper;
import gg.mew.slabby.helper.ItemHelper;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@SerializableAs("Shop")
@Accessors(fluent = true, chain = false)
@Getter
public final class SlabboShop implements ConfigurationSerializable {

    private Double buyPrice;
    private Double sellPrice;

    private int quantity;

    private int x;
    private int y;
    private int z;
    private String world;

    private String item;

    private Integer stock;

    private UUID uniqueId;

    private String note;

    private Integer inventoryX;
    private Integer inventoryY;
    private Integer inventoryZ;
    private String inventoryWorld;

    private String name;

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.of();
    }

    public static SlabboShop deserialize(final Map<String, Object> args) {
        final var result = new SlabboShop();

        result.buyPrice = (Double) args.computeIfPresent("buyPrice", (k, v) -> {
            final var value = (double) v;
            return value == -1D ? null : value;
        });

        result.sellPrice = (Double) args.computeIfPresent("sellPrice", (k, v) -> {
            final var value = (double) v;
            return value == -1D ? null : value;
        });

        result.quantity = Math.max((int) args.get("quantity"), 1);

        final var location = (Location) args.get("location");

        if (location != null) {
            result.x = location.getBlockX();
            result.y = location.getBlockY();
            result.z = location.getBlockZ();
            result.world = location.getWorld().getName();
        }

        final var item = (ItemStack) args.get("item");

        result.item = SlabbyHelper.api().serialization().serialize(item);

        final var isAdmin = (boolean) args.get("admin");

        result.stock = isAdmin ? null : (int) args.get("stock");

        result.uniqueId = UUID.fromString((String)args.get("ownerId"));

        result.note = (String) args.compute("note", (k, v) -> v == null ? SlabbyHelper.api().configuration().defaults().note() : v);

        final var linkedChestLocation = (String) args.get("linkedChestLocation");

        if (linkedChestLocation != null && !linkedChestLocation.isEmpty()) {
            final var split = linkedChestLocation.split(",");
            result.inventoryWorld = split[0];
            result.inventoryX = Integer.parseInt(split[1]);
            result.inventoryY = Integer.parseInt(split[2]);
            result.inventoryZ = Integer.parseInt(split[3]);
        }

        final var shopName = (String) args.get("shopName");

        if (shopName != null && !shopName.isEmpty()) {
            result.name = shopName;
        }

        return result;
    }

}

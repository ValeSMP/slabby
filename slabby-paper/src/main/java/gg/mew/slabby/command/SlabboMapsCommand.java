package gg.mew.slabby.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.mew.slabby.Slabby;
import gg.mew.slabby.SlabbyHelper;
import gg.mew.slabby.helper.ItemHelper;
import gg.mew.slabby.maps.OrderBy;
import gg.mew.slabby.permission.SlabbyPermissions;
import gg.mew.slabby.shop.Shop;
import gg.mew.slabby.wrapper.claim.ClaimWrapper;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import static net.kyori.adventure.text.Component.*;

@CommandAlias("slabbo-maps")
public final class SlabboMapsCommand extends BaseCommand {

    private final Slabby api;

    public SlabboMapsCommand(final Slabby api) {
        this.api = api;
    }

    private boolean giveCompass(Player player, Shop shop) {
        if (player.getInventory().firstEmpty() == -1)
            return false;

        final var compass = new ItemStack(Material.COMPASS, 1);
        final var meta = (CompassMeta) compass.getItemMeta();

        final var item = api.serialization().<ItemStack>deserialize(shop.item());

        meta.displayName(translatable(item.translationKey(), NamedTextColor.YELLOW).appendSpace());

        meta.getPersistentDataContainer().set(this.api.deleteKey(), PersistentDataType.BOOLEAN, true);

        meta.setLodestone(new Location(Bukkit.getWorld(shop.world()), shop.x(), shop.y(), shop.z()));
        meta.setLodestoneTracked(false);

        compass.setItemMeta(meta);

        if (player.getInventory().contains(compass))
            return false;

        player.getInventory().addItem(compass);

        return true;
    }


    @Subcommand("locate item")
    @Syntax("<item>")
    @CommandPermission(SlabbyPermissions.SLABBO_MAPS_LOCATE_ITEM)
    @CommandCompletion("@items")
    public void onLocateItem(final Player player, final ItemStack itemStack, @Default("BuyAscending") final OrderBy orderBy) {
        final var area = api.claim() == null
                ? new ClaimWrapper.Area(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, "world")
                : api.claim().getArea();

        @SuppressWarnings("resource")
        final var shop = this.api
                .repository()
                .shopsInArea(area.minX(), area.minZ(), area.maxX(), area.maxZ(), area.world())
                .stream()
                .filter(orderBy)
                .filter(it -> api.serialization().<ItemStack>deserialize(it.item()).isSimilar(itemStack))
                .min(orderBy);

        if (shop.isPresent()) {
            final var success = giveCompass(player, shop.get());

            if (success) {
                player.sendMessage(text("[Slabby] You have received a compass that will lead the way to", NamedTextColor.GRAY)
                        .appendSpace()
                        .append(empty().color(NamedTextColor.YELLOW).append(text("[").append(translatable(itemStack.translationKey()).append(text("]")))).hoverEvent(itemStack))
                        .append(text(". You can drop the compass to remove it from your inventory.")));
            } else {
                player.sendMessage(text("[Slabby] You already have this compass or you have no inventory space.", NamedTextColor.GRAY));
            }
        } else {
            player.sendMessage(text("[Slabby]", NamedTextColor.GRAY)
                    .appendSpace()
                    .append(text("[", NamedTextColor.YELLOW).append(translatable(itemStack.translationKey()).append(text("]"))).hoverEvent(itemStack))
                    .append(text(" is not available in any shop.")));
        }
    }

    public static boolean isRestrictedItem(final ItemStack itemStack) {
        final var slabby = (Slabby) SlabbyHelper.api();
        return itemStack != null && itemStack.getItemMeta() != null && itemStack.getItemMeta().getPersistentDataContainer().has(slabby.deleteKey());
    }

}
package com.valesmp.slabby.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.valesmp.slabby.Slabby;
import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.SlabbyHelper;
import com.valesmp.slabby.gui.RestoreShopUI;
import com.valesmp.slabby.importer.ImportType;
import com.valesmp.slabby.maps.OrderBy;
import com.valesmp.slabby.permission.SlabbyPermissions;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.wrapper.claim.ClaimWrapper;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;

@RequiredArgsConstructor
@CommandAlias("slabby")
public final class SlabbyCommand extends BaseCommand {

    private final SlabbyAPI api;

    @Subcommand("reload")
    @CommandPermission(SlabbyPermissions.ADMIN_RELOAD)
    private void onReload(final Player player) {
        api.reload();

        player.sendMessage(api.messages().command().reload().message());
    }

    @Subcommand("admin")
    @CommandPermission(SlabbyPermissions.ADMIN_TOGGLE)
    private void onAdminToggle(final Player player) {
        if (api.setAdminMode(player.getUniqueId(), !api.isAdminMode(player.getUniqueId())))
            player.sendMessage(api.messages().command().admin().enabled());
        else
            player.sendMessage(api.messages().command().admin().disabled());
    }

    @Subcommand("restore")
    @CommandPermission(SlabbyPermissions.SHOP_RESTORE)
    private void onRestore(final Player player, final @Optional String targetName) {
        if (targetName != null && !player.hasPermission(SlabbyPermissions.ADMIN_RESTORE)) {
            player.sendMessage(Bukkit.permissionMessage());
        } else {
            final var target = targetName != null ? Bukkit.getOfflinePlayer(targetName).getUniqueId() : player.getUniqueId();
            RestoreShopUI.open(api, player, target);
        }
    }

    @Subcommand("import")
    @CommandPermission(SlabbyPermissions.ADMIN_IMPORT)
    private void onImport(final Player player, final ImportType importType) {
        importType.importer().onImport(api);

        player.sendMessage(api.messages().command().importer().message());
    }

    private boolean giveCompass(Player player, Shop shop) {
        if (player.getInventory().firstEmpty() == -1)
            return false;

        final var compass = new ItemStack(Material.COMPASS, 1);
        final var meta = (CompassMeta) compass.getItemMeta();

        final var item = api.serialization().<ItemStack>deserialize(shop.item());

        meta.displayName(translatable(item.translationKey(), NamedTextColor.YELLOW).appendSpace());

        meta.getPersistentDataContainer().set(((Slabby)this.api).deleteKey(), PersistentDataType.BOOLEAN, true);

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
    @CommandPermission(SlabbyPermissions.LOCATE_ITEM)
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

}

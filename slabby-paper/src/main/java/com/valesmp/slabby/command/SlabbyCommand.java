package com.valesmp.slabby.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import com.valesmp.slabby.Slabby;
import com.valesmp.slabby.SlabbyAPI;
import com.valesmp.slabby.exception.SlabbyException;
import com.valesmp.slabby.gui.RestoreShopUI;
import com.valesmp.slabby.helper.BlockHelper;
import com.valesmp.slabby.importer.ImportType;
import com.valesmp.slabby.maps.OrderBy;
import com.valesmp.slabby.permission.SlabbyPermissions;
import com.valesmp.slabby.shop.Shop;
import com.valesmp.slabby.shop.ShopOwner;
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

    /**
     * reset all display entities - regenerates them at correct positions
     * useful if axiom or other mods accidentally move display entities
     */
    @Subcommand("resetdisplays")
    @CommandPermission(SlabbyPermissions.ADMIN_RESET_DISPLAYS)
    @Description("Regenerate all shop display entities")
    private void onResetDisplays(final Player player) {
        player.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                .append(text("Regenerating all display entities...", NamedTextColor.GRAY)));

        // Get all active shops by querying entire world area
        // Use the lands config area, or fallback to a huge area if not configured
        final var lands = api.configuration().lands();
        final int minX = lands.minX() != 0 ? lands.minX() : Integer.MIN_VALUE / 2; // Avoid overflow
        final int minZ = lands.minZ() != 0 ? lands.minZ() : Integer.MIN_VALUE / 2;
        final int maxX = lands.maxX() != 0 ? lands.maxX() : Integer.MAX_VALUE / 2;
        final int maxZ = lands.maxZ() != 0 ? lands.maxZ() : Integer.MAX_VALUE / 2;
        final String world = lands.world() != null && !lands.world().isEmpty() ? lands.world() : "world";

        final var shops = api.repository().shopsInArea(minX, minZ, maxX, maxZ, world);
        int count = 0;
        int errors = 0;

        for (Shop shop : shops) {
            try {
                
                if (shop.state() == Shop.State.DELETED) {
                    continue;
                }

                // remove any lingering old entities
                if (shop.displayEntityId() != null) {
                    final var entity = Bukkit.getEntity(shop.displayEntityId());
                    if (entity != null) {
                        entity.remove();
                    }
                }

                // regenerate displays, at default spots
                api.operations().removeAndSpawnDisplayItem(
                        shop.x(), shop.y(), shop.z(), shop.world(), shop);
                count++;
                
            } catch (Exception e) {
                errors++;
                api.logger().warning("Failed to reset display for slabby shop at " + shop.x() + "," + shop.y() + "," + shop.z() + " in " + shop.world() + ": " + e.getMessage());
            }
        }

        if (errors > 0) {
            player.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("Regenerated " + count + " displays with " + errors + " errors!", NamedTextColor.YELLOW))
                    .appendNewline()
                    .append(text("Check console for error details. And git gud.", NamedTextColor.GRAY)));
        } else {
            player.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("Successfully regenerated " + count + " display entities!", NamedTextColor.GREEN)));
        }
    }

    /**
     * set the owner of a shop useful for player fuckups, players leaving and migration from quickshop
     */
    @Subcommand("setowner")
    @Syntax("<player>")
    @CommandPermission(SlabbyPermissions.ADMIN_SET_OWNER)
    @Description("Transfer shop ownership to another player")
    private void onSetOwner(final Player admin, final String targetName) {
        // must be an admin, and must be lookin at a slabby
        final var block = admin.getTargetBlockExact(5);
        if (block == null || !BlockHelper.isSlabbyBlock(block)) {
            admin.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("You must be looking at a shop slab!", NamedTextColor.RED)));
            return;
        }

        // shop getter
        final var shopOpt = api.repository().shopAt(
                block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
        
        if (shopOpt.isEmpty()) {
            admin.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("No shop found at this location!", NamedTextColor.RED)));
            return;
        }

        final var shop = shopOpt.get();
        final var targetPlayer = Bukkit.getOfflinePlayer(targetName);

        // clear the existing owner(s)
        shop.owners().clear();

        // add new single owner with 100% share TODO: allowance for adding multiple users and determining a share
        shop.owners().add(api.repository().<ShopOwner.Builder>builder(ShopOwner.Builder.class)
                .uniqueId(targetPlayer.getUniqueId())
                .share(100)
                .build());

        // save changes
        try {
            api.repository().update(shop);
            admin.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("Shop ownership transferred to ", NamedTextColor.GREEN))
                    .append(text(targetName, NamedTextColor.GOLD))
                    .append(text("!", NamedTextColor.GREEN)));
        } catch (SlabbyException e) {
            admin.sendMessage(text("[Slabby] ", NamedTextColor.YELLOW)
                    .append(text("Error transferring ownership: " + e.getMessage(), NamedTextColor.RED)));
        }
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

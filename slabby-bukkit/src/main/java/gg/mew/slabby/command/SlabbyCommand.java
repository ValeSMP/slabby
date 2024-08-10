package gg.mew.slabby.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.gui.RestoreShopUI;
import gg.mew.slabby.importer.ImportType;
import gg.mew.slabby.importer.slabbo.SlabboImporter;
import gg.mew.slabby.permission.SlabbyPermissions;
import gg.mew.slabby.shop.SQLiteShopRepository;
import gg.mew.slabby.shop.Shop;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.sql.SQLException;

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
            return;
        }

        final var target = targetName == null ? null : Bukkit.getOfflinePlayer(targetName);

        RestoreShopUI.open(api, player, target != null ? target.getUniqueId() : player.getUniqueId());
    }

    @Subcommand("import")
    @CommandPermission(SlabbyPermissions.ADMIN_IMPORT)
    private void onImport(final Player player, final ImportType importType) {
        importType.importer().onImport(api);

        player.sendMessage(api.messages().command().importer().message());
    }

    @Subcommand("debug")
    private void onDebug(final int shopId) {
        final var shop = api.repository().shopById(shopId).get();
        api.logger().info(api.serialization().<ItemStack>deserialize(shop.item()).toString());
    }

    @Subcommand("fix")
    private void onFix(final CommandSender sender) {
        final var x = (SQLiteShopRepository) api.repository();

        try {
            final var updateBuilder = x.getShopDao().updateBuilder();
            updateBuilder.where().eq(Shop.Names.QUANTITY, 0);
            updateBuilder.updateColumnValue(Shop.Names.QUANTITY, 1);
            updateBuilder.update();

            sender.sendMessage("[Slabby] Fixing shops where quantity is 0");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}

package gg.mew.slabby.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import gg.mew.slabby.SlabbyAPI;
import gg.mew.slabby.gui.RestoreShopUI;
import gg.mew.slabby.importer.slabbo.SlabboImporter;
import gg.mew.slabby.permission.SlabbyPermissions;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.awt.*;

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
    private void onRestore(final Player player, final @Optional OfflinePlayer target) {
        if (target != null && !player.hasPermission(SlabbyPermissions.ADMIN_RESTORE)) {
            player.sendMessage(Bukkit.permissionMessage());
            return;
        }
        RestoreShopUI.open(api, player, target != null ? target.getUniqueId() : player.getUniqueId());
    }

    @Subcommand("import")
    @CommandPermission(SlabbyPermissions.ADMIN_IMPORT)
    private void onImport(final Player player) {
        new SlabboImporter().onImport(api);

        player.sendMessage("import complete");
    }

}

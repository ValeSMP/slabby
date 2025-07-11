package gg.mew.slabby.listener;

import gg.mew.slabby.command.SlabboMapsCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class SlabboMapsListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        if (!SlabboMapsCommand.isRestrictedItem(event.getItemDrop().getItemStack()))
            return;

        event.getItemDrop().remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.DROP_ONE_SLOT || event.getAction() == InventoryAction.DROP_ALL_CURSOR || event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_CURSOR)
            return;

        if (SlabboMapsCommand.isRestrictedItem(event.getCursor()) && event.getClickedInventory() != event.getWhoClicked().getInventory()
                || event.isShiftClick() && SlabboMapsCommand.isRestrictedItem(event.getCurrentItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (event.getInventory() == event.getWhoClicked().getInventory())
            return;

        if (SlabboMapsCommand.isRestrictedItem(event.getOldCursor()))
            event.setCancelled(true);
    }

}

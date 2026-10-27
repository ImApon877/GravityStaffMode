package it.gravitymc.gravitystaffmode.listener;

import it.gravitymc.gravitystaffmode.data.PlayerStates;
import it.gravitymc.gravitystaffmode.manager.InvseeManager;
import it.gravitymc.gravitystaffmode.manager.StaffTools;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

/** Invsee menus and inventories, and the staff hotbar that must not be rearranged. */
public final class InventoryListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStates states;
    private final InvseeManager invsee;
    private final StaffTools tools;

    public InventoryListener(JavaPlugin plugin, PlayerStates states, InvseeManager invsee, StaffTools tools) {
        this.plugin = plugin;
        this.states = states;
        this.invsee = invsee;
        this.tools = tools;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        boolean staffMode = states.isInStaffMode(player);
        if (!staffMode && invsee.isIdle()) {
            return;
        }
        Inventory topInventory = event.getView().getTopInventory();
        if (invsee.isMenu(topInventory)) {
            event.setCancelled(true);
            if (event.getRawSlot() < topInventory.getSize()) {
                invsee.openSelected(player, event.getCurrentItem());
            }
            return;
        }
        if (invsee.isOpenBy(player, topInventory)) {
            // In staff mode the staff inventory is wiped on exit, so anything taken from the target
            // would be lost: invsee is read-only there.
            if (staffMode || tools.isAnyTool(event.getCurrentItem()) || tools.isAnyTool(event.getCursor())) {
                cancelAndResync(event, player);
            }
            return;
        }
        if (!staffMode) {
            return;
        }
        if (event.getInventory().getHolder() instanceof Player) {
            cancelAndResync(event, player);
            return;
        }
        if (tools.isAnyTool(event.getCurrentItem()) || tools.isAnyTool(event.getCursor())) {
            cancelAndResync(event, player);
        }
    }

    // A cancelled click is not always sent back to the client (1.8): without a resync it shows
    // ghost items, e.g. an item "taken" from a read-only invsee.
    private void cancelAndResync(InventoryClickEvent event, final Player player) {
        event.setCancelled(true);
        plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.updateInventory();
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        if (invsee.isMenu(topInventory) || (states.isInStaffMode(player) && invsee.isOpenBy(player, topInventory))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        invsee.handleClose((Player) event.getPlayer(), event.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (states.isInStaffMode((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }
}

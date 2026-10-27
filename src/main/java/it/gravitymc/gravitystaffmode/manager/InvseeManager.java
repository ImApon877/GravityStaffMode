package it.gravitymc.gravitystaffmode.manager;

import it.gravitymc.gravitystaffmode.Compat;
import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.data.PlayerStates;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

/** The player-selection menu and the inventories opened with invsee, tracked per viewer. */
public final class InvseeManager {
    private final Messages messages;
    private final PlayerStates states;
    private final Set<Inventory> menus = new HashSet<Inventory>();
    private final Map<UUID, Inventory> openInventories = new HashMap<UUID, Inventory>();

    public InvseeManager(Messages messages, PlayerStates states) {
        this.messages = messages;
        this.states = states;
    }

    public void openMenu(Player viewer) {
        Inventory menu = Bukkit.createInventory(null, 54, messages.get("invsee-titolo", "&8InvSee &7| &fGiocatori"));
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= menu.getSize()) {
                break;
            }
            ItemStack playerHead = Compat.playerHead();
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwner(target.getName());
            meta.setDisplayName(ChatColor.WHITE + target.getName());
            playerHead.setItemMeta(meta);
            menu.setItem(slot++, playerHead);
        }
        menus.add(menu);
        viewer.openInventory(menu);
    }

    /** Opens the inventory of the player whose head was clicked in the menu. */
    public void openSelected(Player viewer, ItemStack selectedItem) {
        if (selectedItem == null || selectedItem.getType() != Compat.playerHeadMaterial() || !selectedItem.hasItemMeta()) {
            return;
        }
        ItemMeta meta = selectedItem.getItemMeta();
        if (!meta.hasDisplayName()) {
            return;
        }
        String name = ChatColor.stripColor(meta.getDisplayName());
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            messages.send(viewer, "giocatore-non-trovato", name);
            return;
        }
        open(viewer, target);
    }

    public void open(Player viewer, Player target) {
        Inventory inventory = target.getInventory();
        // Register after opening: opening fires the close event of the previously open inventory.
        viewer.openInventory(inventory);
        openInventories.put(viewer.getUniqueId(), inventory);
        messages.send(viewer, states.isInStaffMode(viewer) ? "inventario-aperto-sola-lettura" : "inventario-aperto",
                target.getName());
    }

    public boolean isMenu(Inventory inventory) {
        return menus.contains(inventory);
    }

    /** True if {@code inventory} is the target inventory the viewer opened with invsee. */
    public boolean isOpenBy(Player viewer, Inventory inventory) {
        return inventory.equals(openInventories.get(viewer.getUniqueId()));
    }

    public boolean isIdle() {
        return menus.isEmpty() && openInventories.isEmpty();
    }

    public void handleClose(Player viewer, Inventory inventory) {
        menus.remove(inventory);
        if (isOpenBy(viewer, inventory)) {
            openInventories.remove(viewer.getUniqueId());
        }
    }

    public void forget(UUID viewer) {
        openInventories.remove(viewer);
    }
}

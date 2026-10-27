package it.gravitymc.gravitystaffmode.manager;

import it.gravitymc.gravitystaffmode.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** The items of the staff hotbar, recognised by material and configured name. */
public final class StaffTools {
    public enum Tool {
        VANISH(Material.INK_SACK, "strumento-vanish", 0),
        RANDOM_TP(Material.COMPASS, "strumento-randomtp", 1),
        FLIGHT(Material.FEATHER, "piuma-volo", 2),
        FREEZE(Material.BLAZE_ROD, "strumento-freeze", 7),
        INVSEE(Material.BOOK, "strumento-invsee", 8);

        private final Material material;
        private final String messageKey;
        private final int slot;

        Tool(Material material, String messageKey, int slot) {
            this.material = material;
            this.messageKey = messageKey;
            this.slot = slot;
        }
    }

    private final Messages messages;

    public StaffTools(Messages messages) {
        this.messages = messages;
    }

    /** Replaces the whole inventory with the staff tools. */
    public void fill(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        for (Tool tool : Tool.values()) {
            player.getInventory().setItem(tool.slot, create(tool));
        }
        player.updateInventory();
    }

    public boolean isTool(ItemStack item, Tool tool) {
        if (item == null || item.getType() != tool.material || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() && meta.getDisplayName().equals(name(tool));
    }

    public boolean isAnyTool(ItemStack item) {
        for (Tool tool : Tool.values()) {
            if (isTool(item, tool)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack create(Tool tool) {
        ItemStack item = new ItemStack(tool.material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name(tool));
        item.setItemMeta(meta);
        return item;
    }

    private String name(Tool tool) {
        return messages.get(tool.messageKey, tool.messageKey);
    }
}

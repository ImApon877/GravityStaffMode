package it.gravitymc.gravitystaffmode.manager;

import it.gravitymc.gravitystaffmode.Compat;
import it.gravitymc.gravitystaffmode.Messages;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** The items of the staff hotbar, recognised by material and configured name. */
public final class StaffTools {
    public enum Tool {
        VANISH("strumento-vanish", 0, "INK_SAC", "INK_SACK"),
        RANDOM_TP("strumento-randomtp", 1, "COMPASS"),
        FLIGHT("piuma-volo", 2, "FEATHER"),
        FREEZE("strumento-freeze", 7, "BLAZE_ROD"),
        INVSEE("strumento-invsee", 8, "BOOK");

        private final String messageKey;
        private final int slot;
        private final String[] materialNames;
        private Material material;

        Tool(String messageKey, int slot, String... materialNames) {
            this.messageKey = messageKey;
            this.slot = slot;
            this.materialNames = materialNames;
        }

        // Resolved by name: INK_SACK only exists up to 1.12, INK_SAC from 1.13.
        private Material material() {
            if (material == null) {
                material = Compat.material(materialNames);
            }
            return material;
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
        if (item == null || item.getType() != tool.material() || !item.hasItemMeta()) {
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
        ItemStack item = new ItemStack(tool.material(), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name(tool));
        item.setItemMeta(meta);
        return item;
    }

    private String name(Tool tool) {
        return messages.get(tool.messageKey, tool.messageKey);
    }
}

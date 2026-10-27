package it.gravitymc.gravitystaffmode;

import java.lang.reflect.Method;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * Differences between Minecraft 1.8 and modern versions. The plugin is compiled against 1.8,
 * so modern-only names and methods are resolved at runtime.
 */
public final class Compat {
    private Compat() {
    }

    /** The first material that exists on this server, e.g. {@code material("INK_SAC", "INK_SACK")}. */
    public static Material material(String... names) {
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                return material;
            }
        }
        throw new IllegalStateException("No material found among " + String.join(", ", names));
    }

    public static Material playerHeadMaterial() {
        return material("PLAYER_HEAD", "SKULL_ITEM");
    }

    public static ItemStack playerHead() {
        Material material = playerHeadMaterial();
        // On 1.8 the player head is SKULL_ITEM with data value 3.
        return material.name().equals("SKULL_ITEM") ? new ItemStack(material, 1, (short) 3) : new ItemStack(material, 1);
    }

    /** True for the off-hand copy of an interact event (1.9+ fires one event per hand). */
    public static boolean isOffHand(Event event) {
        try {
            Method getHand = event.getClass().getMethod("getHand");
            Object hand = getHand.invoke(event);
            return hand != null && "OFF_HAND".equals(hand.toString());
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}

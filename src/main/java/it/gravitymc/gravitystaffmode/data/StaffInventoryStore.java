package it.gravitymc.gravitystaffmode.data;

import it.gravitymc.gravitystaffmode.Utf8Yaml;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * What a staff member had before entering staff mode (inventory, armor, game mode, flight).
 * It is mirrored on disk so a crash (no onDisable) cannot lose the real inventory.
 */
public final class StaffInventoryStore {
    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Boolean> flightBefore = new HashMap<UUID, Boolean>();
    private final Map<UUID, GameMode> gameModeBefore = new HashMap<UUID, GameMode>();
    private final Map<UUID, ItemStack[]> inventoriesBefore = new HashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack[]> armorBefore = new HashMap<UUID, ItemStack[]>();
    private YamlConfiguration saved;

    public StaffInventoryStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "inventari-staffmode.yml");
    }

    public void load() {
        saved = Utf8Yaml.load(file);
        for (String key : saved.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("UUID non valido in inventari-staffmode.yml: " + key);
                continue;
            }
            inventoriesBefore.put(uuid, readItems(key + ".inventario", saved.getInt(key + ".dimensione-inventario", 36)));
            armorBefore.put(uuid, readItems(key + ".armatura", 4));
            String gameMode = saved.getString(key + ".gamemode");
            if (gameMode != null) {
                try {
                    gameModeBefore.put(uuid, GameMode.valueOf(gameMode));
                } catch (IllegalArgumentException ignored) {
                    // Unknown game mode: the player simply keeps the current one.
                }
            }
            if (saved.isBoolean(key + ".volo")) {
                flightBefore.put(uuid, saved.getBoolean(key + ".volo"));
            }
        }
    }

    public boolean hasSnapshot(Player player) {
        return inventoriesBefore.containsKey(player.getUniqueId());
    }

    /** Remembers the current state of the player, unless a snapshot already exists. */
    public void capture(Player player) {
        UUID uuid = player.getUniqueId();
        if (!flightBefore.containsKey(uuid)) {
            flightBefore.put(uuid, player.getAllowFlight());
        }
        if (!gameModeBefore.containsKey(uuid)) {
            gameModeBefore.put(uuid, player.getGameMode());
        }
        if (!inventoriesBefore.containsKey(uuid)) {
            inventoriesBefore.put(uuid, copyItems(player.getInventory().getContents()));
            armorBefore.put(uuid, copyItems(player.getInventory().getArmorContents()));
        }
        saveSnapshot(uuid);
    }

    /** Gives back flight, game mode and inventory, then forgets the snapshot. */
    public void restore(Player player) {
        UUID uuid = player.getUniqueId();
        Boolean couldFlyBefore = flightBefore.remove(uuid);
        if (couldFlyBefore != null) {
            player.setFlying(false);
            player.setAllowFlight(couldFlyBefore.booleanValue());
        }
        GameMode previousGameMode = gameModeBefore.remove(uuid);
        if (previousGameMode != null) {
            player.setGameMode(previousGameMode);
        }
        ItemStack[] inventory = inventoriesBefore.remove(uuid);
        ItemStack[] armor = armorBefore.remove(uuid);
        if (inventory != null && armor != null) {
            player.getInventory().clear();
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.updateInventory();
        }
        if (saved.contains(uuid.toString())) {
            saved.set(uuid.toString(), null);
            saveFile();
        }
    }

    private void saveSnapshot(UUID uuid) {
        ItemStack[] inventory = inventoriesBefore.get(uuid);
        ItemStack[] armor = armorBefore.get(uuid);
        if (inventory == null || armor == null) {
            return;
        }
        String key = uuid.toString();
        saved.set(key, null);
        saved.set(key + ".dimensione-inventario", inventory.length);
        writeItems(key + ".inventario", inventory);
        writeItems(key + ".armatura", armor);
        GameMode gameMode = gameModeBefore.get(uuid);
        if (gameMode != null) {
            saved.set(key + ".gamemode", gameMode.name());
        }
        Boolean flight = flightBefore.get(uuid);
        if (flight != null) {
            saved.set(key + ".volo", flight);
        }
        saveFile();
    }

    private void writeItems(String path, ItemStack[] items) {
        for (int index = 0; index < items.length; index++) {
            if (items[index] != null && items[index].getType() != Material.AIR) {
                saved.set(path + "." + index, items[index]);
            }
        }
    }

    private ItemStack[] readItems(String path, int size) {
        ItemStack[] items = new ItemStack[size];
        ConfigurationSection section = saved.getConfigurationSection(path);
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                if (index >= 0 && index < size) {
                    items[index] = section.getItemStack(key);
                }
            } catch (NumberFormatException ignored) {
                // Not a slot number: ignore the entry.
            }
        }
        return items;
    }

    private static ItemStack[] copyItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            copy[index] = items[index] == null ? null : items[index].clone();
        }
        return copy;
    }

    private void saveFile() {
        try {
            Utf8Yaml.save(saved, file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Impossibile salvare inventari-staffmode.yml: " + exception.getMessage());
        }
    }
}

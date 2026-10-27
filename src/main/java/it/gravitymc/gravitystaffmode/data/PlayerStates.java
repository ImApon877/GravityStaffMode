package it.gravitymc.gravitystaffmode.data;

import it.gravitymc.gravitystaffmode.Utf8Yaml;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Vanish and staff mode states, kept in dati.yml so they survive restarts. */
public final class PlayerStates {
    private static final String VANISHED_PATH = "giocatori-in-vanish";
    private static final String STAFF_MODE_PATH = "giocatori-in-staffmode";
    private static final String VANISHED_BEFORE_STAFF_MODE_PATH = "giocatori-in-vanish-prima-della-staffmode";
    private static final String[] PATHS = {VANISHED_PATH, STAFF_MODE_PATH, VANISHED_BEFORE_STAFF_MODE_PATH};
    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> vanished = new HashSet<UUID>();
    private final Set<UUID> staffMode = new HashSet<UUID>();
    private final Set<UUID> vanishedBeforeStaffMode = new HashSet<UUID>();
    private YamlConfiguration data;
    private boolean saveQueued;

    public PlayerStates(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dati.yml");
    }

    public void load() {
        data = Utf8Yaml.load(file);
        migrateFromConfig();
        loadUuidSet(VANISHED_PATH, vanished);
        loadUuidSet(STAFF_MODE_PATH, staffMode);
        loadUuidSet(VANISHED_BEFORE_STAFF_MODE_PATH, vanishedBeforeStaffMode);
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public void addVanished(UUID uuid) {
        vanished.add(uuid);
    }

    /** Returns true if the player was vanished. */
    public boolean removeVanished(UUID uuid) {
        return vanished.remove(uuid);
    }

    public boolean isInStaffMode(Player player) {
        return staffMode.contains(player.getUniqueId());
    }

    public void addStaffMode(UUID uuid) {
        staffMode.add(uuid);
    }

    /** Returns true if the player was in staff mode. */
    public boolean removeStaffMode(UUID uuid) {
        return staffMode.remove(uuid);
    }

    public void addVanishedBeforeStaffMode(UUID uuid) {
        vanishedBeforeStaffMode.add(uuid);
    }

    /** Returns true if the player was already vanished before entering staff mode. */
    public boolean removeVanishedBeforeStaffMode(UUID uuid) {
        return vanishedBeforeStaffMode.remove(uuid);
    }

    /** Saves on the next second, merging the changes made in the meantime into one write. */
    public void scheduleSave() {
        if (saveQueued) {
            return;
        }
        saveQueued = true;
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                saveQueued = false;
                saveNow();
            }
        }, 20L);
    }

    public void saveNow() {
        saveUuidSet(VANISHED_PATH, vanished);
        saveUuidSet(STAFF_MODE_PATH, staffMode);
        saveUuidSet(VANISHED_BEFORE_STAFF_MODE_PATH, vanishedBeforeStaffMode);
        saveFile();
    }

    // Older versions kept player states in config.yml: move them to dati.yml once.
    private void migrateFromConfig() {
        boolean migrated = false;
        for (String path : PATHS) {
            if (!plugin.getConfig().contains(path)) {
                continue;
            }
            if (!data.contains(path)) {
                data.set(path, plugin.getConfig().getStringList(path));
            }
            plugin.getConfig().set(path, null);
            migrated = true;
        }
        if (migrated) {
            plugin.saveConfig();
            saveFile();
        }
    }

    private void loadUuidSet(String path, Set<UUID> destination) {
        for (String value : data.getStringList(path)) {
            try {
                destination.add(UUID.fromString(value));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("UUID non valido in " + path + ": " + value);
            }
        }
    }

    private void saveUuidSet(String path, Set<UUID> values) {
        List<String> serializedValues = new ArrayList<String>();
        for (UUID uuid : values) {
            serializedValues.add(uuid.toString());
        }
        data.set(path, serializedValues);
    }

    private void saveFile() {
        try {
            Utf8Yaml.save(data, file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Impossibile salvare dati.yml: " + exception.getMessage());
        }
    }
}

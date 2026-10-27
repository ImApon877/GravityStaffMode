package it.gravitymc.gravitystaffmode;

import it.gravitymc.gravitystaffmode.command.FlyCommand;
import it.gravitymc.gravitystaffmode.command.InvseeCommand;
import it.gravitymc.gravitystaffmode.command.RandomTpCommand;
import it.gravitymc.gravitystaffmode.command.ReloadCommand;
import it.gravitymc.gravitystaffmode.command.StaffModeCommand;
import it.gravitymc.gravitystaffmode.command.VanishCommand;
import it.gravitymc.gravitystaffmode.data.PlayerStates;
import it.gravitymc.gravitystaffmode.data.StaffInventoryStore;
import it.gravitymc.gravitystaffmode.listener.ConnectionListener;
import it.gravitymc.gravitystaffmode.listener.FreezeListener;
import it.gravitymc.gravitystaffmode.listener.InventoryListener;
import it.gravitymc.gravitystaffmode.listener.StaffModeListener;
import it.gravitymc.gravitystaffmode.manager.FreezeManager;
import it.gravitymc.gravitystaffmode.manager.InvseeManager;
import it.gravitymc.gravitystaffmode.manager.StaffModeManager;
import it.gravitymc.gravitystaffmode.manager.StaffTools;
import it.gravitymc.gravitystaffmode.manager.VanishManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class GravityStaffModePlugin extends JavaPlugin {
    private Messages messages;
    private PlayerStates states;
    private VanishManager vanish;
    private StaffModeManager staffMode;
    private YamlConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        messages = new Messages(this);
        messages.load();
        states = new PlayerStates(this);
        states.load();
        StaffInventoryStore inventories = new StaffInventoryStore(this);
        inventories.load();

        StaffTools tools = new StaffTools(messages);
        vanish = new VanishManager(this, states);
        staffMode = new StaffModeManager(this, messages, states, vanish, inventories, tools);
        InvseeManager invsee = new InvseeManager(messages, states);
        FreezeManager freeze = new FreezeManager(this, messages);

        register("vanish", new VanishCommand(messages, states, vanish));
        register("staffmode", new StaffModeCommand(messages, staffMode));
        register("invsee", new InvseeCommand(messages, invsee));
        register("randomtp", new RandomTpCommand(messages, staffMode));
        register("fly", new FlyCommand(messages, staffMode));
        register("gravitystaffmode", new ReloadCommand(this, messages));

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ConnectionListener(this, states, staffMode, vanish, invsee, freeze), this);
        pluginManager.registerEvents(new StaffModeListener(messages, states, staffMode, tools, invsee, freeze), this);
        pluginManager.registerEvents(new InventoryListener(this, states, invsee, tools), this);
        pluginManager.registerEvents(new FreezeListener(freeze), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            staffMode.updatePlayerState(player);
        }
    }

    @Override
    public void onDisable() {
        if (states == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (states.isVanished(player)) {
                vanish.showToEveryone(player);
            }
            staffMode.disable(player);
            vanish.restoreTabName(player);
        }
        states.saveNow();
    }

    public void reloadPluginConfiguration() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        messages.load();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (states.isInStaffMode(player)) {
                staffMode.refreshTools(player);
            }
            staffMode.updatePlayerState(player);
        }
    }

    // config.yml is read and written as UTF-8 on every version (see Utf8Yaml).
    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    public void reloadConfig() {
        config = Utf8Yaml.load(new File(getDataFolder(), "config.yml"));
        InputStream defaults = getResource("config.yml");
        if (defaults == null) {
            return;
        }
        try {
            config.setDefaults(Utf8Yaml.load(defaults));
            defaults.close();
        } catch (IOException exception) {
            getLogger().warning("Impossibile leggere il config.yml predefinito: " + exception.getMessage());
        }
    }

    @Override
    public void saveConfig() {
        try {
            Utf8Yaml.save((YamlConfiguration) getConfig(), new File(getDataFolder(), "config.yml"));
        } catch (IOException exception) {
            getLogger().severe("Impossibile salvare config.yml: " + exception.getMessage());
        }
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}

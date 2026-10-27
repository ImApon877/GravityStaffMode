package it.gravitymc.gravitystaffmode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {
    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defaultsStream = plugin.getResource("messages.yml");
        if (defaultsStream == null) {
            return;
        }
        try {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
            messages.options().copyDefaults(true);
            messages.save(messagesFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Impossibile aggiornare messages.yml: " + exception.getMessage());
        } finally {
            try {
                defaultsStream.close();
            } catch (IOException ignored) {
                // Nothing to do: the messages were already loaded.
            }
        }
    }

    /** The colored text of a message, or {@code fallback} when it is missing. */
    public String get(String key, String fallback) {
        return color(messages.getString(key, fallback));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, "");
    }

    public void send(CommandSender sender, String key, String playerName) {
        String prefix = messages.getString("prefisso", "");
        String message = messages.getString(key, "&cMessaggio mancante: " + key);
        message = message.replace("{giocatore}", playerName);
        sender.sendMessage(color(prefix + message));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

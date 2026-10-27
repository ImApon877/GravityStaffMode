package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.GravityStaffModePlugin;
import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

public final class ReloadCommand implements TabExecutor {
    private final GravityStaffModePlugin plugin;
    private final Messages messages;

    public ReloadCommand(GravityStaffModePlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permissions.RELOAD)) {
            messages.send(sender, "nessun-permesso");
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            messages.send(sender, "uso-reload");
            return true;
        }
        plugin.reloadPluginConfiguration();
        messages.send(sender, "reload-completato");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission(Permissions.RELOAD)
                && "reload".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}

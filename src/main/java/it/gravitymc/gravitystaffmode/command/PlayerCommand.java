package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.Messages;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** A command only players can use, guarded by one permission. */
abstract class PlayerCommand implements TabExecutor {
    protected final Messages messages;
    private final String permission;

    PlayerCommand(Messages messages, String permission) {
        this.messages = messages;
        this.permission = permission;
    }

    protected abstract void execute(Player player, String[] args);

    @Override
    public final boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messages.send(sender, "solo-giocatori");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(permission)) {
            messages.send(player, "nessun-permesso");
            return true;
        }
        execute(player, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

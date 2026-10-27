package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import it.gravitymc.gravitystaffmode.manager.InvseeManager;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class InvseeCommand extends PlayerCommand {
    private final InvseeManager invsee;

    public InvseeCommand(Messages messages, InvseeManager invsee) {
        super(messages, Permissions.INVSEE);
        this.invsee = invsee;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 1) {
            messages.send(player, "uso-invsee");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            messages.send(player, "giocatore-non-trovato", args[0]);
            return;
        }
        invsee.open(player, target);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // null = default completion with the names of the players the sender can see.
        return null;
    }
}

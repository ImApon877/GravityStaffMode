package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import it.gravitymc.gravitystaffmode.data.PlayerStates;
import it.gravitymc.gravitystaffmode.manager.VanishManager;
import org.bukkit.entity.Player;

public final class VanishCommand extends PlayerCommand {
    private final PlayerStates states;
    private final VanishManager vanish;

    public VanishCommand(Messages messages, PlayerStates states, VanishManager vanish) {
        super(messages, Permissions.VANISH);
        this.states = states;
        this.vanish = vanish;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 0) {
            messages.send(player, "uso");
            return;
        }
        if (states.isInStaffMode(player)) {
            messages.send(player, "staffmode-attivata");
            return;
        }
        if (states.removeVanished(player.getUniqueId())) {
            vanish.removeVanish(player);
            messages.send(player, "disattivato");
        } else {
            states.addVanished(player.getUniqueId());
            vanish.applyVanish(player);
            messages.send(player, "attivato");
        }
        states.scheduleSave();
    }
}

package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import it.gravitymc.gravitystaffmode.manager.StaffModeManager;
import org.bukkit.entity.Player;

public final class RandomTpCommand extends PlayerCommand {
    private final StaffModeManager staffMode;

    public RandomTpCommand(Messages messages, StaffModeManager staffMode) {
        super(messages, Permissions.RANDOMTP);
        this.staffMode = staffMode;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 0) {
            messages.send(player, "uso-randomtp");
            return;
        }
        staffMode.randomTeleport(player);
    }
}

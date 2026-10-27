package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import it.gravitymc.gravitystaffmode.manager.StaffModeManager;
import org.bukkit.entity.Player;

public final class FlyCommand extends PlayerCommand {
    private final StaffModeManager staffMode;

    public FlyCommand(Messages messages, StaffModeManager staffMode) {
        super(messages, Permissions.FLY);
        this.staffMode = staffMode;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 0) {
            messages.send(player, "uso-fly");
            return;
        }
        staffMode.toggleFlight(player);
    }
}

package it.gravitymc.gravitystaffmode.command;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import it.gravitymc.gravitystaffmode.manager.StaffModeManager;
import org.bukkit.entity.Player;

public final class StaffModeCommand extends PlayerCommand {
    private final StaffModeManager staffMode;

    public StaffModeCommand(Messages messages, StaffModeManager staffMode) {
        super(messages, Permissions.STAFFMODE);
        this.staffMode = staffMode;
    }

    @Override
    protected void execute(Player player, String[] args) {
        if (args.length != 0) {
            messages.send(player, "uso-staffmode");
            return;
        }
        staffMode.toggle(player);
    }
}

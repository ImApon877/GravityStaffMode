package it.gravitymc.gravitystaffmode.manager;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class FreezeManager {
    private final JavaPlugin plugin;
    private final Messages messages;
    private final Set<UUID> frozenPlayers = new HashSet<UUID>();

    public FreezeManager(JavaPlugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void toggle(Player staff, Player target) {
        if (frozenPlayers.remove(target.getUniqueId())) {
            messages.send(staff, "giocatore-sfreezato", target.getName());
            messages.send(target, "sei-sfreezato");
        } else if (target.hasPermission(Permissions.FREEZE_BYPASS)) {
            messages.send(staff, "freeze-bypass", target.getName());
        } else {
            frozenPlayers.add(target.getUniqueId());
            messages.send(staff, "giocatore-freezato", target.getName());
            messages.send(target, "sei-freezato");
        }
    }

    /** A frozen player who logs out: warn the staff and run the configured command. */
    public void handleQuit(Player player) {
        if (!frozenPlayers.remove(player.getUniqueId())) {
            return;
        }
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.equals(player) && staff.hasPermission(Permissions.STAFFMODE)) {
                messages.send(staff, "giocatore-uscito-freezato", player.getName());
            }
        }
        String command = plugin.getConfig().getString("freeze.comando-uscita", "");
        if (command != null && !command.trim().isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{giocatore}", player.getName()));
        }
    }
}

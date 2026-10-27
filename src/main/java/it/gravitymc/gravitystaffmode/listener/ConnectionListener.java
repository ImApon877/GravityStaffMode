package it.gravitymc.gravitystaffmode.listener;

import it.gravitymc.gravitystaffmode.data.PlayerStates;
import it.gravitymc.gravitystaffmode.manager.FreezeManager;
import it.gravitymc.gravitystaffmode.manager.InvseeManager;
import it.gravitymc.gravitystaffmode.manager.StaffModeManager;
import it.gravitymc.gravitystaffmode.manager.VanishManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConnectionListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerStates states;
    private final StaffModeManager staffMode;
    private final VanishManager vanish;
    private final InvseeManager invsee;
    private final FreezeManager freeze;

    public ConnectionListener(JavaPlugin plugin, PlayerStates states, StaffModeManager staffMode,
                              VanishManager vanish, InvseeManager invsee, FreezeManager freeze) {
        this.plugin = plugin;
        this.states = states;
        this.staffMode = staffMode;
        this.vanish = vanish;
        this.invsee = invsee;
        this.freeze = freeze;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        staffMode.updatePlayerState(joining);
        if (states.isVanished(joining) && hideJoinQuitMessages()) {
            event.setJoinMessage(null);
        }
        vanish.hideVanishedFrom(joining);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (states.isVanished(player) && hideJoinQuitMessages()) {
            event.setQuitMessage(null);
        }
        staffMode.disable(player);
        vanish.forget(player.getUniqueId());
        invsee.forget(player.getUniqueId());
        freeze.handleQuit(player);
    }

    private boolean hideJoinQuitMessages() {
        return plugin.getConfig().getBoolean("nascondi-messaggi-accesso-uscita", true);
    }
}

package it.gravitymc.gravitystaffmode.listener;

import it.gravitymc.gravitystaffmode.manager.FreezeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class FreezeListener implements Listener {
    private final FreezeManager freeze;

    public FreezeListener(FreezeManager freeze) {
        this.freeze = freeze;
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenMove(PlayerMoveEvent event) {
        if (!freeze.isFrozen(event.getPlayer())) {
            return;
        }
        // Looking around is allowed, changing block is not.
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenInteract(PlayerInteractEvent event) {
        if (freeze.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenTeleport(PlayerTeleportEvent event) {
        if (freeze.isFrozen(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}

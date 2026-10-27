package it.gravitymc.gravitystaffmode.listener;

import it.gravitymc.gravitystaffmode.manager.FreezeManager;
import org.bukkit.Location;
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
        // Looking around and falling are allowed; walking and jumping are not. Falling stays allowed so a
        // player frozen in mid-air is not kicked for flying. Cancel instead of setTo(from): on 1.8 setTo is
        // applied with a teleport, which onFrozenTeleport below would cancel too.
        Location from = event.getFrom();
        Location to = event.getTo();
        if (Math.abs(to.getX() - from.getX()) > 0.001 || Math.abs(to.getZ() - from.getZ()) > 0.001
                || to.getY() > from.getY() + 0.001) {
            event.setCancelled(true);
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

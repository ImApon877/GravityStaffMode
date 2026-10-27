package it.gravitymc.gravitystaffmode.manager;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.data.PlayerStates;
import it.gravitymc.gravitystaffmode.data.StaffInventoryStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffModeManager {
    private final JavaPlugin plugin;
    private final Messages messages;
    private final PlayerStates states;
    private final VanishManager vanish;
    private final StaffInventoryStore inventories;
    private final StaffTools tools;
    private final Random random = new Random();
    // Who turned flight on with /fly or the feather. Tracked here because other plugins
    // (e.g. a hub double jump) also grant allowFlight, which must not count as "flying".
    private final Set<UUID> flightEnabled = new HashSet<UUID>();

    public StaffModeManager(JavaPlugin plugin, Messages messages, PlayerStates states, VanishManager vanish,
                            StaffInventoryStore inventories, StaffTools tools) {
        this.plugin = plugin;
        this.messages = messages;
        this.states = states;
        this.vanish = vanish;
        this.inventories = inventories;
        this.tools = tools;
    }

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (states.removeStaffMode(uuid)) {
            disable(player);
            if (states.removeVanishedBeforeStaffMode(uuid) && states.isVanished(player)) {
                vanish.applyVanish(player);
            } else {
                states.removeVanished(uuid);
                vanish.removeVanish(player);
            }
            messages.send(player, "staffmode-disattivata");
        } else {
            if (states.isVanished(player)) {
                states.addVanishedBeforeStaffMode(uuid);
            } else {
                states.addVanished(uuid);
            }
            states.addStaffMode(uuid);
            enable(player);
            vanish.applyVanish(player);
            messages.send(player, "staffmode-attivata");
        }
        states.scheduleSave();
    }

    /** The vanish tool of the staff hotbar. */
    public void toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        if (states.removeVanished(uuid)) {
            states.removeVanishedBeforeStaffMode(uuid);
            vanish.removeVanish(player);
            messages.send(player, "disattivato");
        } else {
            states.addVanished(uuid);
            vanish.applyVanish(player);
            messages.send(player, "attivato");
        }
        states.scheduleSave();
    }

    public void enable(Player player) {
        inventories.capture(player);
        player.setGameMode(staffGameMode());
        flightEnabled.remove(player.getUniqueId());
        player.setFlying(false);
        player.setAllowFlight(false);
        tools.fill(player);
    }

    public void disable(Player player) {
        flightEnabled.remove(player.getUniqueId());
        inventories.restore(player);
    }

    public void forget(UUID uuid) {
        flightEnabled.remove(uuid);
    }

    /** Hands out the tools again, e.g. after a reload changed their names. */
    public void refreshTools(Player player) {
        inventories.capture(player);
        tools.fill(player);
    }

    /** Applies the saved vanish/staff mode state, on join and after a reload. */
    public void updatePlayerState(Player player) {
        if (!states.isInStaffMode(player) && inventories.hasSnapshot(player)) {
            // A snapshot left behind by a crash for a player no longer in staff mode: give the real inventory back.
            disable(player);
        }
        if (states.isInStaffMode(player)) {
            enable(player);
            if (states.isVanished(player)) {
                vanish.applyVanish(player);
            } else {
                vanish.removeVanish(player);
            }
            return;
        }
        if (!states.isVanished(player)) {
            vanish.restoreTabName(player);
            return;
        }
        vanish.applyVanish(player);
    }

    public void toggleFlight(Player player) {
        boolean enabled = flightEnabled.add(player.getUniqueId());
        if (!enabled) {
            flightEnabled.remove(player.getUniqueId());
        }
        if (enabled) {
            player.setAllowFlight(true);
            player.setFlying(true);
        } else {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        messages.send(player, enabled ? "volo-attivato" : "volo-disattivato");
    }

    public void randomTeleport(Player player) {
        List<Player> targets = new ArrayList<Player>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player) && !states.isVanished(onlinePlayer)) {
                targets.add(onlinePlayer);
            }
        }
        if (targets.isEmpty()) {
            messages.send(player, "nessun-giocatore-rtp");
            return;
        }
        Player target = targets.get(random.nextInt(targets.size()));
        player.teleport(target.getLocation());
        messages.send(player, "randomtp", target.getName());
    }

    private GameMode staffGameMode() {
        String configured = plugin.getConfig().getString("gamemode-staffmode", "SURVIVAL");
        try {
            return GameMode.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("gamemode-staffmode non valida: " + configured + ", uso SURVIVAL.");
            return GameMode.SURVIVAL;
        }
    }
}

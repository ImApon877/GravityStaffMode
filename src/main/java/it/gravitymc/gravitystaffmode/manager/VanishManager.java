package it.gravitymc.gravitystaffmode.manager;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.Permissions;
import it.gravitymc.gravitystaffmode.data.PlayerStates;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Who can see vanished players, and the [V]/[S] prefix in the tab list. */
public final class VanishManager {
    private static final String VANISH_TAB_PATH = "tab.prefisso-vanish";
    private static final String STAFF_MODE_TAB_PATH = "tab.prefisso-staffmode";
    private final JavaPlugin plugin;
    private final PlayerStates states;
    private final Map<UUID, String> originalTabNames = new HashMap<UUID, String>();
    private final Set<UUID> queuedTabUpdates = new HashSet<UUID>();
    private boolean tabUpdateQueued;

    public VanishManager(JavaPlugin plugin, PlayerStates states) {
        this.plugin = plugin;
        this.states = states;
    }

    /** Hides the player from whoever may not see them and shows the vanish prefix in tab. */
    public void applyVanish(Player player) {
        hideFromUnauthorizedPlayers(player);
        setTabName(player);
        scheduleTabNameUpdate(player.getUniqueId());
    }

    /** Makes the player visible again and restores the original tab name. */
    public void removeVanish(Player player) {
        showToEveryone(player);
        restoreTabName(player);
    }

    /** Hides the already vanished players from someone who just joined. */
    public void hideVanishedFrom(Player joining) {
        for (Player vanished : Bukkit.getOnlinePlayers()) {
            if (!joining.equals(vanished) && states.isVanished(vanished) && !canSee(joining, vanished)) {
                joining.hidePlayer(vanished);
            }
        }
    }

    public void showToEveryone(Player vanished) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(vanished)) {
                viewer.showPlayer(vanished);
            }
        }
    }

    public void restoreTabName(Player player) {
        String originalName = originalTabNames.remove(player.getUniqueId());
        if (originalName != null) {
            player.setPlayerListName(originalName);
        }
    }

    public void forget(UUID uuid) {
        originalTabNames.remove(uuid);
    }

    private void hideFromUnauthorizedPlayers(Player vanished) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(vanished)) {
                if (canSee(viewer, vanished)) {
                    viewer.showPlayer(vanished);
                } else {
                    viewer.hidePlayer(vanished);
                }
            }
        }
    }

    private boolean canSee(Player viewer, Player vanished) {
        return states.isInStaffMode(vanished)
                ? viewer.hasPermission(Permissions.STAFFMODE)
                : viewer.hasPermission(Permissions.SEE_VANISH);
    }

    private void setTabName(Player player) {
        UUID uuid = player.getUniqueId();
        if (!originalTabNames.containsKey(uuid)) {
            originalTabNames.put(uuid, player.getPlayerListName());
        }
        String originalName = originalTabNames.get(uuid);
        String configurationPath = states.isInStaffMode(player) ? STAFF_MODE_TAB_PATH : VANISH_TAB_PATH;
        String prefix = plugin.getConfig().getString(configurationPath, "{prefisso}");
        String name = Messages.color(prefix.replace("{prefisso}", originalName));
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        player.setPlayerListName(name);
    }

    // Other plugins (ranks, TAB) may overwrite the tab name right after: set it again on the next tick.
    private void scheduleTabNameUpdate(UUID uuid) {
        queuedTabUpdates.add(uuid);
        if (tabUpdateQueued) {
            return;
        }
        tabUpdateQueued = true;
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Set<UUID> updates = new HashSet<UUID>(queuedTabUpdates);
                queuedTabUpdates.clear();
                tabUpdateQueued = false;
                for (UUID uuid : updates) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && states.isVanished(player)) {
                        setTabName(player);
                    }
                }
            }
        });
    }
}

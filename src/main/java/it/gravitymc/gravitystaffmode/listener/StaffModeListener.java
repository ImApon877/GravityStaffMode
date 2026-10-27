package it.gravitymc.gravitystaffmode.listener;

import it.gravitymc.gravitystaffmode.Messages;
import it.gravitymc.gravitystaffmode.data.PlayerStates;
import it.gravitymc.gravitystaffmode.manager.FreezeManager;
import it.gravitymc.gravitystaffmode.manager.InvseeManager;
import it.gravitymc.gravitystaffmode.manager.StaffModeManager;
import it.gravitymc.gravitystaffmode.manager.StaffTools;
import it.gravitymc.gravitystaffmode.manager.StaffTools.Tool;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

/** Staff tools, plus the protections that keep staff in staff mode from affecting the game. */
public final class StaffModeListener implements Listener {
    private final Messages messages;
    private final PlayerStates states;
    private final StaffModeManager staffMode;
    private final StaffTools tools;
    private final InvseeManager invsee;
    private final FreezeManager freeze;

    public StaffModeListener(Messages messages, PlayerStates states, StaffModeManager staffMode, StaffTools tools,
                             InvseeManager invsee, FreezeManager freeze) {
        this.messages = messages;
        this.states = states;
        this.staffMode = staffMode;
        this.tools = tools;
        this.invsee = invsee;
        this.freeze = freeze;
    }

    @EventHandler
    public void onStaffToolUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!states.isInStaffMode(player)) {
            return;
        }
        ItemStack item = event.getItem();
        if (tools.isTool(item, Tool.FLIGHT)) {
            event.setCancelled(true);
            staffMode.toggleFlight(player);
        } else if (tools.isTool(item, Tool.INVSEE)) {
            event.setCancelled(true);
            invsee.openMenu(player);
        } else if (tools.isTool(item, Tool.VANISH)) {
            event.setCancelled(true);
            staffMode.toggleVanish(player);
        } else if (tools.isTool(item, Tool.RANDOM_TP)) {
            event.setCancelled(true);
            staffMode.randomTeleport(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStaffToolEntityUse(PlayerInteractEntityEvent event) {
        Player staff = event.getPlayer();
        if (!states.isInStaffMode(staff) || !(event.getRightClicked() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getRightClicked();
        ItemStack item = staff.getItemInHand();
        if (tools.isTool(item, Tool.INVSEE)) {
            event.setCancelled(true);
            invsee.open(staff, target);
        } else if (tools.isTool(item, Tool.FREEZE)) {
            event.setCancelled(true);
            freeze.toggle(staff, target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStaffToolDrop(PlayerDropItemEvent event) {
        if (states.isInStaffMode(event.getPlayer()) && tools.isAnyTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && states.isInStaffMode((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && states.isInStaffMode((Player) event.getEntity())) {
            event.setCancelled(true);
            ((Player) event.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && states.isInStaffMode((Player) event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (states.isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (states.isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (states.isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (states.isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGiveCommand(PlayerCommandPreprocessEvent event) {
        if (!states.isInStaffMode(event.getPlayer())) {
            return;
        }
        String command = event.getMessage().toLowerCase(Locale.ROOT).split(" ", 2)[0];
        if (command.equals("/give") || command.equals("/minecraft:give")
                || command.equals("/item") || command.equals("/i")) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "nessun-permesso");
        }
    }
}

package it.gravitymc.gravitystaffmode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class GravityStaffModePlugin extends JavaPlugin implements Listener {
    private static final String VANISH_PERMISSION = "gravityvanish.use";
    private static final String SEE_VANISH_PERMISSION = "gravityvanish.see";
    private static final String STAFFMODE_PERMISSION = "gravityvanish.staffmode";
    private static final String INVSEE_PERMISSION = "gravityvanish.invsee";
    private static final String RANDOMTP_PERMISSION = "gravityvanish.randomtp";
    private static final String FLY_PERMISSION = "gravityvanish.fly";
    private static final String RELOAD_PERMISSION = "gravitystaffmode.reload";
    private static final String FREEZE_BYPASS_PERMISSION = "gravityvanish.freeze.bypass";
    private final Set<UUID> vanishedPlayers = new HashSet<UUID>();
    private final Set<UUID> staffModePlayers = new HashSet<UUID>();
    private final Set<UUID> vanishedBeforeStaffMode = new HashSet<UUID>();
    private final Set<UUID> frozenPlayers = new HashSet<UUID>();
    private final Set<Inventory> invseeMenus = new HashSet<Inventory>();
    private final Map<UUID, Inventory> openInvseeInventories = new HashMap<UUID, Inventory>();
    private final Map<UUID, String> originalTabNames = new HashMap<UUID, String>();
    private final Map<UUID, Boolean> flightBeforeStaffMode = new HashMap<UUID, Boolean>();
    private final Map<UUID, GameMode> gameModeBeforeStaffMode = new HashMap<UUID, GameMode>();
    private final Map<UUID, ItemStack[]> inventoriesBeforeStaffMode = new HashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack[]> armorBeforeStaffMode = new HashMap<UUID, ItemStack[]>();
    private final Set<UUID> queuedTabUpdates = new HashSet<UUID>();
    private final Random random = new Random();
    private FileConfiguration messages;
    private File staffInventoriesFile;
    private YamlConfiguration savedStaffInventories;
    private boolean tabUpdateQueued;
    private boolean playerStateSaveQueued;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        saveResource("messages.yml", false);
        loadMessages();
        loadPlayerStates();
        loadSavedStaffInventories();
        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerState(player);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isVanished(player)) {
                showToEveryone(player);
            }
            disableStaffTools(player);
            restoreTabName(player);
        }
        savePlayerStatesNow();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName = command.getName().toLowerCase();
        if (commandName.equals("vanish")) {
            return handleVanish(sender, args);
        }
        if (commandName.equals("staffmode")) {
            return handleStaffMode(sender, args);
        }
        if (commandName.equals("invsee")) {
            return handleInvsee(sender, args);
        }
        if (commandName.equals("randomtp")) {
            return handleRandomTeleport(sender, args);
        }
        if (commandName.equals("fly")) {
            return handleFly(sender, args);
        }
        if (commandName.equals("gravitystaffmode")) {
            return handleReload(sender, args);
        }
        return false;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joining = event.getPlayer();
        updatePlayerState(joining);
        if (isVanished(joining) && getConfig().getBoolean("nascondi-messaggi-accesso-uscita", true)) {
            event.setJoinMessage(null);
        }
        for (Player vanished : Bukkit.getOnlinePlayers()) {
            if (!joining.equals(vanished) && isVanished(vanished) && !canSee(joining, vanished)) {
                joining.hidePlayer(vanished);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isVanished(player) && getConfig().getBoolean("nascondi-messaggi-accesso-uscita", true)) {
            event.setQuitMessage(null);
        }
        disableStaffTools(player);
        originalTabNames.remove(player.getUniqueId());
        flightBeforeStaffMode.remove(player.getUniqueId());
        openInvseeInventories.remove(player.getUniqueId());
        if (frozenPlayers.remove(player.getUniqueId())) {
            handleFrozenQuit(player);
        }
    }

    private void handleFrozenQuit(Player player) {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.equals(player) && staff.hasPermission(STAFFMODE_PERMISSION)) {
                sendMessage(staff, "giocatore-uscito-freezato", player.getName());
            }
        }
        String command = getConfig().getString("freeze.comando-uscita", "");
        if (command != null && !command.trim().isEmpty()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("{giocatore}", player.getName()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isInStaffMode((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && isInStaffMode((Player) event.getEntity())) {
            event.setCancelled(true);
            ((Player) event.getEntity()).setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getTarget() instanceof Player && isInStaffMode((Player) event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(PlayerPickupItemEvent event) {
        if (isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStaffToolUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInStaffMode(player)) {
            return;
        }
        if (isFlightFeather(event.getItem())) {
            event.setCancelled(true);
            toggleFlight(player);
        } else if (isStaffTool(event.getItem(), Material.BOOK, "strumento-invsee")) {
            event.setCancelled(true);
            openInvseeMenu(player);
        } else if (isStaffTool(event.getItem(), Material.INK_SACK, "strumento-vanish")) {
            event.setCancelled(true);
            toggleStaffVanish(player);
        } else if (isStaffTool(event.getItem(), Material.COMPASS, "strumento-randomtp")) {
            event.setCancelled(true);
            randomTeleport(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStaffToolEntityUse(PlayerInteractEntityEvent event) {
        Player staff = event.getPlayer();
        if (!isInStaffMode(staff) || !(event.getRightClicked() instanceof Player)) {
            return;
        }
        Player target = (Player) event.getRightClicked();
        ItemStack item = staff.getItemInHand();
        if (isStaffTool(item, Material.BOOK, "strumento-invsee")) {
            event.setCancelled(true);
            openEditableInventory(staff, target);
        } else if (isStaffTool(item, Material.BLAZE_ROD, "strumento-freeze")) {
            event.setCancelled(true);
            toggleFreeze(staff, target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenMove(PlayerMoveEvent event) {
        if (!frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenInteract(PlayerInteractEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFrozenTeleport(PlayerTeleportEvent event) {
        if (frozenPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onStaffToolDrop(PlayerDropItemEvent event) {
        if (isInStaffMode(event.getPlayer()) && isStaffTool(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        boolean staffMode = isInStaffMode(player);
        if (!staffMode && invseeMenus.isEmpty() && openInvseeInventories.isEmpty()) {
            return;
        }
        Inventory topInventory = event.getView().getTopInventory();
        if (invseeMenus.contains(topInventory)) {
            event.setCancelled(true);
            if (event.getRawSlot() < topInventory.getSize()) {
                openSelectedInventory(player, event.getCurrentItem());
            }
            return;
        }
        if (topInventory.equals(openInvseeInventories.get(player.getUniqueId()))) {
            // In staff mode the staff inventory is wiped on exit, so anything taken from the target
            // would be lost: invsee is read-only there.
            if (staffMode || isStaffTool(event.getCurrentItem()) || isStaffTool(event.getCursor())) {
                event.setCancelled(true);
            }
            return;
        }
        if (!staffMode) {
            return;
        }
        if (event.getInventory().getHolder() instanceof Player) {
            event.setCancelled(true);
            return;
        }
        if (isStaffTool(event.getCurrentItem()) || isStaffTool(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        if (invseeMenus.contains(topInventory)
                || (isInStaffMode(player) && topInventory.equals(openInvseeInventories.get(player.getUniqueId())))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        invseeMenus.remove(event.getInventory());
        Inventory openInvsee = openInvseeInventories.get(event.getPlayer().getUniqueId());
        if (event.getInventory().equals(openInvsee)) {
            openInvseeInventories.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (isInStaffMode((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (isInStaffMode(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGiveCommand(PlayerCommandPreprocessEvent event) {
        if (!isInStaffMode(event.getPlayer())) {
            return;
        }
        String command = event.getMessage().toLowerCase(Locale.ROOT).split(" ", 2)[0];
        if (command.equals("/give") || command.equals("/minecraft:give")
                || command.equals("/item") || command.equals("/i")) {
            event.setCancelled(true);
            sendMessage(event.getPlayer(), "nessun-permesso");
        }
    }

    private boolean handleVanish(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, VANISH_PERMISSION);
        if (player == null) {
            return true;
        }
        if (args.length != 0) {
            sendMessage(player, "uso");
            return true;
        }
        if (isInStaffMode(player)) {
            sendMessage(player, "staffmode-attivata");
            return true;
        }
        if (vanishedPlayers.remove(player.getUniqueId())) {
            showToEveryone(player);
            restoreTabName(player);
            sendMessage(player, "disattivato");
        } else {
            vanishedPlayers.add(player.getUniqueId());
            hideFromUnauthorizedPlayers(player);
            setTabName(player, "tab.prefisso-vanish");
            scheduleTabNameUpdate(player.getUniqueId());
            sendMessage(player, "attivato");
        }
        savePlayerStates();
        return true;
    }

    private boolean handleStaffMode(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, STAFFMODE_PERMISSION);
        if (player == null) {
            return true;
        }
        if (args.length != 0) {
            sendMessage(player, "uso");
            return true;
        }
        UUID uuid = player.getUniqueId();
        if (staffModePlayers.remove(uuid)) {
            disableStaffTools(player);
            if (vanishedBeforeStaffMode.remove(uuid) && isVanished(player)) {
                hideFromUnauthorizedPlayers(player);
                setTabName(player, "tab.prefisso-vanish");
                scheduleTabNameUpdate(player.getUniqueId());
            } else {
                vanishedPlayers.remove(uuid);
                showToEveryone(player);
                restoreTabName(player);
            }
            sendMessage(player, "staffmode-disattivata");
        } else {
            if (isVanished(player)) {
                vanishedBeforeStaffMode.add(uuid);
            } else {
                vanishedPlayers.add(uuid);
            }
            staffModePlayers.add(uuid);
            enableStaffTools(player);
            hideFromUnauthorizedPlayers(player);
            setTabName(player, "tab.prefisso-staffmode");
            scheduleTabNameUpdate(player.getUniqueId());
            sendMessage(player, "staffmode-attivata");
        }
        savePlayerStates();
        return true;
    }

    private boolean handleInvsee(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, INVSEE_PERMISSION);
        if (player == null) {
            return true;
        }
        if (args.length != 1) {
            sendMessage(player, "uso-invsee");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sendMessage(player, "giocatore-non-trovato", args[0]);
            return true;
        }
        openEditableInventory(player, target);
        return true;
    }

    private boolean handleRandomTeleport(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, RANDOMTP_PERMISSION);
        if (player == null) {
            return true;
        }
        if (args.length != 0) {
            sendMessage(player, "uso");
            return true;
        }
        randomTeleport(player);
        return true;
    }

    private boolean handleFly(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender, FLY_PERMISSION);
        if (player == null) {
            return true;
        }
        if (args.length != 0) {
            sendMessage(player, "uso");
            return true;
        }
        toggleFlight(player);
        return true;
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sendMessage(sender, "nessun-permesso");
            return true;
        }
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sendMessage(sender, "uso-reload");
            return true;
        }
        reloadPluginConfiguration();
        sendMessage(sender, "reload-completato");
        return true;
    }

    private Player requirePlayer(CommandSender sender, String permission) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "solo-giocatori");
            return null;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(permission)) {
            sendMessage(player, "nessun-permesso");
            return null;
        }
        return player;
    }

    private void enableStaffTools(Player player) {
        UUID uuid = player.getUniqueId();
        if (!flightBeforeStaffMode.containsKey(uuid)) {
            flightBeforeStaffMode.put(uuid, player.getAllowFlight());
        }
        if (!gameModeBeforeStaffMode.containsKey(uuid)) {
            gameModeBeforeStaffMode.put(uuid, player.getGameMode());
        }
        player.setGameMode(staffGameMode());
        player.setFlying(false);
        player.setAllowFlight(false);
        createStaffInventory(player);
        saveStaffSnapshot(uuid);
    }

    private GameMode staffGameMode() {
        String configured = getConfig().getString("gamemode-staffmode", "SURVIVAL");
        try {
            return GameMode.valueOf(configured.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            getLogger().warning("gamemode-staffmode non valida: " + configured + ", uso SURVIVAL.");
            return GameMode.SURVIVAL;
        }
    }

    private void disableStaffTools(Player player) {
        Boolean couldFlyBefore = flightBeforeStaffMode.remove(player.getUniqueId());
        if (couldFlyBefore != null) {
            player.setFlying(false);
            player.setAllowFlight(couldFlyBefore.booleanValue());
        }
        GameMode gameModeBefore = gameModeBeforeStaffMode.remove(player.getUniqueId());
        if (gameModeBefore != null) {
            player.setGameMode(gameModeBefore);
        }
        restorePlayerInventory(player);
    }

    private void createStaffInventory(Player player) {
        UUID uuid = player.getUniqueId();
        if (!inventoriesBeforeStaffMode.containsKey(uuid)) {
            inventoriesBeforeStaffMode.put(uuid, copyItems(player.getInventory().getContents()));
            armorBeforeStaffMode.put(uuid, copyItems(player.getInventory().getArmorContents()));
        }
        fillStaffInventory(player);
    }

    private void fillStaffInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItem(0, createTool(Material.INK_SACK, "strumento-vanish"));
        player.getInventory().setItem(1, createTool(Material.COMPASS, "strumento-randomtp"));
        player.getInventory().setItem(2, createFlightFeather());
        player.getInventory().setItem(7, createTool(Material.BLAZE_ROD, "strumento-freeze"));
        player.getInventory().setItem(8, createTool(Material.BOOK, "strumento-invsee"));
        player.updateInventory();
    }

    private ItemStack createFlightFeather() {
        ItemStack feather = new ItemStack(Material.FEATHER, 1);
        ItemMeta meta = feather.getItemMeta();
        meta.setDisplayName(color(messages.getString("piuma-volo", "&bPiuma del Volo")));
        feather.setItemMeta(meta);
        return feather;
    }

    private void restorePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] inventory = inventoriesBeforeStaffMode.remove(uuid);
        ItemStack[] armor = armorBeforeStaffMode.remove(uuid);
        if (inventory != null && armor != null) {
            player.getInventory().clear();
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.updateInventory();
        }
        removeStaffSnapshot(uuid);
    }

    // The pre-staff-mode state is mirrored on disk so a crash (no onDisable) cannot lose the real inventory.
    private void loadSavedStaffInventories() {
        staffInventoriesFile = new File(getDataFolder(), "inventari-staffmode.yml");
        savedStaffInventories = YamlConfiguration.loadConfiguration(staffInventoriesFile);
        for (String key : savedStaffInventories.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                getLogger().warning("UUID non valido in inventari-staffmode.yml: " + key);
                continue;
            }
            inventoriesBeforeStaffMode.put(uuid, readItems(key + ".inventario",
                    savedStaffInventories.getInt(key + ".dimensione-inventario", 36)));
            armorBeforeStaffMode.put(uuid, readItems(key + ".armatura", 4));
            String gameMode = savedStaffInventories.getString(key + ".gamemode");
            if (gameMode != null) {
                try {
                    gameModeBeforeStaffMode.put(uuid, GameMode.valueOf(gameMode));
                } catch (IllegalArgumentException ignored) {
                    // Unknown game mode: the player simply keeps the current one.
                }
            }
            if (savedStaffInventories.isBoolean(key + ".volo")) {
                flightBeforeStaffMode.put(uuid, savedStaffInventories.getBoolean(key + ".volo"));
            }
        }
    }

    private void saveStaffSnapshot(UUID uuid) {
        ItemStack[] inventory = inventoriesBeforeStaffMode.get(uuid);
        ItemStack[] armor = armorBeforeStaffMode.get(uuid);
        if (inventory == null || armor == null) {
            return;
        }
        String key = uuid.toString();
        savedStaffInventories.set(key, null);
        savedStaffInventories.set(key + ".dimensione-inventario", inventory.length);
        writeItems(key + ".inventario", inventory);
        writeItems(key + ".armatura", armor);
        GameMode gameMode = gameModeBeforeStaffMode.get(uuid);
        if (gameMode != null) {
            savedStaffInventories.set(key + ".gamemode", gameMode.name());
        }
        Boolean flight = flightBeforeStaffMode.get(uuid);
        if (flight != null) {
            savedStaffInventories.set(key + ".volo", flight);
        }
        saveStaffInventoriesFile();
    }

    private void removeStaffSnapshot(UUID uuid) {
        if (savedStaffInventories != null && savedStaffInventories.contains(uuid.toString())) {
            savedStaffInventories.set(uuid.toString(), null);
            saveStaffInventoriesFile();
        }
    }

    private void writeItems(String path, ItemStack[] items) {
        for (int index = 0; index < items.length; index++) {
            if (items[index] != null && items[index].getType() != Material.AIR) {
                savedStaffInventories.set(path + "." + index, items[index]);
            }
        }
    }

    private ItemStack[] readItems(String path, int size) {
        ItemStack[] items = new ItemStack[size];
        ConfigurationSection section = savedStaffInventories.getConfigurationSection(path);
        if (section == null) {
            return items;
        }
        for (String key : section.getKeys(false)) {
            try {
                int index = Integer.parseInt(key);
                if (index >= 0 && index < size) {
                    items[index] = section.getItemStack(key);
                }
            } catch (NumberFormatException ignored) {
                // Not a slot number: ignore the entry.
            }
        }
        return items;
    }

    private void saveStaffInventoriesFile() {
        try {
            savedStaffInventories.save(staffInventoriesFile);
        } catch (IOException exception) {
            getLogger().severe("Impossibile salvare inventari-staffmode.yml: " + exception.getMessage());
        }
    }

    private boolean isFlightFeather(ItemStack item) {
        return isStaffTool(item, Material.FEATHER, "piuma-volo");
    }

    private ItemStack createTool(Material material, String messageKey) {
        ItemStack tool = new ItemStack(material, 1);
        ItemMeta meta = tool.getItemMeta();
        meta.setDisplayName(color(messages.getString(messageKey, messageKey)));
        tool.setItemMeta(meta);
        return tool;
    }

    private boolean isStaffTool(ItemStack item) {
        return isFlightFeather(item)
                || isStaffTool(item, Material.BOOK, "strumento-invsee")
                || isStaffTool(item, Material.INK_SACK, "strumento-vanish")
                || isStaffTool(item, Material.COMPASS, "strumento-randomtp")
                || isStaffTool(item, Material.BLAZE_ROD, "strumento-freeze");
    }

    private boolean isStaffTool(ItemStack item, Material material, String messageKey) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName()
                && meta.getDisplayName().equals(color(messages.getString(messageKey, messageKey)));
    }

    private ItemStack[] copyItems(ItemStack[] items) {
        ItemStack[] copy = new ItemStack[items.length];
        for (int index = 0; index < items.length; index++) {
            copy[index] = items[index] == null ? null : items[index].clone();
        }
        return copy;
    }

    private void openInvseeMenu(Player viewer) {
        Inventory menu = Bukkit.createInventory(null, 54,
                color(messages.getString("invsee-titolo", "&8InvSee &7| &fGiocatori")));
        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= menu.getSize()) {
                break;
            }
            ItemStack playerHead = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
            SkullMeta meta = (SkullMeta) playerHead.getItemMeta();
            meta.setOwner(target.getName());
            meta.setDisplayName(ChatColor.WHITE + target.getName());
            playerHead.setItemMeta(meta);
            menu.setItem(slot++, playerHead);
        }
        invseeMenus.add(menu);
        viewer.openInventory(menu);
    }

    private void openSelectedInventory(Player viewer, ItemStack selectedItem) {
        if (selectedItem == null || selectedItem.getType() != Material.SKULL_ITEM || !selectedItem.hasItemMeta()) {
            return;
        }
        ItemMeta meta = selectedItem.getItemMeta();
        if (!meta.hasDisplayName()) {
            return;
        }
        String name = ChatColor.stripColor(meta.getDisplayName());
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sendMessage(viewer, "giocatore-non-trovato", name);
            return;
        }
        openEditableInventory(viewer, target);
    }

    private void openEditableInventory(Player viewer, Player target) {
        Inventory inventory = target.getInventory();
        // Register after opening: opening fires the close event of the previously open inventory.
        viewer.openInventory(inventory);
        openInvseeInventories.put(viewer.getUniqueId(), inventory);
        sendMessage(viewer, isInStaffMode(viewer) ? "inventario-aperto-sola-lettura" : "inventario-aperto", target.getName());
    }

    private void toggleStaffVanish(Player player) {
        UUID uuid = player.getUniqueId();
        if (vanishedPlayers.remove(uuid)) {
            vanishedBeforeStaffMode.remove(uuid);
            showToEveryone(player);
            restoreTabName(player);
            sendMessage(player, "disattivato");
        } else {
            vanishedPlayers.add(uuid);
            hideFromUnauthorizedPlayers(player);
            setTabName(player, "tab.prefisso-staffmode");
            scheduleTabNameUpdate(uuid);
            sendMessage(player, "attivato");
        }
        savePlayerStates();
    }

    private void randomTeleport(Player player) {
        List<Player> targets = new ArrayList<Player>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (!onlinePlayer.equals(player) && !isVanished(onlinePlayer)) {
                targets.add(onlinePlayer);
            }
        }
        if (targets.isEmpty()) {
            sendMessage(player, "nessun-giocatore-rtp");
            return;
        }
        Player target = targets.get(random.nextInt(targets.size()));
        player.teleport(target.getLocation());
        sendMessage(player, "randomtp", target.getName());
    }

    private void toggleFreeze(Player staff, Player target) {
        if (frozenPlayers.remove(target.getUniqueId())) {
            sendMessage(staff, "giocatore-sfreezato", target.getName());
            sendMessage(target, "sei-sfreezato");
        } else if (target.hasPermission(FREEZE_BYPASS_PERMISSION)) {
            sendMessage(staff, "freeze-bypass", target.getName());
        } else {
            frozenPlayers.add(target.getUniqueId());
            sendMessage(staff, "giocatore-freezato", target.getName());
            sendMessage(target, "sei-freezato");
        }
    }

    private void toggleFlight(Player player) {
        boolean enabled = !player.getAllowFlight();
        if (enabled) {
            player.setAllowFlight(true);
            player.setFlying(true);
        } else {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
        sendMessage(player, enabled ? "volo-attivato" : "volo-disattivato");
    }

    private void updatePlayerState(Player player) {
        if (!isInStaffMode(player) && inventoriesBeforeStaffMode.containsKey(player.getUniqueId())) {
            // A snapshot left behind by a crash for a player no longer in staff mode: give the real inventory back.
            disableStaffTools(player);
        }
        if (isInStaffMode(player)) {
            enableStaffTools(player);
            if (isVanished(player)) {
                hideFromUnauthorizedPlayers(player);
                setTabName(player, "tab.prefisso-staffmode");
                scheduleTabNameUpdate(player.getUniqueId());
            } else {
                showToEveryone(player);
                restoreTabName(player);
            }
            return;
        }
        if (!isVanished(player)) {
            restoreTabName(player);
            return;
        }
        hideFromUnauthorizedPlayers(player);
        setTabName(player, "tab.prefisso-vanish");
        scheduleTabNameUpdate(player.getUniqueId());
    }

    private void reloadPluginConfiguration() {
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        loadMessages();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isInStaffMode(player)) {
                createStaffInventory(player);
            }
            updatePlayerState(player);
        }
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
        return isInStaffMode(vanished)
                ? viewer.hasPermission(STAFFMODE_PERMISSION)
                : viewer.hasPermission(SEE_VANISH_PERMISSION);
    }

    private void showToEveryone(Player vanished) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(vanished)) {
                viewer.showPlayer(vanished);
            }
        }
    }

    private void setTabName(Player player, String configurationPath) {
        UUID uuid = player.getUniqueId();
        if (!originalTabNames.containsKey(uuid)) {
            originalTabNames.put(uuid, player.getPlayerListName());
        }
        String originalName = originalTabNames.get(uuid);
        String prefix = getConfig().getString(configurationPath, "{prefisso}");
        String name = color(prefix.replace("{prefisso}", originalName));
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        player.setPlayerListName(name);
    }

    private void scheduleTabNameUpdate(UUID uuid) {
        queuedTabUpdates.add(uuid);
        if (tabUpdateQueued) {
            return;
        }
        tabUpdateQueued = true;
        Bukkit.getScheduler().runTask(this, new Runnable() {
            @Override
            public void run() {
                Set<UUID> updates = new HashSet<UUID>(queuedTabUpdates);
                queuedTabUpdates.clear();
                tabUpdateQueued = false;
                for (UUID uuid : updates) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && isVanished(player)) {
                        setTabName(player, isInStaffMode(player)
                                ? "tab.prefisso-staffmode" : "tab.prefisso-vanish");
                    }
                }
            }
        });
    }

    private void restoreTabName(Player player) {
        String originalName = originalTabNames.remove(player.getUniqueId());
        if (originalName != null) {
            player.setPlayerListName(originalName);
        }
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        InputStream defaultsStream = getResource("messages.yml");
        if (defaultsStream == null) {
            return;
        }
        try {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaults);
            messages.options().copyDefaults(true);
            messages.save(messagesFile);
        } catch (IOException exception) {
            getLogger().warning("Impossibile aggiornare messages.yml: " + exception.getMessage());
        } finally {
            try {
                defaultsStream.close();
            } catch (IOException ignored) {
                // Nothing to do: the messages were already loaded.
            }
        }
    }

    private boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    private boolean isInStaffMode(Player player) {
        return staffModePlayers.contains(player.getUniqueId());
    }

    private void loadPlayerStates() {
        loadUuidSet("giocatori-in-vanish", vanishedPlayers);
        loadUuidSet("giocatori-in-staffmode", staffModePlayers);
        loadUuidSet("giocatori-in-vanish-prima-della-staffmode", vanishedBeforeStaffMode);
    }

    private void loadUuidSet(String path, Set<UUID> destination) {
        for (String value : getConfig().getStringList(path)) {
            try {
                destination.add(UUID.fromString(value));
            } catch (IllegalArgumentException exception) {
                getLogger().warning("UUID non valido in " + path + ": " + value);
            }
        }
    }

    private void savePlayerStates() {
        if (playerStateSaveQueued) {
            return;
        }
        playerStateSaveQueued = true;
        Bukkit.getScheduler().runTaskLater(this, new Runnable() {
            @Override
            public void run() {
                playerStateSaveQueued = false;
                savePlayerStatesNow();
            }
        }, 20L);
    }

    private void savePlayerStatesNow() {
        saveUuidSet("giocatori-in-vanish", vanishedPlayers);
        saveUuidSet("giocatori-in-staffmode", staffModePlayers);
        saveUuidSet("giocatori-in-vanish-prima-della-staffmode", vanishedBeforeStaffMode);
        saveConfig();
    }

    private void saveUuidSet(String path, Set<UUID> values) {
        List<String> serializedValues = new ArrayList<String>();
        for (UUID uuid : values) {
            serializedValues.add(uuid.toString());
        }
        getConfig().set(path, serializedValues);
    }

    private void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, "");
    }

    private void sendMessage(CommandSender sender, String key, String playerName) {
        String prefix = messages.getString("prefisso", "");
        String message = messages.getString(key, "&cMessaggio mancante: " + key);
        message = message.replace("{giocatore}", playerName);
        sender.sendMessage(color(prefix + message));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

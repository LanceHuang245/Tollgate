package org.claret.tollgate.listener;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.claret.tollgate.ConfigManager;
import org.bukkit.inventory.EquipmentSlot;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.claret.tollgate.TollgatePlugin;
import org.claret.tollgate.TollgateData;
import org.claret.tollgate.TollgateManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for iron door interactions in the Tollgate plugin.
 * Handles payment-based door passage and tollgate removal on door break.
 */
public class DoorListener implements Listener {

    /** Reference to the main plugin instance. */
    private final TollgatePlugin plugin;

    /** Tracks the last payment time per player per tollgate (playerUUID:doorLocation -> epoch millis). */
    private final Map<String, Long> cooldowns;

    /** Suppresses duplicate cooldown messages per player within a short window. */
    private final Map<UUID, Long> lastCooldownMessage;

    /**
     * Constructs a new DoorListener with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public DoorListener(TollgatePlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        this.lastCooldownMessage = new HashMap<>();
    }

    /**
     * Handles sneak + right-click interaction with iron doors.
     * Players who sneak and right-click a tollgate door will pay the toll
     * and be teleported to the other side of the door.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDoorInteract(PlayerInteractEvent event) {
        // Only handle right-click on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // Only process main hand to avoid duplicate events
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // Ensure a block was actually clicked
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        // Only interact with iron doors
        if (!TollgateManager.isIronDoor(clickedBlock)) {
            return;
        }

        // Require the player to be sneaking for intentional interaction
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        // Resolve the bottom half location for consistent lookup
        Location bottomDoorLoc = TollgateManager.getBottomDoorLocation(clickedBlock);

        // Enforce maximum interaction distance of 2 blocks (horizontal + vertical)
        Player player = event.getPlayer();
        Location doorCenter = bottomDoorLoc.clone().add(0.5, 0, 0.5);
        Location playerLoc = player.getLocation();
        double hDist = Math.sqrt(
                Math.pow(playerLoc.getX() - doorCenter.getX(), 2) +
                Math.pow(playerLoc.getZ() - doorCenter.getZ(), 2));
        if (hDist > 2.0 || Math.abs(playerLoc.getY() - doorCenter.getY()) > 2.0) {
            return;
        }

        // Look up the tollgate data for this door
        TollgateData data = plugin.getTollgateManager().getTollgate(bottomDoorLoc);
        if (data == null) {
            return;
        }

        // Cancel vanilla interaction to prevent any unintended behavior
        event.setCancelled(true);

        // Check per-tollgate cooldown (configurable, 0 = disabled)
        int cooldownSeconds = plugin.getConfigManager().getCooldownSeconds();
        if (cooldownSeconds > 0) {
            String cooldownKey = player.getUniqueId().toString() + ":" + bottomDoorLoc.toString();
            Long lastPayment = cooldowns.get(cooldownKey);
            long cooldownMillis = cooldownSeconds * 1000L;
            if (lastPayment != null && System.currentTimeMillis() - lastPayment < cooldownMillis) {
                Long lastMsg = lastCooldownMessage.get(player.getUniqueId());
                if (lastMsg != null && System.currentTimeMillis() - lastMsg < 500L) {
                    return;
                }
                lastCooldownMessage.put(player.getUniqueId(), System.currentTimeMillis());
                long remainingSeconds = cooldownSeconds - (System.currentTimeMillis() - lastPayment) / 1000L;
                Map<String, String> cooldownPlaceholders = new HashMap<>();
                cooldownPlaceholders.put("seconds", String.valueOf(remainingSeconds));
                player.sendMessage(plugin.getConfigManager().getMessage("cooldown", cooldownPlaceholders));
                return;
            }
        }

        Economy economy = plugin.getEconomy();
        double price = data.getPrice();

        // Check if the player can afford the toll
        if (!economy.has(player, price)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("price", economy.format(price));
            placeholders.put("balance", economy.format(economy.getBalance(player)));
            player.sendMessage(plugin.getConfigManager().getMessage("insufficient-funds", placeholders));
            return;
        }

        // Withdraw the toll payment from the passer
        economy.withdrawPlayer(player, price);

        // Deposit the toll payment to the tollgate owner
        OfflinePlayer owner = plugin.getServer().getOfflinePlayer(data.getOwnerUuid());
        economy.depositPlayer(owner, price);
        data.addRevenue(price);

        // Notify the owner if they are online
        Player ownerPlayer = owner.getPlayer();
        if (ownerPlayer != null && ownerPlayer.isOnline()) {
            Map<String, String> ownerPlaceholders = new HashMap<>();
            ownerPlaceholders.put("player", player.getName());
            ownerPlaceholders.put("title", data.getTitle());
            ownerPlaceholders.put("price", economy.format(price));
            ownerPlaceholders.put("revenue", economy.format(data.getTotalRevenue()));
            ownerPlayer.sendMessage(plugin.getConfigManager().getMessage("payment-received", ownerPlaceholders));
        }

        // Record the cooldown timestamp if cooldown is enabled
        if (cooldownSeconds > 0) {
            String cooldownKey = player.getUniqueId().toString() + ":" + bottomDoorLoc.toString();
            cooldowns.put(cooldownKey, System.currentTimeMillis());
        }

        // Send success message
        sendMessage(player, "payment-success", data);

        // Teleport the player to the opposite side of the door.
        // Determine which side the player is on (front or back of the door)
        // and teleport them 1.5 blocks to the opposite side.
        Block doorBlock = bottomDoorLoc.getBlock();
        if (doorBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) doorBlock.getBlockData();
            Vector doorFacing = directional.getFacing().getDirection();

            Vector toPlayer = playerLoc.toVector().subtract(doorCenter.toVector());
            Vector teleportDir;
            if (toPlayer.dot(doorFacing) >= 0) {
                teleportDir = doorFacing.multiply(-1.5);
            } else {
                teleportDir = doorFacing.multiply(1.5);
            }

            Location target = doorCenter.clone().add(teleportDir);
            target.setYaw(playerLoc.getYaw());
            target.setPitch(playerLoc.getPitch());
            player.teleport(target);

            spawnPassageParticles(player, doorCenter);
        }
    }

    /**
     * Handles door break events to clean up tollgate registrations.
     * When a registered tollgate door is broken, the tollgate data and
     * associated sign are removed.
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDoorBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only handle iron door breaks
        if (!TollgateManager.isIronDoor(block)) {
            return;
        }

        // Resolve the bottom half location for tollgate lookup
        Location bottomDoorLoc = TollgateManager.getBottomDoorLocation(block);

        // Check if this door is a registered tollgate
        TollgateData data = plugin.getTollgateManager().getTollgate(bottomDoorLoc);
        if (data == null) {
            return;
        }

        // Unregister the tollgate
        plugin.getTollgateManager().unregisterTollgate(bottomDoorLoc);

        // Clear the sign associated with this tollgate
        Location signLoc = data.getSignLocation();
        Block signBlock = signLoc.getBlock();
        if (signBlock.getChunk().isLoaded() && signBlock.getState() instanceof Sign) {
            Sign sign = (Sign) signBlock.getState();
            org.bukkit.block.sign.SignSide side = sign.getSide(Side.FRONT);
            side.line(0, Component.empty());
            side.line(1, Component.empty());
            side.line(2, Component.empty());
            side.line(3, Component.empty());
            sign.update();
        }

        // Notify the player
        Player player = event.getPlayer();
        sendMessage(player, "tollgate-removed", data);
    }

    /**
     * Blocks redstone activation of tollgate iron doors.
     * Pressure plates, buttons, levers, and any other redstone sources
     * cannot open a door that is registered as a tollgate.
     *
     * @param event the block redstone event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();

        // Only intercept iron doors
        if (!TollgateManager.isIronDoor(block)) {
            return;
        }

        // If the door is a registered tollgate, cancel redstone activation
        Location bottomDoorLoc = TollgateManager.getBottomDoorLocation(block);
        if (plugin.getTollgateManager().getTollgate(bottomDoorLoc) != null) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    /**
     * Spawns a three-wave expanding burst of END_ROD particles at the door center,
     * visible only to the passing player. Each wave has a progressively larger radius.
     *
     * @param player the player who sees the particles
     * @param center the center of the door block for particle spawning
     */
    private void spawnPassageParticles(Player player, Location center) {
        ConfigManager config = plugin.getConfigManager();
        if (!config.isParticlesEnabled()) {
            return;
        }

        int count = config.getParticleCount();
        double maxRadius = config.getParticleRadius();

        double[] radii = {maxRadius * 0.25, maxRadius * 0.6, maxRadius};
        long[] delays = {0L, 3L, 6L};

        for (int i = 0; i < radii.length; i++) {
            final double radius = radii[i];
            new BukkitRunnable() {
                @Override
                public void run() {
                    player.spawnParticle(Particle.END_ROD, center, count, radius, radius, radius, 0);
                }
            }.runTaskLater(plugin, delays[i]);
        }
    }

    /**
     * Sends a configurable message to the player with tollgate-specific placeholders.
     *
     * @param player    the player to send the message to
     * @param configKey the message key in the config
     * @param data      the tollgate data for placeholder values
     */
    private void sendMessage(Player player, String configKey, TollgateData data) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("price", plugin.getEconomy().format(data.getPrice()));
        placeholders.put("balance", plugin.getEconomy().format(plugin.getEconomy().getBalance(player)));
        placeholders.put("title", data.getTitle());
        player.sendMessage(plugin.getConfigManager().getMessage(configKey, placeholders));
    }
}

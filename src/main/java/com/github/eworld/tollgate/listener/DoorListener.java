package com.github.eworld.tollgate.listener;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import com.github.eworld.tollgate.TollgatePlugin;
import com.github.eworld.tollgate.TollgateData;
import com.github.eworld.tollgate.TollgateManager;
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

    /** Tracks the last payment time per player (UUID -> epoch millis). */
    private final Map<UUID, Long> cooldowns;

    /**
     * Constructs a new DoorListener with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public DoorListener(TollgatePlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
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

        // Look up the tollgate data for this door
        TollgateData data = plugin.getTollgateManager().getTollgate(bottomDoorLoc);
        if (data == null) {
            return;
        }

        // Cancel vanilla interaction to prevent any unintended behavior
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Check payment cooldown
        int cooldownSeconds = plugin.getConfigManager().getCooldown();
        if (cooldownSeconds > 0) {
            Long lastPayment = cooldowns.get(player.getUniqueId());
            if (lastPayment != null && System.currentTimeMillis() - lastPayment < cooldownSeconds * 1000L) {
                // Calculate remaining cooldown seconds and send cooldown message
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

        // Withdraw the toll payment
        economy.withdrawPlayer(player, price);

        // Record the cooldown timestamp
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Send success message
        sendMessage(player, "payment-success", data);

        // Teleport the player to the opposite side of the door.
        // Determine which side the player is on (front or back of the door)
        // and teleport them 1.5 blocks to the opposite side.
        Block doorBlock = bottomDoorLoc.getBlock();
        if (doorBlock.getBlockData() instanceof Directional) {
            Directional directional = (Directional) doorBlock.getBlockData();
            Vector doorFacing = directional.getFacing().getDirection();
            Location doorCenter = bottomDoorLoc.clone().add(0.5, 0, 0.5);
            Location playerLoc = player.getLocation();

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
        if (signBlock.getState() instanceof Sign) {
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

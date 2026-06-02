package com.github.eworld.tollgate.listener;

import com.github.eworld.tollgate.TollgateData;
import com.github.eworld.tollgate.TollgateManager;
import com.github.eworld.tollgate.TollgatePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Handles sign-related events for the Tollgate plugin.
 * Detects when players create or break tollgate signs.
 */
public class SignListener implements Listener {

    private final TollgatePlugin plugin;

    /**
     * Constructs a new SignListener with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public SignListener(TollgatePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the SignChangeEvent to detect when a player creates a tollgate sign.
     * If the sign's first line is "[Tollgate]" and there is an iron door below,
     * the event is cancelled and the chat input flow is started to collect toll
     * details.
     *
     * @param event the sign change event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent event) {
        // Verify the first line contains the tollgate identifier "[Tollgate]"
        String line0 = event.getLine(0);
        if (line0 == null || !line0.equals("[Tollgate]")) {
            return;
        }

        Location signLoc = event.getBlock().getLocation();

        // Require the sign to be a wall sign attached to a block face,
        // not a standing sign placed on top of a block
        if (!Tag.WALL_SIGNS.isTagged(event.getBlock().getType())) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c必须在铁门上方方块贴边放置告示牌！"));
            return;
        }

        // Find the block the wall sign is attached to
        WallSign wallData = (WallSign) event.getBlock().getBlockData();
        Block attachedBlock = event.getBlock().getRelative(wallData.getFacing().getOppositeFace());

        // Check 1-2 blocks below the attached block for an iron door
        Block doorBlock = null;
        for (int dy = -1; dy >= -2; dy--) {
            Block candidate = attachedBlock.getRelative(0, dy, 0);
            if (TollgateManager.isIronDoor(candidate)) {
                doorBlock = candidate;
                break;
            }
        }

        if (doorBlock == null) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c必须在铁门上方放置告示牌！"));
            return;
        }

        // Normalize to the bottom half of the door
        Location doorBottomLoc = TollgateManager.getBottomDoorLocation(doorBlock);

        // Cancel the sign text change and start chat input flow for toll creation.
        // The sign block stays at its original position where the player placed it.
        event.setCancelled(true);
        plugin.getChatInputHandler().startCreation(event.getPlayer(), doorBottomLoc,
                event.getBlock().getLocation());
    }

    /**
     * Handles the BlockBreakEvent to detect when a player breaks a tollgate sign.
     * If the broken block is a sign registered as part of a tollgate, the tollgate
     * is unregistered and a removal message is sent to the player.
     *
     * @param event the block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSignBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // Only process if the broken block is a sign type
        if (!Tag.SIGNS.isTagged(block.getType())) {
            return;
        }

        Location signLoc = block.getLocation();

        // Search for a tollgate registered at this sign location
        TollgateManager manager = plugin.getTollgateManager();
        for (TollgateData tollgate : manager.getAllTollgates()) {
            if (tollgate.getSignLocation().equals(signLoc)) {
                // Remove the tollgate and notify the player
                manager.unregisterTollgate(tollgate.getDoorLocation());
                event.getPlayer().sendMessage(
                        plugin.getConfigManager().getMessage("tollgate-removed"));
                break;
            }
        }
    }
}

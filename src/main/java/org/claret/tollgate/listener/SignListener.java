package org.claret.tollgate.listener;

import org.claret.tollgate.TollgateData;
import org.claret.tollgate.TollgateManager;
import org.claret.tollgate.TollgatePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

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
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

        // Find the iron door below the sign
        Location doorBottomLoc = findDoorBelowSign(event.getBlock());
        if (doorBottomLoc == null) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c必须在铁门上方放置告示牌！"));
            return;
        }

        // Cancel the sign text change and start chat input flow for toll creation.
        // The sign block stays at its original position where the player placed it.
        event.setCancelled(true);
        plugin.getChatInputHandler().startCreation(event.getPlayer(), doorBottomLoc,
                event.getBlock().getLocation());
    }

    /**
     * Alternative tollgate creation via sneak + right-click on a wall sign above
     * an iron door. This bypasses SignChangeEvent conflicts with chest lock plugins
     * that prevent sign text editing.
     *
     * @param event the player interact event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !Tag.WALL_SIGNS.isTagged(clickedBlock.getType())) {
            return;
        }

        if (!event.getPlayer().isSneaking()) {
            return;
        }

        Location doorBottomLoc = findDoorBelowSign(clickedBlock);
        if (doorBottomLoc == null) {
            return;
        }

        event.setCancelled(true);
        plugin.getChatInputHandler().startCreation(event.getPlayer(), doorBottomLoc,
                clickedBlock.getLocation());
    }

    /**
     * Handles sign placement (new sign) above an iron door while sneaking.
     * Intercepts BlockPlaceEvent to detect when a player places a wall sign
     * above an iron door and immediately starts the tollgate creation flow,
     * bypassing SignChangeEvent conflicts with chest lock plugins.
     *
     * @param event the block place event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();

        // Only process wall sign placements
        if (!Tag.WALL_SIGNS.isTagged(placedBlock.getType())) {
            return;
        }

        // Require sneaking for tollgate intent
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        // Verify the placed sign is above an iron door
        Location doorBottomLoc = findDoorBelowSign(placedBlock);
        if (doorBottomLoc == null) {
            return;
        }

        // Start the tollgate creation flow, sign is already placed
        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a检测到铁门上方贴边放置告示牌，开始创建收费站..."));
        plugin.getChatInputHandler().startCreation(event.getPlayer(), doorBottomLoc,
                placedBlock.getLocation());
    }

    /**
     * Finds the bottom half location of an iron door below a wall sign.
     * The sign must be attached to a block that is 1-2 blocks above the door.
     *
     * @param signBlock the wall sign block
     * @return the bottom door location, or null if no iron door found below
     */
    private Location findDoorBelowSign(Block signBlock) {
        BlockData data = signBlock.getBlockData();
        if (!(data instanceof WallSign)) {
            return null;
        }
        WallSign wallData = (WallSign) data;
        Block attachedBlock = signBlock.getRelative(wallData.getFacing().getOppositeFace());

        for (int dy = -1; dy >= -2; dy--) {
            Block candidate = attachedBlock.getRelative(0, dy, 0);
            if (TollgateManager.isIronDoor(candidate)) {
                Location doorBottom = TollgateManager.getBottomDoorLocation(candidate);
                // Sign must be above the top half of the door, not at door height
                if (signBlock.getY() <= doorBottom.getBlockY() + 1) {
                    return null;
                }
                return doorBottom;
            }
        }
        return null;
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

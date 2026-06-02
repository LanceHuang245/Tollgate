package com.github.eworld.tollgate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core business logic manager for the Tollgate plugin.
 * Manages all toll gate data and operations.
 */
public class TollgateManager {

    private final TollgatePlugin plugin;
    private final Map<Location, TollgateData> tollgates;

    /**
     * Constructs a new TollgateManager with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public TollgateManager(TollgatePlugin plugin) {
        this.plugin = plugin;
        this.tollgates = new ConcurrentHashMap<>();
    }

    /**
     * Registers a new toll gate at the given door location with the specified
     * sign location, title, price, and owner.
     *
     * @param doorLocation the location of the bottom half of the iron door
     * @param signLocation the location of the sign block above the door
     * @param title        the custom title for the toll gate
     * @param price        the toll price
     * @param ownerUuid    the UUID of the player who created this tollgate
     * @return the newly created TollgateData instance
     */
    public TollgateData registerTollgate(Location doorLocation, Location signLocation, String title, double price, UUID ownerUuid) {
        TollgateData data = new TollgateData(doorLocation, signLocation, title, price, ownerUuid);
        tollgates.put(doorLocation.clone(), data);
        return data;
    }

    /**
     * Removes the toll gate registration for the given door location.
     *
     * @param doorLocation the location of the door to unregister
     */
    public void unregisterTollgate(Location doorLocation) {
        // Remove the tollgate entry from the map
        tollgates.remove(doorLocation);
    }

    /**
     * Retrieves the toll gate data for the given door location.
     *
     * @param doorLocation the location of the door to look up
     * @return the TollgateData if found, otherwise null
     */
    public TollgateData getTollgate(Location doorLocation) {
        // Return the tollgate data for the given location, or null if not found
        return tollgates.get(doorLocation);
    }

    /**
     * Returns all registered toll gate data entries.
     *
     * @return a collection of all TollgateData instances
     */
    public Collection<TollgateData> getAllTollgates() {
        // Return all tollgate data entries as a Collection
        return tollgates.values();
    }

    /**
     * Returns the total number of registered toll gates.
     *
     * @return the count of registered tollgates
     */
    public int getTollgateCount() {
        // Return the number of registered tollgates
        return tollgates.size();
    }

    /**
     * Checks whether the given block is an iron door.
     *
     * @param block the block to check
     * @return true if the block is an iron door, false otherwise
     */
    public static boolean isIronDoor(Block block) {
        // Check if the block material is an iron door
        return block.getType() == Material.IRON_DOOR;
    }

    /**
     * Given an iron door block (either top or bottom half), returns a clone of
     * the bottom half's location. If the block does not implement Bisected,
     * the block's location is returned as-is.
     *
     * @param doorBlock the door block to inspect
     * @return a cloned Location of the bottom half of the door
     */
    public static Location getBottomDoorLocation(Block doorBlock) {
        // Determine the bottom half location of a Bisected door block
        if (doorBlock.getBlockData() instanceof Bisected) {
            Bisected bisected = (Bisected) doorBlock.getBlockData();
            if (bisected.getHalf() == Bisected.Half.BOTTOM) {
                return doorBlock.getLocation().clone();
            } else {
                return doorBlock.getLocation().subtract(0, 1, 0);
            }
        }
        return doorBlock.getLocation().clone();
    }

    /**
     * Checks whether there is a wall sign attached above the given door block.
     * The door block must be the bottom half.
     *
     * @param doorBlock the bottom half of the iron door
     * @return true if a wall sign is found 2 blocks above the door, false otherwise
     */
    public static boolean isSignAboveDoor(Block doorBlock) {
        // Check if the block 2 blocks above the door is a wall sign
        Block blockAbove = doorBlock.getLocation().add(0, 2, 0).getBlock();
        return blockAbove.getBlockData() instanceof WallSign;
    }

    /**
     * Returns the location of the sign above the given door block.
     * The door block must be the bottom half.
     *
     * @param doorBlock the bottom half of the iron door
     * @return a cloned Location 2 blocks above the door
     */
    public static Location getSignLocationAboveDoor(Block doorBlock) {
        // Return the location 2 blocks above the door
        return doorBlock.getLocation().add(0, 2, 0).clone();
    }

    /**
     * Updates the sign's display text to show toll gate information using the
     * modern Component API.
     *
     * @param sign the sign block to update
     * @param data the toll gate data to display
     */
    public void updateSignDisplay(Sign sign, TollgateData data) {
        // Update sign text with tollgate information using the Component API
        org.bukkit.block.sign.SignSide side = sign.getSide(Side.FRONT);
        side.line(0, Component.text("[Tollgate]", NamedTextColor.RED, TextDecoration.BOLD));
        side.line(1, Component.text(data.getTitle()));
        side.line(2, Component.text(plugin.getEconomy().format(data.getPrice()), NamedTextColor.YELLOW));
        side.line(3, Component.text("潜行+右键铁门"));
        sign.update();
    }

    /**
     * Returns the Sign block state at the given location, or null if the block
     * is not a sign.
     *
     * @param location the location to check
     * @return the Sign if the block is a sign, otherwise null
     */
    public Sign getSignAt(Location location) {
        // Get the sign block state at the given location, or null if not a sign
        Block block = location.getBlock();
        if (block.getState() instanceof Sign) {
            return (Sign) block.getState();
        }
        return null;
    }
}

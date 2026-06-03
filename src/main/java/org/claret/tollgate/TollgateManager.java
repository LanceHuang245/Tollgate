package org.claret.tollgate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Core business logic manager for the Tollgate plugin.
 * Manages all toll gate data and operations.
 */
public class TollgateManager implements Listener {

    private final TollgatePlugin plugin;
    private final Map<Location, TollgateData> tollgates;
    private final File dataFile;
    private final List<Map<?, ?>> pendingEntries;

    /**
     * Constructs a new TollgateManager with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public TollgateManager(TollgatePlugin plugin) {
        this.plugin = plugin;
        this.tollgates = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.pendingEntries = Collections.synchronizedList(new ArrayList<>());
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
        saveData();
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
        saveData();
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
        sign.setLine(0, ChatColor.RED.toString() + ChatColor.BOLD + "[Tollgate]");
        sign.setLine(1, data.getTitle());
        sign.setLine(2, ChatColor.YELLOW + plugin.getEconomy().format(data.getPrice()));
        sign.setLine(3, "潜行+右键铁门");
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

    /**
     * Persists all tollgate data to data.yml in the plugin data folder.
     */
    public void saveData() {
        FileConfiguration data = new YamlConfiguration();
        List<Map<String, Object>> list = new ArrayList<>();
        for (TollgateData td : tollgates.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            Location dl = td.getDoorLocation();
            Location sl = td.getSignLocation();
            entry.put("door-world", dl.getWorld().getName());
            entry.put("door-x", dl.getBlockX());
            entry.put("door-y", dl.getBlockY());
            entry.put("door-z", dl.getBlockZ());
            entry.put("sign-world", sl.getWorld().getName());
            entry.put("sign-x", sl.getBlockX());
            entry.put("sign-y", sl.getBlockY());
            entry.put("sign-z", sl.getBlockZ());
            entry.put("owner", td.getOwnerUuid().toString());
            entry.put("title", td.getTitle());
            entry.put("price", td.getPrice());
            entry.put("revenue", td.getTotalRevenue());
            list.add(entry);
        }
        data.set("tollgates", list);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save tollgate data", e);
        }
    }

    /**
     * Loads tollgate data from data.yml in the plugin data folder.
     * Restores all previously saved tollgates into memory.
     */
    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        pendingEntries.clear();
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        List<Map<?, ?>> list = data.getMapList("tollgates");
        for (Map<?, ?> entry : list) {
            World doorWorld = Bukkit.getWorld((String) entry.get("door-world"));
            if (doorWorld == null) {
                pendingEntries.add(entry);
                continue;
            }
            World signWorld = Bukkit.getWorld((String) entry.get("sign-world"));
            if (signWorld == null) {
                pendingEntries.add(entry);
                continue;
            }
            registerFromEntry(entry, doorWorld, signWorld);
        }
        if (!pendingEntries.isEmpty()) {
            plugin.getLogger().info("Deferred " + pendingEntries.size() + " tollgate(s) waiting for world load");
        }
        plugin.getLogger().info("Loaded " + tollgates.size() + " tollgate(s) from data.yml");
    }

    /**
     * Builds a TollgateData instance from a serialized map entry and resolved worlds,
     * restores any saved revenue, and registers it in the tollgates map.
     *
     * @param entry     the serialized map entry from data.yml
     * @param doorWorld the resolved door world
     * @param signWorld the resolved sign world
     */
    private void registerFromEntry(Map<?, ?> entry, World doorWorld, World signWorld) {
        Location dl = new Location(doorWorld,
                ((Number) entry.get("door-x")).intValue(),
                ((Number) entry.get("door-y")).intValue(),
                ((Number) entry.get("door-z")).intValue());

        Location sl = new Location(signWorld,
                ((Number) entry.get("sign-x")).intValue(),
                ((Number) entry.get("sign-y")).intValue(),
                ((Number) entry.get("sign-z")).intValue());

        UUID owner = UUID.fromString((String) entry.get("owner"));
        String title = (String) entry.get("title");
        double price = ((Number) entry.get("price")).doubleValue();

        TollgateData td = new TollgateData(dl, sl, title, price, owner);
        Number revenue = (Number) entry.get("revenue");
        if (revenue != null) {
            td.addRevenue(revenue.doubleValue());
        }
        tollgates.put(dl.clone(), td);
    }

    /**
     * Handles world load events to register tollgates whose worlds were not
     * available when loadData() ran during plugin startup.
     *
     * @param event the WorldLoadEvent
     */
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();
        int loaded = 0;
        Iterator<Map<?, ?>> iter = pendingEntries.iterator();
        while (iter.hasNext()) {
            Map<?, ?> entry = iter.next();
            String doorWorldName = (String) entry.get("door-world");
            String signWorldName = (String) entry.get("sign-world");
            if (!worldName.equals(doorWorldName) && !worldName.equals(signWorldName)) {
                continue;
            }
            World doorWorld = Bukkit.getWorld(doorWorldName);
            if (doorWorld == null) {
                continue;
            }
            World signWorld = Bukkit.getWorld(signWorldName);
            if (signWorld == null) {
                continue;
            }
            registerFromEntry(entry, doorWorld, signWorld);
            iter.remove();
            loaded++;
        }
        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " deferred tollgate(s) for world '" + worldName + "'");
        }
    }
}

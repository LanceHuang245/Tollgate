package org.claret.tollgate;

import org.bukkit.Location;
import java.util.UUID;

/**
 * Data model for a toll gate entry, storing the door location, sign location,
 * title, price, owner, and total revenue.
 */
public class TollgateData {

    /** The location of the bottom half of the iron door. */
    private final Location doorLocation;

    /** The location of the sign block above the door. */
    private final Location signLocation;

    /** The UUID of the player who created this tollgate. */
    private final UUID ownerUuid;

    /** The custom title set by the player, displayed on sign line 2. */
    private String title;

    /** The toll price set by the player, displayed on sign line 3. */
    private double price;

    /** Total revenue earned from this tollgate since creation. */
    private double totalRevenue;

    /**
     * Constructs a new TollgateData instance with cloned locations to ensure
     * immutability of the stored position data.
     *
     * @param doorLocation the location of the bottom half of the iron door
     * @param signLocation the location of the sign block above the door
     * @param title        the custom title shown on the sign
     * @param price        the toll price
     * @param ownerUuid    the UUID of the player who created this tollgate
     */
    public TollgateData(Location doorLocation, Location signLocation, String title, double price, UUID ownerUuid) {
        this.doorLocation = doorLocation.clone();
        this.signLocation = signLocation.clone();
        this.ownerUuid = ownerUuid;
        this.title = title;
        this.price = price;
        this.totalRevenue = 0;
    }

    /**
     * Returns a cloned copy of the door location.
     *
     * @return a clone of the door location
     */
    public Location getDoorLocation() {
        return doorLocation.clone();
    }

    /**
     * Returns a cloned copy of the sign location.
     *
     * @return a clone of the sign location
     */
    public Location getSignLocation() {
        return signLocation.clone();
    }

    /**
     * Returns the custom title displayed on the sign.
     *
     * @return the title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the toll price.
     *
     * @return the price
     */
    public double getPrice() {
        return price;
    }

    /**
     * Returns the UUID of the player who created this tollgate.
     *
     * @return the owner's UUID
     */
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    /**
     * Returns the total revenue earned from this tollgate.
     *
     * @return the total revenue
     */
    public double getTotalRevenue() {
        return totalRevenue;
    }

    /**
     * Adds the specified amount to the total revenue of this tollgate.
     *
     * @param amount the amount to add
     */
    public void addRevenue(double amount) {
        this.totalRevenue += amount;
    }

}

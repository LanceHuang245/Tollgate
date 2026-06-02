package com.github.eworld.tollgate;

import org.bukkit.Location;

/**
 * Data model for a toll gate entry, storing the door location, sign location,
 * title, and price.
 */
public class TollgateData {

    /** The location of the bottom half of the iron door. */
    private final Location doorLocation;

    /** The location of the sign block above the door. */
    private final Location signLocation;

    /** The custom title set by the player, displayed on sign line 2. */
    private String title;

    /** The toll price set by the player, displayed on sign line 3. */
    private double price;

    /**
     * Constructs a new TollgateData instance with cloned locations to ensure
     * immutability of the stored position data.
     *
     * @param doorLocation the location of the bottom half of the iron door
     * @param signLocation the location of the sign block above the door
     * @param title        the custom title shown on the sign
     * @param price        the toll price
     */
    public TollgateData(Location doorLocation, Location signLocation, String title, double price) {
        this.doorLocation = doorLocation.clone();
        this.signLocation = signLocation.clone();
        this.title = title;
        this.price = price;
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
     * Updates the custom title shown on the sign.
     *
     * @param title the new title string
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the toll price.
     *
     * @param price the new price
     */
    public void setPrice(double price) {
        this.price = price;
    }
}

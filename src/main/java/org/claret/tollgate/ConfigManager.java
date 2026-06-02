package org.claret.tollgate;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages plugin configuration including currency symbol, cooldown, and
 * message templates with placeholder support.
 */
public class ConfigManager {

    private final TollgatePlugin plugin;
    private String currencySymbol = "$";
    private int cooldown = 0;
    private Map<String, String> messages;

    /**
     * Constructs a new ConfigManager with a reference to the main plugin.
     *
     * @param plugin the main plugin instance
     */
    public ConfigManager(TollgatePlugin plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
    }

    /**
     * Reloads the configuration from disk and populates all managed fields
     * from the current config values.
     */
    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        currencySymbol = config.getString("currency-symbol", "$");
        cooldown = config.getInt("cooldown", 0);

        messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                String value = config.getString("messages." + key);
                if (value != null) {
                    messages.put(key, value);
                }
            }
        }
    }

    /**
     * Returns the currency symbol configured for display purposes.
     *
     * @return the currency symbol string
     */
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Returns the cooldown duration in seconds between toll gate uses.
     *
     * @return the cooldown in seconds
     */
    public int getCooldown() {
        return cooldown;
    }

    /**
     * Retrieves a message template by key and applies color code translation
     * using alternate color codes.
     *
     * @param key the message key to look up
     * @return the color-translated message, or an error message if the key is not found
     */
    public String getMessage(String key) {
        String message = messages.get(key);
        if (message == null) {
            return "Message not found: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Retrieves a message template by key, applies color code translation,
     * and replaces all placeholder patterns (%key%) with the corresponding
     * values from the provided map.
     *
     * @param key          the message key to look up
     * @param placeholders a map of placeholder names to replacement values
     * @return the processed message with colors and placeholders applied
     */
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }
}

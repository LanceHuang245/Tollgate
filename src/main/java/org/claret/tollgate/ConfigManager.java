package org.claret.tollgate;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages plugin configuration including currency symbol and
 * message templates with placeholder support.
 */
public class ConfigManager {

    private final TollgatePlugin plugin;
    private Map<String, String> messages;
    private boolean particlesEnabled = true;
    private int particleCount = 25;
    private double particleRadius = 1.2;
    private int cooldownSeconds = 3;

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

        cooldownSeconds = config.getInt("cooldown", 3);

        messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                String value = config.getString("messages." + key);
                if (value != null) {
                    messages.put(key, value);
                }
            }
        }

        particlesEnabled = config.getBoolean("particles.enabled", true);
        particleCount = config.getInt("particles.count", 25);
        particleRadius = config.getDouble("particles.radius", 1.2);
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

    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public double getParticleRadius() {
        return particleRadius;
    }

    /**
     * Returns the cooldown duration in seconds between tollgate uses.
     *
     * @return the cooldown in seconds, 0 means disabled
     */
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
}

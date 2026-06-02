package org.claret.tollgate;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import org.claret.tollgate.handler.ChatInputHandler;
import org.claret.tollgate.handler.CommandHandler;
import org.claret.tollgate.listener.SignListener;
import org.claret.tollgate.listener.DoorListener;

/** Main plugin class for the Tollgate Paper plugin. */
public class TollgatePlugin extends JavaPlugin {

    /** Singleton reference to the plugin instance. */
    private static TollgatePlugin instance;

    /** Vault economy provider for handling transactions. */
    private Economy economy;

    /** Core business logic manager for toll gate operations. */
    private TollgateManager tollgateManager;

    /** Configuration manager for handling plugin settings. */
    private ConfigManager configManager;

    /** Handles chat input flow for tollgate creation. */
    private ChatInputHandler chatInputHandler;

    /**
     * Called when the plugin is enabled. Initializes all components,
     * sets up the economy provider, registers event listeners, and
     * registers commands.
     */
    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.load();

        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        tollgateManager = new TollgateManager(this);
        tollgateManager.loadData();

        chatInputHandler = new ChatInputHandler(this);

        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(new DoorListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputHandler, this);
        getServer().getPluginManager().registerEvents(tollgateManager, this);

        CommandHandler commandHandler = new CommandHandler(this);
        getCommand("tollgate").setExecutor(commandHandler);
        getCommand("tollgate").setTabCompleter(commandHandler);

        getLogger().info("Tollgate plugin enabled!");
    }

    /**
     * Called when the plugin is disabled. Performs cleanup operations.
     */
    @Override
    public void onDisable() {
        tollgateManager.saveData();
        getLogger().info("Tollgate plugin disabled!");
    }

    /**
     * Attempts to set up the Vault economy provider.
     *
     * @return true if the economy provider was successfully set up, false otherwise
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Returns the singleton instance of the plugin.
     *
     * @return the TollgatePlugin instance
     */
    public static TollgatePlugin getInstance() {
        return instance;
    }

    /**
     * Returns the Vault economy provider.
     *
     * @return the economy provider, or null if not available
     */
    public Economy getEconomy() {
        return economy;
    }

    /**
     * Returns the toll gate manager.
     *
     * @return the TollgateManager instance
     */
    public TollgateManager getTollgateManager() {
        return tollgateManager;
    }

    /**
     * Returns the configuration manager.
     *
     * @return the ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Returns the chat input handler.
     *
     * @return the ChatInputHandler instance
     */
    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }
}

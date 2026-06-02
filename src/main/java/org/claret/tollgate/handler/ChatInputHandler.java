package org.claret.tollgate.handler;

import org.claret.tollgate.TollgateData;
import org.claret.tollgate.TollgatePlugin;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the sequential chat input flow for tollgate creation.
 * After a player clicks "Done" on a sign with "[Tollgate]" on line 1,
 * this class guides them through entering a title and price via chat.
 */
public class ChatInputHandler implements Listener {

    private final TollgatePlugin plugin;
    private final Map<UUID, PendingCreation> pendingCreations;

    /**
     * Stores the tollgate creation state for a player in the chat flow.
     */
    private static class PendingCreation {
        final Location doorLocation;
        final Location signLocation;
        CreationState state;
        String title;
        int timeoutTaskId = -1;

        PendingCreation(Location doorLocation, Location signLocation) {
            this.doorLocation = doorLocation;
            this.signLocation = signLocation;
            this.state = CreationState.WAITING_TITLE;
        }
    }

    private enum CreationState {
        WAITING_TITLE,
        WAITING_PRICE
    }

    public ChatInputHandler(TollgatePlugin plugin) {
        this.plugin = plugin;
        this.pendingCreations = new ConcurrentHashMap<>();
    }

    /**
     * Starts the chat input flow for a player creating a tollgate.
     */
    public void startCreation(Player player, Location doorLocation, Location signLocation) {
        cancelCreation(player);

        PendingCreation pending = new PendingCreation(doorLocation, signLocation);
        pendingCreations.put(player.getUniqueId(), pending);

        pending.timeoutTaskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pendingCreations.containsKey(player.getUniqueId())) {
                cancelCreation(player);
                player.sendMessage(plugin.getConfigManager().getMessage("enter-timeout"));
            }
        }, 1200L).getTaskId();

        player.sendMessage(plugin.getConfigManager().getMessage("enter-title"));
    }

    /**
     * Cancels an ongoing tollgate creation for a player.
     */
    private void cancelCreation(Player player) {
        PendingCreation pending = pendingCreations.remove(player.getUniqueId());
        if (pending != null) {
            plugin.getServer().getScheduler().cancelTask(pending.timeoutTaskId);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingCreation pending = pendingCreations.get(player.getUniqueId());
        if (pending == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            cancelCreation(player);
            player.sendMessage(plugin.getConfigManager().getMessage("title-cancelled"));
            return;
        }

        switch (pending.state) {
            case WAITING_TITLE:
                pending.title = message;
                plugin.getServer().getScheduler().cancelTask(pending.timeoutTaskId);
                pending.timeoutTaskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (pendingCreations.containsKey(player.getUniqueId())) {
                        cancelCreation(player);
                        player.sendMessage(plugin.getConfigManager().getMessage("enter-timeout"));
                    }
                }, 1200L).getTaskId();
                pending.state = CreationState.WAITING_PRICE;
                player.sendMessage(plugin.getConfigManager().getMessage("enter-price"));
                break;

            case WAITING_PRICE:
                String cleaned = message.replace("$", "")
                        .replace("\u20AC", "")
                        .replace("\u00A5", "")
                        .replace("\u00A3", "")
                        .replace("\u00A2", "")
                        .replace(",", "");
                double price;
                try {
                    price = Double.parseDouble(cleaned);
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-price"));
                    return;
                }
                if (price <= 0) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-price"));
                    return;
                }

                // Cancel timeout and remove pending state before async block operations
                plugin.getServer().getScheduler().cancelTask(pending.timeoutTaskId);
                pendingCreations.remove(player.getUniqueId());

                // Register tollgate and update sign on the main server thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    TollgateData data = plugin.getTollgateManager().registerTollgate(
                            pending.doorLocation.clone(),
                            pending.signLocation.clone(),
                            pending.title,
                            price,
                            player.getUniqueId());

                    Sign sign = plugin.getTollgateManager().getSignAt(pending.signLocation);
                    if (sign != null) {
                        plugin.getTollgateManager().updateSignDisplay(sign, data);
                    }

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("title", pending.title);
                    placeholders.put("price", plugin.getEconomy().format(price));
                    player.sendMessage(plugin.getConfigManager().getMessage("tollgate-created", placeholders));
                });
                break;
        }
    }
}

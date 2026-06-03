package org.claret.tollgate.handler;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

import org.claret.tollgate.TollgatePlugin;
import org.claret.tollgate.TollgateData;
import org.claret.tollgate.TollgateManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles admin subcommands (/tollgate reload|list|remove) for the Tollgate plugin,
 * implementing both command execution and tab completion.
 */
public class CommandHandler implements CommandExecutor, TabCompleter {

    private final TollgatePlugin plugin;

    public CommandHandler(TollgatePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles command execution for /tollgate subcommands.
     * Routes reload, list, and remove actions based on the first argument.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tollgate.admin")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Usage: /tollgate <reload|list|remove>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getConfigManager().load();
                sendMessage(sender, "plugin-reloaded");
                return true;

            case "list":
                return handleList(sender);

            case "remove":
                return handleRemove(sender);

            default:
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "Usage: /tollgate <reload|list|remove>"));
                return true;
        }
    }

    /**
     * Handles the /tollgate list subcommand.
     * Displays all registered toll gates with index, title, location, and price.
     */
    private boolean handleList(CommandSender sender) {
        Collection<TollgateData> tollgates = plugin.getTollgateManager().getAllTollgates();

        if (tollgates.isEmpty()) {
            sender.sendMessage("No toll gates registered.");
            return true;
        }

        int index = 1;
        for (TollgateData data : tollgates) {
            String price = plugin.getEconomy().format(data.getPrice());
            String revenue = plugin.getEconomy().format(data.getTotalRevenue());
            String location = formatLocation(data.getDoorLocation());
            String ownerName = plugin.getServer().getOfflinePlayer(data.getOwnerUuid()).getName();
            if (ownerName == null) {
                ownerName = data.getOwnerUuid().toString().substring(0, 8);
            }
            String line = String.format("&e[%d] &f%s &7at &f%s &7- &6%s &7(owner: &f%s&7, earned: &6%s&7)",
                    index, data.getTitle(), location, price, ownerName, revenue);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
            index++;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Total: " + tollgates.size() + " toll gate(s)"));
        return true;
    }

    /**
     * Handles the /tollgate remove subcommand.
     * Removes a toll gate by looking at an iron door or sign block.
     */
    private boolean handleRemove(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cYou are not looking at a block."));
            return true;
        }

        TollgateData found = null;

        if (TollgateManager.isIronDoor(targetBlock)) {
            Location bottomDoor = TollgateManager.getBottomDoorLocation(targetBlock);
            found = plugin.getTollgateManager().getTollgate(bottomDoor);
        } else if (Tag.SIGNS.isTagged(targetBlock.getType())) {
            for (TollgateData data : plugin.getTollgateManager().getAllTollgates()) {
                if (data.getSignLocation().equals(targetBlock.getLocation())) {
                    found = data;
                    break;
                }
            }
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cYou must look at an iron door or tollgate sign."));
            return true;
        }

        if (found != null) {
            plugin.getTollgateManager().unregisterTollgate(found.getDoorLocation());
            sendMessage(sender, "tollgate-removed");

            // Clear the sign text at the sign location
            Block signBlock = found.getSignLocation().getBlock();
            if (signBlock.getChunk().isLoaded() && signBlock.getState() instanceof Sign sign) {
                sign.setLine(0, "");
                sign.setLine(1, "");
                sign.setLine(2, "");
                sign.setLine(3, "");
                sign.update();
            }
        } else {
            sendMessage(sender, "tollgate-not-found");
        }

        return true;
    }

    /**
     * Provides tab completion for /tollgate subcommands.
     * Suggests reload, list, or remove based on partial input.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return Arrays.asList("reload", "list", "remove").stream()
                    .filter(s -> s.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * Sends a message from the plugin configuration to the given sender.
     * The message key is looked up in the config manager's messages section.
     */
    private void sendMessage(CommandSender sender, String configKey) {
        sender.sendMessage(plugin.getConfigManager().getMessage(configKey));
    }

    /**
     * Formats a location as a human-readable string: "worldName (x, y, z)".
     */
    private String formatLocation(Location loc) {
        return loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}

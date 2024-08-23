package net.crypticmc.dynmapgriefprevention;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final DynmapGriefPrevention plugin;
    private MarkerHandler markerHandler;

    public CommandHandler(DynmapGriefPrevention plugin, MarkerHandler markerHandler) {
        this.plugin = plugin;
        this.markerHandler = markerHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage("Usage: /dynmapgp reload");
            return true;
        }

        if (sender instanceof Player && !sender.hasPermission("dynmapgp.reload")) {
            sender.sendMessage("You don't have permission to reload this plugin.");
            return true;
        }

        plugin.reloadConfig();
        plugin.markerHandler = new MarkerHandler(plugin);
        plugin.markerHandler.activate(true);

        sender.sendMessage("DynmapGriefPrevention configuration reloaded.");
        return true;
    }
}

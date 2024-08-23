package net.crypticmc.dynmapgriefprevention;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import java.util.HashMap;
import java.util.Map;

public class DynmapGriefPrevention extends JavaPlugin {

    public static final String DEF_INFOWINDOW = "<div class=\"infowindow\">Claim Owner: <span style=\"font-weight:bold;\">%owner%</span><br/>Permission Trust: <span style=\"font-weight:bold;\">%managers%</span><br/>Trust: <span style=\"font-weight:bold;\">%builders%</span><br/>Container Trust: <span style=\"font-weight:bold;\">%containers%</span><br/>Access Trust: <span style=\"font-weight:bold;\">%accessors%</span></div>";
    public static final String DEF_ADMININFOWINDOW = "<div class=\"infowindow\"><span style=\"font-weight:bold;\">Administrator Claim</span></div>";
    public static final long TWO_SECONDS_IN_TICKS = 40L; // 2 seconds * 20 ticks per second

    private static Permission perms = null;
    private Plugin dynmap;
    DynmapAPI api;
    MarkerAPI markerapi;
    GriefPrevention griefPrevention;
    MarkerSet set;
    Map<String, AreaMarker> resareas;
    MarkerHandler markerHandler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        resareas = new HashMap<>();
        markerHandler = new MarkerHandler(this);

        if (!initializeDynmap() || !initializeGriefPrevention()) {
            return;
        }

        setupVault();
        activate();

        getCommand("dynmapgp").setExecutor(new CommandHandler(this, markerHandler));

        getLogger().info("Enabled successfully.");
    }

    private boolean initializeDynmap() {
        dynmap = getServer().getPluginManager().getPlugin("dynmap");
        if (dynmap == null || !dynmap.isEnabled()) {
            getLogger().severe("Unable to find Dynmap! The plugin will shut down.");
            disablePlugin();
            return false;
        }
        api = (DynmapAPI) dynmap;
        markerapi = api.getMarkerAPI();
        if (markerapi == null) {
            getLogger().severe("Unable to load Dynmap marker API!");
            disablePlugin();
            return false;
        }
        return true;
    }

    private boolean initializeGriefPrevention() {
        Plugin gpPlugin = getServer().getPluginManager().getPlugin("GriefPrevention");
        if (gpPlugin == null || !gpPlugin.isEnabled()) {
            getLogger().severe("Unable to find GriefPrevention! The plugin will shut down.");
            disablePlugin();
            return false;
        }
        griefPrevention = (GriefPrevention) gpPlugin;
        return true;
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
            if (rsp != null) {
                perms = rsp.getProvider();
                getLogger().info("Vault found and permissions are being used.");
            } else {
                getLogger().warning("Vault found, but no Permission provider is available.");
            }
        } else {
            getLogger().info("Vault not found. Skipping permission-based features.");
        }
    }

    public String getPlayerRank(Player player) {
        if (player == null || perms == null) {
            return "unknown";
        }
        String primaryGroup = perms.getPrimaryGroup(player);
        return (primaryGroup == null || primaryGroup.isEmpty()) ? "unknown" : primaryGroup;
    }

    void activate() {
        markerHandler.activate(false);
    }

    void disablePlugin() {
        getServer().getPluginManager().disablePlugin(this);
    }
}

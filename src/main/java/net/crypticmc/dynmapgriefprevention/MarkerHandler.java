package net.crypticmc.dynmapgriefprevention;

import org.bukkit.scheduler.BukkitRunnable;
import java.util.HashSet;
import java.util.Set;

public class MarkerHandler {

    private final DynmapGriefPrevention plugin;
    private final UpdateProcessing updateProcessing;

    public MarkerHandler(DynmapGriefPrevention plugin) {
        this.plugin = plugin;
        boolean use3d = plugin.getConfig().getBoolean("use3dregions", false);
        String infowindow = plugin.getConfig().getString("infowindow", DynmapGriefPrevention.DEF_INFOWINDOW);
        String admininfowindow = plugin.getConfig().getString("adminclaiminfowindow", DynmapGriefPrevention.DEF_ADMININFOWINDOW);

        AreaStyle defstyle = new AreaStyle(plugin.getConfig(), "regionstyle");
        AreaStyle adminstyle = new AreaStyle(plugin.getConfig(), "adminstyle");

        Set<String> visible = new HashSet<>(plugin.getConfig().getStringList("visibleregions"));
        Set<String> hidden = new HashSet<>(plugin.getConfig().getStringList("hiddenregions"));

        this.updateProcessing = new UpdateProcessing(plugin, use3d, defstyle, adminstyle, visible, hidden, admininfowindow, infowindow);

        plugin.getLogger().info("infowindow loaded: " + infowindow);
        plugin.getLogger().info("adminclaiminfowindow loaded: " + admininfowindow);
    }

    public void activate(boolean reload) {
        plugin.markerapi = plugin.api.getMarkerAPI();
        if (plugin.markerapi == null) {
            plugin.getLogger().severe("Unable to load dynmap marker API!");
            plugin.disablePlugin();
            return;
        }

        if (reload) {
            plugin.reloadConfig();
            if (plugin.set != null) {
                plugin.set.deleteMarkerSet();
                plugin.set = null;
            }
            plugin.resareas.clear();
        }

        plugin.set = plugin.markerapi.getMarkerSet("griefprevention.markerset");
        if (plugin.set == null) {
            plugin.set = plugin.markerapi.createMarkerSet(
                    "griefprevention.markerset",
                    plugin.getConfig().getString("layer.name", "GriefPrevention"),
                    null,
                    false);
        } else {
            plugin.set.setMarkerSetLabel(plugin.getConfig().getString("layer.name", "GriefPrevention"));
        }

        if (plugin.set == null) {
            plugin.getLogger().severe("Unable to create marker set!");
            plugin.disablePlugin();
            return;
        }

        plugin.set.setMinZoom(plugin.getConfig().getInt("layer.minzoom", 0));
        plugin.set.setLayerPriority(plugin.getConfig().getInt("layer.layerprio", 10));
        plugin.set.setHideByDefault(plugin.getConfig().getBoolean("layer.hidebydefault", false));

        startUpdateTask();

        plugin.getLogger().info("Activated successfully.");
    }
    private void startUpdateTask() {
        long updatePeriod = 20L * Math.max(15L, plugin.getConfig().getLong("update.period", 300L));

        new BukkitRunnable() {
            @Override
            public void run() {
                updateProcessing.updateClaims();
            }
        }.runTaskTimerAsynchronously(plugin, DynmapGriefPrevention.TWO_SECONDS_IN_TICKS, updatePeriod);
    }
}

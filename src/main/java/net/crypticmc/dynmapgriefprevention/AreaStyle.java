package net.crypticmc.dynmapgriefprevention;

import org.bukkit.configuration.file.FileConfiguration;

public class AreaStyle {

    String strokecolor;
    double strokeopacity;
    int strokeweight;
    String fillcolor;
    double fillopacity;
    String label;

    public AreaStyle(FileConfiguration cfg, String path) {
        this(cfg, path, new AreaStyle("#FF0000", "#FF0000", 0.8, 0.35));
    }

    public AreaStyle(FileConfiguration cfg, String path, AreaStyle def) {
        strokecolor = cfg.getString(path + ".strokeColor", def.strokecolor);
        strokeopacity = cfg.getDouble(path + ".strokeOpacity", def.strokeopacity);
        strokeweight = cfg.getInt(path + ".strokeWeight", def.strokeweight);
        fillcolor = cfg.getString(path + ".fillColor", def.fillcolor);
        fillopacity = cfg.getDouble(path + ".fillOpacity", def.fillopacity);
        label = cfg.getString(path + ".label", def.label);
    }

    public AreaStyle(String strokecolor, String fillcolor, double strokeopacity, double fillopacity) {
        this.strokecolor = strokecolor;
        this.fillcolor = fillcolor;
        this.strokeopacity = strokeopacity;
        this.fillopacity = fillopacity;
        this.strokeweight = 3; // Default weight
        this.label = null; // No label by default
    }
}

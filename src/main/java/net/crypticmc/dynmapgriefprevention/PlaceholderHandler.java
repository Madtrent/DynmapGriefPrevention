package net.crypticmc.dynmapgriefprevention;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaceholderHandler {

    private final Map<String, String> placeholders;
    private final DynmapGriefPrevention plugin;

    public PlaceholderHandler(FileConfiguration config, DynmapGriefPrevention plugin) {
        placeholders = new HashMap<>();
        this.plugin = plugin;
        loadDefaultPlaceholders(config);
    }

    private void loadDefaultPlaceholders(FileConfiguration config) {
        if (config.isConfigurationSection("placeholderdefaults")) {
            for (String key : config.getConfigurationSection("placeholderdefaults").getKeys(false)) {
                String defaultValue = config.getString("placeholderdefaults." + key, "");
                placeholders.put(key, defaultValue);
            }
        }
    }

    public String replacePlaceholders(String text, Player player, Claim claim) {
        String result = text;

        // Replace placeholders
        result = result.replace("%rank%", getRank(player));
        result = result.replace("%owner%", getOwner(claim));
        result = result.replace("%accessors%", getAccessors(claim));
        result = result.replace("%managers%", getManagers(claim));
        result = result.replace("%builders%", getBuilders(claim));
        result = result.replace("%containers%", getContainers(claim));
        result = result.replace("%trust%", getTrust(claim)); // Add %trust% placeholder

        return result;
    }

    private String getRank(Player player) {
        String rank = placeholders.getOrDefault("rank", "unknown");
        if (player != null) {
            rank = plugin.getPlayerRank(player);
        }
        return rank.isEmpty() ? placeholders.get("rank") : rank;
    }

    private String getOwner(Claim claim) {
        String owner = placeholders.getOrDefault("owner", "unknown");
        if (claim != null && claim.getOwnerName() != null) {
            owner = claim.getOwnerName();
        }
        return owner.isEmpty() ? placeholders.get("owner") : resolvePlayernameFromId(owner);
    }

    private String getAccessors(Claim claim) {
        return getTrustedPlayers(claim, "accessors");
    }

    private String getManagers(Claim claim) {
        return getTrustedPlayers(claim, "managers");
    }

    private String getBuilders(Claim claim) {
        return getTrustedPlayers(claim, "builders");
    }

    private String getContainers(Claim claim) {
        return getTrustedPlayers(claim, "containers");
    }

    private String getTrust(Claim claim) {
        String trust = placeholders.getOrDefault("trust", "");
        if (claim != null) {
            ArrayList<String> builders = new ArrayList<>();
            ArrayList<String> containers = new ArrayList<>();
            ArrayList<String> accessorsList = new ArrayList<>();
            ArrayList<String> managers = new ArrayList<>();

            claim.getPermissions(builders, containers, accessorsList, managers);

            // Combine all trusted players into one list
            List<String> allTrusted = new ArrayList<>();
            allTrusted.addAll(builders);
            allTrusted.addAll(containers);
            allTrusted.addAll(accessorsList);
            allTrusted.addAll(managers);

            if (!allTrusted.isEmpty()) {
                trust = resolveNames(allTrusted);
            }
        }
        return trust.isEmpty() ? placeholders.get("trust") : trust;
    }

    private String getTrustedPlayers(Claim claim, String placeholderKey) {
        String placeholderValue = placeholders.getOrDefault(placeholderKey, "");
        if (claim != null) {
            ArrayList<String> builders = new ArrayList<>();
            ArrayList<String> containers = new ArrayList<>();
            ArrayList<String> accessorsList = new ArrayList<>();
            ArrayList<String> managers = new ArrayList<>();

            claim.getPermissions(builders, containers, accessorsList, managers);

            switch (placeholderKey) {
                case "accessors":
                    return !accessorsList.isEmpty() ? resolveNames(accessorsList) : placeholderValue;
                case "managers":
                    return !managers.isEmpty() ? resolveNames(managers) : placeholderValue;
                case "builders":
                    return !builders.isEmpty() ? resolveNames(builders) : placeholderValue;
                case "containers":
                    return !containers.isEmpty() ? resolveNames(containers) : placeholderValue;
            }
        }
        return placeholderValue;
    }

    private String resolveNames(List<String> uuidList) {
        List<String> names = new ArrayList<>();
        for (String uuidString : uuidList) {
            names.add(resolvePlayernameFromId(uuidString));
        }
        return String.join(", ", names);
    }

    private String resolvePlayernameFromId(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return player.getName() != null ? player.getName() : uuidString;
        } catch (IllegalArgumentException e) {
            return uuidString;
        }
    }
}

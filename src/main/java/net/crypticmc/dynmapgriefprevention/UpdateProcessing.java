package net.crypticmc.dynmapgriefprevention;

import com.avaje.ebean.validation.NotNull;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.markers.AreaMarker;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

public class UpdateProcessing {
    private static final String ADMIN_ID = "administrator";
    private static final long CLAIM_FETCH_TIMEOUT_MS = 500L;

    private final DynmapGriefPrevention main;
    private final Pattern idPattern;
    private final boolean use3d;
    private final AreaStyle defstyle;
    private final AreaStyle adminstyle;
    private final Set<String> visible;
    private final Set<String> hidden;
    private final String admininfowindow;
    private final String infowindow;
    private final Map<UUID, String> playerNameCache = new TreeMap<>();
    private final PlaceholderHandler placeholderHandler;

    public UpdateProcessing(DynmapGriefPrevention main, boolean use3d, AreaStyle defstyle, AreaStyle adminstyle, Set<String> visible, Set<String> hidden, String admininfowindow, String infowindow) {
        this.main = main;
        this.use3d = use3d;
        this.defstyle = defstyle;
        this.adminstyle = adminstyle;
        this.visible = visible;
        this.hidden = hidden;
        this.admininfowindow = admininfowindow;
        this.infowindow = infowindow;
        this.idPattern = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-5][0-9a-f]{3}-[089ab][0-9a-f]{3}-[0-9a-f]{12}$");
        this.placeholderHandler = new PlaceholderHandler(main.getConfig(), main);
    }

    @Nullable
    private ArrayList<Claim> getClaims() {
        try {
            Field field = DataStore.class.getDeclaredField("claims");
            field.setAccessible(true);
            return (ArrayList<Claim>) field.get(main.griefPrevention.dataStore);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            main.getLogger().warning("Error getting claims from reflection: " + e.getMessage());
            return null;
        }
    }

    private @Nullable ArrayList<Claim> getClaimsAsync() {
        CompletableFuture<ArrayList<Claim>> completableFuture = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                completableFuture.complete(getClaims());
            }
        }.runTask(main);

        try {
            return completableFuture.get(CLAIM_FETCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            main.getLogger().severe("Error while fetching claims asynchronously: " + e.getMessage());
            return null;
        }
    }

    void updateClaims() {
        Map<String, AreaMarker> newmap = new HashMap<>();
        ArrayList<Claim> claims = getClaimsAsync();

        int parentClaims = 0, childClaims = 0, deletions = 0;

        if (claims != null) {
            for (Claim claim : claims) {
                handleClaim(claim, newmap);
                parentClaims++;
                if (claim.children != null) {
                    for (Claim childClaim : claim.children) {
                        handleClaim(childClaim, newmap);
                        childClaims++;
                    }
                }
            }
        }

        for (AreaMarker oldMarker : main.resareas.values()) {
            oldMarker.deleteMarker();
            deletions++;
        }

        main.resareas = newmap;

        if (main.getConfig().getBoolean("debug", false)) {
            main.getLogger().info(String.format("claims: %d, child claims: %d, deletions: %d", parentClaims, childClaims, deletions));
        }
    }

    private void handleClaim(Claim claim, Map<String, AreaMarker> newmap) {
        Location l0 = claim.getLesserBoundaryCorner();
        Location l1 = claim.getGreaterBoundaryCorner();

        if (l0 == null) return;

        String worldName = l0.getWorld() != null ? l0.getWorld().getName() : "";
        String owner = claim.isAdminClaim() ? ADMIN_ID : Optional.ofNullable(claim.getOwnerName()).orElse("unknown");

        if (!isVisible(owner, worldName)) return;

        double[] x = {l0.getX(), l0.getX(), l1.getX() + 1.0, l1.getX() + 1.0};
        double[] z = {l0.getZ(), l1.getZ() + 1.0, l1.getZ() + 1.0, l0.getZ()};
        Long id = claim.getID();
        String markerId = "GP_" + Long.toHexString(id);
        AreaMarker marker = main.resareas.remove(markerId);

        if (marker == null) {
            marker = main.set.createAreaMarker(markerId, owner, false, worldName, x, z, false);
        } else {
            marker.setCornerLocations(x, z);
            marker.setLabel(owner);
        }

        if (use3d) {
            marker.setRangeY(l1.getY() + 1.0, l0.getY());
        }

        if (claim.isAdminClaim()) {
            applyStyle(marker, adminstyle);
        } else {
            applyStyle(marker, defstyle);
        }

        marker.setDescription(formatInfoWindow(claim));
        newmap.put(markerId, marker);
    }

    private void applyStyle(AreaMarker marker, AreaStyle style) {
        try {
            int strokeColor = Integer.parseInt(style.strokecolor.substring(1), 16);
            int fillColor = Integer.parseInt(style.fillcolor.substring(1), 16);
            marker.setLineStyle(style.strokeweight, style.strokeopacity, strokeColor);
            marker.setFillStyle(style.fillopacity, fillColor);
            if (style.label != null) {
                marker.setLabel(style.label);
            }
        } catch (NumberFormatException e) {
            main.getLogger().warning("Error parsing style colors: " + e.getMessage());
        }
    }

    private boolean isVisible(String owner, String worldName) {
        if (!visible.isEmpty() && !visible.contains(owner) && !visible.contains("world:" + worldName) && !visible.contains(worldName + "/" + owner)) {
            return false;
        }
        if (!hidden.isEmpty() && (hidden.contains(owner) || hidden.contains("world:" + worldName) || hidden.contains(worldName + "/" + owner))) {
            return false;
        }
        return true;
    }

    @NotNull
    private String formatInfoWindow(Claim claim) {
        String v;
        if (claim.isAdminClaim()) {
            v = main.getConfig().getString("adminclaiminfowindow", DynmapGriefPrevention.DEF_ADMININFOWINDOW);
        } else {
            v = main.getConfig().getString("infowindow", DynmapGriefPrevention.DEF_INFOWINDOW);
        }

        Player player = Bukkit.getPlayer(claim.getOwnerName());
        v = placeholderHandler.replacePlaceholders(v, player, claim);

        return v;
    }


    private String generateCommaSeparatedList(List<String> items) {
        return String.join(", ", items);
    }

    private boolean isStringUUID(String input) {
        return idPattern.matcher(input).matches();
    }

    @NotNull
    private String resolvePlayernameFromId(String playerId) {
        UUID id = UUID.fromString(playerId);
        return playerNameCache.computeIfAbsent(id, key -> {
            Player player = Bukkit.getPlayer(id);
            return player != null ? player.getName() : Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse(playerId);
        });
    }
}

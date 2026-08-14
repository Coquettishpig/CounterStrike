package src.counterstrike.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;

public class GameUtils {

    /**
     * 仅获取服务器原始版本字符串，不做任何处理
     * 例如返回: "1.21.1-R0.1-SNAPSHOT"
     */
    public static String getServerVersion() {
        return Bukkit.getBukkitVersion();
    }

    public static List<Location> getDeserializedLocations(final List<String> list) {
        final List<Location> loclist = new ArrayList<>();
        for (final String l : list) {
            loclist.add(getDeserializedLocation(l));
        }
        return loclist;
    }

    public static List<String> getSerializedLocations(final List<Location> list) {
        final List<String> loclist = new ArrayList<>();
        for (final Location l : list) {
            loclist.add(getSerializedLocation(l));
        }
        return loclist;
    }

    public static boolean containsIgnoreCase(final List<String> list, final String cmd) {
        for (final String value : list) {
            if (cmd.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    public static String getSerializedLocation(final Location l) {
        if (l == null || l.getWorld() == null) return "";
        return l.getWorld().getName() + "," + (l.getBlockX() + 0.5) + "," + l.getBlockY() + "," + (l.getBlockZ() + 0.5) + "," + l.getYaw() + "," + l.getPitch();
    }

    public static Location getDeserializedLocation(final String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            final String[] st = s.split(",");
            return new Location(Bukkit.getWorld(st[0]), Double.parseDouble(st[1]), Double.parseDouble(st[2]), Double.parseDouble(st[3]), Float.parseFloat(st[4]), Float.parseFloat(st[5]));
        } catch (Exception e) {
            return null;
        }
    }
}
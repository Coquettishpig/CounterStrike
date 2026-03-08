package src.counterstrike.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class GameUtils {
    private static String version;

    public static String getServerVersion() {
        return GameUtils.version;
    }

    public static List<Location> getDeserializedLocations(final List<String> list) {
        final List<Location> loclist = new ArrayList<Location>();
        for (final String l : list) {
            loclist.add(getDeserializedLocation(l));
        }
        return loclist;
    }

    public static List<String> getSerializedLocations(final List<Location> list) {
        final List<String> loclist = new ArrayList<String>();
        for (final Location l : list) {
            loclist.add(getSerializedLocation(l));
        }
        return loclist;
    }

    public static boolean containsIgnoreCase(final List<String> list, final String cmd) {
        for (final String value : list) {
            if (cmd.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static String getSerializedLocation(final Location l) {
        return l.getWorld().getName() + "," + (l.getBlockX() + 0.5) + "," + l.getBlockY() + "," + (l.getBlockZ() + 0.5) + "," + l.getYaw() + "," + l.getPitch();
    }

    public static Location getDeserializedLocation(final String s) {
        if (s == null) {
            return null;
        }
        final String[] st = s.split(",");
        return new Location(Bukkit.getWorld(st[0]), Double.parseDouble(st[1]), Double.parseDouble(st[2]) + 1.0, Double.parseDouble(st[3]), Float.parseFloat(st[4]), Float.parseFloat(st[5]));
    }

    static {
        GameUtils.version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }
}

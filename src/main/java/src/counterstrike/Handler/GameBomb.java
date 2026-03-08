package src.counterstrike.Handler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GameBomb {
    private Item item;
    private int timer;
    private Location l;
    private Player carrier;
    private boolean isPlanted;

    public GameBomb() {
        this.isPlanted = false;
    }

    public boolean isPlanted() {
        return this.isPlanted;
    }

    public void setDrop(final Item item) {
        this.item = item;
        this.carrier = null;
    }

    public void setCarrier(final Player carrier) {
        this.l = null;
        this.carrier = carrier;
        this.item = null;
    }

    public Player getCarrier() {
        return this.carrier;
    }

    public int getTimer() {
        return this.timer;
    }

    public void setTimer(final int timer) {
        this.timer = timer;
    }

    public void setLocation(final Location l) {
        this.l = l;
        this.carrier = null;
        this.item = null;
    }

    public void reset() {
        this.timer = 0;
        if (this.l != null) {
            this.l.getBlock().setType(Material.AIR);
            this.l = null;
        }
        this.item = null;
        this.carrier = null;
        this.isPlanted = false;
    }

    public List<Player> getNearbyPlayers(final Game g, final int distance) {
        final List<Player> players = new ArrayList<Player>();
        for (final Player p : g.getTeamA().getPlayers()) {
            if (p.getLocation().distance(this.l) <= distance) {
                players.add(p);
            }
        }
        for (final Player p : g.getTeamB().getPlayers()) {
            if (p.getLocation().distance(this.l) <= distance) {
                players.add(p);
            }
        }
        return players;
    }

    public Location getLocation() {
        if (this.l != null) {
            return this.l;
        }
        if (this.carrier != null) {
            return this.carrier.getLocation();
        }
        return this.item.getLocation();
    }

    public void isPlanted(final boolean value) {
        this.isPlanted = value;
    }
}

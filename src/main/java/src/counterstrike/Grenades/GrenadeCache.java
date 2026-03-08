package src.counterstrike.Grenades;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import src.counterstrike.Handler.Game;
import src.counterstrike.Version.Entity.NMSPsyhicsItem;

import java.util.ArrayList;
import java.util.List;

public class GrenadeCache
{
    private Game g;
    private Player p;
    private long time;
    private Long duration;
    private NMSPsyhicsItem grenade;
    private List<Block> blocks;
    
    public GrenadeCache(final Game g, final long time, final Player p, final NMSPsyhicsItem grenade) {
        this.blocks = new ArrayList<Block>();
        this.g = g;
        this.p = p;
        this.time = time;
        this.grenade = grenade;
    }
    
    public Game getGame() {
        return this.g;
    }
    
    public Long getDuration() {
        return this.duration;
    }
    
    public List<Block> getBlocks() {
        return this.blocks;
    }
    
    public void setDuration(final long duration) {
        this.duration = duration;
    }
    
    public long getTime() {
        return this.time;
    }
    
    public Player getPlayer() {
        return this.p;
    }
    
    public NMSPsyhicsItem getGrenade() {
        return this.grenade;
    }
    
    public List<Player> getNearbyPlayers(final double distance) {
        final List<Player> players = new ArrayList<Player>();
        for (final Player p : this.g.getTeamA().getPlayers()) {
            if (p.getLocation().getWorld() == this.grenade.getLocation().getWorld() && p.getLocation().distance(this.grenade.getLocation()) <= distance) {
                players.add(p);
            }
        }
        for (final Player p : this.g.getTeamB().getPlayers()) {
            if (p.getLocation().getWorld() == this.grenade.getLocation().getWorld() && p.getLocation().distance(this.grenade.getLocation()) <= distance) {
                players.add(p);
            }
        }
        return players;
    }
    
    public List<Player> getNearbyToBlockPlayers() {
        final List<Player> players = new ArrayList<Player>();
        for (final Player p : this.g.getTeamA().getPlayers()) {
            for (final Block block : this.blocks) {
                if (p.getLocation().getWorld() == block.getWorld() && p.getLocation().distance(block.getLocation()) <= 1.0) {
                    players.add(p);
                }
            }
        }
        for (final Player p : this.g.getTeamB().getPlayers()) {
            for (final Block block : this.blocks) {
                if (p.getLocation().getWorld() == block.getWorld() && p.getLocation().distance(block.getLocation()) <= 1.5) {
                    players.add(p);
                }
            }
        }
        return players;
    }
}

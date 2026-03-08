package src.counterstrike.Handler;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class GameSetup
{
    private int id;
    private int min;
    private int max;
    private String name;
    private Location lobby;
    private List<Location> cops;
    private List<Location> bombs;
    private List<Location> criminals;
    private List<Location> fireworks;
    
    public GameSetup(final int id, final String name, final int min, final int max) {
        this.cops = new ArrayList<Location>();
        this.bombs = new ArrayList<Location>();
        this.criminals = new ArrayList<Location>();
        this.fireworks = new ArrayList<Location>();
        this.setID(id);
        this.setName(name);
        this.setMin(min);
        this.setMax(max);
    }
    
    public Integer getID() {
        return this.id;
    }
    
    public void setID(final Integer id) {
        this.id = id;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(final String name) {
        this.name = name;
    }
    
    public List<Location> getCriminals() {
        return this.criminals;
    }
    
    public List<Location> getCops() {
        return this.cops;
    }
    
    public Location getLobby() {
        return this.lobby;
    }
    
    public void setLobby(final Location lobby) {
        this.lobby = lobby;
    }
    
    public List<Location> getBombs() {
        return this.bombs;
    }
    
    public List<Location> getFireworks() {
        return this.fireworks;
    }
    
    public int getMin() {
        return this.min;
    }
    
    public void setMin(final int min) {
        this.min = min;
    }
    
    public int getMax() {
        return this.max;
    }
    
    public void setMax(final int max) {
        this.max = max;
    }
}

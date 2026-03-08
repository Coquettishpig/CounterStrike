package src.counterstrike.Cache;

import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerStatus
{
    private int kills;
    private int deaths;
    private String uuid;
    private String name;
    private int headshotkills;
    private int roundKills;
    private int bombplanted;
    
    public PlayerStatus(final String name, final UUID uuid) {
        this.kills = 0;
        this.deaths = 0;
        this.uuid = null;
        this.name = null;
        this.headshotkills = 0;
        this.roundKills = 0;
        this.bombplanted = 0;
        this.uuid = uuid.toString();
        this.name = name;
    }
    
    public int getKills() {
        return this.kills;
    }
    
    public int getRoundKills() {
        return this.roundKills;
    }
    
    public int getDeaths() {
        return this.deaths;
    }
    
    public String getUUID() {
        return this.uuid;
    }
    
    public int getBombPlanted() {
        return this.bombplanted;
    }
    
    public int getHeadshotKill() {
        return this.headshotkills;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void addBombPlanted() {
        ++this.bombplanted;
    }
    
    public void addHeadshotKill() {
        ++this.headshotkills;
    }
    
    public void addKill() {
        ++this.kills;
        ++this.roundKills;
    }
    
    public void resetRound() {
        this.roundKills = 0;
    }
    
    public void addDeath() {
        ++this.deaths;
    }
}

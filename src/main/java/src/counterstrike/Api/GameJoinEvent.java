package src.counterstrike.Api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameJoinEvent extends Event
{
    private Player p;
    private static final HandlerList handlers;
    
    public GameJoinEvent(final Player p) {
        this.p = p;
    }
    
    public Player getPlayer() {
        return this.p;
    }
    
    public HandlerList getHandlers() {
        return GameJoinEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return GameJoinEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}

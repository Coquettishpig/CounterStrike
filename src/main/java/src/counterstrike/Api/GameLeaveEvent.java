package src.counterstrike.Api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GameLeaveEvent extends Event
{
    private Player p;
    private static final HandlerList handlers;
    
    public GameLeaveEvent(final Player p) {
        this.p = p;
    }
    
    public Player getPlayer() {
        return this.p;
    }
    
    public HandlerList getHandlers() {
        return GameLeaveEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return GameLeaveEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}

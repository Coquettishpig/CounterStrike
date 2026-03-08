package src.counterstrike.Api;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BombExplodeEvent extends Event
{
    private static final HandlerList handlers;
    private Location l;
    
    public BombExplodeEvent(final Location l) {
        this.l = l;
    }
    
    public Location getBombLocation() {
        return this.l;
    }
    
    public HandlerList getHandlers() {
        return BombExplodeEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return BombExplodeEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}

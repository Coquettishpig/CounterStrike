package src.counterstrike.Api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import src.counterstrike.Handler.Game;

public class RoundEndEvent extends Event
{
    private final Game game;
    private static final HandlerList handlers;
    
    public RoundEndEvent(final Game game) {
        this.game = game;
    }
    
    public Game getGame() {
        return this.game;
    }
    
    public HandlerList getHandlers() {
        return RoundEndEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return RoundEndEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}

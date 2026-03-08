package src.counterstrike.Api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import src.counterstrike.Handler.Game;

public class RoundStartEvent extends Event
{
    private final Game game;
    private Player p;
    private static final HandlerList handlers;
    
    public RoundStartEvent(final Game game) {
        this.game = game;
    }
    
    public Player getPlayer() {
        return this.p;
    }
    
    public Game getGame() {
        return this.game;
    }
    
    public HandlerList getHandlers() {
        return RoundStartEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return RoundStartEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}

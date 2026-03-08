package src.counterstrike.Api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import src.counterstrike.Handler.Game;
import src.counterstrike.Handler.GameState;

public class GameStateChangeEvent extends Event
{
    private Game game;
    private GameState state;
    private static final HandlerList handlers;
    
    public GameStateChangeEvent(final Game game, final GameState state) {
        this.game = game;
        this.state = state;
    }
    
    public GameState getState() {
        return this.state;
    }
    
    public Game getGame() {
        return this.game;
    }
    
    public HandlerList getHandlers() {
        return GameStateChangeEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return GameStateChangeEvent.handlers;
    }
    
    static {
        handlers = new HandlerList();
    }
}

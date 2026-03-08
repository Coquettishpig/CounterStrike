package src.counterstrike.Handler;

import src.counterstrike.Messages;

public enum GameState
{
    WAITING(Messages.STATE_WAITING),
    ROUND(Messages.STATE_IN_GAME), 
    IN_GAME(Messages.STATE_IN_GAME), 
    END(Messages.STATE_ENDING), 
    DISABLED(Messages.STATE_DISABLED);
    
    private Messages state;
    
    private GameState(final Messages state) {
        this.state = state;
    }
    
    public String getState() {
        return this.state.toString();
    }

}

package src.counterstrike.Grenades;

public enum GrenadeType
{
    FRAG(3, "cs_gamesounds.gamesounds.throwinggrenade"), 
    FIREBOMB(3, "cs_gamesounds.gamesounds.throwingfirebomb"), 
    DECOY(4, "cs_gamesounds.gamesounds.throwingdecoy"), 
    SMOKE(4, "cs_gamesounds.gamesounds.throwingsmoke"), 
    FLASHBANG(4, "cs_gamesounds.gamesounds.throwingflashbang");
    
    private final int slot;
    private final String sound;
    
    GrenadeType(final int slot, final String sound) {
        this.slot = slot;
        this.sound = sound;
    }
    
    public int getSlot() {
        return this.slot;
    }
    
    public String getSound() {
        return this.sound;
    }

}

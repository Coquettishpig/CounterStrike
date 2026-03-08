package src.counterstrike.Guns;

public enum GunType
{
    PRIMARY(0), 
    SECONDARY(1);
    
    private final int id;
    
    private GunType(final int id) {
        this.id = id;
    }
    
    public Integer getID() {
        return this.id;
    }

}

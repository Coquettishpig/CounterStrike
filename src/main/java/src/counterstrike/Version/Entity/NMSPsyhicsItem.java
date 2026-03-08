package src.counterstrike.Version.Entity;

import org.bukkit.Location;

public interface NMSPsyhicsItem
{
    void remove();
    
    boolean isRemoved();
    
    Location getLocation();
}

package src.counterstrike.Version;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import src.counterstrike.Version.Entity.NMSPsyhicsItem;

public class PsyhicsItem implements NMSPsyhicsItem
{
    private Item item;
    private boolean removed;
    
    public PsyhicsItem(final Player p, final ItemStack is, final double throwSpeedMultiplier) {
        this.removed = false;
        final ItemMeta im = is.getItemMeta();
        im.setDisplayName(MathUtils.randomString(15));
        is.setItemMeta(im);
        (this.item = p.getWorld().dropItem(p.getEyeLocation(), is)).setPickupDelay(Integer.MAX_VALUE);
        this.item.setVelocity(p.getEyeLocation().getDirection().multiply(throwSpeedMultiplier));
    }
    
    @Override
    public void remove() {
        this.item.remove();
        this.removed = true;
    }
    
    @Override
    public boolean isRemoved() {
        return this.removed;
    }
    
    @Override
    public Location getLocation() {
        return this.item.getLocation();
    }
}

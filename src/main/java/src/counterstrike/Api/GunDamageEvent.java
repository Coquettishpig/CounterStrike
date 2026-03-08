package src.counterstrike.Api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GunDamageEvent extends Event
{
    private double damage;
    private boolean isHeadshot;
    private Player damager;
    private Player victim;
    private static final HandlerList handlers;
    
    public GunDamageEvent(final double damage, final boolean isHeadshot, final Player damager, final Player victim) {
        this.damage = damage;
        this.isHeadshot = isHeadshot;
        this.damager = damager;
        this.victim = victim;
    }
    
    public HandlerList getHandlers() {
        return GunDamageEvent.handlers;
    }
    
    public static HandlerList getHandlerList() {
        return GunDamageEvent.handlers;
    }
    
    public double getDamage() {
        return this.damage;
    }
    
    public boolean isHeadshot() {
        return this.isHeadshot;
    }
    
    public Player getDamager() {
        return this.damager;
    }
    
    public Player getVictim() {
        return this.victim;
    }
    
    static {
        handlers = new HandlerList();
    }
}

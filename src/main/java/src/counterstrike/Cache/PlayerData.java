package src.counterstrike.Cache;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Scoreboard;
import src.counterstrike.Main;

import java.util.Collection;

public class PlayerData
{
    private Main main;
    private final float xp;
    private final int food;
    private final int level;
    private final Player p;
    private final Location loc;
    private final GameMode mode;
    private final double health;
    private final int fireticks;
    private final float flyspeed;
    private final float walkspeed;
    private final boolean isFlying;
    private final float fallDistance;
    private final Scoreboard scoreboard;
    private final ItemStack[] armour;
    private final ItemStack[] inventory;
    private final Collection<PotionEffect> effects;
    
    public PlayerData(final Main main, final Player p) {
        this.p = p;
        this.main = main;
        this.xp = p.getExp();
        this.level = p.getLevel();
        this.loc = p.getLocation();
        this.health = p.getHealth();
        this.mode = p.getGameMode();
        this.food = p.getFoodLevel();
        this.isFlying = p.getAllowFlight();
        this.flyspeed = p.getFlySpeed();
        this.fireticks = p.getFireTicks();
        this.walkspeed = p.getWalkSpeed();
        this.scoreboard = p.getScoreboard();
        this.fallDistance = p.getFallDistance();
        this.effects = (Collection<PotionEffect>)p.getActivePotionEffects();
        this.inventory = p.getInventory().getContents();
        this.armour = p.getInventory().getArmorContents();
        main.getManager().clearPlayer(p);
    }
    
    public void restore(final boolean teleport) {
        if (teleport) {
            this.p.teleport(this.loc);
        }
        this.p.setExp(this.xp);
        this.p.setLevel(this.level);
        this.p.setGameMode(this.mode);
        this.p.setHealth(20.0);
        this.p.setFoodLevel(this.food);
        if (this.isFlying) {
            this.p.setAllowFlight(true);
            this.p.setFlying(true);
        }
        else {
            this.p.setAllowFlight(true);
            this.p.setFlying(false);
            this.p.setAllowFlight(false);
        }
        this.p.setFlySpeed(this.flyspeed);
        this.p.setFireTicks(this.fireticks);
        this.p.setWalkSpeed(this.walkspeed);
        this.p.setFallDistance(this.fallDistance);
        this.p.getInventory().setArmorContents(this.armour);
        this.p.getInventory().setContents(this.inventory);
        this.main.getVersionInterface().setHandSpeed(this.p, 4.0);
        for (final PotionEffect effect : this.p.getActivePotionEffects()) {
            this.p.removePotionEffect(effect.getType());
        }
        for (final PotionEffect effect : this.effects) {
            this.p.addPotionEffect(effect);
        }
        this.p.setScoreboard(this.scoreboard);
        this.p.updateInventory();
    }
}

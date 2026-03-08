package src.counterstrike.Guns;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import src.counterstrike.Api.GunDamageEvent;
import src.counterstrike.Handler.Game;
import src.counterstrike.Main;
import src.counterstrike.Messages;
import src.counterstrike.Utils.Item;
import src.counterstrike.Version.MathUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class Gun
{
    private final Main main;
    private final Item item;
    private final String name;
    private final GunType type;
    private int duration;
    private int amount;
    private float accuracy;
    private double damage;
    private int bullets;
    private String symbol;
    private boolean snipe;
    private int delayshot;
    private int rounds;
    private final String shotsound;
    private int distance;
    private int delayrounds;
    private int RoundsPerYaw;
    private int MaxRoundsPerPitch;
    private int module;
    private final String reloadsound_end;
    private final String reloadsound_start;
    private final HashMap<UUID, Long> delay;
    private final HashMap<UUID, Integer> delayBlood;
    private final HashMap<UUID, GunCache> cache;
    private final HashMap<UUID, GunReload> inReloading;
    
    public Gun(final Main main, final String name, final Item item, final GunType type, final String shotsound, final String reloadsound_start, final String reloadsound_end) {
        this.delay = new HashMap<UUID, Long>();
        this.delayBlood = new HashMap<UUID, Integer>();
        this.cache = new HashMap<UUID, GunCache>();
        this.inReloading = new HashMap<UUID, GunReload>();
        this.main = main;
        this.name = name;
        this.item = item;
        this.type = type;
        this.shotsound = shotsound;
        this.reloadsound_end = reloadsound_end;
        this.reloadsound_start = reloadsound_start;
    }
    
    public void shot(final Game g, final Player p) {
        if (p.getInventory().getHeldItemSlot() == this.type.getID()) {
            final long now = System.currentTimeMillis() / 49L;
            if (this.delay.get(p.getUniqueId()) == null) {
                this.delay.put(p.getUniqueId(), now);
            }
            else if (now - this.delay.get(p.getUniqueId()) <= this.delayshot) {
                return;
            }
            final GunCache cache = this.cache.get(p.getUniqueId());
            if (cache == null) {
                this.cache.put(p.getUniqueId(), new GunCache(g, p.isSneaking() ? 1 : this.rounds));
            }
            else {
                cache.setRounds(p.isSneaking() ? 1 : this.rounds);
            }
            this.delay.put(p.getUniqueId(), now);
        }
    }
    
    public void reload(final Player p, final int slot) {
        final ItemStack gun = p.getInventory().getItem(slot);
        if (this.item.equals(gun, this.symbol) && gun.getAmount() < this.amount && !this.inReloading.containsKey(p.getUniqueId())) {
            p.playSound(p.getEyeLocation(), this.reloadsound_start, 5.0f, 1.0f);
            int duration = this.duration;
//            if (p.hasPermission("cs.weapon.faster_reload70")) {
//                duration *= (int)0.7;
//            }
//            else if (p.hasPermission("cs.weapon.faster_reload50")) {
//                duration *= (int)0.5;
//            }
            this.inReloading.put(p.getUniqueId(), new GunReload(duration));
        }
    }

    // 修改 getAdjustedAccuracy 方法，加入狙击枪不开镜的检测
    private float getAdjustedAccuracy(Player p, float baseAccuracy) {
        float adjustedAccuracy = baseAccuracy;

        // 检查玩家是否在跳跃（不在地面且有向上的速度）
        if (!p.isOnGround() && p.getVelocity().getY() > 0) {
            adjustedAccuracy += 5.0f;
        }

        // 检查是否是狙击枪且未开镜（未潜行）
        if (this.snipe && !p.isSneaking()) {
            adjustedAccuracy += 3.0f;
        }

        return adjustedAccuracy;
    }
    
    public String getSymbol() {
        return this.symbol;
    }
    
    public void setRoundsPerYaw(final int RoundsPerYaw) {
        this.RoundsPerYaw = RoundsPerYaw;
    }
    
    public void setMaxRoundsPerPitch(final int MaxRoundsPerPitch) {
        this.MaxRoundsPerPitch = MaxRoundsPerPitch;
    }
    
    public void setDelayRounds(final int delayrounds) {
        this.delayrounds = delayrounds;
    }
    
    public void setRounds(final int rounds) {
        this.rounds = rounds;
    }
    
    public void setDistance(final int distance) {
        this.distance = distance;
    }
    
    public void setDamage(final double damage) {
        this.damage = damage;
    }
    
    public void setDuration(final int duration) {
        this.duration = duration;
    }
    
    public int getModule() {
        return this.module;
    }
    
    public void setSymbol(final String symbol) {
        this.symbol = symbol;
    }
    
    public void setModule(final int module) {
        this.module = module;
    }
    
    public void setAccuracy(final float accuracy) {
        this.accuracy = accuracy;
    }
    
    public void setDelay(final int delayshot) {
        this.delayshot = delayshot;
    }
    
    public void setBullets(final int bullets) {
        this.bullets = bullets;
    }
    
    public void setAmount(final int amount) {
        this.amount = amount;
    }
    
    public void resetPlayer(final Player p) {
        this.inReloading.remove(p.getUniqueId());
        this.cache.remove(p.getUniqueId());
    }
    
    public int getAmount() {
        return this.amount;
    }
    
    public void hasSnipe(final boolean snipe) {
        this.snipe = snipe;
    }
    
    public boolean hasSnipe() {
        return this.snipe;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Item getItem() {
        return this.item;
    }
    
    public GunType getGunType() {
        return this.type;
    }
    
    public void resetDelay(final Player p) {
        this.delay.remove(p.getUniqueId());
    }
    
    public void tick() {
        if (!this.inReloading.isEmpty()) {
            final Iterator<Map.Entry<UUID, GunReload>> it = this.inReloading.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<UUID, GunReload> entry = it.next();
                final Player p = Bukkit.getPlayer((UUID)entry.getKey());
                if (p != null && !p.isDead() && p.isOnline() && this.main.getManager().getGame(p) != null) {
                    if (this.item.equals(p.getInventory().getItemInHand(), this.symbol)) {
                        if (this.module == 2) {
                            p.setExp((float)(1.0 - entry.getValue().getLeft() / entry.getValue().getMaxTime()));
                            if (entry.getValue().getLeft() <= 0.0) {
                                it.remove();
                                p.setExp(0.0f);
                                p.getInventory().getItemInHand().setAmount(this.amount);
                                p.playSound(p.getEyeLocation(), this.reloadsound_end, 5.0f, 1.0f);
                            }
                        }
                        else {
                            p.getInventory().getItemInHand().setDurability((short)(entry.getValue().getLeft() / entry.getValue().getMaxTime() * p.getInventory().getItemInHand().getType().getMaxDurability()));
                            if (entry.getValue().getLeft() <= 0.0) {
                                it.remove();
                                p.getInventory().getItemInHand().setAmount(this.amount);
                                p.getInventory().getItemInHand().setDurability((short)0);
                                p.playSound(p.getEyeLocation(), this.reloadsound_end, 5.0f, 1.0f);
                            }
                        }
                        entry.getValue().setLeft((int)entry.getValue().getLeft() - 1);
                    }
                    else {
                        it.remove();
                    }
                }
                else {
                    it.remove();
                }
            }
        }
        if (!this.delayBlood.isEmpty()) {
            final Iterator<Map.Entry<UUID, Integer>> it2 = this.delayBlood.entrySet().iterator();
            while (it2.hasNext()) {
                final Map.Entry<UUID, Integer> entry2 = it2.next();
                if (entry2.getValue() > 0) {
                    entry2.setValue(entry2.getValue() - 1);
                }
                else {
                    it2.remove();
                }
            }
        }
        if (!this.delay.isEmpty()) {
            final Iterator<Map.Entry<UUID, Long>> it3 = this.delay.entrySet().iterator();
            while (it3.hasNext()) {
                final Map.Entry<UUID, Long> entry3 = it3.next();
                final Player p = Bukkit.getPlayer((UUID)entry3.getKey());
                final long now = System.currentTimeMillis() / 49L;
                if (p == null || p.isDead() || !p.isOnline() || this.main.getManager().getGame(p) == null || now - entry3.getValue() > this.delayshot) {
                    it3.remove();
                }
            }
        }
        if (!this.cache.isEmpty()) {
            final Iterator<Map.Entry<UUID, GunCache>> it4 = this.cache.entrySet().iterator();
            while (it4.hasNext()) {
                final Map.Entry<UUID, GunCache> entry4 = it4.next();
                final Player p = Bukkit.getPlayer((UUID)entry4.getKey());
                final GunCache cache = entry4.getValue();
                if (!this.inReloading.containsKey(entry4.getKey())) {
                    if (p != null && !p.isDead() && p.isOnline() && cache.getGame() != null) {
                        if (!cache.getGame().getSpectators().contains(p)) {
                            cache.setTicksLeft(cache.getTicksLeft() - 1);
                            if (cache.getRounds() > 0 && this.item.equals(p.getInventory().getItemInHand(), this.symbol)) {
                                cache.setTicks(cache.getTicks() + 1);
                                if (cache.isFirstShot()) {
                                    cache.setFirstShot(false);
                                }
                                else if (cache.getTicks() % this.delayrounds != 0) {
                                    return;
                                }
                                if (p.getInventory().getItemInHand().getAmount() <= 1) {
                                    it4.remove();
                                    this.reload(p, this.type.getID());
                                    for (final Player a : cache.getGame().getTeamA().getPlayers()) {
                                        a.playSound(p.getLocation(), "cs_gamesounds.gamesounds.reloading", 1.0f, 1.0f);
                                    }
                                    for (final Player b : cache.getGame().getTeamB().getPlayers()) {
                                        b.playSound(p.getLocation(), "cs_gamesounds.gamesounds.reloading", 1.0f, 1.0f);
                                    }
                                    return;
                                }
                                cache.setRounds(cache.getRounds() - 1);
                                p.getInventory().getItemInHand().setAmount(p.getInventory().getItemInHand().getAmount() - 1);
                                final Location l = p.getEyeLocation();
                                if (p.isSneaking()) {
                                    l.subtract(0.0, 0.03, 0.0);
                                }
                                else {
                                    l.subtract(0.0, 0.01, 0.0);
                                }
                                for (final Player player : cache.getGame().getTeamA().getPlayers()) {
                                    player.playSound(l, this.shotsound, 1.0f, 1.0f);
                                }
                                for (final Player player : cache.getGame().getTeamB().getPlayers()) {
                                    player.playSound(l, this.shotsound, 1.0f, 1.0f);
                                }
                                // 获取动态调整后的accuracy值
                                float adjustedAccuracy = getAdjustedAccuracy(p, this.accuracy);

                                if (adjustedAccuracy == 0.0f) {
                                    cache.setAccuracyYaw(0.0f);
                                    cache.setAccuracyPitch(0.0f);
                                }
                                final float original_yaw = l.getYaw();
                                final float original_pitch = l.getPitch();
                                final double original_x = l.getX();
                                final double original_y = l.getY();
                                final double original_z = l.getZ();
                                for (int b2 = 0; b2 < this.bullets; ++b2) {
                                    if (this.snipe && p.isSneaking()) {
                                        cache.setAccuracyYaw(0.0f);
                                        cache.setAccuracyPitch(0.0f);
                                    }
                                    else if (this.snipe && !p.isSneaking()) {
                                        cache.setAccuracyYaw(MathUtils.randomRange((int)(-this.accuracy), (int)this.accuracy) + 0.5f);
                                        cache.setAccuracyPitch(MathUtils.randomRange((int)(-this.accuracy), (int)this.accuracy) + 0.5f);
                                    }
                                    else if (this.bullets > 1) {
                                        cache.setAccuracyYaw(MathUtils.randomRange((int)(-this.accuracy), (int)this.accuracy) + 0.5f);
                                        cache.setAccuracyPitch(MathUtils.randomRange((int)(-this.accuracy), (int)this.accuracy) + 0.5f);
                                    }
                                    else if (this.accuracy == 0.0f && p.isSprinting()) {
                                        cache.setAccuracyYaw(MathUtils.randomRange(-2, 2) + 0.5f);
                                        cache.setAccuracyPitch(MathUtils.randomRange(-2, 2) + 0.5f);
                                    }
                                    else if (cache.getAccuracyPitch() >= -this.accuracy * this.MaxRoundsPerPitch && !cache.isFirstShot()) {
                                        if (this.RoundsPerYaw == 0) {
                                            cache.setAccuracyPitch(cache.getAccuracyPitch() - this.accuracy);
                                        }
                                        else if (cache.getYawDirection() == 0) {
                                            cache.setYawDirection(1);
                                            cache.setAccuracyYaw(this.accuracy);
                                        }
                                        else if (cache.getAccuracyYaw() / this.accuracy == this.RoundsPerYaw) {
                                            cache.setYawDirection(-1);
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() - this.accuracy);
                                        }
                                        else if (cache.getAccuracyYaw() / this.accuracy == -this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(0.0f);
                                            cache.setYawDirection(1);
                                            cache.setAccuracyPitch(cache.getAccuracyPitch() - this.accuracy);
                                        }
                                        else if (cache.getYawDirection() > 0 && cache.getAccuracyYaw() / this.accuracy < this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() + this.accuracy);
                                        }
                                        else if (cache.getYawDirection() < 0 && cache.getAccuracyYaw() / this.accuracy < this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() - this.accuracy);
                                        }
                                    }
                                    else if (cache.getAccuracyPitch() <= -this.accuracy * this.MaxRoundsPerPitch && p.getInventory().getItemInHand().getAmount() >= 1 && this.RoundsPerYaw == 0) {
                                        if (cache.getYawDirection() == 0) {
                                            cache.setYawDirection(1);
                                            cache.setAccuracyYaw(2.0f);
                                        }
                                        else if (cache.getAccuracyYaw() / 2.0f <= -2.0f) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() + 2.0f);
                                            cache.setYawDirection(1);
                                        }
                                        else if (cache.getAccuracyYaw() / 2.0f >= 2.0f) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() - 2.0f);
                                            cache.setYawDirection(-1);
                                        }
                                        else if (cache.getYawDirection() > 0 && cache.getAccuracyYaw() / 2.0f < 2.0f) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() + 2.0f);
                                        }
                                        else if (cache.getYawDirection() < 0 && cache.getAccuracyYaw() / 2.0f < 2.0f) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() - 2.0f);
                                        }
                                    }
                                    else if (cache.getAccuracyPitch() <= -this.accuracy * this.MaxRoundsPerPitch && p.getInventory().getItemInHand().getAmount() >= 1) {
                                        if (cache.getYawDirection() == 0) {
                                            cache.setYawDirection(1);
                                            cache.setAccuracyYaw(this.accuracy);
                                        }
                                        else if (cache.getAccuracyYaw() / this.accuracy == this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() - this.accuracy);
                                            cache.setYawDirection(-1);
                                        }
                                        else if (cache.getAccuracyYaw() / this.accuracy == -this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() + this.accuracy);
                                            cache.setYawDirection(1);
                                        }
                                        else if (cache.getYawDirection() > 0 && cache.getAccuracyYaw() / this.accuracy < this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() + this.accuracy);
                                        }
                                        else if (cache.getYawDirection() < 0 && cache.getAccuracyYaw() / this.accuracy < this.RoundsPerYaw) {
                                            cache.setAccuracyYaw(cache.getAccuracyYaw() - this.accuracy);
                                        }
                                    }
                                    final double radians_yaw = Math.toRadians(MathUtils.toDegrees(original_yaw) + cache.getAccuracyYaw() + 90.0);
                                    final double radians_pitch = Math.toRadians(original_pitch + cache.getAccuracyPitch() + 90.0f);
                                    final double direction_x = Math.sin(radians_pitch) * Math.cos(radians_yaw);
                                    final double direction_y = Math.cos(radians_pitch);
                                    final double direction_z = Math.sin(radians_pitch) * Math.sin(radians_yaw);
                                    double distance = 0.5;
                                    while (distance < this.distance) {
                                        l.setX(original_x + distance * direction_x);
                                        l.setY(original_y + distance * direction_y);
                                        l.setZ(original_z + distance * direction_z);
                                        if (distance % 1.5 == 0.0) {
                                            p.spawnParticle(Particle.SMALL_FLAME, l, 1, 0.0, 0.0, 0.0, 0.0);
                                        }
                                        if (distance == 0.5) {
                                            p.spawnParticle(Particle.SNOWBALL, l, 1, 0.0, 0.0, 0.0, 0.0);
                                            p.spawnParticle(Particle.SNOWBALL, l, 1, 0.0, 0.0, 0.0, 0.0);
                                        }
                                        final Block block = l.getBlock();
                                        if (block.getType() != Material.WHEAT && this.main.getVersionInterface().hasHitboxAt(block, l.getX(), l.getY(), l.getZ())) {
                                            if (!block.getType().name().contains("GLASS")) {
                                                p.getWorld().playEffect(l, Effect.STEP_SOUND, (Object)block.getType());
                                                break;
                                            }
                                            l.getWorld().playEffect(l, Effect.STEP_SOUND, (Object)block.getType());
                                            cache.getGame().restoreBlocks().add(l.getBlock().getState());
                                            block.setType(Material.AIR);
                                        }
                                        Player hit = null;
                                        for (final Player victim : cache.getGame().getTeamA().getPlayers()) {
                                            if (p != victim && !cache.getGame().getSpectators().contains(victim) && !this.main.getManager().sameTeam(cache.getGame(), p, victim) && (victim.getLocation().add(0.0, 0.2, 0.0).distance(l) <= 0.4 + this.main.hitAddition() || victim.getLocation().add(0.0, 1.0, 0.0).distance(l) <= 0.5 + this.main.hitAddition() || victim.getEyeLocation().distance(l) <= 0.35) && !victim.isDead()) {
                                                hit = victim;
                                                break;
                                            }
                                        }
                                        for (final Player victim : cache.getGame().getTeamB().getPlayers()) {
                                            if (p != victim && !cache.getGame().getSpectators().contains(victim) && !this.main.getManager().sameTeam(cache.getGame(), p, victim) && victim.getLocation().getWorld() == l.getWorld() && (victim.getLocation().add(0.0, 0.2, 0.0).distance(l) <= 0.4 + this.main.hitAddition() || victim.getLocation().add(0.0, 1.0, 0.0).distance(l) <= 0.5 + this.main.hitAddition() || victim.getEyeLocation().distance(l) <= 0.35) && !victim.isDead()) {
                                                hit = victim;
                                                break;
                                            }
                                        }
                                        if (hit != null) {
                                            if (this.main.enableBlood() && this.delayBlood.get(hit.getUniqueId()) == null) {
                                                hit.getWorld().playEffect(l, Effect.STEP_SOUND, (Object)Material.REDSTONE_WIRE);
                                                this.delayBlood.put(hit.getUniqueId(), this.delayshot);
                                            }
                                            if ((!hit.isSneaking() && l.getY() - hit.getLocation().getY() > 1.35 && l.getY() - hit.getLocation().getY() < 1.9) || (l.getY() - hit.getLocation().getY() > 1.27 && l.getY() - hit.getLocation().getY() < 1.82)) {
                                                final double precent = (hit.getInventory().getHelmet().getType() == Material.LEATHER_HELMET) ? 0.5 : 0.25;
                                                final GunDamageEvent e = new GunDamageEvent(this.damage + precent * this.damage, true, p, hit);
                                                this.main.getServer().getPluginManager().callEvent((Event)e);
                                                if (this.main.getManager().damage(cache.getGame(), p, hit, this.damage + precent * this.damage, this.symbol + Messages.PACK_HEADSHOT)) {
                                                    p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.headshotkill", 1.0f, 1.0f);
                                                    cache.getGame().getStats().get(p.getUniqueId()).addHeadshotKill();
                                                }
                                                else {
                                                    p.playSound(p.getLocation(), "cs_random.random.headshot_shooter", 1.0f, 1.0f);
                                                    hit.playSound(p.getLocation(), "cs_random.random.headshot_victim", 1.0f, 1.0f);
                                                }
                                                break;
                                            }
                                            final double precent = (hit.getInventory().getChestplate().getType() == Material.LEATHER_CHESTPLATE) ? 0.0 : 0.15;
                                            final GunDamageEvent e = new GunDamageEvent(this.damage - precent * this.damage, false, p, hit);
                                            this.main.getServer().getPluginManager().callEvent((Event)e);
                                            this.main.getManager().damage(cache.getGame(), p, hit, this.damage - precent * this.damage, this.symbol);
                                            break;
                                        }
                                        else {
                                            distance += 0.25;
                                        }
                                    }
                                    l.setX(original_x);
                                    l.setY(original_y);
                                    l.setZ(direction_z);
                                }
                            }
                            else {
                                if (cache.getTicksLeft() > 0) {
                                    continue;
                                }
                                it4.remove();
                            }
                        }
                        else {
                            it4.remove();
                        }
                    }
                    else {
                        it4.remove();
                    }
                }
                else {
                    it4.remove();
                }
            }
        }
    }
}

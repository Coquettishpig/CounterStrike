package src.counterstrike.Grenades;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import src.counterstrike.Handler.Game;
import src.counterstrike.Handler.GameTeam;
import src.counterstrike.Main;
import src.counterstrike.Utils.Item;
import src.counterstrike.Version.Entity.NMSPsyhicsItem;
import src.counterstrike.Version.MathUtils;

import java.util.*;
import java.util.stream.Stream;

public class Grenade
{
    private Item item;
    private double effect_power;
    private String symbol;
    private String name;
    private int delay;
    private Main main;
    private int duration;
    private GrenadeType type;
    private double throwSpeed;
    private List<GrenadeCache> played;

    // 添加用于管理 TextDisplay 的集合
    private static final Matrix4f textBackgroundTransform = new Matrix4f().translate(0.4F, 0.0F, 0.0F).scale(8.0F, 4.0F, 1.0F);
    private static final List<Matrix4f> transforms = Stream.of(
            new Quaternionf(),                     // 前方
            new Quaternionf().rotateY(1.5707964F), // 右侧（90度）
            new Quaternionf().rotateY(3.1415927F), // 后方（180度）
            new Quaternionf().rotateY(4.712389F), // 左侧（270度）
            new Quaternionf().rotateX(1.5707964F), // 上方
            new Quaternionf().rotateX(-1.5707964F) // 下方
    ).map(q -> new Matrix4f().rotate(q).scale(1.9F, 1.9F, 1.0F).translate(-0.5F, -0.5F, -1.9F / 2.0F).mul(textBackgroundTransform)).toList();
    
    public Grenade(final Main main, final String name, final GrenadeType type, final Item item, final int delay, final int duration, final double throwSpeed, final double effect_power, final String symbol) {
        this.played = new ArrayList<GrenadeCache>();
        this.type = type;
        this.item = item;
        this.name = name;
        this.main = main;
        this.duration = duration;
        this.effect_power = effect_power;
        this.symbol = symbol;
        this.delay = delay;
        this.throwSpeed = throwSpeed;
    }
    
    public void throwGrenade(final Main main, final Game g, final Player p) {
        if (p.getInventory().getHeldItemSlot() == this.type.getSlot()) {
            final NMSPsyhicsItem grenade = main.getVersionInterface().spawnPsyhicsItem(p, p.getItemInHand(), this.throwSpeed);
            this.played.add(new GrenadeCache(g, System.currentTimeMillis(), p, grenade));
            p.getInventory().setItem(this.type.getSlot(), (ItemStack)null);
            for (final Player team : main.getManager().getTeam(g, main.getManager().getTeam(g, p)).getPlayers()) {
                team.playSound(p.getEyeLocation(), this.type.getSound(), 1.0f, 1.0f);
            }
        }
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getSymbol() {
        return this.symbol;
    }
    
    public GrenadeType getGrenadeType() {
        return this.type;
    }
    
    public void tick(final long ticks) {
        final Iterator<GrenadeCache> it = this.played.iterator();
        while (it.hasNext()) {
            final GrenadeCache cache = it.next();
            if ((System.currentTimeMillis() - cache.getTime()) / 1000L >= this.delay) {
                final Location l = cache.getGrenade().getLocation();
                if (this.type == GrenadeType.FRAG) {
                    for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                        p.playSound(l, "cs_throwables.throwables.explodegrenade", 1.0f, 1.0f);
                    }
                    for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.TERRORIST).getPlayers()) {
                        p.playSound(l, "cs_throwables.throwables.explodegrenade", 1.0f, 1.0f);
                    }
                    l.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, l, 15);
                    for (final Player p : cache.getNearbyPlayers(7.0)) {
                        if ((cache.getPlayer() == p || this.main.getManager().getTeam(cache.getGame(), cache.getPlayer()) != this.main.getManager().getTeam(cache.getGame(), p)) && !cache.getGame().getSpectators().contains(p)) {
                            final Location a = p.getEyeLocation().clone();
                            final Vector v = l.toVector().subtract(a.toVector());
                            boolean blocked = false;
                            for (int i = 0; i < Math.round(l.distance(a)) + 1L; ++i) {
                                a.add(v.normalize());
                                if (a.getBlock().getType() != Material.AIR) {
                                    blocked = true;
                                    break;
                                }
                            }
                            if (blocked) {
                                continue;
                            }
                            this.main.getManager().damage(cache.getGame(), cache.getPlayer(), p, this.effect_power - cache.getGrenade().getLocation().distance(p.getLocation()) * 2.0, this.symbol);
                        }
                    }
                }
                if (this.type == GrenadeType.FLASHBANG) {
                    // 播放爆炸音效给所有玩家
                    for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                        p.playSound(l, "cs_throwables.throwables.explodeflashbang", 1.0f, 1.0f);
                    }
                    for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.TERRORIST).getPlayers()) {
                        p.playSound(l, "cs_throwables.throwables.explodeflashbang", 1.0f, 1.0f);
                    }

                    // 处理每个受影响的玩家
                    for (final Player p : cache.getNearbyPlayers(this.effect_power)) {
                        if (!cache.getGame().getSpectators().contains(p) && (cache.getPlayer() == p || this.main.getManager().getTeam(cache.getGame(), cache.getPlayer()) != this.main.getManager().getTeam(cache.getGame(), p))) {
                            final Location eyeLoc = p.getEyeLocation();
                            final Vector toFlashVec = l.toVector().subtract(eyeLoc.toVector());

                            // 如果向量长度为 0，跳过
                            if (toFlashVec.lengthSquared() == 0.0) {
//                                this.main.getLogger().info("[调试] 玩家 " + p.getName() + " 与闪光弹位置重叠，未被闪到");
                                continue;
                            }

                            // 计算玩家视角与闪光方向的夹角
                            Vector directionToExplosion = toFlashVec.clone().normalize();
                            Vector viewDirection = eyeLoc.getDirection().clone().normalize();
                            double dot = viewDirection.dot(directionToExplosion);
                            double angle = Math.toDegrees(Math.acos(Math.min(Math.max(dot, -1.0), 1.0)));
                            double MAX_ANGLE = 90.0;

//                            this.main.getLogger().info("[调试] 玩家 " + p.getName() + " | 视角方向: (" + String.format("%.2f", viewDirection.getX()) + ", " +
//                                    String.format("%.2f", viewDirection.getY()) + ", " + String.format("%.2f", viewDirection.getZ()) + ") | 闪光方向: (" +
//                                    String.format("%.2f", directionToExplosion.getX()) + ", " + String.format("%.2f", directionToExplosion.getY()) + ", " +
//                                    String.format("%.2f", directionToExplosion.getZ()) + ") | 角度: " + String.format("%.2f", angle) + "°");

                            // 如果玩家背对闪光（角度 > 90°），跳过
                            if (angle > MAX_ANGLE) {
//                                this.main.getLogger().info("[调试] 玩家 " + p.getName() + " 背对闪光弹（角度: " + String.format("%.2f", angle) + "°），未被闪到");
                                continue;
                            }

                            // 使用 Bukkit 的射线追踪检测视线阻挡
                            RayTraceResult rayTraceResult = p.getWorld().rayTraceBlocks(eyeLoc, directionToExplosion, toFlashVec.length(), FluidCollisionMode.NEVER, true);
                            boolean blocked = false;
                            Block blockingBlock = null;

                            if (rayTraceResult != null && rayTraceResult.getHitBlock() != null) {
                                blockingBlock = rayTraceResult.getHitBlock();
                                if (blockingBlock.getType() != Material.AIR && !blockingBlock.getType().isTransparent()) {
                                    blocked = true;
                                }
                            }

                            if (blocked) {
//                                this.main.getLogger().info("[调试] 玩家 " + p.getName() + " 视线被阻挡，未被闪到，阻挡方块: " + blockingBlock.getType() +
//                                        " @ (" + blockingBlock.getX() + ", " + blockingBlock.getY() + ", " + blockingBlock.getZ() + ")");
                                continue;
                            }

                            // 计算闪光持续时间
                            double distance = p.getLocation().distance(l);
                            double distanceFactor = 1.0 - distance / this.effect_power;
                            double angleFactor = 1.0 - angle / MAX_ANGLE;
                            double duration = 4.0 * distanceFactor * angleFactor;

                            if (duration <= 0.1) {
//                                this.main.getLogger().info("[调试] 玩家 " + p.getName() + " 闪光时长太短（" + String.format("%.2f", duration) + "秒），未被闪到");
                                continue;
                            }

//                            this.main.getLogger().info("[调试] 玩家 " + p.getName() + " 被闪到！距离: " + String.format("%.2f", distance) + "格，角度: " +
//                                    String.format("%.2f", angle) + "°，闪光时长: " + String.format("%.2f", duration) + "秒");

                            // 创建闪光效果的 TextDisplay
                            final Set<TextDisplay> displays = new HashSet<>();
                            for (int i = 0; i < 6; ++i) {
                                Location displayLoc = eyeLoc.clone();
                                TextDisplay display = (TextDisplay) p.getWorld().spawn(displayLoc, TextDisplay.class);
                                for (Player other : Bukkit.getOnlinePlayers()) {
                                    if (!other.equals(p)) {
                                        other.hideEntity(main, display);
                                    }
                                }
                                display.setText(" ");
                                display.setBackgroundColor(org.bukkit.Color.fromARGB(255, 255, 255, 255));
                                display.setBrightness(new Display.Brightness(15, 15));
                                display.setTeleportDuration(1);
                                display.setTransformationMatrix(transforms.get(i));
                                display.setBillboard(Display.Billboard.FIXED);
                                displays.add(display);
                            }

                            // 计算时间（tick 单位）
                            long totalDuration = (long) (duration * 20);
                            long fadeStart = (long) Math.max(totalDuration - 10, totalDuration / 2);

                            // 应用药水效果
//                            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) totalDuration, 1, false, false, false));

                            // 动态更新显示位置
                            new BukkitRunnable() {
                                long time = totalDuration;

                                @Override
                                public void run() {
                                    if (time <= 0 || p.isDead() || !p.isOnline()) {
                                        for (TextDisplay display : displays) {
                                            display.remove();
                                        }
//                                        main.getLogger().info("[调试] 玩家 " + p.getName() + " 闪光效果结束");
                                        cancel();
                                    } else {
                                        for (TextDisplay display : displays) {
                                            Location displayLoc = p.getEyeLocation().clone();
                                            displayLoc.setYaw(0.0F);
                                            displayLoc.setPitch(0.0F);
                                            display.teleport(displayLoc);
                                        }
                                        time--;
                                    }
                                }
                            }.runTaskTimer(main, 0L, 1L);

                            // 淡出效果
                            new BukkitRunnable() {
                                int alpha = 255;

                                @Override
                                public void run() {
                                    if (alpha <= 0 || p.isDead() || !p.isOnline()) {
                                        for (TextDisplay display : displays) {
                                            display.remove();
                                        }
                                        cancel();
                                    } else {
                                        for (TextDisplay display : displays) {
                                            display.setBackgroundColor(org.bukkit.Color.fromARGB(alpha, 255, 255, 255));
                                        }
                                        alpha -= 25;
                                    }
                                }
                            }.runTaskTimer(main, fadeStart, 1L);
                        }
                    }
                    cache.getGrenade().remove();
                }
                if (this.type == GrenadeType.DECOY) {
                    if (cache.getDuration() == null) {
                        cache.setDuration(System.currentTimeMillis());
                    }
                    if (cache.getDuration() == null) {
                        continue;
                    }
                    if ((System.currentTimeMillis() - cache.getDuration()) / 1000L >= this.duration) {
                        cache.getGrenade().remove();
                        it.remove();
                    }
                    else {
                        if (ticks % 3L != 0L) {
                            continue;
                        }
                        for (final Player p : cache.getNearbyPlayers(20.0)) {
                            p.playSound(cache.getGrenade().getLocation(), "cs.weapons.ak47", 1.0f, 1.0f);
                            p.spawnParticle(Particle.SMOKE_LARGE, cache.getGrenade().getLocation(), 3, 0.1, 0.1, 0.1);
                        }
                    }
                }
                else if (this.type == GrenadeType.SMOKE) {
                    if (cache.getGrenade().isRemoved()) {
                        if ((System.currentTimeMillis() - cache.getDuration()) / 1000L < this.duration) {
                            continue;
                        }
                        for (final Block b : cache.getBlocks()) {
                            b.setType(Material.AIR);
                        }
                        cache.getBlocks().clear();
                        it.remove();
                    } else {
                        for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                            p.playSound(l, "cs_throwables.throwables.explodesmoke", 1.0f, 1.0f);
                        }
                        for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.TERRORIST).getPlayers()) {
                            p.playSound(l, "cs_throwables.throwables.explodesmoke", 1.0f, 1.0f);
                        }
                        for (final Block b : this.getBlocks(cache.getGrenade().getLocation().getBlock(), this.effect_power)) {
                            if (b.getType() == Material.AIR) {
                                cache.getBlocks().add(b);
                                b.setType(Material.TRIPWIRE);
                                BlockData data = b.getBlockData();
                                if (data instanceof org.bukkit.block.data.type.Tripwire tripwire) {
//                                    tripwire.setPowered(true); // 设置 powered 属性为 true
                                    tripwire.setFace(BlockFace.NORTH, true); // 设置 east 方向为连接状态
                                    tripwire.setFace(BlockFace.SOUTH, true); // 设置 east 方向为连接状态
                                    b.setBlockData(tripwire);
                                }
                                b.getDrops().clear();
                            }
                        }
                        cache.setDuration(System.currentTimeMillis());
                        cache.getGrenade().remove();
                    }
                }
                else if (this.type == GrenadeType.FIREBOMB) {
                    if (cache.getGrenade().isRemoved()) {
                        if ((System.currentTimeMillis() - cache.getDuration()) / 1000L >= this.duration) {
                            for (final Block b : cache.getBlocks()) {
                                b.setType(Material.AIR);
                            }
                            cache.getBlocks().clear();
                            it.remove();
                        }
                        else {
                            if (ticks % 15L != 0L) {
                                continue;
                            }
                            for (final Player p : cache.getNearbyToBlockPlayers()) {
                                if ((cache.getPlayer() == p || this.main.getManager().getTeam(cache.getGame(), cache.getPlayer()) != this.main.getManager().getTeam(cache.getGame(), p)) && !cache.getGame().getSpectators().contains(p)) {
                                    this.main.getManager().damage(cache.getGame(), cache.getPlayer(), p, 1.5, this.symbol);
                                    p.setFireTicks(0);
                                }
                            }
                        }
                    }
                    else {
                        for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                            p.playSound(l, "cs_throwables.throwables.explodefirebomb", 1.0f, 1.0f);
                        }
                        for (final Player p : this.main.getManager().getTeam(cache.getGame(), GameTeam.Role.TERRORIST).getPlayers()) {
                            p.playSound(l, "cs_throwables.throwables.explodefirebomb", 1.0f, 1.0f);
                        }
                        for (final Block b : this.getBlocks(cache.getGrenade().getLocation().getBlock(), this.effect_power)) {
                            if (b.getType() == Material.AIR) {
                                final Block below = b.getRelative(BlockFace.DOWN);
                                if (!below.getType().isSolid()) {
                                    continue;
                                }
                                cache.getBlocks().add(b);
                                b.setType(Material.FIRE);
                                b.getDrops().clear();
                            }
                            else {
                                if (!b.getType().isSolid()) {
                                    continue;
                                }
                                final Block relative = b.getRelative(BlockFace.UP);
                                if (relative.getType() != Material.AIR) {
                                    continue;
                                }
                                cache.getBlocks().add(relative);
                                relative.setType(Material.FIRE);
                                relative.getDrops().clear();
                            }
                        }
                        cache.setDuration(System.currentTimeMillis());
                        cache.getGrenade().remove();
                    }
                }
                else {
                    cache.getGrenade().remove();
                    it.remove();
                }
            }
        }
    }
    
    public Item getItem() {
        return this.item;
    }
    
    public void removePlayer(final Player p) {
        final Iterator<GrenadeCache> it = this.played.iterator();
        while (it.hasNext()) {
            final GrenadeCache cache = it.next();
            if (cache.getPlayer() == p) {
                for (final Block b : cache.getBlocks()) {
                    b.setType(Material.AIR);
                }
                cache.getGrenade().remove();
                it.remove();
            }
        }
    }
    
    public void remove(final Game g) {
        final Iterator<GrenadeCache> it = this.played.iterator();
        while (it.hasNext()) {
            final GrenadeCache cache = it.next();
            if (cache.getGame() == g) {
                for (final Block b : cache.getBlocks()) {
                    b.setType(Material.AIR);
                }
                cache.getGrenade().remove();
                it.remove();
            }
        }
    }
    
    private ArrayList<Block> getBlocks(final Block block, final double radius) {
        final ArrayList<Block> blocks = new ArrayList<Block>();
        for (int iR = (int)radius + 1, x = -iR; x <= iR; ++x) {
            for (int z = -iR; z <= iR; ++z) {
                for (int y = -iR; y <= iR; ++y) {
                    final Block curBlock = block.getRelative(x, y, z);
                    if (block.getLocation().toVector().subtract(curBlock.getLocation().toVector()).length() <= radius) {
                        blocks.add(curBlock);
                    }
                }
            }
        }
        return blocks;
    }
}

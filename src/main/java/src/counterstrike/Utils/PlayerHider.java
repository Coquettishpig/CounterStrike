package src.counterstrike.Utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHider implements org.bukkit.event.Listener {
    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private final Table<Integer, Integer, Boolean> observerEntityMap = HashBasedTable.create();
    private final ConcurrentHashMap<Player, Map<Player, PlayerVisibility>> playerVisibilityMap = new ConcurrentHashMap<>();

    private static final PacketType[] PLAYER_PACKETS = {
            PacketType.Play.Server.SPAWN_ENTITY,
            PacketType.Play.Server.ENTITY_VELOCITY,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.ENTITY_LOOK,
            PacketType.Play.Server.ENTITY_MOVE_LOOK,
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.ENTITY_METADATA
    };

    public PlayerHider(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        plugin.getLogger().info("玩家隐藏功能已启动");
        registerPacketListener();
        startVisibilityCheck();
    }

    // 注册数据包监听器
    private void registerPacketListener() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PLAYER_PACKETS) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player observer = event.getPlayer();
                int entityId = event.getPacket().getIntegers().read(0);
                if (!isVisible(observer, entityId) && entityId != observer.getEntityId()) {
                    event.setCancelled(true);
                }
            }
        });
    }

    // 开始周期性检查玩家视线并更新可见性
    private void startVisibilityCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPlayersVisibility();
            }
        }.runTaskTimer(plugin, 0L, 1L); // 每1tick（约0.05秒）检查一次
//        plugin.getLogger().info("开始快速检查玩家可见性，每1tick检测一次");
    }

    // 更新所有玩家的可见性（主线程）
    private void updateAllPlayersVisibility() {
        for (Player observer : Bukkit.getOnlinePlayers()) {
            Map<Player, PlayerVisibility> visibilityMap = playerVisibilityMap.computeIfAbsent(observer, k -> new ConcurrentHashMap<>());
            List<Entity> nearbyEntities = observer.getNearbyEntities(50, 50, 50); // 主线程获取附近实体

            if (observer.getGameMode() == GameMode.SPECTATOR) {
                for (Entity entity : nearbyEntities) {
                    if (!(entity instanceof Player)) continue;
                    Player target = (Player) entity;
                    if (target.equals(observer)) continue;
                    showPlayer(observer, target); // 直接显示，不检查视线或遮挡
                }
                visibilityMap.entrySet().removeIf(entry -> !nearbyEntities.contains(entry.getKey()));
                continue; // 跳过后续的正常逻辑
            }

            // 更新附近玩家的可见性
            for (Entity entity : nearbyEntities) {
                if (!(entity instanceof Player)) continue;
                Player target = (Player) entity;
                if (target.equals(observer)) continue;

                PlayerVisibility visibility = visibilityMap.computeIfAbsent(target, k -> new PlayerVisibility(target));
                visibility.updateLocation(target.getLocation(), target.getEyeLocation());
                visibility.calculateVisibility(observer); // 计算可见性
            }

            // 应用可见性结果
            for (Map.Entry<Player, PlayerVisibility> entry : visibilityMap.entrySet()) {
                Player target = entry.getKey();
                PlayerVisibility visibility = entry.getValue();
                boolean canSee = visibility.isVisible();

                if (canSee) {
                    showPlayer(observer, target);
                } else {
                    hidePlayer(observer, target);
                }
            }

            // 清理已离开范围的玩家
            visibilityMap.entrySet().removeIf(entry -> !nearbyEntities.contains(entry.getKey()));
        }
    }

    // 设置玩家可见性
    private boolean setVisibility(Player observer, int entityId, boolean visible) {
        if (visible) {
            return observerEntityMap.remove(observer.getEntityId(), entityId) != null;
        } else {
            return observerEntityMap.put(observer.getEntityId(), entityId, true) != null;
        }
    }

    // 判断玩家是否可见
    private boolean isVisible(Player observer, int entityId) {
        return !observerEntityMap.contains(observer.getEntityId(), entityId);
    }

    // 显示玩家
    public boolean showPlayer(Player observer, Player target) {
        boolean wasHidden = setVisibility(observer, target.getEntityId(), true);
        if (wasHidden) {
            protocolManager.updateEntity(target, Collections.singletonList(observer));
//            plugin.getLogger().info("玩家 " + target.getName() + " 对 " + observer.getName() + " 可见");
        }
        return wasHidden;
    }

    // 隐藏玩家
    public boolean hidePlayer(Player observer, Player target) {
        boolean wasVisible = setVisibility(observer, target.getEntityId(), false);
        if (wasVisible) {
            PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyEntity.getIntLists().write(0, Collections.singletonList(target.getEntityId()));
            protocolManager.sendServerPacket(observer, destroyEntity);
//            plugin.getLogger().info("玩家 " + target.getName() + " 对 " + observer.getName() + " 隐藏");
        }
        return wasVisible;
    }

    // 玩家可见性数据类
    private static class PlayerVisibility {
        private final Player player;
        private Location location;
        private Location eyeLocation;
        private volatile boolean visible;
        private long lastCheckTime;

        public PlayerVisibility(Player player) {
            this.player = player;
            this.location = player.getLocation();
            this.eyeLocation = player.getEyeLocation();
            this.visible = true;
            this.lastCheckTime = System.currentTimeMillis();
        }

        public void updateLocation(Location location, Location eyeLocation) {
            this.location = location;
            this.eyeLocation = eyeLocation;
        }

        public void calculateVisibility(Player observer) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCheckTime < 50) {
                return;
            }
            this.visible = canSeePlayer(observer, player);
            this.lastCheckTime = currentTime;
        }

        public boolean isVisible() {
            return visible;
        }

        // 检查玩家是否在视线内，包括近距离和多点检测
        private boolean canSeePlayer(Player observer, Player target) {
            Location eyeLoc = observer.getEyeLocation();
            double distance = eyeLoc.distance(target.getEyeLocation());
            double CLOSE_DISTANCE_THRESHOLD = 10.0;

            // 近距离直接可见
            if (distance <= CLOSE_DISTANCE_THRESHOLD) {
                return true;
            }

            Vector direction = eyeLoc.getDirection();
            Vector toTarget = target.getEyeLocation().toVector().subtract(eyeLoc.toVector());
            double angle = direction.angle(toTarget);
            if (angle > Math.toRadians(90)) return false; // 目标在背后

            // 定义多个检测点（头部、身体、脚部）
            Location[] targetPoints = {
                    target.getEyeLocation(),                    // 头部
                    target.getLocation().add(0, 1.0, 0),       // 身体中部
                    target.getLocation().add(0, 0.5, 0)        // 脚部附近
            };

            // 检查每条射线
            for (Location targetPoint : targetPoints) {
                Vector rayDirection = targetPoint.toVector().subtract(eyeLoc.toVector());
                double rayDistance = rayDirection.length();
                Vector ray = rayDirection.normalize();
                Location current = eyeLoc.clone();
                boolean occluded = false;

                // 沿射线逐步检查
                for (double i = 0; i < rayDistance; i += 0.5) {
                    current.add(ray.clone().multiply(0.5));
                    if (current.getBlock().getType().isOccluding()) {
                        occluded = true;
                        break;
                    }
                }

                // 如果这条射线没被遮挡，则认为可见
                if (!occluded) {
                    return true;
                }
            }

            // 所有射线都被遮挡，才返回不可见
            return false;
        }
    }
}
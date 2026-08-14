package src.counterstrike.Version.v1_21_R1;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import src.counterstrike.Version.Entity.NMSPsyhicsItem;
import src.counterstrike.Version.PsyhicsItem;
import src.counterstrike.Version.VersionInterface;

import java.util.ArrayList;
import java.util.List;

public class v1_21_R1 implements VersionInterface {

    @Override
    public void hideNameTag(final Team team) {
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    public void sendFakeItem(Player player, int slot, ItemStack itemStack) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

        // 设置目标玩家的实体ID（即让谁看起来穿了这件装备）
        packet.getIntegers().write(0, player.getEntityId());

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = new ArrayList<>();

        EnumWrappers.ItemSlot targetSlot = switch (slot) {
            case 0 -> EnumWrappers.ItemSlot.HEAD;
            case 1 -> EnumWrappers.ItemSlot.CHEST;
            case 2 -> EnumWrappers.ItemSlot.LEGS;
            case 3 -> EnumWrappers.ItemSlot.FEET;
            case 4 -> EnumWrappers.ItemSlot.MAINHAND;
            case 5 -> EnumWrappers.ItemSlot.OFFHAND;
            default -> EnumWrappers.ItemSlot.MAINHAND;
        };

        pairs.add(new Pair<>(targetSlot, itemStack));

        packet.getSlotStackPairLists().write(0, pairs);

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setHandSpeed(final Player player, final double speed) {
        var attributeInstance = player.getAttribute(Attribute.ATTACK_SPEED);

        if (attributeInstance != null) {
            attributeInstance.setBaseValue(speed);
        }
    }

    @Override
    public void sendInvisibility(final Scoreboard scoreboard, final List<Player> team, final List<Player> spectators) {
        for (Player player : team) {
            if (!spectators.contains(player)) {
                Team playerTeam = scoreboard.getEntryTeam(player.getName());
                if (playerTeam != null) {
                    playerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                }
            }
        }
    }

    @Override
    public void setFireworkExplode(final Firework firework) {
        // 1.21.1 依然使用 detonate 让烟花立即爆炸
        firework.detonate();
    }

    @Override
    public void sendTitle(final Player player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
        // 虽然有新的 Title API (Adventure), 但 player.sendTitle 依然是 1.21.1 的标准捷径
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public NMSPsyhicsItem spawnPsyhicsItem(final Player player, final ItemStack itemStack, final double throwSpeedMultiplier) {
        return new PsyhicsItem(player, itemStack, throwSpeedMultiplier);
    }

    @Override
    public double getHandSpeed(final Player player) {
        var attributeInstance = player.getAttribute(Attribute.ATTACK_SPEED);
        return attributeInstance != null ? attributeInstance.getBaseValue() : 4.0; // 4.0 是默认攻击速度
    }

    @Override
    public boolean hasHitboxAt(final Block block, final double x, final double y, final double z) {
        // 1.21.1 完美支持此方法，用于精确碰撞检测
        return block.getCollisionShape().getBoundingBoxes().stream()
                .anyMatch(bbox -> bbox.contains(x, y, z));
        // 注意：bbox.contains 如果传入的是世界绝对坐标，则不需要减去 block.getX()
        // 根据 BoundingBox.contains(double x, double y, double z) 的定义调整
    }
}
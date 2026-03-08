package src.counterstrike.Version.v1_20_R3;

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
import java.util.Objects;

public class v1_20_R3 implements VersionInterface {

    @Override
    public void hideNameTag(final Team team) {
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    public void sendFakeItem(Player player, int slot, ItemStack itemStack) {
        // 创建设备数据包
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);

        // 设置玩家实体ID
        packet.getIntegers().write(0, player.getEntityId());

        // 创建一个用于存储装备槽位和物品的列表
        ArrayList<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = new ArrayList<>();

        // 根据槽位添加对应的装备
        if (slot == 0) { // 头盔槽位
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, itemStack));
        } else if (slot == 1) { // 胸甲槽位
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, itemStack));
        } else if (slot == 2) { // 腿甲槽位
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, itemStack));
        } else if (slot == 3) { // 鞋子槽位
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.FEET, itemStack));
        } else if (slot == 4) { // 主手槽位
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, itemStack));
        } else if (slot == 5) { // 副手槽位
            pairs.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND, itemStack));
        }

        // 将槽位和物品的列表写入数据包
        packet.getSlotStackPairLists().writeSafely(0, pairs);

        // 发送数据包给玩家
        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setHandSpeed(final Player player, final double speed) {
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(speed);
    }

    @Override
    public void sendInvisibility(final Scoreboard scoreboard, final List<Player> team, final List<Player> spectators) {
        for (Player player : team) {
            if (!spectators.contains(player)) {
                Team playerTeam = scoreboard.getEntryTeam(player.getName());
                if (playerTeam != null) {
                    playerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER); // 隐藏名牌
                }
            }
        }
    }

    @Override
    public void setFireworkExplode(final Firework firework) {
        firework.detonate();
    }

    @Override
    public void sendTitle(final Player player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    @Override
    public NMSPsyhicsItem spawnPsyhicsItem(final Player player, final ItemStack itemStack, final double throwSpeedMultiplier) {
        // 假设 PsyhicsItem 是一个自定义类，你可以在这里实例化它
        return new PsyhicsItem(player, itemStack, throwSpeedMultiplier);
    }

    @Override
    public double getHandSpeed(final Player player) {
        return player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).getBaseValue();
    }

    @Override
    public boolean hasHitboxAt(final Block block, final double x, final double y, final double z) {
        // 使用 Bukkit API 检查方块是否有碰撞箱
        return block.getCollisionShape().getBoundingBoxes().stream()
                .anyMatch(bbox -> bbox.contains(x - block.getX(), y - block.getY(), z - block.getZ()));
    }
}
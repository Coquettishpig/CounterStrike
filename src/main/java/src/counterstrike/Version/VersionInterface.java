package src.counterstrike.Version;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import src.counterstrike.Version.Entity.NMSPsyhicsItem;

import java.util.List;

public interface VersionInterface {
    void hideNameTag(final Team team);

    void sendFakeItem(final Player player, final int slot, final ItemStack itemStack);

    default void sendActionBar(final Player player, final String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    void setHandSpeed(final Player player, final double speed);

    void sendInvisibility(final Scoreboard scoreboard, final List<Player> team, final List<Player> spectators);

    void setFireworkExplode(final Firework firework);

    void sendTitle(final Player player, final int fadeIn, final int stay, final int fadeOut, final String title, final String subtitle);

    NMSPsyhicsItem spawnPsyhicsItem(final Player player, final ItemStack itemStack, final double throwSpeedMultiplier);

    double getHandSpeed(final Player player);

    boolean hasHitboxAt(final Block block, final double x, final double y, final double z);
}
package src.counterstrike;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import src.counterstrike.Grenades.Grenade;
import src.counterstrike.Guns.Gun;
import src.counterstrike.Handler.Game;
import src.counterstrike.Handler.GameState;
import src.counterstrike.ScoreBoard.ScoreBoard;
import src.counterstrike.Version.MathUtils;

import java.util.*;

public class UpdateTask extends BukkitRunnable
{
    private Main main;
    private long ticks;
    private List<FireworkEffect> effects;
    private HashMap<UUID, Integer> delay;
    
    public UpdateTask(final Main main) {
        this.ticks = 0L;
        this.delay = new HashMap<>();
        this.main = main;
        this.effects = new ArrayList<>();
        final FireworkEffect.Builder b = FireworkEffect.builder().trail(false).flicker(false);
        b.withColor(Color.RED, Color.YELLOW, Color.GREEN, Color.BLUE);
        this.effects.add(b.with(FireworkEffect.Type.BALL).build());
        this.effects.add(b.with(FireworkEffect.Type.BALL_LARGE).build());
        this.effects.add(b.with(FireworkEffect.Type.BURST).build());
        this.runTaskTimer((Plugin)main, 0L, 1L);
    }
    
    public HashMap<UUID, Integer> getDelay() {
        return this.delay;
    }
    
    public void run() {
        final Iterator<Map.Entry<UUID, Integer>> it = this.delay.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<UUID, Integer> entry = it.next();
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
            else {
                it.remove();
            }
        }
        for (final Gun g : this.main.getGuns()) {
            g.tick();
        }
        for (final Grenade grenade : this.main.getGrenades()) {
            grenade.tick(this.ticks);
        }
        for (final Game g2 : this.main.getManager().getGames()) {
            if (g2.getState() == GameState.END && this.ticks % 10L == 0L && g2.getTimer() >= 2) {
                final Location l = g2.getFireworks().get(MathUtils.random().nextInt(g2.getFireworks().size()));
                final double x = l.getX();
                final double y = l.getY();
                final double z = l.getZ();
                for (int i = 0; i < 4; ++i) {
                    l.setX(x + MathUtils.randomRange(-20, 20));
                    l.setY(y + MathUtils.randomRange(-5, 5));
                    l.setZ(z + MathUtils.randomRange(-20, 20));
                    final Firework f = (Firework)l.getWorld().spawnEntity(l, EntityType.FIREWORK);
                    final FireworkMeta fm = f.getFireworkMeta();
                    fm.addEffect((FireworkEffect)this.effects.get(MathUtils.random().nextInt(this.effects.size())));
                    f.setFireworkMeta(fm);
                    f.detonate();
                    l.setX(x);
                    l.setY(y);
                    l.setZ(z);
                }
            }
            if (this.ticks % 20L == 0L) {
                if (g2.getTimer() > 0) {
                    for (final ScoreBoard board : g2.getStatus().values()) {
                        this.main.getManager().updateStatus(g2, board.getStatus());
                    }
                }
                try {
                    g2.run();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    this.main.getManager().stopGame(g2, false);
                }
                if (g2.getState() != GameState.WAITING || g2.getTeamA().size() + g2.getTeamB().size() >= g2.getMinPlayers()) {
                    continue;
                }
                for (final Player p : g2.getTeamA().getPlayers()) {
                    this.main.getVersionInterface().sendActionBar(p, Messages.BAR_PLAYERS.toString().replace("%min%", g2.getMinPlayers() + ""));
                }
                for (final Player p : g2.getTeamB().getPlayers()) {
                    this.main.getVersionInterface().sendActionBar(p, Messages.BAR_PLAYERS.toString().replace("%min%", g2.getMinPlayers() + ""));
                }
            }
        }
        ++this.ticks;
    }
}

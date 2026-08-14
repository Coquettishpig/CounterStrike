package src.counterstrike.ScoreBoard;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import src.counterstrike.Cache.PlayerStatus;
import src.counterstrike.Handler.Game;
import src.counterstrike.Handler.GameTeam;
import src.counterstrike.Main;
import src.counterstrike.Messages;

import java.util.ArrayList;
import java.util.List;

public class ScoreBoardTeam {
    private Main main;
    private Scoreboard board;
    private List<Team> teams;

    public ScoreBoardTeam(final Main main, final Game g, final Scoreboard board) {
        this.teams = new ArrayList<Team>();
        this.main = main;
        this.board = board;
        for (final Player p : g.getMain().getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
            setupTeam(g, p);
        }
        for (final Player p : g.getMain().getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
            setupTeam(g, p);
        }
    }

    // 抽离出的公共方法，方便统一处理 PAPI
    private void setupTeam(Game g, Player p) {
        final Team t = board.registerNewTeam(p.getName());
        applyTags(g, p, t);
        t.addEntry(p.getName());
        this.teams.add(t);
    }

    private void applyTags(Game g, Player p, Team t) {
        final PlayerStatus stats = g.getStats().get(p.getUniqueId());
        final boolean s = g.getSpectators().contains(p);

        String prefix = "§8[" + Messages.PACK_CRIMS + "§8] " + (s ? "§7§o" : "§4");
        if (g.getMain().getManager().getTeam(g, p) == GameTeam.Role.COUNTERTERRORIST) {
            prefix = "§8[" + Messages.PACK_COPS + "§8] " + (s ? "§7§o" : "§3");
        }

        String suffix = " §8[§e" + stats.getKills() + "-" + stats.getDeaths() + "§8]";

        // 修改：解析 PAPI
        if (this.main.placeholderSupport()) {
            prefix = PlaceholderAPI.setPlaceholders(p, prefix);
            suffix = PlaceholderAPI.setPlaceholders(p, suffix);
        }

        t.setPrefix(prefix);
        t.setSuffix(suffix);
        main.getVersionInterface().hideNameTag(t);
    }

    public void add(final Game g, final Player p) {
        final Team t = this.board.registerNewTeam(p.getName());
        applyTags(g, p, t);
        t.addEntry(p.getName());
        this.teams.add(t);
    }

    public void remove(final Game g, final Player p) {
        final Team t = this.board.getTeam(p.getName());
        if (t != null) {
            this.teams.remove(t);
            t.unregister();
        }
    }

    public List<Team> getTeams() {
        return this.teams;
    }

    public void update(final Game g, final Player p) {
        final Team t = this.board.getTeam(p.getName());
        if (t != null) {
            applyTags(g, p, t);
        }
    }
}
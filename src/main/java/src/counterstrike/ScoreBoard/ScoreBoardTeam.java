package src.counterstrike.ScoreBoard;

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

public class ScoreBoardTeam
{
    private Main main;
    private Scoreboard board;
    private List<Team> teams;
    
    public ScoreBoardTeam(final Main main, final Game g, final Scoreboard board) {
        this.teams = new ArrayList<Team>();
        this.main = main;
        this.board = board;
        for (final Player p : g.getMain().getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
            final Team t = board.registerNewTeam(p.getName());
            final PlayerStatus stats = g.getStats().get(p.getUniqueId());
            final boolean s = g.getSpectators().contains(p);
            t.setPrefix("§8[" + Messages.PACK_CRIMS + "§8] " + (s ? "§7§o" : "§4"));
            t.setSuffix(" §8[§e" + stats.getKills() + "-" + stats.getDeaths() + "§8]");
            main.getVersionInterface().hideNameTag(t);
            t.addEntry(p.getName());
            this.teams.add(t);
        }
        for (final Player p : g.getMain().getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
            final Team t = board.registerNewTeam(p.getName());
            final PlayerStatus stats = g.getStats().get(p.getUniqueId());
            final boolean s = g.getSpectators().contains(p);
            t.setPrefix("§8[" + Messages.PACK_COPS + "§8] " + (s ? "§7§o" : "§3"));
            t.setSuffix(" §8[§e" + stats.getKills() + "-" + stats.getDeaths() + "§8]");
            main.getVersionInterface().hideNameTag(t);
            t.addEntry(p.getName());
            this.teams.add(t);
        }
    }

    public void add(final Game g, final Player p) {
        final Team t = this.board.registerNewTeam(p.getName());
        final PlayerStatus stats = g.getStats().get(p.getUniqueId());
        final boolean s = g.getSpectators().contains(p);
        t.setPrefix((g.getMain().getManager().getTeam(g, p) == GameTeam.Role.TERRORIST) ? ("§8[" + Messages.PACK_CRIMS + "§8] " + (s ? "§7§o" : "§4")) : ("§8[" + Messages.PACK_COPS + "§8] " + (s ? "§7§o" : "§3")));
        t.setSuffix(" §8[§e" + stats.getKills() + "-" + stats.getDeaths() + "§8]");
        this.main.getVersionInterface().hideNameTag(t);
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
        final boolean s = g.getSpectators().contains(p);
        final PlayerStatus stats = g.getStats().get(p.getUniqueId());
        t.setPrefix((g.getMain().getManager().getTeam(g, p) == GameTeam.Role.TERRORIST) ? ("§8[" + Messages.PACK_CRIMS + "§8] " + (s ? "§7§o" : "§4")) : ("§8[" + Messages.PACK_COPS + "§8] " + (s ? "§7§o" : "§3")));
        t.setSuffix(" §8[§e" + stats.getKills() + "-" + stats.getDeaths() + "§8]");
        this.main.getVersionInterface().hideNameTag(t);
    }
}

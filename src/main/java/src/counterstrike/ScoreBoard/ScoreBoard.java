package src.counterstrike.ScoreBoard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import src.counterstrike.Handler.Game;
import src.counterstrike.Main;

public class ScoreBoard
{
    private Main main;
    private Scoreboard board;
    private ScoreBoardTeam team;
    private ScoreBoardHealth health;
    private ScoreBoardStatus status;
    
    public ScoreBoard(final Main main, final Game g, final Player p) {
        this.main = main;
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.status = new ScoreBoardStatus(main, p, this);
        p.setScoreboard(this.board);
    }
    
    public Scoreboard getScoreboard() {
        return this.board;
    }
    
    public ScoreBoardTeam getTeams() {
        return this.team;
    }
    
    public ScoreBoardStatus getStatus() {
        return this.status;
    }
    
    public ScoreBoardHealth getHealth() {
        return this.health;
    }
    
    public void showTeams(final Game g) {
        this.team = new ScoreBoardTeam(this.main, g, this.board);
        this.main.getVersionInterface().sendInvisibility(this.board, g.getTeamA().getPlayers(), g.getSpectators());
        this.main.getVersionInterface().sendInvisibility(this.board, g.getTeamB().getPlayers(), g.getSpectators());
    }
    
    public void showHealth(final Game g) {
        this.health = new ScoreBoardHealth(g, this);
    }
    
    public void removeTeam() {
        if (this.team != null) {
            for (final Team t : this.team.getTeams()) {
                t.unregister();
            }
            this.team.getTeams().clear();
            this.team = null;
        }
    }
    
    public void removeHealth() {
        if (this.health != null) {
            for (final Score score : this.health.getScores()) {
                this.board.resetScores(score.getEntry());
            }
            this.health.getObjective().unregister();
            this.health = null;
        }
    }
    
    public void remove() {
        this.removeTeam();
        this.removeHealth();
        for (final Objective o : this.board.getObjectives()) {
            o.unregister();
        }
    }
}

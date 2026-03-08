package src.counterstrike.ScoreBoard;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import src.counterstrike.Handler.Game;

import java.util.ArrayList;
import java.util.List;

public class ScoreBoardHealth
{
    private Objective obj;
    private List<Score> scores;
    
    public ScoreBoardHealth(final Game g, final ScoreBoard board) {
        this.scores = new ArrayList<>();
        (this.obj = board.getScoreboard().registerNewObjective("health", "dummy")).setDisplaySlot(DisplaySlot.BELOW_NAME);
        this.obj.setDisplayName("銀");
        for (final Player p : g.getTeamA().getPlayers()) {
            final Score score = this.obj.getScore(p.getName());
            score.setScore((int)(p.getHealth() / p.getMaxHealth() * 100.0));
            this.scores.add(score);
        }
        for (final Player p : g.getTeamB().getPlayers()) {
            final Score score = this.obj.getScore(p.getName());
            score.setScore((int)(p.getHealth() / p.getMaxHealth() * 100.0));
            this.scores.add(score);
        }
    }
    
    public List<Score> getScores() {
        return this.scores;
    }
    
    public Objective getObjective() {
        return this.obj;
    }
    
    public void update(final Player p) {
        this.obj.getScore(p.getName()).setScore((int)(p.getHealth() / p.getMaxHealth() * 100.0));
    }
}

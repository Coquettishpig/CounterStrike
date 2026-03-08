package src.counterstrike.ScoreBoard;

import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;
import src.counterstrike.Main;

public class ScoreBoardLine
{
    private Main main;
    private Team team;
    private Score score;
    private String name;
    
    public ScoreBoardLine(final Main main, final ScoreBoard board, final String name, final int line) {
        final String color = ChatColor.values()[line - 1] + "§r";
        this.team = board.getScoreboard().registerNewTeam(color);
        (this.score = board.getStatus().getObjective().getScore(color)).setScore(line);
        this.team.addEntry(color);
        this.main = main;
        this.update(name);
    }
    
    public void unregister() {
        if (this.team != null) {
            this.team.unregister();
            this.team = null;
        }
    }
    
    public Score getScore() {
        return this.score;
    }
    
    public void update(final String name) {
        if (this.main.placeholderSupport()) {}
        if (!name.equals(this.name)) {
            this.name = name;
            String prefix = (name.length() >= 16) ? name.substring(0, 16) : name;
            boolean colorMark = false;
            if (prefix.length() > 0 && prefix.charAt(prefix.length() - 1) == '§') {
                prefix = prefix.substring(0, prefix.length() - 1);
                colorMark = true;
            }
            this.team.setPrefix(prefix);
            if (name.length() > 16) {
                String suffix = colorMark ? "" : ChatColor.getLastColors(prefix);
                suffix += name.substring(prefix.length(), name.length());
                if (suffix.length() <= 16) {
                    this.team.setSuffix(suffix);
                }
                else {
                    this.team.setSuffix(suffix.substring(0, 16));
                }
            }
            else {
                this.team.setSuffix("");
            }
        }
    }
}

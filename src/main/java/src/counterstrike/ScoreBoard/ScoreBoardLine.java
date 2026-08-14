package src.counterstrike.ScoreBoard;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;
import src.counterstrike.Main;

public class ScoreBoardLine {
    private Main main;
    private Team team;
    private Score score;
    private String name;
    private Player player; // 新增：保存玩家引用

    public ScoreBoardLine(final Main main, final ScoreBoard board, final Player player, final String name, final int line) {
        this.main = main;
        this.player = player; // 初始化玩家
        final String color = ChatColor.values()[line - 1] + "§r";
        this.team = board.getScoreboard().registerNewTeam(color);
        (this.score = board.getStatus().getObjective().getScore(color)).setScore(line);
        this.team.addEntry(color);
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

    public void update(String name) {
        // 修改：在处理长度前先解析 PAPI 变量
        if (this.main.placeholderSupport()) {
            name = PlaceholderAPI.setPlaceholders(this.player, name);
        }

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
                } else {
                    this.team.setSuffix(suffix.substring(0, 16));
                }
            } else {
                this.team.setSuffix("");
            }
        }
    }
}
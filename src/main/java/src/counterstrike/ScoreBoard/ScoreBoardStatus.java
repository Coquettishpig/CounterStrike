package src.counterstrike.ScoreBoard;

import me.clip.placeholderapi.PlaceholderAPI; // 确保导入了 PAPI
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import src.counterstrike.Main;

import java.util.HashMap;

public class ScoreBoardStatus {
    private Main main;
    private Player p;
    private Objective obj;
    private ScoreBoard board;
    private HashMap<Integer, ScoreBoardLine> entries;

    public ScoreBoardStatus(final Main main, final Player p, final ScoreBoard board) {
        this.entries = new HashMap<Integer, ScoreBoardLine>();
        this.p = p;
        this.main = main;
        this.board = board;
        (this.obj = board.getScoreboard().registerNewObjective("status", "dummy")).setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public Player getPlayer() {
        return this.p;
    }

    public Objective getObjective() {
        return this.obj;
    }

    // 修改点：在此处增加 PAPI 解析
    public void setTitle(String title) {
        if (this.main.placeholderSupport()) {
            title = PlaceholderAPI.setPlaceholders(this.p, title);
        }

        // 计分板 Title 长度限制为 32 个字符
        if (title.length() > 32) {
            title = title.substring(0, 32);
        }

        this.obj.setDisplayName(title);
    }

    public void reset() {
        for (final ScoreBoardLine key : this.entries.values()) {
            key.unregister();
            this.board.getScoreboard().resetScores(key.getScore().getEntry());
        }
        this.entries.clear();
    }

    public void updateLine(final int line, final String name) {
        if (this.entries.get(line) != null) {
            this.entries.get(line).update(name);
        } else {
            this.entries.put(line, new ScoreBoardLine(this.main, this.board, this.p, name, line));
        }
    }
}
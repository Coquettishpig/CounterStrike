package src.counterstrike.Hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import src.counterstrike.Handler.Game;
import src.counterstrike.Handler.GameManager;
import src.counterstrike.Handler.GameState;
import src.counterstrike.Handler.GameTeam;
import src.counterstrike.Main;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private Main main;
    private GameManager gameManager;

    public PlaceholderAPIHook(final Main main) {
        this.main = main;
        this.gameManager = main.getManager();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getAuthor() {
        return "Coquettishpigs";
    }

    @Override
    public String getIdentifier() {
        return "cs";
    }

    @Override
    public String getVersion() {
        return this.main.getDescription().getVersion();
    }

    @Override
    public String onRequest(final OfflinePlayer p, final String identifier) {
        if (p == null || !p.isOnline()) {
            return null;
        }

        Player player = p.getPlayer();
        if (player == null) {
            return null;
        }

        Game game = gameManager.getGame(player);
        if (game == null) {
            // 玩家不在游戏中，返回 null
            return null;
        }

        // 获取玩家所在的阵营
        GameTeam.Role playerRole = null;
        if (game.getTeamA().getPlayers().contains(player)) {
            playerRole = game.getTeamA().getRole();
        } else if (game.getTeamB().getPlayers().contains(player)) {
            playerRole = game.getTeamB().getRole();
        }

        if (playerRole == null) {
            // 玩家不在任何阵营中，返回 null
            return null;
        }

        // 玩家在游戏内，根据标识符返回相应数据
        return switch (identifier.toLowerCase()) {
            case "kills" -> String.valueOf(game.getStats().get(player.getUniqueId()).getKills());
            case "deaths" -> String.valueOf(game.getStats().get(player.getUniqueId()).getDeaths());
            case "round_kills" -> String.valueOf(game.getStats().get(player.getUniqueId()).getRoundKills());
            case "bomb_planted" -> String.valueOf(game.getStats().get(player.getUniqueId()).getBombPlanted());
            case "headshot_kills" -> String.valueOf(game.getStats().get(player.getUniqueId()).getHeadshotKill());
            case "team_alive" -> String.valueOf(gameManager.getAlivePlayers(game, playerRole == GameTeam.Role.TERRORIST ? game.getTeamA() : game.getTeamB()));
            case "enemy_alive" -> String.valueOf(gameManager.getAlivePlayers(game, playerRole == GameTeam.Role.TERRORIST ? game.getTeamB() : game.getTeamA()));
            case "terrorist_alive" -> String.valueOf(gameManager.getAlivePlayers(game, gameManager.getTeam(game, GameTeam.Role.TERRORIST)));
            case "counterterrorist_alive" -> String.valueOf(gameManager.getAlivePlayers(game, gameManager.getTeam(game, GameTeam.Role.COUNTERTERRORIST)));
            case "game_status" -> game.getState().getState();
            case "team_name" -> playerRole.toString();
            case "current_round" -> String.valueOf(game.getRound() + 1); // 返回当前回合数
            case "round_timer" -> getRoundTimer(game); // 返回当前回合的倒计时或炸弹倒计时
            case "money" -> String.valueOf(game.getMoney(player)); // 返回玩家的金钱
            case "team_score" -> String.valueOf(playerRole == GameTeam.Role.COUNTERTERRORIST ? game.getScoreTeamA() : game.getScoreTeamB());
            case "terrorist_score" -> getTeamScore(game, GameTeam.Role.TERRORIST);
            case "counterterrorist_score" -> getTeamScore(game, GameTeam.Role.COUNTERTERRORIST);
            default -> null;
        };
    }

    // 获取指定阵营的得分
    private String getTeamScore(Game game, GameTeam.Role role) {
        if (game.getTeamA().getRole() == role) {
            return String.valueOf(game.getScoreTeamA());
        } else if (game.getTeamB().getRole() == role) {
            return String.valueOf(game.getScoreTeamB());
        }
        return "0"; // 默认返回0，防止异常
    }

    // 获取当前回合的倒计时或炸弹倒计时
    private String getRoundTimer(Game game) {
        if (game.getState() == GameState.IN_GAME && !game.isRoundEnding()) {
            int seconds = game.getTimer(); // 获取当前回合的倒计时
            if (game.getBomb().isPlanted()) {
                seconds = game.getBomb().getTimer(); // 如果炸弹已放置，获取炸弹的倒计时
                return "<red>" + formatTimer(seconds); // 炸弹倒计时显示为红色
            }
            return formatTimer(seconds);
        } else {
            return "00:00"; // 如果不在游戏中或回合结束，返回 00:00
        }
    }

    // 格式化时间（秒）为 MM:SS 格式
    private String formatTimer(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%02d:%02d", minutes, remainingSeconds);
    }
}
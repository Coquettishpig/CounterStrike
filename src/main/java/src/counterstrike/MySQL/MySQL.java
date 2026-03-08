package src.counterstrike.MySQL;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import src.counterstrike.Cache.PlayerStatus;
import src.counterstrike.Main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MySQL extends BukkitRunnable {
    private int amountQueue;
    private Connection connection;
    private List<PlayerStatus> status;
    private Main main;

    public MySQL(final Main main, final String host, final String database, final String username, final String password, final int port, final int amountQueue) {
        this.main = main;
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true", username, password);
            this.amountQueue = amountQueue;
            this.status = new ArrayList<>();
            final Statement statement = this.connection.createStatement();
            this.runTaskTimerAsynchronously(main, 0L, 20L);
            // 创建表，包含 OWNED_MUSIC 和 EQUIPPED_MUSIC 字段
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS CounterStrike (id INTEGER NOT NULL AUTO_INCREMENT, UUID VARCHAR(36) UNIQUE, NAME VARCHAR(16), KILLS INTEGER, DEATHS INTEGER, HEADSHOTS INTEGER, BOMBPLANTED INTEGER, OWNED_MUSIC VARCHAR(255), EQUIPPED_MUSIC VARCHAR(255), PRIMARY KEY (id))");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<PlayerStatus> getCache() {
        return this.status;
    }

    public Connection getConnection() {
        return this.connection;
    }

    public void addInQueue(final PlayerStatus status) {
        if (!this.status.contains(status)) {
            this.status.add(status);
        }
    }

    public void closeConnection() {
        try {
            final Statement statement = this.connection.createStatement();
            for (final PlayerStatus s : this.status) {
                statement.executeUpdate("INSERT INTO CounterStrike (UUID, NAME, KILLS, DEATHS, HEADSHOTS, BOMBPLANTED, OWNED_MUSIC, EQUIPPED_MUSIC) VALUES ('" + s.getUUID() + "', '" + s.getName() + "', " + s.getKills() + ", " + s.getDeaths() + ", " + s.getHeadshotKill() + ", " + s.getBombPlanted() + ", NULL, NULL) ON DUPLICATE KEY UPDATE NAME='" + s.getName() + "', KILLS=KILLS+" + s.getKills() + ", DEATHS=DEATHS+" + s.getDeaths() + ", HEADSHOTS=HEADSHOTS+" + s.getHeadshotKill() + ", BOMBPLANTED=BOMBPLANTED+" + s.getBombPlanted() + ";");
            }
            this.status.clear();
            statement.close();
            this.connection.close();
            this.cancel();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        if (this.status.size() >= this.amountQueue) {
            try {
                final Statement statement = this.connection.createStatement();
                for (final PlayerStatus s : this.status) {
                    statement.executeUpdate("INSERT INTO CounterStrike (UUID, NAME, KILLS, DEATHS, HEADSHOTS, BOMBPLANTED, OWNED_MUSIC, EQUIPPED_MUSIC) VALUES ('" + s.getUUID() + "', '" + s.getName() + "', " + s.getKills() + ", " + s.getDeaths() + ", " + s.getHeadshotKill() + ", " + s.getBombPlanted() + ", NULL, NULL) ON DUPLICATE KEY UPDATE NAME='" + s.getName() + "', KILLS=KILLS+" + s.getKills() + ", DEATHS=DEATHS+" + s.getDeaths() + ", HEADSHOTS=HEADSHOTS+" + s.getHeadshotKill() + ", BOMBPLANTED=BOMBPLANTED+" + s.getBombPlanted() + ";");
                }
                statement.close();
                this.status.clear();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 设置玩家装备的音乐
    public void setEquippedMusic(UUID uuid, String musicPath) {
        try {
            String query = "INSERT INTO CounterStrike (UUID, NAME, KILLS, DEATHS, HEADSHOTS, BOMBPLANTED, EQUIPPED_MUSIC) VALUES (?, ?, 0, 0, 0, 0, ?) ON DUPLICATE KEY UPDATE EQUIPPED_MUSIC = ?";
            PreparedStatement stmt = this.connection.prepareStatement(query);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getName() : "Unknown");
            stmt.setString(3, musicPath);
            stmt.setString(4, musicPath);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 获取玩家装备的音乐 (MySQL method with default value parameter)
    public String getEquippedMusic(UUID uuid, String defaultMusic) {
        try {
            String query = "SELECT EQUIPPED_MUSIC FROM CounterStrike WHERE UUID = ?";
            PreparedStatement stmt = this.connection.prepareStatement(query);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String music = rs.getString("EQUIPPED_MUSIC");
                rs.close();
                stmt.close();
                return music != null ? music : defaultMusic; // Use provided default music
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return defaultMusic; // 未找到时返回提供的默认音乐
    }

    // 设置玩家拥有的音乐（追加到列表）
    public void addMusicOwned(UUID uuid, String musicName) {
        try {
            // 先获取当前拥有的音乐
            String currentOwned = getOwnedMusic(uuid);
            List<String> ownedList = currentOwned.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(currentOwned.split(",")));
            if (!ownedList.contains(musicName)) {
                ownedList.add(musicName);
                String newOwned = String.join(",", ownedList);

                String query = "INSERT INTO CounterStrike (UUID, NAME, OWNED_MUSIC) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE OWNED_MUSIC = ?";
                PreparedStatement stmt = this.connection.prepareStatement(query);
                stmt.setString(1, uuid.toString());
                stmt.setString(2, Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getName() : "Unknown");
                stmt.setString(3, newOwned);
                stmt.setString(4, newOwned);
                stmt.executeUpdate();
                stmt.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 获取玩家拥有的音乐列表
    public String getOwnedMusic(UUID uuid) {
        try {
            String query = "SELECT OWNED_MUSIC FROM CounterStrike WHERE UUID = ?";
            PreparedStatement stmt = this.connection.prepareStatement(query);
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String owned = rs.getString("OWNED_MUSIC");
                rs.close();
                stmt.close();
                return owned != null ? owned : "";
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ""; // 未找到时返回空字符串
    }

    // 检查玩家是否拥有某音乐
    public boolean isMusicOwned(UUID uuid, String musicName) {
        String ownedMusic = getOwnedMusic(uuid);
        return ownedMusic.contains(musicName);
    }
}
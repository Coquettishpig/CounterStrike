package src.counterstrike.Handler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.BlockState;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import src.counterstrike.Api.BombExplodeEvent;
import src.counterstrike.Api.GameStateChangeEvent;
import src.counterstrike.Api.RoundEndEvent;
import src.counterstrike.Api.RoundStartEvent;
import src.counterstrike.Cache.Defuse;
import src.counterstrike.Cache.PlayerShop;
import src.counterstrike.Cache.PlayerStatus;
import src.counterstrike.Cache.ShopType;
import src.counterstrike.Grenades.Grenade;
import src.counterstrike.Guns.Gun;
import src.counterstrike.Main;
import src.counterstrike.Messages;
import src.counterstrike.ScoreBoard.ScoreBoard;
import src.counterstrike.Utils.ItemBuilder;
import src.counterstrike.Version.MathUtils;
import src.counterstrike.Version.SpigotSound;

import java.util.*;

public class Game {
    private final int id;
    private int round;
    private final int min;
    private final int max;
    private int timer;
    private long startTime;
    private final BossBar bar;
    private final Main main;
    private GameTeam.Role round_winner;
    private final String name;
    private int scoreTeamA;
    private int scoreTeamB;
    private boolean isGameEnding;
    private final Location lobby;
    private boolean isGameStarted;
    private final Location mid;
    private boolean roundEnding;
    private final List<Location> fireworks;
    private final GameBomb bomb;
    private final List<Location> bombsiteLoc;
    private final List<Location> TerroristLoc;
    private GameState state;
    private final List<Location> CounterTerroristLoc;
    private final List<Location> signs;
    private final List<Player> spectators;
    private final GameTeam TeamA;
    private final GameTeam TeamB;
    private final List<BlockState> blockrestore;
    private final Map<Item, Integer> drops = new HashMap<>();
    private final Map<UUID, Integer> money = new HashMap<>();
    private final Map<UUID, Inventory> shops = new HashMap<>();
    private final Map<Player, Defuse> defusing = new HashMap<>();
    private final Map<UUID, ScoreBoard> status = new HashMap<>();
    private final Map<String, GameTeam> queue = new HashMap<>();
    private final Map<UUID, PlayerStatus> stats = new HashMap<>();
    private int teamALoseStreak; // TeamA 的连输次数
    private int teamBLoseStreak; // TeamB 的连输次数
    private Player bombPlanter; // 安装炸弹的玩家
    private Player bombDefuser; // 拆除炸弹的玩家

    public Game(final Main main, final int id, final Location lobby, final String name, final int min, final List<Location> CounterTerroristLoc, final List<Location> TerroristLoc, final List<Location> bombsiteLoc, final List<Location> fireworks) {
        this.round = 0;
        this.timer = 10;
        this.scoreTeamA = 0;
        this.scoreTeamB = 0;
        this.roundEnding = false;
        this.bomb = new GameBomb();
        this.state = GameState.WAITING;
        this.signs = new ArrayList<Location>();
        this.spectators = new ArrayList<Player>();
        this.TeamA = new GameTeam(new ArrayList<Player>());
        this.TeamB = new GameTeam(new ArrayList<Player>());
        this.blockrestore = new ArrayList<BlockState>();
        this.id = id;
        this.min = min;
        this.main = main;
        this.name = name;
        this.timer = main.getLobbyTime();
        this.CounterTerroristLoc = CounterTerroristLoc;
        this.lobby = lobby;
        this.bombsiteLoc = bombsiteLoc;
        this.TerroristLoc = TerroristLoc;
        this.fireworks = fireworks;
        this.max = TerroristLoc.size() + CounterTerroristLoc.size();
        this.bar = (main.enableBoss() ? Bukkit.createBossBar(Messages.BAR_WAITING.toString().replace("%name%", name), BarColor.WHITE, BarStyle.SOLID, new BarFlag[0]) : null);
        this.mid = CounterTerroristLoc.get(0).clone().add(CounterTerroristLoc.get(0).clone().subtract((Location) TerroristLoc.get(0)).multiply(0.5));
        this.teamALoseStreak = 0;
        this.teamBLoseStreak = 0;
        if (main.corpseSupport()) {
        }
    }

    public int getID() {
        return this.id;
    }

    public BossBar getBar() {
        return this.bar;
    }

    public Main getMain() {
        return this.main;
    }

    public Location getLobby() {
        return this.lobby;
    }

    public boolean inQueue(final Player p) {
        return this.queue.containsKey(p.getName());
    }

    public String getName() {
        return this.name;
    }

    public int getMinPlayers() {
        return this.min;
    }

    public int getMaxPlayers() {
        return this.max;
    }

    public GameTeam getTeamA() {
        return this.TeamA;
    }

    public GameTeam getTeamB() {
        return this.TeamB;
    }

    public int getTimer() {
        return this.timer;
    }

    public int getRound() {
        return this.round;
    }

    public int getScoreTeamA() {
        return this.scoreTeamA;
    }

    public int getScoreTeamB() {
        return this.scoreTeamB;
    }

    public List<BlockState> restoreBlocks() {
        return this.blockrestore;
    }

    public void setScoreTeamA(final int scoreTeamA) {
        this.scoreTeamA = scoreTeamA;
        this.main.getManager().updateTitle(this);
    }

    public void setScoreTeamB(final int scoreTeamB) {
        this.scoreTeamB = scoreTeamB;
        this.main.getManager().updateTitle(this);
    }

    public void removeFromQueue(final Player p) {
        this.queue.remove(p.getName());
    }

    public List<Location> getFireworks() {
        return this.fireworks;
    }

    public List<Location> getSigns() {
        return this.signs;
    }

    public List<Location> getBombLoc() {
        return this.bombsiteLoc;
    }

    public List<Location> getTerroristLoc() {
        return this.TerroristLoc;
    }

    public List<Location> getCounterTerroristLoc() {
        return this.CounterTerroristLoc;
    }

    public Map<Item, Integer> getDrops() {
        return this.drops;
    }

    public List<Player> getSpectators() {
        return this.spectators;
    }

    public void addSign(final Location sign) {
        this.signs.add(sign);
    }

    public GameBomb getBomb() {
        return this.bomb;
    }

    public Location getMid() {
        return this.mid;
    }

    public void setMoney(final Player p, final int money) {
        this.money.put(p.getUniqueId(), money);
    }

    public int getMoney(final Player p) {
        return this.money.get(p.getUniqueId());
    }

    public Map<UUID, PlayerStatus> getStats() {
        return this.stats;
    }

    public void addDefuser(final Player p, final int t) {
        this.defusing.put(p, new Defuse(t));
    }

    public boolean isDefusing(final Player p) {
        return this.defusing.get(p) != null;
    }

    public void resetDefusers() {
        this.defusing.clear();
    }

    public boolean isGameStarted() {
        return this.isGameStarted;
    }

    public boolean isGameEnding() {
        return this.isGameEnding;
    }

    public void isGameEnding(final boolean value) {
        this.isGameEnding = value;
    }

    public GameState getState() {
        return this.state;
    }

    public boolean isRoundEnding() {
        return this.roundEnding;
    }

    public void setRoundEnding(final boolean value) {
        this.roundEnding = value;
    }

    public void setGameTimer(final int timer) {
        this.timer = timer;
    }

    public Map<UUID, Inventory> getShops() {
        return this.shops;
    }

    public void setRoundWinner(final GameTeam.Role round_winner) {
        this.round_winner = round_winner;
    }

    public Map<UUID, ScoreBoard> getStatus() {
        return this.status;
    }

    // 获取和设置炸弹安装者
    public void setBombPlanter(Player planter) {
        this.bombPlanter = planter;
    }

    // 获取和设置炸弹拆除者
    public void setBombDefuser(Player defuser) {
        this.bombDefuser = defuser;
    }

    public void setState(final GameState state) {
        this.state = state;
        final GameStateChangeEvent e = new GameStateChangeEvent(this, state);
        this.main.getServer().getPluginManager().callEvent(e);
        this.main.getManager().updateSigns(this);
        this.main.getManager().updateTitle(this);
        if (this.bar != null) {
            switch (state) {
                case END: {
                    this.bar.setTitle("§8[§3" + Messages.TEAM_NAME + " " + Messages.TEAM_FIRST + "§8]§f " + this.scoreTeamA + "§7 " + Messages.SPLITTER + " §8[§4" + Messages.TEAM_NAME + " " + Messages.TEAM_SECOND + "§8]§f " + this.scoreTeamB);
                    this.bar.setProgress(1.0);
                    break;
                }
                case ROUND: {
                    this.bar.setTitle(Messages.BAR_INGAME.toString().replace("%name%", this.name).replace("%timer%", this.main.getRoundTime() + ""));
                    this.bar.setProgress(1.0);
                    break;
                }
                case WAITING: {
                    this.bar.setTitle(Messages.BAR_WAITING.toString().replace("%name%", this.name));
                    this.bar.setProgress(1.0);
                    break;
                }
            }
        }
    }

    public void addRandomTeam(final Player p) {
        this.TeamA.removePlayer(p);
        this.TeamB.removePlayer(p);
        if (MathUtils.random().nextBoolean()) {
            this.TeamA.addPlayer(p);
        } else {
            this.TeamB.addPlayer(p);
        }
    }

    public void addTeamA(final Player p) {
        this.TeamB.removePlayer(p);
        this.TeamA.removePlayer(p);
        this.TeamA.addPlayer(p);
    }

    public void addTeamB(final Player p) {
        this.TeamB.removePlayer(p);
        this.TeamA.removePlayer(p);
        this.TeamB.addPlayer(p);
    }

    /*
    游戏开始
    * */
    public void start() {
        this.isGameStarted = true;
        this.teamALoseStreak = 0;
        this.teamBLoseStreak = 0;
        if (MathUtils.random().nextBoolean()) {
            this.TeamA.setRole(GameTeam.Role.COUNTERTERRORIST);
            this.TeamB.setRole(GameTeam.Role.TERRORIST);
        } else {
            this.TeamA.setRole(GameTeam.Role.TERRORIST);
            this.TeamB.setRole(GameTeam.Role.COUNTERTERRORIST);
        }
    }

    public void addinQueue(final Player p) {
        if (this.TeamA.size() <= this.TeamB.size()) {
            this.queue.put(p.getName(), this.TeamA);
            p.teleport(this.TeamA.getPlayer(MathUtils.random().nextInt(this.TeamA.size())).getLocation());
            this.TeamA.addPlayer(p);
        } else {
            this.queue.put(p.getName(), this.TeamB);
            p.teleport(this.TeamB.getPlayer(MathUtils.random().nextInt(this.TeamB.size())).getLocation());
            this.TeamB.addPlayer(p);
        }
        this.spectators.add(p);
        this.stats.put(p.getUniqueId(), new PlayerStatus(p.getName(), p.getUniqueId()));
    }

    public void broadcast(final String message) {
        for (final Player p : this.TeamA.getPlayers()) {
            p.sendMessage(message.replace("&", "§"));
        }
        for (final Player p : this.TeamB.getPlayers()) {
            p.sendMessage(message.replace("&", "§"));
        }
    }

    // 计算战败补偿金额
    private int calculateLoseMoney(int loseStreak) {
        if (loseStreak <= 1) return 1400; // 连输0或1次
        else if (loseStreak == 2) return 1900;
        else if (loseStreak == 3) return 2400;
        else if (loseStreak == 4) return 2900;
        else return 3400; // 连输5次及以上
    }

    // 计算胜利奖励（根据胜利条件）
    private int calculateWinMoney(GameTeam.Role winnerRole) {
        return 3250; // 消灭敌人胜利
    }

    public void stop() {
        this.setScoreTeamA(this.round = 0);
        this.setScoreTeamB(0);
        this.TeamA.setRole(null);
        this.TeamB.setRole(null);
        this.isGameStarted = false;
    }

    public void run() {
        if (this.isGameStarted) {
            switch (this.state) {
                case END: {
                    if (this.timer != 0) {
                        break;
                    }
                    this.main.getManager().stopGame(this, this.main.autoJoin());
                    if (this.main.shutdown()) {
                        this.main.getServer().shutdown();
                        break;
                    }
                    break;
                }
                case IN_GAME: {
                    if (this.bar != null && !this.roundEnding) {
                        this.bar.setTitle(Messages.BAR_INGAME.toString().replace("%name%", this.name).replace("%timer%", this.timer + ""));
                        this.bar.setProgress(this.bomb.isPlanted() ? ((double) (this.timer / 45)) : (this.timer / (double) this.main.getRoundTime()));
                    }
                    if (this.timer == 90) {
                        for (final Player p : this.TeamA.getPlayers()) {
                            if (p.getOpenInventory() != null && p.getOpenInventory().getTitle().equals(Messages.ITEM_SHOP_NAME.toString())) {
                                p.closeInventory();
                                p.sendMessage(Messages.SHOP_AFTER_30_SECONDS.toString());
                            }
                        }
                        for (final Player p : this.TeamB.getPlayers()) {
                            if (p.getOpenInventory() != null && p.getOpenInventory().getTitle().equals(Messages.ITEM_SHOP_NAME.toString())) {
                                p.closeInventory();
                                p.sendMessage(Messages.SHOP_AFTER_30_SECONDS.toString());
                            }
                        }
                    }
                    if (this.timer == 30) {
                        for (final Player p : this.TeamA.getPlayers()) {
                            p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.secondsremain", 1.0f, 1.0f);
                        }
                        for (final Player p : this.TeamB.getPlayers()) {
                            p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.secondsremain", 1.0f, 1.0f);
                        }
                    }
                    for (final Player p : this.spectators) {
                        if (this.timer % 4 == 0) {
                            this.main.getVersionInterface().sendActionBar(p, Messages.BAR_RESPAWN_SECOND.toString());
                        } else {
                            if (this.timer % 2 != 0) {
                                continue;
                            }
                            this.main.getVersionInterface().sendActionBar(p, Messages.BAR_RESPAWN_FIRST.toString());
                        }
                    }
                    if (this.bomb.isPlanted()) {
                        this.bomb.setTimer(this.bomb.getTimer() - 1);
                        final int timer = this.bomb.getTimer();
                        if (timer % 4 == 0) {
                            for (final Player p2 : this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) {
                                if (!this.spectators.contains(p2)) {
                                    this.main.getVersionInterface().sendActionBar(p2, Messages.BAR_BOMB_PLANTED_CRIMS_SECOND.toString());
                                }
                            }
                            for (final Player p2 : this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                                if (!this.spectators.contains(p2)) {
                                    this.main.getVersionInterface().sendActionBar(p2, Messages.BAR_BOMB_PLANTED_COPS_SECOND.toString());
                                }
                            }
                        } else if (timer % 2 == 0) {
                            for (final Player p2 : this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) {
                                if (!this.spectators.contains(p2)) {
                                    this.main.getVersionInterface().sendActionBar(p2, Messages.BAR_BOMB_PLANTED_CRIMS_FIRST.toString());
                                }
                            }
                            for (final Player p2 : this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                                if (!this.spectators.contains(p2)) {
                                    this.main.getVersionInterface().sendActionBar(p2, Messages.BAR_BOMB_PLANTED_COPS_FIRST.toString());
                                }
                            }
                        }
                        if (timer > 0 && (timer <= 10 || timer % 2 == 0)) {
                            for (final Player p2 : this.TeamA.getPlayers()) {
                                p2.playSound(this.bomb.getLocation(), "cs_gamesounds.gamesounds.bombbeep", 1.0f, 1.0f);
                            }
                            for (final Player p2 : this.TeamB.getPlayers()) {
                                p2.playSound(this.bomb.getLocation(), "cs_gamesounds.gamesounds.bombbeep", 1.0f, 1.0f);
                            }
                        }
                        if (timer >= 0 && timer <= 5) {
                            for (final Player p2 : this.TeamA.getPlayers()) {
                                if (!this.defusing.containsKey(p2)) {
                                    this.main.getVersionInterface().sendTitle(p2, 0, 23, 0, "", "§c" + timer);
                                }
                            }
                            for (final Player p2 : this.TeamB.getPlayers()) {
                                if (!this.defusing.containsKey(p2)) {
                                    this.main.getVersionInterface().sendTitle(p2, 0, 23, 0, "", "§c" + timer);
                                }
                            }
                        }
                        final Iterator<Map.Entry<Player, Defuse>> it = this.defusing.entrySet().iterator();
                        while (it.hasNext()) {
                            final Map.Entry<Player, Defuse> entry = it.next();
                            final Player d = entry.getKey();
                            if (this.spectators.contains(entry.getKey()) || !this.isGameStarted) {
                                it.remove();
                                break;
                            }
                            if (d.getLocation().distance(this.bomb.getLocation()) > 2.0) {
                                this.main.getVersionInterface().sendTitle(d, 0, 40, 0, "", Messages.BOMB_DEFUSING_CANCELED.toString());
                                it.remove();
                                break;
                            }
                            String completed = "§a";
                            String incompleted = "§7";
                            for (int x = 0; x < entry.getValue().getMax() - entry.getValue().getTime(); ++x) {
                                completed += '┃';
                            }
                            for (int x = 0; x < entry.getValue().getTime(); ++x) {
                                incompleted += '┃';
                            }
                            this.main.getVersionInterface().sendTitle(d, 0, 40, 0, Messages.BOMB_DEFUSING.toString(), "§8[" + completed + incompleted + "§8] §c" + entry.getValue().getTime());
                            if (entry.getValue().getTime() == -1 && !this.roundEnding) {
                                this.setBombDefuser(d); // 记录成功拆除炸弹的玩家
                                String musicPath = main.getCommandExecutor().getCopEquippedMusic(d);
                                ++this.round;
                                for (final Player a : this.TeamA.getPlayers()) {
                                    a.playSound(a.getLocation(), "cs_gamesounds.gamesounds.bombdefused", 1.0f, 1.0f);
                                    this.main.getVersionInterface().sendTitle(a, 0, 60, 0, Messages.BOMB_DEFUSED.toString(), "§f" + Messages.PACK_BOMB + " §3" + d.getName());
                                    a.sendMessage(Messages.LINE_SPLITTER.toString());
                                    a.sendMessage(Messages.LINE_PREFIX.toString());
                                    a.sendMessage("");
                                    a.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_COP);
                                    final PlayerStatus top_cop = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                                    a.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString().replace("%player%", top_cop.getName()).replace("%kills%", "" + top_cop.getRoundKills()));
                                    final PlayerStatus top_crim = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                                    a.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString().replace("%player%", top_crim.getName()).replace("%kills%", "" + top_crim.getRoundKills()));
                                    a.sendMessage(Messages.LINE_SPLITTER.toString());
                                    a.playSound(a.getLocation(), musicPath, 1.0f, 1.0f);
                                    this.main.getVersionInterface().sendActionBar(a, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(musicPath));
                                }
                                for (final Player b : this.TeamB.getPlayers()) {
                                    b.playSound(b.getLocation(), "cs_gamesounds.gamesounds.bombdefused", 1.0f, 1.0f);
                                    this.main.getVersionInterface().sendTitle(b, 0, 60, 0, Messages.BOMB_DEFUSED.toString(), "§f鉻 §3" + d.getName());
                                    b.sendMessage(Messages.LINE_SPLITTER.toString());
                                    b.sendMessage(Messages.LINE_PREFIX.toString());
                                    b.sendMessage("");
                                    b.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_COP);
                                    final PlayerStatus top_cop = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                                    b.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString().replace("%player%", top_cop.getName()).replace("%kills%", "" + top_cop.getRoundKills()));
                                    final PlayerStatus top_crim = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                                    b.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString().replace("%player%", top_crim.getName()).replace("%kills%", "" + top_crim.getRoundKills()));
                                    b.sendMessage(Messages.LINE_SPLITTER.toString());
                                    b.playSound(b.getLocation(), musicPath, 1.0f, 1.0f);
                                    this.main.getVersionInterface().sendActionBar(b, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(musicPath));
                                }
                                this.timer = 13;
                                this.round_winner = GameTeam.Role.COUNTERTERRORIST;
                                if (this.TeamA.getRole() == GameTeam.Role.COUNTERTERRORIST) {
                                    this.setScoreTeamA(this.scoreTeamA + 1);
                                } else {
                                    this.setScoreTeamB(this.scoreTeamB + 1);
                                }
                                it.remove();
                                this.bomb.reset();
                                for (final Grenade grenade : this.main.getGrenades()) {
                                    grenade.remove(this);
                                }
                                this.roundEnding = true;
                                Bukkit.getPluginManager().callEvent(new RoundEndEvent(this));
                                break;
                            }
                            entry.getValue().setTime(entry.getValue().getTime() - 1);
                        }
                        if (timer == 0 && this.bomb.isPlanted()) {
                            this.bomb.getLocation().getBlock().setType(Material.AIR);
                            final BombExplodeEvent event = new BombExplodeEvent(this.bomb.getLocation());
                            this.main.getServer().getPluginManager().callEvent((Event) event);
                            this.bomb.getLocation().getWorld().playSound(this.bomb.getLocation(), SpigotSound.EXPLODE.getSound(), 5.0f, 5.0f);
                            this.bomb.getLocation().getWorld().spawnParticle(Particle.EXPLOSION_LARGE, this.bomb.getLocation(), 5);
                            for (final Player p3 : this.bomb.getNearbyPlayers(this, 15)) {
                                if (!this.spectators.contains(p3)) {
                                    this.main.getManager().damage(this, null, p3, 20.0, Messages.PACK_BOMB.toString());
                                }
                            }
                            if (!this.roundEnding) {
                                ++this.round;
                                String musicPath = main.getCommandExecutor().getCopEquippedMusic(bombPlanter);
                                this.timer = 13;
                                this.round_winner = GameTeam.Role.TERRORIST;
                                if (this.TeamA.getRole() == GameTeam.Role.TERRORIST) {
                                    this.setScoreTeamA(this.scoreTeamA + 1);
                                } else {
                                    this.setScoreTeamB(this.scoreTeamB + 1);
                                }
                                for (final Player p3 : this.TeamA.getPlayers()) {
                                    p3.playSound(this.bomb.getLocation(), "cs_gamesounds.gamesounds.wilhelm", 1.0f, 1.0f);
                                    p3.sendMessage(Messages.LINE_SPLITTER.toString());
                                    p3.sendMessage(Messages.LINE_PREFIX.toString());
                                    p3.sendMessage("");
                                    p3.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_CRIMS);
                                    final PlayerStatus top_cop2 = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                                    p3.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString().replace("%player%", top_cop2.getName()).replace("%kills%", "" + top_cop2.getRoundKills()));
                                    final PlayerStatus top_crim2 = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                                    p3.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString().replace("%player%", top_crim2.getName()).replace("%kills%", "" + top_crim2.getRoundKills()));
                                    p3.sendMessage(Messages.LINE_SPLITTER.toString());
                                    p3.playSound(p3.getLocation(), musicPath, 1.0f, 1.0f);
                                    this.main.getVersionInterface().sendActionBar(p3, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(musicPath));
                                }
                                for (final Player p3 : this.TeamB.getPlayers()) {
                                    p3.playSound(this.bomb.getLocation(), "cs_gamesounds.gamesounds.wilhelm", 1.0f, 1.0f);
                                    p3.sendMessage(Messages.LINE_SPLITTER.toString());
                                    p3.sendMessage(Messages.LINE_PREFIX.toString());
                                    p3.sendMessage("");
                                    p3.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_CRIMS);
                                    final PlayerStatus top_cop2 = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                                    p3.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString().replace("%player%", top_cop2.getName()).replace("%kills%", "" + top_cop2.getRoundKills()));
                                    final PlayerStatus top_crim2 = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                                    p3.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString().replace("%player%", top_crim2.getName()).replace("%kills%", "" + top_crim2.getRoundKills()));
                                    p3.sendMessage(Messages.LINE_SPLITTER.toString());
                                    p3.playSound(p3.getLocation(), musicPath, 1.0f, 1.0f);
                                    this.main.getVersionInterface().sendActionBar(p3, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(musicPath));
                                }
                                this.roundEnding = true;
                                Bukkit.getPluginManager().callEvent(new RoundEndEvent(this));
                            }
                            this.bomb.reset();
                        }
                    }
                    if (this.roundEnding) {
                        if (this.main.getRoundToWin() <= this.scoreTeamA || this.main.getRoundToWin() <= this.scoreTeamB) {
                            this.timer = 10;
                            this.setState(GameState.END);
                            final String winner = (this.scoreTeamA > this.scoreTeamB) ? (Messages.TEAM_NAME.toString().replace(":", "") + " " + Messages.TEAM_FIRST) : (Messages.TEAM_NAME.toString().replace(":", "") + " " + Messages.TEAM_SECOND);
                            for (final Player p2 : this.TeamA.getPlayers()) {
                                if (!this.spectators.contains(p2)) {
                                    this.main.getManager().clearPlayer(p2);
                                }
                                if (this.scoreTeamA > this.scoreTeamB) {
                                    if (this.main.getEndGameCommandWin() != null && !this.main.getEndGameCommandWin().equalsIgnoreCase("none")) {
                                        Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), this.main.getEndGameCommandWin().replace("%player%", p2.getName()));
                                    }
                                } else if (this.main.getEndGameCommandLose() != null && !this.main.getEndGameCommandLose().equalsIgnoreCase("none")) {
                                    Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), this.main.getEndGameCommandLose().replace("%player%", p2.getName()));
                                }
                                p2.sendMessage(Messages.LINE_SPLITTER.toString());
                                p2.sendMessage(Messages.LINE_PREFIX.toString());
                                p2.sendMessage("");
                                p2.sendMessage("➢ §e" + Messages.WINNER.toString() + "§f: §c" + winner);
                                p2.sendMessage(Messages.LINE_SPLITTER.toString());
                                this.main.getVersionInterface().sendTitle(p2, 0, 200, 0, Messages.TITLE_GAME_OVER.toString(), "§8[§3" + Messages.TEAM_NAME + " " + Messages.TEAM_FIRST + "§8]§f " + this.scoreTeamA + "§7 " + Messages.SPLITTER + " §8[§4" + Messages.TEAM_NAME + " " + Messages.TEAM_SECOND + "§8]§f " + this.scoreTeamB);
                            }
                            for (final Player p2 : this.TeamB.getPlayers()) {
                                if (!this.spectators.contains(p2)) {
                                    this.main.getManager().clearPlayer(p2);
                                }
                                if (this.scoreTeamB > this.scoreTeamA) {
                                    if (this.main.getEndGameCommandWin() != null && !this.main.getEndGameCommandWin().equalsIgnoreCase("none")) {
                                        Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), this.main.getEndGameCommandWin().replace("%player%", p2.getName()));
                                    }
                                } else if (this.main.getEndGameCommandLose() != null && !this.main.getEndGameCommandLose().equalsIgnoreCase("none")) {
                                    Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), this.main.getEndGameCommandLose().replace("%player%", p2.getName()));
                                }
                                p2.sendMessage(Messages.LINE_SPLITTER.toString());
                                p2.sendMessage(Messages.LINE_PREFIX.toString());
                                p2.sendMessage("");
                                p2.sendMessage("➢ §e" + Messages.WINNER.toString() + "§f: §c" + winner);
                                p2.sendMessage(Messages.LINE_SPLITTER.toString());
                                this.main.getVersionInterface().sendTitle(p2, 0, 200, 0, Messages.TITLE_GAME_OVER.toString(), "§8[§3" + Messages.TEAM_NAME + " " + Messages.TEAM_FIRST + "§8]§f " + this.scoreTeamA + "§7 " + Messages.SPLITTER + " §8[§4" + Messages.TEAM_NAME + " " + Messages.TEAM_SECOND + "§8]§f " + this.scoreTeamB);
                            }
//                            结束回放录制
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "replay stop " + name + "-" + startTime);
                            this.main.getManager().endRound(this);
                            break;
                        }
                        if (this.timer == 0) {
                            this.timer = 11;
                            if (this.round == this.main.getRoundToSwitch()) {
                                this.timer = 16;
                                if (this.TeamA.getRole() == GameTeam.Role.TERRORIST) {
                                    this.TeamB.setRole(GameTeam.Role.TERRORIST);
                                    this.TeamA.setRole(GameTeam.Role.COUNTERTERRORIST);
                                } else {
                                    this.TeamA.setRole(GameTeam.Role.TERRORIST);
                                    this.TeamB.setRole(GameTeam.Role.COUNTERTERRORIST);
                                }
                                for (final ScoreBoard board : this.status.values()) {
                                    board.removeTeam();
                                    board.showTeams(this);
                                }
                                for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) {
                                    final Inventory inv = this.shops.get(p.getUniqueId());
                                    inv.clear();
                                    for (final PlayerShop shop : this.main.getShops()) {
                                        if (shop.getType() == ShopType.GRENADE) {
                                            final Grenade grenade2 = this.main.getGrenade(shop.getWeaponName());
                                            inv.setItem(shop.getSlot(), ItemBuilder.create(grenade2.getItem().getType(), 1, grenade2.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                        } else if (shop.getType() == ShopType.GUN && (shop.getRole() == null || shop.getRole() == GameTeam.Role.TERRORIST)) {
                                            if (this.main.hideVipGuns() && shop.hasPermission() && !p.hasPermission("cs.weapon." + shop.getWeaponName())) {
                                                continue;
                                            }
                                            final Gun gun = this.main.getGun(shop.getWeaponName());
                                            inv.setItem(shop.getSlot(), ItemBuilder.create(gun.getItem().getType(), 1, gun.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                        } else {
                                            if (shop.getRole() != null && shop.getRole() != GameTeam.Role.TERRORIST) {
                                                continue;
                                            }

                                            inv.setItem(shop.getSlot(), ItemBuilder.create(shop.getMaterial(), 1, shop.getName().replace('&', '§'), shop.getLore()));
                                        }
                                    }
                                }
                                for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                                    final Inventory inv = this.shops.get(p.getUniqueId());
                                    inv.clear();
                                    for (final PlayerShop shop : this.main.getShops()) {
                                        if (shop.getType() == ShopType.GRENADE) {
                                            final Grenade grenade2 = this.main.getGrenade(shop.getWeaponName());
                                            inv.setItem(shop.getSlot(), ItemBuilder.create(grenade2.getItem().getType(), 1, grenade2.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                        } else if (shop.getType() == ShopType.GUN && (shop.getRole() == null || shop.getRole() == GameTeam.Role.COUNTERTERRORIST)) {
                                            if (this.main.hideVipGuns() && shop.hasPermission() && !p.hasPermission("cs.weapon." + shop.getWeaponName())) {
                                                continue;
                                            }
                                            final Gun gun = this.main.getGun(shop.getWeaponName());
                                            inv.setItem(shop.getSlot(), ItemBuilder.create(gun.getItem().getType(), 1, gun.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                        } else {
                                            if (shop.getRole() != null && shop.getRole() != GameTeam.Role.COUNTERTERRORIST) {
                                                continue;
                                            }
                                            inv.setItem(shop.getSlot(), ItemBuilder.create(shop.getMaterial(), 1, shop.getName().replace('&', '§'), shop.getLore()));
                                        }
                                    }
                                }
                            } else {
                                this.timer = 13;

                                // 更新连输次数并发放金钱
                                if (this.round_winner == this.TeamA.getRole()) {
                                    // TeamA 胜利
                                    int winMoney = this.main.getRoundWinMoney();
                                    for (Player p : this.TeamA.getPlayers()) {
                                        this.setMoney(p, this.getMoney(p) + winMoney);
                                        p.sendMessage("§a回合胜利奖励: $" + winMoney);
                                    }
                                    int loseMoney = calculateLoseMoney(this.teamBLoseStreak);
                                    for (Player p : this.TeamB.getPlayers()) {
                                        this.setMoney(p, this.getMoney(p) + loseMoney);
                                        p.sendMessage("§c回合失败补偿: $" + loseMoney);
                                    }
                                    this.teamALoseStreak = 0; // TeamA 胜利，重置连输
                                    this.teamBLoseStreak++;  // TeamB 失败，连输+1
                                } else if (this.round_winner == this.TeamB.getRole()) {
                                    // TeamB 胜利
                                    int winMoney = this.main.getRoundWinMoney();
                                    for (Player p : this.TeamB.getPlayers()) {
                                        this.setMoney(p, this.getMoney(p) + winMoney);
                                        p.sendMessage("§a回合胜利奖励: $" + winMoney);
                                    }
                                    int loseMoney = calculateLoseMoney(this.teamALoseStreak);
                                    for (Player p : this.TeamA.getPlayers()) {
                                        this.setMoney(p, this.getMoney(p) + loseMoney);
                                        p.sendMessage("§c回合失败补偿: $" + loseMoney);
                                    }
                                    this.teamBLoseStreak = 0; // TeamB 胜利，重置连输
                                    this.teamALoseStreak++;  // TeamA 失败，连输+1
                                }
                            }
                            for (final Player p : this.TeamA.getPlayers()) {
                                if (this.queue.size() > 0) {
                                    p.sendMessage(Messages.NEW_COMBATANTS.toString());
                                    for (final Map.Entry<String, GameTeam> next : this.queue.entrySet()) {
                                        final String prefix = (next.getValue().getRole() == GameTeam.Role.TERRORIST) ? Messages.PACK_CRIMS.toString() : Messages.PACK_COPS.toString();
                                        p.sendMessage(prefix + " " + next.getKey());
                                    }
                                }
                            }
                            for (final Player p : this.TeamB.getPlayers()) {
                                if (this.queue.size() > 0) {
                                    p.sendMessage(Messages.NEW_COMBATANTS.toString());
                                    for (final Map.Entry<String, GameTeam> next : this.queue.entrySet()) {
                                        final String prefix = (next.getValue().getRole() == GameTeam.Role.TERRORIST) ? Messages.PACK_CRIMS.toString() : Messages.PACK_COPS.toString();
                                        p.sendMessage(prefix + " " + next.getKey());
                                    }
                                }
                            }
                            this.queue.clear();
                            this.setState(GameState.ROUND);
                            this.main.getManager().endRound(this);
                            this.main.getManager().resetPlayers(this);
                            Bukkit.getPluginManager().callEvent((Event) new RoundStartEvent(this));
                            break;
                        }
                    } else {
                        if (!this.bomb.isPlanted()) {
                            for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) {
                                if (this.bomb.getCarrier() == p) {
                                    p.setCompassTarget(this.bomb.getLocation());
                                    this.main.getVersionInterface().sendActionBar(p, Messages.BAR_BOMB_PLANT.toString());
                                } else {
                                    if (this.spectators.contains(p)) {
                                        continue;
                                    }
                                    p.setCompassTarget(this.bomb.getLocation());
                                    this.main.getVersionInterface().sendActionBar(p, Messages.BAR_BOMB_PROTECTOR.toString());
                                }
                            }
                            for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                                if (!this.spectators.contains(p)) {
                                    this.main.getVersionInterface().sendActionBar(p, Messages.BAR_BOMB_DEFEND.toString());
                                }
                            }
                        }
                        // 情况1：匪方（TERRORIST）获胜
                        if (new HashSet<>(this.spectators).containsAll(this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers())) {
                            // 确定胜方MVP（匪方击杀王）
                            PlayerStatus topCrim = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                            String mvpMusicPath = main.getCommandExecutor().getCopEquippedMusic(Bukkit.getPlayer(topCrim.getName())); // 匪方MVP的音乐

                            // 给所有玩家（TeamA 和 TeamB）发送消息和播放音乐
                            for (Player p : this.TeamA.getPlayers()) {
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.sendMessage(Messages.LINE_PREFIX.toString());
                                p.sendMessage("");
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_CRIMS);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString()
                                        .replace("%player%", this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST).getName())
                                        .replace("%kills%", "" + this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST).getRoundKills()));
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString()
                                        .replace("%player%", topCrim.getName())
                                        .replace("%kills%", "" + topCrim.getRoundKills()));
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.playSound(p.getLocation(), mvpMusicPath, 1.0f, 1.0f); // 播放MVP音乐
                                this.main.getVersionInterface().sendTitle(p, 0, 80, 0, Messages.MVP.toString(), topCrim.getName()); // 显示MVP名字
                                this.main.getVersionInterface().sendActionBar(p, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(mvpMusicPath));
                            }
                            for (Player p : this.TeamB.getPlayers()) {
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.sendMessage(Messages.LINE_PREFIX.toString());
                                p.sendMessage("");
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_CRIMS);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString()
                                        .replace("%player%", this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST).getName())
                                        .replace("%kills%", "" + this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST).getRoundKills()));
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString()
                                        .replace("%player%", topCrim.getName())
                                        .replace("%kills%", "" + topCrim.getRoundKills()));
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.playSound(p.getLocation(), mvpMusicPath, 1.0f, 1.0f); // 播放MVP音乐
                                this.main.getVersionInterface().sendTitle(p, 0, 80, 0, Messages.MVP.toString(), topCrim.getName());
                                this.main.getVersionInterface().sendActionBar(p, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(mvpMusicPath));
                            }

                            // 更新回合和分数
                            ++this.round;
                            this.timer = 13;
                            this.round_winner = GameTeam.Role.TERRORIST;
                            if (this.TeamA.getRole() == GameTeam.Role.TERRORIST) {
                                this.setScoreTeamA(this.scoreTeamA + 1);
                            } else {
                                this.setScoreTeamB(this.scoreTeamB + 1);
                            }
                            for (final Grenade grenade3 : this.main.getGrenades()) {
                                grenade3.remove(this);
                            }
                            this.roundEnding = true;
                            Bukkit.getPluginManager().callEvent(new RoundEndEvent(this));
                        }

                        // 情况2：警方（COUNTERTERRORIST）获胜
                        if (new HashSet<>(this.spectators).containsAll(this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) && !this.bomb.isPlanted()) {
                            // 确定胜方MVP（警方击杀王）
                            PlayerStatus topCop = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                            String mvpMusicPath = main.getCommandExecutor().getCrimEquippedMusic(Bukkit.getPlayer(topCop.getName())); // 警方MVP的音乐

                            // 给所有玩家（TeamA 和 TeamB）发送消息和播放音乐
                            for (Player p : this.TeamA.getPlayers()) {
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.sendMessage(Messages.LINE_PREFIX.toString());
                                p.sendMessage("");
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_COP);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString()
                                        .replace("%player%", topCop.getName())
                                        .replace("%kills%", "" + topCop.getRoundKills()));
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString()
                                        .replace("%player%", this.main.getManager().getTop(this, GameTeam.Role.TERRORIST).getName())
                                        .replace("%kills%", "" + this.main.getManager().getTop(this, GameTeam.Role.TERRORIST).getRoundKills()));
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.playSound(p.getLocation(), mvpMusicPath, 1.0f, 1.0f); // 播放MVP音乐
                                this.main.getVersionInterface().sendTitle(p, 0, 80, 0, Messages.MVP.toString(), topCop.getName()); // 显示MVP名字
                                this.main.getVersionInterface().sendActionBar(p, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(mvpMusicPath));
                            }
                            for (Player p : this.TeamB.getPlayers()) {
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.sendMessage(Messages.LINE_PREFIX.toString());
                                p.sendMessage("");
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_WINNER_COP);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString()
                                        .replace("%player%", topCop.getName())
                                        .replace("%kills%", "" + topCop.getRoundKills()));
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString()
                                        .replace("%player%", this.main.getManager().getTop(this, GameTeam.Role.TERRORIST).getName())
                                        .replace("%kills%", "" + this.main.getManager().getTop(this, GameTeam.Role.TERRORIST).getRoundKills()));
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.playSound(p.getLocation(), mvpMusicPath, 1.0f, 1.0f); // 播放MVP音乐
                                this.main.getVersionInterface().sendTitle(p, 0, 80, 0, Messages.MVP.toString(), topCop.getName());
                                this.main.getVersionInterface().sendActionBar(p, Messages.MVP_PLAY + main.getCommandExecutor().getMusicDisplayName(mvpMusicPath));
                            }

                            // 更新回合和分数
                            ++this.round;
                            this.timer = 13;
                            this.round_winner = GameTeam.Role.COUNTERTERRORIST;
                            if (this.TeamA.getRole() == GameTeam.Role.COUNTERTERRORIST) {
                                this.setScoreTeamA(this.scoreTeamA + 1);
                            } else {
                                this.setScoreTeamB(this.scoreTeamB + 1);
                            }
                            for (final Grenade grenade3 : this.main.getGrenades()) {
                                grenade3.remove(this);
                            }
                            this.roundEnding = true;
                            Bukkit.getPluginManager().callEvent(new RoundEndEvent(this));
                        }
                        if (this.timer == 0) {
                            ++this.round;
                            for (final Player p : this.TeamA.getPlayers()) {
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.sendMessage(Messages.LINE_PREFIX.toString());
                                p.sendMessage("");
                                final String[] split;
                                final String[] message = split = Messages.WINNER_TIMES_OUT.toString().split("#");
                                for (final String m : split) {
                                    p.sendMessage(Messages.GAME_MARKER.toString() + m);
                                }
                                p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.copswinthegame", 1.0f, 1.0f);
                                final PlayerStatus top_cop4 = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString().replace("%player%", top_cop4.getName()).replace("%kills%", "" + top_cop4.getRoundKills()));
                                final PlayerStatus top_crim4 = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString().replace("%player%", top_crim4.getName()).replace("%kills%", "" + top_crim4.getRoundKills()));
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                            }
                            for (final Player p : this.TeamB.getPlayers()) {
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                                p.sendMessage(Messages.LINE_PREFIX.toString());
                                p.sendMessage("");
                                final String[] split2;
                                final String[] message = split2 = Messages.WINNER_TIMES_OUT.toString().split("#");
                                for (final String m : split2) {
                                    p.sendMessage(Messages.GAME_MARKER.toString() + m);
                                }
                                p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.copswinthegame", 1.0f, 1.0f);
                                final PlayerStatus top_cop4 = this.main.getManager().getTop(this, GameTeam.Role.COUNTERTERRORIST);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_TOP_COP.toString().replace("%player%", top_cop4.getName()).replace("%kills%", "" + top_cop4.getRoundKills()));
                                final PlayerStatus top_crim4 = this.main.getManager().getTop(this, GameTeam.Role.TERRORIST);
                                p.sendMessage(Messages.GAME_MARKER.toString() + Messages.ROUND_MOST_WANTED.toString().replace("%player%", top_crim4.getName()).replace("%kills%", "" + top_crim4.getRoundKills()));
                                p.sendMessage(Messages.LINE_SPLITTER.toString());
                            }
                            this.timer = 8;
                            this.round_winner = GameTeam.Role.COUNTERTERRORIST;
                            if (this.TeamA.getRole() == GameTeam.Role.COUNTERTERRORIST) {
                                this.setScoreTeamA(this.scoreTeamA + 1);
                            } else {
                                this.setScoreTeamB(this.scoreTeamB + 1);
                            }
                            for (final Grenade grenade3 : this.main.getGrenades()) {
                                grenade3.remove(this);
                            }
                            this.roundEnding = true;
                            Bukkit.getPluginManager().callEvent(new RoundEndEvent(this));
                            break;
                        }
                    }
                    break;
                }
                case ROUND: {
                    if (this.round == this.main.getRoundToSwitch() && this.timer >= 11) {
                        for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                            this.main.getVersionInterface().sendTitle(p, 0, 40, 0, Messages.TEAM_SWAP.toString(), Messages.TEAM_SWAP_COPS.toString());
                        }
                        for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) {
                            this.main.getVersionInterface().sendTitle(p, 0, 40, 0, Messages.TEAM_SWAP.toString(), Messages.TEAM_SWAP_CRIMS.toString());
                        }
                        break;
                    }
                    if (this.timer >= 11) {
                        for (final Player p : this.TeamA.getPlayers()) {
                            this.main.getVersionInterface().sendTitle(p, 0, 40, 0, Messages.PREFIX.toString(), Messages.ROUND_FIRST.toString());
                        }
                        for (final Player p : this.TeamB.getPlayers()) {
                            this.main.getVersionInterface().sendTitle(p, 0, 40, 0, Messages.PREFIX.toString(), Messages.ROUND_FIRST.toString());
                        }
                        break;
                    }
                    if (this.timer == 0) {
                        for (final Player p : this.TeamA.getPlayers()) {
                            this.main.getVersionInterface().sendTitle(p, 0, 30, 0, Messages.ROUND_START.toString(), Messages.OPEN_SHOP.toString());
                            p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.roundstart", 1.0f, 1.0f);
                        }
                        for (final Player p : this.TeamB.getPlayers()) {
                            this.main.getVersionInterface().sendTitle(p, 0, 30, 0, Messages.ROUND_START.toString(), Messages.OPEN_SHOP.toString());
                            p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.roundstart", 1.0f, 1.0f);
                        }
                        this.timer = this.main.getRoundTime();
                        this.setState(GameState.IN_GAME);
                        break;
                    }
                    final String time = this.timer >= 7 ? "§a" + this.timer : this.timer >= 4 ? "§e" + this.timer : "§c" + this.timer;
                    for (final Player p2 : this.TeamA.getPlayers()) {
                        this.main.getVersionInterface().sendTitle(p2, 0, 30, 0, time, Messages.OPEN_SHOP.toString());
                    }
                    for (final Player p2 : this.TeamB.getPlayers()) {
                        this.main.getVersionInterface().sendTitle(p2, 0, 30, 0, time, Messages.OPEN_SHOP.toString());
                    }
                    break;
                }
                case WAITING: {
                    if (this.TeamA.getPlayers().size() + this.TeamB.getPlayers().size() < this.min) {
                        this.stop();
                        for (final Player p : this.TeamA.getPlayers()) {
                            p.sendMessage(Messages.PREFIX + Messages.NOT_ENOUGH_PLAYERS.toString());
                        }
                        for (final Player p : this.TeamB.getPlayers()) {
                            p.sendMessage(Messages.PREFIX + Messages.NOT_ENOUGH_PLAYERS.toString());
                        }
                        this.timer = this.main.getLobbyTime();
                        for (final ScoreBoard board : this.status.values()) {
                            this.main.getManager().updateStatus(this, board.getStatus());
                        }
                        break;
                    }
                    if (this.timer == 0) {
//        拼接玩家名
                        StringBuilder playerNames = new StringBuilder(" ");
                        TeamA.getPlayers().forEach(player -> playerNames.append(player.getName()).append(" "));
                        TeamB.getPlayers().forEach(player -> playerNames.append(player.getName()).append(" "));
//        录制回放
                        startTime = System.currentTimeMillis();
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "replay start " + name + "-" + startTime + playerNames);

                        while (true) {
                            if (this.TeamA.size() <= this.max / 2 && this.TeamB.size() <= this.max / 2) {
                                if (MathUtils.abs(this.TeamA.size() - this.TeamB.size()) <= 1) {
                                    break;
                                }
                                if (this.TeamA.size() < this.TeamB.size()) {
                                    final Player p4 = this.TeamB.getPlayer(MathUtils.random().nextInt(this.TeamB.size()));
                                    this.TeamB.removePlayer(p4);
                                    this.TeamA.addPlayer(p4);
                                } else {
                                    if (this.TeamB.size() >= this.TeamA.size()) {
                                        continue;
                                    }
                                    final Player p4 = this.TeamA.getPlayer(MathUtils.random().nextInt(this.TeamA.size()));
                                    this.TeamA.removePlayer(p4);
                                    this.TeamB.addPlayer(p4);
                                }
                            } else if (this.TeamA.size() > this.max / 2) {
                                final Player p4 = this.TeamA.getPlayer(MathUtils.random().nextInt(this.TeamA.size()));
                                this.TeamA.removePlayer(p4);
                                this.TeamB.addPlayer(p4);
                            } else {
                                if (this.TeamB.size() <= this.max / 2) {
                                    continue;
                                }
                                final Player p4 = this.TeamB.getPlayer(MathUtils.random().nextInt(this.TeamB.size()));
                                this.TeamB.removePlayer(p4);
                                this.TeamA.addPlayer(p4);
                            }
                        }
                        for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.TERRORIST).getPlayers()) {
                            final Inventory inv = Bukkit.createInventory((InventoryHolder) null, 54, Messages.ITEM_SHOP_NAME.toString());
                            for (final PlayerShop shop : this.main.getShops()) {
                                if (shop.getType() == ShopType.GRENADE) {
                                    final Grenade grenade2 = this.main.getGrenade(shop.getWeaponName());
                                    inv.setItem(shop.getSlot(), ItemBuilder.create(grenade2.getItem().getType(), 1, grenade2.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                } else if (shop.getType() == ShopType.GUN && (shop.getRole() == null || shop.getRole() == GameTeam.Role.TERRORIST)) {
                                    if (this.main.hideVipGuns() && shop.hasPermission() && !p.hasPermission("cs.weapon." + shop.getWeaponName())) {
                                        continue;
                                    }
                                    final Gun gun = this.main.getGun(shop.getWeaponName());
                                    inv.setItem(shop.getSlot(), ItemBuilder.create(gun.getItem().getType(), 1, gun.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                } else {
                                    if (shop.getRole() != null && shop.getRole() != GameTeam.Role.TERRORIST) {
                                        continue;
                                    }
                                    inv.setItem(shop.getSlot(), ItemBuilder.create(shop.getMaterial(), 1, shop.getName().replace('&', '§'), shop.getLore()));
                                }
                            }
                            this.shops.put(p.getUniqueId(), inv);
                            this.status.get(p.getUniqueId()).showTeams(this);
                            this.status.get(p.getUniqueId()).showHealth(this);
                        }
                        for (final Player p : this.main.getManager().getTeam(this, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                            final Inventory inv = Bukkit.createInventory((InventoryHolder) null, 54, Messages.ITEM_SHOP_NAME.toString());
                            for (final PlayerShop shop : this.main.getShops()) {
                                if (shop.getType() == ShopType.GRENADE) {
                                    final Grenade grenade2 = this.main.getGrenade(shop.getWeaponName());
                                    inv.setItem(shop.getSlot(), ItemBuilder.create(grenade2.getItem().getType(), 1, grenade2.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                } else if (shop.getType() == ShopType.GUN && (shop.getRole() == null || shop.getRole() == GameTeam.Role.COUNTERTERRORIST)) {
                                    if (this.main.hideVipGuns() && shop.hasPermission() && !p.hasPermission("cs.weapon." + shop.getWeaponName())) {
                                        continue;
                                    }
                                    final Gun gun = this.main.getGun(shop.getWeaponName());
                                    inv.setItem(shop.getSlot(), ItemBuilder.create(gun.getItem().getType(), 1, gun.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
                                } else {
                                    if (shop.getRole() != null && shop.getRole() != GameTeam.Role.COUNTERTERRORIST) {
                                        continue;
                                    }
                                    inv.setItem(shop.getSlot(), ItemBuilder.create(shop.getMaterial(), 1, shop.getName().replace('&', '§'), shop.getLore()));
                                }
                            }
                            this.shops.put(p.getUniqueId(), inv);
                            this.status.get(p.getUniqueId()).showTeams(this);
                            this.status.get(p.getUniqueId()).showHealth(this);
                        }
                        // 回合准备时间
                        this.timer = 11;
                        this.main.getManager().resetPlayers(this);
                        for (final ScoreBoard board : this.status.values()) {
                            board.getStatus().reset();
                        }
                        this.setState(GameState.ROUND);
                        Bukkit.getPluginManager().callEvent(new RoundStartEvent(this));
                        break;
                    }
                    for (final Player p : this.TeamA.getPlayers()) {
                        if (this.timer <= 5) {
                            p.playSound(p.getEyeLocation(), SpigotSound.NOTE_PLING.getSound(), 1.0f, 0.5f);
                            p.sendMessage(Messages.PREFIX + Messages.GAME_START.toString().replace("%timer%", this.timer + ""));
                        } else {
                            if (this.timer % 10 != 0) {
                                continue;
                            }
                            p.sendMessage(Messages.PREFIX + Messages.GAME_START.toString().replace("%timer%", this.timer + ""));
                        }
                    }
                    for (final Player p : this.TeamB.getPlayers()) {
                        if (this.timer <= 5) {
                            p.playSound(p.getEyeLocation(), SpigotSound.NOTE_PLING.getSound(), 1.0f, 0.5f);
                            p.sendMessage(Messages.PREFIX.toString() + Messages.GAME_START.toString().replace("%timer%", this.timer + ""));
                        } else {
                            if (this.timer % 10 != 0) {
                                continue;
                            }
                            p.sendMessage(Messages.PREFIX.toString() + Messages.GAME_START.toString().replace("%timer%", this.timer + ""));
                        }
                    }
                    break;
                }
            }
            --this.timer;
        }
    }
}

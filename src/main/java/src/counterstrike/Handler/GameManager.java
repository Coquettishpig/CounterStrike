package src.counterstrike.Handler;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import src.counterstrike.Api.GameJoinEvent;
import src.counterstrike.Api.GameLeaveEvent;
import src.counterstrike.Cache.PlayerData;
import src.counterstrike.Cache.PlayerShop;
import src.counterstrike.Cache.PlayerStatus;
import src.counterstrike.Cache.ShopType;
import src.counterstrike.Grenades.Grenade;
import src.counterstrike.Guns.Gun;
import src.counterstrike.Main;
import src.counterstrike.Messages;
import src.counterstrike.MySQL.MySQL;
import src.counterstrike.ScoreBoard.ScoreBoard;
import src.counterstrike.ScoreBoard.ScoreBoardStatus;
import src.counterstrike.Utils.ItemBuilder;
import src.counterstrike.Version.MathUtils;
import src.counterstrike.Version.SpigotSound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class GameManager {
    private Main main;
    private Location spawn;
    private boolean bungee;
    private int bungee_map;
    private List<Game> games;
    private List<Location> quick_join;
    private HashMap<UUID, PlayerData> data;

    public GameManager(final Main main, final boolean bungee) {
        this.bungee_map = 0;
        this.games = new ArrayList<Game>();
        this.quick_join = new ArrayList<Location>();
        this.data = new HashMap<UUID, PlayerData>();
        this.main = main;
        if (bungee) {
            this.bungee = bungee;
            main.getServer().getMessenger().registerOutgoingPluginChannel((Plugin) main, "BungeeCord");
        }
    }

    public void setSpawn(final Location l) {
        this.spawn = l;
    }

    public int getMap() {
        return this.bungee_map;
    }

    public Game findGame(final Player p) {
        int difference = 0;
        Game in_game = null;
        for (final Game game : this.main.getManager().getGames()) {
            final int game_difference = MathUtils.abs(game.getTeamA().size() - game.getTeamB().size());
            if ((game.getState() == GameState.IN_GAME || game.getState() == GameState.ROUND) && game.getTeamA().size() + game.getTeamB().size() < game.getMaxPlayers() && game.getTimer() > 1 && game.getTeamA().size() > 0 && game.getTeamB().size() > 0) {
                if (in_game == null) {
                    in_game = game;
                    difference = game_difference;
                } else if (game_difference > 1 && game_difference > difference) {
                    in_game = game;
                    difference = game_difference;
                } else {
                    if (difference > game_difference || game.getTeamA().size() + game.getTeamB().size() <= in_game.getTeamA().size() + in_game.getTeamB().size()) {
                        continue;
                    }
                    in_game = game;
                    difference = game_difference;
                }
            }
        }
        if (in_game != null) {
            return in_game;
        }
        int players = 0;
        for (final Game game2 : this.main.getManager().getGames()) {
            final int size = game2.getTeamA().size() + game2.getTeamB().size();
            if (game2.getState() == GameState.WAITING && game2.getTeamA().size() + game2.getTeamB().size() < game2.getMaxPlayers() && game2.getTimer() >= 1) {
                if (in_game == null) {
                    in_game = game2;
                    players = size;
                } else {
                    if (size <= players) {
                        continue;
                    }
                    in_game = game2;
                    players = size;
                }
            }
        }
        if (in_game == null) {
            p.sendMessage(Messages.NO_GAME_FOUND.toString());
            return null;
        }
        return in_game;
    }

    public void addQuickPlayer(final Game g, final Player p) {
        // 检查玩家是否已经在某个游戏中
        if (this.getGame(p) != null) {
            p.sendMessage(Messages.ARENA_JOIN_ANOTHER_GAME.toString());
            return;
        }

        final GameJoinEvent e = new GameJoinEvent(p);
        this.main.getServer().getPluginManager().callEvent((Event) e);
        this.data.put(p.getUniqueId(), new PlayerData(this.main, p));
        g.addinQueue(p);
        g.setMoney(p, 800);
        this.main.getVersionInterface().setHandSpeed(p, 100.0);
        final GameTeam.Role role = this.main.getManager().getTeam(g, p);
        final Inventory inv = Bukkit.createInventory((InventoryHolder) null, 54, Messages.ITEM_SHOP_NAME.toString());
        for (final PlayerShop shop : this.main.getShops()) {
            if (shop.getType() == ShopType.GRENADE) {
                final Grenade grenade = this.main.getGrenade(shop.getWeaponName());
                inv.setItem(shop.getSlot(), ItemBuilder.create(grenade.getItem().getType(), 1, grenade.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
            } else if (shop.getType() == ShopType.GUN && (shop.getRole() == null || shop.getRole() == role)) {
                if (this.main.hideVipGuns() && shop.hasPermission() && !p.hasPermission("cs.weapon." + shop.getWeaponName())) {
                    continue;
                }
                final Gun gun = this.main.getGun(shop.getWeaponName());
                inv.setItem(shop.getSlot(), ItemBuilder.create(gun.getItem().getType(), 1, gun.getItem().getData(), shop.getName().replace('&', '§'), shop.getLore()));
            } else {
                if (shop.getRole() != role) {
                    continue;
                }

                inv.setItem(shop.getSlot(), ItemBuilder.create(shop.getMaterial(), 1, shop.getName().replace('&', '§'), shop.getLore()));
            }
        }
        g.getShops().put(p.getUniqueId(), inv);
        final ScoreBoard board = new ScoreBoard(this.main, g, p);
        g.getStatus().put(p.getUniqueId(), board);
        p.playSound(p.getLocation(), SpigotSound.ENDERMAN_TELEPORT.getSound(), 1.0f, 1.0f);
        this.updateSigns(g);
        for (final ScoreBoard status : g.getStatus().values()) {
            if (board != status) {
                status.getTeams().add(g, p);
            }
            this.updateStatus(g, status.getStatus());
        }
        this.updateTitle(g);
        board.showTeams(g);
        board.showHealth(g);
        p.setGameMode(GameMode.SPECTATOR);
        if (g.getBar() != null) {
            g.getBar().addPlayer(p);
        }
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (g.getTeamA().getPlayers().contains(online) || g.getTeamB().getPlayers().contains(online)) {
                online.showPlayer(p);
            } else {
                p.hidePlayer(online);
            }
        }
    }

    public void addPlayer(final Player p, final Game g) {
        if (g == null) {
            p.sendMessage(Messages.ARENA_IS_NULL.toString());
        } else if (this.getGame(p) != null) {
            p.sendMessage(Messages.ARENA_JOIN_ANOTHER_GAME.toString());
        } else if (g.getState() == GameState.DISABLED) {
            p.sendMessage(Messages.ARENA_IS_DISABLED.toString());
        } else if (g.getState() != GameState.WAITING) {
            p.sendMessage(Messages.ARENA_HAS_STARTED.toString());
        } else if (!this.bungee && !this.main.getTextureUsers().contains(p.getUniqueId()) && this.main.canForceTexture()) {
            p.sendMessage(Messages.ARENA_NO_TEXTURE.toString());
        } else if (g.getTeamA().size() + g.getTeamB().size() == g.getMaxPlayers()) {
            p.sendMessage(Messages.ARENA_IS_FULL.toString());
        } else {
            final GameJoinEvent e = new GameJoinEvent(p);
            this.main.getServer().getPluginManager().callEvent((Event) e);
            g.addRandomTeam(p);
            this.main.getVersionInterface().setHandSpeed(p, 100.0);
            p.getInventory().setHeldItemSlot(4);
            this.data.put(p.getUniqueId(), new PlayerData(this.main, p));
            p.teleport(g.getLobby());
            final ScoreBoard board = new ScoreBoard(this.main, g, p);
            g.getStatus().put(p.getUniqueId(), board);
            g.getStats().put(p.getUniqueId(), new PlayerStatus(p.getName(), p.getUniqueId()));
            p.playSound(p.getLocation(), SpigotSound.ENDERMAN_TELEPORT.getSound(), 1.0f, 1.0f);
            p.getInventory().setItem(0, ItemBuilder.create(Material.LEATHER, 1, "&a" + Messages.SELECTOR_NAME + " &8(&e" + Messages.ITEM_RIGHT_CLICK + "&8)", Messages.SELECTOR_LORE.toString()));
            p.getInventory().setItem(8, ItemBuilder.create(Material.RED_BED, 1, Messages.ITEM_LEFTGAME_NAME + " &8(&e" + Messages.ITEM_RIGHT_CLICK + "&8)", Messages.ITEM_LEFTGAME_LORE.toString()));
            p.updateInventory();
            g.broadcast(Messages.PREFIX + Messages.GAME_JOIN.toString().replace("%name%", p.getName()).replace("%size%", String.valueOf(g.getTeamA().size() + g.getTeamB().size())).replace("%maxsize%", String.valueOf(g.getMaxPlayers())));
            if (g.getTeamA().size() + g.getTeamB().size() >= g.getMinPlayers()) {
                g.start();
            }
            this.updateSigns(g);
            this.updateTitle(g);
            if (g.getBar() != null) {
                g.getBar().addPlayer(p);
            }
            for (final ScoreBoard status : g.getStatus().values()) {
                this.updateStatus(g, status.getStatus());
            }
            for (final Player online : Bukkit.getOnlinePlayers()) {
                if (g.getTeamA().getPlayers().contains(online) || g.getTeamB().getPlayers().contains(online)) {
                    online.showPlayer(p);
                } else {
                    p.hidePlayer(online);
                }
            }
        }
    }

    public void removePlayer(final Game g, final Player p, final boolean lobby, final boolean hasquit) {
        g.removeFromQueue(p);
        g.getTeamA().removePlayer(p);
        g.getTeamB().removePlayer(p);
        g.getSpectators().remove(p);
        if (!g.isGameEnding()) {
            for (final Grenade grenade : this.main.getGrenades()) {
                grenade.removePlayer(p);
            }
        }
        final MySQL mysql = this.main.getMySQL();
        if (mysql != null && g.getState() != GameState.WAITING) {
            mysql.addInQueue(g.getStats().get(p.getUniqueId()));
        }
        if (!lobby && g.getState() == GameState.IN_GAME && g.getBomb().getCarrier() == p) {
            final ItemStack is = ItemBuilder.create(Material.TNT, 1, "§e" + Messages.PACK_BOMB + "§a " + Messages.ITEM_BOMB_NAME, false);
            final Item s = p.getWorld().dropItemNaturally(p.getLocation(), is);
            g.getDrops().put(s, 1);
            g.getBomb().setDrop(s);
        }
        if (!lobby && g.getState() != GameState.WAITING && g.getState() != GameState.END && !g.isGameEnding() && (g.getTeamA().size() == 0 || g.getTeamB().size() == 0)) {
            this.stopGame(g, true);
            g.broadcast(Messages.PREFIX + Messages.GAME_NO_PLAYERS.toString());
        }
        if (g.getBar() != null) {
            g.getBar().removePlayer(p);
        }
        if (lobby) {
            if (!this.bungee || !this.main.randomMap()) {
                p.teleport(g.getLobby());
            }
            this.clearPlayer(p);
            final ScoreBoard status = g.getStatus().get(p.getUniqueId());
            status.getStatus().reset();
            this.updateStatus(g, status.getStatus());
            p.getInventory().setItem(0, ItemBuilder.create(Material.LEATHER, 1, "&a" + Messages.SELECTOR_NAME + " &8(&e" + Messages.ITEM_RIGHT_CLICK + "&8)", Messages.SELECTOR_LORE.toString()));
            p.getInventory().setItem(8, ItemBuilder.create(Material.RED_BED, 1, Messages.ITEM_LEFTGAME_NAME + " &8(&e" + Messages.ITEM_RIGHT_CLICK + "&8)", Messages.ITEM_LEFTGAME_LORE.toString()));
        } else {
            this.updateSigns(g);
            final boolean teleport = this.spawn == null;
            g.getStats().remove(p.getUniqueId());
            if (!teleport) {
                p.teleport(this.spawn);
            }
            this.data.remove(p.getUniqueId()).restore(teleport);
            final ScoreBoard board = g.getStatus().remove(p.getUniqueId());
            if (g.getState() != GameState.WAITING) {
                for (final ScoreBoard status2 : g.getStatus().values()) {
                    if (board != status2 && status2.getTeams() != null) {
                        status2.getTeams().remove(g, p);
                    }
                }
            }
            board.remove();
            if (!hasquit) {
                for (final Player online : Bukkit.getOnlinePlayers()) {
                    p.showPlayer(online);
                }
                for (final Player a : g.getTeamA().getPlayers()) {
                    a.hidePlayer(p);
                }
                for (final Player b : g.getTeamB().getPlayers()) {
                    b.hidePlayer(p);
                }
            }
            if (hasquit && g.getState() == GameState.WAITING) {
                g.broadcast(Messages.PREFIX + Messages.GAME_LEAVE.toString().replace("%name%", p.getName()).replace("%size%", String.valueOf(g.getTeamA().size() + g.getTeamB().size())).replace("%maxsize%", String.valueOf(g.getMaxPlayers())));
            }
            this.main.getVersionInterface().setHandSpeed(p, 4.0);
            if (!lobby || !this.main.randomMap()) {
                final GameLeaveEvent e = new GameLeaveEvent(p);
                this.main.getServer().getPluginManager().callEvent((Event) e);
            }
        }
        p.updateInventory();
    }

    public void addGame(final Game g) {
        this.games.add(g);
    }

    public void updateSigns(final Game g) {
        for (final Location l : g.getSigns()) {
            if (l.getBlock().getState() instanceof Sign) {
                final Sign s = (Sign) l.getBlock().getState();
                s.setLine(0, Messages.SIGN_FIRST.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                s.setLine(1, Messages.SIGN_SECOND.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                s.setLine(2, Messages.SIGN_THIRD.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                s.setLine(3, Messages.SIGN_FOURTH.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                s.update();
            }
        }
    }

    public void updateTitle(final Game g) {
        for (final ScoreBoard board : g.getStatus().values()) {
            if (g.getState() == GameState.WAITING) {
                board.getStatus().setTitle(Messages.SCOREBOARD_TITLE.toString());
            } else if (g.getState() == GameState.ROUND || g.getState() == GameState.IN_GAME) {
                final int a = g.getScoreTeamA();
                final int b = g.getScoreTeamB();
                final GameTeam.Role a_role = g.getTeamA().getRole();
                board.getStatus().setTitle("§3" + ((a_role == GameTeam.Role.COUNTERTERRORIST) ? a : b) + " "+ Messages.PACK_COPS + " §7" + Messages.SPLITTER + " "+ Messages.PACK_CRIMS + " " + ((a_role == GameTeam.Role.TERRORIST) ? a : b));
            } else {
                board.getStatus().setTitle(Messages.TITLE_GAME_OVER.toString());
            }
        }
    }

    public void updateStatus(final Game g, final ScoreBoardStatus status) {
        final Player p = status.getPlayer();
        if (g.getState() == GameState.WAITING || g.isGameEnding()) {
            status.updateLine(7, "");
            status.updateLine(6, Messages.SCOREBOARD_LOBBY_NAME + " §a" + g.getName());
            status.updateLine(5, Messages.SCOREBOARD_LOBBY_PLAYERS + " §a" + (g.getTeamA().size() + g.getTeamB().size()) + "/" + g.getMaxPlayers());
            status.updateLine(4, "");
            if (g.isGameStarted()) {
                status.updateLine(3, Messages.SCOREBOARD_LOBBY_GAME_START + " §c" + g.getTimer());
            } else {
                status.updateLine(3, Messages.SCOREBOARD_LOBBY_WAITING.toString());
            }
            status.updateLine(2, "");
            status.updateLine(1, Messages.SCOREBOARD_LOBBY_SERVER.toString());
        } else if (g.getState() != GameState.END || g.getState() != GameState.DISABLED) {
            status.updateLine(15, "");
            if (this.getTeam(g, status.getPlayer()) == GameTeam.Role.COUNTERTERRORIST) {
                if (g.getBomb().isPlanted()) {
                    status.updateLine(14, Messages.PACK_COPS_TARGET_2 + Messages.SCOREBOARD_GAME_OBJECTIVE.toString() + "]");
                    status.updateLine(13, Messages.SCOREBOARD_GAME_DEFUSE.toString());
                } else {
                    status.updateLine(14, Messages.PACK_COPS_TARGET_1 + Messages.SCOREBOARD_GAME_OBJECTIVE.toString() + "]");
                    status.updateLine(13, Messages.SCOREBOARD_GAME_PROTECT.toString());
                }
            } else if (g.getBomb().isPlanted()) {
                status.updateLine(14, Messages.PACK_CRIMS_TARGET_2 + Messages.SCOREBOARD_GAME_OBJECTIVE.toString() + "]");
                status.updateLine(13, Messages.SCOREBOARD_GAME_PROTECT_BOMB.toString());
            } else if (g.getBomb().getCarrier() == p) {
                status.updateLine(14, Messages.PACK_CRIMS_TARGET_1 + Messages.SCOREBOARD_GAME_OBJECTIVE.toString() + "]");
                status.updateLine(13, Messages.SCOREBOARD_GAME_PLANT_BOMB.toString());
            } else {
                status.updateLine(14, Messages.PACK_CRIMS_TARGET_1 + Messages.SCOREBOARD_GAME_OBJECTIVE.toString() + "]");
                status.updateLine(13, Messages.SCOREBOARD_GAME_ESCORT_CARRIER.toString());
            }
            if (g.getState() == GameState.IN_GAME && !g.isRoundEnding()) {
                final int seconds = g.getTimer();
                final String color = g.getBomb().isPlanted() ? ((seconds % 2 == 0) ? "§c" : "") : "";
                status.updateLine(12, color + ((seconds % 3600 / 60 < 10) ? "0" : "") + seconds % 3600 / 60 + ":" + ((seconds % 3600 % 60 < 10) ? "0" : "") + seconds % 3600 % 60);
            } else if (g.isRoundEnding() && !g.getBomb().isPlanted()) {
                status.updateLine(12, "00:00");
            } else {
                final int seconds = g.getBomb().isPlanted() ? g.getBomb().getTimer() : this.main.getRoundTime();
                status.updateLine(12, ((seconds % 3600 / 60 < 10) ? "0" : "") + seconds % 3600 / 60 + ":" + ((seconds % 3600 % 60 < 10) ? "0" : "") + seconds % 3600 % 60);
            }
            status.updateLine(11, "");
            status.updateLine(10, Messages.PACK_STATE + Messages.SCOREBOARD_GAME_STATUS.toString() + "]");
            status.updateLine(9, Messages.SCOREBOARD_GAME_MONEY.toString() + "§6$" + g.getMoney(p));
            final ItemStack helmet = p.getInventory().getHelmet();
            final ItemStack chestplate = p.getInventory().getChestplate();
            status.updateLine(8, Messages.SCOREBOARD_GAME_ARMOR.toString() + ((helmet != null && helmet.getType() != Material.LEATHER_HELMET) ? "§a" + Messages.PACK_HELMET : "§7" + Messages.PACK_HELMET) + ((chestplate != null && chestplate.getType() != Material.LEATHER_CHESTPLATE) ? "§a" + Messages.PACK_CHESTPLATE : "§7" + Messages.PACK_CHESTPLATE));
            final PlayerStatus data = g.getStats().get(p.getUniqueId());
            status.updateLine(7, Messages.SCOREBOARD_GAME_DEATHS.toString() + "§3" + data.getDeaths());
            status.updateLine(6, Messages.SCOREBOARD_GAME_KILLS.toString() + "§3" + data.getKills());
            status.updateLine(5, "");
            status.updateLine(4, Messages.PACK_CRIMS + "§f " + Messages.SCOREBOARD_GAME_ALIVE.toString() + "§4" + this.getAlivePlayers(g, this.getTeam(g, GameTeam.Role.TERRORIST)));
            status.updateLine(3, Messages.PACK_COPS + "§f " + Messages.SCOREBOARD_GAME_ALIVE.toString() + "§3" + this.getAlivePlayers(g, this.getTeam(g, GameTeam.Role.COUNTERTERRORIST)));
            status.updateLine(2, "");
            final String team = (this.getTeam(g, p) == GameTeam.Role.TERRORIST) ? (Messages.PACK_CRIMS + " " + Messages.TEAM_TERRORIST_NAME) : (Messages.PACK_COPS + " " + Messages.TEAM_COUNTERTERRORIST_NAME);
            final String color2 = (this.getTeam(g, p) == GameTeam.Role.TERRORIST) ? "§4" : "§3";
            status.updateLine(1, Messages.TEAM_NAME + " " + (this.getTeam(g, GameTeam.Role.TERRORIST).getPlayers().contains(p) ? (color2 + Messages.TEAM_SECOND + " - " + team) : (color2 + Messages.TEAM_FIRST + " - " + team)));
        }
    }

    public void stopGame(final Game g, final boolean lobby) {
        g.stop();
        g.isGameEnding(true);
        this.endRound(g);
        for (final ScoreBoard board : g.getStatus().values()) {
            board.getStatus().reset();
        }
        final List<Player> players = new ArrayList<>();
        if (lobby) {
            players.addAll(g.getTeamA().getPlayers());
            players.addAll(g.getTeamB().getPlayers());
        }
        if (g.getTeamA().size() > 0) {
            for (int x = 0; x < g.getTeamA().size(); --x, ++x) {
                final Player p = g.getTeamA().getPlayer(x);
                this.removePlayer(g, p, lobby, false);
            }
        }
        if (g.getTeamB().size() > 0) {
            for (int x = 0; x < g.getTeamB().size(); --x, ++x) {
                final Player p = g.getTeamB().getPlayer(x);
                this.removePlayer(g, p, lobby, false);
            }
        }
        if (this.bungee && this.main.randomMap()) {
            ++this.bungee_map;
            if (this.bungee_map >= this.games.size()) {
                this.bungee_map = 0;
            }
            g.setGameTimer(this.main.getLobbyTime());
            g.setState(GameState.WAITING);
            g.isGameEnding(false);
            for (final Player p : players) {
                final Game game = this.main.getManager().getGames().get(this.bungee_map);
                this.addPlayer(p, game);
            }
            players.clear();
            return;
        }
        if (lobby) {
            for (final Player p : players) {
                g.addRandomTeam(p);
            }
            for (final ScoreBoard status : g.getStatus().values()) {
                status.removeHealth();
                status.removeTeam();
                this.updateStatus(g, status.getStatus());
            }
            players.clear();
        }
        if (lobby && g.getTeamA().size() + g.getTeamB().size() >= g.getMinPlayers()) {
            g.start();
        }
        g.setGameTimer(this.main.getLobbyTime());
        g.setState(GameState.WAITING);
        g.isGameEnding(false);
    }

    public void endRound(final Game g) {
        for (final Grenade gr : this.main.getGrenades()) {
            gr.remove(g);
        }
        for (final Item item : g.getDrops().keySet()) {
            item.remove();
        }
        g.getDrops().clear();
        if (g.getState() == GameState.ROUND) {
            g.getSpectators().forEach(p -> p.setGameMode(GameMode.SURVIVAL));
        }
        for (final BlockState b : g.restoreBlocks()) {
            b.update(true);
        }
        for (final PlayerStatus status : g.getStats().values()) {
            status.resetRound();
        }
        g.setRoundWinner(null);
        g.resetDefusers();
        g.getBomb().reset();
        g.getSpectators().clear();
        g.setRoundEnding(false);
    }

    public Game getGame(final int id) {
        for (final Game g : this.games) {
            if (g.getID() == id) {
                return g;
            }
        }
        return null;
    }

    public boolean isBungeeMode() {
        return this.bungee;
    }

    public PlayerData getData(final Player p) {
        return this.data.get(p.getUniqueId());
    }

    public boolean sameTeam(final Game g, final Player first, final Player second) {
        return (g.getTeamA().getPlayers().contains(first) && g.getTeamA().getPlayers().contains(second)) || (g.getTeamB().getPlayers().contains(first) && g.getTeamB().getPlayers().contains(second));
    }

    public boolean isAtSpawn(final Game g, final Player p) {
        if (g.getMain().getManager().getTeam(g, p) == GameTeam.Role.TERRORIST) {
            for (final Location l : g.getTerroristLoc()) {
                if (p.getLocation().distance(l) <= 7.0) {
                    return true;
                }
            }
        } else {
            for (final Location l : g.getCounterTerroristLoc()) {
                if (p.getLocation().distance(l) <= 7.0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isInBombArea(final Game g, final Location l) {
        for (final Location loc : g.getBombLoc()) {
            if (loc.distance(l) <= 4.0) {
                return true;
            }
        }
        return false;
    }

    public GameTeam.Role getTeam(final Game g, final Player p) {
        if (g.getTeamA().getPlayers().contains(p)) {
            return g.getTeamA().getRole();
        }
        return g.getTeamB().getRole();
    }

    public GameTeam getTeam(final Game g, final GameTeam.Role role) {
        if (g.getTeamA().getRole() == role) {
            return g.getTeamA();
        }
        return g.getTeamB();
    }

    public PlayerStatus getTop(final Game g, final GameTeam.Role role) {
        PlayerStatus player_status = null;
        for (final Player p : this.getTeam(g, role).getPlayers()) {
            final PlayerStatus status = g.getStats().get(p.getUniqueId());
            if (player_status == null) {
                player_status = status;
            } else {
                if (status.getRoundKills() <= player_status.getRoundKills()) {
                    continue;
                }
                player_status = status;
            }
        }
        return player_status;
    }

    public int getAlivePlayers(final Game g, final GameTeam team) {
        int i = 0;
        for (final Player p : team.getPlayers()) {
            if (!g.getSpectators().contains(p)) {
                ++i;
            }
        }
        return i;
    }

    public Game getGame(final Player p) {
        for (final Game g : this.games) {
            if (g.getTeamA().getPlayers().contains(p) || g.getTeamB().getPlayers().contains(p)) {
                return g;
            }
        }
        return null;
    }

    public List<Game> getGames() {
        return this.games;
    }

    public List<Location> getQuickJoinSigns() {
        return this.quick_join;
    }

    public void clearPlayer(final Player p) {
        p.setExp(0.0f);
        p.setLevel(0);
        p.setHealth(20.0);
        p.setFireTicks(0);
        p.setFoodLevel(20);
        p.setFlying(false);
        p.setFlySpeed(0.2f);
        p.setWalkSpeed(0.2f);
        p.setFallDistance(0.0f);
        p.setAllowFlight(false);
        p.getInventory().clear();
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().setArmorContents((ItemStack[]) null);
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        for (final PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        p.closeInventory();
    }

    public void resetPlayers(final Game g) {
        for (final Location ts : g.getTerroristLoc()) {
            ts.getChunk().load(true);
        }
        for (final Location cts : g.getCounterTerroristLoc()) {
            cts.getChunk().load(true);
        }
        for (int a = 0; a < this.getTeam(g, GameTeam.Role.COUNTERTERRORIST).size(); ++a) {
            final Player p = this.getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayer(a);
            p.setHealth(20.0);
            p.closeInventory();
            for (final Player TeamA : g.getTeamA().getPlayers()) {
                g.getStatus().get(TeamA.getUniqueId()).getHealth().update(p);
            }
            for (final Player TeamB : g.getTeamB().getPlayers()) {
                g.getStatus().get(TeamB.getUniqueId()).getHealth().update(p);
            }
            p.sendMessage(Messages.LINE_SPLITTER.toString());
            p.sendMessage(Messages.LINE_PREFIX.toString());
            p.sendMessage("");
            final String[] split;
            final String[] message = split = Messages.COP_START_MESSAGE.toString().split("#");
            for (final String m : split) {
                p.sendMessage("➢ " + m);
            }
            p.sendMessage(Messages.LINE_SPLITTER.toString());
            if (g.getRound() == 0) {
                g.setMoney(p, 800);
                p.getInventory().setItem(0, (ItemStack) null);
                p.getInventory().setItem(2, ItemBuilder.create(this.main.getDefaultCopKnife(), 1, "&a" + Messages.ITEM_KNIFE_NAME + " &7" + Messages.PACK_KNIFE, true));
            } else if (g.getRound() == this.main.getRoundToSwitch()) {
                g.setMoney(p, 800);
                p.getInventory().setItem(0, (ItemStack) null);
                p.getInventory().setItem(3, (ItemStack) null);
                p.getInventory().setItem(4, (ItemStack) null);
                p.getInventory().setItem(7, (ItemStack) null);
                p.getInventory().setItem(5, ItemBuilder.create(Material.TRIPWIRE_HOOK, 1, "&a" + Messages.ITEM_SHEAR_NAME + Messages.PACK_SHEAR, false));
                final Gun gun = this.main.getGun(this.main.getCopsDefaultWeapon());
                p.getInventory().setItem(1, ItemBuilder.create(gun.getItem().getType(), gun.getAmount(), gun.getItem().getData(), gun.getItem().getName() + " &7" + gun.getSymbol()));
                p.getInventory().setItem(2, ItemBuilder.create(this.main.getDefaultCopKnife(), 1, "&a" + Messages.ITEM_KNIFE_NAME + " &7" + Messages.PACK_KNIFE, true));
            }
            if (p.getInventory().getItem(1) == null) {
                final Gun gun = this.main.getGun(this.main.getCopsDefaultWeapon());
                p.getInventory().setItem(1, ItemBuilder.create(gun.getItem().getType(), gun.getAmount(), gun.getItem().getData(), gun.getItem().getName() + " &7" + gun.getSymbol()));
            }
            if (p.getInventory().getItem(2) == null) {
                p.getInventory().setItem(2, ItemBuilder.create(this.main.getDefaultCopKnife(), 1, "&a" + Messages.ITEM_KNIFE_NAME + " &7" + Messages.PACK_KNIFE, true));
            }
            if (p.getInventory().getItem(5) == null) {
                p.getInventory().setItem(5, ItemBuilder.create(Material.TRIPWIRE_HOOK, 1, "&a" + Messages.ITEM_SHEAR_NAME + Messages.PACK_SHEAR, false));
            }
            for (int x = 0; x <= 1; ++x) {
                final Gun gun2 = this.main.getGun(p.getInventory().getItem(x));
                if (gun2 != null) {
                    p.setExp(0.0f);
                    gun2.resetPlayer(p);
                    p.getInventory().setItem(x, ItemBuilder.create(gun2.getItem().getType(), gun2.getAmount(), gun2.getItem().getData(), gun2.getItem().getName() + " &7" + gun2.getSymbol()));
                }
            }
            final ScoreBoard board = g.getStatus().get(p.getUniqueId());
            for (final Player on : g.getTeamA().getPlayers()) {
                board.getTeams().update(g, on);
            }
            for (final Player on : g.getTeamB().getPlayers()) {
                board.getTeams().update(g, on);
            }
            this.main.getVersionInterface().sendInvisibility(board.getScoreboard(), g.getTeamA().getPlayers(), g.getSpectators());
            this.main.getVersionInterface().sendInvisibility(board.getScoreboard(), g.getTeamB().getPlayers(), g.getSpectators());
            p.getInventory().setItem(8, ItemBuilder.create(Material.EMERALD, 1, "&a" + Messages.ITEM_SHOP_NAME, false));
            p.teleport((Location) g.getCounterTerroristLoc().get(a));
            p.playSound(p.getEyeLocation(), SpigotSound.NOTE_PLING.getSound(), 2.0f, 2.0f);
            if (g.getRound() == this.main.getRoundToSwitch()) {
                p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.swappingsides", 1.0f, 1.0f);
            }
            if (p.getInventory().getHeldItemSlot() == 2) {
                p.setWalkSpeed(0.25f);
            } else {
                p.setWalkSpeed(0.2f);
            }
            if (p.getInventory().getHelmet() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setHelmet(ItemBuilder.createItem(Material.LEATHER_HELMET, Color.BLUE, this.main.getDefaultHelmetName()));
            }
            if (p.getInventory().getChestplate() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setChestplate(ItemBuilder.createItem(Material.LEATHER_CHESTPLATE, Color.BLUE, this.main.getDefaultChestplateName()));
            }
            if (p.getInventory().getLeggings() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setLeggings(ItemBuilder.createItem(Material.LEATHER_LEGGINGS, Color.BLUE, this.main.getDefaultLeggingName()));
            }
            if (p.getInventory().getBoots() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setBoots(ItemBuilder.createItem(Material.LEATHER_BOOTS, Color.BLUE, this.main.getDefaultBootName()));
            }
        }
        for (int b = 0; b < this.getTeam(g, GameTeam.Role.TERRORIST).size(); ++b) {
            final Player p = this.getTeam(g, GameTeam.Role.TERRORIST).getPlayer(b);
            p.setHealth(20.0);
            p.closeInventory();
            for (final Player TeamA : g.getTeamA().getPlayers()) {
                g.getStatus().get(TeamA.getUniqueId()).getHealth().update(p);
            }
            for (final Player TeamB : g.getTeamB().getPlayers()) {
                g.getStatus().get(TeamB.getUniqueId()).getHealth().update(p);
            }
            if (g.getRound() == 0) {
                g.setMoney(p, 800);
                p.getInventory().setItem(0, (ItemStack) null);
                p.getInventory().setItem(2, ItemBuilder.create(this.main.getDefaultCrimKnife(), 1, "&a" + Messages.ITEM_KNIFE_NAME + " &7" + Messages.PACK_KNIFE, true));
            } else if (g.getRound() == this.main.getRoundToSwitch()) {
                g.setMoney(p, 800);
                p.getInventory().setItem(0, (ItemStack) null);
                p.getInventory().setItem(3, (ItemStack) null);
                p.getInventory().setItem(4, (ItemStack) null);
                p.getInventory().setArmorContents((ItemStack[]) null);
                p.getInventory().setItem(2, ItemBuilder.create(this.main.getDefaultCrimKnife(), 1, "&a" + Messages.ITEM_KNIFE_NAME + " &7" + Messages.PACK_KNIFE, true));
                final Gun gun3 = this.main.getGun(this.main.getCrimsDefaultWeapon());
                p.getInventory().setItem(1, ItemBuilder.create(gun3.getItem().getType(), gun3.getAmount(), gun3.getItem().getData(), gun3.getItem().getName() + " &7" + gun3.getSymbol()));
            }
            p.getInventory().setItem(5, (ItemStack) null);
            if (p.getInventory().getItem(1) == null) {
                final Gun gun3 = this.main.getGun(this.main.getCrimsDefaultWeapon());
                p.getInventory().setItem(1, ItemBuilder.create(gun3.getItem().getType(), gun3.getAmount(), gun3.getItem().getData(), gun3.getItem().getName() + " &7" + gun3.getSymbol()));
            }
            if (p.getInventory().getItem(2) == null) {
                p.getInventory().setItem(2, ItemBuilder.create(this.main.getDefaultCrimKnife(), 1, "&a" + Messages.ITEM_KNIFE_NAME + " &7" + Messages.PACK_KNIFE, true));
            }
            for (int x2 = 0; x2 <= 1; ++x2) {
                final Gun gun = this.main.getGun(p.getInventory().getItem(x2));
                if (gun != null) {
                    p.setExp(0.0f);
                    gun.resetPlayer(p);
                    p.getInventory().setItem(x2, ItemBuilder.create(gun.getItem().getType(), gun.getAmount(), gun.getItem().getData(), gun.getItem().getName() + " &7" + gun.getSymbol()));
                }
            }
            final ScoreBoard board2 = g.getStatus().get(p.getUniqueId());
            for (final Player on2 : g.getTeamA().getPlayers()) {
                board2.getTeams().update(g, on2);
            }
            for (final Player on2 : g.getTeamB().getPlayers()) {
                board2.getTeams().update(g, on2);
            }
            this.main.getVersionInterface().sendInvisibility(board2.getScoreboard(), g.getTeamA().getPlayers(), g.getSpectators());
            this.main.getVersionInterface().sendInvisibility(board2.getScoreboard(), g.getTeamB().getPlayers(), g.getSpectators());
            if (this.main.hasCompass()) {
                p.getInventory().setItem(7, ItemBuilder.create(Material.COMPASS, 1, "&5" + Messages.ITEM_BOMB_LOCATOR, false));
            }
            p.getInventory().setItem(8, ItemBuilder.create(Material.EMERALD, 1, "&a" + Messages.ITEM_SHOP_NAME, false));
            p.teleport((Location) g.getTerroristLoc().get(b));
            p.playSound(p.getEyeLocation(), SpigotSound.NOTE_PLING.getSound(), 2.0f, 2.0f);
            if (g.getRound() == this.main.getRoundToSwitch()) {
                p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.swappingsides", 1.0f, 1.0f);
            }
            if (p.getInventory().getHeldItemSlot() == 2) {
                p.setWalkSpeed(0.25f);
            } else {
                p.setWalkSpeed(0.2f);
            }
            if (p.getInventory().getHelmet() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setHelmet(ItemBuilder.createItem(Material.LEATHER_HELMET, Color.RED, this.main.getDefaultHelmetName()));
            }
            if (p.getInventory().getChestplate() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setChestplate(ItemBuilder.createItem(Material.LEATHER_CHESTPLATE, Color.RED, this.main.getDefaultChestplateName()));
            }
            if (p.getInventory().getLeggings() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setLeggings(ItemBuilder.createItem(Material.LEATHER_LEGGINGS, Color.RED, this.main.getDefaultLeggingName()));
            }
            if (p.getInventory().getBoots() == null || g.getRound() == this.main.getRoundToSwitch()) {
                p.getInventory().setBoots(ItemBuilder.createItem(Material.LEATHER_BOOTS, Color.RED, this.main.getDefaultBootName()));
            }
        }
        final Player carrier = this.getTeam(g, GameTeam.Role.TERRORIST).getPlayer(MathUtils.random().nextInt(this.getTeam(g, GameTeam.Role.TERRORIST).size()));
        g.getBomb().setCarrier(carrier);
        carrier.getInventory().setItem(5, ItemBuilder.create(Material.TNT, 1, "§e" + Messages.PACK_BOMB + "§a " + Messages.ITEM_BOMB_NAME, false));
        for (final Player p2 : this.getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
            p2.setCompassTarget(carrier.getLocation());
            if (carrier.getUniqueId() == p2.getUniqueId()) {
                p2.sendMessage(Messages.LINE_SPLITTER.toString());
                p2.sendMessage(Messages.LINE_PREFIX.toString());
                p2.sendMessage("");
                final String[] split2;
                final String[] message2 = split2 = Messages.CRIMS_START_MESSAGE_HAS_BOMB.toString().split("#");
                for (final String i : split2) {
                    p2.sendMessage("➢ " + i);
                }
                p2.sendMessage(Messages.LINE_SPLITTER.toString());
            } else {
                p2.sendMessage(Messages.LINE_SPLITTER.toString());
                p2.sendMessage(Messages.LINE_PREFIX.toString());
                p2.sendMessage("");
                final String[] split3;
                final String[] message2 = split3 = Messages.CRIMS_START_MESSAGE_NO_BOMB.toString().split("#");
                for (final String i : split3) {
                    p2.sendMessage("➢ " + i);
                }
                p2.sendMessage(Messages.LINE_SPLITTER.toString());
            }
        }
    }

    public boolean damage(final Game g, final Player killer, final Player victim, final double damage, final String symbol) {
        if (damage <= 0.0) {
            return false;
        }
        if (victim.getHealth() <= damage) {
            victim.setHealth(5.0);
            victim.damage(4.0);
            victim.setHealth(20.0);
            victim.closeInventory();
            g.getSpectators().add(victim);
            for (final ItemStack is : victim.getInventory().getContents()) {
                if (is != null) {
                    final Gun gun = this.main.getGun(is);
                    if (gun != null) {
                        final int amount = is.getAmount() - 1;
                        is.setAmount(1);
                        final Item s = victim.getWorld().dropItemNaturally(victim.getLocation(), is);
                        g.getDrops().put(s, amount);
                        victim.getInventory().remove(is);
                    }
                    final Grenade grenade = this.main.getGrenade(is);
                    if (grenade != null) {
                        is.setAmount(1);
                        final Item s = victim.getWorld().dropItemNaturally(victim.getLocation(), is);
                        g.getDrops().put(s, 1);
                        victim.getInventory().remove(is);
                    }
                    if (is.getType() == Material.SHEARS) {
                        final Item s = victim.getWorld().dropItemNaturally(victim.getLocation(), is);
                        g.getDrops().put(s, 1);
                        s.setItemStack(is);
                    }
                    if (is.getType() == Material.TNT) {
                        final Item s = victim.getWorld().dropItemNaturally(victim.getLocation(), is);
                        g.getDrops().put(s, 1);
                        s.setItemStack(is);
                        g.getBomb().setDrop(s);
                        for (final Player t : this.main.getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
                            t.playSound(t.getLocation(), "cs_gamesounds.gamesounds.bombdroppedyourteam", 1.0f, 1.0f);
                        }
                        for (final Player ct : this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                            ct.playSound(ct.getLocation(), "cs_gamesounds.gamesounds.bombdroppedenemyteam", 1.0f, 1.0f);
                        }
                    }
                    if (is.getType() == Material.GOLDEN_APPLE) {
                        final Item s = victim.getWorld().dropItemNaturally(victim.getLocation(), is);
                        g.getDrops().put(s, 1);
                        final ItemMeta im = is.getItemMeta();
                        im.setDisplayName("§e" + Messages.PACK_BOMB + "§a " + Messages.ITEM_BOMB_NAME);
                        im.setCustomModelData(1000);
                        is.setItemMeta(im);
                        is.setType(Material.TNT);
                        s.setItemStack(is);
                        g.getBomb().setDrop(s);
                        for (final Player t2 : this.main.getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
                            t2.playSound(t2.getLocation(), "cs_gamesounds.gamesounds.bombdroppedyourteam", 1.0f, 1.0f);
                        }
                        for (final Player ct2 : this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                            ct2.playSound(ct2.getLocation(), "cs_gamesounds.gamesounds.bombdroppedenemyteam", 1.0f, 1.0f);
                        }
                    }
                }
            }
            if (killer != null) {
                final PlayerStatus killer_stats = g.getStats().get(killer.getUniqueId());
                killer_stats.addKill();
                if (this.main.getKillCommand() != null && !this.main.getKillCommand().equalsIgnoreCase("none")) {
//                    击杀者指令
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), this.main.getKillCommand().replace("%player%", killer.getName()));
                }
            }
            final PlayerStatus victim_stats = g.getStats().get(victim.getUniqueId());
            victim_stats.addDeath();
            //            被击杀者指令
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), this.main.getKilledDeathCommand().replace("%player%", victim.getName()));
            for (final Player p : g.getTeamA().getPlayers()) {
                final ScoreBoard board = g.getStatus().get(p.getUniqueId());
                if (killer != null) {
                    board.getTeams().update(g, killer);
                }
                board.getTeams().update(g, victim);
                this.main.getVersionInterface().sendInvisibility(board.getScoreboard(), g.getTeamA().getPlayers(), g.getSpectators());
                this.main.getVersionInterface().sendInvisibility(board.getScoreboard(), g.getTeamB().getPlayers(), g.getSpectators());
            }
            for (final Player p : g.getTeamB().getPlayers()) {
                final ScoreBoard board = g.getStatus().get(p.getUniqueId());
                if (killer != null) {
                    board.getTeams().update(g, killer);
                }
                board.getTeams().update(g, victim);
                this.main.getVersionInterface().sendInvisibility(board.getScoreboard(), g.getTeamA().getPlayers(), g.getSpectators());
                this.main.getVersionInterface().sendInvisibility(board.getScoreboard(), g.getTeamB().getPlayers(), g.getSpectators());
            }
            if (this.main.corpseSupport()) {
            }
            this.clearPlayer(victim);
            victim.updateInventory();
            victim.setGameMode(GameMode.SPECTATOR);
            if (killer != null) {
                g.setMoney(killer, g.getMoney(killer) + 300);
                final int killer_color = (this.getTeam(g, killer) == GameTeam.Role.TERRORIST) ? 4 : 3;
                final int victim_color = (this.getTeam(g, victim) == GameTeam.Role.TERRORIST) ? 4 : 3;
                g.broadcast("&" + killer_color + killer.getName() + " &f" + symbol + " &" + victim_color + victim.getName());
                this.main.getVersionInterface().sendTitle(victim, 0, 100, 0, Messages.ALREADY_DEAD.toString(), "§" + killer_color + killer.getName() + " §f" + symbol + " §" + victim_color + victim.getName());
            } else {
                final int victim_color2 = (this.getTeam(g, victim) == GameTeam.Role.TERRORIST) ? 4 : 3;
                g.broadcast("&f" + symbol + " &" + victim_color2 + victim.getName());
                this.main.getVersionInterface().sendTitle(victim, 0, 100, 0, Messages.ALREADY_DEAD.toString(), "§f" + symbol + " §" + victim_color2 + victim.getName());
            }
            return true;
        }
        if (victim.getNoDamageTicks() < 1) {
            final double health = victim.getHealth();
            victim.setHealth(5.0);
            victim.damage(4.0);
            victim.setHealth(health);
            victim.setNoDamageTicks(1);
        }
        victim.setHealth(victim.getHealth() - damage);
        for (final Player p2 : g.getTeamA().getPlayers()) {
            g.getStatus().get(p2.getUniqueId()).getHealth().update(victim);
        }
        for (final Player p2 : g.getTeamB().getPlayers()) {
            g.getStatus().get(p2.getUniqueId()).getHealth().update(victim);
        }
        return false;
    }

    public void removeGame(final Game g) {
        for (final Location l : g.getSigns()) {
            if (l.getBlock().getState() instanceof Sign) {
                final Sign s = (Sign) l.getBlock().getState();
                s.setLine(0, "");
                s.setLine(1, "");
                s.setLine(2, "");
                s.setLine(3, "");
                s.update();
                final List<String> keys = this.main.getGameDatabase().getStringList("Signs");
                keys.remove(g.getID() + "," + s.getWorld().getName() + "," + s.getLocation().getBlockX() + "," + s.getLocation().getBlockY() + "," + s.getLocation().getBlockZ());
                this.main.getGameDatabase().set("Signs", keys);
            }
        }
        g.getShops().clear();
        g.getFireworks().clear();
        g.getBombLoc().clear();
        g.getTerroristLoc().clear();
        g.getCounterTerroristLoc().clear();
        this.games.remove(g);
    }
}

package src.counterstrike;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import src.counterstrike.Handler.Game;
import src.counterstrike.Handler.GameSetup;
import src.counterstrike.Handler.GameState;
import src.counterstrike.MySQL.MySQL;
import src.counterstrike.Utils.GameUtils;
import me.yic.xconomy.api.XConomyAPI;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class Commands implements CommandExecutor {
    private Main main;
    private File playerDataFile;
    private FileConfiguration playerData;
    private final Map<UUID, String> equippedMusic = new HashMap<>(); // 存储玩家装备的音乐
    private final Map<String, String> musicPaths = new HashMap<>();
    private final XConomyAPI xConomyAPI;

    public Commands(final Main main) {
        this.main = main;
        this.xConomyAPI = new XConomyAPI(); // 使用单例实例
        musicPaths.put("hualian", "cs_music.music.hualian");
        musicPaths.put("ez4ence", "cs_music.music.ez4ence");
        musicPaths.put("chongjixing", "cs_music.music.chongjixing");
        musicPaths.put("shanguangwu", "cs_music.music.shanguangwu");
        musicPaths.put("tuzidong", "cs_music.music.tuzidong");
        musicPaths.put("feirenlei", "cs_music.music.feirenlei");
        loadPlayerData(); // 初始化 playerdata.yml 并加载已装备的音乐
    }

    // 根据 MySQL 是否启用加载数据
    private void loadPlayerData() {
        MySQL mysql = main.getMySQL();
        if (mysql != null) {
            // MySQL 启用，从数据库加载
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                String music = mysql.getEquippedMusic(uuid, "");
                if (music != null) {
                    equippedMusic.put(uuid, music);
                }
            }
        } else {
            // 使用 playerdata.yml
            playerDataFile = new File(main.getDataFolder(), "playerdata.yml");
            if (!playerDataFile.exists()) {
                try {
                    playerDataFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            playerData = YamlConfiguration.loadConfiguration(playerDataFile);
            equippedMusic.clear();
            for (String uuid : playerData.getKeys(false)) {
                String equippedMusicPath = playerData.getString(uuid + ".equippedMusic");
                if (equippedMusicPath != null) {
                    equippedMusic.put(UUID.fromString(uuid), equippedMusicPath);
                }
            }
        }
    }

    // 保存数据，根据 MySQL 是否启用选择方式
    private void savePlayerData() {
        MySQL mysql = main.getMySQL();
        if (mysql == null) {
            // 保存到 playerdata.yml
            try {
                playerData.save(playerDataFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 如果 MySQL 启用，数据通过 setEquippedMusic/addMusicOwned 保存
    }

    public boolean onCommand(final CommandSender sender, final Command command, final String s, final String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Messages.PREFIX + " §7插件制作 §aCoquettishpigs");
            sender.sendMessage(Messages.PREFIX + " §c/cs help§7 帮助指令.");
            return true;
        }

        MySQL mysql = main.getMySQL();

        // 支持控制台执行的命令
        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("cs.admin")) {
            this.main.getServer().getPluginManager().disablePlugin((Plugin) this.main);
            this.main.getServer().getPluginManager().enablePlugin((Plugin) this.main);
            loadPlayerData();
            sender.sendMessage(Messages.PREFIX + " §a成功重载!");
            return true;
        }

        if (args[0].equalsIgnoreCase("givemusic") && args.length == 3 && sender.hasPermission("cs.admin")) {
            String targetPlayerName = args[1];
            String musicName = args[2].toLowerCase();

            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
            if (targetPlayer == null) {
                sender.sendMessage(Messages.PREFIX + " §c玩家 " + targetPlayerName + " 不在线!");
                return true;
            }

            UUID targetUUID = targetPlayer.getUniqueId();

            if (!musicPaths.containsKey(musicName)) {
                sender.sendMessage(Messages.PREFIX + " §c未知的音乐名称! 可用的音乐: " + String.join(", ", musicPaths.keySet()));
                return true;
            }

            String musicPath = musicPaths.get(musicName);
            String displayName = getMusicDisplayName(musicPath);

            // 检查是否已拥有音乐
            boolean alreadyOwned;
            if (mysql != null) {
                alreadyOwned = mysql.isMusicOwned(targetUUID, musicName);
            } else {
                String ownedMusic = playerData.getString(targetUUID.toString() + ".ownedMusic", "");
                List<String> ownedList = ownedMusic.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(ownedMusic.split(",")));
                alreadyOwned = ownedList.contains(musicName);
            }

            if (alreadyOwned) {
                sender.sendMessage(Messages.PREFIX + " §c玩家 " + targetPlayerName + " 已拥有 " + displayName + " 音乐盒!");
                return true;
            }

            // 检查目标玩家的余额（XConomy 2.26.3）
            BigDecimal cost = new BigDecimal("5000");
            BigDecimal balance = xConomyAPI.getPlayerData(targetUUID).getBalance(); // 获取余额
            if (balance.compareTo(cost) < 0) {
                sender.sendMessage(Messages.PREFIX + " §c玩家 " + targetPlayerName + " 的余额不足5000，无法购买 " + displayName + " 音乐盒!");
                targetPlayer.sendMessage(Messages.PREFIX + " §c你的余额不足5000，无法获得 " + displayName + " 音乐盒!");
                return true;
            }

            // 扣除金钱并给予音乐
            try {
                xConomyAPI.changePlayerBalance(
                        targetUUID,
                        targetPlayer.getName(), // 需要提供玩家名
                        cost,
                        false, // false 表示扣款
                        "购买音乐盒: " + displayName
                );
                if (mysql != null) {
                    mysql.addMusicOwned(targetUUID, musicName);
                } else {
                    String ownedMusic = playerData.getString(targetUUID.toString() + ".ownedMusic", "");
                    List<String> ownedList = ownedMusic.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(ownedMusic.split(",")));
                    ownedList.add(musicName);
                    playerData.set(targetUUID.toString() + ".ownedMusic", String.join(",", ownedList));
                    savePlayerData();
                }
                sender.sendMessage(Messages.PREFIX + " §a已为 " + targetPlayerName + " 给予音乐盒: " + displayName + "，已扣除5000金币!");
                targetPlayer.sendMessage(Messages.PREFIX + " §a你花费5000金币获得了一个 " + displayName + " 音乐盒!");
            } catch (Exception e) {
                sender.sendMessage(Messages.PREFIX + " §c扣除 " + targetPlayerName + " 的金钱失败: " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        // 以下命令需要玩家身份
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.PREFIX + " §c此命令只能由玩家执行!");
            return true;
        }

        UUID playerUUID = player.getUniqueId();

        if (sender.hasPermission("cs.admin")) {
            if (args[0].equalsIgnoreCase("enable")) {
                if (args.length != 2) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs enable <id>");
                } else {
                    try {
                        final int intValue = Integer.valueOf(args[1]);
                        final Game game = this.main.getManager().getGame(intValue);
                        if (game != null) {
                            if (game.getState() != GameState.DISABLED) {
                                sender.sendMessage("§c游戏已经开始了!");
                            } else {
                                game.setState(GameState.WAITING);
                                sender.sendMessage(Messages.PREFIX + " §7游戏 §c" + intValue + "§7 已启用!");
                            }
                        } else {
                            sender.sendMessage("§c该游戏不存在!");
                        }
                    } catch (NumberFormatException ex) {
                        sender.sendMessage("§c必须是一个数字!");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("disable")) {
                if (args.length != 2) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs disable <id>");
                } else {
                    try {
                        final int intValue2 = Integer.valueOf(args[1]);
                        final Game game2 = this.main.getManager().getGame(intValue2);
                        if (game2 != null) {
                            if (game2.getState() == GameState.DISABLED) {
                                sender.sendMessage("§c游戏已经结束了!");
                            } else {
                                game2.setState(GameState.DISABLED);
                                sender.sendMessage(Messages.PREFIX + " §7游戏 §c" + intValue2 + "§7 已停用!");
                            }
                        } else {
                            sender.sendMessage("§c该游戏不存在!");
                        }
                    } catch (NumberFormatException ex2) {
                        sender.sendMessage("§c必须是一个数字!");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("create")) {
                if (args.length != 5) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs create <id> <游戏名> <最小玩家> <最大玩家>");
                } else {
                    try {
                        final int intValue3 = Integer.valueOf(args[1]);
                        final int intValue4 = Integer.valueOf(args[3]);
                        final int intValue5 = Integer.valueOf(args[4]);
                        if (this.main.getManager().getGame(intValue3) == null) {
                            if (this.main.getSetup(player) == null) {
                                if (intValue5 >= 2) {
                                    if (intValue5 % 2 == 0) {
                                        this.main.addSetup(player, new GameSetup(intValue3, args[2], intValue4, intValue5));
                                        player.sendMessage(Messages.PREFIX + " §7游戏创建成功. 请先设置等待大厅 §a/cs setlobby!");
                                    } else {
                                        player.sendMessage("§c两队的最大人数应该相等!");
                                    }
                                } else {
                                    player.sendMessage("§c玩家最少应该有2名!");
                                }
                            } else {
                                player.sendMessage(Messages.PREFIX + " §a游戏设置完成!");
                            }
                        } else {
                            player.sendMessage("§c游戏已经存在!");
                        }
                    } catch (NumberFormatException ex3) {
                        sender.sendMessage("§c必须是一个数字!");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("setlobby")) {
                if (args.length != 1) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs setlobby");
                } else {
                    final GameSetup setup = this.main.getSetup(player);
                    if (setup != null) {
                        setup.setLobby(player.getLocation().add(0.0, 1.0, 0.0).clone());
                        player.sendMessage(Messages.PREFIX + " §7等待大厅已成功创建, 请使用 §a/cs addcop!");
                    } else {
                        player.sendMessage("§c你没有任何设置! 使用 /cs create <id> <游戏名> <最小玩家> <最大玩家>");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("addcop")) {
                if (args.length != 1) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs addcop");
                } else {
                    final GameSetup setup2 = this.main.getSetup(player);
                    if (setup2 != null) {
                        final int n = setup2.getMax() / 2;
                        if (setup2.getCops().size() == n) {
                            player.sendMessage(Messages.PREFIX + " §c你不能设置更多的警方出生点 请使用 §a/cs addcrim");
                        } else {
                            setup2.getCops().add(player.getLocation().add(0.0, 1.0, 0.0).clone());
                            if (setup2.getCops().size() == n) {
                                player.sendMessage(Messages.PREFIX + " §7你设置了所有警方的出生点 请使用 §a/cs addcrim");
                            } else {
                                player.sendMessage(Messages.PREFIX + " §7出生点数量 §e" + setup2.getCops().size() + " §7在游戏中添加 §a" + setup2.getID() + " §8(§d" + setup2.getCops().size() + "/" + n + "§8)");
                            }
                        }
                    } else {
                        player.sendMessage("§c你没有任何设置! 使用 /cs create <id> <游戏名> <最小玩家> <最大玩家>");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("addcrim")) {
                if (args.length != 1) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs addcrim");
                } else {
                    final GameSetup setup3 = this.main.getSetup(player);
                    if (setup3 != null) {
                        final int n2 = setup3.getMax() / 2;
                        if (setup3.getCriminals().size() == n2) {
                            player.sendMessage(Messages.PREFIX + " §c你不能设置更多的匪方出生点 请使用 §a/cs addbombsite");
                        } else {
                            setup3.getCriminals().add(player.getLocation().add(0.0, 1.0, 0.0).clone());
                            if (setup3.getCriminals().size() == n2) {
                                player.sendMessage(Messages.PREFIX + " §7你设置了所有匪方的出生点 请使用 §a/cs addbombsite");
                            } else {
                                player.sendMessage(Messages.PREFIX + " §7出生点数量 §e" + setup3.getCriminals().size() + " §7在游戏中添加 §a" + setup3.getID() + " §8(§d" + setup3.getCriminals().size() + "/" + n2 + "§8)");
                            }
                        }
                    } else {
                        player.sendMessage("§c你没有任何设置! 使用 /cs create <id> <游戏名> <最小玩家> <最大玩家>");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("addbombsite")) {
                if (args.length != 1) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs addbombsite");
                } else {
                    final GameSetup setup4 = this.main.getSetup(player);
                    if (setup4 != null) {
                        setup4.getBombs().add(player.getLocation().clone());
                        if (setup4.getBombs().size() == 2) {
                            player.sendMessage(Messages.PREFIX + " §7你设置了2个可以下包的位置, 你可以设置更多的位置 §a/cs addfireworks");
                        } else {
                            player.sendMessage(Messages.PREFIX + " §7下包点 §a" + setup4.getBombs().size() + " §7设置成功!");
                        }
                    } else {
                        player.sendMessage("§c你没有任何设置! 使用 /cs create <id> <游戏名> <最小玩家> <最大玩家>");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("addfireworks")) {
                if (args.length != 1) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs addfireworks");
                } else {
                    final GameSetup setup5 = this.main.getSetup(player);
                    if (setup5 != null) {
                        setup5.getFireworks().add(player.getLocation().clone());
                        if (setup5.getFireworks().size() == 10) {
                            player.sendMessage(Messages.PREFIX + " §7已经设置了十个烟花地点。你可以设置更多的位置 §a/cs finish");
                        } else {
                            player.sendMessage(Messages.PREFIX + " §7位置 §a §8(§d" + setup5.getFireworks().size() + "/10§8) §7已添加!");
                        }
                    } else {
                        player.sendMessage("§c你没有任何设置! 使用 /cs create <id> <游戏名> <最小玩家> <最大玩家>");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("setspawn")) {
                final Location add = player.getLocation().clone().add(0.0, 1.0, 0.0);
                this.main.getManager().setSpawn(add);
                this.main.getGameDatabase().set("GameLobby", (Object) GameUtils.getSerializedLocation(add));
                this.main.saveGameDatabase();
                sender.sendMessage(Messages.PREFIX + " §a出生点设置成功!");
                return true;
            }

            if (args[0].equalsIgnoreCase("finish")) {
                if (args.length != 1) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs finish");
                } else {
                    final GameSetup setup6 = this.main.getSetup(player);
                    if (setup6 != null) {
                        final int n3 = setup6.getMax() / 2;
                        if (setup6.getCops().size() != n3) {
                            player.sendMessage(Messages.PREFIX + " §7你必须添加更多的警方出生点 请使用 §a/cs addcop");
                        } else if (setup6.getCriminals().size() != n3) {
                            player.sendMessage(Messages.PREFIX + " §7你必须添加更多的匪方出生点 请使用 §a/cs addcrim");
                        } else if (setup6.getLobby() == null) {
                            player.sendMessage("§c未设置等待大厅! 使用 §a/cs setlobby");
                        } else if (this.main.getManager().getGame(setup6.getID()) != null) {
                            player.sendMessage("§c游戏已经同时创建!");
                        } else if (setup6.getFireworks().isEmpty()) {
                            player.sendMessage(Messages.PREFIX + " §7你必须添加至少1个烟花地点. 请使用 §a/cs addfireworks");
                        } else {
                            this.main.getManager().addGame(new Game(this.main, setup6.getID(), setup6.getLobby(), setup6.getName(), setup6.getMin(), setup6.getCops(), setup6.getCriminals(), setup6.getBombs(), setup6.getFireworks()));
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".Min", (Object) setup6.getMin());
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".Name", (Object) setup6.getName());
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".Lobby", (Object) GameUtils.getSerializedLocation(setup6.getLobby()));
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".CopSpawns", (Object) GameUtils.getSerializedLocations(setup6.getCops()));
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".CriminalSpawns", (Object) GameUtils.getSerializedLocations(setup6.getCriminals()));
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".BombSites", (Object) GameUtils.getSerializedLocations(setup6.getBombs()));
                            this.main.getGameDatabase().set("Game." + setup6.getID() + ".Fireworks", (Object) GameUtils.getSerializedLocations(setup6.getFireworks()));
                            this.main.saveGameDatabase();
                            this.main.removeSetup(player);
                            player.sendMessage(Messages.PREFIX + " §a游戏创建成功!");
                        }
                    } else {
                        player.sendMessage("§c你没有任何设置! 使用 /cs create <id> <游戏名> <最小玩家> <最大玩家>");
                    }
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length != 2) {
                    sender.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs delete <id>");
                } else {
                    try {
                        final int int1 = Integer.parseInt(args[1]);
                        final Game game3 = this.main.getManager().getGame(int1);
                        if (game3 != null) {
                            this.main.getManager().stopGame(game3, false);
                            this.main.getManager().removeGame(game3);
                            player.sendMessage(Messages.PREFIX + " §7游戏成功删除.");
                            this.main.getGameDatabase().set("Game." + int1, (Object) null);
                            this.main.saveGameDatabase();
                        } else {
                            player.sendMessage("§c该游戏不存在!");
                        }
                    } catch (NumberFormatException ex4) {
                        player.sendMessage("§c必须是一个数字!");
                    }
                }
                return true;
            }
        }

        // 普通玩家命令
        if (args[0].equalsIgnoreCase("setlobby")) {
            if (args.length != 1) {
                player.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs setlobby");
            }
        } else if (args[0].equalsIgnoreCase("join")) {
            if (args.length != 2) {
                player.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs join <id>");
            } else {
                try {
                    this.main.getManager().addPlayer(player, this.main.getManager().getGame(Integer.parseInt(args[1])));
                } catch (NumberFormatException ex5) {
                    player.sendMessage("§c必须是一个数字!");
                }
            }
        } else if (args[0].equalsIgnoreCase("equipmusic") && args.length == 2) {
            String musicName = args[1].toLowerCase();

            if (!musicPaths.containsKey(musicName)) {
                player.sendMessage(Messages.PREFIX + " §c未知的音乐名称! 可用的音乐: " + String.join(", ", musicPaths.keySet()));
                return true;
            }

            String musicPath = musicPaths.get(musicName);

            if (mysql != null) {
                // MySQL 启用
                if (!mysql.isMusicOwned(playerUUID, musicName)) {
                    player.sendMessage(Messages.PREFIX + " §c你尚未拥有 " + getMusicDisplayName(musicPath) + " 音乐盒! 联系管理员使用 /cs givemusic");
                    return true;
                }

                if (equippedMusic.containsKey(playerUUID) && equippedMusic.get(playerUUID).equals(musicPath)) {
                    // 取消装备
                    equippedMusic.remove(playerUUID);
                    mysql.setEquippedMusic(playerUUID, null);
                    player.sendMessage(Messages.PREFIX + " §c你已取消装备音乐: " + getMusicDisplayName(musicPath));
                } else {
                    // 装备音乐
                    equippedMusic.put(playerUUID, musicPath);
                    mysql.setEquippedMusic(playerUUID, musicPath);
                    player.sendMessage(Messages.PREFIX + " §a你已装备音乐: " + getMusicDisplayName(musicPath));
                }
            } else {
                // 使用 playerdata.yml
                String ownedMusic = playerData.getString(playerUUID.toString() + ".ownedMusic", "");
                List<String> ownedList = ownedMusic.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(ownedMusic.split(",")));
                if (!ownedList.contains(musicName)) {
                    player.sendMessage(Messages.PREFIX + " §c你尚未拥有 " + getMusicDisplayName(musicPath) + " 音乐盒! 联系管理员使用 /cs givemusic");
                    return true;
                }

                if (equippedMusic.containsKey(playerUUID) && equippedMusic.get(playerUUID).equals(musicPath)) {
                    // 取消装备
                    equippedMusic.remove(playerUUID);
                    playerData.set(playerUUID.toString() + ".equippedMusic", null);
                    savePlayerData();
                    player.sendMessage(Messages.PREFIX + " §c你已取消装备音乐: " + getMusicDisplayName(musicPath));
                } else {
                    // 装备音乐
                    equippedMusic.put(playerUUID, musicPath);
                    playerData.set(playerUUID.toString() + ".equippedMusic", musicPath);
                    savePlayerData();
                    player.sendMessage(Messages.PREFIX + " §a你已装备音乐: " + getMusicDisplayName(musicPath));
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("quickjoin")) {
            if (args.length != 1) {
                player.sendMessage(Messages.PREFIX + " §7无效参数! 使用 §a/cs quickjoin");
            } else {
                final Game game4 = this.main.getManager().findGame(player);
                if (game4 != null) {
                    if (game4.getState() == GameState.WAITING) {
                        this.main.getManager().addPlayer(player, game4);
                    } else if (game4.getState() == GameState.IN_GAME || game4.getState() == GameState.ROUND) {
                        this.main.getManager().addQuickPlayer(game4, player);
                    }
                }
            }
        } else if (args[0].equalsIgnoreCase("leave")) {
            final Game game5 = this.main.getManager().getGame(player);
            if (game5 == null) {
                player.sendMessage("§c你必须在游戏中才能使用这个命令!");
            } else {
                player.sendMessage(Messages.PREFIX + Messages.GAME_LEFT.toString());
                this.main.getManager().removePlayer(game5, player, false, false);
            }
        } else if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Messages.PREFIX + " §7- §dHelp");
            player.sendMessage("- §8/§cCounterStrike join §8<§aid§8>");
            player.sendMessage("- §8/§cCounterStrike quickjoin");
            player.sendMessage("- §8/§cCounterStrike leave");
            player.sendMessage("- §8/§cCounterStrike equipmusic <music_name> §8- §7装备/取消装备音乐");
            if (player.hasPermission("cs.admin")) {
                player.sendMessage("- §8/§cCounterStrike reload");
                player.sendMessage("- §8/§cCounterStrike setspawn");
                player.sendMessage("- §8/§cCounterStrike delete §8<§aid§8>");
                player.sendMessage("- §8/§cCounterStrike enable §8<§aid§8>");
                player.sendMessage("- §8/§cCounterStrike disable §8<§aid§8>");
                player.sendMessage("- §8/§cCounterStrike create §8<§aid§8> §8<§aname§8> §8<§amin§8> §8<§amax§8>");
                player.sendMessage("- §8/§cCounterStrike givemusic §8<§aid§8> <music_name> §8- §7给予虚拟音乐盒并装备音乐");
            }
        } else {
            sender.sendMessage("§e未知指令, 使用 /CounterStrike 获取帮助");
        }

        return false;
    }

    // 获取玩家装备的音乐，根据 MySQL 或 YAML
    public String getCrimEquippedMusic(Player player) {
        UUID uuid = player.getUniqueId();
        MySQL mysql = main.getMySQL();
        if (mysql != null) {
            return mysql.getEquippedMusic(uuid, "cs_gamesounds.gamesounds.copswinthegame");
        } else {
            loadPlayerData();
            return equippedMusic.getOrDefault(uuid, "cs_gamesounds.gamesounds.copswinthegame");
        }
    }

    public String getCopEquippedMusic(Player player) {
        UUID uuid = player.getUniqueId();
        MySQL mysql = main.getMySQL();
        if (mysql != null) {
            return mysql.getEquippedMusic(uuid, "cs_gamesounds.gamesounds.criminalswinthegame");
        } else {
            loadPlayerData();
            return equippedMusic.getOrDefault(uuid, "cs_gamesounds.gamesounds.criminalswinthegame");
        }
    }

    // 获取音乐的显示名称
    public String getMusicDisplayName(String musicPath) {
        String musicKey = musicPaths.entrySet().stream()
                .filter(entry -> entry.getValue().equals(musicPath))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (musicKey == null) {
            return "未知音乐";
        }

        String enumName = "MUSIC_" + musicKey.toUpperCase();
        Messages message = Messages.getEnum(enumName);

        return message != null ? message.toString() : "未知音乐";
    }
}
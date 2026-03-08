package src.counterstrike;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import src.counterstrike.Cache.PlayerShop;
import src.counterstrike.Grenades.Grenade;
import src.counterstrike.Grenades.GrenadeType;
import src.counterstrike.Guns.Gun;
import src.counterstrike.Guns.GunType;
import src.counterstrike.Handler.*;
import src.counterstrike.Hooks.PlaceholderAPIHook;
import src.counterstrike.MySQL.MySQL;
import src.counterstrike.Utils.GameUtils;
import src.counterstrike.Utils.Item;
import src.counterstrike.Utils.PlayerHider;
import src.counterstrike.Version.VersionInterface;
import src.counterstrike.Version.v1_20_R3.v1_20_R3;


import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin
{
    private MySQL mysql;
    private static Main main;
    private UpdateTask update;
    private int lobby_time;
    private int round_time;
    private int bomb_time;
    private int round_to_win;
    private double hitAddition;
    private boolean blood;
    private boolean boss;
    private boolean autojoin;
    private boolean shutdown;
    private String bungee_hub;
    private boolean bungee;
    private int maxRadius;
    private boolean canjoinstartedgame;
    private int round_to_switch;
    private int round_win_money;
    private int bomb_plant_money;
    private boolean compass;
    private boolean force_texture;
    private GameManager manager;
    private boolean hide_vip_guns;
    private boolean bungee_random;
    private YamlConfiguration gun;
    private YamlConfiguration grenade;
    private GameListener listener;
    private YamlConfiguration shop;
    private VersionInterface version;
    private FileConfiguration config;
    private YamlConfiguration database;
    private YamlConfiguration messages;
    private String copsdefaultweapon;
    private String crimsdefaultweapon;
    private List<Gun> guns;
    private boolean ReplaceGunsWithoutDrop;
    private Material defaultCrimKnife;
    private Material defaultCopKnife;
    private String defaulthelmetname;
    private String defaultchestplatename;
    private String defaultleggingname;
    private String defaultbootname;
    private List<UUID> TextureUsers;
    private List<String> whitelist;
    private String end_command_win;
    private List<Grenade> grenades;
    private String end_command_lose;
    private String kill_win_command;
    private String killed_death_command;
    private boolean corpse;
    private boolean papi;
    private List<PlayerShop> shop_items;
    private HashMap<UUID, GameSetup> setup;
    private PlayerHider playerHider;
    private Commands commandExecutor;

    public Main() {
        this.lobby_time = 10;
        this.round_time = 120;
        this.bomb_time = 45;
        this.round_to_win = 8;
        this.hitAddition = 0.0;
        this.blood = false;
        this.boss = true;
        this.autojoin = true;
        this.shutdown = false;
        this.maxRadius = 100;
        this.canjoinstartedgame = false;
        this.round_to_switch = 6;
        this.round_win_money = 3250;
        this.bomb_plant_money = 300;
        this.compass = true;
        this.force_texture = true;
        this.manager = null;
        this.hide_vip_guns = false;
        this.bungee_random = false;
        this.gun = null;
        this.grenade = null;
        this.listener = null;
        this.shop = null;
        this.version = null;
        this.config = null;
        this.database = null;
        this.messages = null;
        this.copsdefaultweapon = "P250";
        this.crimsdefaultweapon = "P250";
        this.guns = new ArrayList<Gun>();
        this.ReplaceGunsWithoutDrop = false;
        this.defaultCrimKnife = Material.IRON_AXE;
        this.defaultCopKnife = Material.STONE_AXE;
        this.defaulthelmetname = "&aKevlar Helmet";
        this.defaultchestplatename = "&aKevlar Vest";
        this.defaultleggingname = "&aKevlar Legging";
        this.defaultbootname = "&aKevlar Boot";
        this.TextureUsers = new ArrayList<>();
        this.whitelist = new ArrayList<>();
        this.end_command_win = "money give %player% 30";
        this.grenades = new ArrayList<>();
        this.end_command_lose = "money give %player% 20";
        this.kill_win_command = "money give %player% 3";
        this.corpse = false;
        this.papi = false;
        this.shop_items = new ArrayList<>();
        this.setup = new HashMap<>();
        this.commandExecutor = new Commands(this); // 在构造函数中初始化
    }

    public void onEnable() {
        Main.main = this;
        this.getDataFolder().mkdirs();
        final ConsoleCommandSender console = this.getServer().getConsoleSender();
        final String serverVersion = GameUtils.getServerVersion();
        this.getCommand("counterstrike").setExecutor(this.commandExecutor);
//        console.sendMessage(serverVersion);
        switch (serverVersion) {
            case "v1_20_R2":
            case "v1_20_R3": {
                this.version = new v1_20_R3();
                break;
            }
        }
        if (this.version == null) {
            console.sendMessage("§cCounterStrike 只能在1.20.4版本运行");
            try {
                Thread.sleep(2000L);
            }
            catch (InterruptedException ex) {}
            this.setEnabled(false);
            return;
        }
        console.sendMessage("§b _____                   _            _____ _        _ _        ");
        console.sendMessage("§b/  __ \\                 | |          /  ___| |      (_) |       ");
        console.sendMessage("§b| /  \\/ ___  _   _ _ __ | |_ ___ _ __\\ `--.| |_ _ __ _| | _____ ");
        console.sendMessage("§b| |    / _ \\| | | | '_ \\| __/ _ \\ '__|`--. \\ __| '__| | |/ / _ \\");
        console.sendMessage("§b| \\__/\\ (_) | |_| | | | | ||  __/ |  /\\__/ / |_| |  | |   <  __/");
        console.sendMessage("§b \\____/\\___/ \\__,_|_| |_|\\__\\___|_|  \\____/ \\__|_|  |_|_|\\_\\___|");
        console.sendMessage("§b - 版本: " + this.getDescription().getVersion());
        console.sendMessage("§b - 作者: Coquettishpigs");
        final Plugin mcm = this.getServer().getPluginManager().getPlugin("CorpseReborn");
        if (mcm != null && mcm.isEnabled()) {
            this.corpse = true;
            console.sendMessage("§e - Corpse Reborn依赖已加载!");
        }
        final Plugin api = this.getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if (api != null && api.isEnabled()) {
            this.papi = true;
            console.sendMessage("§e - PlaceholderAPI依赖已加载!");
        }
        final File file_config = new File(this.getDataFolder(), "config.yml");
        if (file_config.exists()) {
            console.sendMessage("§b - 正在加载 config.yml...");
        }
        else {
            console.sendMessage("§a - 正在创建 config.yml");
        }
        this.loadConfig();
        if (GameUtils.getServerVersion().equals("v1_7_R4")) {
            this.force_texture = false;
        }
        final File m = new File(this.getDataFolder(), "messages.yml");
        if (!m.exists()) {
            try {
                console.sendMessage("§a - 正在创建 messages.yml");
                m.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            console.sendMessage("§b - 正在加载 messages.yml...");
        }
        this.messages = YamlConfiguration.loadConfiguration(m);
        for (final Messages msg : Messages.values()) {
            if (this.messages.getString("Messages." + msg.name()) == null) {
                this.messages.set("Messages." + msg.name(), (Object)msg.toString().replace("§", "&"));
                try {
                    this.messages.save(m);
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        for (final String name : this.messages.getConfigurationSection("Messages").getKeys(false)) {
            final Messages msg2 = Messages.getEnum(name);
            if (msg2 != null) {
                msg2.setMessage(this.messages.getString("Messages." + name));
            }
            else {
                this.messages.set("Messages." + name, (Object)null);
                try {
                    this.messages.save(m);
                }
                catch (Exception e3) {
                    e3.printStackTrace();
                }
            }
        }
        this.update = new UpdateTask(this);
        this.manager = new GameManager(this, this.bungee);
        final File file_database = new File(this.getDataFolder(), "database.yml");
        if (file_database.exists()) {
            console.sendMessage("§b - 正在加载 database.yml...");
            this.database = YamlConfiguration.loadConfiguration(file_database);
        }
        else {
            console.sendMessage("§a - 正在创建 database.yml");
            this.saveGameDatabase();
        }
        this.manager.setSpawn(GameUtils.getDeserializedLocation(this.database.getString("GameLobby")));
        if (this.database.getString("Game") != null && !this.getGameDatabase().isString("Game")) {
            for (final String ID : this.database.getConfigurationSection("Game").getKeys(false)) {
                try {
                    final int Min = this.database.getInt("Game." + ID + ".Min");
                    final String Name = this.database.getString("Game." + ID + ".Name");
                    final Location Lobby = GameUtils.getDeserializedLocation(Main.main.getGameDatabase().getString("Game." + ID + ".Lobby"));
                    final List<Location> CopSpawns = GameUtils.getDeserializedLocations(Main.main.getGameDatabase().getStringList("Game." + ID + ".CopSpawns"));
                    final List<Location> CriminalSpawns = GameUtils.getDeserializedLocations(Main.main.getGameDatabase().getStringList("Game." + ID + ".CriminalSpawns"));
                    final List<Location> BombSpawns = GameUtils.getDeserializedLocations(Main.main.getGameDatabase().getStringList("Game." + ID + ".BombSites"));
                    final List<Location> Fireworks = GameUtils.getDeserializedLocations(Main.main.getGameDatabase().getStringList("Game." + ID + ".Fireworks"));
                    this.manager.addGame(new Game(Main.main, Integer.parseInt(ID), Lobby, Name, Min, CopSpawns, CriminalSpawns, BombSpawns, Fireworks));
                }
                catch (Exception e3) {
                    console.sendMessage("§c - 加载游戏时出现错误，ID: " + ID);
                }
            }
        }
        final Location l = new Location(null, 0.0, 0.0, 0.0);
        for (final String sign : this.database.getStringList("Signs")) {
            final String[] split = sign.split(",");
            final int id = Integer.valueOf(split[0]);
            final String world = split[1];
            final int x = Integer.valueOf(split[2]);
            final int y = Integer.valueOf(split[3]);
            final int z = Integer.valueOf(split[4]);
            l.setWorld(Bukkit.getWorld(world));
            l.setX((double)x);
            l.setY((double)y);
            l.setZ((double)z);
            final Game g = this.manager.getGame(id);
            if (g != null) {
                g.getSigns().add(l.getBlock().getLocation());
            }
        }
        for (final String sign : this.database.getStringList("QuickJoinSigns")) {
            final String[] split = sign.split(",");
            final String world2 = split[0];
            final int x2 = Integer.parseInt(split[1]);
            final int y2 = Integer.parseInt(split[2]);
            final int z2 = Integer.parseInt(split[3]);
            l.setWorld(Bukkit.getWorld(world2));
            l.setX((double)x2);
            l.setY((double)y2);
            l.setZ((double)z2);
            this.manager.getQuickJoinSigns().add(l.getBlock().getLocation());
        }
        final File file_gun = new File(this.getDataFolder(), "guns.yml");
        if (!file_gun.exists()) {
            console.sendMessage("§a - 正在创建 guns.yml");
            this.saveResource("guns.yml", true);
        }
        else {
            console.sendMessage("§b - 正在加载 guns.yml...");
        }
        this.gun = YamlConfiguration.loadConfiguration(file_gun);
        for (final String name2 : this.gun.getConfigurationSection("Guns").getKeys(false)) {
            final Material material = Material.valueOf(this.gun.getString("Guns." + name2 + ".Item-Information.Item-Type"));
            final int data = this.gun.getInt("Guns." + name2 + ".Item-Information.Item-Data");
            final String item_name = this.gun.getString("Guns." + name2 + ".Item-Information.Item-Name").replace('&', '§');
            final GunType type = GunType.valueOf(this.gun.getString("Guns." + name2 + ".Item-Information.Gun-Type"));
            final String sound = this.gun.getString("Guns." + name2 + ".Shoot.Sound");
            final String start_sound = this.gun.getString("Guns." + name2 + ".Reload.Sound_Start");
            final String end_sound = this.gun.getString("Guns." + name2 + ".Reload.Sound_End");
            final Gun g2 = new Gun(this, name2, new Item(material, data, item_name), type, sound, start_sound, end_sound);
            g2.setMaxRoundsPerPitch(this.gun.getInt("Guns." + name2 + ".Burst.MaxRoundsPerPitch"));
            g2.setRoundsPerYaw(this.gun.getInt("Guns." + name2 + ".Burst.RoundsPerYaw"));
            g2.setDelayRounds(this.gun.getInt("Guns." + name2 + ".Burst.DelayRounds"));
            g2.setRounds(this.gun.getInt("Guns." + name2 + ".Burst.Rounds"));
            g2.setDistance(this.gun.getInt("Guns." + name2 + ".Shoot.Distance"));
            g2.setAccuracy((float)this.gun.getDouble("Guns." + name2 + ".Shoot.Accuracy"));
            g2.hasSnipe(this.gun.getBoolean("Guns." + name2 + ".Shoot.Snipe"));
            g2.setDamage(this.gun.getDouble("Guns." + name2 + ".Shoot.Damage"));
            g2.setDuration(this.gun.getInt("Guns." + name2 + ".Reload.Duration"));
            g2.setAmount(this.gun.getInt("Guns." + name2 + ".Reload.Amount"));
            g2.setBullets(this.gun.getInt("Guns." + name2 + ".Shoot.Bullets"));
            g2.setDelay(this.gun.getInt("Guns." + name2 + ".Shoot.Delay"));
            g2.setSymbol(this.gun.getString("Guns." + name2 + ".Item-Information.Symbol"));
            g2.setModule(this.gun.getInt("Guns." + name2 + ".Reload.Module"));
            this.guns.add(g2);
        }
        final File file_grenade = new File(this.getDataFolder(), "grenades.yml");
        if (!file_grenade.exists()) {
            console.sendMessage("§a - 正在创建 grenades.yml");
            this.saveResource("grenades.yml", true);
        }
        else {
            console.sendMessage("§b - 正在加载 grenades.yml...");
        }
        this.grenade = YamlConfiguration.loadConfiguration(file_grenade);
        for (final String name3 : this.grenade.getConfigurationSection("Grenades").getKeys(false)) {
            final Material material2 = Material.valueOf(this.grenade.getString("Grenades." + name3 + ".Item-Information.Item-Type"));
            final int data2 = this.grenade.getInt("Grenades." + name3 + ".Item-Information.Item-Data");
            final String symbol = this.grenade.getString("Grenades." + name3 + ".Item-Information.Symbol");
            final String item_name2 = this.grenade.getString("Grenades." + name3 + ".Item-Information.Item-Name").replace('&', '§');
            final GrenadeType type2 = GrenadeType.valueOf(this.grenade.getString("Grenades." + name3 + ".Item-Information.Grenade-Type"));
            final double effect_power = this.grenade.getDouble("Grenades." + name3 + ".Properties.EffectPower");
            final double distance = this.grenade.getDouble("Grenades." + name3 + ".Properties.ThrowSpeed");
            final int delay = this.grenade.getInt("Grenades." + name3 + ".Properties.Delay");
            final int duration = this.grenade.getInt("Grenades." + name3 + ".Properties.Duration");
            this.grenades.add(new Grenade(this, name3, type2, new Item(material2, data2, item_name2), delay, duration, distance, effect_power, symbol));
        }
        final File s = new File(this.getDataFolder(), "shop.yml");
        if (!s.exists()) {
            console.sendMessage("§a - 正在创建 shop.yml");
            this.saveResource("shop.yml", true);
        }
        else {
            console.sendMessage("§b - 正在加载 shop.yml...");
        }
        this.shop = YamlConfiguration.loadConfiguration(s);
        for (final String gun_name : this.shop.getConfigurationSection("ShopGuns").getKeys(false)) {
            if (this.getGun(gun_name) != null) {
                final int price = this.shop.getInt("ShopGuns." + gun_name + ".Price");
                final GameTeam.Role role = GameTeam.getEnum(this.shop.getString("ShopGuns." + gun_name + ".Role"));
                final String name4 = this.shop.getString("ShopGuns." + gun_name + ".Item-Name");
                final String lore = this.shop.getString("ShopGuns." + gun_name + ".Item-Lore");
                final int slot = this.shop.getInt("ShopGuns." + gun_name + ".Slot");
                final Boolean permission = this.shop.getBoolean("ShopGuns." + gun_name + ".Permission");
                this.shop_items.add(new PlayerShop(gun_name, name4, slot, price, lore, role, permission != null && permission));
            }
            else {
                console.sendMessage("§c - " + gun_name + " from shop.yml 不存在于 guns.yml 中!");
            }
        }
        for (final String grenade_name : this.shop.getConfigurationSection("ShopGrenades").getKeys(false)) {
            if (this.getGrenade(grenade_name) != null) {
                final int price = this.shop.getInt("ShopGrenades." + grenade_name + ".Price");
                final String name5 = this.shop.getString("ShopGrenades." + grenade_name + ".Item-Name");
                final String lore2 = this.shop.getString("ShopGrenades." + grenade_name + ".Item-Lore");
                final int slot2 = this.shop.getInt("ShopGrenades." + grenade_name + ".Slot");
                this.shop_items.add(new PlayerShop(grenade_name, name5, slot2, price, lore2));
            }
            else {
                console.sendMessage("§c - " + grenade_name + " from shop.yml 不存在于 grenades.yml 中!");
            }
        }
        for (final String name6 : this.shop.getConfigurationSection("ShopRole").getKeys(false)) {
            final int slot3 = this.shop.getInt("ShopRole." + name6 + ".Slot");
            final int price2 = this.shop.getInt("ShopRole." + name6 + ".Price");
            final int slot_place = this.shop.getInt("ShopRole." + name6 + ".SlotPlace");
            final String item_name3 = this.shop.getString("ShopRole." + name6 + ".Item-Name");
            final String item_lore = this.shop.getString("ShopRole." + name6 + ".Item-Lore");
            final GameTeam.Role team = GameTeam.getEnum(this.shop.getString("ShopRole." + name6 + ".Team"));
            final Boolean permission2 = this.shop.getBoolean("ShopRole." + name6 + ".Permission");
            final Material item_material = Material.getMaterial(this.shop.getString("ShopRole." + name6 + ".Item-Material"));
            this.shop_items.add(new PlayerShop(slot3, slot_place, item_name3, item_material, price2, item_lore, team, permission2 != null && permission2, name6));
        }
        this.getServer().getPluginManager().registerEvents((Listener)(this.listener = new GameListener(this)), (Plugin)this);
        this.getCommand("counterstrike").setExecutor((CommandExecutor)new Commands(this));
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
        }
        if (this.bungee && this.bungee_random) {
            Collections.shuffle(Main.main.getManager().getGames());
        }
        final boolean enabled = this.config.getBoolean("MySQL.Enabled");
        if (enabled) {
            console.sendMessage("§b - 正在加载 MySQL...");
        }
        final int queueAmount = this.config.getInt("MySQL.QueueAmount");
        final String host = this.config.getString("MySQL.Host");
        final String database = this.config.getString("MySQL.Database");
        final String username = this.config.getString("MySQL.Username");
        final String password = this.config.getString("MySQL.Password");
        final int port = this.config.getInt("MySQL.Port");
        this.mysql = (enabled ? new MySQL(this, host, database, username, password, port, queueAmount) : null);
        final boolean playerHider = this.config.getBoolean("Config.AntiESP");
        if (playerHider) {
            new PlayerHider(this);
            console.sendMessage("§b - 正在加载 AntiESP...");
        }
        console.sendMessage("§aCounterStrike加载完毕!");
    }

    public void onDisable() {
        if (this.manager != null) {
            for (final Game g : this.manager.getGames()) {
                this.manager.stopGame(g, false);
                for (final Entity en : g.getCounterTerroristLoc().get(0).getWorld().getEntities()) {
                    if (en.getType() == EntityType.DROPPED_ITEM) {
                        en.remove();
                    }
                }
            }
            this.manager = null;
        }
        if (this.mysql != null) {
            this.mysql.closeConnection();
        }
        HandlerList.unregisterAll(this.listener);
        this.listener = null;
        if (this.update != null) {
            this.update.cancel();
            this.update = null;
        }
    }

    public YamlConfiguration getGameDatabase() {
        return this.database;
    }

    public VersionInterface getVersionInterface() {
        return this.version;
    }

    public Commands getCommandExecutor() {
        return this.commandExecutor;
    }

    public GameListener getListener() {
        return this.listener;
    }

    public void loadConfig() {
        this.reloadConfig();
        (this.config = this.getConfig()).addDefault("Config.AutoJoinOnEnd", (Object)false);
        this.config.addDefault("Config.AntiESP", (Object)true);
        this.config.addDefault("Config.LobbyTime", (Object)120);
        this.config.addDefault("Config.RoundToWin", (Object)13);
        this.config.addDefault("Config.RoundToSwitch", (Object)12);
        this.config.addDefault("Config.TexturePack", (Object)false);
        this.config.addDefault("Config.GameRoundTime", (Object)130);
        this.config.addDefault("Config.BombExplodeTimer", (Object)45);
        this.config.addDefault("Config.RoundWinMoney", (Object)3250);
        this.config.addDefault("Config.BombPlantMoney", (Object)300);
        this.config.addDefault("Config.MaxRadiusAsSpectator", (Object)100);
        this.config.addDefault("Config.HitAddition", (Object)0.0);
        this.config.addDefault("Config.HideVipGuns", (Object)false);
        this.config.addDefault("Config.EnableBlood", (Object)true);
        this.config.addDefault("Config.Compass", (Object)false);
        this.config.addDefault("Config.EnableBossBar", (Object)false);
        this.config.addDefault("Config.CopsDefaultGun", (Object)"pistol_cops");
        this.config.addDefault("Config.CrimsDefaultGun", (Object)"pistol");
        this.config.addDefault("Config.DefaultCrimKnife", (Object)this.defaultCrimKnife.name());
        this.config.addDefault("Config.DefaultCopKnife", (Object)this.defaultCopKnife.name());
        this.config.addDefault("Config.DefaultHelmetName", (Object)"&a普通头盔");
        this.config.addDefault("Config.DefaultChestplateName", (Object)"&a普通胸甲");
        this.config.addDefault("Config.DefaultLeggingName", (Object)"&a普通护腿");
        this.config.addDefault("Config.DefaultBootName", (Object)"&a普通靴子");
        this.config.addDefault("Config.Whitelist", (Object)Arrays.asList("/mute", "/list"));
        this.config.addDefault("Config.ReplaceGunsWithoutDrop", (Object)false);
        this.config.addDefault("Config.EndGameCommandWin", (Object)"money give %player% 30");
        this.config.addDefault("Config.EndGameCommandLose", (Object)"money give %player% 20");
        this.config.addDefault("Config.KillWinCommand", (Object)"money give %player% 2");
        this.config.addDefault("Config.KilledDeathCommand", (Object)"money give %player% 0");
        this.config.addDefault("BungeeMode.Enabled", (Object)false);
        this.config.addDefault("BungeeMode.ShutDown", (Object)false);
        this.config.addDefault("BungeeMode.CanJoinStartedGames", (Object)true);
        this.config.addDefault("BungeeMode.ServerOnGameEnd", (Object)"Hub");
        this.config.addDefault("BungeeMode.RandomMap", (Object)false);
        this.config.addDefault("MySQL.Enabled", (Object)false);
        this.config.addDefault("MySQL.QueueAmount", (Object)20);
        this.config.addDefault("MySQL.Host", (Object)"127.0.0.1");
        this.config.addDefault("MySQL.Database", (Object)"database");
        this.config.addDefault("MySQL.Username", (Object)"csgo");
        this.config.addDefault("MySQL.Password", (Object)"csgo");
        this.config.addDefault("MySQL.Port", (Object)3306);
        this.config.options().copyDefaults(true);
        this.saveConfig();
        this.autojoin = this.config.getBoolean("Config.AutoJoinOnEnd");
        this.canjoinstartedgame = this.config.getBoolean("BungeeMode.CanJoinStartedGames");
        this.bungee = this.config.getBoolean("BungeeMode.Enabled");
        this.bungee_hub = this.config.getString("BungeeMode.ServerOnGameEnd");
        this.bungee_random = this.config.getBoolean("BungeeMode.RandomMap");
        this.blood = this.config.getBoolean("Config.EnableBlood");
        this.boss = this.config.getBoolean("Config.EnableBossBar");
        this.lobby_time = this.config.getInt("Config.LobbyTime");
        this.whitelist = (List<String>)this.config.getStringList("Config.Whitelist");
        this.round_time = this.config.getInt("Config.GameRoundTime");
        this.hitAddition = this.config.getDouble("Config.HitAddition");
        this.bomb_time = this.config.getInt("Config.BombExplodeTimer");
        this.compass = this.config.getBoolean("Config.Compass");
        this.maxRadius = this.config.getInt("Config.MaxRadiusAsSpectator");
        this.copsdefaultweapon = this.config.getString("Config.CopsDefaultGun");
        this.crimsdefaultweapon = this.config.getString("Config.CrimsDefaultGun");
        this.end_command_win = this.config.getString("Config.EndGameCommandWin");
        this.kill_win_command = this.config.getString("Config.KillWinCommand");
        this.killed_death_command = this.config.getString("Config.KilledDeathCommand");
        this.end_command_lose = this.config.getString("Config.EndGameCommandLose");
        this.round_win_money = this.config.getInt("Config.RoundWinMoney");
        this.bomb_plant_money = this.config.getInt("Config.BombPlantMoney");
        this.round_to_switch = this.config.getInt("Config.RoundToSwitch");
        this.round_to_win = this.config.getInt("Config.RoundToWin");
        this.shutdown = this.config.getBoolean("BungeeMode.ShutDown");
        this.defaultCrimKnife = Material.valueOf(this.config.getString("Config.DefaultCrimKnife"));
        this.defaultCopKnife = Material.valueOf(this.config.getString("Config.DefaultCopKnife"));
        this.defaulthelmetname = this.config.getString("Config.DefaultHelmetName");
        this.defaultchestplatename = this.config.getString("Config.DefaultChestplateName");
        this.defaultleggingname = this.config.getString("Config.DefaultLeggingName");
        this.defaultbootname = this.config.getString("Config.DefaultBootName");
        this.hide_vip_guns = this.config.getBoolean("Config.HideVipGuns");
        this.ReplaceGunsWithoutDrop = this.config.getBoolean("Config.ReplaceGunsWithoutDrop");
        this.force_texture = this.config.getBoolean("Config.TexturePack");
    }

    public void saveGameDatabase() {
        final File f = new File(this.getDataFolder(), "database.yml");
        try {
            if (!f.exists()) {
                f.createNewFile();
                this.database = YamlConfiguration.loadConfiguration(f);
                if (this.database.getString("Signs") == null) {
                    final List<String> signs = new ArrayList<String>();
                    this.database.set("Signs", (Object)signs);
                }
                if (this.database.getString("QuickJoinSigns") == null) {
                    final List<String> signs = new ArrayList<String>();
                    this.database.set("QuickJoinSigns", (Object)signs);
                }
                if (this.database.getString("Game") == null) {
                    this.database.set("Game", (Object)"No game made yet");
                }
            }
            this.database.save(f);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MySQL getMySQL() {
        return this.mysql;
    }

    public String getHub() {
        return this.bungee_hub;
    }

    public void addSetup(final Player p, final GameSetup setup) {
        this.setup.put(p.getUniqueId(), setup);
    }

    public List<UUID> getTextureUsers() {
        return this.TextureUsers;
    }

    public void removeSetup(final Player p) {
        this.setup.remove(p.getUniqueId());
    }

    public GameSetup getSetup(final Player p) {
        return this.setup.get(p.getUniqueId());
    }

    public List<Gun> getGuns() {
        return this.guns;
    }

    public boolean randomMap() {
        return this.bungee_random;
    }

    public List<Grenade> getGrenades() {
        return this.grenades;
    }

    public List<PlayerShop> getShops() {
        return this.shop_items;
    }

    public UpdateTask getUpdateTask() {
        return this.update;
    }

    public Gun getGun(final ItemStack item) {
        for (final Gun gun : this.guns) {
            if (gun.getItem().equals(item, gun.getSymbol())) {
                return gun;
            }
        }
        return null;
    }

    public Gun getGun(final String name) {
        for (final Gun gun : this.guns) {
            if (gun.getName().equals(name)) {
                return gun;
            }
        }
        return null;
    }

    public Grenade getGrenade(final String name) {
        for (final Grenade grenade : this.grenades) {
            if (grenade.getName().equals(name)) {
                return grenade;
            }
        }
        return null;
    }

    public Grenade getGrenade(final ItemStack item) {
        for (final Grenade grenade : this.grenades) {
            if (grenade.getItem().equals(item)) {
                return grenade;
            }
        }
        return null;
    }

    public GameManager getManager() {
        return this.manager;
    }

    public boolean hasCompass() {
        return this.compass;
    }

    public int getLobbyTime() {
        return this.lobby_time;
    }

    public int getRoundTime() {
        return this.round_time;
    }

    public int getRadius() {
        return this.maxRadius;
    }

    public int getRoundWinMoney() {
        return this.round_win_money;
    }

    public int getRoundToWin() {
        return this.round_to_win;
    }

    public int getRoundToSwitch() {
        return this.round_to_switch;
    }

    public int getBombPlantMoney() {
        return this.bomb_plant_money;
    }

    public String getEndGameCommandWin() {
        return this.end_command_win;
    }

    public String getEndGameCommandLose() {
        return this.end_command_lose;
    }

    public String getKillCommand() {
        return this.kill_win_command;
    }

    public int getBombTime() {
        return this.bomb_time;
    }

    public double hitAddition() {
        return this.hitAddition;
    }

    public boolean replaceOldGuns() {
        return this.ReplaceGunsWithoutDrop;
    }

    public boolean shutdown() {
        return this.shutdown;
    }

    public List<String> getWhitelistCommands() {
        return this.whitelist;
    }

    public boolean hideVipGuns() {
        return this.hide_vip_guns;
    }

    public boolean enableBlood() {
        return this.blood;
    }

    public boolean enableBoss() {
        return this.boss;
    }

    public String getCopsDefaultWeapon() {
        return this.copsdefaultweapon;
    }

    public String getCrimsDefaultWeapon() {
        return this.crimsdefaultweapon;
    }

    public String getDefaultHelmetName() {
        return this.defaulthelmetname;
    }

    public String getDefaultChestplateName() { return this.defaultchestplatename; }
    public String getDefaultLeggingName() { return this.defaultleggingname; }
    public String getDefaultBootName() { return this.defaultbootname; }

    public boolean canForceTexture() {
        return this.force_texture;
    }

    public Material getDefaultCrimKnife() {
        return this.defaultCrimKnife;
    }

    public Material getDefaultCopKnife() {
        return this.defaultCopKnife;
    }

    public boolean autoJoin() {
        return this.autojoin;
    }

    public boolean corpseSupport() {
        return this.corpse;
    }

    public boolean placeholderSupport() {
        return this.papi;
    }

    public boolean canJoinStartedGame() {
        return this.canjoinstartedgame;
    }

    public static Main getInstance() {
        return Main.main;
    }

    public String getKilledDeathCommand() {
        return killed_death_command;
    }
}

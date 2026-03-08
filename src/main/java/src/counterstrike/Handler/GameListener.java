package src.counterstrike.Handler;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import src.counterstrike.Api.GameLeaveEvent;
import src.counterstrike.Cache.PlayerShop;
import src.counterstrike.Cache.ShopType;
import src.counterstrike.Grenades.Grenade;
import src.counterstrike.Guns.Gun;
import src.counterstrike.Main;
import src.counterstrike.Messages;
import src.counterstrike.Utils.GameUtils;
import src.counterstrike.Utils.ItemBuilder;
import src.counterstrike.Version.SpigotSound;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Iterator;
import java.util.List;

public class GameListener implements Listener {
    private Main main;
    private Inventory selector;

    public GameListener(final Main main) {
        this.selector = Bukkit.createInventory(null, 27, Messages.SELECTOR_NAME.toString());
        this.main = main;
        this.selector.setItem(11, ItemBuilder.create(Material.YELLOW_DYE, 1, 14, "&a" + Messages.TEAM_NAME + " " + Messages.TEAM_FIRST, Messages.SELECTOR_TEAM_A.toString()));
        this.selector.setItem(13, ItemBuilder.create(Material.GRAY_DYE, 1, 8, "&a" + Messages.TEAM_RANDOM, Messages.SELECTOR_TEAM_RANDOM.toString()));
        this.selector.setItem(15, ItemBuilder.create(Material.GREEN_DYE, 1, 10, "&a" + Messages.TEAM_NAME + " " + Messages.TEAM_SECOND, Messages.SELECTOR_TEAM_B.toString()));
    }

    @EventHandler
    public void onBlockBurn(final BlockBurnEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(final BlockIgniteEvent e) {
        if (e.getCause() == BlockIgniteEvent.IgniteCause.SPREAD) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGameModeChange(final PlayerGameModeChangeEvent e) {
        final Player p = e.getPlayer();
        if (e.getNewGameMode() != GameMode.SPECTATOR) {
            final Game g = this.main.getManager().getGame(p);
            if (g != null && g.inQueue(p)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            if (e.getHand() != EquipmentSlot.HAND) {
                e.setCancelled(true);
                return;
            }
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (g.getState() == GameState.WAITING) {
                    if (p.getInventory().getItemInHand() != null) {
                        if (p.getInventory().getItemInHand().getType() == Material.LEATHER) {
                            p.openInventory(this.selector);
                        } else if (p.getInventory().getItemInHand().getType() == Material.RED_BED) {
                            e.setCancelled(true);
                            this.main.getManager().removePlayer(g, p, false, false);
                            p.sendMessage(Messages.PREFIX + Messages.GAME_LEFT.toString());
                        }
                    }
                } else if (g.getState() == GameState.IN_GAME || g.getState() == GameState.ROUND) {
                    final ItemStack i = p.getInventory().getItemInHand();
                    if (i != null && i.getType() != Material.AIR && i.getType() == Material.EMERALD) {
                        if (this.main.getManager().isAtSpawn(g, p)) {
                            if (g.getTimer() > 90 || g.getState() == GameState.ROUND) {
                                p.openInventory((Inventory) g.getShops().get(p.getUniqueId()));
                            } else {
                                p.sendMessage(Messages.SHOP_AFTER_30_SECONDS.toString());
                            }
                        } else {
                            p.sendMessage(Messages.OPEN_SHOP_SPAWN.toString());
                        }
                        return;
                    }
                    if (i != null && i.getType() != Material.AIR && g.getState() == GameState.IN_GAME) {
                        if ((i.getType() == Material.SHEARS || i.getType() == Material.TRIPWIRE_HOOK) && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.NOTE_BLOCK) {
                            e.setCancelled(true);
                            if (this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers().contains(p) && !g.isDefusing(p) && p.getLocation().distance(g.getBomb().getLocation()) <= 2.0) {
                                g.addDefuser(p, (i.getType() == Material.SHEARS) ? 5 : 10);
                                p.playSound(p.getLocation(), SpigotSound.LEVEL_UP.getSound(), 1.0f, 1.0f);
                            }
                        }
                        if ((i.getType() == Material.SHEARS || i.getType() == Material.TRIPWIRE_HOOK) && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.WHEAT) {
                            e.setCancelled(true);
                            if (this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers().contains(p) && !g.isDefusing(p) && p.getLocation().distance(g.getBomb().getLocation()) <= 2.0) {
                                g.addDefuser(p, (i.getType() == Material.SHEARS) ? 5 : 10);
                                p.playSound(p.getLocation(), SpigotSound.LEVEL_UP.getSound(), 1.0f, 1.0f);
                            }
                        }
                        final Gun gun = this.main.getGun(i);
                        if (gun != null && !g.isDefusing(p)) {
                            gun.shot(g, p);
                        }
                        final Grenade grenade = this.main.getGrenade(i);
                        if (grenade != null && g.getState() == GameState.IN_GAME && !g.isRoundEnding() && !g.isDefusing(p)) {
                            e.setCancelled(true);
                            grenade.throwGrenade(this.main, g, p);
                        }
                    }
                }
            } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) {
                e.setCancelled(true);
                final ItemStack i = p.getInventory().getItemInHand();
                if (i != null && i.getType() != Material.AIR) {
                    final Gun gun = this.main.getGun(i);
                    if (gun != null) {
                        gun.reload(p, p.getInventory().getHeldItemSlot());
                    }
                }
            }
        } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getState() instanceof Sign) {
            final Location s = e.getClickedBlock().getLocation();
            for (final Game game : this.main.getManager().getGames()) {
                for (final Location sign : game.getSigns()) {
                    if (s.getWorld() == sign.getWorld() && s.distance(sign) == 0.0) {
                        e.setCancelled(true);
                        this.main.getManager().addPlayer(p, game);
                        return;
                    }
                }
            }
            for (final Location sign2 : this.main.getManager().getQuickJoinSigns()) {
                if (s.equals(sign2)) {
                    final Game ga = this.main.getManager().findGame(p);
                    if (ga == null) {
                        break;
                    }
                    if (ga.getState() == GameState.WAITING) {
                        this.main.getManager().addPlayer(p, ga);
                        break;
                    }
                    if (ga.getState() == GameState.IN_GAME || ga.getState() == GameState.ROUND) {
                        this.main.getManager().addQuickPlayer(ga, p);
                        break;
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onDamageWithKnife(final EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() == EntityType.PLAYER && e.getDamager().getType() == EntityType.PLAYER) {
            final Player victim = (Player) e.getEntity();
            final Player killer = (Player) e.getDamager();
            final Game g = this.main.getManager().getGame(victim);
            if (g != null) {
                e.setCancelled(true);
                if (g.getState() == GameState.IN_GAME && !this.main.getManager().sameTeam(g, victim, killer) && killer.getInventory().getHeldItemSlot() == 2 && killer.getItemInHand() != null && this.main.getUpdateTask().getDelay().get(victim.getUniqueId()) == null && !g.getSpectators().contains(victim)) {
                    if (this.main.enableBlood()) {
                        victim.getWorld().playEffect(victim.getLocation(), Effect.STEP_SOUND, (Object) Material.REDSTONE_WIRE);
                    }
                    final Vector toBlock = killer.getEyeLocation().toVector().subtract(victim.getEyeLocation().toVector());
                    final Vector playerLook = victim.getEyeLocation().getDirection().normalize();
                    final float angle = toBlock.angle(playerLook);
                    if (killer.getLocation().distance(victim.getLocation()) <= 1.7 || angle <= 1.5) {
                        this.main.getManager().damage(g, killer, victim, 3.0, Messages.PACK_KNIFE.toString());
                    } else {
                        this.main.getManager().damage(g, killer, victim, 20.0, Messages.PACK_KNIFE.toString());
                    }
                    this.main.getUpdateTask().getDelay().put(victim.getUniqueId(), 35);
                }
            }
        } else if (e.getDamager().getType() == EntityType.PLAYER && e.getEntity().getType() == EntityType.ITEM_FRAME) {
            final Game g2 = this.main.getManager().getGame((Player) e.getDamager());
            if (g2 != null) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBombPlant(final PlayerItemConsumeEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            e.setCancelled(true);
            if (p.getInventory().getItemInHand().getType() == Material.GOLDEN_APPLE && g.getState() == GameState.IN_GAME && !g.isRoundEnding() && p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                final Block b = p.getLocation().getBlock();
                if (b.getType() == Material.AIR) {
                    p.getInventory().setItem(5, new ItemStack(Material.AIR));
                    b.setType(Material.NOTE_BLOCK); // 将 DAYLIGHT_DETECTOR 替换为 NOTEBLOCK
                    BlockData data = b.getBlockData();
                    // 获取音符盒的 BlockState 并设置 instrument 和 note
                    if (data instanceof org.bukkit.block.data.type.NoteBlock noteBlock) {
                        noteBlock.setInstrument(Instrument.BASS_DRUM); // 设置 instrument 为 BASEDRUM
                        noteBlock.setNote(new Note(2)); // 设置 note 值为 2
                        b.setBlockData(noteBlock);
                    }

                    g.getBomb().setLocation(b.getLocation());
                    g.getBomb().setTimer(this.main.getBombTime());
                    g.setGameTimer(this.main.getBombTime());
                    g.getBomb().isPlanted(true);
                    g.setBombPlanter(p); // 记录安装炸弹的玩家
                    g.setMoney(p, g.getMoney(p) + this.main.getBombPlantMoney());
                    g.getStats().get(p.getUniqueId()).addBombPlanted();
                    for (final Player t : this.main.getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
                        t.setCompassTarget(p.getLocation().getBlock().getLocation());
                        t.playSound(t.getLocation(), "cs_gamesounds.gamesounds.bombplanted", 1.0f, 1.0f);
                        this.main.getVersionInterface().sendTitle(t, 0, 23, 0, "", Messages.BOMB_PLANTED.toString());
                    }
                    for (final Player ct : this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                        ct.playSound(ct.getLocation(), "cs_gamesounds.gamesounds.bombplanted", 1.0f, 1.0f);
                        this.main.getVersionInterface().sendTitle(ct, 0, 23, 0, "", Messages.BOMB_PLANTED.toString());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g == null) {
            for (final Game game : this.main.getManager().getGames()) {
                game.getTeamA().getPlayers().forEach(e.getRecipients()::remove);
                game.getTeamB().getPlayers().forEach(e.getRecipients()::remove);
            }
        } else {
            e.getRecipients().clear();
//            旁观者不能说话
            if (p.getGameMode().equals(GameMode.SPECTATOR)) {
                p.sendMessage(Messages.SPECTATOR_CHAT.toString());
                return;
            }
            if (g.getState() == GameState.WAITING || g.getState() == GameState.END) {
                e.getRecipients().addAll(g.getTeamA().getPlayers());
                e.getRecipients().addAll(g.getTeamB().getPlayers());
                e.setFormat(Messages.CHAT_WAITING_FORMAT.toString().replace("%player%", p.getName()).replace("%message%", "%2$s"));
            } else if (e.getMessage().startsWith("@") && e.getMessage().length() > 1) {
                e.getRecipients().addAll(g.getTeamA().getPlayers());
                e.getRecipients().addAll(g.getTeamB().getPlayers());
                e.setMessage(e.getMessage().substring(1));
                e.setFormat(Messages.CHAT_GLOBAL_FORMAT.toString().replace("%player%", p.getName()).replace("%message%", "%2$s"));
            } else if (this.main.getManager().getTeam(g, p) == GameTeam.Role.COUNTERTERRORIST) {
                final GameTeam ct = this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST);
                e.getRecipients().addAll(ct.getPlayers());
                e.setFormat(Messages.CHAT_PLAYING_FORMAT.toString().replace("%team%", Messages.PACK_COPS.toString()).replace("%player%", p.getName()).replace("%message%", "%2$s"));
            } else {
                final GameTeam t = this.main.getManager().getTeam(g, GameTeam.Role.TERRORIST);
                e.getRecipients().addAll(t.getPlayers());
                e.setFormat(Messages.CHAT_PLAYING_FORMAT.toString().replace("%team%", Messages.PACK_CRIMS.toString()).replace("%player%", p.getName()).replace("%message%", "%2$s"));
            }
        }
    }

    @EventHandler
    public void onSlotChange(final PlayerItemHeldEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null && g.getState() != GameState.WAITING) {
            final Gun gun = this.main.getGun(p.getInventory().getItem(e.getPreviousSlot()));
            if (gun != null) {
                gun.resetPlayer(p);
                if (gun.getModule() == 2) {
                    p.setExp(0.0f);
                } else {
                    p.getInventory().getItem(e.getPreviousSlot()).setDurability((short) 0);
                }
            }
            final Gun gun_hand = this.main.getGun(p.getInventory().getItemInHand());
            if (gun_hand != null && gun_hand.hasSnipe() && p.isSneaking()) {
                e.setCancelled(true);
            }
            if (!e.isCancelled()) {
                if (e.getNewSlot() == 2) {
                    p.setWalkSpeed(0.25f);
                } else {
                    p.setWalkSpeed(0.2f);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(final PlayerTeleportEvent e) {
        final Player p = e.getPlayer();
        if (this.main.getManager().getGame(p) != null && e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        final Player p = e.getPlayer();
        if (this.main.getManager().getGame(p) == null) {
            return;
        }
        if (e.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(final InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            e.setCancelled(true);
            if (e.getSlotType() != InventoryType.SlotType.OUTSIDE) {
                if (e.getClickedInventory().equals(this.selector)) {
                    if (e.getSlot() == 11) {
                        g.addTeamA(p);
                        p.playSound(p.getLocation(), SpigotSound.NOTE_STICKS.getSound(), 1.0f, 1.0f);
                        p.sendMessage(Messages.SELECTOR_CHOOSE_TEAM_A.toString());
                        p.closeInventory();
                    } else if (e.getSlot() == 13) {
                        g.addRandomTeam(p);
                        p.playSound(p.getLocation(), SpigotSound.NOTE_STICKS.getSound(), 1.0f, 1.0f);
                        p.sendMessage(Messages.SELECTOR_CHOOSE_TEAM_RANDOM.toString());
                        p.closeInventory();
                    } else if (e.getSlot() == 15) {
                        g.addTeamB(p);
                        p.playSound(p.getLocation(), SpigotSound.NOTE_STICKS.getSound(), 1.0f, 1.0f);
                        p.sendMessage(Messages.SELECTOR_CHOOSE_TEAM_B.toString());
                        p.closeInventory();
                    }
                }
                if (g.getState() != GameState.WAITING && e.getClickedInventory().getType() == InventoryType.CHEST && !e.getClickedInventory().equals(p.getInventory())) {
                    for (final PlayerShop s : this.main.getShops()) {
                        if (e.getSlot() == s.getSlot() && (s.getRole() == null || this.main.getManager().getTeam(g, p) == s.getRole())) {
                            if (s.getType() == ShopType.ITEMS && s.hasPermission() && !p.hasPermission("cs.weapon." + s.getItName())) {
                                p.closeInventory();
                                p.sendMessage(Messages.SHOP_ITEM_NO_PERMISSION.toString());
                                p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                break;
                            }
                            if (s.getType() != ShopType.ITEMS && s.hasPermission() && !p.hasPermission("cs.weapon." + s.getWeaponName())) {
                                p.closeInventory();
                                p.sendMessage(Messages.SHOP_NO_PERMISSION.toString());
                                p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                break;
                            }
                            if (s.getPrice() > g.getMoney(p)) {
                                p.closeInventory();
                                p.sendMessage(Messages.SHOP_NOT_ENOUGH_MONEY.toString());
                                p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                break;
                            }
                            if (s.getType() == ShopType.GRENADE) {
                                final Grenade grenade = this.main.getGrenade(s.getWeaponName());
                                if (p.getInventory().getItem(grenade.getGrenadeType().getSlot()) == null) {
                                    g.setMoney(p, g.getMoney(p) - s.getPrice());
                                    p.getInventory().setItem(grenade.getGrenadeType().getSlot(), ItemBuilder.create(grenade.getItem().getType(), 1, grenade.getItem().getData(), grenade.getItem().getName() + " &7" + grenade.getSymbol()));
                                    p.playSound(p.getLocation(), "cs_shop.shop.shopbuyitem", 1.0f, 1.0f);
                                } else {
                                    p.closeInventory();
                                    p.sendMessage(Messages.SHOP_GRENADE_ALREADY_IN_SLOT.toString());
                                    p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                }
                                break;
                            }
                            if (s.getType() == ShopType.GUN) {
                                final Gun gun = this.main.getGun(s.getWeaponName());
                                if (p.getInventory().getItem((int) gun.getGunType().getID()) == null || this.main.replaceOldGuns()) {
                                    g.setMoney(p, g.getMoney(p) - s.getPrice());
                                    p.getInventory().setItem((int) gun.getGunType().getID(), ItemBuilder.create(gun.getItem().getType(), gun.getAmount(), gun.getItem().getData(), gun.getItem().getName() + " &7" + gun.getSymbol()));
                                    p.playSound(p.getLocation(), "cs_shop.shop.shopbuyitem", 1.0f, 1.0f);
                                } else {
                                    p.closeInventory();
                                    p.sendMessage(Messages.SHOP_GUN_ALREADY_IN_SLOT.toString());
                                    p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                }
                                break;
                            }
                            if (s.getRole() != null && this.main.getManager().getTeam(g, p) != s.getRole()) {
                                break;
                            }
                            if (s.getMaterial() != Material.SHEARS) {
                                final ItemStack place = p.getInventory().getItem(s.getSlotPlace());

                                // 检查是否是头盔
                                if (s.getMaterial() == Material.IRON_HELMET || s.getMaterial() == Material.CHAINMAIL_HELMET) {
                                    // 获取对应团队的胸甲槽位
                                    int chestplateSlot = 38;

                                    // 检查胸甲槽位是否有胸甲
                                    ItemStack chestplate = p.getInventory().getItem(chestplateSlot);

                                    if (chestplate.getType() == Material.LEATHER_CHESTPLATE) {
                                        p.closeInventory();
                                        p.sendMessage(Messages.SHOP_NEED_CHESTPLATE.toString()); // 提示需要先购买胸甲
                                        p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                        break;
                                    }
                                }

                                // 正常购买逻辑
                                if (s.getSlotPlace() == 2 || place == null || place.getType() == Material.LEATHER_HELMET || place.getType() == Material.LEATHER_CHESTPLATE) {
                                    g.setMoney(p, g.getMoney(p) - s.getPrice());
                                    p.getInventory().setItem(s.getSlotPlace(), ItemBuilder.create(s.getMaterial(), 1, s.getName(), false));
                                    p.playSound(p.getLocation(), "cs_shop.shop.shopbuyitem", 1.0f, 1.0f);
                                } else {
                                    p.closeInventory();
                                    p.sendMessage(Messages.SHOP_ALREADY_BROUGHT.toString());
                                    p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                                }
                                break;
                            }
                            if (p.getInventory().getItem(s.getSlotPlace()).getType() != Material.SHEARS) {
                                g.setMoney(p, g.getMoney(p) - s.getPrice());
                                p.getInventory().setItem(s.getSlotPlace(), ItemBuilder.create(s.getMaterial(), 1, s.getName(), false));
                                p.playSound(p.getLocation(), "cs_shop.shop.shopbuyitem", 1.0f, 1.0f);
                                break;
                            }
                            p.closeInventory();
                            p.sendMessage(Messages.SHOP_ALREADY_BROUGHT.toString());
                            p.playSound(p.getLocation(), "cs_shop.shop.shopcantbuy", 1.0f, 1.0f);
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerCommand(final PlayerCommandPreprocessEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            final String[] split = e.getMessage().split(" ");
            final String cmd = split[0];
            if (cmd.equalsIgnoreCase("/leave") || cmd.equalsIgnoreCase("/quit")) {
                e.setCancelled(true);
                p.sendMessage(Messages.PREFIX + Messages.GAME_LEFT.toString());
                this.main.getManager().removePlayer(g, p, false, false);
            } else if (!cmd.equalsIgnoreCase("/cs") && !cmd.equalsIgnoreCase("/counterstrike") && !GameUtils.containsIgnoreCase(this.main.getWhitelistCommands(), cmd)) {
                e.setCancelled(true);
                p.sendMessage(Messages.PREFIX + " " + Messages.RESTRICTED_COMMAND.toString());
            }
        }
    }

    @EventHandler
    public void onPlace(final BlockPlaceEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        final Player p = e.getPlayer();
        final Game ga = this.main.getManager().getGame(p);
        if (ga != null) {
            e.setCancelled(true);
        } else if (e.getBlock().getState() instanceof Sign && p.hasPermission("cs.sign")) {
            final Location s = e.getBlock().getLocation();
            for (final Game g : this.main.getManager().getGames()) {
                final Iterator<Location> it = g.getSigns().iterator();
                while (it.hasNext()) {
                    final Location sign = it.next();
                    if (s.getWorld() == sign.getWorld() && s.distance(sign) == 0.0) {
                        p.sendMessage(Messages.PREFIX + " §cSign removed succefully!");
                        final String key = g.getID() + "," + s.getWorld().getName() + "," + s.getBlockX() + "," + s.getBlockY() + "," + s.getBlockZ();
                        final List<String> keys = (List<String>) this.main.getGameDatabase().getStringList("Signs");
                        keys.remove(key);
                        this.main.getGameDatabase().set("Signs", (Object) keys);
                        this.main.saveGameDatabase();
                        it.remove();
                        return;
                    }
                }
            }
            final Iterator<Location> it2 = this.main.getManager().getQuickJoinSigns().iterator();
            while (it2.hasNext()) {
                final Location sign2 = it2.next();
                if (s.equals((Object) sign2)) {
                    p.sendMessage(Messages.PREFIX + " §cQuick-Sign removed succefully!");
                    final String key2 = s.getWorld().getName() + "," + s.getBlockX() + "," + s.getBlockY() + "," + s.getBlockZ();
                    final List<String> keys2 = (List<String>) this.main.getGameDatabase().getStringList("QuickJoinSigns");
                    keys2.remove(key2);
                    this.main.getGameDatabase().set("QuickJoinSigns", (Object) keys2);
                    this.main.saveGameDatabase();
                    it2.remove();
                }
            }
        }
    }

    @EventHandler
    public void onPing(final ServerListPingEvent e) {
        if (this.main.getManager().isBungeeMode()) {
            final Game g = this.main.getManager().getGames().get(this.main.getManager().getMap());
            e.setMotd(g.getState().getState());
            e.setMaxPlayers(g.getMaxPlayers());
        }
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent e) {
        if (this.main.getManager().isBungeeMode()) {
            final Game g = this.main.getManager().getGames().get(this.main.getManager().getMap());
            if (g.getTeamA().size() + g.getTeamB().size() == g.getMaxPlayers()) {
                e.disallow(PlayerLoginEvent.Result.KICK_FULL, Messages.ARENA_IS_FULL.toString());
            } else if (g.getState() != GameState.WAITING && !this.main.canJoinStartedGame()) {
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Messages.ARENA_HAS_STARTED.toString());
            }
        }
    }

    @EventHandler
    public void onLeave(final GameLeaveEvent e) {
        if (this.main.getManager().isBungeeMode()) {
            final ByteArrayOutputStream data = new ByteArrayOutputStream();
            final DataOutputStream out = new DataOutputStream(data);
            try {
                out.writeUTF("Connect");
                out.writeUTF(this.main.getHub());
                e.getPlayer().sendPluginMessage((Plugin) this.main, "BungeeCord", data.toByteArray());
                out.close();
                data.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        final GameManager manager = this.main.getManager();
        if (manager.isBungeeMode()) {
            final Player p = e.getPlayer();
            e.setJoinMessage((String) null);
            final Game g = manager.getGames().get(manager.getMap());
            if (g.getState() != GameState.WAITING) {
                manager.addQuickPlayer(g, p);
            } else {
                manager.addPlayer(p, g);
            }
        } else {
            for (final Game g : manager.getGames()) {
                for (final Player p2 : g.getTeamA().getPlayers()) {
                    p2.hidePlayer(e.getPlayer());
                }
                for (final Player p2 : g.getTeamB().getPlayers()) {
                    p2.hidePlayer(e.getPlayer());
                }
            }
        }
        this.main.getVersionInterface().setHandSpeed(e.getPlayer(), 4.0);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onLeave(final PlayerQuitEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            this.main.getManager().removePlayer(g, p, false, true);
        }
        if (this.main.getManager().isBungeeMode()) {
            e.setQuitMessage("");
        }
        this.main.getTextureUsers().remove(p.getUniqueId());
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPluginEnable(final PluginEnableEvent e) {
        if (e.getPlugin().equals(this.main)) {
            for (final Player p : Bukkit.getOnlinePlayers()) {
                this.main.getTextureUsers().add(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            if (g.getState() == GameState.IN_GAME) {
                if (p.getFallDistance() >= 6.0f && !g.getSpectators().contains(p) && p.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
                    this.main.getManager().damage(g, null, p, p.getFallDistance(), Messages.PACK_FALL.toString());
                }
                if ((e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) && g.getBomb().getCarrier() == p) {
                    final ItemStack is = p.getInventory().getItem(5);
                    if (is != null) {
                        if (this.main.getManager().isInBombArea(g, e.getTo())) {
                            if (is.getType() == Material.TNT) {
                                final ItemMeta im = is.getItemMeta();
                                im.setDisplayName("§eꐴ§a " + Messages.ITEM_BOMB_NAME + " §8(§c" + Messages.ITEM_RIGHT_CLICK + "§8)");
                                im.setCustomModelData(1000);
                                is.setItemMeta(im);
                                is.setType(Material.GOLDEN_APPLE);
                                p.playSound(p.getLocation(), SpigotSound.CLICK.getSound(), 1.0f, 1.0f);
                            }
                        } else if (is.getType() == Material.GOLDEN_APPLE) {
                            final ItemMeta im = is.getItemMeta();
                            im.setDisplayName("§eꐴ§a " + Messages.ITEM_BOMB_NAME);
                            im.setCustomModelData(1000);
                            is.setItemMeta(im);
                            is.setType(Material.TNT);
                            p.playSound(p.getLocation(), SpigotSound.CLICK.getSound(), 1.0f, 1.0f);
                        }
                    }
                }
            } else if (g.getState() == GameState.ROUND && !g.getSpectators().contains(p) && (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ())) {
                e.setTo(e.getFrom());
            }
        }
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && this.main.getManager().getGame((Player) e.getEntity()) != null && e.getCause() != EntityDamageEvent.DamageCause.CUSTOM) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(final FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player && this.main.getManager().getGame((Player) e.getEntity()) != null) {
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onHealthRegain(final EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player && this.main.getManager().getGame((Player) e.getEntity()) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityBreak(final HangingBreakByEntityEvent e) {
        if (e.getRemover().getType() == EntityType.PLAYER) {
            final Player p = (Player) e.getRemover();
            final Game g = this.main.getManager().getGame(p);
            if (g != null) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityBreak(final HangingBreakEvent e) {
        if (e.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPick(final PlayerPickupItemEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            e.setCancelled(true);
            if (!g.getSpectators().contains(p) && (g.getState() == GameState.IN_GAME || g.getState() == GameState.ROUND)) {
                final Item item = e.getItem();
                final ItemStack is = e.getItem().getItemStack();
                if (is.getType() == Material.SHEARS && this.main.getManager().getTeam(g, p) == GameTeam.Role.COUNTERTERRORIST) {
                    item.remove();
                    g.getDrops().remove(item);
                    p.getInventory().setItem(5, is);
                    p.playSound(p.getLocation(), SpigotSound.ITEM_PICKUP.getSound(), 5.0f, 1.0f);
                } else {
                    if ((is.getType() == Material.TNT || is.getType() == Material.GOLDEN_APPLE) && this.main.getManager().getTeam(g, p) == GameTeam.Role.TERRORIST) {
                        item.remove();
                        g.getDrops().remove(item);
                        if (is.getType() == Material.TNT && this.main.getManager().isInBombArea(g, item.getLocation())) {
                            final ItemMeta im = is.getItemMeta();
                            im.setDisplayName("§eꐴ§a " + Messages.ITEM_BOMB_NAME + " §8(§c" + Messages.ITEM_RIGHT_CLICK + "§8)");
                            im.setCustomModelData(1000);
                            is.setItemMeta(im);
                            is.setType(Material.GOLDEN_APPLE);
                        }
                        if (is.getType() == Material.GOLDEN_APPLE && !this.main.getManager().isInBombArea(g, item.getLocation())) {
                            final ItemMeta im = is.getItemMeta();
                            im.setDisplayName("§eꐴ§a " + Messages.ITEM_BOMB_NAME);
                            im.setCustomModelData(1000);
                            is.setItemMeta(im);
                            is.setType(Material.TNT);
                        }
                        for (final Player t : this.main.getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
                            t.playSound(t.getLocation(), "cs_gamesounds.gamesounds.pickedupthebomb", 1.0f, 1.0f);
                        }
                        g.getBomb().setCarrier(p);
                        p.getInventory().setItem(5, is);
                        return;
                    }
                    final Grenade grenade = this.main.getGrenade(is);
                    if (grenade != null && g.getDrops().get(item) != null) {
                        final int slot = grenade.getGrenadeType().getSlot();
                        if (p.getInventory().getItem(slot) == null) {
                            e.setCancelled(true);
                            p.getInventory().setItem(slot, is);
                            g.getDrops().remove(item);
                            item.remove();
                            p.playSound(p.getLocation(), SpigotSound.ITEM_PICKUP.getSound(), 5.0f, 1.0f);
                        }
                    }
                    final Gun gun = this.main.getGun(is);
                    final Integer amount = g.getDrops().get(item);
                    if (gun != null && amount != null) {
                        final int id = gun.getGunType().getID();
                        if (p.getInventory().getItem(id) == null) {
                            e.setCancelled(true);
                            is.setAmount(amount + 1);
                            p.getInventory().setItem(id, is);
                            g.getDrops().remove(item);
                            item.remove();
                            p.playSound(p.getLocation(), SpigotSound.ITEM_PICKUP.getSound(), 5.0f, 1.0f);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrop(final PlayerDropItemEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            final int slot = p.getInventory().getHeldItemSlot();
            final ItemStack is = e.getItemDrop().getItemStack();
            final int amount = p.getInventory().getItemInHand().getAmount();
            if (g.getState() == GameState.IN_GAME || g.getState() == GameState.ROUND) {
                if (is.getType() == Material.SHEARS) {
                    g.getDrops().put(e.getItemDrop(), 1);
                    p.getInventory().setItem(5, ItemBuilder.create(Material.TRIPWIRE_HOOK, 1, "&a" + Messages.ITEM_SHEAR_NAME + Messages.PACK_SHEAR, false));
                    return;
                }
                if (is.getType() == Material.TNT || is.getType() == Material.GOLDEN_APPLE) {
                    if (is.getType() == Material.GOLDEN_APPLE) {
                        final ItemMeta im = is.getItemMeta();
                        im.setDisplayName("§eꐴ§a " + Messages.ITEM_BOMB_NAME);
                        im.setCustomModelData(1000);
                        is.setItemMeta(im);
                        is.setType(Material.TNT);
                    }
                    g.getDrops().put(e.getItemDrop(), 1);
                    g.getBomb().setDrop(e.getItemDrop());
                    for (final Player t : this.main.getManager().getTeam(g, GameTeam.Role.TERRORIST).getPlayers()) {
                        t.playSound(t.getLocation(), "cs_gamesounds.gamesounds.bombdroppedyourteam", 1.0f, 1.0f);
                    }
                    for (final Player ct : this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers()) {
                        ct.playSound(ct.getLocation(), "cs_gamesounds.gamesounds.bombdroppedenemyteam", 1.0f, 1.0f);
                    }
                    return;
                }
                final Gun gun = this.main.getGun(is);
                if (gun != null) {
                    g.getDrops().put(e.getItemDrop(), amount);
                    is.setAmount(1);
                    p.setExp(0.0f);
                    gun.resetDelay(p);
                    e.getItemDrop().setItemStack(ItemBuilder.create(is.getType(), 1, gun.getItem().getData(), is.getItemMeta().getDisplayName()));
                    p.getInventory().setItem(slot, (ItemStack) null);
                    p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.droppedagun", 1.0f, 1.0f);
                    if (gun.hasSnipe()) {
                        this.main.getVersionInterface().sendFakeItem(p, 0, p.getInventory().getHelmet());
                    }
                    return;
                }
                final Grenade grenade = this.main.getGrenade(is);
                if (grenade != null) {
                    g.getDrops().put(e.getItemDrop(), 1);
                    is.setAmount(1);
                    e.getItemDrop().setItemStack(ItemBuilder.create(is.getType(), 1, grenade.getItem().getData(), is.getItemMeta().getDisplayName()));
                    p.getInventory().setItem(slot, (ItemStack) null);
                    p.playSound(p.getLocation(), "cs_gamesounds.gamesounds.droppedagun", 1.0f, 1.0f);
                    return;
                }
            }
            final ItemStack i = is.clone();
            e.getItemDrop().remove();
            p.getInventory().setItem(p.getInventory().getHeldItemSlot(), i);
        }
    }

    @EventHandler
    public void onSneak(final PlayerToggleSneakEvent e) {
        final Player p = e.getPlayer();
        final Game g = this.main.getManager().getGame(p);
        if (g != null) {
            final ItemStack is = p.getInventory().getItemInHand();
            final Gun gun = this.main.getGun(is);
            if (gun != null && gun.hasSnipe()) {
                if (e.isSneaking()) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 2));
                    this.main.getVersionInterface().sendFakeItem(p, 0, new ItemStack(Material.CARVED_PUMPKIN));
                } else {
                    p.removePotionEffect(PotionEffectType.SLOW);
                    this.main.getVersionInterface().sendFakeItem(p, 0, p.getInventory().getHelmet());
                }
            }
        }
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onInteract(final PlayerInteractAtEntityEvent e) {
        final Player p = e.getPlayer();
        final ItemStack i = p.getInventory().getItemInHand();
        final Game g = this.main.getManager().getGame(p);
        if (g != null && i != null && i.getType() != Material.AIR) {
            e.setCancelled(true);
            if ((i.getType() == Material.SHEARS || i.getType() == Material.TRIPWIRE_HOOK) && e.getRightClicked().getType() == EntityType.valueOf("ARMOR_STAND") && this.main.getManager().getTeam(g, GameTeam.Role.COUNTERTERRORIST).getPlayers().contains(p) && !g.isDefusing(p) && p.getLocation().distance(g.getBomb().getLocation()) <= 2.0) {
                g.addDefuser(p, (i.getType() == Material.SHEARS) ? 5 : 10);
                p.playSound(p.getLocation(), SpigotSound.LEVEL_UP.getSound(), 1.0f, 1.0f);
            }
            final Gun gun = this.main.getGun(i);
            if (gun != null && !g.isDefusing(p) && g.getState() == GameState.IN_GAME) {
                gun.shot(g, p);
            }
        }
    }

    @EventHandler
    public void onPsyhics(final BlockPhysicsEvent e) {
        if (e.getBlock().getType() == Material.WHEAT) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(final PlayerSwapHandItemsEvent e) {
        final Player p = e.getPlayer();
        if (this.main.getManager().getGame(p) != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onResourcePack(final PlayerResourcePackStatusEvent e) {
        if (this.main.canForceTexture()) {
            final Player p = e.getPlayer();
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED) {
                p.sendMessage(Messages.TEXTURE_DECLINED.toString());
                if (this.main.getManager().isBungeeMode()) {
                    this.main.getServer().getPluginManager().callEvent((Event) new GameLeaveEvent(p));
                }
            }
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
                p.sendMessage(Messages.TEXTURE_ACCEPTED.toString());
            }
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
                p.sendMessage(Messages.TEXTURE_FAILED.toString());
                if (this.main.getManager().isBungeeMode()) {
                    this.main.getServer().getPluginManager().callEvent((Event) new GameLeaveEvent(p));
                }
            }
            if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                p.sendMessage(Messages.TEXTURE_LOADED.toString());
                this.main.getTextureUsers().add(p.getUniqueId());
            }
        }
    }

    @EventHandler
    public void onSignPlace(final SignChangeEvent e) {
        final Player p = e.getPlayer();
        if (e.getLine(0).equals("[CounterStrike]") && p.hasPermission("cs.sign")) {
            try {
                final int line = Integer.valueOf(e.getLine(1));
                final Game g = this.main.getManager().getGame(line);
                if (g != null) {
                    final Location l = e.getBlock().getLocation();
                    g.getSigns().add(l);
                    e.setLine(0, Messages.SIGN_FIRST.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                    e.setLine(1, Messages.SIGN_SECOND.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                    e.setLine(2, Messages.SIGN_THIRD.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                    e.setLine(3, Messages.SIGN_FOURTH.toString().replace("%prefix%", Messages.PREFIX.toString()).replace("%name%", g.getName()).replace("%state%", g.getState().getState()).replace("%min%", g.getTeamA().size() + g.getTeamB().size() + "").replace("%max%", g.getMaxPlayers() + ""));
                    final List<String> keys = (List<String>) this.main.getGameDatabase().getStringList("Signs");
                    keys.add(line + "," + l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ());
                    this.main.getGameDatabase().set("Signs", (Object) keys);
                    this.main.saveGameDatabase();
                    p.sendMessage(Messages.PREFIX + " §aSign created succefully!");
                } else {
                    e.setCancelled(true);
                    p.sendMessage(Messages.PREFIX + " §cThe game dosen't exist!");
                }
            } catch (Exception ex) {
                final String l2 = e.getLine(1);
                if (l2 != null && l2.equalsIgnoreCase("QuickJoin")) {
                    final Location l = e.getBlock().getLocation();
                    e.setLine(0, Messages.PREFIX.toString());
                    e.setLine(1, "§5• §0" + Messages.SIGN_QUICKJOIN.toString() + " §5•");
                    final String key = l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
                    final List<String> keys2 = (List<String>) this.main.getGameDatabase().getStringList("QuickJoinSigns");
                    keys2.add(key);
                    this.main.getGameDatabase().set("QuickJoinSigns", (Object) keys2);
                    this.main.saveGameDatabase();
                    this.main.getManager().getQuickJoinSigns().add(l);
                    p.sendMessage(Messages.PREFIX + " §aQuick-Sign created succefully!");
                } else {
                    p.sendMessage(Messages.PREFIX + " §cGame ID invalid!");
                }
            }
        }
    }
}

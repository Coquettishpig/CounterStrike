package src.counterstrike.Cache;

import org.bukkit.Material;
import src.counterstrike.Handler.GameTeam;

public class PlayerShop
{
    private int slot;
    private int price;
    private GameTeam.Role role;
    private String name;
    private String lore;
    private int slot_place;
    private String weapon_name;
    private Material material;
    private ShopType shoptype;
    private boolean permission;
    private String it_name;
    
    public PlayerShop(final String gun_name, final String name, final int slot, final int price, final String lore, final GameTeam.Role role, final boolean permission) {
        this.name = name;
        this.slot = slot;
        this.price = price;
        this.lore = lore;
        this.role = role;
        this.shoptype = ShopType.GUN;
        this.weapon_name = gun_name;
        this.permission = permission;
    }
    
    public PlayerShop(final String grenade_name, final String name, final int slot, final int price, final String lore) {
        this.name = name;
        this.slot = slot;
        this.price = price;
        this.lore = lore;
        this.weapon_name = grenade_name;
        this.shoptype = ShopType.GRENADE;
    }
    
    public PlayerShop(final int slot, final int place_slot, final String name, final Material material, final int price, final String lore, final GameTeam.Role role, final boolean permission, final String it_name) {
        this.shoptype = ShopType.ITEMS;
        this.slot = slot;
        this.name = name;
        this.price = price;
        this.lore = lore;
        this.it_name = it_name;
        this.role = role;
        this.slot_place = place_slot;
        this.material = material;
        this.permission = permission;
    }
    
    public boolean hasPermission() {
        return this.permission;
    }
    
    public ShopType getType() {
        return this.shoptype;
    }
    
    public GameTeam.Role getRole() {
        return this.role;
    }
    
    public int getPrice() {
        return this.price;
    }
    
    public int getSlotPlace() {
        return this.slot_place;
    }
    
    public String getItName() {
        return this.it_name;
    }
    
    public String getLore() {
        return this.lore;
    }
    
    public Material getMaterial() {
        return this.material;
    }
    
    public int getSlot() {
        return this.slot;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getWeaponName() {
        return this.weapon_name;
    }
}

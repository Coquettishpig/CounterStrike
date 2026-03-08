package src.counterstrike.Utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Item
{
    private String name;
    private int data;
    private Material material;
    
    public Item(final ItemStack i) {
        this.material = i.getType();
        final ItemMeta itemMeta = i.getItemMeta();
        if (itemMeta != null) {
            this.data = itemMeta.getCustomModelData();
            this.name = itemMeta.getDisplayName();
        }
    }
    
    public Item(final Material material, final int data, final String name) {
        this.material = material;
        this.data = data;
        this.name = name;
    }
    
    public boolean equals(final ItemStack i) {
        return i != null && i.getItemMeta() != null && i.getType() == this.material && i.getItemMeta().getDisplayName().contains(this.name);
    }
    
    public boolean equals(final ItemStack i, final String symbol) {
        return i != null && i.getItemMeta() != null && i.getType() == this.material && i.getItemMeta().getDisplayName().equals(this.name + " §7" + symbol);
    }
    
    public Material getType() {
        return this.material;
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getData() {
        return this.data;
    }
}

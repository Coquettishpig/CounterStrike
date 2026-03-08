package src.counterstrike.Utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder
{
    public static ItemStack create(final Material m, final Integer number, final String nume, final String lore) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        im.setDisplayName(nume.replace("&", "§"));
        if (lore != null) {
            im.setLore(Arrays.asList(lore.replace("&", "§").split("#")));
        }
        is.setItemMeta(im);
        return is;
    }
    
    public static ItemStack create(final Material m, final int number, final String nume, final boolean value) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        if (value) {
            im.setUnbreakable(true);
        }
        im.setDisplayName(nume.replace("&", "§"));
        is.setItemMeta(im);
        return is;
    }
    
    public static ItemStack create(final Material m, final int number, final int variant, final String nume) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        if (im != null) {
            im.setCustomModelData(Integer.valueOf(variant));
            im.setDisplayName(nume.replace("&", "§"));
            is.setItemMeta(im);
        }
        return is;
    }
    
    public static ItemStack createItem(final Material material, final Color color, final String nume) {
        final ItemStack is = new ItemStack(material);
        final LeatherArmorMeta meta = (LeatherArmorMeta)is.getItemMeta();
        meta.setColor(color);
        is.setItemMeta((ItemMeta)meta);
        final ItemMeta im = is.getItemMeta();
        im.setDisplayName(nume.replace("&", "§"));
        is.setItemMeta(im);
        return is;
    }
    
    public static ItemStack create(final Material m, final int number, final int variant, final String nume, final String lore) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        if (im != null) {
            im.setCustomModelData(Integer.valueOf(variant));
            im.setDisplayName(nume.replace("&", "§"));
            im.setLore((List)Arrays.<String>asList(lore.replace("&", "§").split("#")));
            is.setItemMeta(im);
        }
        return is;
    }
}

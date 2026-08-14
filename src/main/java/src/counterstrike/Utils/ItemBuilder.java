package src.counterstrike.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import me.clip.placeholderapi.PlaceholderAPI; // 确保导入了 PAPI

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder
{
    /**
     * 内部统一处理文本的方法：处理颜色 + PAPI解析
     */
    private static String parse(String text) {
        if (text == null) return null;

        // 1. 基础颜色替换
        String processed = text.replace("&", "§");

        // 2. 尝试 PAPI 解析 (传入 null 玩家)
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            processed = PlaceholderAPI.setPlaceholders(null, processed);
        }

        return processed;
    }

    /**
     * 内部统一处理 Lore 的方法
     */
    private static List<String> parseLore(String lore) {
        if (lore == null) return null;
        String[] split = lore.split("#");
        List<String> list = new ArrayList<>();
        for (String line : split) {
            list.add(parse(line));
        }
        return list;
    }

    public static ItemStack create(final Material m, final Integer number, final String nume, final String lore) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        im.setDisplayName(parse(nume));
        if (lore != null) {
            im.setLore(parseLore(lore));
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
        im.setDisplayName(parse(nume));
        is.setItemMeta(im);
        return is;
    }

    public static ItemStack create(final Material m, final int number, final int variant, final String nume) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        if (im != null) {
            im.setCustomModelData(variant);
            im.setDisplayName(parse(nume));
            is.setItemMeta(im);
        }
        return is;
    }

    public static ItemStack createItem(final Material material, final Color color, final String nume) {
        final ItemStack is = new ItemStack(material);
        final LeatherArmorMeta meta = (LeatherArmorMeta)is.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            meta.setDisplayName(parse(nume));
            is.setItemMeta(meta);
        }
        return is;
    }

    public static ItemStack create(final Material m, final int number, final int variant, final String nume, final String lore) {
        final ItemStack is = new ItemStack(m, number);
        final ItemMeta im = is.getItemMeta();
        if (im != null) {
            im.setCustomModelData(variant);
            im.setDisplayName(parse(nume));
            im.setLore(parseLore(lore));
            is.setItemMeta(im);
        }
        return is;
    }
}
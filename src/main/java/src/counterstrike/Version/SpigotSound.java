package src.counterstrike.Version;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jetbrains.annotations.Nullable;

public enum SpigotSound {
    SPLASH(new String[] { "entity.bobber.splash", "SPLASH" }),
    ANVIL_USE(new String[] { "block.anvil.use", "ANVIL_USE" }),
    ORB_PICKUP(new String[] { "entity.experience_orb.pickup", "ORB_PICKUP" }),
    GHAST_FIREBALL(new String[] { "entity.ghast.shoot", "GHAST_FIREBALL" }),
    LEVEL_UP(new String[] { "entity.player.levelup", "LEVEL_UP" }),
    CLICK(new String[] { "ui.button.click", "CLICK" }),
    EXPLODE(new String[] { "entity.generic.explode", "EXPLODE" }),
    NOTE_STICKS(new String[] { "block.note_block.snare", "NOTE_STICKS" }),
    ENTITY_LIGHTNING_IMPACT(new String[] { "entity.lightning_bolt.impact", "AMBIENCE_THUNDER" }),
    ENDERMAN_TELEPORT(new String[] { "entity.enderman.teleport", "ENDERMAN_TELEPORT" }),
    ITEM_PICKUP(new String[] { "entity.item.pickup", "ITEM_PICKUP" }),
    NOTE_PLING(new String[] { "block.note_block.pling", "NOTE_PLING" }),
    SLIME_WALK(new String[] { "entity.slime.jump", "SLIME_WALK" });

    private final Sound sound;

    private SpigotSound(final String[] sounds) {
        Sound found = null;
        for (final String name : sounds) {
            found = matchSound(name);
            if (found != null) break;
        }
        this.sound = found;
    }

    /**
     * 1.21.1+ 最安全的匹配方式：避开 getKey() 和 name()
     */
    @Nullable
    private static Sound matchSound(String name) {
        if (name == null || name.isEmpty()) return null;

        // 1. 尝试将旧版大写格式转换为现代命名空间格式 (例如 CLICK -> click)
        String cleanedName = name.toLowerCase().replace("_", ".");

        // 2. 直接通过 Registry 检索 NamespacedKey
        // 这是目前最推荐的方式，不依赖于 Sound 对象实例的方法，而是依赖于注册表检索
        Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(cleanedName));
        if (s != null) return s;

        // 3. 如果还是找不到（针对某些不规则的旧名称），尝试原始名称检索
        return Registry.SOUNDS.get(NamespacedKey.minecraft(name.toLowerCase()));
    }

    public Sound getSound() {
        return this.sound;
    }
}
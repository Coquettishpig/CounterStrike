package src.counterstrike.Version;

import org.bukkit.Sound;

public enum SpigotSound
{
    SPLASH(new String[] { "SPLASH", "ENTITY_BOBBER_SPLASH" }), 
    ANVIL_USE(new String[] { "ANVIL_USE", "BLOCK_ANVIL_USE" }), 
    ORB_PICKUP(new String[] { "ORB_PICKUP", "ENTITY_EXPERIENCE_ORB_PICKUP" }), 
    GHAST_FIREBALL(new String[] { "GHAST_FIREBALL", "ENTITY_GHAST_SHOOT" }), 
    LEVEL_UP(new String[] { "LEVEL_UP", "ENTITY_PLAYER_LEVELUP" }), 
    CLICK(new String[] { "CLICK", "UI_BUTTON_CLICK" }), 
    EXPLODE(new String[] { "EXPLODE", "ENTITY_GENERIC_EXPLODE" }), 
    NOTE_STICKS(new String[] { "NOTE_STICKS", "BLOCK_NOTE_SNARE" }), 
    ENTITY_LIGHTNING_IMPACT(new String[] { "AMBIENCE_THUNDER", "ENTITY_LIGHTNING_IMPACT" }), 
    ENDERMAN_TELEPORT(new String[] { "ENDERMAN_TELEPORT", "ENTITY_ENDERMEN_TELEPORT" }), 
    ITEM_PICKUP(new String[] { "ITEM_PICKUP", "ENTITY_ITEM_PICKUP" }), 
    NOTE_PLING(new String[] { "NOTE_PLING", "BLOCK_NOTE_PLING" }), 
    SLIME_WALK(new String[] { "SLIME_WALK", "ENTITY_SLIME_JUMP" });
    
    private Sound sound;
    
    private SpigotSound(final String[] sounds) {
        for (final String name : sounds) {
            for (final Sound sound : Sound.values()) {
                if (sound.name().equals(name)) {
                    this.sound = sound;
                    break;
                }
            }
        }
    }
    
    public Sound getSound() {
        return this.sound;
    }

}

package boomcow.minezero;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSoundEvents {
    public static final Identifier DEATH_CHIME_ID = Identifier.of(MineZeroMain.MOD_ID, "death_chime");
    public static final Identifier ALT_DEATH_CHIME_ID = Identifier.of(MineZeroMain.MOD_ID, "alt_death_chime");
    public static final Identifier EMPTY_SOUND_ID = Identifier.of(MineZeroMain.MOD_ID, "empty_sound");
    public static final Identifier FLUTE_CHIME_ID = Identifier.of(MineZeroMain.MOD_ID, "flute_chime");
    public static final SoundEvent DEATH_CHIME = SoundEvent.of(DEATH_CHIME_ID);
    public static final SoundEvent ALT_DEATH_CHIME = SoundEvent.of(ALT_DEATH_CHIME_ID);
    public static final SoundEvent EMPTY_SOUND = SoundEvent.of(EMPTY_SOUND_ID);
    public static final SoundEvent FLUTE_CHIME = SoundEvent.of(FLUTE_CHIME_ID);

    /**
     * Registers the sound events with Minecraft's sound event registry.
     * This method should be called once during your mod's initialization (e.g., in onInitialize).
     */
    public static void registerSoundEvents() {
        Registry.register(Registries.SOUND_EVENT, DEATH_CHIME_ID, DEATH_CHIME);
        Registry.register(Registries.SOUND_EVENT, ALT_DEATH_CHIME_ID, ALT_DEATH_CHIME);
        Registry.register(Registries.SOUND_EVENT, EMPTY_SOUND_ID, EMPTY_SOUND);
        Registry.register(Registries.SOUND_EVENT, FLUTE_CHIME_ID, FLUTE_CHIME);

        MineZeroMain.LOGGER.info("MineZero sound events registered.");
    }
}
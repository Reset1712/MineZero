package boomcow.minezero; // Your package

import net.minecraft.registry.Registries; // Yarn mapping for the RegistryKey container
import net.minecraft.registry.Registry;    // Yarn mapping for the Registry class itself
import net.minecraft.sound.SoundEvent;    // Yarn mapping
import net.minecraft.util.Identifier;     // Yarn mapping

public class ModSoundEvents {

    // --- Define Identifiers for your sounds ---
    public static final Identifier DEATH_CHIME_ID = Identifier.of(MineZeroMain.MOD_ID, "death_chime");
    public static final Identifier ALT_DEATH_CHIME_ID = Identifier.of(MineZeroMain.MOD_ID, "alt_death_chime");
    public static final Identifier EMPTY_SOUND_ID = Identifier.of(MineZeroMain.MOD_ID, "empty_sound");
    public static final Identifier FLUTE_CHIME_ID = Identifier.of(MineZeroMain.MOD_ID, "flute_chime");

    // --- Create SoundEvent instances ---
    // SoundEvent.of(Identifier) is the direct equivalent for creating a basic sound event.
    // The "variableRange" aspect is primarily controlled by your sounds.json definitions.
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
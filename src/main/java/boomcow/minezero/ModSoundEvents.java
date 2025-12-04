package boomcow.minezero;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MineZero.MODID)
public class ModSoundEvents {

    // Public static fields to be populated during registration
    public static SoundEvent DEATH_CHIME;
    public static SoundEvent ALT_DEATH_CHIME;
    public static SoundEvent EMPTY_SOUND;
    public static SoundEvent FLUTE_CHIME;

    /**
     * Event handler for registering SoundEvents.
     * This method is called automatically by Forge during startup.
     */
    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        DEATH_CHIME = createSound("death_chime");
        ALT_DEATH_CHIME = createSound("alt_death_chime");
        EMPTY_SOUND = createSound("empty_sound");
        FLUTE_CHIME = createSound("flute_chime");

        event.getRegistry().registerAll(
                DEATH_CHIME,
                ALT_DEATH_CHIME,
                EMPTY_SOUND,
                FLUTE_CHIME
        );
    }

    /**
     * Helper method to create a SoundEvent and set its registry name.
     */
    private static SoundEvent createSound(String name) {
        ResourceLocation location = new ResourceLocation(MineZero.MODID, name);
        SoundEvent sound = new SoundEvent(location);
        sound.setRegistryName(location);
        return sound;
    }
}
package boomcow.minezero;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSoundEvents {
    // Replace "minezero" with your mod's lowercase mod ID if it's different.
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MineZero.MODID);

    public static final RegistryObject<SoundEvent> DEATH_CHIME = SOUND_EVENTS.register("death_chime",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MineZero.MODID, "death_chime")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}

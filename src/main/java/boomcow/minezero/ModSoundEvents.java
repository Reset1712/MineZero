package boomcow.minezero;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MineZero.MODID);

    // Use ResourceLocation.fromNamespaceAndPath(namespace, path)
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATH_CHIME = SOUND_EVENTS.register("death_chime",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "death_chime")));

    public static final DeferredHolder<SoundEvent, SoundEvent> ALT_DEATH_CHIME = SOUND_EVENTS.register("alt_death_chime",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "alt_death_chime")));

    public static final DeferredHolder<SoundEvent, SoundEvent> EMPTY_SOUND = SOUND_EVENTS.register("empty_sound",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "empty_sound")));

    public static final DeferredHolder<SoundEvent, SoundEvent> FLUTE_CHIME = SOUND_EVENTS.register("flute_chime",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MineZero.MODID, "flute_chime")));

    // As mentioned, this static register method is likely redundant if
    // ModSoundEvents.SOUND_EVENTS.register(modEventBus); is called in MineZero's constructor.
    /*
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
    */
}
package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZetterSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Zetter.MOD_ID);

    /**
     * Chalk dragging on slate.
     *
     * Declared as streaming in sounds.json, which is what lets the client hand the
     * sound engine a generator in place of a file — see ScratchAudioStream. The ogg
     * behind it is never normally decoded, but it has to exist, and it is a rendering
     * of the same voice so that anything going wrong falls back to a loop of roughly
     * the right sound rather than to silence.
     */
    public static final RegistryObject<SoundEvent> CHALK_SCRATCH = SOUNDS.register(
        "chalk_scratch",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zetter.MOD_ID, "chalk_scratch"))
    );

    /**
     * The chalk first touching the board.
     *
     * Sampled rather than made up, and the only part of this that is. A transient is
     * what a model built for a continuous scrape is worst at, and a recording of one
     * is cheap — five of them, picked at random by the sound engine.
     *
     * Cut from a recording by soundud3, used under CC BY-NC 4.0.
     */
    public static final RegistryObject<SoundEvent> CHALK_IMPACT = SOUNDS.register(
        "chalk_impact",
        () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Zetter.MOD_ID, "chalk_impact"))
    );

    public static void init(IEventBus bus) {
        SOUNDS.register(bus);
    }
}

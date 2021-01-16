package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.google.common.collect.Lists;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.GameData;

import java.util.List;

public class ModLockSounds {
    private static final List<SoundEvent> SOUNDS = Lists.newArrayList();

    public static final SoundEvent BLOCK_DOOR_LOCK = register("block.door.lock");
    public static final SoundEvent BLOCK_DOOR_UNLOCK = register("block.door.unlock");

    public static SoundEvent register(String name) {
        ResourceLocation loc = new ResourceLocation(ImmersiveMp.MOD_ID, name);
        SoundEvent event = new SoundEvent(loc);
        SOUNDS.add(event);
        return event;
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void registerItems(final RegistryEvent.Register<SoundEvent> event)
    {
        SOUNDS.forEach(soundEvent -> event.getRegistry().register(soundEvent));
        SOUNDS.clear();
    }
}
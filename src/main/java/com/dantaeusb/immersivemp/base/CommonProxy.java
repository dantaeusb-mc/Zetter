package com.dantaeusb.immersivemp.base;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.ModLockBlocks;
import com.dantaeusb.immersivemp.locks.core.ModLockGameEvents;
import com.dantaeusb.immersivemp.locks.core.ModLockIMCEvents;
import com.dantaeusb.immersivemp.locks.core.ModLockTileEntities;
import com.dantaeusb.immersivemp.locks.item.crafting.LockingRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;

public class CommonProxy {
    public void start() {
        ForgeRegistries.RECIPE_SERIALIZERS.register(LockingRecipe.SERIALIZER);

        ImmersiveMp.LOG.warn("Registered locking recipes");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        registerListeners(modEventBus);
    }


    public void registerListeners(IEventBus modEventBus) {
        modEventBus.addListener(this::processIMC);
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        ImmersiveMp.LOG.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
}

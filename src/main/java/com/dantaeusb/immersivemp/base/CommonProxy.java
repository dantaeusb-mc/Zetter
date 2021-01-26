package com.dantaeusb.immersivemp.base;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerProvider;
import com.dantaeusb.immersivemp.locks.core.*;
import com.dantaeusb.immersivemp.locks.item.crafting.LockingRecipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;

public class CommonProxy {
    public void start() {
        ForgeRegistries.RECIPE_SERIALIZERS.register(LockingRecipe.SERIALIZER);

        IEventBus modEventBus = ImmersiveMp.MOD_EVENT_BUS;
        registerListeners(modEventBus);
    }


    public void registerListeners(IEventBus modEventBus) {
        modEventBus.addListener(this::processIMC);
        modEventBus.addListener(this::onCommonSetupEvent);
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        ImmersiveMp.LOG.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    private void onCommonSetupEvent(FMLCommonSetupEvent event) {
        CanvasTrackerCapability.register();
    }
}

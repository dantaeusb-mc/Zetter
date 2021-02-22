package com.dantaeusb.zetter.base;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

import java.util.stream.Collectors;

public class CommonProxy {
    public void start() {
        IEventBus modEventBus = Zetter.MOD_EVENT_BUS;
        registerListeners(modEventBus);
    }


    public void registerListeners(IEventBus modEventBus) {
        modEventBus.addListener(this::processIMC);
        modEventBus.addListener(this::onCommonSetupEvent);
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        Zetter.LOG.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }

    private void onCommonSetupEvent(FMLCommonSetupEvent event) {
        CanvasTrackerCapability.register();
    }
}

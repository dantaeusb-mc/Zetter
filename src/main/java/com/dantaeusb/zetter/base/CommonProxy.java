package com.dantaeusb.zetter.base;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;

public class CommonProxy {
    public void start() {
        IEventBus modEventBus = Zetter.MOD_EVENT_BUS;
        registerListeners(modEventBus);

        this.enqueueImc();
    }

    public void registerListeners(IEventBus modEventBus) {
        modEventBus.addListener(this::processIMC);
        modEventBus.addListener(this::onCommonSetupEvent);
    }

    private void enqueueImc() {
        InterModComms.sendTo("carryon", "blacklistEntity", () -> "zetter:custom_painting_entity");
        InterModComms.sendTo("carryon", "blacklistBlock", () -> "zetter:easel");
    }

    private void processIMC(final InterModProcessEvent event)
    {
        /*Zetter.LOG.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));*/
    }

    private void onCommonSetupEvent(FMLCommonSetupEvent event) {
        CanvasTrackerCapability.register();
    }
}

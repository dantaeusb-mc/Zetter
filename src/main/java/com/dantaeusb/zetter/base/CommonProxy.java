package com.dantaeusb.zetter.base;

import com.dantaeusb.zetter.Zetter;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

public class CommonProxy {
    public void start() {
        IEventBus modEventBus = Zetter.MOD_EVENT_BUS;
        registerListeners(modEventBus);

        this.enqueueImc();

        Minecraft test = Minecraft.getInstance();
    }

    public void registerListeners(IEventBus modEventBus) {
        modEventBus.addListener(this::processIMC);
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
}

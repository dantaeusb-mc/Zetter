package com.dantaeusb.immersivemp.base;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.ModLockBlocks;
import com.dantaeusb.immersivemp.locks.item.crafting.LockingRecipe;
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

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        registerListeners(bus);
    }

    public void registerListeners(IEventBus bus) {
        bus.addListener(this::enqueueIMC);
        bus.addListener(this::processIMC);
    }

    private void enqueueIMC(final InterModEnqueueEvent event)
    {
        ImmersiveMp.LOG.info("Sending IMCs to CarryOn");

        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.ACACIA_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.BIRCH_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.DARK_OAK_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.JUNGLE_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.OAK_LOCKABLE_DOOR::getRegistryName);
        InterModComms.sendTo("carryon", "blacklistBlock", ModLockBlocks.SPRUCE_LOCKABLE_DOOR::getRegistryName);

        // some example code to dispatch IMC to another mod
        //InterModComms.sendTo("examplemod", "helloworld", () -> { LOGGER.info("Hello world from the MDK"); return "Hello world";});
    }

    private void processIMC(final InterModProcessEvent event)
    {
        // some example code to receive and process InterModComms from other mods
        ImmersiveMp.LOG.info("Got IMC {}", event.getIMCStream().
                map(m->m.getMessageSupplier().get()).
                collect(Collectors.toList()));
    }
}

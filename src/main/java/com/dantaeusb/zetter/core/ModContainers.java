package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.container.EaselContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    public static ContainerType<EaselContainer> PAINTING;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onContainerRegistry(final RegistryEvent.Register<ContainerType<?>> event)
    {

        PAINTING = IForgeContainerType.create(EaselContainer::createContainerClientSide);
        PAINTING.setRegistryName(Zetter.MOD_ID, "painting_container");
        event.getRegistry().register(PAINTING);
    }
}

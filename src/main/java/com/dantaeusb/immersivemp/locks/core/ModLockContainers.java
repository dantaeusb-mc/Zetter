package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer;
import com.dantaeusb.immersivemp.locks.inventory.container.PaintingContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModLockContainers {
    public static ContainerType<LockTableContainer> LOCK_TABLE;
    public static ContainerType<PaintingContainer> PAINTING;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onContainerRegistry(final RegistryEvent.Register<ContainerType<?>> event)
    {
        LOCK_TABLE = IForgeContainerType.create(LockTableContainer::createContainerClientSide);
        LOCK_TABLE.setRegistryName(ImmersiveMp.MOD_ID, "lock_table_container");
        event.getRegistry().register(LOCK_TABLE);

        PAINTING = IForgeContainerType.create(PaintingContainer::createContainerClientSide);
        PAINTING.setRegistryName(ImmersiveMp.MOD_ID, "painting_container");
        event.getRegistry().register(PAINTING);
    }
}

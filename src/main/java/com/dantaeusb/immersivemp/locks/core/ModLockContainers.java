package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.EaselContainer;
import com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer;
import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModLockContainers {
    public static ContainerType<LockTableContainer> LOCK_TABLE;
    public static ContainerType<EaselContainer> PAINTING;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onContainerRegistry(final RegistryEvent.Register<ContainerType<?>> event)
    {
        LOCK_TABLE = IForgeContainerType.create(LockTableContainer::createContainerClientSide);
        LOCK_TABLE.setRegistryName(ImmersiveMp.MOD_ID, "lock_table_container");
        event.getRegistry().register(LOCK_TABLE);

        PAINTING = IForgeContainerType.create(EaselContainer::createContainerClientSide);
        PAINTING.setRegistryName(ImmersiveMp.MOD_ID, "painting_container");
        event.getRegistry().register(PAINTING);
    }
}

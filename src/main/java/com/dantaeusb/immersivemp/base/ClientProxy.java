package com.dantaeusb.immersivemp.base;

import com.dantaeusb.immersivemp.locks.client.gui.LockTableContainerScreen;
import com.dantaeusb.immersivemp.locks.client.gui.PaintingScreen;
import com.dantaeusb.immersivemp.locks.core.ModLockContainers;
import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void registerListeners(IEventBus bus) {
        super.registerListeners(bus);

        bus.addListener(this::clientSetup);
    }

    public void clientSetup(FMLClientSetupEvent event) {
        ScreenManager.registerFactory(ModLockContainers.LOCK_TABLE, LockTableContainerScreen::new);
        ScreenManager.registerFactory(ModLockContainers.PAINTING, PaintingScreen::new);
    }
}

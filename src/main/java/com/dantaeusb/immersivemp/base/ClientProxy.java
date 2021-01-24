package com.dantaeusb.immersivemp.base;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.client.gui.CanvasRenderer;
import com.dantaeusb.immersivemp.locks.client.gui.LockTableContainerScreen;
import com.dantaeusb.immersivemp.locks.client.gui.PaintingScreen;
import com.dantaeusb.immersivemp.locks.client.renderer.tileentity.EaselTileEntityRenderer;
import com.dantaeusb.immersivemp.locks.core.ModLockBlocks;
import com.dantaeusb.immersivemp.locks.core.ModLockContainers;
import com.dantaeusb.immersivemp.locks.core.ModLockTileEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
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

        RenderTypeLookup.setRenderLayer(ModLockBlocks.EASEL, RenderType.getCutout());
        ClientRegistry.bindTileEntityRenderer(ModLockTileEntities.EASEL_TILE_ENTITY, EaselTileEntityRenderer::new);

        new CanvasRenderer(Minecraft.getInstance().getTextureManager());
    }
}

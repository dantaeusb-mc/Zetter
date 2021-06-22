package com.dantaeusb.zetter.base;

import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.client.gui.ArtistTableScreen;
import com.dantaeusb.zetter.client.renderer.entity.CustomPaintingRenderer;
import com.dantaeusb.zetter.client.renderer.tileentity.EaselTileEntityRenderer;
import com.dantaeusb.zetter.core.*;
import com.dantaeusb.zetter.item.FrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
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
        ScreenManager.registerFactory(ModContainers.PAINTING, PaintingScreen::new);
        ScreenManager.registerFactory(ModContainers.ARTIST_TABLE, ArtistTableScreen::new);

        for (Item frame : ModItems.FRAMES.values()) {
            ItemModelsProperties.registerProperty(frame, new ResourceLocation("has_painting"), FrameItem::getHasPaintingPropertyOverride);
        }

        RenderTypeLookup.setRenderLayer(ModBlocks.EASEL, RenderType.getCutout());
        ClientRegistry.bindTileEntityRenderer(ModTileEntities.EASEL_TILE_ENTITY, EaselTileEntityRenderer::new);

        RenderingRegistry.registerEntityRenderingHandler(ModEntities.CUSTOM_PAINTING_ENTITY, CustomPaintingRenderer::new);

        new CanvasRenderer(Minecraft.getInstance().getTextureManager());

    }
}

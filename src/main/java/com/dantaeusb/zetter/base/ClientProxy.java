package com.dantaeusb.zetter.base;

import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.client.gui.ArtistTableScreen;
import com.dantaeusb.zetter.core.*;
import com.dantaeusb.zetter.item.FrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.item.Item;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
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
        MenuScreens.register(ModContainerMenus.PAINTING, PaintingScreen::new);
        MenuScreens.register(ModContainerMenus.ARTIST_TABLE, ArtistTableScreen::new);

        for (Item frame : ModItems.FRAMES.values()) {
            ItemProperties.register(frame, new ResourceLocation("painting"), FrameItem::getHasPaintingPropertyOverride);
            ItemProperties.register(frame, new ResourceLocation("plate"), FrameItem::getHasPaintingPropertyOverride);
        }

        ItemBlockRenderTypes.setRenderLayer(ModBlocks.EASEL, RenderType.cutout());

        new CanvasRenderer(Minecraft.getInstance().getTextureManager());

    }
}

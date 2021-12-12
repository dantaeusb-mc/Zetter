package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.gui.ArtistTableScreen;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.item.FrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSetup
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onClientSetupEvent(FMLClientSetupEvent event) {
        MenuScreens.register(ModContainerMenus.PAINTING, PaintingScreen::new);
        MenuScreens.register(ModContainerMenus.ARTIST_TABLE, ArtistTableScreen::new);

        for (Item frame : ModItems.FRAMES.values()) {
            ItemProperties.register(frame, new ResourceLocation("painting"), FrameItem::getHasPaintingPropertyOverride);
            ItemProperties.register(frame, new ResourceLocation("plate"), FrameItem::getHasPaintingPropertyOverride);
        }

        ItemBlockRenderTypes.setRenderLayer(ModBlocks.EASEL, RenderType.cutout());

        new CanvasRenderer(Minecraft.getInstance().getTextureManager());
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onImcSetupEvent(InterModEnqueueEvent event) {
        InterModComms.sendTo("carryon", "blacklistEntity", () -> "zetter:custom_painting_entity");
        InterModComms.sendTo("carryon", "blacklistBlock", () -> "zetter:easel");
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void registerListeners(InterModProcessEvent event) {

    }
}
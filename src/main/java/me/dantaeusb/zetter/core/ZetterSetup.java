package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTrackerCapability;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistryCapability;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistryProvider;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.painting.ClientPaintingToolParameters;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.client.renderer.entity.EaselEntityRenderer;
import me.dantaeusb.zetter.client.renderer.entity.FramedPaintingRenderer;
import me.dantaeusb.zetter.item.FrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.item.ItemModelsProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterSetup
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onClientSetupEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Not registering PaintingScreen as it's client-side only
            ScreenManager.register(ZetterContainerMenus.EASEL.get(), EaselScreen::new);
            ScreenManager.register(ZetterContainerMenus.ARTIST_TABLE.get(), ArtistTableScreen::new);

            for (RegistryObject<FrameItem> frame : ZetterItems.FRAMES.values()) {
                ItemModelsProperties.register(frame.get(), new ResourceLocation("painting"), FrameItem::getHasPaintingPropertyOverride);
                ItemModelsProperties.register(frame.get(), new ResourceLocation("plate"), FrameItem::getHasPaintingPropertyOverride);
            }

            new CanvasRenderer(Minecraft.getInstance().getTextureManager());
            new ClientPaintingToolParameters();

            ZetterOverlays.register();
        });
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        CanvasTrackerCapability.register();
        PaintingRegistryCapability.register();
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onImcSetupEvent(InterModEnqueueEvent event) {
        InterModComms.sendTo("carryon", "blacklistEntity", () -> "zetter:custom_painting_entity");
        InterModComms.sendTo("carryon", "blacklistEntity", () -> "zetter:easel_entity");
        InterModComms.sendTo("carryon", "blacklistBlock", () -> "zetter:easel");
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void registerListeners(InterModProcessEvent event) {

    }
}
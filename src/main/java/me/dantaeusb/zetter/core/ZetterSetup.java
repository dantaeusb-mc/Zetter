package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.painting.ClientPaintingToolParameters;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.item.FrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterSetup
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onClientSetupEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Not registering PaintingScreen as it's client-side only

            MenuScreens.register(ZetterContainerMenus.EASEL.get(), EaselScreen::new);
            MenuScreens.register(ZetterContainerMenus.ARTIST_TABLE.get(), ArtistTableScreen::new);

            for (RegistryObject<FrameItem> frame : ZetterItems.FRAMES.values()) {
                ItemProperties.register(frame.get(), new ResourceLocation("painting"), FrameItem::getHasPaintingPropertyOverride);
                ItemProperties.register(frame.get(), new ResourceLocation("plate"), FrameItem::getHasPaintingPropertyOverride);
            }

            ItemBlockRenderTypes.setRenderLayer(ZetterBlocks.EASEL.get(), RenderType.cutout());

            new CanvasRenderer(Minecraft.getInstance().getTextureManager());
            new ClientPaintingToolParameters();
        });
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
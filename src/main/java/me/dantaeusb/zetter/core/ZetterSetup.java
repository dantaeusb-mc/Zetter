package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.painting.ClientPaintingToolParameters;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.item.FrameItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.event.lifecycle.InterModProcessEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

@net.neoforged.fml.common.EventBusSubscriber(modid = me.dantaeusb.zetter.Zetter.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public class ZetterSetup
{
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onClientSetupEvent(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Not registering PaintingScreen as it's client-side only

            // @todo: [CRIT] Broke icons with paintings!
            for (DeferredHolder<Item, FrameItem> frame : ZetterItems.FRAMES.values()) {
                ItemProperties.register(frame.get(), ResourceLocation.parse("painting"), FrameItem::getHasPaintingPropertyOverride);
                ItemProperties.register(frame.get(), ResourceLocation.parse("plate"), FrameItem::getHasPaintingPropertyOverride);
            }

            new ClientPaintingToolParameters();
        });
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(ZetterContainerMenus.EASEL.get(), EaselScreen::new);
        event.register(ZetterContainerMenus.ARTIST_TABLE.get(), ArtistTableScreen::new);
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onImcSetupEvent(InterModEnqueueEvent event) {
        InterModComms.sendTo("carryon", "blacklistEntity", () -> "zetter:custom_painting_entity");
        InterModComms.sendTo("carryon", "blacklistEntity", () -> "zetter:easel_entity");
        InterModComms.sendTo("carryon", "blacklistBlock", () -> "zetter:easel");
    }


    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ZetterBlockEntities.ARTIST_TABLE_BLOCK_ENTITY.get(),
                (be, side) -> be.getArtistTableGridContainer()
        );
        event.registerEntity(
                Capabilities.ItemHandler.ENTITY,
                ZetterEntities.EASEL_ENTITY.get(),
                (entity, context) -> entity.getEaselContainer()
        );
    }
}
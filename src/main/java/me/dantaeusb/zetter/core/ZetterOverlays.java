package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import java.util.HashMap;

@net.neoforged.fml.common.EventBusSubscriber(modid = me.dantaeusb.zetter.Zetter.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ZetterOverlays {
    /**
     * Storage for all overlays
     * We cannot store overlays directly in types as it causes classloader
     * to possibly load client-only classes on server
     *
     * Also I discourage future me from rewriting this,
     * because it seems like with Java type system we cannot
     * match the overlay expected type with the event type on generics
     *
     * So relying on event handlers for every overlay by every mod
     * which could use this system is seemingly the only way to go
     */
    public static final HashMap<ResourceLocation, CanvasOverlay<?>>  OVERLAYS = new HashMap<>();

    public static final String PAINTING_INFO_OVERLAY = "painting_info";

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        PaintingInfoOverlay overlay = new PaintingInfoOverlay();
        OVERLAYS.put(PaintingData.OVERLAY_KEY, overlay);
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath(Zetter.MOD_ID, overlay.getId()), overlay);
    }
}

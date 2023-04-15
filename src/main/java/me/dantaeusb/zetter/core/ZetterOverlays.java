package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import net.minecraft.client.Minecraft;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
    public static final HashMap<ResourceLocation, CanvasOverlay<?>> OVERLAYS = new HashMap<>();

    public static final String PAINTING_INFO_OVERLAY = "painting_info";

    @SubscribeEvent
    public static void onRenderOverlays(RenderGameOverlayEvent.Post event) {
        if(event.getType().equals(RenderGameOverlayEvent.ElementType.ALL)) {
            OVERLAYS.values().forEach(overlay -> overlay.render(Minecraft.getInstance().gui, event.getMatrixStack(), event.getPartialTicks(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight()));
        }

    }

    public static void register() {
        PaintingInfoOverlay overlay = new PaintingInfoOverlay();
        OVERLAYS.put(PaintingData.OVERLAY_KEY, overlay);
    }
}

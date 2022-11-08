package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterClientModEvents {
    /**
     * Handle event when canvas is viewed. Because canvas types are
     * extendable, we
     * @param event
     */
    @SubscribeEvent
    public static void onCanvasViewed(CanvasViewEvent event) {
        if (event.canvasData instanceof CanvasData canvasCanvasData) {
            ClientHelper.openCanvasScreen(event.player, event.canvasCode, canvasCanvasData, event.hand);
            event.setCanceled(true);
        } else if (event.canvasData instanceof PaintingData paintingData) {
            ClientHelper.openPaintingScreen(event.player, event.canvasCode, paintingData, event.hand);
            event.setCanceled(true);
        }
    }
}

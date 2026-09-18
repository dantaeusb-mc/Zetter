package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.storage.PaintingData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = Zetter.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ZetterModEvents {
    /**
     * Handle canvas registration on event, some menus/screens
     * might need to update
     *
     * We check if player is using some menus and try to update
     * canvas through those menus.
     *
     * @param event
     */
    @SubscribeEvent
    public static void onCanvasPostRegistered(CanvasRegisterEvent.Post event) {
        if (!event.level.isClientSide() && event.canvasData instanceof PaintingData) {
            PaintingRegistry registry = Helper.getLevelPaintingRegistry(event.level);
            registry.addPaintingCanvasCode(event.canvasCode);
        }
    }
}

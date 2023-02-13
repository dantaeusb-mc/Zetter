package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterOverlays {
    public static PaintingInfoOverlay PAINTING_INFO = new PaintingInfoOverlay();

    @SubscribeEvent
    public static void onRenderOverlays(RenderGameOverlayEvent.Post event) {
        PAINTING_INFO.render(Minecraft.getInstance().gui, event.getMatrixStack(), event.getPartialTicks(), event.getWindow().getWidth(), event.getWindow().getHeight());
    }
}

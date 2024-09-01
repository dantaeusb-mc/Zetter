package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.tooltip.CanvasTooltipRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ZetterTooltips {
  @SubscribeEvent
  public static void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
    event.register(CanvasTooltipRenderer.CanvasComponent.class, CanvasTooltipRenderer::new);
  }
}

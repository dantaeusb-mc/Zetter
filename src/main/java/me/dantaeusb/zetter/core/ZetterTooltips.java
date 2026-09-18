package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.tooltip.CanvasTooltipRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber(modid = me.dantaeusb.zetter.Zetter.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = net.neoforged.api.distmarker.Dist.CLIENT)
public class ZetterTooltips {
  @SubscribeEvent
  public static void registerClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
    event.register(CanvasTooltipRenderer.CanvasComponent.class, CanvasTooltipRenderer::new);
  }
}

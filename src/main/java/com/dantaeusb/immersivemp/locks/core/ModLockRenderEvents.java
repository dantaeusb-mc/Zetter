package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.client.gui.CanvasRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModLockRenderEvents {
    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.RenderTickEvent event) {
        Minecraft test = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START && Minecraft.getInstance().world != null) {
            CanvasRenderer.getInstance().update(event.renderTickTime);
        }
    }
}

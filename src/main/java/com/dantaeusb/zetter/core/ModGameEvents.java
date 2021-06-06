package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModGameEvents {
    @SubscribeEvent
    public static void onPlayerDisconnected(PlayerEvent.PlayerLoggedOutEvent event) {
        PlayerEntity player = event.getPlayer();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getWorldCanvasTracker(player.world);

        canvasTracker.stopTrackingAllCanvases(player.getUniqueID());
    }

    @SubscribeEvent
    public static void tickCanvasTracker(TickEvent.ServerTickEvent event) {
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getWorldCanvasTracker(ServerLifecycleHooks.getCurrentServer().func_241755_D_());
        canvasTracker.tick();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().world != null) {
            CanvasRenderer.getInstance().update(Util.milliTime());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();

        if(!event.getWorld().isRemote || event.getPlayer().isDiscrete() || event.isCanceled() || event.getResult() == Event.Result.DENY || event.getUseBlock() == Event.Result.DENY) {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
    }
}

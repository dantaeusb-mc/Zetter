package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.menu.EaselMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGameEvents {
    @SubscribeEvent
    public static void onPlayerDisconnected(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = (Player) event.getEntity();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(player.level);

        canvasTracker.stopTrackingAllCanvases(player.getUUID());
    }

    @SubscribeEvent
    public static void tickCanvasTracker(TickEvent.ServerTickEvent event) {
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(ServerLifecycleHooks.getCurrentServer().overworld());
        canvasTracker.tick();
    }

    /**
     * @todo: [MED] Do we really need that hook here? It might be called very frequently
     * @param event
     */
    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null) {
            CanvasRenderer.getInstance().update(Util.getMillis());
        }

        ZetterOverlays.PAINTING_INFO.tick();
    }

    /**
     * We need this because we need to start flow
     * only when container is opened and initialized
     *
     * Server-only
     * @param event
     */
    @SubscribeEvent
    public static void onPlayerContainerOpened(PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof EaselMenu easelMenu) {
            easelMenu.getState().addPlayer((Player) event.getEntity());
        }
    }

    /**
     * We need this because we need to start flow
     * only when container is opened and initialized
     *
     * Server-only
     * @param event
     */
    @SubscribeEvent
    public static void onPlayerContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getContainer() instanceof EaselMenu easelMenu) {
            easelMenu.getState().removePlayer((Player) event.getEntity());
        }
    }
}

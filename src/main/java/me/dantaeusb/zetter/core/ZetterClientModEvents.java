package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.event.PaintingInfoOverlayEvent;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselMenu;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

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
    public static void onCanvasPreRegistered(CanvasRegisterEvent.Pre event) {
        if (!event.level.isClientSide()) {
            return;
        }

        String canvasCode = event.canvasCode;
        AbstractCanvasData canvasData = event.canvasData;
        long timestamp = event.timestamp;

        Player player = Minecraft.getInstance().player;

        if (canvasData instanceof CanvasData canvasCanvasData) {
            // Initialize canvas if client had no canvas loaded when it was updated
            if (player.containerMenu instanceof EaselMenu easelMenu) {
                String canvasItemCode = easelMenu.getCanvasItemCode();
                // If it's the same canvas player is editing
                if (canvasItemCode != null && canvasItemCode.equals(canvasCode)) {
                    // Pushing changes that were added after sync packet was created
                    if (easelMenu.handleCanvasSync(canvasCode, canvasCanvasData, timestamp)) {
                        // Easel does own registration with latest changes applied
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

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
        if (!event.level.isClientSide()) {
            return;
        }

        String canvasCode = event.canvasCode;
        AbstractCanvasData canvasData = event.canvasData;
        long timestamp = event.timestamp;

        Player player = Minecraft.getInstance().player;

        if (canvasData instanceof CanvasData canvasCanvasData) {
            if  (player.containerMenu instanceof ArtistTableMenu artistTableMenu) {
                if (artistTableMenu.handleCanvasSync(canvasCode, canvasCanvasData, timestamp)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Every other overlay should do that too, so they
     * won't overlap
     * @param event
     */
    @SubscribeEvent
    public static void overlayViewEvent(PaintingInfoOverlayEvent event) {
        ZetterOverlays.PAINTING_INFO.hide();
    }

    /**
     * Prepare textures for default canvases and load them into memory
     * for quick reference
     * @param event
     */
    @SubscribeEvent
    public static void initializeDefaultTextures(ClientPlayerNetworkEvent.LoggingIn event) {
        for (Map.Entry<String, CanvasData> defaultCanvasDataEntry : CanvasData.DEFAULTS.entrySet()) {
            Helper.getLevelCanvasTracker(event.getPlayer().getLevel()).registerCanvasData(
                defaultCanvasDataEntry.getKey(),
                defaultCanvasDataEntry.getValue()
            );
        }
    }

    /**
     * We don't need to keep textures when we're in menus
     * @param event
     */
    @SubscribeEvent
    public static void unloadDefaultTextures(ClientPlayerNetworkEvent.LoggingOut event) {
        // When minecraft loads world and closes game it triggers event with no player
        if (event.getPlayer() == null) {
            return;
        }

        for (Map.Entry<String, CanvasData> defaultCanvasDataEntry : CanvasData.DEFAULTS.entrySet()) {
            Helper.getLevelCanvasTracker(event.getPlayer().getLevel()).unregisterCanvasData(
                defaultCanvasDataEntry.getKey()
            );
        }
    }
}

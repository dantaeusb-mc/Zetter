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
import net.minecraft.entity.player.PlayerEntity;
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
        if (event.canvasData instanceof CanvasData) {
            ClientHelper.openCanvasScreen(event.player, event.canvasCode, (CanvasData) event.canvasData, event.hand);
            event.setCanceled(true);
        } else if (event.canvasData instanceof PaintingData) {
            ClientHelper.openPaintingScreen(event.player, event.canvasCode, (PaintingData) event.canvasData, event.hand);
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

        PlayerEntity player = Minecraft.getInstance().player;

        if (canvasData instanceof CanvasData) {
            // Initialize canvas if client had no canvas loaded when it was updated
            if (player.containerMenu instanceof EaselMenu) {
                EaselMenu easelMenu = (EaselMenu) player.containerMenu;
                String canvasItemCode = easelMenu.getCanvasItemCode();
                // If it's the same canvas player is editing
                if (canvasItemCode != null && canvasItemCode.equals(canvasCode)) {
                    // Pushing changes that were added after sync packet was created
                    if (easelMenu.handleCanvasSync(canvasCode, (CanvasData) canvasData, timestamp)) {
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

        PlayerEntity player = Minecraft.getInstance().player;

        if (canvasData instanceof CanvasData) {
            if  (player.containerMenu instanceof ArtistTableMenu) {
                if (((ArtistTableMenu) player.containerMenu).handleCanvasSync(canvasCode, (CanvasData) canvasData, timestamp)) {
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
    public static void initializeDefaultTextures(ClientPlayerNetworkEvent.LoggedInEvent event) {
        for (Map.Entry<String, CanvasData> defaultCanvasDataEntry : CanvasData.DEFAULTS.entrySet()) {
            Helper.getLevelCanvasTracker(event.getPlayer().level).registerCanvasData(
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
    public static void unloadDefaultTextures(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        // When minecraft loads world and closes game it triggers event with no player
        if (event.getPlayer() == null) {
            return;
        }

        for (Map.Entry<String, CanvasData> defaultCanvasDataEntry : CanvasData.DEFAULTS.entrySet()) {
            Helper.getLevelCanvasTracker(event.getPlayer().level).unregisterCanvasData(
                defaultCanvasDataEntry.getKey()
            );
        }
    }
}

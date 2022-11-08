package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import me.dantaeusb.zetter.network.packet.SCanvasSnapshotSync;
import me.dantaeusb.zetter.network.packet.SCanvasSyncViewMessage;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

public class ClientHandler {
    /**
     * When canvas sent from sever, update client's canvas
     * and process update on container screens
     *
     * @param packetIn
     * @param world
     */
    public static void processCanvasSync(final SCanvasSyncMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final String canvasCode = packetIn.getCanvasCode();

        final AbstractCanvasData canvasData = packetIn.getCanvasData();
        final long timestamp = packetIn.getTimestamp();

        ICanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER)
                .orElseThrow(() -> new RuntimeException("Cannot find world canvas capability"));

        /**
         * First, we check if player is using some menus and try to update
         * canvas through those menus. This is needed to avoid flashing
         * texture on client, when client still draws.
         *
         * Then, if no containers used, we do update canvas
         * directly in canvas manager.
         */
        if (canvasData instanceof CanvasData canvasCanvasData) {
            // @todo: this is weird
            // Initialize canvas if client had no canvas loaded when it was updated
            if (player.containerMenu instanceof EaselContainerMenu easelContainerMenu) {
                String canvasItemCode = easelContainerMenu.getCanvasItemCode();
                // If it's the same canvas player is editing
                if (canvasItemCode != null && canvasItemCode.equals(canvasCode)) {
                    // Pushing changes that were added after sync packet was created
                    if (easelContainerMenu.handleCanvasSync(canvasCode, canvasCanvasData, timestamp)) {
                        return;
                    }
                }
            }

            if  (player.containerMenu instanceof ArtistTableMenu artistTableMenu) {
                if (artistTableMenu.handleCanvasSync(canvasCode, canvasCanvasData, timestamp)) {
                    return;
                }
            }
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
    }

    /**
     * Process SCanvasSyncViewMessage, open screen depending on
     * the type of canvas (basic or painting)
     * @param packetIn
     * @param world
     */
    public static void processCanvasSyncView(final SCanvasSyncViewMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final String canvasCode = packetIn.getCanvasCode();

        final AbstractCanvasData canvasData = packetIn.getCanvasData();

        CanvasViewEvent event = new CanvasViewEvent(player, canvasCode, canvasData, packetIn.getHand());

        MinecraftForge.EVENT_BUS.post(event);

        processCanvasSync(packetIn, world);
    }

    public static void processSnapshotSync(final SCanvasSnapshotSync packetIn, Level world) {
        EaselEntity easel = (EaselEntity) world.getEntity(packetIn.getEaselEntityId());

        if (easel != null) {
            easel.getStateHandler().processSnapshotSyncClient(packetIn.getCanvasCode(), packetIn.getCanvasData(), packetIn.getTimestamp());
        } else {
            Zetter.LOG.warn("Unable to find entity " + packetIn.getEaselEntityId() + " disregarding canvas snapshot");
        }
    }
}

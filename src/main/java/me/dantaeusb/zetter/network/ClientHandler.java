package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.item.CanvasItem;
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

        assert packetIn.getType().equals(AbstractCanvasData.Type.CANVAS);

        final CanvasData canvasData = (CanvasData) packetIn.getCanvasData();
        final long timestamp = packetIn.getTimestamp();

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);

        // Do extra things

        // Initialize canvas if client had no canvas loaded when it was updated
        if (player.containerMenu instanceof EaselContainerMenu) {
            String canvasItemCode = ((EaselContainerMenu) player.containerMenu).getCanvasItemCode();
            // If it's the same canvas player is editing
            if (canvasItemCode != null && canvasItemCode.equals(canvasCode)) {
                // Pushing changes that were added after sync packet was created
                ((EaselContainerMenu) player.containerMenu).handleCanvasSync(canvasCode, canvasData, timestamp);
            }
        }

        if  (player.containerMenu instanceof ArtistTableMenu) {
            // If player's combining canvases
            // @todo: not sure if needed

            //((ArtistTableMenu) player.containerMenu).updateCanvasCombination();
        }
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

        if (packetIn.getType().equals(AbstractCanvasData.Type.CANVAS)) {
            final CanvasData canvasData = (CanvasData) packetIn.getCanvasData();

            CanvasItem.openScreen(player, canvasCode, canvasData, packetIn.getHand());
            processCanvasSync(packetIn, world);
        } else if (packetIn.getType().equals(AbstractCanvasData.Type.PAINTING)) {
            final PaintingData canvasData = (PaintingData) packetIn.getCanvasData();

            CanvasItem.openScreen(player, canvasCode, canvasData, packetIn.getHand());
            processPaintingSync(packetIn, world);
        }
    }

    /**
     * Painting data needs lesser amount of check
     * after being received, just update textue on
     * client's side
     *
     * @todo: better add switch for type in handler
     *
     * @param packetIn
     * @param world
     */
    public static void processPaintingSync(final SCanvasSyncMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;

        assert packetIn.getType().equals(AbstractCanvasData.Type.PAINTING);

        String canvasCode = packetIn.getCanvasCode();
        PaintingData canvasData = (PaintingData) packetIn.getCanvasData();

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
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

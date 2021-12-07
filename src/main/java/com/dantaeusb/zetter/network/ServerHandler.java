package com.dantaeusb.zetter.network;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import com.dantaeusb.zetter.menu.ArtistTableMenu;
import com.dantaeusb.zetter.core.ModCapabilities;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.menu.EaselContainerMenu;
import com.dantaeusb.zetter.network.packet.*;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

public class ServerHandler {
    /**
     * Update canvas on server-side and send update to other tracking players
     * @param packetIn
     * @param sendingPlayer
     *
     * @todo: send changes to TE, not container, as it's created per player
     */
    public static void processFrameBuffer(final CPaintingFrameBufferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof EaselContainerMenu) {
            EaselContainerMenu paintingContainer = (EaselContainerMenu)sendingPlayer.containerMenu;

            paintingContainer.processFrameBufferServer(packetIn.getFrameBuffer(), sendingPlayer.getUUID());

            /**
             * Keep it there, but it's not really useful since it's easier to sync whole canvas and keep recent
             * changes on top
             */

            /*for (PlayerEntity playerEntity : paintingContainer.getTileEntityReference().getPlayersUsing()) {
                if (playerEntity.equals(sendingPlayer)) {
                    // No need to send it back
                    continue;
                }

                SPaintingFrameBufferPacket packet = new SPaintingFrameBufferPacket(packetIn.getFrameBuffer());
                ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) playerEntity), packet);
            }*/
        }
    }

    public static void processRequestSync(final CCanvasRequestPacket packetIn, ServerPlayer sendingPlayer) {
        // Get overworld world instance
        final MinecraftServer server = sendingPlayer.getLevel().getServer();
        final Level world = server.overworld();
        final CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(ModCapabilities.CANVAS_TRACKER).orElse(null);
        final String canvasName = packetIn.getCanvasName();

        Zetter.LOG.debug("Got request to sync canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        // Notify canvas manager that player is tracking canvas from no ow
        canvasTracker.trackCanvas(sendingPlayer.getUUID(), canvasName);

        AbstractCanvasData canvasData;

        if (packetIn.getCanvasType() == AbstractCanvasData.Type.CANVAS) {
            canvasData = canvasTracker.getCanvasData(canvasName, CanvasData.class);
        } else if (packetIn.getCanvasType() == AbstractCanvasData.Type.PAINTING) {
            canvasData = canvasTracker.getCanvasData(canvasName, PaintingData.class);
        } else {
            canvasData = canvasTracker.getCanvasData(canvasName, DummyCanvasData.class);
        }

        if (canvasData == null) {
            Zetter.LOG.error("Player " + sendingPlayer + " requested non-existent canvas: " + canvasName);
            return;
        }

        if (canvasData instanceof PaintingData) {
            SPaintingSyncMessage paintingSyncMessage = new SPaintingSyncMessage(canvasName, (PaintingData) canvasData, System.currentTimeMillis());

            ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), paintingSyncMessage);
        } else {
            SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasName, canvasData, System.currentTimeMillis());

            ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncMessage);
        }
    }

    /**
     * @todo: Think about removing this
     * Not sure if it's needed, this can cause condition when canvas is unloaded while
     * other players would like to track it. Unloading on back-end should happen
     * by requests timeout and I believe this should work properly already
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processUnloadRequest(final CCanvasUnloadRequestPacket packetIn, ServerPlayer sendingPlayer) {
        // Get overworld world instance
        MinecraftServer server = sendingPlayer.getLevel().getServer();
        Level world = server.overworld();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(ModCapabilities.CANVAS_TRACKER).orElse(null);

        Zetter.LOG.debug("Got request to unload canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.stopTrackingCanvas(sendingPlayer.getUUID(), packetIn.getCanvasName());
    }

    public static void processPaletteUpdate(final CPaletteUpdatePacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof EaselContainerMenu) {
            EaselContainerMenu paintingContainer = (EaselContainerMenu)sendingPlayer.containerMenu;
            paintingContainer.setPaletteColor(packetIn.getSlotIndex(), packetIn.getColor());
        }
    }

    public static void processCreatePainting(final CUpdatePaintingPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof ArtistTableMenu) {
            ArtistTableMenu artistTableMenu = (ArtistTableMenu)sendingPlayer.containerMenu;
            artistTableMenu.updatePaintingName(packetIn.getPaintingName());
        }
    }

    public static void processBucketTool(final CCanvasBucketToolPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof EaselContainerMenu) {
            EaselContainerMenu easelContainer = (EaselContainerMenu)sendingPlayer.containerMenu;
            easelContainer.processBucketToolServer(packetIn.position, packetIn.color);
        }
    }
}

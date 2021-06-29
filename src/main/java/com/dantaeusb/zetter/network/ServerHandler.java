package com.dantaeusb.zetter.network;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.network.packet.*;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public class ServerHandler {
    /**
     * Update canvas on server-side and send update to other tracking players
     * @param packetIn
     * @param sendingPlayer
     *
     * @todo: send changes to TE, not container, as it's created per player
     */
    public static void processFrameBuffer(final CPaintingFrameBufferPacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof EaselContainer) {
            EaselContainer paintingContainer = (EaselContainer)sendingPlayer.openContainer;

            paintingContainer.processFrameBufferServer(packetIn.getFrameBuffer(), sendingPlayer.getUniqueID());

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

    public static void processRequestSync(final CCanvasRequestPacket packetIn, ServerPlayerEntity sendingPlayer) {
        // Get overworld world instance
        MinecraftServer server = sendingPlayer.getServerWorld().getServer();
        World world = server.func_241755_D_();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        Zetter.LOG.info("Got request to sync canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        // Notify canvas manager that player is tracking canvas from no ow
        canvasTracker.trackCanvas(sendingPlayer.getUniqueID(), packetIn.getCanvasName());

        AbstractCanvasData canvasData;

        if (packetIn.getCanvasType() == AbstractCanvasData.Type.CANVAS) {
            canvasData = canvasTracker.getCanvasData(packetIn.getCanvasName(), CanvasData.class);
        } else if (packetIn.getCanvasType() == AbstractCanvasData.Type.PAINTING) {
            canvasData = canvasTracker.getCanvasData(packetIn.getCanvasName(), PaintingData.class);
        } else {
            canvasData = canvasTracker.getCanvasData(packetIn.getCanvasName(), DummyCanvasData.class);
        }

        if (canvasData == null) {
            Zetter.LOG.error("Player " + sendingPlayer + " requested non-existent canvas: " + packetIn.getCanvasName());
            return;
        }

        if (canvasData instanceof PaintingData) {
            SPaintingSyncMessage paintingSyncMessage = new SPaintingSyncMessage((PaintingData) canvasData, System.currentTimeMillis());

            ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), paintingSyncMessage);
        } else {
            SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasData, System.currentTimeMillis());

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
    public static void processUnloadRequest(final CCanvasUnloadRequestPacket packetIn, ServerPlayerEntity sendingPlayer) {
        // Get overworld world instance
        MinecraftServer server = sendingPlayer.getServerWorld().getServer();
        World world = server.func_241755_D_();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        Zetter.LOG.info("Got request to unload canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.stopTrackingCanvas(sendingPlayer.getUniqueID(), packetIn.getCanvasName());
    }

    public static void processPaletteUpdate(final CPaletteUpdatePacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof EaselContainer) {
            EaselContainer paintingContainer = (EaselContainer)sendingPlayer.openContainer;
            paintingContainer.setPaletteColor(packetIn.getSlotIndex(), packetIn.getColor());
        }
    }

    public static void processCreatePainting(final CUpdatePaintingPacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof ArtistTableContainer) {
            ArtistTableContainer artistTableContainer = (ArtistTableContainer)sendingPlayer.openContainer;
            artistTableContainer.updatePaintingName(packetIn.getPaintingName());
        }
    }

    public static void processBucketTool(final CCanvasBucketToolPacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof EaselContainer) {
            EaselContainer easelContainer = (EaselContainer)sendingPlayer.openContainer;
            easelContainer.processBucketToolServer(packetIn.position, packetIn.color);
        }
    }
}

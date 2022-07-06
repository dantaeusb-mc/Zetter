package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.menu.painting.PaintingActionBuffer;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class ServerHandler {
    /**
     * Update canvas on server-side and send update to other tracking players
     * @param packetIn
     * @param sendingPlayer
     *
     * @todo: send changes to TE, not container, as it's created per player
     */
    public static void processActionBuffer(final CCanvasActionBufferPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof EaselContainerMenu) {
            EaselContainerMenu paintingMenu = (EaselContainerMenu)sendingPlayer.containerMenu;

            for (PaintingActionBuffer actionBuffer : packetIn.getPaintingActionBuffers()) {
                if (!actionBuffer.authorId.equals(sendingPlayer.getUUID())) {
                    Zetter.LOG.warn("Received action from player claimed another player UUID, ignoring");
                    return;
                }

                paintingMenu.processActionBufferServer(actionBuffer);
            }

            /**
             * Keep it there, but it's not really useful since it's easier to sync whole canvas and keep recent
             * changes on top
             */

            /*for (PlayerEntity playerEntity : paintingMenu.getTileEntityReference().getPlayersUsing()) {
                if (playerEntity.equals(sendingPlayer)) {
                    // No need to send it back
                    continue;
                }

                SPaintingFrameBufferPacket packet = new SPaintingFrameBufferPacket(packetIn.getFrameBuffer());
                ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) playerEntity), packet);
            }*/
        }
    }

    public static void processCanvasRequest(final CCanvasRequestPacket packetIn, ServerPlayer sendingPlayer) {
        // Get overworld world instance
        final MinecraftServer server = sendingPlayer.getLevel().getServer();
        final Level world = server.overworld();
        final CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
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

            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), paintingSyncMessage);
        } else {
            SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasName, canvasData, System.currentTimeMillis());

            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncMessage);
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
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);

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
            paintingContainer.setPaletteColor(packetIn.getColor(), packetIn.getSlotIndex());
        }
    }

    public static void processRenamePainting(final CRenamePaintingPacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof ArtistTableMenu) {
            ArtistTableMenu artistTableMenu = (ArtistTableMenu)sendingPlayer.containerMenu;
            artistTableMenu.updatePaintingName(packetIn.getPaintingName());
        }
    }
}

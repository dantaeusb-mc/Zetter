package com.dantaeusb.immersivemp.locks.network.handler.server;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasServerTracker;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.EaselContainer;
import com.dantaeusb.immersivemp.locks.network.packet.painting.*;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.function.Supplier;

public class PaintingServerHandler {
    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD, NOT THE SERVER THREAD
     * @param message The message
     */
    public static void handleFrameBuffer(final PaintingFrameBufferPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            ImmersiveMp.LOG.warn("CPaintingUpdatePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ImmersiveMp.LOG.warn("EntityPlayerMP was null when CPaintingUpdatePacket was received");
        }

        ctx.enqueueWork(() -> processFrameBuffer(packetIn, sendingPlayer));
    }

    public static void processFrameBuffer(final PaintingFrameBufferPacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof EaselContainer) {
            EaselContainer paintingContainer = (EaselContainer)sendingPlayer.openContainer;

            paintingContainer.processFrameBuffer(packetIn.getFrameBuffer(), sendingPlayer.getUniqueID());
        }
    }

    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD, NOT THE SERVER THREAD
     * @param message The message
     */
    public static void handleRequestSync(final CanvasRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            ImmersiveMp.LOG.warn("CRequestSyncPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ImmersiveMp.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> processRequestSync(packetIn, sendingPlayer));
    }

    public static void processRequestSync(final CanvasRequestPacket packetIn, ServerPlayerEntity sendingPlayer) {
        // Get overworld world instance
        MinecraftServer server = sendingPlayer.getServerWorld().getServer();
        World world = server.func_241755_D_();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        ImmersiveMp.LOG.info("Got request to sync canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            ImmersiveMp.LOG.error("Cannot find world canvas capability");
            return;
        }

        // Notify canvas manager that player is tracking canvas from no ow
        canvasTracker.trackCanvas(sendingPlayer.getUniqueID(), packetIn.getCanvasName());

        CanvasData canvasData = canvasTracker.getCanvasData(packetIn.getCanvasName());

        if (canvasData == null) {
            ImmersiveMp.LOG.error("Player " + sendingPlayer + " requested non-existent canvas: " + packetIn.getCanvasName());
            return;
        }

        SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasData, System.currentTimeMillis());

        ModLockNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncMessage);
    }

    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD, NOT THE SERVER THREAD
     * @param message The message
     */
    public static void handleUnloadRequest(final CanvasUnloadRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            ImmersiveMp.LOG.warn("CRequestSyncPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ImmersiveMp.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> processUnloadRequest(packetIn, sendingPlayer));
    }

    public static void processUnloadRequest(final CanvasUnloadRequestPacket packetIn, ServerPlayerEntity sendingPlayer) {
        // Get overworld world instance
        MinecraftServer server = sendingPlayer.getServerWorld().getServer();
        World world = server.func_241755_D_();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        ImmersiveMp.LOG.info("Got request to unload canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            ImmersiveMp.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.stopTrackingCanvas(sendingPlayer.getUniqueID(), packetIn.getCanvasName());
    }


    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD, NOT THE SERVER THREAD
     * @param message The message
     */
    public static void handlePaletteUpdate(final CPaletteUpdatePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            ImmersiveMp.LOG.warn("PaletteUpdatePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            ImmersiveMp.LOG.warn("EntityPlayerMP was null when PaletteUpdatePacket was received");
        }

        ctx.enqueueWork(() -> processPaletteUpdate(packetIn, sendingPlayer));
    }

    public static void processPaletteUpdate(final CPaletteUpdatePacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof EaselContainer) {
            EaselContainer paintingContainer = (EaselContainer)sendingPlayer.openContainer;
            paintingContainer.setPaletteColor(packetIn.getSlotIndex(), packetIn.getColor());
        }
    }
}

package com.dantaeusb.immersivemp.locks.network.handler.server;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.ICanvasTracker;
import com.dantaeusb.immersivemp.locks.client.gui.CanvasRenderer;
import com.dantaeusb.immersivemp.locks.inventory.container.EaselContainer;
import com.dantaeusb.immersivemp.locks.network.packet.painting.PaintingFrameBufferPacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.SCanvasSyncMessage;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class PaintingClientHandler {
    /**
     * Handle sync packet on client
     *
     * @param packetIn
     * @param ctxSupplier
     */
    public static void handleSync(final SCanvasSyncMessage packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.CLIENT) {
            ImmersiveMp.LOG.warn("SCanvasSyncMessage received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            ImmersiveMp.LOG.warn("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> processSync(packetIn, clientWorld.get()));
    }

    public static void processSync(final SCanvasSyncMessage packetIn, ClientWorld world) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        CanvasData canvasData = packetIn.getCanvasData();

        if (player.openContainer instanceof EaselContainer) {
            // If it's the same canvas player is editing
            if (canvasData.getName().equals(((EaselContainer) player.openContainer).getCanvasData().getName())) {
                // Pushing changes that were added after sync packet was created
                ((EaselContainer) player.openContainer).processSync(canvasData, packetIn.getTimestamp());
            }
        }

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            ImmersiveMp.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasData);
    }

    /**
     * Client frame buffer process - needed when multiple users editing painting at once
     *
     * @param packetIn
     * @param ctxSupplier
     */
    public static void handleFrameBuffer(final PaintingFrameBufferPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.CLIENT) {
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
}

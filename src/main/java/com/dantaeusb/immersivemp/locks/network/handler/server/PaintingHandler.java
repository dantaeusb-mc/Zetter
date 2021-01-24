package com.dantaeusb.immersivemp.locks.network.handler.server;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.EaselContainer;
import com.dantaeusb.immersivemp.locks.network.packet.painting.PaintingFrameBufferPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PaintingHandler {
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
}

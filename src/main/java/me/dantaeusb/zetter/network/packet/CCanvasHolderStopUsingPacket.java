package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Player closed the painting screen, so the holder can stop tracking them and
 * their palette. Without it the server only notices on the next sweep in
 * CanvasHolderEntity#checkPlayersUsing.
 */
public class CCanvasHolderStopUsingPacket {
    private final int canvasHolderId;

    public CCanvasHolderStopUsingPacket(int canvasHolderId) {
        this.canvasHolderId = canvasHolderId;
    }

    public int getCanvasHolderId() {
        return this.canvasHolderId;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CCanvasHolderStopUsingPacket readPacketData(FriendlyByteBuf networkBuffer) {
        return new CCanvasHolderStopUsingPacket(networkBuffer.readInt());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.canvasHolderId);
    }

    public static void handle(final CCanvasHolderStopUsingPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("CCanvasHolderStopUsingPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasHolderStopUsingPacket was received");
            return;
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasHolderStopUsing(packetIn, sendingPlayer));
    }

    @Override
    public String toString()
    {
        return "CCanvasHolderStopUsingPacket[canvasHolderId=" + this.canvasHolderId + "]";
    }
}

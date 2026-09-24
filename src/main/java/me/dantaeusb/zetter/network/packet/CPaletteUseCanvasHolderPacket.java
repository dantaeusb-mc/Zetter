package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Player raised a palette at a canvas holder.
 *
 * Only the hand travels. A palette is used from the hand like anything else, so the
 * server can read the stack out of it, and a client that names its own hand cannot
 * name a palette it is not holding.
 *
 * @see CChalkUseCanvasHolderPacket
 */
public class CPaletteUseCanvasHolderPacket {
    private final int canvasHolderId;
    private final InteractionHand hand;

    public CPaletteUseCanvasHolderPacket(int canvasHolderId, InteractionHand hand) {
        this.canvasHolderId = canvasHolderId;
        this.hand = hand;
    }

    public int getCanvasHolderId() {
        return this.canvasHolderId;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CPaletteUseCanvasHolderPacket readPacketData(FriendlyByteBuf networkBuffer) {
        return new CPaletteUseCanvasHolderPacket(
            networkBuffer.readInt(),
            networkBuffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
        );
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.canvasHolderId);
        networkBuffer.writeBoolean(this.hand == InteractionHand.OFF_HAND);
    }

    public static void handle(final CPaletteUseCanvasHolderPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("CPaletteUseCanvasHolderPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CPaletteUseCanvasHolderPacket was received");
            return;
        }

        ctx.enqueueWork(() -> ServerHandler.processPaletteUseCanvasHolder(packetIn, sendingPlayer));
    }

    @Override
    public String toString()
    {
        return "CPaletteUseCanvasHolderPacket[canvasHolderId=" + this.canvasHolderId + ",hand=" + this.hand + "]";
    }
}
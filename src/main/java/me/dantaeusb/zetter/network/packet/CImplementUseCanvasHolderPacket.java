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
 * Player started working on a board with whatever they are holding — chalk, a sponge.
 *
 * A palette is looked up by the id written into it, because it can be anywhere in
 * the player's inventory. A board is worked on with the hand, so the hand is all that
 * is sent and the server reads the stack out of it: a client cannot name an item it
 * is not holding.
 *
 * @see CPaletteUseCanvasHolderPacket
 */
public class CImplementUseCanvasHolderPacket {
    private final int canvasHolderId;
    private final InteractionHand hand;

    public CImplementUseCanvasHolderPacket(int canvasHolderId, InteractionHand hand) {
        this.canvasHolderId = canvasHolderId;
        this.hand = hand;
    }

    public int getCanvasHolderId() {
        return this.canvasHolderId;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static CImplementUseCanvasHolderPacket readPacketData(FriendlyByteBuf networkBuffer) {
        return new CImplementUseCanvasHolderPacket(
            networkBuffer.readInt(),
            networkBuffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
        );
    }

    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.canvasHolderId);
        networkBuffer.writeBoolean(this.hand == InteractionHand.OFF_HAND);
    }

    public static void handle(final CImplementUseCanvasHolderPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("CImplementUseCanvasHolderPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CImplementUseCanvasHolderPacket was received");
            return;
        }

        ctx.enqueueWork(() -> ServerHandler.processImplementUseCanvasHolder(packetIn, sendingPlayer));
    }

    @Override
    public String toString() {
        return "CImplementUseCanvasHolderPacket[canvasHolderId=" + this.canvasHolderId + ",hand=" + this.hand + "]";
    }
}

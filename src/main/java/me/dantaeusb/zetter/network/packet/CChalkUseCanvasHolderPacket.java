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
 * Player started drawing on a board with a stick of chalk.
 *
 * A palette is looked up by the id written into it, because it can be anywhere in
 * the player's inventory. Chalk is drawn with in hand, so the hand is all that is
 * sent and the server reads the stack out of it — a client cannot name a stick of
 * chalk it is not holding.
 *
 * @see CPaletteUseCanvasHolderPacket
 */
public class CChalkUseCanvasHolderPacket {
    private final int canvasHolderId;
    private final InteractionHand hand;

    public CChalkUseCanvasHolderPacket(int canvasHolderId, InteractionHand hand) {
        this.canvasHolderId = canvasHolderId;
        this.hand = hand;
    }

    public int getCanvasHolderId() {
        return this.canvasHolderId;
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static CChalkUseCanvasHolderPacket readPacketData(FriendlyByteBuf networkBuffer) {
        return new CChalkUseCanvasHolderPacket(
            networkBuffer.readInt(),
            networkBuffer.readBoolean() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND
        );
    }

    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.canvasHolderId);
        networkBuffer.writeBoolean(this.hand == InteractionHand.OFF_HAND);
    }

    public static void handle(final CChalkUseCanvasHolderPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("CChalkUseCanvasHolderPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CChalkUseCanvasHolderPacket was received");
            return;
        }

        ctx.enqueueWork(() -> ServerHandler.processChalkUseCanvasHolder(packetIn, sendingPlayer));
    }

    @Override
    public String toString() {
        return "CChalkUseCanvasHolderPacket[canvasHolderId=" + this.canvasHolderId + ",hand=" + this.hand + "]";
    }
}

package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CCanvasRequestViewPacket extends CCanvasRequestPacket {
    private final InteractionHand hand;

    public CCanvasRequestViewPacket(String canvasName, InteractionHand hand) {
        super(canvasName);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestViewPacket readPacketData(FriendlyByteBuf buf) {
        String canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_BYTE_LENGTH);
        byte handCode = buf.readByte();

        InteractionHand hand = InteractionHand.values()[handCode];

        return new CCanvasRequestViewPacket(canvasName, hand);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_BYTE_LENGTH);
        buf.writeByte(this.hand.ordinal());
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static void handle(final CCanvasRequestViewPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasViewRequest(packetIn, sendingPlayer));
    }
}
package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CCanvasRequestViewPacket extends CCanvasRequestPacket {
    private final Hand hand;

    public CCanvasRequestViewPacket(String canvasName, Hand hand) {
        super(canvasName);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestViewPacket readPacketData(PacketBuffer buf) {
        String canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);
        byte handCode = buf.readByte();

        Hand hand = Hand.values()[handCode];

        return new CCanvasRequestViewPacket(canvasName, hand);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_LENGTH);
        buf.writeByte(this.hand.ordinal());
    }

    public Hand getHand() {
        return this.hand;
    }

    public static void handle(final CCanvasRequestViewPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasViewRequest(packetIn, sendingPlayer));
    }
}
package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CCanvasRequestViewPacket extends CCanvasRequestPacket {
    private final InteractionHand hand;

    public CCanvasRequestViewPacket(AbstractCanvasData.Type type, String canvasName, InteractionHand hand) {
        super(type, canvasName);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestViewPacket readPacketData(FriendlyByteBuf buf) {
        AbstractCanvasData.Type type = AbstractCanvasData.Type.getTypeById(buf.readInt());
        String canvasName = buf.readUtf(64);
        byte handCode = buf.readByte();

        InteractionHand hand = InteractionHand.values()[handCode];

        return new CCanvasRequestViewPacket(type, canvasName, hand);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.type.getId());
        buf.writeUtf(this.canvasName);
        buf.writeByte(this.hand.ordinal());
    }

    public String getCanvasName() {
        return this.canvasName;
    }

    public AbstractCanvasData.Type getCanvasType() {
        return this.type;
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
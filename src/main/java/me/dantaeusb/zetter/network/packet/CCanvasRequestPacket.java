package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CCanvasRequestPacket {
    private String canvasName;
    private AbstractCanvasData.Type type;

    public CCanvasRequestPacket() {
    }

    public CCanvasRequestPacket(AbstractCanvasData.Type type, String canvasName) {
        this.type = type;
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestPacket readPacketData(FriendlyByteBuf buf) {
        CCanvasRequestPacket packet = new CCanvasRequestPacket();

        packet.type = AbstractCanvasData.Type.values()[buf.readInt()];
        packet.canvasName = buf.readUtf(32767);

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.type.ordinal());
        buf.writeUtf(this.canvasName);
    }

    public String getCanvasName() {
        return this.canvasName;
    }

    public AbstractCanvasData.Type getCanvasType() {
        return this.type;
    }

    public static void handle(final CCanvasRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasRequest(packetIn, sendingPlayer));
    }
}
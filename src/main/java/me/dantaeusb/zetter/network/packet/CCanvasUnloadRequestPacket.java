package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Absolute copy of request packet but we have to copy them cause
 * there's no way to determine which purpose packet is used for
 * unless they're different classes for some reason
 */
public class CCanvasUnloadRequestPacket {
    private String canvasName;

    public CCanvasUnloadRequestPacket() {
    }

    public CCanvasUnloadRequestPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasUnloadRequestPacket readPacketData(FriendlyByteBuf buf) {
        CCanvasUnloadRequestPacket packet = new CCanvasUnloadRequestPacket();

        packet.canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_BYTE_LENGTH);

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_BYTE_LENGTH);
    }

    public String getCanvasName() {
        return this.canvasName;
    }

    public static void handle(final CCanvasUnloadRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processUnloadRequest(packetIn, sendingPlayer));
    }
}
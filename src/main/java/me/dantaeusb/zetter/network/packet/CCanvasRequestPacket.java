package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CCanvasRequestPacket {
    public final String canvasName;

    public CCanvasRequestPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestPacket readPacketData(PacketBuffer buf) {
        String canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

        return new CCanvasRequestPacket(canvasName);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_LENGTH);
    }

    public static void handle(final CCanvasRequestPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasRequest(packetIn, sendingPlayer));
    }
}
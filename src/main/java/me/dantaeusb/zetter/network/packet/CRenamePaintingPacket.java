package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Rename painting packet used to send updated name field in
 * Artist Table screen from clent to the server
 */
public class CRenamePaintingPacket {
    private int windowId;
    private String paintingName;

    public CRenamePaintingPacket() {

    }

    public CRenamePaintingPacket(int windowId, String paintingName) {
        this.windowId = windowId;
        this.paintingName = paintingName;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CRenamePaintingPacket readPacketData(FriendlyByteBuf buf) {
        CRenamePaintingPacket packet = new CRenamePaintingPacket();

        try {
            packet.windowId = buf.readByte();
            packet.paintingName = buf.readUtf(32767);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading CCreatePaintingPacket: " + e);
            return packet;
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeByte(this.windowId);
        buf.writeUtf(this.paintingName);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public String getPaintingName() {
        return this.paintingName;
    }

    public static void handle(final CRenamePaintingPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCreatePaintingPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processRenamePainting(packetIn, sendingPlayer));
    }
}
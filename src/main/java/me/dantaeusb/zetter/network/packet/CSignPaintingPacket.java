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
public class CSignPaintingPacket {
    private int slot;
    private String paintingTitle;

    public CSignPaintingPacket() {

    }

    public CSignPaintingPacket(int slot, String paintingTitle) {
        this.slot = slot;
        this.paintingTitle = paintingTitle;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CSignPaintingPacket readPacketData(FriendlyByteBuf buf) {
        CSignPaintingPacket packet = new CSignPaintingPacket();

        try {
            packet.slot = buf.readByte();
            packet.paintingTitle = buf.readUtf(32);
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
        buf.writeByte(this.slot);
        buf.writeUtf(this.paintingTitle, 32);
    }

    public int getSlot() {
        return this.slot;
    }

    public String getPaintingTitle() {
        return this.paintingTitle;
    }

    public static void handle(final CSignPaintingPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCreatePaintingPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processSignPainting(packetIn, sendingPlayer));
    }
}
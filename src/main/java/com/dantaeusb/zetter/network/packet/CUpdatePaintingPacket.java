package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ServerHandler;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CUpdatePaintingPacket {
    private int windowId;
    private String paintingName;
    private AbstractCanvasData canvasData;

    public CUpdatePaintingPacket() {

    }

    public CUpdatePaintingPacket(int windowId, String paintingName, AbstractCanvasData canvasData) {
        this.windowId = windowId;
        this.paintingName = paintingName;
        this.canvasData = canvasData;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CUpdatePaintingPacket readPacketData(FriendlyByteBuf buf) {
        CUpdatePaintingPacket packet = new CUpdatePaintingPacket();

        try {
            packet.windowId = buf.readByte();
            packet.paintingName = buf.readUtf(32767);
            packet.canvasData = CanvasContainer.readPacketCanvasData(buf);
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
        CanvasContainer.writePacketCanvasData(buf, this.canvasData);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public String getPaintingName() {
        return this.paintingName;
    }

    public AbstractCanvasData getCanvasData() {
        return this.canvasData;
    }

    public static void handle(final CUpdatePaintingPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCreatePaintingPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCreatePainting(packetIn, sendingPlayer));
    }
}
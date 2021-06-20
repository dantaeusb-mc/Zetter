package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.network.PacketBuffer;

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
    public static CCanvasRequestPacket readPacketData(PacketBuffer buf) {
        CCanvasRequestPacket packet = new CCanvasRequestPacket();

        packet.type = AbstractCanvasData.Type.values()[buf.readInt()];
        packet.canvasName = buf.readString(32767);

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeInt(this.type.ordinal());
        buf.writeString(this.canvasName);
    }

    public String getCanvasName() {
        return this.canvasName;
    }

    public AbstractCanvasData.Type getCanvasType() {
        return this.type;
    }
}
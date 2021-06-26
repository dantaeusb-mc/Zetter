package com.dantaeusb.zetter.network.packet;

import net.minecraft.network.PacketBuffer;

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
    public static CCanvasUnloadRequestPacket readPacketData(PacketBuffer buf) {
        CCanvasUnloadRequestPacket packet = new CCanvasUnloadRequestPacket();

        packet.canvasName = buf.readString(32767);

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeString(this.canvasName);
    }

    public String getCanvasName() {
        return this.canvasName;
    }
}
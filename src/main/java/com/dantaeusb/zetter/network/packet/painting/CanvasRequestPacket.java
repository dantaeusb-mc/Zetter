package com.dantaeusb.zetter.network.packet.painting;

import net.minecraft.network.PacketBuffer;

public class CanvasRequestPacket {
    private String canvasName;

    public CanvasRequestPacket() {
    }

    public CanvasRequestPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CanvasRequestPacket readPacketData(PacketBuffer buf) {
        CanvasRequestPacket packet = new CanvasRequestPacket();

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
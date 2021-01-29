package com.dantaeusb.immersivemp.locks.network.packet.painting;

import net.minecraft.network.PacketBuffer;

/**
 * Absolute copy of request packet but we have to copy them cause
 * there's no way to determine which purpose packet is used for
 * unless they're different classes for some reason
 */
public class CanvasUnloadRequestPacket {
    private String canvasName;

    public CanvasUnloadRequestPacket() {
    }

    public CanvasUnloadRequestPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CanvasUnloadRequestPacket readPacketData(PacketBuffer buf) {
        CanvasUnloadRequestPacket packet = new CanvasUnloadRequestPacket();

        packet.canvasName = buf.readString();

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
package com.dantaeusb.zetter.network.packet;

import net.minecraft.network.PacketBuffer;

/**
 * @todo: Is that okay that we don't have classic handler here?
 */
public class SCanvasNamePacket {
    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static String readCanvasName(PacketBuffer buf) {
        return buf.readString();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public static void writeCanvasName(PacketBuffer buf, String canvasName) {
        buf.writeString(canvasName);
    }
}
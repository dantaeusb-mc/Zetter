package com.dantaeusb.zetter.network.packet;

import net.minecraft.network.FriendlyByteBuf;

/**
 * @todo: Is that okay that we don't have classic handler here?
 */
public class SCanvasNamePacket {
    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static String readCanvasName(FriendlyByteBuf buf) {
        return buf.readUtf();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public static void writeCanvasName(FriendlyByteBuf buf, String canvasName) {
        if (canvasName == null) {
            canvasName = "";
        }

        buf.writeUtf(canvasName);
    }
}
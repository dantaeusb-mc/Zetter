package com.dantaeusb.zetter.network.packet.painting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;

public abstract class CanvasContainer {
    public static CanvasData readPacketCanvasData(PacketBuffer networkBuffer) {
        try {
            String canvasName = networkBuffer.readString();
            int width = networkBuffer.readInt();
            int height = networkBuffer.readInt();
            ByteBuffer colorData = networkBuffer.readBytes(networkBuffer.writerIndex() - networkBuffer.readerIndex()).nioBuffer();
            byte[] unwrappedColorData = new byte[width * height * 4];
            colorData.get(unwrappedColorData);

            CanvasData readCanvasData = new CanvasData(canvasName);
            readCanvasData.initData(width, height, unwrappedColorData);

            return readCanvasData;
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while extracting canvas from contaienr: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public static void writePacketCanvasData(PacketBuffer networkBuffer, CanvasData canvasData) {
        networkBuffer.writeString(canvasData.getName());
        networkBuffer.writeInt(canvasData.getWidth());
        networkBuffer.writeInt(canvasData.getHeight());
        networkBuffer.writeBytes(canvasData.getColorDataBuffer());
    }
}

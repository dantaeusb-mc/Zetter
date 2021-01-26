package com.dantaeusb.immersivemp.locks.network.packet.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;

public class SCanvasSyncMessage {
    private CanvasData canvasData;

    public SCanvasSyncMessage(CanvasData canvasData) {
        this.canvasData = canvasData;
    }

    public CanvasData getCanvasData() {
        return canvasData;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncMessage readPacketData(PacketBuffer buf) {
        try {
            String canvasName = buf.readString();
            int width = buf.readInt();
            int height = buf.readInt();
            ByteBuffer colorData = buf.readBytes(buf.writerIndex() - buf.readerIndex()).nioBuffer();
            byte[] unwrappedColorData = new byte[width * height * 4];
            colorData.get(unwrappedColorData);

            CanvasData readCanvasData = new CanvasData(canvasName);
            readCanvasData.initData(width, height, unwrappedColorData);

            return new SCanvasSyncMessage(readCanvasData);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ImmersiveMp.LOG.warn("Exception while reading SPaintingSyncPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeString(this.canvasData.getName());
        networkBuffer.writeInt(this.canvasData.getWidth());
        networkBuffer.writeInt(this.canvasData.getHeight());
        networkBuffer.writeBytes(this.canvasData.getColorDataBuffer());
    }

    @Override
    public String toString()
    {
        return "SPaintingSyncMessage[canvas=" + this.canvasData + "]";
    }
}
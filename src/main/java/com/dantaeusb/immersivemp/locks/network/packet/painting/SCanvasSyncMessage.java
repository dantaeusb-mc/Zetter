package com.dantaeusb.immersivemp.locks.network.packet.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;

public class SCanvasSyncMessage {
    private final CanvasData canvasData;
    private final long timestamp;

    public SCanvasSyncMessage(CanvasData canvasData, long timestamp) {
        this.canvasData = canvasData;
        this.timestamp = timestamp;
    }

    public CanvasData getCanvasData() {
        return canvasData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncMessage readPacketData(PacketBuffer buf) {
        try {
            long timestamp = buf.readLong();
            String canvasName = buf.readString();
            int width = buf.readInt();
            int height = buf.readInt();
            ByteBuffer colorData = buf.readBytes(buf.writerIndex() - buf.readerIndex()).nioBuffer();
            byte[] unwrappedColorData = new byte[width * height * 4];
            colorData.get(unwrappedColorData);

            CanvasData readCanvasData = new CanvasData(canvasName);
            readCanvasData.initData(width, height, unwrappedColorData);

            return new SCanvasSyncMessage(readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ImmersiveMp.LOG.warn("Exception while reading SPaintingSyncPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeLong(this.timestamp);
        networkBuffer.writeString(this.canvasData.getName());
        networkBuffer.writeInt(this.canvasData.getWidth());
        networkBuffer.writeInt(this.canvasData.getHeight());
        networkBuffer.writeBytes(this.canvasData.getColorDataBuffer());
    }

    @Override
    public String toString()
    {
        return "SPaintingSyncMessage[canvas=" + this.canvasData + ",timestamp=" + this.timestamp + "]";
    }
}
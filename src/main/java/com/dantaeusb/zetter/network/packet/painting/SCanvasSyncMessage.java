package com.dantaeusb.zetter.network.packet.painting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.storage.CanvasData;
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
    public static SCanvasSyncMessage readPacketData(PacketBuffer networkBuffer) {
        try {
            long timestamp = networkBuffer.readLong();
            CanvasData readCanvasData = CanvasContainer.readPacketCanvasData(networkBuffer);

            return new SCanvasSyncMessage(readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeLong(this.timestamp);
        CanvasContainer.writePacketCanvasData(networkBuffer, this.canvasData);
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncMessage[canvas=" + this.canvasData + ",timestamp=" + this.timestamp + "]";
    }
}
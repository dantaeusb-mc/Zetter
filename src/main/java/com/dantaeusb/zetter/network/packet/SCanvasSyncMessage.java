package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.network.PacketBuffer;

public class SCanvasSyncMessage {
    private final AbstractCanvasData canvasData;
    private final long timestamp;

    public SCanvasSyncMessage(AbstractCanvasData canvasData, long timestamp) {
        this.canvasData = canvasData;
        this.timestamp = timestamp;
    }

    public AbstractCanvasData getCanvasData() {
        return this.canvasData;
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
            AbstractCanvasData readCanvasData = CanvasContainer.readPacketCanvasData(networkBuffer);

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
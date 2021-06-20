package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.PacketBuffer;

public class SPaintingSyncMessage {
    private final PaintingData paintingData;
    private final long timestamp;

    public SPaintingSyncMessage(PaintingData paintingData, long timestamp) {
        this.paintingData = paintingData;
        this.timestamp = timestamp;
    }

    public PaintingData getPaintingData() {
        return this.paintingData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SPaintingSyncMessage readPacketData(PacketBuffer networkBuffer) {
        try {
            long timestamp = networkBuffer.readLong();
            PaintingData readCanvasData = (PaintingData) CanvasContainer.readPacketCanvasData(networkBuffer);

            String paintingName = networkBuffer.readString();
            String authorName = networkBuffer.readString();

            readCanvasData.setMetaProperties(authorName, paintingName);

            return new SPaintingSyncMessage(readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SPaintingSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeLong(this.timestamp);
        CanvasContainer.writePacketCanvasData(networkBuffer, this.paintingData);

        // @todo: proper length based on game limitations
        networkBuffer.writeString(this.paintingData.getPaintingName(), 64);
        networkBuffer.writeString(this.paintingData.getAuthorName(), 64);
    }

    @Override
    public String toString()
    {
        return "SPaintingSyncMessage[painting=" + this.paintingData + ",timestamp=" + this.timestamp + "]";
    }
}
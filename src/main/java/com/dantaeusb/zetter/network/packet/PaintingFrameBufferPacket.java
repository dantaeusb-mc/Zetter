package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.container.painting.PaintingFrameBuffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;

/**
 * Painting update - get frame buffer from client when they're making changes
 */
public class PaintingFrameBufferPacket {
    private PaintingFrameBuffer paintingFrameBuffer;

    public PaintingFrameBufferPacket() {
    }

    public PaintingFrameBufferPacket(PaintingFrameBuffer paintingFrameBuffer) {
        this.paintingFrameBuffer = paintingFrameBuffer;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static PaintingFrameBufferPacket readPacketData(PacketBuffer buf) {
        PaintingFrameBufferPacket packet = new PaintingFrameBufferPacket();

        long frameStartTime = buf.readLong();
        ByteBuf bufferData = buf.readBytes(buf.writerIndex() - buf.readerIndex());

        Zetter.LOG.warn(bufferData);
        packet.paintingFrameBuffer = new PaintingFrameBuffer(frameStartTime, bufferData.nioBuffer());

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeLong(this.paintingFrameBuffer.getFrameStartTime());
        buf.writeBytes(this.paintingFrameBuffer.getBufferData());
    }

    public PaintingFrameBuffer getFrameBuffer() {
        return this.paintingFrameBuffer;
    }
}
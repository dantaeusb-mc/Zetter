package com.dantaeusb.immersivemp.locks.network.packet.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrameBuffer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * Painting update - get frame buffer from client when they're making changes
 */
public class PaintingFrameBufferPacket {
    private PaintingFrameBuffer paintingFrameBuffer;

    public PaintingFrameBufferPacket() {
    }

    @OnlyIn(Dist.CLIENT)
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

        ImmersiveMp.LOG.warn(frameStartTime);
        ImmersiveMp.LOG.warn(String.format("%d %d %d", buf.capacity(), buf.writerIndex(), buf.readerIndex()));
        ByteBuf bufferData = buf.readBytes(buf.writerIndex() - buf.readerIndex());

        ImmersiveMp.LOG.warn(bufferData);
        packet.paintingFrameBuffer = new PaintingFrameBuffer(frameStartTime, bufferData.nioBuffer());

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeLong(this.paintingFrameBuffer.getFrameStartTime());
        // For some reason, without it writeBytes just copies the rest of empty buffer
        buf.writeBytes(this.paintingFrameBuffer.getBufferData());
    }

    public PaintingFrameBuffer getFrameBuffer() {
        return this.paintingFrameBuffer;
    }
}
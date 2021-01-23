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
     */
    public static PaintingFrameBufferPacket readPacketData(PacketBuffer buf) {
        PaintingFrameBufferPacket packet = new PaintingFrameBufferPacket();

        try {
            long frameStartTime = buf.readLong();
            ByteBuf bufferData = buf.readBytes(PaintingFrameBuffer.BUFFER_SIZE);

            packet.paintingFrameBuffer = new PaintingFrameBuffer(frameStartTime, bufferData.nioBuffer());
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ImmersiveMp.LOG.warn("Exception while reading CPaintingUpdatePacket: " + e);
            return packet;
        }

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
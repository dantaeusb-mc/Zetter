package com.dantaeusb.immersivemp.locks.network.packet.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

public class SPaintingSyncPacket {
    private ByteBuffer canvasCopy;

    public SPaintingSyncPacket() {
    }

    @OnlyIn(Dist.CLIENT)
    public SPaintingSyncPacket(ByteBuffer canvas) {
        this.canvasCopy = SPaintingSyncPacket.cloneByteBuffer(canvas);
    }

    /**
     * Creates a deep copy that way so changes on canvas won't be applied to the buffer
     * @param original
     * @return
     */
    public static ByteBuffer cloneByteBuffer(final ByteBuffer original) {
        // Create clone with same capacity as original.
        final ByteBuffer clone = (original.isDirect()) ?
                ByteBuffer.allocateDirect(original.capacity()) :
                ByteBuffer.allocate(original.capacity());

        // Create a read-only copy of the original.
        // This allows reading from the original without modifying it.
        final ByteBuffer readOnlyCopy = original.asReadOnlyBuffer();

        // Flip and read from the original.

        readOnlyCopy.flip();
        clone.put(readOnlyCopy);

        return clone;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SPaintingSyncPacket readPacketData(PacketBuffer buf) {
        SPaintingSyncPacket packet = new SPaintingSyncPacket();

        try {
            buf.readBytes(packet.canvasCopy);
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
        buf.writeBytes(this.canvasCopy);
    }

    public ByteBuffer getCanvas() {
        return this.canvasCopy;
    }
}
package com.dantaeusb.immersivemp.locks.network.packet.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class PaintingSyncPacket {
    /**
     * Reads the raw packet data from the data stream.
     */
    public static ByteBuffer readPacketData(PacketBuffer networkBuffer) {
        try {
            boolean hasData = networkBuffer.readBoolean();

            if (!hasData) {
                return null;
            }

            return networkBuffer.readBytes(CanvasItem.CANVAS_BYTE_SIZE).nioBuffer();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ImmersiveMp.LOG.warn("Exception while reading PaintingSyncPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public static void writePacketData(PacketBuffer networkBuffer, @Nullable ByteBuffer canvas) {
        if (canvas == null) {
            networkBuffer.writeBoolean(false);
        } else if (canvas.limit() != CanvasItem.CANVAS_BYTE_SIZE) {
            ImmersiveMp.LOG.error("Trying to sync canvas with invalid size");
            networkBuffer.writeBoolean(false);
        } else {
            networkBuffer.writeBoolean(true);

            canvas.rewind();
            networkBuffer.writeBytes(canvas);
        }
    }
}
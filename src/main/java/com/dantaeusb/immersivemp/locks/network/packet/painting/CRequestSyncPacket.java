package com.dantaeusb.immersivemp.locks.network.packet.painting;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrameBuffer;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public class CRequestSyncPacket {
    private String canvasName;

    public CRequestSyncPacket() {
    }

    public CRequestSyncPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CRequestSyncPacket readPacketData(PacketBuffer buf) {
        CRequestSyncPacket packet = new CRequestSyncPacket();

        packet.canvasName = buf.readString();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeString(this.canvasName);
    }

    public String getCanvasName() {
        return this.canvasName;
    }
}
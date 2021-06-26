package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.network.PacketBuffer;

public class CCanvasBucketToolPacket {
    public final int position;
    public final int color;

    public CCanvasBucketToolPacket(int position, int color) {
        this.position = position;
        this.color = color;
    }

    public static CCanvasBucketToolPacket readPacketData(PacketBuffer buf) {
        final int position = buf.readInt();
        final int color = buf.readInt();

        return new CCanvasBucketToolPacket(position, color);
    }

    public void writePacketData(PacketBuffer buf) {
        buf.writeInt(this.position);
        buf.writeInt(this.color);
    }
}
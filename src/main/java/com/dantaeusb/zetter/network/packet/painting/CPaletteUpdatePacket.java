package com.dantaeusb.zetter.network.packet.painting;

import net.minecraft.network.PacketBuffer;

public class CPaletteUpdatePacket {
    private int slotIndex;
    private int color;

    public CPaletteUpdatePacket() {
    }

    public CPaletteUpdatePacket(int slotIndex, int color) {
        this.slotIndex = slotIndex;
        this.color = color;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CPaletteUpdatePacket readPacketData(PacketBuffer buf) {
        CPaletteUpdatePacket packet = new CPaletteUpdatePacket();

        packet.slotIndex = buf.readInt();
        packet.color = buf.readInt();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeInt(this.slotIndex);
        buf.writeInt(this.color);
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public int getColor() {
        return this.color;
    }
}
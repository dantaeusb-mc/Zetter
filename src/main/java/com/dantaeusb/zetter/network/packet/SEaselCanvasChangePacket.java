package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

/**
 * @link {SSetSlotPacket}
 */
public class SEaselCanvasChangePacket {
    private int windowId;
    private ItemStack item = ItemStack.EMPTY;

    public SEaselCanvasChangePacket(int windowId, ItemStack item) {
        this.windowId = windowId;
        this.item = item.copy();
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselCanvasChangePacket readPacketData(PacketBuffer networkBuffer) {
        try {
            final int windowId = networkBuffer.readByte();
            final ItemStack item = networkBuffer.readItemStack();

            return new SEaselCanvasChangePacket(windowId, item);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselCanvasChangePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeByte(this.windowId);
        networkBuffer.writeItemStack(this.item);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public ItemStack getItem() {
        return this.item;
    }

    @Override
    public String toString()
    {
        return "SEaselCanvasChangePacket[windowId=" + this.windowId + ",stack=" + this.item + "]";
    }
}
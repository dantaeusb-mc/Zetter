package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.network.FriendlyByteBuf;

public class SEaselMenuCreatePacket {
    private final int easelEntityId;
    private final String canvasCode;

    public SEaselMenuCreatePacket(int easelEntityId, String canvasCode) {
        this.easelEntityId = easelEntityId;
        this.canvasCode = canvasCode;
    }

    public int getEaselEntityId() {
        return this.easelEntityId;
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselMenuCreatePacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            int easelEntityId = networkBuffer.readInt();
            String canvasCode = networkBuffer.readUtf(256);

            return new SEaselMenuCreatePacket(easelEntityId, canvasCode);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselMenuCreatePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
        networkBuffer.writeUtf(this.canvasCode, 256);
    }

    @Override
    public String toString()
    {
        return "SEaselMenuCreatePacket[easelId=" + this.easelEntityId + ",canvasCode=" + this.canvasCode + "]";
    }
}
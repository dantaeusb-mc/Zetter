package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;

public class SEaselMenuCreatePacket {
    public final int easelEntityId;
    public final @Nullable String canvasCode;

    public SEaselMenuCreatePacket(int easelEntityId, @Nullable String canvasCode) {
        this.easelEntityId = easelEntityId;
        this.canvasCode = canvasCode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselMenuCreatePacket readPacketData(PacketBuffer networkBuffer) {
        try {
            int easelEntityId = networkBuffer.readInt();
            boolean hasCanvasCode = networkBuffer.readBoolean();
            String canvasCode = null;

            if (hasCanvasCode) {
                canvasCode = networkBuffer.readUtf(256);
            }

            return new SEaselMenuCreatePacket(easelEntityId, canvasCode);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselMenuCreatePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);

        if (this.canvasCode != null) {
            networkBuffer.writeBoolean(true);
            networkBuffer.writeUtf(this.canvasCode, 256);
        } else {
            networkBuffer.writeBoolean(false);
        }
    }

    @Override
    public String toString()
    {
        return "SEaselMenuCreatePacket[easelId=" + this.easelEntityId + ",canvasCode=" + this.canvasCode + "]";
    }
}
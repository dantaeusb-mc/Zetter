package com.dantaeusb.immersivemp.locks.network.packet;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLockTableModePacket {
    private int windowId;
    private boolean keyMode;

    public CLockTableModePacket() {
    }

    @OnlyIn(Dist.CLIENT)
    public CLockTableModePacket(int windowId, boolean keyMode) {
        this.windowId = windowId;
        this.keyMode = keyMode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CLockTableModePacket readPacketData(PacketBuffer buf) {
        CLockTableModePacket packet = new CLockTableModePacket();

        try {
            packet.windowId = buf.readByte();
            packet.keyMode = buf.readBoolean();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            LOGGER.warn("Exception while reading CLockTableModePacket: " + e);
            return packet;
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeByte(this.windowId);
        buf.writeBoolean(this.keyMode);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public boolean getKeyMode() {
        return this.keyMode;
    }

    private static final Logger LOGGER = LogManager.getLogger();
}
package com.dantaeusb.immersivemp.locks.network.packet;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class CLockTableRenameItemPacket {
    private String name;

    public CLockTableRenameItemPacket() {
    }

    @OnlyIn(Dist.CLIENT)
    public CLockTableRenameItemPacket(String name) {
        this.name = name;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CLockTableRenameItemPacket readPacketData(PacketBuffer buf) {
        CLockTableRenameItemPacket packet = new CLockTableRenameItemPacket();

        try {
            packet.name = buf.readString(32767);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            LOGGER.warn("Exception while reading CLockTableRenameItemPacket: " + e);
            return packet;
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeString(this.name);
    }

    public String getName() {
        return this.name;
    }

    private static final Logger LOGGER = LogManager.getLogger();
}
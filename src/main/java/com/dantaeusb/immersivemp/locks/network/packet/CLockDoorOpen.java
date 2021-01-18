package com.dantaeusb.immersivemp.locks.network.packet;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CLockDoorOpen {
    private BlockPos activatedBlockPos;

    public CLockDoorOpen() {
    }

    @OnlyIn(Dist.CLIENT)
    public CLockDoorOpen(BlockPos activatedBlockPos) {
        this.activatedBlockPos = activatedBlockPos;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CLockDoorOpen readPacketData(PacketBuffer buf) {
        CLockDoorOpen packet = new CLockDoorOpen();

        try {
            packet.activatedBlockPos = buf.readBlockPos();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ImmersiveMp.LOG.warn("Exception while reading CLockDoorOpen: " + e);
            return packet;
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeBlockPos(this.activatedBlockPos);
    }

    public BlockPos getActivatedBlockPos() {
        return this.activatedBlockPos;
    }
}
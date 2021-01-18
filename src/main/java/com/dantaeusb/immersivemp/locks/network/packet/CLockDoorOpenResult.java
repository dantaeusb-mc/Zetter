package com.dantaeusb.immersivemp.locks.network.packet;

import com.dantaeusb.immersivemp.ImmersiveMp;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class CLockDoorOpenResult {
    private boolean success;

    public CLockDoorOpenResult() {
    }

    @OnlyIn(Dist.CLIENT)
    public CLockDoorOpenResult(boolean success) {
        this.success = success;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CLockDoorOpenResult readPacketData(PacketBuffer buf) {
        CLockDoorOpenResult packet = new CLockDoorOpenResult();

        try {
            packet.success = buf.readBoolean();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            ImmersiveMp.LOG.warn("Exception while reading CLockDoorOpenResult: " + e);
            return packet;
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeBoolean(this.success);
    }

    public boolean getSuccess() {
        return this.success;
    }
}
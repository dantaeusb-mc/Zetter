package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ClientHandler;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncMessage {
    private final AbstractCanvasData canvasData;
    private final long timestamp;

    public SCanvasSyncMessage(AbstractCanvasData canvasData, long timestamp) {
        this.canvasData = canvasData;
        this.timestamp = timestamp;
    }

    public AbstractCanvasData getCanvasData() {
        return this.canvasData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncMessage readPacketData(PacketBuffer networkBuffer) {
        try {
            long timestamp = networkBuffer.readLong();
            AbstractCanvasData readCanvasData = CanvasContainer.readPacketCanvasData(networkBuffer);

            return new SCanvasSyncMessage(readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeLong(this.timestamp);
        CanvasContainer.writePacketCanvasData(networkBuffer, this.canvasData);
    }

    public static void handle(final SCanvasSyncMessage packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasSync(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncMessage[canvas=" + this.canvasData + ",timestamp=" + this.timestamp + "]";
    }
}
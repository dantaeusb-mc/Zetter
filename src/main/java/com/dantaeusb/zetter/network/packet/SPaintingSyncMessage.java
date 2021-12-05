package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ClientHandler;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SPaintingSyncMessage {
    private final PaintingData paintingData;
    private final long timestamp;

    public SPaintingSyncMessage(PaintingData paintingData, long timestamp) {
        this.paintingData = paintingData;
        this.timestamp = timestamp;
    }

    public PaintingData getPaintingData() {
        return this.paintingData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SPaintingSyncMessage readPacketData(PacketBuffer networkBuffer) {
        try {
            long timestamp = networkBuffer.readLong();
            PaintingData readCanvasData = (PaintingData) CanvasContainer.readPacketCanvasData(networkBuffer);

            String paintingName = networkBuffer.readUtf();
            String authorName = networkBuffer.readUtf();

            readCanvasData.setMetaProperties(authorName, paintingName);

            return new SPaintingSyncMessage(readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SPaintingSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeLong(this.timestamp);
        CanvasContainer.writePacketCanvasData(networkBuffer, this.paintingData);

        // @todo: proper length based on game limitations
        networkBuffer.writeUtf(this.paintingData.getPaintingName(), 64);
        networkBuffer.writeUtf(this.paintingData.getAuthorName(), 64);
    }

    public static void handle(final SPaintingSyncMessage packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processPaintingSync(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SPaintingSyncMessage[painting=" + this.paintingData + ",timestamp=" + this.timestamp + "]";
    }
}
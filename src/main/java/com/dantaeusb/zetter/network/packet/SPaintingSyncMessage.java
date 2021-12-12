package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ClientHandler;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import java.util.Optional;
import java.util.function.Supplier;

public class SPaintingSyncMessage {
    private final String canvasCode;
    private final PaintingData paintingData;
    private final long timestamp;

    public SPaintingSyncMessage(String canvasCode, PaintingData paintingData, long timestamp) {
        this.canvasCode = canvasCode;
        this.paintingData = paintingData;
        this.timestamp = timestamp;
    }

    public String getCanvasCode() {
        return this.canvasCode;
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
    public static SPaintingSyncMessage readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf();
            long timestamp = networkBuffer.readLong();
            PaintingData readCanvasData = (PaintingData) CanvasContainer.readPacketCanvasData(networkBuffer);

            String paintingName = networkBuffer.readUtf();
            String authorName = networkBuffer.readUtf();

            readCanvasData.setMetaProperties(authorName, paintingName);

            return new SPaintingSyncMessage(canvasCode, readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SPaintingSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode);
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

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
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
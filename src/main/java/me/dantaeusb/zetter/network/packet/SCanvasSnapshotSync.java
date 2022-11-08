package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Send snapshot of a canvas, used only for easel when drawing, a bit more specific
 * object that is sent more frequently than default canvases when
 * multiple players are drawing
 */
public class SCanvasSnapshotSync {
    private final int easelEntityId;
    private final String canvasCode;
    private final CanvasData canvasData;
    private final long timestamp;

    // @todo: [HIGH] Probably should pass color array, not canvas data
    public SCanvasSnapshotSync(int easelEntityId, String canvasCode, CanvasData canvasData, long timestamp) {
        this.easelEntityId = easelEntityId;
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.timestamp = timestamp;
    }

    public int getEaselEntityId() {
        return this.easelEntityId;
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    public CanvasData getCanvasData() {
        return this.canvasData;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSnapshotSync readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            int easelEntityId = networkBuffer.readInt();
            String canvasCode = networkBuffer.readUtf();
            long timestamp = networkBuffer.readLong();
            CanvasData readCanvasData = CanvasData.readPacketData(networkBuffer);

            return new SCanvasSnapshotSync(easelEntityId, canvasCode, readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SPaintingSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
        networkBuffer.writeUtf(this.canvasCode);
        networkBuffer.writeLong(this.timestamp);
        CanvasData.writePacketData(this.canvasData, networkBuffer);
    }

    public static void handle(final SCanvasSnapshotSync packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSnapshotSync context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processSnapshotSync(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SPaintingSnapshotSync[easel=" + this.easelEntityId + "painting=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
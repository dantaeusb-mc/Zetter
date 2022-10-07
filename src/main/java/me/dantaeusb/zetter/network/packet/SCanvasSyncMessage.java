package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.Optional;

public class SCanvasSyncMessage {
    protected final String canvasCode;
    protected final AbstractCanvasData canvasData;
    protected final AbstractCanvasData.Type type;
    protected final long timestamp;

    public SCanvasSyncMessage(String canvasCode, AbstractCanvasData canvasData, long timestamp) {
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.type = AbstractCanvasData.Type.getTypeByCanvas(canvasData);
        this.timestamp = timestamp;
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    public AbstractCanvasData getCanvasData() {
        return this.canvasData;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public AbstractCanvasData.Type getType() {
        return this.type;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncMessage readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf();
            long timestamp = networkBuffer.readLong();
            byte typeId = networkBuffer.readByte();

            AbstractCanvasData.Type type = AbstractCanvasData.Type.getTypeById(typeId);
            AbstractCanvasData readCanvasData = CanvasContainer.readPacketCanvasData(networkBuffer);

            if (type == AbstractCanvasData.Type.PAINTING) {
                String paintingName = networkBuffer.readUtf();
                String authorName = networkBuffer.readUtf();

                ((PaintingData) readCanvasData).setMetaProperties(authorName, paintingName);
            }

            return new SCanvasSyncMessage(canvasCode, readCanvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode);
        networkBuffer.writeLong(this.timestamp);
        networkBuffer.writeByte(this.type.ordinal());
        CanvasContainer.writePacketCanvasData(networkBuffer, this.canvasData);

        if (this.canvasData instanceof PaintingData) {
            // @todo: [LOW] Proper length based on game limitations
            networkBuffer.writeUtf(((PaintingData) this.canvasData).getPaintingTitle(), 64);
            networkBuffer.writeUtf(((PaintingData) this.canvasData).getAuthorName(), 64);
        }
    }

    public static void handle(final SCanvasSyncMessage packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.error("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        if (packetIn.type == AbstractCanvasData.Type.CANVAS) {
            ctx.enqueueWork(() -> ClientHandler.processCanvasSync(packetIn, clientWorld.get()));
        } else if (packetIn.type == AbstractCanvasData.Type.PAINTING) {
            ctx.enqueueWork(() -> ClientHandler.processPaintingSync(packetIn, clientWorld.get()));
        } else {
            Zetter.LOG.error("SCanvasSyncMessage has wrong type.");
        }
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncMessage[canvas=" + this.canvasCode + ",type=" + this.type.getId() + ",timestamp=" + this.timestamp + "]";
    }
}
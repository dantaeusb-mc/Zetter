package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import java.util.Optional;

public class SCanvasSyncPacket<T extends AbstractCanvasData> {
    protected final String canvasCode;
    protected final long timestamp;
    protected final T canvasData;

    public SCanvasSyncPacket(String canvasCode, T canvasData, long timestamp) {
        this.canvasCode = canvasCode;
        this.timestamp = timestamp;
        this.canvasData = canvasData;
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    public T getCanvasData() {
        return this.canvasData;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncPacket<?> readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String type = networkBuffer.readUtf(128);
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();

            CanvasDataType<?> canvasDataType = ZetterRegistries.CANVAS_TYPE.get().getValue(new ResourceLocation(type));

            if (canvasDataType == null) {
                throw new IllegalArgumentException("Unable to find canvas type " + type);
            }

            AbstractCanvasData canvasData = canvasDataType.readPacketData(networkBuffer);

            return new SCanvasSyncPacket<>(canvasCode, canvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasData.getType().getRegistryName().toString(), 128);
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);

        CanvasDataType<T> canvasDataType = (CanvasDataType<T>) ZetterRegistries.CANVAS_TYPE.get().getValue(this.canvasData.getType().getRegistryName());

        assert canvasDataType != null;
        canvasDataType.writePacketData(this.canvasData, networkBuffer);
    }

    public static void handle(final SCanvasSyncPacket<?> packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (clientWorld.isEmpty()) {
            Zetter.LOG.error("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasSync(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
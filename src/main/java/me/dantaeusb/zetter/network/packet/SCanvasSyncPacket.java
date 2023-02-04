package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncPacket<T extends AbstractCanvasData> {
    public final String canvasCode;
    public final long timestamp;
    public final T canvasData;

    public SCanvasSyncPacket(String canvasCode, T canvasData, long timestamp) {
        this.canvasCode = canvasCode;
        this.timestamp = timestamp;
        this.canvasData = canvasData;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncPacket<?> readPacketData(PacketBuffer networkBuffer) {
        try {
            final String type = networkBuffer.readUtf(128);
            final String canvasCode = networkBuffer.readUtf(128);
            final long timestamp = networkBuffer.readLong();

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
    public void writePacketData(PacketBuffer networkBuffer) {
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

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
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
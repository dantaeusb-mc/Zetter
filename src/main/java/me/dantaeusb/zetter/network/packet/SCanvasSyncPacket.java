package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;


import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncPacket<T extends AbstractCanvasData> implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SCanvasSyncPacket<?>> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_sync"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SCanvasSyncPacket<?>> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SCanvasSyncPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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
    public static SCanvasSyncPacket<?> readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final String type = networkBuffer.readUtf(128);
            final String canvasCode = networkBuffer.readUtf(128);
            final long timestamp = networkBuffer.readLong();

            CanvasDataType<?> canvasDataType = ZetterRegistries.CANVAS_TYPE.get().get(net.minecraft.resources.ResourceLocation.parse(type));

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
        networkBuffer.writeUtf(this.canvasData.getType().resourceLocation.toString(), 128);
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);

        CanvasDataType<T> canvasDataType = (CanvasDataType<T>) ZetterRegistries.CANVAS_TYPE.get().get(this.canvasData.getType().resourceLocation);

        assert canvasDataType != null;
        canvasDataType.writePacketData(this.canvasData, networkBuffer);
    }

    public static void handle(final SCanvasSyncPacket<?> packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (clientWorld.isEmpty()) {
            Zetter.LOG.error("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processCanvasSync(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
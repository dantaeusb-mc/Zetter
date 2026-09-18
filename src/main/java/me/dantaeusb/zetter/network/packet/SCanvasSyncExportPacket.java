package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;


import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncExportPacket extends SCanvasSyncPacket<PaintingData> {
    public static final Type<SCanvasSyncExportPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_sync_export"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SCanvasSyncExportPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SCanvasSyncExportPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public SCanvasSyncExportPacket(String canvasCode, PaintingData paintingData, long timestamp) {
        super(canvasCode, paintingData, timestamp);
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncExportPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();

            PaintingData canvasData = ZetterCanvasTypes.PAINTING.get().readPacketData(networkBuffer);

            return new SCanvasSyncExportPacket(canvasCode, canvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncExportPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);

        ZetterCanvasTypes.PAINTING.get().writePacketData(this.canvasData, networkBuffer);
    }

    public static void handle(final SCanvasSyncExportPacket packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncExportPacket context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processCanvasSyncExportError(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncViewMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
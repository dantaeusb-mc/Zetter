package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;


import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Supplier;

public record SCanvasRemovalPacket(String canvasCode, long timestamp) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SCanvasRemovalPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_removal"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SCanvasRemovalPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SCanvasRemovalPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasRemovalPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();

            return new SCanvasRemovalPacket(canvasCode, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);
    }

    public static void handle(final SCanvasRemovalPacket packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (clientWorld.isEmpty()) {
            Zetter.LOG.error("SCanvasRemovalMessage context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processCanvasRemoval(packetIn, clientWorld.get()));
    }

    @Override
    public String toString() {
        return "SCanvasRemovalMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
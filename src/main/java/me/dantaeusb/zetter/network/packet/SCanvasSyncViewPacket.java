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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;


import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncViewPacket<T extends AbstractCanvasData> extends SCanvasSyncPacket<T> implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SCanvasSyncViewPacket<?>> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_sync_view"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SCanvasSyncViewPacket<?>> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SCanvasSyncViewPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final InteractionHand hand;

    public SCanvasSyncViewPacket(String canvasCode, T canvasData, long timestamp, InteractionHand hand) {
        super(canvasCode, canvasData, timestamp);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncViewPacket<?> readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String type = networkBuffer.readUtf(128);
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();
            byte handCode = networkBuffer.readByte();
            InteractionHand hand = InteractionHand.values()[handCode];

            CanvasDataType<?> canvasDataType = ZetterRegistries.CANVAS_TYPE.get().get(net.minecraft.resources.ResourceLocation.parse(type));

            if (canvasDataType == null) {
                throw new IllegalArgumentException("Unable to find canvas type " + type);
            }

            AbstractCanvasData canvasData = canvasDataType.readPacketData(networkBuffer);

            return new SCanvasSyncViewPacket(canvasCode, canvasData, timestamp, hand);
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
        networkBuffer.writeByte(this.hand.ordinal());

        CanvasDataType<T> canvasDataType = (CanvasDataType<T>) ZetterRegistries.CANVAS_TYPE.get().get(this.canvasData.getType().resourceLocation);

        assert canvasDataType != null;
        canvasDataType.writePacketData(this.canvasData, networkBuffer);

    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static void handle(final SCanvasSyncViewPacket<?> packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncViewMessage context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processCanvasSyncView(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncViewMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
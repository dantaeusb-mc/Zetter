package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;


import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Send snapshot of a canvas, used only for easel when drawing, a bit more specific
 * object that is sent more frequently than default canvases when
 * multiple players are drawing
 *
 * @todo: [MED] Do we need that since we can track on client
 * if canvas item was changed
 */
public class SEaselCanvasInitializationPacket extends SCanvasSyncPacket<CanvasData> {
    public static final Type<SEaselCanvasInitializationPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "easel_canvas_initialization"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SEaselCanvasInitializationPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SEaselCanvasInitializationPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public final int easelEntityId;

    public SEaselCanvasInitializationPacket(int easelEntityId, String canvasCode, CanvasData canvasData, long timestamp) {
        super(canvasCode, canvasData, timestamp);

        this.easelEntityId = easelEntityId;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselCanvasInitializationPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final int easelEntityId = networkBuffer.readInt();
            final String canvasCode = networkBuffer.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);
            final long timestamp = networkBuffer.readLong();

            final CanvasData canvasData = ZetterCanvasTypes.CANVAS.get().readPacketData(networkBuffer);

            return new SEaselCanvasInitializationPacket(easelEntityId, canvasCode, canvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselCanvasInitializationPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
        networkBuffer.writeUtf(this.canvasCode, Helper.CANVAS_CODE_MAX_LENGTH);
        networkBuffer.writeLong(this.timestamp);

        ZetterCanvasTypes.CANVAS.get().writePacketData(this.canvasData, networkBuffer);
    }

    public static void handle(final SEaselCanvasInitializationPacket packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselReset context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processEaselCanvasInitialization(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SEaselCanvasInitializationPacket[easel=" + this.easelEntityId + ",canvasCode=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}
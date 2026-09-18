package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Absolute copy of request packet but we have to copy them cause
 * there's no way to determine which purpose packet is used for
 * unless they're different classes for some reason
 */
public class CCanvasUnloadRequestPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<CCanvasUnloadRequestPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_unload_request"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CCanvasUnloadRequestPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CCanvasUnloadRequestPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private String canvasName;

    public CCanvasUnloadRequestPacket() {
    }

    public CCanvasUnloadRequestPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasUnloadRequestPacket readPacketData(FriendlyByteBuf buf) {
        CCanvasUnloadRequestPacket packet = new CCanvasUnloadRequestPacket();

        packet.canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_LENGTH);
    }

    public String getCanvasName() {
        return this.canvasName;
    }

    public static void handle(final CCanvasUnloadRequestPacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        context.enqueueWork(() -> ServerHandler.processUnloadRequest(packetIn, sendingPlayer));
    }
}
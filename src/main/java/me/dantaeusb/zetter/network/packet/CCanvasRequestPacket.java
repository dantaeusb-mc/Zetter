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

public class CCanvasRequestPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<CCanvasRequestPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_request"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CCanvasRequestPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CCanvasRequestPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public final String canvasName;

    public CCanvasRequestPacket(String canvasName) {
        this.canvasName = canvasName;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestPacket readPacketData(FriendlyByteBuf buf) {
        String canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

        return new CCanvasRequestPacket(canvasName);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_LENGTH);
    }

    public static void handle(final CCanvasRequestPacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        context.enqueueWork(() -> ServerHandler.processCanvasRequest(packetIn, sendingPlayer));
    }
}
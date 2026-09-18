package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class CCanvasRequestViewPacket extends CCanvasRequestPacket {
    public static final Type<CCanvasRequestViewPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_request_view"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CCanvasRequestViewPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CCanvasRequestViewPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final InteractionHand hand;

    public CCanvasRequestViewPacket(String canvasName, InteractionHand hand) {
        super(canvasName);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestViewPacket readPacketData(FriendlyByteBuf buf) {
        String canvasName = buf.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);
        byte handCode = buf.readByte();

        InteractionHand hand = InteractionHand.values()[handCode];

        return new CCanvasRequestViewPacket(canvasName, hand);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeUtf(this.canvasName, Helper.CANVAS_CODE_MAX_LENGTH);
        buf.writeByte(this.hand.ordinal());
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static void handle(final CCanvasRequestViewPacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CRequestSyncPacket was received");
        }

        context.enqueueWork(() -> ServerHandler.processCanvasViewRequest(packetIn, sendingPlayer));
    }
}
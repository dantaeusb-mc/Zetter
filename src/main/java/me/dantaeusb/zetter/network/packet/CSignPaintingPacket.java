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
 * Rename painting packet used to send updated name field in
 * Artist Table screen from clent to the server
 */
public class CSignPaintingPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<CSignPaintingPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "sign_painting"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CSignPaintingPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CSignPaintingPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private int slot;
    private String paintingTitle;

    public CSignPaintingPacket() {

    }

    public CSignPaintingPacket(int slot, String paintingTitle) {
        this.slot = slot;
        this.paintingTitle = paintingTitle;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CSignPaintingPacket readPacketData(FriendlyByteBuf buf) {
        CSignPaintingPacket packet = new CSignPaintingPacket();

        try {
            packet.slot = buf.readByte();
            packet.paintingTitle = buf.readUtf(Helper.PAINTING_TITLE_MAX_LENGTH);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading CCreatePaintingPacket: " + e);
            return packet;
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeByte(this.slot);
        buf.writeUtf(this.paintingTitle, Helper.PAINTING_TITLE_MAX_LENGTH);
    }

    public int getSlot() {
        return this.slot;
    }

    public String getPaintingTitle() {
        return this.paintingTitle;
    }

    public static void handle(final CSignPaintingPacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCreatePaintingPacket was received");
        }

        context.enqueueWork(() -> ServerHandler.processSignPainting(packetIn, sendingPlayer));
    }
}
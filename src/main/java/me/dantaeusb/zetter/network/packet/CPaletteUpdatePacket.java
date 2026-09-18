package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class CPaletteUpdatePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<CPaletteUpdatePacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "palette_update"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CPaletteUpdatePacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CPaletteUpdatePacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private int slotIndex;
    private int color;

    public CPaletteUpdatePacket() {
    }

    public CPaletteUpdatePacket(int slotIndex, int color) {
        this.slotIndex = slotIndex;
        this.color = color;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CPaletteUpdatePacket readPacketData(FriendlyByteBuf buf) {
        CPaletteUpdatePacket packet = new CPaletteUpdatePacket();

        packet.slotIndex = buf.readInt();
        packet.color = buf.readInt();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeInt(this.color);
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public int getColor() {
        return this.color;
    }

    public static void handle(final CPaletteUpdatePacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when PaletteUpdatePacket was received");
        }

        context.enqueueWork(() -> ServerHandler.processPaletteUpdate(packetIn, sendingPlayer));
    }
}
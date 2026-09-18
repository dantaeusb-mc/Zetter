package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class CArtistTableModeChangePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<CArtistTableModeChangePacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "artist_table_mode_change"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CArtistTableModeChangePacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CArtistTableModeChangePacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final int windowId;
    private final ArtistTableMenu.Mode mode;

    public CArtistTableModeChangePacket(int windowId, ArtistTableMenu.Mode mode) {
        this.windowId = windowId;
        this.mode = mode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CArtistTableModeChangePacket readPacketData(FriendlyByteBuf buf) {
        int windowId = buf.readInt();
        byte modeId = buf.readByte();

        CArtistTableModeChangePacket packet = new CArtistTableModeChangePacket(windowId, ArtistTableMenu.Mode.getById(modeId));

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeByte(this.mode.getId());
    }

    public int getWindowId() {
        return this.windowId;
    }

    public ArtistTableMenu.Mode getMode() {
        return this.mode;
    }

    public static void handle(final CArtistTableModeChangePacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when ArtistTableModeChange was received");
        }

        context.enqueueWork(() -> ServerHandler.processArtistTableModeChange(packetIn, sendingPlayer));
    }
}
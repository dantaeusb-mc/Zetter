package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class SArtistTableMenuCreatePacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SArtistTableMenuCreatePacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "artist_table_menu_create"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SArtistTableMenuCreatePacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SArtistTableMenuCreatePacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final BlockPos artistTablePos;
    private final ArtistTableMenu.Mode mode;

    public SArtistTableMenuCreatePacket(BlockPos artistTablePos, ArtistTableMenu.Mode mode) {
        this.artistTablePos = artistTablePos;
        this.mode = mode;
    }

    public BlockPos getArtistTablePos() {
        return this.artistTablePos;
    }

    public ArtistTableMenu.Mode getMode() {
        return this.mode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SArtistTableMenuCreatePacket readPacketData(FriendlyByteBuf networkBuffer) {
        BlockPos artistTablePos = networkBuffer.readBlockPos();
        byte modeId = networkBuffer.readByte();
        ArtistTableMenu.Mode mode = ArtistTableMenu.Mode.getById(modeId);

        return new SArtistTableMenuCreatePacket(artistTablePos, mode);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeBlockPos(this.artistTablePos);
        networkBuffer.writeByte(this.mode.getId());
    }

    @Override
    public String toString()
    {
        return "SArtistTableMenuCreatePacket[artistTablePos=" + this.artistTablePos + ",modeId=" + this.mode.getId() + "]";
    }
}
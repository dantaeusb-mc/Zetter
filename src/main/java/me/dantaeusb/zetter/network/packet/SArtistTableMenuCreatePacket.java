package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class SArtistTableMenuCreatePacket {
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
    public static SArtistTableMenuCreatePacket readPacketData(PacketBuffer networkBuffer) {
        BlockPos artistTablePos = networkBuffer.readBlockPos();
        byte modeId = networkBuffer.readByte();
        ArtistTableMenu.Mode mode = ArtistTableMenu.Mode.getById(modeId);

        return new SArtistTableMenuCreatePacket(artistTablePos, mode);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeBlockPos(this.artistTablePos);
        networkBuffer.writeByte(this.mode.getId());
    }

    @Override
    public String toString()
    {
        return "SArtistTableMenuCreatePacket[artistTablePos=" + this.artistTablePos + ",modeId=" + this.mode.getId() + "]";
    }
}
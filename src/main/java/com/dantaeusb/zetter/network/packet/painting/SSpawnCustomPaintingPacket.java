package com.dantaeusb.zetter.network.packet.painting;

import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import net.minecraft.entity.item.PaintingType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public class SSpawnCustomPaintingPacket {
    private int typeId;
    private int entityId;
    private UUID uniqueId;
    private BlockPos position;
    private Direction facing;
    private String canvasCode;
    private String paintingName;
    private String authorName;
    private int[] blockSize;

    public SSpawnCustomPaintingPacket() {
    }

    public SSpawnCustomPaintingPacket(CustomPaintingEntity painting) {
        this.typeId = Registry.ENTITY_TYPE.getId(painting.getType());
        this.entityId = painting.getEntityId();
        this.uniqueId = painting.getUniqueID();
        this.position = painting.getHangingPosition();
        this.facing = painting.getHorizontalFacing();
        this.canvasCode = painting.getCanvasCode();
        this.paintingName = painting.getPaintingName();
        this.authorName = painting.getAuthorName();
        this.blockSize = painting.getBlockSize();
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public void readPacketData(PacketBuffer buf) {
        this.typeId = buf.readVarInt();
        this.entityId = buf.readVarInt();
        this.uniqueId = buf.readUniqueId();
        this.position = buf.readBlockPos();
        this.facing = Direction.byHorizontalIndex(buf.readUnsignedByte());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeVarInt(this.typeId);
        buf.writeVarInt(this.entityId);
        buf.writeUniqueId(this.uniqueId);
        buf.writeBlockPos(this.position);
        buf.writeByte(this.facing.getHorizontalIndex());
    }

    @OnlyIn(Dist.CLIENT)
    public int getTypeId() {
        return this.typeId;
    }

    @OnlyIn(Dist.CLIENT)
    public int getEntityId() {
        return this.entityId;
    }

    @OnlyIn(Dist.CLIENT)
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @OnlyIn(Dist.CLIENT)
    public BlockPos getPosition() {
        return this.position;
    }

    @OnlyIn(Dist.CLIENT)
    public Direction getFacing() {
        return this.facing;
    }
}

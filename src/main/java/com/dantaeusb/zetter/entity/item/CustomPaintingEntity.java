package com.dantaeusb.zetter.entity.item;

import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.item.CanvasItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomPaintingEntity extends HangingEntity implements IEntityAdditionalSpawnData {
    public static final String NBT_TAG_FACING = "Facing";
    public static final String NBT_TAG_CANVAS_NAME = "CanvasName";

    protected String canvasName;

    public CustomPaintingEntity(EntityType<? extends CustomPaintingEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public CustomPaintingEntity(World worldIn, BlockPos pos, Direction facing, String canvasName) {
        super(ModEntities.CUSTOM_PAINTING_ENTITY, worldIn, pos);

        this.canvasName = canvasName;
        this.updateFacingWithBoundingBox(facing);
    }

    public String getCanvasName() {
        if (this.canvasName == null) {
            return Helper.fallbackCanvasName;
        }

        return this.canvasName;
    }

    /**
     * Updates facing and bounding box based on it
     */
    protected void updateFacingWithBoundingBox(Direction facingDirectionIn) {
        Validate.notNull(facingDirectionIn);
        this.facingDirection = facingDirectionIn;
        if (facingDirectionIn.getAxis().isHorizontal()) {
            this.rotationPitch = 0.0F;
            this.rotationYaw = (float)(this.facingDirection.getHorizontalIndex() * 90);
        } else {
            this.rotationPitch = (float)(-90 * facingDirectionIn.getAxisDirection().getOffset());
            this.rotationYaw = 0.0F;
        }

        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        this.updateBoundingBox();
    }

    /**
     * Updates the entity bounding box based on current facing
     */
    protected void updateBoundingBox() {
        if (this.facingDirection != null) {
            double halfThick = 0.5D - (1.0D / 32.0D);

            // Block position -> middle of the block -> adjust on facing direction
            double xCenter = (double)this.hangingPosition.getX() + 0.5D - (double)this.facingDirection.getXOffset() * halfThick;
            double yCenter = (double)this.hangingPosition.getY() + 0.5D - (double)this.facingDirection.getYOffset() * halfThick;
            double zCenter = (double)this.hangingPosition.getZ() + 0.5D - (double)this.facingDirection.getZOffset() * halfThick;
            this.setRawPosition(xCenter, yCenter, zCenter);

            // It's "cubic diameter" right now
            double xRadius = this.getWidthPixels();
            double yRadius = this.getHeightPixels();
            double zRadius = this.getWidthPixels();
            Direction.Axis direction$axis = this.facingDirection.getAxis();

            // We have 1 pixel thickness (length in facing direction) for picture
            switch(direction$axis) {
                case X:
                    xRadius = 1.0D;
                    break;
                case Y:
                    yRadius = 1.0D;
                    break;
                case Z:
                    zRadius = 1.0D;
            }

            // Divide by pixel amounts by two to get radius in pixels
            xRadius = xRadius / 16.0D / 2.0D;
            yRadius = yRadius / 16.0D / 2.0D;
            zRadius = zRadius / 16.0D / 2.0D;

            this.setBoundingBox(new AxisAlignedBB(xCenter - xRadius, yCenter - yRadius, zCenter - zRadius, xCenter + xRadius, yCenter + yRadius, zCenter + zRadius));
        }
    }

    public void writeAdditional(CompoundNBT compound) {
        compound.putByte(NBT_TAG_FACING, (byte)this.facingDirection.getHorizontalIndex());
        compound.putString(NBT_TAG_CANVAS_NAME, this.canvasName);
        super.writeAdditional(compound);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditional(CompoundNBT compound) {
        this.facingDirection = Direction.byHorizontalIndex(compound.getByte(NBT_TAG_FACING));

        if (compound.contains(NBT_TAG_CANVAS_NAME, 8)) {
            this.canvasName = compound.getString(NBT_TAG_CANVAS_NAME);
        }

        super.readAdditional(compound);
        this.updateFacingWithBoundingBox(this.facingDirection);
    }

    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeByte((byte)this.facingDirection.getHorizontalIndex());
        buffer.writeString(this.canvasName, 64);
    }

    public void readSpawnData(PacketBuffer buffer) {
        this.facingDirection = Direction.byHorizontalIndex(buffer.readByte());
        this.canvasName = buffer.readString(64);
    }

    public int getWidthPixels() {
        return CanvasItem.CANVAS_SIZE;
    }

    public int getHeightPixels() {
        return CanvasItem.CANVAS_SIZE;
    }

    /**
     * Called when this entity is broken. Entity parameter may be null.
     */
    public void onBroken(@Nullable Entity brokenEntity) {
        if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            this.playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);
            if (brokenEntity instanceof PlayerEntity) {
                PlayerEntity playerentity = (PlayerEntity)brokenEntity;
                if (playerentity.abilities.isCreativeMode) {
                    return;
                }
            }

            // todo: change this
            this.entityDropItem(Items.PAINTING);
        }
    }

    public void playPlaceSound() {
        this.playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
    }

    /**
     * Sets the location and Yaw/Pitch of an entity in the world
     */
    public void setLocationAndAngles(double x, double y, double z, float yaw, float pitch) {
        this.setPosition(x, y, z);
    }

    /**
     * Sets a target for the client to interpolate towards over the next few ticks
     */
    @OnlyIn(Dist.CLIENT)
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        BlockPos blockpos = this.hangingPosition.add(x - this.getPosX(), y - this.getPosY(), z - this.getPosZ());
        this.setPosition(blockpos.getX(), blockpos.getY(), blockpos.getZ());
    }

    /* This sends a vanilla spawn packet, which is then silently discarded when it reaches the client.
     * Your entity will be present on the server and can cause effects, but the client will not have a copy of the entity
     * and hence it will not render. */
    @Nonnull
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

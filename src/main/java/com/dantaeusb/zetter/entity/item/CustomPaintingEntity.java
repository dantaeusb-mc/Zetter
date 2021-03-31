package com.dantaeusb.zetter.entity;

import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.item.CanvasItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CustomPaintingEntity extends HangingEntity {
    public static final String NBT_TAG_CANVAS_NAME = "CanvasName";
    public static final String NBT_TAG_FACING = "Facing";

    public String canvasName;

    public CustomPaintingEntity(EntityType<? extends CustomPaintingEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public CustomPaintingEntity(World worldIn, BlockPos pos, Direction facing) {
        super(ModEntities.CUSTOM_PAINTING_ENTITY, worldIn, pos);

        this.updateFacingWithBoundingBox(facing);
    }

    @OnlyIn(Dist.CLIENT)
    public CustomPaintingEntity(World worldIn, BlockPos pos, Direction facing, String canvas) {
        this(worldIn, pos, facing);
        this.canvasName = canvas;
        this.updateFacingWithBoundingBox(facing);
    }

    public String getCanvasName() {
        return this.canvasName;
    }

    public void setCanvasName(String canvasName) {
        this.canvasName = canvasName;
    }

    public void writeAdditional(CompoundNBT compound) {
        compound.putString(NBT_TAG_CANVAS_NAME, this.canvasName);
        compound.putByte(NBT_TAG_FACING, (byte)this.facingDirection.getHorizontalIndex());
        super.writeAdditional(compound);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditional(CompoundNBT compound) {
        this.canvasName = compound.getString(NBT_TAG_CANVAS_NAME);
        this.facingDirection = Direction.byHorizontalIndex(compound.getByte(NBT_TAG_FACING));
        super.readAdditional(compound);
        this.updateFacingWithBoundingBox(this.facingDirection);
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

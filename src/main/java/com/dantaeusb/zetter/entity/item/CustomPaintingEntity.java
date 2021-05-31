package com.dantaeusb.zetter.entity.item;

import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.CustomPaintingItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
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
    public static final String NBT_TAG_CANVAS_CODE = "CanvasCode";
    public static final String NBT_TAG_TITLE = "Title";
    public static final String NBT_TAG_AUTHOR_NAME = "AuthorName";
    public static final String NBT_TAG_BLOCK_SIZE = "BlockSize";

    protected String canvasCode;
    protected String paintingName;
    protected String authorName;

    /**
     * This data is derivative from canvas data and duplicates width/height attributes
     * However as canvas controlled by capability and loaded asynchronously we
     * need this data in order to place entity properly and pre-render by adding
     * this data to spawn packet
     */
    protected int blockWidth;
    protected int blockHeight;

    public CustomPaintingEntity(EntityType<? extends CustomPaintingEntity> type, World worldIn) {
        super(type, worldIn);
    }

    public CustomPaintingEntity(World worldIn, BlockPos pos, Direction facing, String canvasCode, String paintingName, String authorName, int[] blockSize) {
        super(ModEntities.CUSTOM_PAINTING_ENTITY, worldIn, pos);

        this.canvasCode = canvasCode;
        this.paintingName = paintingName;
        this.authorName = authorName;

        this.blockWidth = blockSize[0];
        this.blockHeight = blockSize[1];

        this.updateFacingWithBoundingBox(facing);
    }

    public String getCanvasCode() {
        if (this.canvasCode == null) {
            return Helper.fallbackCanvasName;
        }

        return this.canvasCode;
    }

    protected float getEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return 0.0F;
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
            double xCenter = (double)this.hangingPosition.getX() + 0.5D;
            double yCenter = (double)this.hangingPosition.getY() + 0.5D;
            double zCenter = (double)this.hangingPosition.getZ() + 0.5D;

            double thicknessOffset = 0.5D - (1.0D / 32.0D);

            double hCenterOffset = this.offs(this.getWidthPixels());
            double vCenterOffset = this.offs(this.getHeightPixels());

            xCenter = xCenter - (double)this.facingDirection.getXOffset() * thicknessOffset;
            zCenter = zCenter - (double)this.facingDirection.getZOffset() * thicknessOffset;

            yCenter = yCenter + vCenterOffset;

            Direction direction = this.facingDirection.rotateY();

            xCenter = xCenter + hCenterOffset * (double)direction.getXOffset();
            zCenter = zCenter + hCenterOffset * (double)direction.getZOffset();

            this.setRawPosition(xCenter, yCenter, zCenter);

            double xWidth = (double)this.getWidthPixels();
            double yHeight = (double)this.getHeightPixels();
            double zWidth = (double)this.getWidthPixels();

            if (this.facingDirection.getAxis() == Direction.Axis.Z) {
                zWidth = 1.0D;
            } else {
                xWidth = 1.0D;
            }

            xWidth = xWidth / 16.0D / 2.0D;
            yHeight = yHeight / 16.0D / 2.0D;
            zWidth = zWidth / 16.0D / 2.0D;

            this.setBoundingBox(new AxisAlignedBB(
                xCenter - xWidth, yCenter - yHeight, zCenter - zWidth,
                xCenter + xWidth, yCenter + yHeight, zCenter + zWidth
            ));
        }
    }

    /**
     * Offset from hanging block position to geometrical center of the picture
     * if pixel size is twice as resolution, like 32, 64
     * [][]*[][]
     * that means that block edge is geometrical center. If it's not, like 48 or 16,
     * we have to move center to the middle of the block
     * [][*][]
     * but because center is adjusted by 0.5 by default, we're returning
     * the opposite
     */
    private double offs(int pixelSize) {
        return pixelSize % (Helper.CANVAS_TEXTURE_RESOLUTION * 2) == 0 ? -0.5D : 0.0D;
    }

    public void writeAdditional(CompoundNBT compound) {
        compound.putByte(NBT_TAG_FACING, (byte)this.facingDirection.getHorizontalIndex());
        compound.putString(NBT_TAG_CANVAS_CODE, this.canvasCode);
        compound.putString(NBT_TAG_TITLE, this.paintingName);
        compound.putString(NBT_TAG_AUTHOR_NAME, this.authorName);
        compound.putIntArray(NBT_TAG_BLOCK_SIZE, new int[]{this.blockWidth, this.blockHeight});

        super.writeAdditional(compound);
    }

    public double[] getRenderOffset() {
        final double xOffset = this.blockWidth / 2.0D;
        final double yOffset = this.blockHeight / 2.0D;

        return new double[]{xOffset, yOffset};
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditional(CompoundNBT compound) {
        this.facingDirection = Direction.byHorizontalIndex(compound.getByte(NBT_TAG_FACING));

        if (compound.contains(NBT_TAG_CANVAS_CODE, 8)) {
            this.canvasCode = compound.getString(NBT_TAG_CANVAS_CODE);
        }

        if (compound.contains(NBT_TAG_TITLE, 8)) {
            this.paintingName = compound.getString(NBT_TAG_TITLE);
        }

        if (compound.contains(NBT_TAG_AUTHOR_NAME, 8)) {
            this.authorName = compound.getString(NBT_TAG_AUTHOR_NAME);
        }

        if (compound.contains(NBT_TAG_BLOCK_SIZE)) {
            int[] blockSize = compound.getIntArray(NBT_TAG_BLOCK_SIZE);
            this.blockWidth = blockSize[0];
            this.blockHeight = blockSize[1];
        }

        super.readAdditional(compound);
        this.updateFacingWithBoundingBox(this.facingDirection);
    }

    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeByte((byte)this.facingDirection.getHorizontalIndex());
        buffer.writeString(this.canvasCode, 64);

        buffer.writeInt(this.blockWidth);
        buffer.writeInt(this.blockHeight);
    }

    public void readSpawnData(PacketBuffer buffer) {
        this.facingDirection = Direction.byHorizontalIndex(buffer.readByte());
        this.canvasCode = buffer.readString(64);

        this.blockWidth = buffer.readInt();
        this.blockHeight = buffer.readInt();

        this.updateFacingWithBoundingBox(this.facingDirection);
    }

    /**
     * Multiplying by 16 because extended class does not expect to support
     * Multiple image resolutions
     * @return
     */
    public int getWidthPixels() {
        return this.blockWidth * 16;
    }

    public int getHeightPixels() {
        return this.blockHeight * 16;
    }

    /**
     * Checks if the entity is in range to render.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        double d0 = 16.0D;
        d0 = d0 * 64.0D * getRenderDistanceWeight();
        return distance < d0 * d0;
    }

    /**
     * Called when this entity is broken. Entity parameter may be null.
     */
    public void onBroken(@Nullable Entity brokenEntity) {
        if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            this.playSound(SoundEvents.ENTITY_PAINTING_BREAK, 1.0F, 1.0F);

            ItemStack canvasStack = new ItemStack(ModItems.CUSTOM_PAINTING_ITEM);
            CustomPaintingItem.setCanvasName(canvasStack, this.canvasCode);
            CustomPaintingItem.setTitle(canvasStack, new StringTextComponent(this.paintingName));
            CustomPaintingItem.setAuthor(canvasStack, this.authorName);
            CustomPaintingItem.setBlockSize(canvasStack, new int[]{this.blockWidth, this.blockHeight});

            this.entityDropItem(canvasStack);
        }
    }

    public void playPlaceSound() {
        this.playSound(SoundEvents.ENTITY_PAINTING_PLACE, 1.0F, 1.0F);
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

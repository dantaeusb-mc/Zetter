package com.dantaeusb.zetter.entity.item;

import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.FrameItem;
import com.dantaeusb.zetter.storage.PaintingData;
import com.google.common.collect.Maps;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

public class CustomPaintingEntity extends HangingEntity implements IEntityAdditionalSpawnData {
    public static final String NBT_TAG_FACING = "Facing";
    public static final String NBT_TAG_PAINTING_CODE = "PaintingCode";
    public static final String NBT_TAG_BLOCK_SIZE = "BlockSize";
    public static final String NBT_TAG_MATERIAL = "Material";
    public static final String NBT_TAG_HAS_PLATE = "HasPlate";

    protected String canvasCode;

    /**
     * This data is derivative from canvas data and duplicates width/height attributes
     * However as canvas controlled by capability and loaded asynchronously we
     * need this data in order to place entity properly and pre-render by adding
     * this data to spawn packet
     */
    protected int blockWidth;
    protected int blockHeight;

    protected boolean hasPlate;

    protected Materials material;

    public CustomPaintingEntity(EntityType<? extends CustomPaintingEntity> type, World world) {
        super(type, world);
    }

    public CustomPaintingEntity(World world, BlockPos pos, Direction facing, Materials material, boolean hasPlate, String canvasCode, int[] blockSize) {
        super(ModEntities.CUSTOM_PAINTING_ENTITY, world, pos);

        this.material = material;
        this.hasPlate = hasPlate;

        this.canvasCode = canvasCode;

        this.blockWidth = blockSize[0];
        this.blockHeight = blockSize[1];

        this.setDirection(facing);
    }

    public String getCanvasCode() {
        if (this.canvasCode == null) {
            return Helper.FALLBACK_CANVAS_CODE;
        }

        return this.canvasCode;
    }

    public int getBlockWidth() {
        return this.blockWidth;
    }

    public int getBlockHeight() {
        return this.blockHeight;
    }

    public Materials getMaterial() {
        return this.material;
    }

    public boolean hasPlate() {
        return this.hasPlate;
    }

    protected float getEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return 0.0F;
    }

    /**
     * Updates facing and bounding box based on it
     */
    protected void setDirection(Direction facingDirectionIn) {
        Validate.notNull(facingDirectionIn);
        this.direction = facingDirectionIn;
        if (facingDirectionIn.getAxis().isHorizontal()) {
            this.xRot = 0.0F;
            this.yRot = (float)(this.direction.get2DDataValue() * 90);
        } else {
            this.xRot = (float)(-90 * facingDirectionIn.getAxisDirection().getStep());
            this.yRot = 0.0F;
        }

        this.xRotO = this.xRot;
        this.yRotO = this.yRot;
        this.recalculateBoundingBox();
    }

    /**
     * Updates the entity bounding box based on current facing
     */
    protected void recalculateBoundingBox() {
        if (this.direction != null) {
            double xCenter = (double)this.pos.getX() + 0.5D;
            double yCenter = (double)this.pos.getY() + 0.5D;
            double zCenter = (double)this.pos.getZ() + 0.5D;

            double thicknessOffset = 0.5D - (1.0D / 32.0D);

            double hCenterOffset = this.offs(this.getWidth());
            double vCenterOffset = this.offs(this.getHeight());

            xCenter = xCenter - (double)this.direction.getStepX() * thicknessOffset;
            zCenter = zCenter - (double)this.direction.getStepZ() * thicknessOffset;

            yCenter = yCenter + vCenterOffset;

            Direction direction = this.direction.getCounterClockWise();

            xCenter = xCenter + hCenterOffset * (double)direction.getStepX();
            zCenter = zCenter + hCenterOffset * (double)direction.getStepZ();

            this.setPosRaw(xCenter, yCenter, zCenter);

            double xWidth = this.getWidth();
            double yHeight = this.getHeight();
            double zWidth = this.getWidth();

            if (this.direction.getAxis() == Direction.Axis.Z) {
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
        return pixelSize % (Helper.getResolution().getNumeric() * 2) == 0 ? 0.5D : 0.0D;
    }

    public double[] getRenderOffset() {
        final double xOffset = this.blockWidth / 2.0D;
        final double yOffset = this.blockHeight / 2.0D;

        return new double[]{xOffset, yOffset};
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (!this.hasPlate()) {
            return ActionResultType.PASS;
        }

        if (!player.getCommandSenderWorld().isClientSide()) {
            return ActionResultType.CONSUME;
        }

        PaintingData paintingData = Helper.getWorldCanvasTracker(this.level).getCanvasData(this.canvasCode, PaintingData.class);

        String paintingName = paintingData.getPaintingName();
        String authorName = paintingData.getAuthorName();

        if (StringUtils.isNullOrEmpty(paintingName)) {
            paintingName = new TranslationTextComponent("item.zetter.painting.unnamed").getString();
        }

        if (StringUtils.isNullOrEmpty(authorName)) {
            authorName = new TranslationTextComponent("item.zetter.painting.unknown").getString();
        }

        player.displayClientMessage(
            new TranslationTextComponent("item.zetter.customPaintingByAuthor", paintingName, authorName),
            true
        );

        return ActionResultType.CONSUME;
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        compound.putByte(NBT_TAG_FACING, (byte)this.direction.get2DDataValue());
        compound.putString(NBT_TAG_PAINTING_CODE, this.canvasCode);
        compound.putIntArray(NBT_TAG_BLOCK_SIZE, new int[]{this.blockWidth, this.blockHeight});
        compound.putString(NBT_TAG_MATERIAL, this.material.toString());
        compound.putBoolean(NBT_TAG_HAS_PLATE, this.hasPlate);

        super.addAdditionalSaveData(compound);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditionalSaveData(CompoundNBT compound) {
        this.direction = Direction.from2DDataValue(compound.getByte(NBT_TAG_FACING));
        this.canvasCode = compound.getString(NBT_TAG_PAINTING_CODE);

        if (compound.contains(NBT_TAG_BLOCK_SIZE)) {
            int[] blockSize = compound.getIntArray(NBT_TAG_BLOCK_SIZE);
            this.blockWidth = blockSize[0];
            this.blockHeight = blockSize[1];
        }

        if (compound.contains(NBT_TAG_MATERIAL)) {
            this.material = Materials.fromString(compound.getString(NBT_TAG_MATERIAL));
        } else {
            // @todo: replace to OAK on release
            this.material = Materials.DARK_OAK;
        }

        if (compound.contains(NBT_TAG_HAS_PLATE)) {
            this.hasPlate = compound.getBoolean(NBT_TAG_HAS_PLATE);
        } else {
            // @todo: remove on release?
            this.hasPlate = false;
        }

        super.readAdditionalSaveData(compound);
        this.setDirection(this.direction);
    }

    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeByte((byte)this.direction.get2DDataValue());

        buffer.writeUtf(this.canvasCode, 64);

        buffer.writeInt(this.blockWidth);
        buffer.writeInt(this.blockHeight);

        buffer.writeUtf(this.material.toString(), 64);
        buffer.writeBoolean(this.hasPlate);
    }

    public void readSpawnData(PacketBuffer buffer) {
        this.pos = buffer.readBlockPos();
        this.direction = Direction.from2DDataValue(buffer.readByte());

        this.canvasCode = buffer.readUtf(64);

        this.blockWidth = buffer.readInt();
        this.blockHeight = buffer.readInt();

        this.material = Materials.fromString(buffer.readUtf(64));
        this.hasPlate = buffer.readBoolean();

        this.setDirection(this.direction);
    }

    /**
     * Multiplying by 16 because extended class does not expect to support
     * Multiple image resolutions
     * @return
     */
    public int getWidth() {
        return this.blockWidth * Helper.getResolution().getNumeric();
    }

    public int getHeight() {
        return this.blockHeight * Helper.getResolution().getNumeric();
    }

    /**
     * Checks if the entity is in range to render.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = 16.0D;
        d0 = d0 * 64.0D * getViewScale();
        return distance < d0 * d0;
    }

    /**
     * Called when this entity is broken. Entity parameter may be null.
     */
    public void dropItem(@Nullable Entity brokenEntity) {
        // @todo: remove item if canvas code is set to fallback code
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);

            ItemStack canvasStack = new ItemStack(ModItems.FRAMES.get(Helper.getFrameKey(this.material, this.hasPlate)));

            PaintingData paintingData = Helper.getWorldCanvasTracker(this.level).getCanvasData(this.canvasCode, PaintingData.class);

            FrameItem.setPaintingData(canvasStack, paintingData);
            FrameItem.setBlockSize(canvasStack, new int[]{this.blockWidth, this.blockHeight});

            this.spawnAtLocation(canvasStack);
        }
    }

    /**
     * Sets the location and Yaw/Pitch of an entity in the world
     * Do not re-center bounding box
     * Copied from PaintingEntity
     */
    public void moveTo(double x, double y, double z, float yaw, float pitch) {
        this.setPos(x, y, z);
    }

    /**
     * Sets a target for the client to interpolate towards over the next few ticks
     * Copied from PaintingEntity
     */
    @OnlyIn(Dist.CLIENT)
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        BlockPos blockpos = this.pos.offset(x - this.getX(), y - this.getY(), z - this.getZ());
        this.setPos(blockpos.getX(), blockpos.getY(), blockpos.getZ());
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Nonnull
    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public enum Materials {
        ACACIA("acacia", true, true),
        BIRCH("birch", true, true),
        DARK_OAK("dark_oak", true, true),
        JUNGLE("jungle", true, true),
        OAK("oak", true, true),
        SPRUCE("spruce", true, true),
        CRIMSON("crimson", true, true),
        WARPED("warped", true, true),
        IRON("iron", false, false),
        GOLD("gold", true, true);

        private static final Map<String, Materials> LOOKUP = Maps.uniqueIndex(
                Arrays.asList(Materials.values()),
                Materials::toString
        );

        private final String text;
        private final boolean canvasOffset;
        private final boolean canHavePlate;

        Materials(final String text, final boolean canvasOffset, boolean canHavePlate) {
            this.text = text;
            this.canvasOffset = canvasOffset;
            this.canHavePlate = canHavePlate;
        }

        @Override
        public String toString() {
            return text;
        }

        public boolean hasOffset() {
            return this.canvasOffset;
        }

        public boolean canHavePlate() {
            return this.canHavePlate;
        }

        @Nullable
        public static Materials fromString(String stringValue) {
            return LOOKUP.get(stringValue);
        }
    }
}

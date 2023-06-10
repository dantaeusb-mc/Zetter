package me.dantaeusb.zetter.entity.item;

import com.google.common.collect.Maps;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterEntities;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

public class PaintingEntity extends HangingEntity implements IEntityAdditionalSpawnData {
    public static final String NBT_TAG_FACING = "Facing";
    public static final String NBT_TAG_BLOCK_SIZE = "BlockSize";
    public static final String NBT_TAG_MATERIAL = "Material";
    public static final String NBT_TAG_HAS_PLATE = "HasPlate";
    public static final String NBT_TAG_GENERATION = "Generation";

    protected String paintingCode;

    /**
     * This data is derivative from canvas data and duplicates width/height attributes
     * However as canvas controlled by capability and loaded asynchronously we
     * need this data in order to place entity properly and pre-render by adding
     * this data to spawn packet
     */
    protected int blockWidth;
    protected int blockHeight;

    protected boolean hasPlate;
    protected int generation;

    protected Materials material;

    public PaintingEntity(EntityType<? extends PaintingEntity> type, Level world) {
        super(type, world);
    }

    public PaintingEntity(Level world, BlockPos pos, Direction facing, Materials material, boolean hasPlate, String canvasCode, int[] blockSize, int generation) {
        super(ZetterEntities.FRAMED_PAINTING_ENTITY.get(), world, pos);

        this.material = material;
        this.hasPlate = hasPlate;

        this.paintingCode = canvasCode;

        this.blockWidth = blockSize[0];
        this.blockHeight = blockSize[1];

        this.generation = generation;

        this.setDirection(facing);
    }

    public String getPaintingCode() {
        if (this.paintingCode == null) {
            return Helper.FALLBACK_CANVAS_CODE;
        }

        return this.paintingCode;
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

    protected float getEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
        return 0.0F;
    }

    /**
     * Updates facing and bounding box based on it
     */
    protected void setDirection(Direction facingDirectionIn) {
        Validate.notNull(facingDirectionIn);
        this.direction = facingDirectionIn;
        if (facingDirectionIn.getAxis().isHorizontal()) {
            this.setXRot(0.0F);
            this.setYRot((float)(this.direction.get2DDataValue() * 90));
        } else {
            this.setXRot(-90 * facingDirectionIn.getAxisDirection().getStep());
            this.setYRot(0.0F);
        }

        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
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

            this.setBoundingBox(new AABB(
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.hasPlate()) {
            return InteractionResult.PASS;
        }

        if (!player.getCommandSenderWorld().isClientSide()) {
            return InteractionResult.CONSUME;
        }

        PaintingData paintingData = Helper.getLevelCanvasTracker(this.level()).getCanvasData(this.paintingCode);

        if (paintingData == null) {
            return InteractionResult.FAIL;
        }

        ClientHelper.showOverlay(paintingData);

        return InteractionResult.CONSUME;
    }

    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putByte(NBT_TAG_FACING, (byte)this.direction.get2DDataValue());
        compoundTag.putString(PaintingItem.NBT_TAG_PAINTING_CODE, this.paintingCode);
        compoundTag.putIntArray(NBT_TAG_BLOCK_SIZE, new int[]{this.blockWidth, this.blockHeight});
        compoundTag.putString(NBT_TAG_MATERIAL, this.material.toString());
        compoundTag.putBoolean(NBT_TAG_HAS_PLATE, this.hasPlate);
        compoundTag.putInt(NBT_TAG_GENERATION, this.generation);

        super.addAdditionalSaveData(compoundTag);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    public void readAdditionalSaveData(CompoundTag compound) {
        this.direction = Direction.from2DDataValue(compound.getByte(NBT_TAG_FACING));
        this.paintingCode = compound.getString(PaintingItem.NBT_TAG_PAINTING_CODE);

        if (compound.contains(NBT_TAG_BLOCK_SIZE)) {
            int[] blockSize = compound.getIntArray(NBT_TAG_BLOCK_SIZE);
            this.blockWidth = blockSize[0];
            this.blockHeight = blockSize[1];
        }

        if (compound.contains(NBT_TAG_MATERIAL)) {
            this.material = Materials.fromString(compound.getString(NBT_TAG_MATERIAL));
        } else {
            this.material = Materials.OAK;
        }

        if (compound.contains(NBT_TAG_HAS_PLATE)) {
            this.hasPlate = compound.getBoolean(NBT_TAG_HAS_PLATE);
        } else {
            // @todo: [LOW] Remove on release after 0.18.x
            this.hasPlate = false;
        }

        if (compound.contains(NBT_TAG_GENERATION)) {
            this.generation = compound.getInt(NBT_TAG_GENERATION);
        } else {
            // Only originals were available before this tag was added
            this.generation = 0;
        }

        super.readAdditionalSaveData(compound);
        this.setDirection(this.direction);
    }

    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeByte((byte)this.direction.get2DDataValue());

        buffer.writeUtf(this.paintingCode, Helper.CANVAS_CODE_MAX_LENGTH);

        buffer.writeInt(this.blockWidth);
        buffer.writeInt(this.blockHeight);

        buffer.writeUtf(this.material.toString(), 64);
        buffer.writeBoolean(this.hasPlate);
    }

    public void readSpawnData(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.direction = Direction.from2DDataValue(buffer.readByte());

        this.paintingCode = buffer.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

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
        return this.blockWidth * 16;
    }

    public int getHeight() {
        return this.blockHeight * 16;
    }

    /**
     * Checks if the entity is in range to render.
     */
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d0 = 16.0D;
        d0 = d0 * 64.0D * getViewScale();
        return distance < d0 * d0;
    }

    /**
     * Called when this entity is broken. Entity parameter may be null.
     */
    public void dropItem(@Nullable Entity brokenEntity) {
        // @todo: [MED] Remove item if canvas code is set to fallback code
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);

            ItemStack canvasStack = new ItemStack(ZetterItems.FRAMES.get(Helper.getFrameKey(this.material, this.hasPlate)).get());

            PaintingData paintingData = Helper.getLevelCanvasTracker(this.level()).getCanvasData(this.paintingCode);

            FrameItem.storePaintingData(canvasStack, this.paintingCode, paintingData, this.generation);

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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        BlockPos blockpos = this.pos.offset((int) (x - this.getX()), (int) (y - this.getY()), (int) (z - this.getZ()));
        this.setPos(blockpos.getX(), blockpos.getY(), blockpos.getZ());
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ZetterItems.FRAMES.get(Helper.getFrameKey(this.getMaterial(), this.hasPlate())).get());
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
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
        GOLD("gold", true, true),
        MANGROVE("mangrove", true, true);

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

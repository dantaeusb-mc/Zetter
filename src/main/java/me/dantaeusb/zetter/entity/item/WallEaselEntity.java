package me.dantaeusb.zetter.entity.item;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.container.CanvasContainer;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class WallEaselEntity extends CanvasHolderEntity implements ItemStackHandlerListener {
    private static final String NBT_TAG_EASEL_STORAGE = "Storage";
    private static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

    private static final Vector3f CANVAS_CENTER_OFFSET = new Vector3f(-2.5f, 0.0625f, -2.5f);

    protected static final Predicate<Entity> IS_WALL_EASEL_ENTITY = (entity) -> entity instanceof WallEaselEntity;

    private static final EntityDataAccessor<String> DATA_ID_CANVAS_CODE = SynchedEntityData.defineId(WallEaselEntity.class, EntityDataSerializers.STRING);

    protected BlockPos pos;
    protected CanvasContainer easelContainer;

    protected Vector3f canvasOffset;
    protected Vector3f canvasNormal;
    protected Vector3f canvasU;
    protected Vector3f canvasV;
    protected Matrix4f canvasMatrix;

    protected final LazyOptional<ItemStackHandler> easelContainerOptional = LazyOptional.of(() -> this.easelContainer);

    private int tick;

    public WallEaselEntity(EntityType<? extends WallEaselEntity> type, Level world) {
        super(type, world);
        this.createInventory();
    }

    @Override
    public void setYRot(float yRot) {
        super.setYRot(yRot);

        this.updateCanvasVectors();
    }

    /**
     * Updates the entity bounding box based on current facing
     */
    /*protected void recalculateBoundingBox() {
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
    }*/

    protected void updateCanvasVectors() {
        final float scaleFactor = 1.0F / 16.0F;

        Vector3f offset = new Vector3f(CANVAS_CENTER_OFFSET);
        Vector3f normal = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f u = new Vector3f(-1.0f, 0.0f, 0.0f);
        Vector3f v = new Vector3f(0.0f, -1.0f, 0.0f);

        Quaternionf canvasXRotation = Axis.XP.rotationDegrees(10.0f);
        Quaternionf entityYRotation = Axis.YP.rotationDegrees(180.0F - this.getYRot());

        this.canvasOffset = entityYRotation.transform(offset);
        this.canvasNormal = entityYRotation.transform(canvasXRotation.transform(normal));
        this.canvasU = entityYRotation.transform(canvasXRotation.transform(u));
        this.canvasV = entityYRotation.transform(canvasXRotation.transform(v));

        this.canvasMatrix = new Matrix4f();
        // Not using getter as we already applied rotation
        this.canvasMatrix.translate(CANVAS_CENTER_OFFSET);
        this.canvasMatrix.scale(scaleFactor, scaleFactor, scaleFactor);
        this.canvasMatrix.rotate(Axis.XP.rotation(0.1745f));
        this.canvasMatrix.rotate(Axis.ZP.rotationDegrees(180.0f));

        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(this.level());
        CanvasData canvasData = canvasTracker.getCanvasData(this.getCanvasCode());

        if (canvasData == null) {
            return;
        }

        final int canvasBlockWidth = canvasData.getWidth() / canvasData.getResolution().getNumeric();
        final int canvasBlockHeight = canvasData.getHeight() / canvasData.getResolution().getNumeric();

        Vector3f canvasUOffset = new Vector3f(this.canvasU);
        canvasUOffset.mul(0.5f + -0.5f * canvasBlockWidth);

        Vector3f canvasVOffset = new Vector3f(this.canvasV);
        canvasVOffset.mul(1.0f -canvasBlockHeight);

        this.canvasOffset.add(canvasUOffset);
        this.canvasOffset.add(canvasVOffset);

        this.canvasMatrix.translate(-8.0f - (8.0f * canvasBlockWidth), -16.0f * canvasBlockHeight, 0.0f);
    }

    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_CANVAS_CODE, "");
    }

    public @Nullable String getCanvasCode() {
        String canvasCode = this.entityData.get(DATA_ID_CANVAS_CODE);

        if (canvasCode.isEmpty()) {
            return null;
        }

        return canvasCode;
    }

    public @Nullable CanvasData getCanvasData() {
        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(this.level());
        return canvasTracker.getCanvasData(this.getCanvasCode());
    }

    /**
     * Set canvas code for referencing without loading the items to
     * render canvas on easel
     *
     * @param canvasCode
     */
    protected void setCanvasCode(@Nullable String canvasCode) {
        if (canvasCode != null) {
            this.entityData.set(DATA_ID_CANVAS_CODE, canvasCode);
        } else {
            this.entityData.set(DATA_ID_CANVAS_CODE, "");
        }
    }

    @Override
    public Vector3f getCanvasOffset() {
        return this.canvasOffset;
    }

    @Override
    public Vector3f getCanvasNormal() {
        return this.canvasNormal;
    }

    @Override
    public Vector3f getCanvasU() {
        return this.canvasU;
    }

    @Override
    public Vector3f getCanvasV() {
        return this.canvasV;
    }

    @Override
    public Matrix4f getCanvasMatrixTransform(float partialTicks) {
        return this.canvasMatrix;
    }

    @Override
    public boolean playerCanDraw(Player player) {
        // @todo: Add frustum check?
        return this.hasCanvas();
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void createInventory() {
        CanvasContainer currentEaselStorage = this.easelContainer;
        this.easelContainer = new CanvasContainer(this);

        if (currentEaselStorage != null) {
            currentEaselStorage.removeListener(this);
            int i = Math.min(currentEaselStorage.getSlots(), this.easelContainer.getSlots());

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = currentEaselStorage.getStackInSlot(j);
                if (!itemstack.isEmpty()) {
                    this.easelContainer.setStackInSlot(j, itemstack.copy());
                }
            }
        }

        this.easelContainer.addListener(this);
        //this.updateDataFromInventory();
    }

    /**
     * If canvas does not exist, set to null
     * If exists but not initialized, set to default
     * If exists and initialized, use code
     */
    protected void updateEntityDataFromInventory() {
        ItemStack canvasStack = this.easelContainer.getCanvasStack();

        if (canvasStack.isEmpty()) {
            this.setCanvasCode(null);
            this.updateCanvasVectors();
            return;
        }

        String canvasCode = CanvasItem.getCanvasCode(canvasStack);

        if (canvasCode == null) {
            int[] size = CanvasItem.getBlockSize(canvasStack);
            assert size != null && size.length == 2;

            canvasCode = CanvasData.getDefaultCanvasCode(size[0], size[1]);
        }

        this.setCanvasCode(canvasCode);
        this.updateCanvasVectors();
    }

    public boolean canPlayerAccessInventory(Player player) {
        // @todo: [HIGH] Implement check
        return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
        if (capability == ForgeCapabilities.ITEM_HANDLER
            && (direction == null || direction == Direction.UP || direction == Direction.DOWN)) {
            return this.easelContainerOptional.cast();
        }

        return super.getCapability(capability, direction);
    }

    /**
     * This is temporary for migrating from BE to Entity
     *
     * @return
     */
    public CanvasContainer getEaselContainer() {
        return this.easelContainer;
    }

    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put(NBT_TAG_EASEL_STORAGE, this.easelContainer.serializeNBT());

        if (this.getCanvasCode() != null) {
            compoundTag.putString(NBT_TAG_CANVAS_CODE, this.getCanvasCode());
        }
    }

    public void readAdditionalSaveData(CompoundTag compoundTag) {
        this.createInventory();

        this.easelContainer.deserializeNBT(compoundTag.getCompound(NBT_TAG_EASEL_STORAGE));

        final String canvasCode = compoundTag.getString(NBT_TAG_CANVAS_CODE);

        if (canvasCode != null) {
            this.setCanvasCode(canvasCode);
        }
    }

    /**
     * Needed for entity interaction
     *
     * @return
     */
    public boolean isPickable() {
        return !this.isRemoved();
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isCrouching() && heldItem.isEmpty()) {
            ItemStack canvasStack = this.easelContainer.extractCanvasStack();
            player.setItemInHand(hand, canvasStack);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (heldItem.is(ZetterItems.CANVAS.get())) {
            if (this.easelContainer.getCanvasStack().isEmpty() && this.easelContainer.isItemValid(CanvasContainer.CANVAS_SLOT, heldItem)) {
                this.easelContainer.setCanvasStack(heldItem);
                player.setItemInHand(hand, ItemStack.EMPTY);

                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * Check history, check that still exists,
     * check if need to keep information
     */
    public void tick() {
        super.tick();
        this.tick++;

        // No need to check correctness and players on client side
        if (this.level().isClientSide()) {
            return;
        }

        this.checkBelowWorld();
        if (this.tick % 200 == 0) {
            this.checkPlayersUsing();
        }

        if (this.tick % 100 == 0) {
            if (!this.isRemoved() && !this.survives()) {
                this.discard();
                this.dropItem(null);
                this.dropAllContents(this.level(), this.getPos());
            }
        }
    }

    public boolean survives() {
        if (!this.level().noCollision(this)) {
            return false;
        } else {
            BlockPos posBelow = this.getPos().below();
            BlockState blockBelowState = this.level().getBlockState(posBelow);

            if (!blockBelowState.isSolid() && !DiodeBlock.isDiode(blockBelowState)) {
                return false;
            }

            return this.level().getEntities(this, this.getBoundingBox(), IS_WALL_EASEL_ENTITY).isEmpty();
        }
    }

    // specific

    @Override
    public boolean hasCanvas() {
        return this.getCanvasCode() != null;
    }

    public @Nullable ItemStack getCanvasStack() {
        return this.easelContainer.getCanvasStack();
    }

    public @Nullable ItemStack getPaletteStack(Player player) {
        return this.playersPalettes.get(player.getUUID());
    }

    public void containerChanged(ItemStackHandler easelContainer, int slot) {
        ItemStack canvasStack = ((CanvasContainer) easelContainer).getCanvasStack();
        String newCanvasCode = null;
        String existingCanvasCode = null;

        if (!canvasStack.isEmpty()) {
            newCanvasCode = CanvasItem.getCanvasCode(canvasStack);

            // Initialize canvas
            if (newCanvasCode == null) {
                CanvasItem.getCanvasData(canvasStack, this.level());
                newCanvasCode = CanvasItem.getCanvasCode(canvasStack);
            }
        }

        if (this.getEaselContainer().getCanvas() != null) {
            existingCanvasCode = this.getEaselContainer().getCanvas().code;
        }

        // @todo: [HIGH] Supposedly won't work on client if new canvas is not yet initialized, because it'll have nullish code
        // Canvas changed, drop state
        if (newCanvasCode == null || !newCanvasCode.equals(existingCanvasCode)) {
            this.canvasState.reset();
        }

        this.updateEntityDataFromInventory();
    }

    /**
     * Normally players subscribe and unsubscribe manually when stopping drawing
     * but sometimes we might miss unsubscribing, i.e. in case of player disconnect.
     * We are ticking updates to check if player still could be using easel.
     *
     * @return
     */
    public List<Player> checkPlayersUsing() {
        List<Player> possiblyUsingPlayers = this.level().getEntitiesOfClass(Player.class, new AABB(this.pos.offset(-5, -5, -5), this.pos.offset(5, 5, 5)));

        /**
         * @todo: [MED] Sending a packet just in case?
         */
        this.playersUsing.removeIf(player ->
            !possiblyUsingPlayers.contains(player)
                || !this.canPlayerAccessInventory(player)
                || !player.isAlive()
                || !player.getItemInHand(player.getUsedItemHand()).is(ZetterItems.PALETTE.get())
        );

        return this.playersUsing;
    }

    public void setPos(double x, double y, double z) {
        this.pos = new BlockPos((int) x, (int) y, (int) z);
        this.setPosRaw(x, y, z);
        this.setBoundingBox(this.makeBoundingBox());
        this.hasImpulse = true;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    /**
     * Drop contents and item then die when moved or hurt
     */

    public boolean hurt(DamageSource damageSource, float pAmount) {
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else {
            if (!this.level().isClientSide()) {
                if (!this.isRemoved()) {
                    this.kill();
                    this.markHurt();
                    this.dropItem(damageSource.getEntity());
                    this.dropAllContents(this.level(), this.pos);
                }
            }
            return true;
        }
    }

    public void move(MoverType mover, Vec3 move) {
        if (!this.level().isClientSide() && !this.isRemoved() && move.lengthSqr() > 0.0D) {
            this.kill();
            this.dropItem(null);
            this.dropAllContents(this.level(), this.pos);
        }
    }

    /**
     * @param x
     * @param y
     * @param z
     * @todo: [LOW] Rename params
     */
    public void push(double x, double y, double z) {
        if (!this.level().isClientSide && !this.isRemoved() && x * x + y * y + z * z > 0.0D) {
            this.kill();
            this.dropItem(null);
            this.dropAllContents(this.level(), this.pos);
        }
    }

    /**
     * Drop an item associated with this entity
     *
     * @param entity
     */
    public void dropItem(@Nullable Entity entity) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if (player.getAbilities().instabuild) {
                    return;
                }
            }

            this.spawnAtLocation(ZetterItems.WALL_EASEL.get());
        }
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     *
     * @param level
     * @param blockPos
     */
    public void dropAllContents(Level level, BlockPos blockPos) {
        for (int i = 0; i < this.easelContainer.getSlots(); i++) {
            Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.easelContainer.getStackInSlot(i));
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ZetterItems.WALL_EASEL.get());
    }

    /**
     * Copied from armor stand
     *
     * @return
     */

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }
}

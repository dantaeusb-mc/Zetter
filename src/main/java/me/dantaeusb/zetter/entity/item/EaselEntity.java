package me.dantaeusb.zetter.entity.item;

import com.mojang.math.Axis;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import me.dantaeusb.zetter.entity.item.state.EaselState;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.menu.EaselMenu;
import me.dantaeusb.zetter.network.packet.SEaselMenuCreatePacket;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;

public class EaselEntity extends Entity implements CanvasHolderEntity, ItemStackHandlerListener, MenuProvider {
    private static final String NBT_TAG_EASEL_STORAGE = "storage";
    private static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

    protected static final Predicate<Entity> IS_EASEL_ENTITY = (entity) -> {
        return entity instanceof EaselEntity;
    };

    private static final EntityDataAccessor<String> DATA_ID_CANVAS_CODE = SynchedEntityData.defineId(EaselEntity.class, EntityDataSerializers.STRING);

    protected BlockPos pos;
    protected EaselContainer easelContainer;
    protected EaselState stateHandler;

    protected final LazyOptional<ItemStackHandler> easelContainerOptional = LazyOptional.of(() -> this.easelContainer);

    /** The list of players currently using this easel */
    private ArrayList<Player> playersUsing = new ArrayList<>();

    private int tick;

    public EaselEntity(EntityType<? extends EaselEntity> type, Level world) {
        super(type, world);
        this.createInventory();
        this.stateHandler = new EaselState(this);
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

    /**
     * Set canvas code for referencing without loading the items to
     * render canvas on easel
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
    public Optional<Matrix4f> getCanvasMatrixTransform(float partialTicks) {
        CanvasData canvasData = CanvasItem.getCanvasData(this.getCanvasStack(), this.level());

        if (canvasData == null) {
            return Optional.empty();
        }

        final int canvasBlockWidth = canvasData.getWidth() / canvasData.getResolution().getNumeric();
        final int canvasBlockHeight = canvasData.getHeight() / canvasData.getResolution().getNumeric();

        final float scaleFactor = 1.0F / 16.0F;

        Matrix4f matrixTransform = new Matrix4f();
        matrixTransform.scale(scaleFactor, scaleFactor, scaleFactor);
        matrixTransform.translate(-8.0f, 12.5f, -4.0f);
        matrixTransform.rotate(Axis.XP.rotation(0.1745f));
        matrixTransform.rotate(Axis.ZP.rotationDegrees(180.0f));
        matrixTransform.translate(-8.0f - (8.0f * canvasBlockWidth), -16.0f * canvasBlockHeight, 0.0f);

        return Optional.of(matrixTransform);
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
        EaselContainer currentEaselStorage = this.easelContainer;
        this.easelContainer = new EaselContainer(this);

        if (currentEaselStorage != null) {
            currentEaselStorage.removeListener(this);
            int i = Math.min(currentEaselStorage.getSlots(), this.easelContainer.getSlots());

            for(int j = 0; j < i; ++j) {
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
            return;
        }

        String canvasCode = CanvasItem.getCanvasCode(canvasStack);

        if (canvasCode == null) {
            int[] size = CanvasItem.getBlockSize(canvasStack);
            assert size != null && size.length == 2;

            canvasCode = CanvasData.getDefaultCanvasCode(size[0], size[1]);
        }

        this.setCanvasCode(canvasCode);
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

    public EaselState getStateHandler() {
        return this.stateHandler;
    }

    /**
     * This is temporary for migrating from BE to Entity
     * @return
     */
    public EaselContainer getEaselContainer() {
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

        final boolean isCanvas = heldItem.is(ZetterItems.CANVAS.get());
        final boolean isPalette = heldItem.is(ZetterItems.PALETTE.get());

        if (isCanvas) {
            if (this.easelContainer.getCanvasStack().isEmpty() && this.easelContainer.isItemValid(EaselContainer.CANVAS_SLOT, heldItem)) {
                this.easelContainer.setCanvasStack(heldItem);
                player.setItemInHand(hand, ItemStack.EMPTY);

                return InteractionResult.sidedSuccess(this.level().isClientSide());
            }
        } else if (isPalette) {
            if (this.easelContainer.getPaletteStack().isEmpty()) {
                this.easelContainer.setPaletteStack(heldItem);
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }

        this.openInventory(player);

        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public void openInventory(Player player) {
        if (!this.level().isClientSide()) {
            NetworkHooks.openScreen((ServerPlayer) player, this, (packetBuffer) -> {
                SEaselMenuCreatePacket packet = new SEaselMenuCreatePacket(this.getId(), this.getCanvasCode());
                packet.writePacketData(packetBuffer);
            });
        }
    }

    /**
     * Check history, check that still exists,
     * check if need to keep information
     */
    public void tick() {
        this.stateHandler.tick();
        this.tick++;

        // No need to check correctness and players on client side
        if (this.level().isClientSide()) {
            return;
        }

        this.checkBelowWorld();
        if (this.tick % 200 == 0) {
            this.playersUsing = this.calculatePlayersUsing();
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

            return this.level().getEntities(this, this.getBoundingBox(), IS_EASEL_ENTITY).isEmpty();
        }
    }

    // specific

    public boolean hasCanvas() {
        return this.getCanvasCode() != null;
    }

    public @Nullable ItemStack getCanvasStack() {
        return this.easelContainer.getCanvasStack();
    }

    public boolean putCanvasStack(ItemStack itemStack) {
        if (itemStack.equals(ItemStack.EMPTY)) {
            this.easelContainer.setCanvasStack(itemStack);
            return true;
        }

        if (!itemStack.is(ZetterItems.CANVAS.get())) {
            Zetter.LOG.error("Trying to put non-canvas on easel, item likely will be removed");
            return false;
        }

        // @todo: [LOW] Seems dumb, lazy load ftw
        // Also could already have canvas, but it's client-only right now so we disregard client data
        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.level());
        this.easelContainer.setCanvasStack(itemStack);

        return true;
    }

    public void containerChanged(ItemStackHandler easelContainer, int slot) {
        ItemStack canvasStack = ((EaselContainer)easelContainer).getCanvasStack();
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
            this.stateHandler.reset();
        }

        this.updateEntityDataFromInventory();
    }

    // track using players to send packets

    /**
     * @return
     */
    public ArrayList<Player> calculatePlayersUsing() {
        ArrayList<Player> usingPlayers = new ArrayList<>();

        for(Player player : this.level().getEntitiesOfClass(Player.class, new AABB(this.pos.offset(-5, -5, -5), this.pos.offset(5, 5, 5)))) {
            if (player.containerMenu instanceof EaselMenu) {
                EaselContainer storage = ((EaselMenu)player.containerMenu).getContainer();

                if (storage == this.getEaselContainer()) {
                    usingPlayers.add(player);
                }
            }
        }

        return usingPlayers;
    }

    public ArrayList<Player> getPlayersUsing() {
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
     * @todo: [LOW] Rename params
     * @param x
     * @param y
     * @param z
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
     * @param entity
     */
    public void dropItem(@Nullable Entity entity) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (player.getAbilities().instabuild) {
                    return;
                }
            }

            this.spawnAtLocation(ZetterItems.EASEL.get());
        }
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param level
     * @param blockPos
     */
    public void dropAllContents(Level level, BlockPos blockPos) {
        for (int i = 0; i < this.easelContainer.getSlots(); i++) {
            Containers.dropItemStack(level, blockPos.getX(), blockPos.getY(), blockPos.getZ(), this.easelContainer.getStackInSlot(i));
        }
    }

    /**
     * The name is misleading; createMenu has nothing to do with creating a Screen, it is used to create the Container on the server only
     * @param windowID
     * @param playerInventory
     * @param playerEntity
     * @return
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInventory, Player playerEntity) {
        this.playersUsing.add(playerEntity);
        return EaselMenu.createMenuServerSide(windowID, playerInventory, this.easelContainer, this.stateHandler);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ZetterItems.EASEL.get());
    }

    /**
     * Copied from armor stand
     * @return
     */

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }
}

package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.state.EaselState;
import me.dantaeusb.zetter.menu.EaselMenu;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.network.packet.SEaselMenuCreatePacket;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.command.impl.data.EntityDataAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

public class  EaselEntity extends Entity implements ItemStackHandlerListener, INamedContainerProvider {
    private static final String NBT_TAG_EASEL_STORAGE = "storage";
    private static final String NBT_TAG_CANVAS_CODE = "CanvasCode";

    protected static final Predicate<Entity> IS_EASEL_ENTITY = (entity) -> {
        return entity instanceof EaselEntity;
    };

    private static final DataParameter<String> DATA_ID_CANVAS_CODE = EntityDataManager.defineId(EaselEntity.class, DataSerializers.STRING);

    protected BlockPos pos;
    protected EaselContainer easelContainer;
    protected EaselState stateHandler;

    protected final LazyOptional<ItemStackHandler> easelContainerOptional = LazyOptional.of(() -> this.easelContainer);

    /** The list of players currently using this easel */
    private ArrayList<PlayerEntity> playersUsing = new ArrayList<>();

    private int tick;

    public EaselEntity(EntityType<? extends EaselEntity> type, World world) {
        super(type, world);
        this.createInventory();
        this.stateHandler = new EaselState(this);
    }

    protected void defineSynchedData() {
        this.entityData.define(DATA_ID_CANVAS_CODE, "");
    }

    public @Nullable String getEntityCanvasCode() {
        String canvasCode = this.entityData.get(DATA_ID_CANVAS_CODE);

        if (canvasCode.isEmpty()) {
            return null;
        }

        return canvasCode;
    }

    public void playPlacementSound() {
        this.playSound(SoundEvents.ARMOR_STAND_PLACE, 1.0F, 1.0F);
    }

    /**
     * Set canvas code for referencing without loading the items to
     * render canvas on easel
     * @param canvasCode
     */
    protected void setEntityCanvasCode(@Nullable String canvasCode) {
        if (canvasCode != null) {
            this.entityData.set(DATA_ID_CANVAS_CODE, canvasCode);
        } else {
            this.entityData.set(DATA_ID_CANVAS_CODE, "");
        }
    }

    public IPacket<?> getAddEntityPacket() {
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
            this.setEntityCanvasCode(null);
            return;
        }

        String canvasCode = CanvasItem.getCanvasCode(canvasStack);

        if (canvasCode == null) {
            int[] size = CanvasItem.getBlockSize(canvasStack);
            assert size != null && size.length == 2;

            canvasCode = CanvasData.getDefaultCanvasCode(size[0], size[1]);
        }

        this.setEntityCanvasCode(canvasCode);
    }

    public boolean canPlayerAccessInventory(PlayerEntity player) {
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

    public void addAdditionalSaveData(CompoundNBT compoundTag) {
        compoundTag.put(NBT_TAG_EASEL_STORAGE, this.easelContainer.serializeNBT());

        if (this.getEntityCanvasCode() != null) {
            compoundTag.putString(NBT_TAG_CANVAS_CODE, this.getEntityCanvasCode());
        }
    }

    public void readAdditionalSaveData(CompoundNBT compoundTag) {
        this.createInventory();

        this.easelContainer.deserializeNBT(compoundTag.getCompound(NBT_TAG_EASEL_STORAGE));

        final String canvasCode = compoundTag.getString(NBT_TAG_CANVAS_CODE);

        if (canvasCode != null) {
            this.setEntityCanvasCode(canvasCode);
        }
    }

    /**
     * Needed for entity interaction
     * @return
     */
    public boolean isPickable() {
        return this.isAlive();
    }

    public boolean isPushable() {
        return false;
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (player.isCrouching() && heldItem.isEmpty()) {
            ItemStack canvasStack = this.easelContainer.extractCanvasStack();
            player.setItemInHand(hand, canvasStack);
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }

        final boolean isCanvas = heldItem.is(ZetterItems.CANVAS.get());
        final boolean isPalette = heldItem.is(ZetterItems.PALETTE.get());

        if (isCanvas) {
            if (this.easelContainer.getCanvasStack().isEmpty() && this.easelContainer.isItemValid(EaselContainer.CANVAS_SLOT, heldItem)) {
                this.easelContainer.setCanvasStack(heldItem);
                player.setItemInHand(hand, ItemStack.EMPTY);

                return ActionResultType.sidedSuccess(this.level.isClientSide);
            }
        } else if (isPalette) {
            if (this.easelContainer.getPaletteStack().isEmpty()) {
                this.easelContainer.setPaletteStack(heldItem);
                player.setItemInHand(hand, ItemStack.EMPTY);
            }
        }

        this.openInventory(player);

        return ActionResultType.sidedSuccess(this.level.isClientSide);
    }

    public void openInventory(PlayerEntity player) {
        if (!this.level.isClientSide) {
            NetworkHooks.openScreen((ServerPlayerEntity) player, this, (packetBuffer) -> {
                SEaselMenuCreatePacket packet = new SEaselMenuCreatePacket(this.getId(), this.getEntityCanvasCode());
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
        if (this.level.isClientSide()) {
            return;
        }

        if (this.getY() < -64.0D) {
            this.outOfWorld();
        }

        if (this.tick % 200 == 0) {
            this.playersUsing = this.calculatePlayersUsing();
        }

        if (this.tick % 100 == 0) {
            if (this.isAlive() && !this.survives()) {
                this.remove();
                this.dropItem(null);
                this.dropAllContents(this.level, this.getPos());
            }
        }
    }

    public boolean survives() {
        if (!this.level.noCollision(this)) {
            return false;
        } else {
            BlockPos posBelow = this.getPos().below();
            BlockState blockBelowState = this.level.getBlockState(posBelow);

            if (!blockBelowState.getMaterial().isSolid() && !DiodeBlock.isDiode(blockBelowState)) {
                return false;
            }

            return this.level.getEntities(this, this.getBoundingBox(), IS_EASEL_ENTITY).isEmpty();
        }
    }

    // specific

    public boolean hasCanvas() {
        return this.getEntityCanvasCode() != null;
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
        CanvasItem.getCanvasData(itemStack, this.level);
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
                CanvasItem.getCanvasData(canvasStack, this.level);
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
    public ArrayList<PlayerEntity> calculatePlayersUsing() {
        ArrayList<PlayerEntity> usingPlayers = new ArrayList<>();

        for(PlayerEntity player : this.level.getEntitiesOfClass(PlayerEntity.class, new AxisAlignedBB(this.pos.offset(-5, -5, -5), this.pos.offset(5, 5, 5)))) {
            if (player.containerMenu instanceof EaselMenu) {
                EaselContainer storage = ((EaselMenu)player.containerMenu).getContainer();

                if (storage == this.getEaselContainer()) {
                    usingPlayers.add(player);
                }
            }
        }

        return usingPlayers;
    }

    public ArrayList<PlayerEntity> getPlayersUsing() {
        return this.playersUsing;
    }

    /**
     * Drop contents and item then die when moved or hurt
     */

    public boolean hurt(DamageSource damageSource, float pAmount) {
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else {
            if (!this.level.isClientSide) {
                if (this.isAlive()) {
                    this.kill();
                    this.markHurt();
                    this.dropItem(damageSource.getEntity());
                    this.dropAllContents(this.level, this.pos);
                }
            }
            return true;
        }
    }

    @Override
    public void move(MoverType mover, Vector3d move) {
        if (!this.level.isClientSide && this.isAlive() && move.lengthSqr() > 0.0D) {
            this.kill();
            this.dropItem(null);
            this.dropAllContents(this.level, this.pos);
        }
    }

    /**
     * @todo: [LOW] Rename params
     * @param x
     * @param y
     * @param z
     */
    public void push(double x, double y, double z) {
        if (!this.level.isClientSide && this.isAlive() && x * x + y * y + z * z > 0.0D) {
            this.kill();
            this.dropItem(null);
            this.dropAllContents(this.level, this.pos);
        }
    }

    /**
     * Drop an item associated with this entity
     * @param entity
     */
    public void dropItem(@Nullable Entity entity) {
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity)entity;
                if (player.abilities.instabuild) {
                    return;
                }
            }

            this.spawnAtLocation(ZetterItems.EASEL.get());
        }
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(World world, BlockPos blockPos) {
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
    public Container createMenu(int windowID, PlayerInventory playerInventory, PlayerEntity playerEntity) {
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

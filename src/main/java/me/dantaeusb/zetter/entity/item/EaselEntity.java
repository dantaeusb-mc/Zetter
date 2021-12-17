package me.dantaeusb.zetter.entity.item;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

public class EaselEntity extends Entity implements ItemStackHandlerListener, MenuProvider {
    private static final String EASEL_STORAGE_TAG = "storage";
    private static final String CANVAS_CODE_TAG = "CanvasCode";

    protected static final Predicate<Entity> IS_EASEL_ENTITY = (entity) -> {
        return entity instanceof EaselEntity;
    };

    private static final EntityDataAccessor<String> DATA_ID_CANVAS_CODE = SynchedEntityData.defineId(EaselEntity.class, EntityDataSerializers.STRING);

    protected BlockPos pos;
    protected EaselContainer easelContainer;
    protected final LazyOptional<ItemStackHandler> easelContainerOptional = LazyOptional.of(() -> this.easelContainer);

    /** The list of players currently using this easel */
    private ArrayList<Player> playersUsing = new ArrayList<>();
    private int ticksSinceSync;
    private int checkInterval = 0;

    public EaselEntity(EntityType<? extends EaselEntity> type, Level world) {
        super(type, world);
        this.createInventory();
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

    protected void setCanvasCode(@Nullable String canvasCode) {
        if (canvasCode != null) {
            this.entityData.set(DATA_ID_CANVAS_CODE, canvasCode);
        } else {
            this.entityData.set(DATA_ID_CANVAS_CODE, "");
        }
    }

    public void onSyncedDataUpdated(EntityDataAccessor<?> entityDataAccessor) {
        super.onSyncedDataUpdated(entityDataAccessor);

        /**
         * As canvas is not synced on client side, we need to handle the situation
         * where canvas is automatically initialized when placed on easel,
         * and update canvas code on canvas item on client side accordingly
         */
        if (
            this.level.isClientSide
            && DATA_ID_CANVAS_CODE.equals(entityDataAccessor)
            && this.getCanvasCode() != null
        ) {
            final ItemStack canvasStack = this.easelContainer.getCanvasStack();
            CanvasItem.setCanvasCode(canvasStack, this.getCanvasCode());
        }
    }

    public Packet<?> getAddEntityPacket() {
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
        this.updateDataFromInventory();
    }

    protected void updateDataFromInventory() {
        this.setCanvasCode(CanvasItem.getCanvasCode(this.easelContainer.getCanvasStack()));
    }

    public boolean canPlayerAccessInventory(Player player) {
        // @todo: this
        return true;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                && (direction == null || direction == Direction.UP || direction == Direction.DOWN)) {
            return this.easelContainerOptional.cast();
        }

        return super.getCapability(capability, direction);
    }

    /**
     * This is temporary for migrating from BE to Entity
     * @return
     */
    @Deprecated()
    public EaselContainer getEaselContainer() {
        return this.easelContainer;
    }

    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put(EASEL_STORAGE_TAG, this.easelContainer.serializeNBT());

        if (this.getCanvasCode() != null) {
            compoundTag.putString(CANVAS_CODE_TAG, this.getCanvasCode());
        }
    }

    public void readAdditionalSaveData(CompoundTag compoundTag) {
        this.createInventory();

        this.easelContainer.deserializeNBT(compoundTag.getCompound(EASEL_STORAGE_TAG));

        final String canvasCode = compoundTag.getString(CANVAS_CODE_TAG);

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
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        }

        final boolean isCanvas = heldItem.is(ZetterItems.CANVAS);
        final boolean isPalette = heldItem.is(ZetterItems.PALETTE);

        if (isCanvas) {
            this.easelContainer.setCanvasStack(heldItem);
            player.setItemInHand(hand, ItemStack.EMPTY);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
        } else if (isPalette) {
            this.easelContainer.setPaletteStack(heldItem);
            player.setItemInHand(hand, ItemStack.EMPTY);
        }

        this.openInventory(player);

        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    public void openInventory(Player player) {
        if (!this.level.isClientSide) {
            NetworkHooks.openGui((ServerPlayer) player, this, (packetBuffer) -> {
                SCanvasNamePacket.writeCanvasName(packetBuffer, (this.getCanvasCode()));
            });
        }
    }

    public void tick() {
        // No need to track on client side
        if (this.level.isClientSide()) {
            return;
        }

        this.checkOutOfWorld();
        if (++this.ticksSinceSync > 200) {
            this.playersUsing = this.calculatePlayersUsing();
        }

        if (this.checkInterval++ == 100) {
            this.checkInterval = 0;
            if (!this.isRemoved() && !this.survives()) {
                this.discard();
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

        if (!itemStack.is(ZetterItems.CANVAS)) {
            Zetter.LOG.error("Trying to put non-canvas on easel, item likely will be removed");
            return false;
        }

        // @todo: seems dumb, lazy load ftw
        // Also could already have canvas, but it's client-only right now so we disregard client data
        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.level);
        this.easelContainer.setCanvasStack(itemStack);

        return true;
    }

    public @Nullable
    CanvasData getCanvasData() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack.isEmpty() || canvasStack.getItem() != ZetterItems.CANVAS) {
            return null;
        }

        return  CanvasItem.getCanvasData(canvasStack, this.level);
    }

    public void containerChanged(ItemStackHandler easelContainer) {
        ItemStack canvasStack = ((EaselContainer)easelContainer).getCanvasStack();

        if (!canvasStack.isEmpty() && CanvasItem.getCanvasCode(canvasStack) == null) {
            // Initialize if not yet
            CanvasItem.getCanvasData(canvasStack, this.level);
        }

        this.updateDataFromInventory();
    }

    // track using players to send packets

    public ArrayList<Player> calculatePlayersUsing() {
        ArrayList<Player> usingPlayers = new ArrayList<>();

        /*for(Player player : this.level.getEntitiesOfClass(Player.class, new AABB(this.pos.offset(-5, -5, -5), this.pos.offset(5, 5, 5)))) {
            if (player.containerMenu instanceof EaselContainerMenu) {
                EaselContainer storage = ((EaselContainerMenu)player.containerMenu).getEaselContainer();

                if (storage == this.getEaselContainer()) {
                    usingPlayers.add(player);
                }
            }
        }*/

        return usingPlayers;
    }

    public ArrayList<Player> getPlayersUsing() {
        return this.playersUsing;
    }

    public void setPos(double x, double y, double z) {
        this.pos = new BlockPos(x, y, z);
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

    public boolean hurt(DamageSource damageSource, float p_31716_) {
        if (this.isInvulnerableTo(damageSource)) {
            return false;
        } else {
            if (!this.level.isClientSide) {
                if (!this.isRemoved()) {
                    this.kill();
                    this.markHurt();
                    this.dropAllContents(this.level, this.pos);
                }

                if (!damageSource.isExplosion()) {
                    this.playSound(this.getRemoveItemSound(), 1.0F, 1.0F);
                }
            }
            return true;
        }
    }

    public void move(MoverType mover, Vec3 move) {
        if (!this.level.isClientSide && !this.isRemoved() && move.lengthSqr() > 0.0D) {
            this.kill();
            this.dropItem(null);
            this.dropAllContents(this.level, this.pos);
        }
    }

    /**
     * @todo: rename params
     * @param x
     * @param y
     * @param z
     */
    public void push(double x, double y, double z) {
        if (!this.level.isClientSide && !this.isRemoved() && x * x + y * y + z * z > 0.0D) {
            this.kill();
            this.dropItem(null);
            this.dropAllContents(this.level, this.pos);
        }
    }

    /**
     * Drop an item associated with this entity
     * @param p_31717_
     */
    public void dropItem(@Nullable Entity entity) {
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (player.getAbilities().instabuild) {
                    return;
                }
            }

            this.spawnAtLocation(ZetterItems.EASEL);
        }
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(Level world, BlockPos blockPos) {
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
        return EaselContainerMenu.createContainerServerSide(windowID, playerInventory, this, this.easelContainer);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ZetterItems.EASEL);
    }

    /**
     * Copied from armor stand
     * @return
     */

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }
}

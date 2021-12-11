package com.dantaeusb.zetter.entity.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModEntities;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.menu.EaselContainerMenu;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import com.dantaeusb.zetter.network.packet.SEaselCanvasChangePacket;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.tileentity.container.EaselContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.decoration.HangingEntity;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.function.Predicate;

public class EaselEntity extends Entity implements ContainerListener, MenuProvider {
    protected static final Predicate<Entity> IS_EASEL_ENTITY = (entity) -> {
        return entity instanceof EaselEntity;
    };

    private static final EntityDataAccessor<String> DATA_ID_CANVAS_CODE = SynchedEntityData.defineId(EaselEntity.class, EntityDataSerializers.STRING);

    protected BlockPos pos;
    protected EaselContainer easelContainer;

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

    public String getCanvasCode() {
        return this.entityData.get(DATA_ID_CANVAS_CODE);
    }

    protected void setCanvasCode(String canvasCode) {
        this.entityData.set(DATA_ID_CANVAS_CODE, canvasCode);
    }

    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    protected void createInventory() {
        EaselContainer currentEaselStorage = this.easelContainer;
        this.easelContainer = new EaselContainer();

        if (currentEaselStorage != null) {
            currentEaselStorage.removeListener(this);
            int i = Math.min(currentEaselStorage.getContainerSize(), this.easelContainer.getContainerSize());

            for(int j = 0; j < i; ++j) {
                ItemStack itemstack = currentEaselStorage.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.easelContainer.setItem(j, itemstack.copy());
                }
            }
        }

        this.easelContainer.addListener(this);
        this.updateDataFromInventory();
        this.itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this.easelContainer));
    }

    protected void updateDataFromInventory() {
        if (!this.level.isClientSide) {
            this.setCanvasCode(CanvasItem.getCanvasCode(this.easelContainer.getCanvasStack()));
        }
    }

    public boolean canPlayerAccessInventory(Player player) {
        // @todo: this
        return true;
    }

    public void addAdditionalSaveData(CompoundTag compoundTag) {
        ListTag easelContainerItems = new ListTag();

        for(int i = 0; i < this.easelContainer.getContainerSize(); i++) {
            ItemStack item = this.easelContainer.getItem(i);
            if (!item.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)i);
                item.save(compoundtag);

                easelContainerItems.add(compoundtag);
            }
        }

        compoundTag.put("Items", easelContainerItems);
    }

    public void readAdditionalSaveData(CompoundTag compoundTag) {
        this.createInventory();

        ListTag easelContainerItems = compoundTag.getList("Items", 10);

        for(int i = 0; i < easelContainerItems.size(); ++i) {
            CompoundTag compoundtag = easelContainerItems.getCompound(i);

            this.easelContainer.setItem(i, ItemStack.of(compoundtag));
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

        final boolean isCanvas = heldItem.is(ModItems.CANVAS);
        final boolean isPalette = heldItem.is(ModItems.PALETTE);

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

        if (!itemStack.is(ModItems.CANVAS)) {
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

    public @Nullable CanvasData getCanvasData() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack.isEmpty() || canvasStack.getItem() != ModItems.CANVAS) {
            return null;
        }

        return  CanvasItem.getCanvasData(canvasStack, this.level);
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

            this.spawnAtLocation(ModItems.EASEL);
        }
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(Level world, BlockPos blockPos) {
        Containers.dropContents(world, blockPos, this.easelContainer);
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
    public void containerChanged(Container easelContainer) {
        ItemStack canvasStack = ((EaselContainer)easelContainer).getCanvasStack();

        if (!canvasStack.isEmpty() && CanvasItem.getCanvasCode(canvasStack) == null) {
            // Initialize if not yet
            CanvasItem.getCanvasData(canvasStack, this.level);
        }

        this.updateDataFromInventory();
    }

    /**
     * Copied from armor stand
     * @return
     */

    public SoundEvent getRemoveItemSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }

    /**
     * Not sure why Forge uses it but let's keep it there
     */

    private net.minecraftforge.common.util.LazyOptional<?> itemHandler = null;

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.core.Direction facing) {
        if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && itemHandler != null)
            return itemHandler.cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemHandler != null) {
            net.minecraftforge.common.util.LazyOptional<?> oldHandler = itemHandler;
            itemHandler = null;
            oldHandler.invalidate();
        }
    }

    public boolean hasInventoryChanged(Container container) {
        return this.easelContainer != container;
    }
}

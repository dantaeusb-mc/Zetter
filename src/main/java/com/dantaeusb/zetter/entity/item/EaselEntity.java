package com.dantaeusb.zetter.entity.item;

import com.dantaeusb.zetter.menu.EaselContainerMenu;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.tileentity.container.EaselContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class EaselEntity extends Entity implements ContainerListener, MenuProvider {
    private static final String CANVAS_ITEM_TAG = "CanvasItem";
    private static final String PALETTE_ITEM_TAG = "PaletteItem";

    protected BlockPos pos;

    protected EaselContainer easelContainer;

    /** The list of players currently using this easel */
    private ArrayList<Player> playersUsing = new ArrayList<>();
    private int ticksSinceSync;

    public EaselEntity(EntityType<? extends EaselEntity> type, Level world) {
        super(type, world);
        this.createInventory();
    }

    protected void defineSynchedData() {
        // Nothing here!
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
        //this.updateContainerEquipment();
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

        if (++this.ticksSinceSync > 200) {
            this.playersUsing = this.calculatePlayersUsing();
        }
    }

    // specific

    public EaselContainer getEaselContainer() {
        return this.easelContainer;
    }

    public boolean hasCanvas() {
        ItemStack canvasStack = this.getCanvasStack();

        return !canvasStack.isEmpty();
    }

    public @Nullable ItemStack getCanvasStack() {
        return this.easelContainer.getCanvasStack();
    }

    public boolean putCanvasStack(ItemStack itemStack) {
        if (itemStack.getItem() != ModItems.CANVAS) {
            return false;
        }

        if (this.hasCanvas()) {
            return false;
        }

        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.level);
        this.easelContainer.setCanvasStack(itemStack);

        return true;
    }

    /**
     * Returns current canvas name or empty string if no canvas assigned
     * @todo: refactor maybe
     * @return
     */
    public @Nullable String getCanvasCode() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack != null && canvasStack.isEmpty()) {
            return null;
        }

        String canvasCode = CanvasItem.getCanvasCode(canvasStack);

        if (canvasCode == null) {
            CanvasItem.getCanvasData(canvasStack, this.level);
            canvasCode = CanvasItem.getCanvasCode(canvasStack);
        }

        return canvasCode;
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
            if (!this.isRemoved() && !this.level.isClientSide) {
                this.kill();
                this.markHurt();
                this.dropAllContents(this.level, this.pos);
            }

            return true;
        }
    }

    public void move(MoverType mover, Vec3 move) {
        if (!this.level.isClientSide && !this.isRemoved() && move.lengthSqr() > 0.0D) {
            this.kill();
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
            this.dropAllContents(this.level, this.pos);
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

    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
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
    public void containerChanged(Container p_18983_) {

    }
}

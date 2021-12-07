package com.dantaeusb.zetter.entity.item;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.menu.EaselContainerMenu;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.tileentity.container.EaselContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class EaselEntity extends Entity implements ContainerListener, MenuProvider {
    private static final String CANVAS_ITEM_TAG = "CanvasItem";
    private static final String PALETTE_ITEM_TAG = "PaletteItem";

    protected BlockPos pos;
    protected Direction direction = Direction.SOUTH;

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
        return new ClientboundAddEntityPacket(this, this.getType(), this.direction.get3DDataValue(), this.getPos());
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

    public void addAdditionalSaveData(CompoundTag tag) {
        if (!this.easelContainer.getCanvasStack().isEmpty()) {
            tag.put(EaselEntity.CANVAS_ITEM_TAG, this.easelContainer.getCanvasStack().save(new CompoundTag()));
        }

        if (!this.easelContainer.getPaletteStack().isEmpty()) {
            tag.put(EaselEntity.PALETTE_ITEM_TAG, this.easelContainer.getPaletteStack().save(new CompoundTag()));
        }
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(EaselEntity.CANVAS_ITEM_TAG, 10)) {
            ItemStack canvasStack = ItemStack.of(tag.getCompound(EaselEntity.CANVAS_ITEM_TAG));

            if (canvasStack.is(ModItems.CANVAS)) {
                this.easelContainer.setCanvasStack(canvasStack);
            } else {
                Zetter.LOG.error("Found non-canvas in Easel storage at slot CanvasItem");
            }
        }

        if (tag.contains(EaselEntity.PALETTE_ITEM_TAG, 10)) {
            ItemStack paletteStack = ItemStack.of(tag.getCompound(EaselEntity.PALETTE_ITEM_TAG));

            if (paletteStack.is(ModItems.PALETTE)) {
                this.easelContainer.setItem(1, paletteStack);
            } else {
                Zetter.LOG.error("Found non-palette in Easel storage at slot PaletteItem");
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);

        final boolean isCanvas = heldItem.is(ModItems.CANVAS);
        final boolean isPalette = heldItem.is(ModItems.PALETTE);

        if (isCanvas) {
            this.openInventory(player);
        } else if (isPalette) {
            this.openInventory(player);
        }

        return InteractionResult.PASS;
    }

    public void openInventory(Player player) {
        if (!this.level.isClientSide) {
            NetworkHooks.openGui((ServerPlayer) player, this, (packetBuffer) -> {
                SCanvasNamePacket.writeCanvasName(packetBuffer, (this.getCanvasName()));
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
        return this.easelContainer.getItem(EaselContainer.CANVAS_SLOT);
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
     * @return
     */
    public String getCanvasName() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack != null && canvasStack.isEmpty()) {
            return "";
        }

        return CanvasItem.getCanvasCode(canvasStack);
    }

    @Nullable
    public CanvasData getCanvasData() {
        ItemStack canvasStack = this.getCanvasStack();

        if (canvasStack.isEmpty() || canvasStack.getItem() != ModItems.CANVAS) {
            return null;
        }

        return  CanvasItem.getCanvasData(canvasStack, this.level);
    }

    // track using players to send packets

    public ArrayList<Player> calculatePlayersUsing() {
        ArrayList<Player> usingPlayers = new ArrayList<>();

        for(Player player : this.level.getEntitiesOfClass(Player.class, new AABB(this.pos.offset(-5, -5, -5), this.pos.offset(5, 5, 5)))) {
            if (player.containerMenu instanceof EaselContainerMenu) {
                EaselContainer storage = ((EaselContainerMenu)player.containerMenu).getEaselContainer();

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
        this.pos = new BlockPos(x, y, z);
        this.hasImpulse = true;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    /*protected AABB makeBoundingBox() {
        return this.dimensions.makeBoundingBox(this.pos);
    }*/

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
    public void containerChanged(Container p_18983_) {

    }
}

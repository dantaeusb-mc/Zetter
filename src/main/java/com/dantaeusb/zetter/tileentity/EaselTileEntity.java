package com.dantaeusb.zetter.tileentity;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.core.ModTileEntities;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.tileentity.storage.EaselStorage;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class EaselTileEntity extends TileEntity implements ITickableTileEntity, INamedContainerProvider {
    private final EaselStorage easelStorage; // two items: canvas and palette

    private static final String EASEL_STORAGE_TAG = "storage";

    /** The list of players currently using this easel */
    private ArrayList<PlayerEntity> playersUsing = new ArrayList<>();
    private int ticksSinceSync;

    public EaselTileEntity() {
        super(ModTileEntities.EASEL_TILE_ENTITY);

        this.easelStorage = EaselStorage.createForTileEntity(this::canPlayerAccessInventory, this::markDirty);
    }

    public boolean canPlayerAccessInventory(PlayerEntity player) {
        if (this.world.getTileEntity(this.pos) != this) {
            return false;
        } else {
            return player.getDistanceSq((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) <= 64.0D;
        }
    }

    public void tick() {
        // No need to track on client side
        if (this.world.isRemote()) {
            return;
        }

        if (++this.ticksSinceSync > 200) {
            this.playersUsing = this.calculatePlayersUsing();
        }
    }

    // specific

    public EaselStorage getEaselStorage() {
        return this.easelStorage;
    }

    public boolean hasCanvas() {
        ItemStack canvasStack = this.getCanvasStack();

        return !canvasStack.isEmpty();
    }

    public @Nullable ItemStack getCanvasStack() {
        return this.easelStorage.getStackInSlot(EaselStorage.CANVAS_SLOT);
    }

    public boolean putCanvasStack(ItemStack itemStack) {
        if (itemStack.getItem() != ModItems.CANVAS) {
            return false;
        }

        if (this.hasCanvas()) {
            return false;
        }

        // Initialize data if it's not yet
        CanvasItem.getCanvasData(itemStack, this.world);
        this.easelStorage.setCanvasStack(itemStack);

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

        return  CanvasItem.getCanvasData(canvasStack, this.world);
    }

    // track using players to send packets

    public ArrayList<PlayerEntity> calculatePlayersUsing() {
        ArrayList<PlayerEntity> usingPlayers = new ArrayList<>();

        for(PlayerEntity player : this.world.getEntitiesWithinAABB(PlayerEntity.class, new AxisAlignedBB(this.pos.add(-5, -5, -5), this.pos.add(5, 5, 5)))) {
            if (player.openContainer instanceof EaselContainer) {
                EaselStorage storage = ((EaselContainer)player.openContainer).getEaselStorage();

                if (storage == this.getEaselStorage()) {
                    usingPlayers.add(player);
                }
            }
        }

        return usingPlayers;
    }

    public ArrayList<PlayerEntity> getPlayersUsing() {
        return this.playersUsing;
    }

    // render

    @Override
    public AxisAlignedBB getRenderBoundingBox()
    {
        return new AxisAlignedBB(this.getPos(), this.getPos().add(1, 2, 1));
    }

    // NBT stack

    @Override
    public CompoundNBT write(CompoundNBT parentNBTTagCompound)
    {
        super.write(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        CompoundNBT inventoryNBT = this.easelStorage.serializeNBT();
        parentNBTTagCompound.put(EASEL_STORAGE_TAG, inventoryNBT);

        return parentNBTTagCompound;
    }

    @Override
    public void read(BlockState blockState, CompoundNBT parentNBTTagCompound)
    {
        super.read(blockState, parentNBTTagCompound);

        CompoundNBT inventoryNBT = parentNBTTagCompound.getCompound(EASEL_STORAGE_TAG);
        this.easelStorage.deserializeNBT(inventoryNBT);

        if (this.easelStorage.getSizeInventory() != EaselStorage.STORAGE_SIZE)
            throw new IllegalArgumentException("Corrupted NBT: Number of inventory slots did not match expected.");
    }

    // network stack

    @Override
    @Nullable
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);

        int tileEntityType = 42;
        return new SUpdateTileEntityPacket(this.pos, tileEntityType, nbtTagCompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        BlockState blockState = world.getBlockState(pos);
        read(blockState, packet.getNbtCompound());
    }

    @Override
    public CompoundNBT getUpdateTag()
    {
        CompoundNBT nbtTagCompound = new CompoundNBT();
        write(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(BlockState blockState, CompoundNBT tag)
    {
        this.read(blockState, tag);
    }

    /**
     * When this tile entity is destroyed, drop all of its contents into the world
     * @param world
     * @param blockPos
     */
    public void dropAllContents(World world, BlockPos blockPos) {
        InventoryHelper.dropInventoryItems(world, blockPos, this.easelStorage);
    }

    //

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("container.zetter.easel");
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
        return EaselContainer.createContainerServerSide(windowID, playerInventory, this, this.easelStorage);
    }
}
